"""
YOLO 训练和预测 Web API 服务
基于 FastAPI 框架提供 RESTful API 接口
"""
import os
import sys
import shutil
import signal
from pathlib import Path
from typing import Optional, List
from fastapi import FastAPI, File, UploadFile, HTTPException, BackgroundTasks, Query
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import uvicorn

# 添加当前目录到路径
sys.path.insert(0, str(Path(__file__).parent))

from config import config
from yolo_trainer import training_manager, TrainingStatus
from yolo_predictor import prediction_manager
from nacos_registry import nacos_registry


# ==================== 初始化 FastAPI ====================
app = FastAPI(
    title="YOLO Training & Prediction API",
    description="YOLOv7 训练和预测服务的 RESTful API",
    version="1.0.0"
)

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 初始化配置
config.initialize()


# ==================== Pydantic 模型 ====================
class TrainingConfigRequest(BaseModel):
    """训练配置请求模型"""
    dataset_yaml: str = Field(..., description="数据集 YAML 配置文件路径")
    epochs: int = Field(default=300, ge=1, le=10000, description="训练轮数")
    batch_size: int = Field(default=4, ge=1, le=128, description="批次大小")
    image_size: int = Field(default=1280, ge=32, le=4096, description="图像尺寸")
    device: str = Field(default="0", description="设备 (cpu 或 GPU ID)")
    weights: str = Field(default="yolov7x.pt", description="预训练权重文件")
    use_adam: bool = Field(default=False, description="是否使用 Adam 优化器")
    hyp: str = Field(default="data/hyp.scratch.p5.yaml", description="超参数配置文件")
    workers: int = Field(default=4, ge=0, le=16, description="数据加载工作进程数")
    cache: bool = Field(default=False, description="是否缓存图像到内存")


class TrainingConfigNoYamlRequest(BaseModel):
    """训练配置请求模型（不需要 data.yaml）"""
    train_dir: str = Field(..., description="训练集图片目录路径")
    val_dir: str = Field(..., description="验证集图片目录路径")
    test_dir: Optional[str] = Field(None, description="测试集图片目录路径（可选）")
    classes: list = Field(..., description="类别名称列表，例如: ['person', 'car', 'dog']")
    nc: Optional[int] = Field(None, description="类别数量（可选，默认根据 classes 列表计算）")
    epochs: int = Field(default=300, ge=1, le=10000, description="训练轮数")
    batch_size: int = Field(default=4, ge=1, le=128, description="批次大小")
    image_size: int = Field(default=1280, ge=32, le=4096, description="图像尺寸")
    device: str = Field(default="0", description="设备 (cpu 或 GPU ID)")
    weights: str = Field(default="yolov7x.pt", description="预训练权重文件")
    use_adam: bool = Field(default=False, description="是否使用 Adam 优化器")
    hyp: str = Field(default="data/hyp.scratch.p5.yaml", description="超参数配置文件")
    workers: int = Field(default=4, ge=0, le=16, description="数据加载工作进程数")
    cache: bool = Field(default=False, description="是否缓存图像到内存")


class PredictionRequest(BaseModel):
    """预测请求模型"""
    model_path: str = Field(..., description="模型权重文件路径")
    device: str = Field(default="0", description="设备 (cpu 或 GPU ID)")
    conf_thres: float = Field(default=0.25, ge=0.0, le=1.0, description="置信度阈值")
    iou_thres: float = Field(default=0.45, ge=0.0, le=1.0, description="IOU 阈值")
    img_size: int = Field(default=640, ge=32, le=4096, description="图像尺寸")


class PredictorCreateRequest(BaseModel):
    """创建预测器请求模型"""
    model_path: str = Field(..., description="模型权重文件路径")
    device: str = Field(default="0", description="设备")
    conf_thres: float = Field(default=0.25, ge=0.0, le=1.0, description="置信度阈值")
    iou_thres: float = Field(default=0.45, ge=0.0, le=1.0, description="IOU 阈值")


# ==================== 健康检查 ====================
@app.get("/")
async def root():
    """根路径 - API 信息"""
    return {
        "service": "YOLO Training & Prediction API",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
        "endpoints": {
            "training": "/api/v1/training",
            "prediction": "/api/v1/prediction"
        },
        "nacos_registered": nacos_registry.is_registered() if config.NACOS_ENABLED else False
    }


@app.get("/health")
async def health_check():
    """健康检查"""
    import torch
    
    return {
        "status": "healthy",
        "gpu_available": torch.cuda.is_available() if 'torch' in sys.modules else False,
        "nacos_registered": nacos_registry.is_registered() if config.NACOS_ENABLED else False
    }


# ==================== 训练相关 API ====================
@app.post("/api/v1/training/tasks", summary="创建训练任务")
async def create_training_task(request: TrainingConfigRequest):
    """
    创建新的 YOLO 训练任务（需要 data.yaml）
    
    - **dataset_yaml**: 数据集配置文件路径
    - **epochs**: 训练轮数 (1-10000)
    - **batch_size**: 批次大小 (1-128)
    - **image_size**: 图像尺寸 (32-4096, 必须是32的倍数)
    - **device**: 设备 (cpu 或 GPU ID)
    - **weights**: 预训练权重文件
    - **use_adam**: 是否使用 Adam 优化器
    """
    try:
        # 构建配置字典
        training_config = {
            "dataset_yaml": request.dataset_yaml,
            "epochs": request.epochs,
            "batch_size": request.batch_size,
            "image_size": request.image_size,
            "device": request.device,
            "weights": request.weights,
            "use_adam": request.use_adam,
            "hyp": request.hyp,
            "workers": request.workers,
            "cache": request.cache
        }
        
        # 创建任务
        task_id = training_manager.create_task(training_config)
        
        return {
            "success": True,
            "task_id": task_id,
            "message": "训练任务已创建，请调用 /start 接口启动训练"
        }
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/api/v1/training/tasks/no-yaml", summary="创建训练任务（无需 data.yaml）")
async def create_training_task_no_yaml(request: TrainingConfigNoYamlRequest):
    """
    创建新的 YOLO 训练任务（不需要 data.yaml 配置文件）
    
    该接口会自动生成 data.yaml 配置文件，您只需提供：
    - 训练集、验证集图片目录
    - 类别名称列表
    
    - **train_dir**: 训练集图片目录路径
    - **val_dir**: 验证集图片目录路径
    - **test_dir**: 测试集图片目录路径（可选）
    - **classes**: 类别名称列表，例如: ["person", "car", "dog"]
    - **nc**: 类别数量（可选，默认根据 classes 列表计算）
    - **epochs**: 训练轮数 (1-10000)
    - **batch_size**: 批次大小 (1-128)
    - **image_size**: 图像尺寸 (32-4096)
    - **device**: 设备 (cpu 或 GPU ID)
    - **weights**: 预训练权重文件
    - **use_adam**: 是否使用 Adam 优化器
    """
    try:
        # 计算类别数量
        nc = request.nc if request.nc is not None else len(request.classes)
        
        # 验证类别数量一致性
        if nc != len(request.classes):
            raise ValueError(f"类别数量 nc ({nc}) 与 classes 列表长度 ({len(request.classes)}) 不一致")
        
        # 验证目录是否存在
        from pathlib import Path
        train_path = Path(request.train_dir)
        val_path = Path(request.val_dir)
        
        if not train_path.exists():
            raise ValueError(f"训练集目录不存在: {request.train_dir}")
        if not val_path.exists():
            raise ValueError(f"验证集目录不存在: {request.val_dir}")
        
        if request.test_dir:
            test_path = Path(request.test_dir)
            if not test_path.exists():
                raise ValueError(f"测试集目录不存在: {request.test_dir}")
        
        # 构建配置字典
        training_config = {
            "train_dir": str(train_path.absolute()),
            "val_dir": str(val_path.absolute()),
            "test_dir": str(Path(request.test_dir).absolute()) if request.test_dir else None,
            "classes": request.classes,
            "nc": nc,
            "epochs": request.epochs,
            "batch_size": request.batch_size,
            "image_size": request.image_size,
            "device": request.device,
            "weights": request.weights,
            "use_adam": request.use_adam,
            "hyp": request.hyp,
            "workers": request.workers,
            "cache": request.cache,
            "no_yaml_mode": True  # 标记为无 yaml 模式
        }
        
        # 创建任务
        task_id = training_manager.create_task(training_config)
        
        return {
            "success": True,
            "task_id": task_id,
            "message": "训练任务已创建（将自动生成 data.yaml），请调用 /start 接口启动训练",
            "info": {
                "train_images": str(train_path),
                "val_images": str(val_path),
                "test_images": str(Path(request.test_dir).absolute()) if request.test_dir else None,
                "classes": request.classes,
                "num_classes": nc
            }
        }
        
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/training/tasks/{task_id}/start", summary="启动训练任务")
async def start_training_task(task_id: str, background_tasks: BackgroundTasks):
    """
    启动指定的训练任务（后台异步执行）
    
    - **task_id**: 任务ID
    """
    try:
        success = training_manager.start_training(task_id)
        
        return {
            "success": success,
            "task_id": task_id,
            "message": "训练任务已启动"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/training/tasks", summary="列出所有训练任务")
async def list_training_tasks(status: Optional[str] = Query(None, description="状态过滤器")):
    """
    获取所有训练任务列表
    
    - **status**: 可选的状态过滤器 (pending/running/completed/failed/cancelled)
    """
    try:
        tasks = training_manager.list_tasks(status_filter=status)
        
        return {
            "success": True,
            "count": len(tasks),
            "tasks": tasks
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/training/tasks/{task_id}", summary="获取训练任务状态")
async def get_training_task_status(task_id: str):
    """
    获取指定训练任务的详细状态
    
    - **task_id**: 任务ID
    """
    try:
        status = training_manager.get_task_status(task_id)
        
        return {
            "success": True,
            "task": status
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/training/tasks/{task_id}/cancel", summary="取消训练任务")
async def cancel_training_task(task_id: str):
    """
    取消正在运行或等待的训练任务
    
    - **task_id**: 任务ID
    """
    try:
        success = training_manager.cancel_task(task_id)
        
        return {
            "success": success,
            "task_id": task_id,
            "message": "训练任务已取消"
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/training/tasks/{task_id}/log", summary="获取训练日志")
async def get_training_log(task_id: str, lines: int = Query(100, ge=1, le=1000)):
    """
    获取训练任务的日志输出
    
    - **task_id**: 任务ID
    - **lines**: 返回的行数 (1-1000)
    """
    try:
        log_content = training_manager.get_training_log(task_id, lines=lines)
        
        return {
            "success": True,
            "task_id": task_id,
            "log": log_content
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ==================== 预测相关 API ====================
@app.post("/api/v1/prediction/predictors", summary="创建预测器实例")
async def create_predictor(request: PredictorCreateRequest):
    """
    创建一个新的 YOLO 预测器实例
    
    - **model_path**: 模型权重文件路径
    - **device**: 设备 (cpu 或 GPU ID)
    - **conf_thres**: 置信度阈值 (0.0-1.0)
    - **iou_thres**: IOU 阈值 (0.0-1.0)
    """
    try:
        predictor_id = prediction_manager.create_predictor(
            model_path=request.model_path,
            device=request.device,
            conf_thres=request.conf_thres,
            iou_thres=request.iou_thres
        )
        
        return {
            "success": True,
            "predictor_id": predictor_id,
            "message": "预测器已创建"
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/prediction/predict", summary="执行图像预测")
async def predict_image(
    file: UploadFile = File(..., description="上传的图像文件"),
    predictor_id: str = Query(..., description="预测器ID"),
    img_size: int = Query(640, ge=32, le=4096, description="图像尺寸")
):
    """
    对上传的图像执行 YOLO 预测
    
    - **file**: 上传的图像文件 (jpg/jpeg/png/bmp/tiff)
    - **predictor_id**: 预测器ID
    - **img_size**: 输入图像尺寸 (32-4096)
    """
    # 验证文件类型
    if not config.is_allowed_image(file.filename):
        raise HTTPException(
            status_code=400,
            detail=f"不支持的文件类型: {file.filename}。支持的格式: {config.ALLOWED_IMAGE_EXTENSIONS}"
        )
    
    try:
        # 保存上传文件
        upload_dir = config.UPLOAD_DIR / f"pred_{predictor_id}"
        upload_dir.mkdir(parents=True, exist_ok=True)
        
        file_path = upload_dir / file.filename
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        # 执行预测
        result = prediction_manager.predict(
            predictor_id=predictor_id,
            image_path=str(file_path),
            img_size=img_size
        )
        
        return {
            "success": True,
            "prediction_id": result.prediction_id,
            "detections": result.detections,
            "inference_time": result.inference_time,
            "output_image": str(result.output_image) if result.output_image else None
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/prediction/results/{prediction_id}", summary="获取预测结果")
async def get_prediction_result(prediction_id: str):
    """
    获取指定预测任务的详细结果
    
    - **prediction_id**: 预测ID
    """
    try:
        result = prediction_manager.get_result(prediction_id)
        
        return {
            "success": True,
            "result": result.to_dict()
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/prediction/results", summary="列出所有预测结果")
async def list_prediction_results():
    """获取所有预测结果列表"""
    try:
        results = prediction_manager.list_results()
        
        return {
            "success": True,
            "count": len(results),
            "results": results
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/prediction/results/{prediction_id}/download", summary="下载预测结果图像")
async def download_prediction_image(prediction_id: str):
    """
    下载预测结果图像
    
    - **prediction_id**: 预测ID
    """
    try:
        result = prediction_manager.get_result(prediction_id)
        
        if not result.output_image or not Path(result.output_image).exists():
            raise HTTPException(status_code=404, detail="结果图像不存在")
        
        return FileResponse(
            path=result.output_image,
            media_type="image/jpeg",
            filename=f"prediction_{prediction_id}.jpg"
        )
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ==================== 系统信息 API ====================
@app.get("/api/v1/system/info", summary="获取系统信息")
async def get_system_info():
    """获取系统和服务配置信息"""
    import torch
    
    return {
        "success": True,
        "system": {
            "python_version": sys.version,
            "platform": sys.platform,
            "cuda_available": torch.cuda.is_available(),
            "gpu_count": torch.cuda.device_count() if torch.cuda.is_available() else 0,
            "gpu_names": [torch.cuda.get_device_name(i) for i in range(torch.cuda.device_count())] if torch.cuda.is_available() else []
        },
        "config": {
            "yolov7_root": str(config.YOLOV7_ROOT),
            "work_dir": str(config.WORK_DIR),
            "supported_models": config.SUPPORTED_MODELS
        },
        "nacos": {
            "enabled": config.NACOS_ENABLED,
            "server": config.NACOS_SERVER_ADDR,
            "service_name": config.SERVICE_NAME,
            "registered": nacos_registry.is_registered()
        } if config.NACOS_ENABLED else None
    }


# ==================== 优雅关闭处理 ====================
def signal_handler(sig, frame):
    """信号处理器 - 优雅关闭"""
    print("\n[SHUTDOWN] 接收到关闭信号，正在清理...")
    
    # 从 Nacos 注销服务
    if config.NACOS_ENABLED and nacos_registry.is_registered():
        print("[NACOS] 正在从 Nacos 注销服务...")
        nacos_registry.deregister()
    
    print("[SHUTDOWN] 服务已停止")
    sys.exit(0)


# 注册信号处理器
signal.signal(signal.SIGINT, signal_handler)
signal.signal(signal.SIGTERM, signal_handler)


# ==================== 启动服务 ====================
if __name__ == "__main__":
    print("=" * 80)
    print("YOLO Training & Prediction API Service")
    print("=" * 80)
    print(f"API 文档: http://{config.API_HOST}:{config.API_PORT}/docs")
    print(f"ReDoc: http://{config.API_HOST}:{config.API_PORT}/redoc")
    
    # 注册到 Nacos
    if config.NACOS_ENABLED:
        print("-" * 80)
        print("正在注册到 Nacos...")
        if nacos_registry.register():
            print(f"✓ Nacos 注册成功")
            print(f"  服务地址: {nacos_registry.get_service_url()}")
        else:
            print("✗ Nacos 注册失败，但服务将继续启动")
        print("-" * 80)
    
    print("=" * 80)
    
    uvicorn.run(
        app,
        host=config.API_HOST,
        port=config.API_PORT,
        log_level="info"
    )

"""
YOLO 训练和预测服务配置管理
"""
import os
from pathlib import Path
from typing import Optional


class YOLOConfig:
    """YOLO 服务配置类"""
    
    # ==================== 基础路径配置 ====================
    # YOLOv7 源码根目录
    YOLOV7_ROOT = Path(r'E:\doc\system-admin\python\yolov7\yolov7-main')
    
    # 工作目录
    WORK_DIR = Path(r'E:\doc\system-admin\yolo_training_results')
    
    # 数据集默认路径
    DEFAULT_DATASET_YAML = Path(r'E:\doc\system-admin\generated_dataset\data.yaml')
    
    # ==================== API 服务配置 ====================
    # API 服务地址和端口
    API_HOST = "0.0.0.0"
    API_PORT = 8000
    
    # CORS 允许的来源
    CORS_ORIGINS = ["*"]  # 生产环境应设置为具体域名
    
    # ==================== Nacos 服务注册配置 ====================
    NACOS_ENABLED = True  # 是否启用 Nacos 注册
    NACOS_SERVER_ADDR = "192.168.52.108:8848"  # Nacos 服务器地址
    NACOS_NAMESPACE = ""  # Nacos 命名空间ID，空表示public
    NACOS_USERNAME = "nacos"  # Nacos 用户名
    NACOS_PASSWORD = "nacos"  # Nacos 密码
    
    # 服务注册信息
    SERVICE_NAME = "yolo-training-prediction-service"
    SERVICE_GROUP = "DEFAULT_GROUP"
    SERVICE_CLUSTER = "DEFAULT"
    SERVICE_VERSION = "1.0.0"
    SERVICE_WEIGHT = 1.0
    SERVICE_EPHEMERAL = True  # 临时实例
    
    # 健康检查配置
    HEALTH_CHECK_PATH = "/health"
    HEALTH_CHECK_INTERVAL = 5  # 秒
    HEALTH_CHECK_TIMEOUT = 3  # 秒
    
    # ==================== 训练配置 ====================
    # 默认训练参数
    DEFAULT_EPOCHS = 300
    DEFAULT_BATCH_SIZE = 4
    DEFAULT_IMAGE_SIZE = 1280
    DEFAULT_DEVICE = "0"  # "0"=GPU, "cpu"=CPU
    DEFAULT_WEIGHTS = "yolov7x.pt"
    DEFAULT_HYP = "data/hyp.scratch.p5.yaml"
    
    # 训练任务存储
    TRAIN_TASKS_DIR = WORK_DIR / "tasks"
    
    # ==================== 预测配置 ====================
    # 默认预测参数
    DEFAULT_CONF_THRES = 0.25
    DEFAULT_IOU_THRES = 0.45
    DEFAULT_MULTI_LABEL = False
    
    # 预测结果存储
    PREDICTION_RESULTS_DIR = WORK_DIR / "predictions"
    
    # ==================== 模型配置 ====================
    # 支持的模型类型
    SUPPORTED_MODELS = [
        "yolov7.pt",
        "yolov7x.pt",
        "yolov7-tiny.pt",
        "yolov7-e6e.pt",
        "custom_model.pt"
    ]
    
    # 模型存储目录
    MODELS_DIR = WORK_DIR / "models"
    
    # ==================== 文件上传配置 ====================
    # 上传文件临时目录
    UPLOAD_DIR = WORK_DIR / "uploads"
    
    # 最大文件大小 (100MB)
    MAX_UPLOAD_SIZE = 100 * 1024 * 1024
    
    # 允许的文件扩展名
    ALLOWED_IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.tif'}
    ALLOWED_VIDEO_EXTENSIONS = {'.mp4', '.avi', '.mov', '.mkv'}
    ALLOWED_WSI_EXTENSIONS = {'.svs', '.tif', '.tiff'}
    
    # ==================== 日志配置 ====================
    LOG_DIR = WORK_DIR / "logs"
    LOG_LEVEL = "INFO"
    
    # ==================== 缓存配置 ====================
    # 模型缓存
    MODEL_CACHE_ENABLED = True
    MODEL_CACHE_TTL = 3600  # 秒
    
    @classmethod
    def initialize(cls):
        """初始化配置，创建必要的目录"""
        dirs = [
            cls.WORK_DIR,
            cls.TRAIN_TASKS_DIR,
            cls.PREDICTION_RESULTS_DIR,
            cls.MODELS_DIR,
            cls.UPLOAD_DIR,
            cls.LOG_DIR
        ]
        
        for dir_path in dirs:
            dir_path.mkdir(parents=True, exist_ok=True)
        
        print(f"[CONFIG] 工作目录已初始化: {cls.WORK_DIR}")
    
    @classmethod
    def get_train_task_dir(cls, task_id: str) -> Path:
        """获取训练任务目录"""
        task_dir = cls.TRAIN_TASKS_DIR / task_id
        task_dir.mkdir(parents=True, exist_ok=True)
        return task_dir
    
    @classmethod
    def get_prediction_result_dir(cls, prediction_id: str) -> Path:
        """获取预测结果目录"""
        result_dir = cls.PREDICTION_RESULTS_DIR / prediction_id
        result_dir.mkdir(parents=True, exist_ok=True)
        return result_dir
    
    @classmethod
    def validate_device(cls, device: str) -> bool:
        """验证设备配置"""
        if device == "cpu":
            return True
        try:
            int(device)
            return True
        except ValueError:
            return False
    
    @classmethod
    def validate_image_size(cls, size: int) -> bool:
        """验证图像尺寸"""
        return 32 <= size <= 4096 and size % 32 == 0
    
    @classmethod
    def is_allowed_image(cls, filename: str) -> bool:
        """检查是否为允许的图像文件"""
        return Path(filename).suffix.lower() in cls.ALLOWED_IMAGE_EXTENSIONS
    
    @classmethod
    def is_allowed_video(cls, filename: str) -> bool:
        """检查是否为允许的视频文件"""
        return Path(filename).suffix.lower() in cls.ALLOWED_VIDEO_EXTENSIONS
    
    @classmethod
    def is_allowed_wsi(cls, filename: str) -> bool:
        """检查是否为允许的 WSI 文件"""
        return Path(filename).suffix.lower() in cls.ALLOWED_WSI_EXTENSIONS


# 全局配置实例
config = YOLOConfig()

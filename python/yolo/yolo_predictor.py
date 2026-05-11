"""
YOLO 预测服务封装
提供统一的预测接口，支持图像、视频和 WSI 文件
"""
import os
import sys
import json
import uuid
import shutil
from pathlib import Path
from datetime import datetime
from typing import Optional, Dict, Any, List

import torch
import numpy as np
from PIL import Image

from config import config


class PredictionResult:
    """预测结果类"""
    
    def __init__(self, prediction_id: str, input_file: str):
        self.prediction_id = prediction_id
        self.input_file = input_file
        self.result_dir = config.get_prediction_result_dir(prediction_id)
        self.output_image = None
        self.detections = []
        self.inference_time = 0.0
        self.timestamp = datetime.now()
        
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "prediction_id": self.prediction_id,
            "input_file": self.input_file,
            "output_image": str(self.output_image) if self.output_image else None,
            "detections": self.detections,
            "inference_time": self.inference_time,
            "timestamp": self.timestamp.isoformat(),
            "result_dir": str(self.result_dir)
        }
    
    def save_metadata(self):
        """保存预测元数据"""
        metadata_file = self.result_dir / "metadata.json"
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)


class YOLOPredictor:
    """YOLO 预测器"""
    
    def __init__(self, model_path: Optional[str] = None, device: str = "0", 
                 conf_thres: float = 0.25, iou_thres: float = 0.45):
        """
        初始化预测器
        
        Args:
            model_path: 模型权重路径
            device: 设备 (cpu 或 GPU ID)
            conf_thres: 置信度阈值
            iou_thres: IOU 阈值
        """
        self.model_path = model_path
        self.device = device
        self.conf_thres = conf_thres
        self.iou_thres = iou_thres
        
        # 加载模型
        self.model = None
        self.class_names = []
        
        if model_path:
            self.load_model(model_path)
    
    def load_model(self, model_path: str):
        """
        加载 YOLO 模型
        
        Args:
            model_path: 模型权重文件路径
        """
        try:
            print(f"[PREDICT] 加载模型: {model_path}")
            
            # 添加 YOLOv7 路径
            yolov7_root = str(config.YOLOV7_ROOT)
            if yolov7_root not in sys.path:
                sys.path.insert(0, yolov7_root)
            
            from models.experimental import attempt_load
            from utils.torch_utils import select_device
            
            # 选择设备
            device = select_device(self.device)
            
            # 加载模型
            self.model = attempt_load(model_path, map_location=device)
            
            # 获取类别名称
            if hasattr(self.model, 'module'):
                self.class_names = self.model.module.names
            else:
                self.class_names = self.model.names
            
            print(f"[PREDICT] 模型加载成功，类别数: {len(self.class_names)}")
            
        except Exception as e:
            raise RuntimeError(f"模型加载失败: {e}")
    
    def predict_image(self, image_path: str, output_dir: Optional[Path] = None,
                     img_size: int = 640) -> PredictionResult:
        """
        预测单张图像
        
        Args:
            image_path: 图像路径
            output_dir: 输出目录（可选）
            img_size: 输入图像尺寸
            
        Returns:
            预测结果对象
        """
        prediction_id = str(uuid.uuid4())[:8]
        result = PredictionResult(prediction_id, image_path)
        
        if output_dir is None:
            output_dir = result.result_dir
        
        try:
            print(f"[PREDICT] 开始预测: {image_path}")
            
            # 导入必要的模块
            yolov7_root = str(config.YOLOV7_ROOT)
            if yolov7_root not in sys.path:
                sys.path.insert(0, yolov7_root)
            
            from utils.datasets import letterbox
            from utils.general import non_max_suppression, scale_coords
            from utils.plots import plot_one_box
            
            import cv2
            
            # 读取图像
            img0 = cv2.imread(image_path)
            if img0 is None:
                raise ValueError(f"无法读取图像: {image_path}")
            
            # 预处理
            img = letterbox(img0, img_size, stride=32, auto=True)[0]
            img = img[:, :, ::-1].transpose(2, 0, 1)  # BGR to RGB, HWC to CHW
            img = np.ascontiguousarray(img)
            
            # 转换为 tensor
            img_tensor = torch.from_numpy(img).to(self.model.device)
            img_tensor = img_tensor.float() / 255.0
            
            if img_tensor.ndimension() == 3:
                img_tensor = img_tensor.unsqueeze(0)
            
            # 推理
            start_time = datetime.now()
            pred = self.model(img_tensor, augment=False)[0]
            
            # NMS
            pred = non_max_suppression(
                pred, 
                self.conf_thres, 
                self.iou_thres,
                multi_label=False,
                max_det=1000
            )
            
            inference_time = (datetime.now() - start_time).total_seconds()
            result.inference_time = inference_time
            
            # 处理检测结果
            detections = []
            im0 = img0.copy()
            
            for i, det in enumerate(pred):
                if len(det):
                    # 缩放坐标到原图
                    det[:, :4] = scale_coords(
                        img_tensor.shape[2:], det[:, :4], im0.shape
                    ).round()
                    
                    # 提取检测信息
                    for *xyxy, conf, cls in det:
                        x1, y1, x2, y2 = map(int, xyxy)
                        confidence = float(conf)
                        class_id = int(cls)
                        class_name = self.class_names[class_id] if class_id < len(self.class_names) else f"class_{class_id}"
                        
                        detection = {
                            "class_id": class_id,
                            "class_name": class_name,
                            "confidence": confidence,
                            "bbox": [x1, y1, x2, y2]
                        }
                        detections.append(detection)
                        
                        # 绘制边界框
                        label = f"{class_name} {confidence:.2f}"
                        plot_one_box(xyxy, im0, label=label, color=(0, 255, 0), line_thickness=2)
            
            result.detections = detections
            
            # 保存结果图像
            output_image_path = output_dir / f"result_{prediction_id}.jpg"
            cv2.imwrite(str(output_image_path), im0)
            result.output_image = output_image_path
            
            # 保存元数据
            result.save_metadata()
            
            print(f"[PREDICT] 预测完成，检测到 {len(detections)} 个目标")
            
            return result
            
        except Exception as e:
            raise RuntimeError(f"预测失败: {e}")
    
    def predict_batch(self, image_paths: List[str], output_dir: Optional[Path] = None,
                     img_size: int = 640) -> List[PredictionResult]:
        """
        批量预测图像
        
        Args:
            image_paths: 图像路径列表
            output_dir: 输出目录（可选）
            img_size: 输入图像尺寸
            
        Returns:
            预测结果列表
        """
        results = []
        
        for image_path in image_paths:
            try:
                result = self.predict_image(image_path, output_dir, img_size)
                results.append(result)
            except Exception as e:
                print(f"[PREDICT] 预测失败 {image_path}: {e}")
                continue
        
        return results
    
    def get_detections_json(self, result: PredictionResult) -> str:
        """
        获取检测结果的 JSON 格式
        
        Args:
            result: 预测结果对象
            
        Returns:
            JSON 字符串
        """
        return json.dumps(result.to_dict(), indent=2, ensure_ascii=False)
    
    def export_to_coco_format(self, results: List[PredictionResult]) -> Dict:
        """
        导出为 COCO 格式
        
        Args:
            results: 预测结果列表
            
        Returns:
            COCO 格式字典
        """
        coco_format = {
            "images": [],
            "annotations": [],
            "categories": []
        }
        
        # 添加类别信息
        for idx, class_name in enumerate(self.class_names):
            coco_format["categories"].append({
                "id": idx,
                "name": class_name,
                "supercategory": "object"
            })
        
        annotation_id = 0
        
        for result in results:
            # 添加图像信息
            if result.output_image and Path(result.output_image).exists():
                img = Image.open(result.output_image)
                width, height = img.size
                
                coco_format["images"].append({
                    "id": len(coco_format["images"]),
                    "file_name": Path(result.input_file).name,
                    "width": width,
                    "height": height
                })
                
                # 添加标注
                for det in result.detections:
                    x1, y1, x2, y2 = det["bbox"]
                    w = x2 - x1
                    h = y2 - y1
                    
                    coco_format["annotations"].append({
                        "id": annotation_id,
                        "image_id": len(coco_format["images"]) - 1,
                        "category_id": det["class_id"],
                        "bbox": [x1, y1, w, h],
                        "area": w * h,
                        "iscrowd": 0,
                        "confidence": det["confidence"]
                    })
                    annotation_id += 1
        
        return coco_format


class PredictionManager:
    """预测任务管理器"""
    
    def __init__(self):
        self.predictors: Dict[str, YOLOPredictor] = {}
        self.results: Dict[str, PredictionResult] = {}
    
    def create_predictor(self, model_path: str, device: str = "0",
                        conf_thres: float = 0.25, iou_thres: float = 0.45) -> str:
        """
        创建预测器实例
        
        Args:
            model_path: 模型路径
            device: 设备
            conf_thres: 置信度阈值
            iou_thres: IOU 阈值
            
        Returns:
            predictor_id: 预测器ID
        """
        predictor_id = str(uuid.uuid4())[:8]
        
        predictor = YOLOPredictor(
            model_path=model_path,
            device=device,
            conf_thres=conf_thres,
            iou_thres=iou_thres
        )
        
        self.predictors[predictor_id] = predictor
        print(f"[PREDICT] 创建预测器: {predictor_id}")
        
        return predictor_id
    
    def predict(self, predictor_id: str, image_path: str, 
               img_size: int = 640) -> PredictionResult:
        """
        执行预测
        
        Args:
            predictor_id: 预测器ID
            image_path: 图像路径
            img_size: 图像尺寸
            
        Returns:
            预测结果
        """
        if predictor_id not in self.predictors:
            raise ValueError(f"预测器不存在: {predictor_id}")
        
        predictor = self.predictors[predictor_id]
        result = predictor.predict_image(image_path, img_size=img_size)
        
        self.results[result.prediction_id] = result
        
        return result
    
    def get_result(self, prediction_id: str) -> PredictionResult:
        """
        获取预测结果
        
        Args:
            prediction_id: 预测ID
            
        Returns:
            预测结果对象
        """
        if prediction_id not in self.results:
            raise ValueError(f"预测结果不存在: {prediction_id}")
        
        return self.results[prediction_id]
    
    def list_results(self) -> List[Dict]:
        """
        列出所有预测结果
        
        Returns:
            结果列表
        """
        return [r.to_dict() for r in self.results.values()]


# 全局预测管理器实例
prediction_manager = PredictionManager()

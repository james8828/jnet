"""
YOLOv7 病理图像预测程序
支持单图预测、批量预测和实时检测
"""
import argparse
import os
import sys
import time
from pathlib import Path

import cv2
import numpy as np
import torch
import yaml

# 添加YOLOv7路径
YOLOV7_ROOT = Path(__file__).parent.parent / 'yolov7' / 'yolov7-main'
if YOLOV7_ROOT.exists():
    sys.path.insert(0, str(YOLOV7_ROOT))
    print(f"[INFO] YOLOv7路径: {YOLOV7_ROOT}")
else:
    # 备用路径: 直接在predict目录下
    YOLOV7_ROOT = Path(__file__).parent / 'yolov7' / 'yolov7-main'
    if YOLOV7_ROOT.exists():
        sys.path.insert(0, str(YOLOV7_ROOT))
        print(f"[INFO] YOLOv7路径 (备用): {YOLOV7_ROOT}")
    else:
        raise FileNotFoundError(f"无法找到YOLOv7目录，请检查路径配置")

from models.experimental import attempt_load
from utils.datasets import letterbox
from utils.general import check_img_size, non_max_suppression, scale_coords
from utils.plots import plot_one_box
from utils.torch_utils import select_device

# 导入导出工具
try:
    from export_results import export_to_json, export_to_csv, export_statistics
except ImportError:
    print("警告: 无法导入export_results模块，导出功能将不可用")


class YOLOv7Predictor:
    """YOLOv7预测器"""
    
    def __init__(self, weights='best.pt', device='', img_size=640, conf_thres=0.25, iou_thres=0.45, multi_label=False):
        """
        初始化预测器
        
        Args:
            weights: 模型权重文件路径
            device: 设备 (cpu 或 cuda:0)
            img_size: 输入图像尺寸
            conf_thres: 置信度阈值
            iou_thres: NMS IoU阈值
            multi_label: 是否开启多标签模式 (病理图像建议开启)
        """
        self.device = select_device(device)
        self.img_size = img_size
        self.conf_thres = conf_thres
        self.iou_thres = iou_thres
        self.multi_label = multi_label  # 新增属性
        
        # 加载模型
        print(f"正在加载模型: {weights}")
        self.model = attempt_load(weights, map_location=self.device)
        self.stride = int(self.model.stride.max())
        self.img_size = check_img_size(img_size, s=self.stride)
        
        # 获取类别信息
        self.names = self.model.module.names if hasattr(self.model, 'module') else self.model.names
        self.colors = [[np.random.randint(0, 255) for _ in range(3)] for _ in self.names]
        
        # 切换到评估模式
        self.model.eval()
        
        # 半精度推理 (如果可用)
        self.half = self.device.type != 'cpu'
        if self.half:
            self.model.half()
        
        print(f"模型加载完成!")
        print(f"设备: {self.device}")
        print(f"类别: {self.names}")
        print(f"图像尺寸: {self.img_size}")
    
    def preprocess(self, img0):
        """
        图像预处理
        
        Args:
            img0: 原始图像 (BGR格式)
            
        Returns:
            img: 预处理后的图像
            img0: 原始图像
        """
        # Padded resize
        img = letterbox(img0, self.img_size, stride=self.stride)[0]
        
        # Convert
        img = img[:, :, ::-1].transpose(2, 0, 1)  # BGR to RGB, to 3xHxW
        img = np.ascontiguousarray(img)
        
        img = torch.from_numpy(img).to(self.device)
        img = img.half() if self.half else img.float()  # uint8 to fp16/32
        img /= 255.0  # 0 - 255 to 0.0 - 1.0
        
        if img.ndimension() == 3:
            img = img.unsqueeze(0)
        
        return img, img0
    
    def predict(self, image_path, save_result=True, output_dir='predictions'):
        """
        单图预测
        
        Args:
            image_path: 图像路径
            save_result: 是否保存结果
            output_dir: 输出目录
            
        Returns:
            detections: 检测结果列表
        """
        # 读取图像
        img0 = cv2.imread(str(image_path))
        if img0 is None:
            print(f"无法读取图像: {image_path}")
            return None
        
        # 预处理
        img, img0 = self.preprocess(img0)
        
        # 推理
        t1 = time.time()
        with torch.no_grad():
            pred = self.model(img, augment=False)[0]
        
        # NMS
        pred = non_max_suppression(pred, self.conf_thres, self.iou_thres, 
                                   classes=None, agnostic=False, multi_label=self.multi_label)
        t2 = time.time()
        
        # 处理检测结果
        detections = []
        for det in pred:
            if len(det):
                # 缩放坐标到原图
                det[:, :4] = scale_coords(img.shape[2:], det[:, :4], img0.shape).round()
                
                # 解析检测结果
                for *xyxy, conf, cls in reversed(det):
                    detection = {
                        'bbox': [int(x) for x in xyxy],
                        'confidence': float(conf),
                        'class_id': int(cls),
                        'class_name': self.names[int(cls)]
                    }
                    detections.append(detection)
                    
                    # 绘制边界框
                    if save_result:
                        label = f"{detection['class_name']} {conf:.2f}"
                        plot_one_box(xyxy, img0, label=label, 
                                   color=self.colors[int(cls)], line_thickness=2)
        
        inference_time = t2 - t1
        print(f"推理时间: {inference_time:.3f}s")
        print(f"检测到 {len(detections)} 个目标")
        
        # 保存结果
        if save_result and len(detections) > 0:
            output_path = Path(output_dir) / Path(image_path).name
            output_path.parent.mkdir(parents=True, exist_ok=True)
            cv2.imwrite(str(output_path), img0)
            print(f"结果已保存: {output_path}")
        
        return detections
    
    def predict_batch(self, input_dir, output_dir='predictions', pattern='*.jpg'):
        """
        批量预测
        
        Args:
            input_dir: 输入目录
            output_dir: 输出目录
            pattern: 文件匹配模式
            
        Returns:
            results: 所有结果的字典
        """
        input_path = Path(input_dir)
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # 获取所有图像文件
        image_files = list(input_path.glob(pattern))
        if not image_files:
            print(f"未找到匹配的图像文件: {input_dir}/{pattern}")
            return {}
        
        print(f"找到 {len(image_files)} 张图像")
        print("=" * 60)
        
        results = {}
        total_detections = 0
        start_time = time.time()
        
        for i, img_path in enumerate(image_files, 1):
            print(f"\n[{i}/{len(image_files)}] 处理: {img_path.name}")
            detections = self.predict(img_path, save_result=True, output_dir=output_dir)
            
            if detections:
                results[str(img_path)] = detections
                total_detections += len(detections)
        
        elapsed_time = time.time() - start_time
        print("\n" + "=" * 60)
        print(f"批量预测完成!")
        print(f"总图像数: {len(image_files)}")
        print(f"检测到目标总数: {total_detections}")
        print(f"总耗时: {elapsed_time:.2f}s")
        print(f"平均速度: {len(image_files)/elapsed_time:.2f} 张/秒")
        print(f"结果保存在: {output_path}")
        
        return results
    
    def predict_video(self, video_path, output_path=None, save_fps=30):
        """
        视频预测
        
        Args:
            video_path: 视频路径
            output_path: 输出视频路径
            save_fps: 输出视频帧率
        """
        cap = cv2.VideoCapture(str(video_path))
        
        if not cap.isOpened():
            print(f"无法打开视频: {video_path}")
            return
        
        # 获取视频信息
        fps = cap.get(cv2.CAP_PROP_FPS)
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        
        if output_path is None:
            output_path = Path(video_path).parent / f"result_{Path(video_path).name}"
        
        # 创建视频写入器
        fourcc = cv2.VideoWriter_fourcc(*'mp4v')
        out = cv2.VideoWriter(str(output_path), fourcc, save_fps, (width, height))
        
        print(f"视频信息:")
        print(f"  分辨率: {width}x{height}")
        print(f"  帧率: {fps}")
        print(f"  总帧数: {total_frames}")
        print(f"  输出: {output_path}")
        print("=" * 60)
        
        frame_count = 0
        start_time = time.time()
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            frame_count += 1
            
            # 预测
            detections = self.predict(frame, save_result=False)
            
            # 显示帧率和检测数量
            elapsed = time.time() - start_time
            fps_current = frame_count / elapsed if elapsed > 0 else 0
            
            info_text = f"Frame: {frame_count}/{total_frames} | FPS: {fps_current:.1f} | Detections: {len(detections)}"
            cv2.putText(frame, info_text, (10, 30), 
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
            
            # 写入输出视频
            out.write(frame)
            
            # 显示
            cv2.imshow('YOLOv7 Detection', frame)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
        
        cap.release()
        out.release()
        cv2.destroyAllWindows()
        
        print(f"\n视频处理完成!")
        print(f"总帧数: {frame_count}")
        print(f"输出: {output_path}")
    
    def predict_webcam(self, camera_id=0):
        """
        摄像头实时检测
        
        Args:
            camera_id: 摄像头ID
        """
        cap = cv2.VideoCapture(camera_id)
        
        if not cap.isOpened():
            print(f"无法打开摄像头: {camera_id}")
            return
        
        print("启动摄像头实时检测...")
        print("按 'q' 键退出")
        print("=" * 60)
        
        frame_count = 0
        start_time = time.time()
        
        while True:
            ret, frame = cap.read()
            if not ret:
                print("无法读取摄像头帧")
                break
            
            frame_count += 1
            
            # 预测
            detections = self.predict(frame, save_result=False)
            
            # 显示帧率
            elapsed = time.time() - start_time
            fps = frame_count / elapsed if elapsed > 0 else 0
            
            info_text = f"FPS: {fps:.1f} | Detections: {len(detections)}"
            cv2.putText(frame, info_text, (10, 30), 
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
            
            # 显示
            cv2.imshow('YOLOv7 Real-time Detection', frame)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
        
        cap.release()
        cv2.destroyAllWindows()
        print(f"\n实时检测结束")
        print(f"平均FPS: {fps:.2f}")


def main():
    parser = argparse.ArgumentParser(description='YOLOv7 病理图像预测程序')
    parser.add_argument('--weights', type=str, 
                       default='E:/doc/system-admin/yolo_training_results/pathology_yolov7_v2/weights/best.pt',
                       help='模型权重文件路径')
    parser.add_argument('--source', type=str, required=True,
                       help='输入源 (图像/目录/视频/摄像头ID)')
    parser.add_argument('--device', type=str, default='0',
                       help='设备 (cpu 或 cuda:0,1,2,3)')
    parser.add_argument('--img-size', type=int, default=640,
                       help='输入图像尺寸')
    parser.add_argument('--conf-thres', type=float, default=0.25,
                       help='置信度阈值')
    parser.add_argument('--iou-thres', type=float, default=0.45,
                       help='NMS IoU阈值')
    parser.add_argument('--output', type=str, default='predictions',
                       help='输出目录')
    parser.add_argument('--mode', type=str, default='auto',
                       choices=['auto', 'image', 'batch', 'video', 'webcam'],
                       help='预测模式 (auto自动检测)')
    parser.add_argument('--export-json', action='store_true',
                       help='导出JSON格式结果')
    parser.add_argument('--export-csv', action='store_true',
                       help='导出CSV格式结果')
    parser.add_argument('--export-stats', action='store_true',
                       help='导出统计报告')
    
    opt = parser.parse_args()
    
    # 创建预测器
    predictor = YOLOv7Predictor(
        weights=opt.weights,
        device=opt.device,
        img_size=opt.img_size,
        conf_thres=opt.conf_thres,
        iou_thres=opt.iou_thres
    )
    
    source = Path(opt.source)
    
    # 自动检测模式或根据指定模式执行
    if opt.mode == 'auto':
        if source.is_file():
            if source.suffix.lower() in ['.jpg', '.jpeg', '.png', '.bmp']:
                mode = 'image'
            elif source.suffix.lower() in ['.mp4', '.avi', '.mov']:
                mode = 'video'
            else:
                mode = 'image'
        elif source.is_dir():
            mode = 'batch'
        elif source.isnumeric():
            mode = 'webcam'
        else:
            mode = 'image'
    else:
        mode = opt.mode
    
    # 执行预测
    results = {}
    
    if mode == 'image':
        print(f"\n单图预测模式")
        print("=" * 60)
        detections = predictor.predict(source, save_result=True, output_dir=opt.output)
        if detections:
            results[str(source)] = detections
    
    elif mode == 'batch':
        print(f"\n批量预测模式")
        print("=" * 60)
        results = predictor.predict_batch(source, output_dir=opt.output)
    
    elif mode == 'video':
        print(f"\n视频预测模式")
        print("=" * 60)
        output_video = Path(opt.output) / f"result_{source.name}"
        predictor.predict_video(source, output_path=output_video)
    
    elif mode == 'webcam':
        print(f"\n摄像头实时检测模式")
        print("=" * 60)
        predictor.predict_webcam(camera_id=int(opt.source))
    
    # 导出结果
    if results:
        output_path = Path(opt.output)
        output_path.mkdir(parents=True, exist_ok=True)
        
        if opt.export_json:
            export_to_json(results, output_path / 'predictions.json')
        
        if opt.export_csv:
            export_to_csv(results, output_path / 'predictions.csv')
        
        if opt.export_stats:
            export_statistics(results, output_path / 'statistics.txt')


if __name__ == '__main__':
    main()

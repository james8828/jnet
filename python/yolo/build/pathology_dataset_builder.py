import os
import sys
import cv2
import numpy as np
import shutil
import random
import logging
import json
from pathlib import Path
from datetime import datetime
import torch

# ================= 配置日志系统 =================
def setup_logger(log_file=None):
    """配置日志系统，同时输出到控制台和文件"""
    logger = logging.getLogger('AutoLabeler')
    logger.setLevel(logging.DEBUG)
    
    # 创建格式化器
    formatter = logging.Formatter(
        '%(asctime)s [%(levelname)s] %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    
    # 控制台处理器
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)
    
    # 文件处理器（可选）
    if log_file:
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    
    return logger

# 初始化日志
logger = setup_logger()

# ================= 关键：设置 YOLOv7 环境路径 =================
logger.info("="*60)
logger.info("自动标注流程启动")
logger.info("="*60)

# 必须确保能导入 models 和 utils
YOLOV7_ROOT = Path(__file__).parent / 'yolov7' / 'yolov7-main'
if YOLOV7_ROOT.exists():
    sys.path.insert(0, str(YOLOV7_ROOT))
    logger.info(f"YOLOv7 路径已添加: {YOLOV7_ROOT}")
else:
    # 尝试备用路径，根据你的实际结构调整
    YOLOV7_ROOT = Path('E:/doc/system-admin/python/yolov7/yolov7-main')
    if YOLOV7_ROOT.exists():
        sys.path.insert(0, str(YOLOV7_ROOT))
        logger.info(f"YOLOv7 路径已添加 (备用): {YOLOV7_ROOT}")
    else:
        logger.error("无法找到 YOLOv7 源代码，请检查 YOLOV7_ROOT 路径")
        raise FileNotFoundError("Cannot find YOLOv7 source code. Please check YOLOV7_ROOT path.")

# 现在可以安全导入 YOLOv7 模块
from models.experimental import attempt_load
from utils.general import check_img_size, non_max_suppression, scale_coords
from utils.torch_utils import select_device
from utils.datasets import letterbox

# 尝试导入 openslide 用于处理 SVS 文件
try:
    import openslide
    OPENSIDE_AVAILABLE = True
    logger.info("✓ OpenSlide 库已加载，支持 SVS 格式")
except ImportError as e:
    OPENSIDE_AVAILABLE = False
    logger.warning("⚠ OpenSlide 库未安装，将不支持 SVS 格式")
    logger.warning(f"  错误详情: {e}")
    logger.warning("  安装方法:")
    logger.warning("  1. Windows: pip install openslide-python")
    logger.warning("  2. 需要先安装 OpenSlide 二进制文件: https://openslide.org/download/")
    logger.warning("  3. 或者使用 conda: conda install -c conda-forge openslide-python")

# ================= 配置区域 =================
CONFIG = {
    # 【重要】请替换为那个产生 results_old (高置信度) 的模型路径！
    # 不要使用当前这个表现差的 0R_WT_YOLO_Box_A1.0.pt
    'model_weights': 'E:/doc/system-admin/python/model/0R_WT_YOLO_Box_A1.0.pt', 
    
    'device': '0',                  # GPU ID
    
    # 数据路径配置
    # 支持单个路径字符串或多个路径的列表
    # 示例1 (单个目录): 'raw_image_dir': 'E:/doc/system-admin/python/raw_unlabeled_data'
    # 示例2 (多个目录): 'raw_image_dir': ['E:/data/svs_batch1', 'E:/data/svs_batch2', 'E:/data/svs_batch3']
    'raw_image_dir': [
        'E:/doc/system-admin/python/raw_unlabeled_data',
        'D:/work/WSI/slide_data'
        # 在此添加更多SVS切片目录
        # 'E:/data/svs_batch_001',
        # 'E:/data/svs_batch_002',
    ], 
    'output_dataset_dir': 'E:/doc/system-admin/generated_dataset', 
    
    # 输出路径配置
    'output_svs_images_dir': 'E:/doc/system-admin/output/svs_images',       # SVS 最低分辨率 JPG 图像
    'output_geojson_dir': 'E:/doc/system-admin/output/geojson',             # GeoJSON 预测结果
    'output_visualization_dir': 'E:/doc/system-admin/output/visualization', # 可视化标注图像
    
    # 预测参数
    'img_size': 640,
    'conf_thres': 0.25,             # 推理阈值
    'iou_thres': 0.45,              # NMS 阈值
    
    # 伪标签筛选参数
    # 由于旧模型置信度高，这里可以设高一点，保证质量
    'pseudo_label_conf_thres': 0.5, # 只有 > 0.5 的才保留
    'min_box_area_ratio': 0.0001,   
    
    'train_ratio': 0.8,             
    'seed': 42,
    
    # 抽样策略配置
    'use_stratified_split': True,   # 是否使用分层抽样（强烈推荐）
    
    # 类别过滤参数
    'min_samples_per_class': 5,    # 每个类别最少样本数，低于此值的类别将被排除
    
    # SVS 图像处理参数
    'svs_level_downsample': None,   # None 表示使用最低分辨率（最大下采样倍数），或指定具体倍数如 16, 32
    
    # 可视化参数
    'box_thickness': 2,             # 标注框线宽
    'font_scale': 0.5,              # 文字大小
    'show_confidence': True,        # 是否显示置信度
}

class AutoLabeler:
    def __init__(self, config):
        self.config = config
        self.device = select_device(config['device'])
        
        logger.info(f"使用设备: {self.device}")
        logger.info(f"模型权重路径: {config['model_weights']}")
        
        try:
            # 使用与 predict.py 相同的加载方式
            logger.info("正在加载 YOLOv7 模型...")
            self.model = attempt_load(config['model_weights'], map_location=self.device)
            self.stride = int(self.model.stride.max())
            self.img_size = check_img_size(config['img_size'], s=self.stride)
            
            # 获取类别名称
            self.names = self.model.module.names if hasattr(self.model, 'module') else self.model.names
            logger.info(f"模型加载成功！类别数量: {len(self.names)}, 类别名称: {self.names}")
            logger.info(f"模型步长: {self.stride}, 输入尺寸: {self.img_size}")
            
            self.model.eval()
            # 半精度
            self.half = self.device.type != 'cpu'
            if self.half:
                self.model.half()
                logger.info("启用半精度推理 (FP16)")
            else:
                logger.info("使用全精度推理 (FP32)")
                
        except Exception as e:
            logger.error(f"模型加载失败: {e}")
            raise e

    def load_svs_image(self, svs_path):
        """
        读取 SVS 文件的最低分辨率图像
        
        Args:
            svs_path: SVS 文件路径
            
        Returns:
            tuple: (numpy.ndarray 图像数组, dict 元数据), 如果失败返回 (None, None)
        """
        if not OPENSIDE_AVAILABLE:
            logger.error(f"OpenSlide 未安装，无法读取 SVS 文件: {svs_path}")
            return None, None
        
        try:
            logger.debug(f"正在打开 SVS 文件: {svs_path}")
            slide = openslide.OpenSlide(str(svs_path))
            
            # 获取层级信息
            level_count = slide.level_count
            logger.debug(f"SVS 文件层级数量: {level_count}")
            
            # 显示所有层级的下采样倍数和尺寸
            for level in range(level_count):
                downsample = slide.level_downsamples[level]
                dimensions = slide.level_dimensions[level]
                logger.debug(f"  层级 {level}: 下采样倍数={downsample:.2f}, 尺寸={dimensions[0]}x{dimensions[1]}")
            # 确定要使用的层级
            if self.config['svs_level_downsample'] is None:
                # 使用最低分辨率（最高层级）
                target_level = level_count - 1
                logger.info(f"使用最低分辨率层级: {target_level} (共 {level_count} 个层级)")
            else:
                # 查找最接近指定下采样倍数的层级
                target_downsample = self.config['svs_level_downsample']
                target_level = 0
                min_diff = float('inf')
                for level in range(level_count):
                    diff = abs(slide.level_downsamples[level] - target_downsample)
                    if diff < min_diff:
                        min_diff = diff
                        target_level = level
                logger.info(f"使用层级 {target_level} (目标下采样倍数: {target_downsample}, 实际: {slide.level_downsamples[target_level]:.2f})")
            
            # 获取该层级的尺寸
            level_dimensions = slide.level_dimensions[target_level]
            actual_downsample = slide.level_downsamples[target_level]
            logger.info(f"读取图像尺寸: {level_dimensions[0]}x{level_dimensions[1]}, 下采样倍数: {actual_downsample:.2f}")
            
            # 读取整个层级的图像
            img = slide.read_region((0, 0), target_level, level_dimensions)
            img = np.array(img)[:, :, :3]  # 去除 alpha 通道
            img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)  # 转换为 BGR
            
            logger.info(f"SVS 图像加载成功，形状: {img.shape}")
            
            # 构建元数据
            metadata = {
                'source_file': str(svs_path),
                'level': target_level,
                'downsample': float(actual_downsample),
                'width': int(level_dimensions[0]),
                'height': int(level_dimensions[1]),
                'level_count': level_count,
                'all_levels': []
            }
            
            # 记录所有层级信息
            for level in range(level_count):
                metadata['all_levels'].append({
                    'level': level,
                    'downsample': float(slide.level_downsamples[level]),
                    'dimensions': list(slide.level_dimensions[level])
                })
            
            slide.close()
            
            return img, metadata
            
        except Exception as e:
            logger.error(f"读取 SVS 文件失败 [{svs_path}]: {e}")
            import traceback
            logger.debug(traceback.format_exc())
            return None, None

    def predict_image(self, img_path):
        """
        单张图片推理，返回归一化的 YOLO 格式检测结果
        支持 JPG、PNG 和 SVS 格式
        
        Args:
            img_path: 图片路径 (支持 .jpg, .png, .svs)
            
        Returns:
            tuple: (检测结果列表, 图像数组, 元数据字典)
                   检测结果列表包含 class, x, y, w, h, conf, name
                   如果是 SVS 文件，还会返回图像和元数据
        """
        img_path = Path(img_path)
        logger.debug(f"开始处理图像: {img_path.name}")
        
        metadata = None
        
        # 根据文件扩展名选择加载方式
        if img_path.suffix.lower() == '.svs':
            logger.info(f"检测到 SVS 格式文件: {img_path.name}")
            orig_img, metadata = self.load_svs_image(img_path)
            if orig_img is None:
                logger.warning(f"SVS 图像加载失败，跳过: {img_path.name}")
                return [], None, None
        else:
            # 传统图像格式
            orig_img = cv2.imread(str(img_path))
            if orig_img is None:
                logger.warning(f"图像读取失败，跳过: {img_path.name}")
                return [], None, None
        
        h_orig, w_orig = orig_img.shape[:2]
        logger.debug(f"原始图像尺寸: {w_orig}x{h_orig}")
        
        # 预处理 (复用 predict.py 的逻辑)
        img = letterbox(orig_img, self.img_size, stride=self.stride)[0]
        img = img[:, :, ::-1].transpose(2, 0, 1)  # BGR to RGB, HWC to CHW
        img = np.ascontiguousarray(img)
        
        img = torch.from_numpy(img).to(self.device)
        img = img.half() if self.half else img.float()
        img /= 255.0
        
        if img.ndimension() == 3:
            img = img.unsqueeze(0)

        # 推理
        with torch.no_grad():
            pred = self.model(img, augment=False)[0]
        
        # NMS
        det = non_max_suppression(pred, self.config['conf_thres'], self.config['iou_thres'])[0]
        
        if len(det) == 0:
            logger.debug(f"未检测到任何目标: {img_path.name}")
            return [], orig_img, metadata

        logger.debug(f"NMS 后检测到 {len(det)} 个目标")

        # 缩放坐标回原图
        det[:, :4] = scale_coords(img.shape[2:], det[:, :4], orig_img.shape).round()

        results = []
        filtered_count = 0
        for *xyxy, conf, cls in reversed(det):
            conf_val = conf.item()
            cls_val = int(cls.item())
            
            # 1. 过滤低置信度伪标签
            if conf_val < self.config['pseudo_label_conf_thres']:
                filtered_count += 1
                continue
                
            # 2. 坐标转换: xyxy (pixel) -> xywh (normalized)
            x1, y1, x2, y2 = xyxy
            w_box = x2 - x1
            h_box = y2 - y1
            
            # 3. 过滤极小框
            area_ratio = (w_box * h_box) / (w_orig * h_orig)
            if area_ratio < self.config['min_box_area_ratio']:
                filtered_count += 1
                continue

            # 归一化中心点坐标
            cx = ((x1 + x2) / 2) / w_orig
            cy = ((y1 + y2) / 2) / h_orig
            nw = w_box / w_orig
            nh = h_box / h_orig
            
            results.append({
                'class': cls_val,
                'x': cx,
                'y': cy,
                'w': nw,
                'h': nh,
                'conf': conf_val,
                'name': self.names[cls_val],  # 记录名称以便检查
                'x1': float(x1),
                'y1': float(y1),
                'x2': float(x2),
                'y2': float(y2)
            })
        
        if filtered_count > 0:
            logger.debug(f"过滤掉 {filtered_count} 个低质量检测框")
        
        logger.debug(f"最终保留 {len(results)} 个有效检测结果")
        return results, orig_img, metadata

    def save_svs_image(self, img, img_path, output_dir):
        """
        保存 SVS 最低分辨率图像为 JPG
        
        Args:
            img: 图像数组
            img_path: 原始文件路径
            output_dir: 输出目录
        """
        if img is None:
            return None
        
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # 生成输出文件名
        output_filename = img_path.stem + '.jpg'
        output_path = output_dir / output_filename
        
        # 保存为 JPG，质量 95
        cv2.imwrite(str(output_path), img, [cv2.IMWRITE_JPEG_QUALITY, 95])
        logger.debug(f"已保存 SVS 图像: {output_path}")
        
        return output_path

    def save_geojson(self, detections, img_path, metadata, output_dir):
        """
        保存预测结果为 GeoJSON 格式（参照病理标注系统格式）
        
        Args:
            detections: 检测结果列表
            img_path: 原始文件路径
            metadata: 图像元数据（SVS 文件有，普通图像为 None）
            output_dir: 输出目录
        """
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # 生成输出文件名
        output_filename = img_path.stem + '.geojson'
        output_path = output_dir / output_filename
        
        # 安全转换浮点数，处理 NaN 和 Inf
        def safe_float(value, default=0.0):
            try:
                if value is None or (isinstance(value, float) and (np.isnan(value) or np.isinf(value))):
                    return default
                return float(value)
            except:
                return default
        
        # 构建 GeoJSON 结构（参照病理标注系统格式）
        geojson = {
            'type': 'FeatureCollection',
            'features': []
        }
        
        # 添加每个检测框作为 Feature
        for i, det in enumerate(detections):
            # 获取像素坐标
            x1 = int(safe_float(det['x1']))
            y1 = int(safe_float(det['y1']))
            x2 = int(safe_float(det['x2']))
            y2 = int(safe_float(det['y2']))
            
            # 计算面积（像素平方）
            area = abs((x2 - x1) * (y2 - y1))
            
            feature = {
                'type': 'Feature',
                'id': f"ai_{det['name']}_{i}_{int(datetime.now().timestamp() * 1000)}",
                'properties': {
                    'annotation_owner': '0',
                    'annotation_type': 'ai',
                    'create_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                    'data_indicators': {
                        det['name'] + '_area': {
                            'value': float(area),
                            'unit': 'pix^2',
                            'name': det['name'] + '_面积'
                        }
                    },
                    'label_name': det['name'],
                    'label_color': 'rgba(255,255,0,1)',
                    'label_code': str(int(det['class'])),
                    'measure_type': '',
                    'measure_relation': '',
                    'measure_name': '',
                    'measure_number': '',
                    'cell_type': 'cell'
                },
                'geometry': {
                    'type': 'Polygon',
                    'coordinates': [[
                        [x1, y1],
                        [x2, y1],
                        [x2, y2],
                        [x1, y2],
                        [x1, y1]
                    ]]
                }
            }
            geojson['features'].append(feature)
        
        # 保存为 JSON 文件
        try:
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(geojson, f, ensure_ascii=False, indent=2)
                f.flush()  # 确保数据完全写入
                os.fsync(f.fileno())  # 强制写入磁盘
            
            # 验证文件完整性
            file_size = output_path.stat().st_size
            logger.debug(f"已保存 GeoJSON: {output_path} ({len(detections)} 个检测框, 文件大小: {file_size/1024:.2f} KB)")
            
            # 尝试验证 JSON 格式
            with open(output_path, 'r', encoding='utf-8') as f:
                verify_data = json.load(f)
                if verify_data['type'] != 'FeatureCollection':
                    logger.warning(f"GeoJSON 格式验证失败: {output_path}")
                else:
                    logger.debug(f"GeoJSON 格式验证通过")
            
            return output_path
        except Exception as e:
            logger.error(f"保存 GeoJSON 失败 [{output_path}]: {e}")
            import traceback
            logger.debug(traceback.format_exc())
            return None

    def save_visualization(self, img, detections, img_path, output_dir):
        """
        保存带标注框的可视化图像
        
        Args:
            img: 图像数组
            detections: 检测结果列表
            img_path: 原始文件路径
            output_dir: 输出目录
        """
        if img is None or len(detections) == 0:
            return None
        
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # 生成输出文件名
        output_filename = img_path.stem + '_vis.jpg'
        output_path = output_dir / output_filename
        
        # 复制图像用于绘制
        vis_img = img.copy()
        
        # 定义颜色映射（为每个类别分配不同颜色）
        colors = [
            (255, 0, 0),     # 红色
            (0, 255, 0),     # 绿色
            (0, 0, 255),     # 蓝色
            (255, 255, 0),   # 黄色
            (255, 0, 255),   # 紫色
            (0, 255, 255),   # 青色
            (128, 0, 0),     # 深红
            (0, 128, 0),     # 深绿
            (0, 0, 128),     # 深蓝
            (128, 128, 0),   # 橄榄色
        ]
        
        # 绘制每个检测框
        for det in detections:
            x1, y1, x2, y2 = int(det['x1']), int(det['y1']), int(det['x2']), int(det['y2'])
            cls_id = det['class']
            conf = det['conf']
            name = det['name']
            
            # 选择颜色
            color = colors[cls_id % len(colors)]
            
            # 绘制矩形框
            cv2.rectangle(vis_img, (x1, y1), (x2, y2), color, self.config['box_thickness'])
            
            # 构建标签文本
            if self.config['show_confidence']:
                label = f"{name}: {conf:.2f}"
            else:
                label = name
            
            # 计算文本大小
            (text_width, text_height), baseline = cv2.getTextSize(
                label, cv2.FONT_HERSHEY_SIMPLEX, self.config['font_scale'], 1
            )
            
            # 绘制文本背景
            cv2.rectangle(vis_img, (x1, y1 - text_height - baseline - 5), 
                         (x1 + text_width, y1), color, -1)
            
            # 绘制文本
            cv2.putText(vis_img, label, (x1, y1 - baseline - 2),
                       cv2.FONT_HERSHEY_SIMPLEX, self.config['font_scale'], 
                       (255, 255, 255), 1)
        
        # 保存可视化图像
        cv2.imwrite(str(output_path), vis_img, [cv2.IMWRITE_JPEG_QUALITY, 95])
        logger.debug(f"已保存可视化图像: {output_path}")
        
        return output_path

    def generate_data_yaml(self, out_dir, train_images_dir, val_images_dir, detected_classes=None):
        """
        生成 YOLO 训练所需的 data.yaml 配置文件
        
        Args:
            out_dir: 输出根目录
            train_images_dir: 训练集图像目录
            val_images_dir: 验证集图像目录
            detected_classes: 实际检测到的类别 ID 集合（可选），如果为 None 则使用模型所有类别
        """
        logger.info("-"*60)
        logger.info("生成 data.yaml 配置文件")
        logger.info("-"*60)
        
        # 确定要使用的类别
        if detected_classes and len(detected_classes) > 0:
            # 使用实际检测到的类别（已过滤）
            class_ids = sorted(list(detected_classes))
            class_names = {cls_id: self.names[cls_id] for cls_id in class_ids}
            num_classes = len(class_ids)
            logger.info(f"从预测结果中获取类别（已应用样本数量过滤）")
        else:
            # 使用模型所有类别
            class_ids = list(range(len(self.names)))
            class_names = {i: name for i, name in enumerate(self.names)}
            num_classes = len(self.names)
            logger.info(f"使用模型所有类别")
        
        logger.info(f"类别数量: {num_classes}")
        logger.info(f"类别列表:")
        for cls_id in sorted(class_names.keys()):
            logger.info(f"  {cls_id}: {class_names[cls_id]}")
        
        # 构建 YAML 内容
        yaml_content = f"# YOLOv7 数据集配置\n"
        yaml_content += f"# 自动生成于: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n"
        yaml_content += f"train: {train_images_dir}\n"
        yaml_content += f"val: {val_images_dir}\n\n"
        yaml_content += f"nc: {num_classes}\n"
        yaml_content += f"names:\n"
        
        for cls_id in sorted(class_names.keys()):
            yaml_content += f"  {cls_id}: {class_names[cls_id]}\n"
        
        # 保存文件
        yaml_path = out_dir / 'data.yaml'
        try:
            with open(yaml_path, 'w', encoding='utf-8') as f:
                f.write(yaml_content)
            
            logger.info(f"✓ data.yaml 已保存: {yaml_path}")
            logger.info(f"  - 训练集路径: {train_images_dir}")
            logger.info(f"  - 验证集路径: {val_images_dir}")
            logger.info(f"  - 类别数量: {num_classes}")
            
            return yaml_path
        except Exception as e:
            logger.error(f"保存 data.yaml 失败: {e}")
            import traceback
            logger.debug(traceback.format_exc())
            return None

    def _stratified_split(self, image_paths):
        """
        按类别进行分层抽样，确保训练集和验证集的类别分布一致
        
        Args:
            image_paths: 所有图像路径列表
            
        Returns:
            tuple: (train_paths, val_paths)
        """
        logger.info("步骤 1/3: 预扫描所有图像以获取类别分布...")
        
        # 用于存储每个图像的类别信息 {img_path: set(class_ids)}
        image_classes = {}
        # 用于存储每个类别包含的图像 {class_id: [img_paths]}
        class_to_images = {}
        
        total_images = len(image_paths)
        processed = 0
        
        for img_path in image_paths:
            try:
                # 快速推理获取类别（不保存结果）
                detections, _, _ = self.predict_image(str(img_path))
                
                if detections:
                    # 提取该图像中出现的所有类别
                    classes_in_image = set(det['class'] for det in detections)
                    image_classes[img_path] = classes_in_image
                    
                    # 将图像添加到对应类别的列表中
                    for cls_id in classes_in_image:
                        if cls_id not in class_to_images:
                            class_to_images[cls_id] = []
                        class_to_images[cls_id].append(img_path)
                else:
                    # 没有检测结果的图像，标记为无类别
                    image_classes[img_path] = set()
                
                processed += 1
                if processed % 20 == 0 or processed == total_images:
                    progress = processed / total_images * 100
                    logger.info(f"  预扫描进度: {processed}/{total_images} ({progress:.1f}%)")
                    
            except Exception as e:
                logger.warning(f"  预扫描失败 [{img_path.name}]: {e}")
                image_classes[img_path] = set()
        
        logger.info(f"预扫描完成！")
        logger.info(f"  - 有检测结果的图像: {sum(1 for v in image_classes.values() if v)}")
        logger.info(f"  - 无检测结果的图像: {sum(1 for v in image_classes.values() if not v)}")
        logger.info(f"  - 检测到的类别数: {len(class_to_images)}")
        logger.info("")
        
        # 显示每个类别的图像数量
        logger.info("各类别图像数量统计:")
        for cls_id in sorted(class_to_images.keys()):
            class_name = self.names[cls_id]
            count = len(class_to_images[cls_id])
            logger.info(f"  类别 {cls_id} ({class_name}): {count} 张图像")
        logger.info("")
        
        # 步骤 2: 对每个类别分别进行随机分割
        logger.info("步骤 2/3: 对每个类别执行分层分割...")
        
        train_set = set()
        val_set = set()
        
        for cls_id in sorted(class_to_images.keys()):
            class_name = self.names[cls_id]
            images_for_class = class_to_images[cls_id].copy()
            
            # 对该类别的图像进行随机打乱
            random.shuffle(images_for_class)
            
            # 计算分割点
            n_total = len(images_for_class)
            n_train = max(1, int(n_total * self.config['train_ratio']))  # 至少保证1张
            n_val = n_total - n_train
            
            # 分割
            train_subset = set(images_for_class[:n_train])
            val_subset = set(images_for_class[n_train:])
            
            train_set.update(train_subset)
            val_set.update(val_subset)
            
            logger.debug(f"  类别 {cls_id} ({class_name}): 总计={n_total}, 训练集={n_train}, 验证集={n_val}")
        
        logger.info(f"分层分割完成！")
        logger.info(f"  - 训练集图像数: {len(train_set)}")
        logger.info(f"  - 验证集图像数: {len(val_set)}")
        logger.info("")
        
        # 步骤 3: 处理未出现在任何类别中的图像（无检测结果）
        logger.info("步骤 3/3: 分配无检测结果的图像...")
        
        no_detection_images = [path for path in image_paths if not image_classes.get(path)]
        
        if no_detection_images:
            random.shuffle(no_detection_images)
            n_no_det_total = len(no_detection_images)
            n_no_det_train = int(n_no_det_total * self.config['train_ratio'])
            
            no_det_train = no_detection_images[:n_no_det_train]
            no_det_val = no_detection_images[n_no_det_train:]
            
            train_set.update(no_det_train)
            val_set.update(no_det_val)
            
            logger.info(f"  无检测结果图像: 总计={n_no_det_total}, 训练集={len(no_det_train)}, 验证集={len(no_det_val)}")
        else:
            logger.info("  无检测结果图像: 0")
        
        logger.info("")
        
        # 转换为列表并排序（保证可复现性）
        train_paths = sorted(list(train_set))
        val_paths = sorted(list(val_set))
        
        # 最终验证
        logger.info("="*60)
        logger.info("分层抽样结果验证")
        logger.info("="*60)
        
        # 验证是否有遗漏
        all_assigned = train_set | val_set
        all_original = set(image_paths)
        
        missing = all_original - all_assigned
        extra = all_assigned - all_original
        
        if missing:
            logger.warning(f"⚠ 警告: 有 {len(missing)} 张图像未被分配!")
            for m in list(missing)[:5]:
                logger.warning(f"    - {m.name}")
        
        if extra:
            logger.error(f"❌ 错误: 有 {len(extra)} 张额外图像!")
        
        if not missing and not extra:
            logger.info("✓ 所有图像均已正确分配")
        
        logger.info(f"")
        logger.info(f"最终划分:")
        logger.info(f"  - 训练集: {len(train_paths)} 张图像")
        logger.info(f"  - 验证集: {len(val_paths)} 张图像")
        logger.info(f"  - 总计: {len(train_paths) + len(val_paths)} 张图像")
        logger.info("="*60)
        logger.info("")
        
        return train_paths, val_paths, image_classes, class_to_images

    def _generate_visualization_report(self, image_classes, class_to_images, 
                                       train_paths, val_paths, out_dir):
        """
        生成可视化的HTML报告，展示类别分布和抽样结果
        
        Args:
            image_classes: {img_path: set(class_ids)} 每个图像的类别
            class_to_images: {class_id: [img_paths]} 每个类别的图像列表
            train_paths: 训练集图像路径列表
            val_paths: 验证集图像路径列表
            out_dir: 输出目录
        """
        logger.info("="*60)
        logger.info("生成可视化报告...")
        logger.info("="*60)
        
        report_dir = out_dir / 'report'
        report_dir.mkdir(parents=True, exist_ok=True)
        
        # 准备数据
        total_images = len(train_paths) + len(val_paths)
        train_set = set(train_paths)
        val_set = set(val_paths)
        
        # 构建类别详细统计
        class_stats = []
        for cls_id in sorted(class_to_images.keys()):
            class_name = self.names[cls_id]
            all_images = class_to_images[cls_id]
            n_total = len(all_images)
            
            # 统计训练集和验证集中的数量
            n_train = sum(1 for img in all_images if img in train_set)
            n_val = sum(1 for img in all_images if img in val_set)
            
            # 计算比例
            train_ratio = (n_train / n_total * 100) if n_total > 0 else 0
            val_ratio = (n_val / n_total * 100) if n_total > 0 else 0
            
            class_stats.append({
                'class_id': cls_id,
                'class_name': class_name,
                'total': n_total,
                'train': n_train,
                'val': n_val,
                'train_ratio': train_ratio,
                'val_ratio': val_ratio
            })
        
        # 生成HTML报告
        html_content = self._build_html_report(class_stats, total_images, 
                                               len(train_paths), len(val_paths),
                                               image_classes, train_set, val_set)
        
        # 保存HTML文件
        report_path = report_dir / 'class_distribution_report.html'
        try:
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(html_content)
            
            logger.info(f"✓ 可视化报告已生成: {report_path}")
            logger.info(f"  可在浏览器中打开查看完整统计信息")
            return report_path
            
        except Exception as e:
            logger.error(f"生成报告失败: {e}")
            import traceback
            logger.debug(traceback.format_exc())
            return None
    
    def _build_html_report(self, class_stats, total_images, n_train, n_val, 
                          image_classes, train_set, val_set):
        """
        构建HTML报告内容
        """
        # 计算总体统计
        n_no_detection = sum(1 for classes in image_classes.values() if not classes)
        n_with_detection = total_images - n_no_detection
        
        # 生成类别表格行
        table_rows = ""
        for stat in class_stats:
            # 根据样本数量设置颜色
            if stat['total'] < 10:
                row_color = "#ffebee"  # 红色背景（样本不足）
            elif stat['total'] < 30:
                row_color = "#fff3e0"  # 橙色背景（样本较少）
            else:
                row_color = "#e8f5e9"  # 绿色背景（样本充足）
            
            table_rows += f"""
            <tr style="background-color: {row_color};">
                <td>{stat['class_id']}</td>
                <td><strong>{stat['class_name']}</strong></td>
                <td>{stat['total']}</td>
                <td>{stat['train']} ({stat['train_ratio']:.1f}%)</td>
                <td>{stat['val']} ({stat['val_ratio']:.1f}%)</td>
                <td>
                    <div style="display: flex; gap: 2px;">
                        <div style="flex: {stat['train_ratio']}; background: #4CAF50; height: 20px;" 
                             title="训练集: {stat['train_ratio']:.1f}%"></div>
                        <div style="flex: {stat['val_ratio']}; background: #2196F3; height: 20px;" 
                             title="验证集: {stat['val_ratio']:.1f}%"></div>
                    </div>
                </td>
            </tr>
            """
        
        # 生成图像-类别映射表（前50张）
        sample_images = list(image_classes.items())[:50]
        image_mapping_rows = ""
        for img_path, classes in sample_images:
            img_name = Path(img_path).name
            if classes:
                class_labels = ", ".join([f"{self.names[cls]}({cls})" for cls in sorted(classes)])
                status = "<span style='color: green;'>✓ 有标注</span>"
            else:
                class_labels = "-"
                status = "<span style='color: gray;'>⊘ 无标注</span>"
            
            # 判断在哪个集合
            if img_path in train_set:
                location = "训练集"
            elif img_path in val_set:
                location = "验证集"
            else:
                location = "未知"
            
            image_mapping_rows += f"""
            <tr>
                <td style="font-family: monospace; font-size: 12px;">{img_name}</td>
                <td>{class_labels}</td>
                <td>{status}</td>
                <td><span style='color: #667eea; font-weight: bold;'>{location}</span></td>
            </tr>
            """
        
        # 完整的HTML模板
        html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>病理图像数据集 - 类别分布报告</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        
        body {{
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }}
        
        .container {{
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }}
        
        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }}
        
        .header h1 {{
            font-size: 32px;
            margin-bottom: 10px;
        }}
        
        .header p {{
            font-size: 16px;
            opacity: 0.9;
        }}
        
        .content {{
            padding: 40px;
        }}
        
        .summary-cards {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }}
        
        .card {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }}
        
        .card h3 {{
            font-size: 14px;
            opacity: 0.9;
            margin-bottom: 10px;
        }}
        
        .card .value {{
            font-size: 36px;
            font-weight: bold;
        }}
        
        .section {{
            margin-bottom: 40px;
        }}
        
        .section h2 {{
            color: #333;
            font-size: 24px;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 3px solid #667eea;
        }}
        
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        
        th {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px;
            text-align: left;
            font-weight: 600;
        }}
        
        td {{
            padding: 12px 15px;
            border-bottom: 1px solid #eee;
        }}
        
        tr:hover {{
            background-color: #f5f5f5;
        }}
        
        .legend {{
            display: flex;
            gap: 20px;
            margin-top: 15px;
            padding: 15px;
            background: #f9f9f9;
            border-radius: 8px;
        }}
        
        .legend-item {{
            display: flex;
            align-items: center;
            gap: 8px;
        }}
        
        .legend-color {{
            width: 20px;
            height: 20px;
            border-radius: 4px;
        }}
        
        .footer {{
            text-align: center;
            padding: 20px;
            color: #666;
            font-size: 14px;
            border-top: 1px solid #eee;
        }}
        
        .warning {{
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }}
        
        .info {{
            background: #d1ecf1;
            border-left: 4px solid #17a2b8;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔬 病理图像数据集分析报告</h1>
            <p>自动生成于 {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>
        
        <div class="content">
            <!-- 总体统计卡片 -->
            <div class="summary-cards">
                <div class="card">
                    <h3>总图像数</h3>
                    <div class="value">{total_images}</div>
                </div>
                <div class="card">
                    <h3>训练集</h3>
                    <div class="value">{n_train}</div>
                </div>
                <div class="card">
                    <h3>验证集</h3>
                    <div class="value">{n_val}</div>
                </div>
                <div class="card">
                    <h3>类别数量</h3>
                    <div class="value">{len(class_stats)}</div>
                </div>
            </div>
            
            <!-- 检测统计 -->
            <div class="info">
                <strong>📊 检测统计:</strong><br>
                • 有检测结果的图像: {n_with_detection} 张 ({n_with_detection/total_images*100:.1f}%)<br>
                • 无检测结果的图像: {n_no_detection} 张 ({n_no_detection/total_images*100:.1f}%)<br>
                • 训练集比例: {n_train/total_images*100:.1f}% | 验证集比例: {n_val/total_images*100:.1f}%
            </div>
            
            <!-- 类别分布表 -->
            <div class="section">
                <h2>📋 各类别详细统计</h2>
                
                <div class="legend">
                    <div class="legend-item">
                        <div class="legend-color" style="background: #4CAF50;"></div>
                        <span>训练集</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #2196F3;"></div>
                        <span>验证集</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #ffebee;"></div>
                        <span>样本不足 (&lt;10)</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #fff3e0;"></div>
                        <span>样本较少 (10-30)</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #e8f5e9;"></div>
                        <span>样本充足 (&gt;30)</span>
                    </div>
                </div>
                
                <table>
                    <thead>
                        <tr>
                            <th>类别ID</th>
                            <th>类别名称</th>
                            <th>总数量</th>
                            <th>训练集</th>
                            <th>验证集</th>
                            <th>分布比例</th>
                        </tr>
                    </thead>
                    <tbody>
                        {table_rows}
                    </tbody>
                </table>
            </div>
            
            <!-- 图像-类别映射示例 -->
            <div class="section">
                <h2>🖼️ 图像-类别映射示例 (前50张)</h2>
                <div class="warning">
                    <strong>💡 提示:</strong> 此处仅显示前50张图像的映射关系，完整数据请查看日志文件。
                </div>
                
                <table>
                    <thead>
                        <tr>
                            <th>文件名</th>
                            <th>包含类别</th>
                            <th>状态</th>
                            <th>所属集合</th>
                        </tr>
                    </thead>
                    <tbody>
                        {image_mapping_rows}
                    </tbody>
                </table>
            </div>
        </div>
        
        <div class="footer">
            <p>Generated by Auto Labeling Pipeline | YOLOv7 Dataset Builder</p>
        </div>
    </div>
</body>
</html>
        """
        
        return html

    def generate_dataset(self):
        """
        主流程：遍历图片 -> 预测 -> 生成输出
        
        输出结构（标准 YOLO 格式）：
        - train/
        │   ├── images/  : SVS 最低分辨率 JPG 图像
        │   └── labels/  : YOLO 格式 TXT 标签
        - val/
        │   ├── images/  : SVS 最低分辨率 JPG 图像
        │   └── labels/  : YOLO 格式 TXT 标签
        - geojson/       : GeoJSON 预测结果
        - visualization/ : 可视化标注图像
        """
        
        logger.info("="*60)
        logger.info("开始生成数据集")
        logger.info("="*60)
        
        out_dir = Path(self.config['output_dataset_dir'])
        
        # 创建标准 YOLO 目录结构
        train_images_dir = out_dir / 'train' / 'images'
        train_labels_dir = out_dir / 'train' / 'labels'
        val_images_dir = out_dir / 'val' / 'images'
        val_labels_dir = out_dir / 'val' / 'labels'
        geojson_dir = out_dir / 'geojson'
        visualization_dir = out_dir / 'visualization'
        
        for d in [train_images_dir, train_labels_dir, val_images_dir, val_labels_dir, 
                  geojson_dir, visualization_dir]:
            d.mkdir(parents=True, exist_ok=True)
        
        logger.info(f"训练集图像目录: {train_images_dir}")
        logger.info(f"训练集标签目录: {train_labels_dir}")
        logger.info(f"验证集图像目录: {val_images_dir}")
        logger.info(f"验证集标签目录: {val_labels_dir}")
        logger.info(f"GeoJSON 目录: {geojson_dir}")
        logger.info(f"可视化目录: {visualization_dir}")
        
        # 用于收集实际出现的类别及其样本数量
        detected_classes = {}  # {class_id: sample_count}
        
        # 扫描所有支持的图像格式（支持多目录）
        raw_dirs = self.config['raw_image_dir']
        
        # 统一转换为列表格式
        if isinstance(raw_dirs, str):
            raw_dirs = [raw_dirs]
        
        logger.info(f"配置了 {len(raw_dirs)} 个数据源目录")
        for i, dir_path in enumerate(raw_dirs, 1):
            logger.info(f"  [{i}] {dir_path}")
        
        # 基础图像格式（始终支持）
        image_extensions = ['*.jpg', '*.jpeg', '*.png', '*.bmp', '*.tif', '*.tiff']
        
        # SVS 格式（需要 OpenSlide）
        if OPENSIDE_AVAILABLE:
            image_extensions.append('*.svs')
            logger.info(f"图像扫描配置: 支持 {len(image_extensions)} 种格式 (包含 SVS)")
        else:
            logger.info(f"图像扫描配置: 支持 {len(image_extensions)} 种格式 (不包含 SVS，需安装 OpenSlide)")
        
        logger.info(f"支持的格式: {', '.join(image_extensions)}")
        
        # 从所有目录中扫描图像
        image_paths = []
        dir_stats = {}  # 统计每个目录找到的文件数
        
        for raw_dir_str in raw_dirs:
            raw_dir = Path(raw_dir_str)
            
            if not raw_dir.exists():
                logger.warning(f"⚠ 目录不存在，跳过: {raw_dir}")
                continue
            
            if not raw_dir.is_dir():
                logger.warning(f"⚠ 路径不是目录，跳过: {raw_dir}")
                continue
            
            dir_file_count = 0
            
            for ext in image_extensions:
                found_files = list(raw_dir.glob(ext))
                if found_files:
                    logger.debug(f"  [{raw_dir.name}] 找到 {len(found_files)} 个 {ext} 文件")
                    image_paths.extend(found_files)
                    dir_file_count += len(found_files)
            
            dir_stats[str(raw_dir)] = dir_file_count
        
        if not image_paths:
            logger.error("="*60)
            logger.error("❌ 未找到任何图像文件")
            logger.error(f"   扫描的目录数量: {len(raw_dirs)}")
            for i, dir_path in enumerate(raw_dirs, 1):
                exists = "✓" if Path(dir_path).exists() else "✗"
                logger.error(f"   [{exists}] [{i}] {dir_path}")
            logger.error(f"   支持的格式: {', '.join(image_extensions)}")
            logger.error("")
            logger.error("可能的原因:")
            logger.error("  1. 目录路径不正确，请检查 CONFIG['raw_image_dir'] 配置")
            logger.error("  2. 目录中没有支持的图像文件")
            if not OPENSIDE_AVAILABLE:
                logger.error("  3. 如果有 .svs 文件，需要安装 OpenSlide 库才能识别")
                logger.error("     安装命令: pip install openslide-python")
            logger.error("="*60)
            return
        
        logger.info(f"扫描完成，共发现 {len(image_paths)} 张图像")
        
        # 显示各目录的扫描结果
        logger.info("各目录扫描统计:")
        for dir_path, count in dir_stats.items():
            dir_name = Path(dir_path).name
            status = "✓" if count > 0 else "⚠ (空)"
            logger.info(f"  [{status}] {dir_name}: {count} 张图像")
        
        # 统计各格式数量
        format_stats = {}
        for img_path in image_paths:
            ext = img_path.suffix.lower()
            format_stats[ext] = format_stats.get(ext, 0) + 1
        logger.info(f"图像格式分布: {format_stats}")

        random.seed(self.config['seed'])
        
        # 选择抽样策略
        if self.config['use_stratified_split']:
            logger.info("")
            logger.info("="*60)
            logger.info("使用分层抽样策略 (Stratified Splitting)")
            logger.info("="*60)
            train_paths, val_paths, image_classes, class_to_images = self._stratified_split(image_paths)
        else:
            logger.info("")
            logger.info("使用简单随机分割策略")
            random.shuffle(image_paths)
            split_idx = int(len(image_paths) * self.config['train_ratio'])
            train_paths = image_paths[:split_idx]
            val_paths = image_paths[split_idx:]
            image_classes = None
            class_to_images = None
        
        logger.info(f"数据集划分 - 训练集: {len(train_paths)}, 验证集: {len(val_paths)}")
        logger.info(f"训练集比例: {self.config['train_ratio']*100:.1f}%")

        def process_train_split(paths):
            """处理训练集：保存 SVS 最低分辨率 JPG 和 YOLO TXT 标签"""
            logger.info("-"*60)
            logger.info("开始处理训练集")
            logger.info("-"*60)
            
            images_saved = 0
            labels_saved = 0
            total_detections = 0
            failed_count = 0
            geojson_saved = 0
            vis_saved = 0
            
            for i, img_path in enumerate(paths):
                if i % 10 == 0 or i == len(paths) - 1:
                    progress = (i + 1) / len(paths) * 100
                    logger.info(f"[train] 进度: {i+1}/{len(paths)} ({progress:.1f}%)")
                
                try:
                    # 执行预测获取图像和检测结果
                    detections, img_array, metadata = self.predict_image(str(img_path))
                    total_detections += len(detections)
                    
                    # 收集实际出现的类别及样本数量（按图像统计，非检测框）
                    if detections and img_array is not None:
                        for det in detections:
                            cls_id = det['class']
                            if cls_id not in detected_classes:
                                detected_classes[cls_id] = 0
                        # 这张图像包含检测框，所有出现的类别计数+1
                        unique_classes_in_image = set(det['class'] for det in detections)
                        for cls_id in unique_classes_in_image:
                            detected_classes[cls_id] += 1
                    
                    # 如果是 SVS 文件，保存最低分辨率 JPG 到 images 目录
                    if img_path.suffix.lower() == '.svs' and img_array is not None:
                        output_filename = img_path.stem + '.jpg'
                        output_img_path = train_images_dir / output_filename
                        
                        # 保存为高质量 JPG
                        cv2.imwrite(str(output_img_path), img_array, [cv2.IMWRITE_JPEG_QUALITY, 95])
                        images_saved += 1
                        logger.debug(f"  ✓ 已保存图像: {output_filename} (尺寸: {img_array.shape[1]}x{img_array.shape[0]})")
                    
                    # 保存 YOLO 格式标签文件到 labels 目录（所有文件）
                    label_path = train_labels_dir / (img_path.stem + '.txt')
                    with open(label_path, 'w') as f:
                        for det in detections:
                            f.write(f"{det['class']} {det['x']:.6f} {det['y']:.6f} {det['w']:.6f} {det['h']:.6f}\n")
                    
                    if detections:
                        labels_saved += 1
                        logger.debug(f"  ✓ 已保存标签: {img_path.stem}.txt ({len(detections)} 个检测框)")
                        
                        # 保存 GeoJSON 文件
                        geojson_path = self.save_geojson(detections, img_path, metadata, geojson_dir)
                        if geojson_path:
                            geojson_saved += 1
                        
                        # 保存可视化图像
                        if img_array is not None:
                            vis_path = self.save_visualization(img_array, detections, img_path, visualization_dir)
                            if vis_path:
                                vis_saved += 1
                    else:
                        logger.debug(f"  ⊘ 无检测结果: {img_path.name}")
                    
                except Exception as e:
                    failed_count += 1
                    logger.error(f"处理失败 [{img_path.name}]: {e}")
                    import traceback
                    logger.debug(traceback.format_exc())
            
            logger.info(f"\n[train] 处理完成:")
            logger.info(f"  - 总文件数: {len(paths)}")
            logger.info(f"  - 成功保存图像 (JPG): {images_saved}")
            logger.info(f"  - 成功保存标签 (TXT): {labels_saved}")
            logger.info(f"  - 总检测框数: {total_detections}")
            logger.info(f"  - GeoJSON 文件保存: {geojson_saved} 个")
            logger.info(f"  - 可视化图像保存: {vis_saved} 个")
            logger.info(f"  - 处理失败: {failed_count}")
            logger.info("")

        def process_val_split(paths):
            """处理验证集：保存 SVS 最低分辨率 JPG 和 YOLO TXT 标签"""
            logger.info("-"*60)
            logger.info("开始处理验证集")
            logger.info("-"*60)
            
            images_saved = 0
            labels_saved = 0
            total_detections = 0
            failed_images = 0
            geojson_saved = 0
            vis_saved = 0
            
            for i, img_path in enumerate(paths):
                if i % 10 == 0 or i == len(paths) - 1:
                    progress = (i + 1) / len(paths) * 100
                    logger.info(f"[val] 进度: {i+1}/{len(paths)} ({progress:.1f}%)")
                    
                try:
                    # 执行预测
                    detections, img_array, metadata = self.predict_image(str(img_path))
                    total_detections += len(detections)
                    
                    # 收集实际出现的类别及样本数量（按图像统计，非检测框）
                    if detections and img_array is not None:
                        for det in detections:
                            cls_id = det['class']
                            if cls_id not in detected_classes:
                                detected_classes[cls_id] = 0
                        # 这张图像包含检测框，所有出现的类别计数+1
                        unique_classes_in_image = set(det['class'] for det in detections)
                        for cls_id in unique_classes_in_image:
                            detected_classes[cls_id] += 1
                    
                    # 如果是 SVS 文件，保存最低分辨率 JPG 到 images 目录
                    if img_path.suffix.lower() == '.svs' and img_array is not None:
                        output_filename = img_path.stem + '.jpg'
                        output_img_path = val_images_dir / output_filename
                        
                        # 保存为高质量 JPG
                        cv2.imwrite(str(output_img_path), img_array, [cv2.IMWRITE_JPEG_QUALITY, 95])
                        images_saved += 1
                        logger.debug(f"  ✓ 已保存图像: {output_filename}")
                    
                    # 保存 YOLO 格式标签文件到 labels 目录
                    label_path = val_labels_dir / (img_path.stem + '.txt')
                    with open(label_path, 'w') as f:
                        for det in detections:
                            f.write(f"{det['class']} {det['x']:.6f} {det['y']:.6f} {det['w']:.6f} {det['h']:.6f}\n")
                    
                    if detections:
                        labels_saved += 1
                        logger.debug(f"  ✓ 已保存标签: {img_path.stem}.txt ({len(detections)} 个检测框)")
                    
                    # 保存 GeoJSON 文件（所有格式）
                    if detections:
                        geojson_path = self.save_geojson(detections, img_path, metadata, geojson_dir)
                        if geojson_path:
                            geojson_saved += 1
                    
                    # 保存可视化图像（有检测结果的）
                    if detections and img_array is not None:
                        vis_path = self.save_visualization(img_array, detections, img_path, visualization_dir)
                        if vis_path:
                            vis_saved += 1
                        
                except Exception as e:
                    failed_images += 1
                    logger.error(f"处理图像失败 [{img_path.name}]: {e}")
                    import traceback
                    logger.debug(traceback.format_exc())
            
            # 输出统计信息
            logger.info(f"\n[val] 处理完成统计:")
            logger.info(f"  - 总图像数: {len(paths)}")
            logger.info(f"  - 成功保存图像 (JPG): {images_saved}")
            logger.info(f"  - 成功保存标签 (TXT): {labels_saved}")
            logger.info(f"  - 总检测框数: {total_detections}")
            logger.info(f"  - 处理失败: {failed_images}")
            if labels_saved > 0:
                logger.info(f"  - 平均每图检测框数: {total_detections/labels_saved:.2f}")
            logger.info(f"  - GeoJSON 文件保存: {geojson_saved} 个")
            logger.info(f"  - 可视化图像保存: {vis_saved} 个")
            logger.info("")

        # 处理训练集和验证集
        process_train_split(train_paths)
        process_val_split(val_paths)
        
        # 应用类别过滤：移除样本数量低于阈值的类别
        logger.info("="*60)
        logger.info("类别样本数量统计与过滤")
        logger.info("="*60)
        
        min_samples = self.config['min_samples_per_class']
        logger.info(f"最小样本数阈值: {min_samples}")
        logger.info("")
        
        # 显示所有类别的样本数量
        logger.info("所有类别样本数量统计:")
        for cls_id in sorted(detected_classes.keys()):
            class_name = self.names[cls_id]
            sample_count = detected_classes[cls_id]
            status = "✓" if sample_count >= min_samples else "✗ (将被过滤)"
            logger.info(f"  [{status}] 类别 {cls_id} ({class_name}): {sample_count} 个样本")
        
        # 过滤出满足阈值的类别
        valid_classes = {
            cls_id: count for cls_id, count in detected_classes.items()
            if count >= min_samples
        }
        
        filtered_classes = {
            cls_id: count for cls_id, count in detected_classes.items()
            if count < min_samples
        }
        
        logger.info("")
        logger.info(f"过滤结果:")
        logger.info(f"  - 满足阈值的类别: {len(valid_classes)} 个")
        logger.info(f"  - 被过滤的类别: {len(filtered_classes)} 个")
        
        if filtered_classes:
            logger.info("")
            logger.info("被过滤的类别详情:")
            for cls_id in sorted(filtered_classes.keys()):
                class_name = self.names[cls_id]
                sample_count = filtered_classes[cls_id]
                logger.info(f"  - 类别 {cls_id} ({class_name}): {sample_count} 个样本 (阈值: {min_samples})")
        
        # 生成 data.yaml 配置文件（使用过滤后的类别）
        if valid_classes:
            self.generate_data_yaml(out_dir, train_images_dir, val_images_dir, set(valid_classes.keys()))
            logger.info("")
            logger.info("✓ 已使用过滤后的类别生成 data.yaml")
        else:
            logger.warning("")
            logger.warning("⚠ 警告: 所有类别都被过滤，将使用模型默认类别生成 data.yaml")
            self.generate_data_yaml(out_dir, train_images_dir, val_images_dir, None)
        
        # 生成可视化报告（仅在启用分层抽样时）
        if self.config['use_stratified_split'] and image_classes is not None:
            self._generate_visualization_report(
                image_classes, class_to_images, 
                train_paths, val_paths, out_dir
            )
        
        logger.info("="*60)
        logger.info("数据集生成完成！")
        logger.info(f"训练集图像: {train_images_dir}")
        logger.info(f"训练集标签: {train_labels_dir}")
        logger.info(f"验证集图像: {val_images_dir}")
        logger.info(f"验证集标签: {val_labels_dir}")
        logger.info(f"GeoJSON: {geojson_dir}")
        logger.info(f"可视化: {visualization_dir}")
        logger.info(f"data.yaml: {out_dir / 'data.yaml'}")
        if self.config['use_stratified_split']:
            logger.info(f"可视化报告: {out_dir / 'report' / 'class_distribution_report.html'}")
        logger.info("="*60)

if __name__ == '__main__':
    start_time = datetime.now()
    logger.info(f"程序启动时间: {start_time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    try:
        labeler = AutoLabeler(CONFIG)
        labeler.generate_dataset()
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        logger.info(f"\n总耗时: {duration:.2f} 秒 ({duration/60:.2f} 分钟)")
        logger.info(f"程序结束时间: {end_time.strftime('%Y-%m-%d %H:%M:%S')}")
        
    except KeyboardInterrupt:
        logger.warning("\n用户中断程序执行")
        sys.exit(1)
    except Exception as e:
        logger.error(f"\n程序执行出错: {e}")
        import traceback
        logger.error(traceback.format_exc())
        sys.exit(1)
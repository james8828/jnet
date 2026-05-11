"""
WSI 图像预测程序
对 wsi_predictions/images 目录中的图像进行YOLOv7预测
"""
import sys
from pathlib import Path

# 添加路径
sys.path.append(str(Path(__file__).parent))

from predict import YOLOv7Predictor
from export_results import export_to_json, export_to_csv, export_statistics


def main():
    """主函数 - 预测WSI生成的图像"""
    
    print("=" * 80)
    print("WSI 图像预测程序")
    print("=" * 80)
    
    # ==================== 配置区域 ====================
    CONFIG = {
        # 'weights': 'E:/doc/system-admin/python/model/0R_WT_YOLO_Box_A1.0.pt',
        'weights': 'E:/doc/system-admin/python/model/best.pt',
        'input_dir': 'E:/doc/system-admin/wsi_predictions/images',   # WSI生成的图像
        'output_dir': 'E:/doc/system-admin/wsi_predictions/results', # 预测结果输出
        'device': '0',          # GPU设备 (cpu 或 0)
        'img_size': 640,        # 输入尺寸
        'conf_thres': 0.25,     # 置信度阈值 (降低以检测更多目标)
        'iou_thres': 0.01,      # NMS IoU阈值
        'export_json': True,    # 导出JSON
        'export_csv': True,     # 导出CSV
        'export_stats': True    # 导出统计报告
    }
    # ================================================
    
    print(f"\n配置信息:")
    print(f"  模型权重: {CONFIG['weights']}")
    print(f"  输入目录: {CONFIG['input_dir']}")
    print(f"  输出目录: {CONFIG['output_dir']}")
    print(f"  设备: {CONFIG['device']}")
    print(f"  图像尺寸: {CONFIG['img_size']}")
    print(f"  置信度阈值: {CONFIG['conf_thres']}")
    print(f"  IoU阈值: {CONFIG['iou_thres']}")
    print(f"  导出JSON: {CONFIG['export_json']}")
    print(f"  导出CSV: {CONFIG['export_csv']}")
    print(f"  导出统计: {CONFIG['export_stats']}")
    print("=" * 80)
    
    # 创建预测器
    print("\n[1/3] 加载模型...")
    predictor = YOLOv7Predictor(
        weights=CONFIG['weights'],
        device=CONFIG['device'],
        img_size=CONFIG['img_size'],
        conf_thres=CONFIG['conf_thres'],
        iou_thres=CONFIG['iou_thres'],
        multi_label=False
    )
    
    # 批量预测
    print("\n[2/3] 开始批量预测...")
    results = predictor.predict_batch(
        input_dir=CONFIG['input_dir'],
        output_dir=CONFIG['output_dir'],
        pattern='*.jpg'
    )
    
    if not results:
        print("\n⚠️ 未检测到任何目标")
        return
    
    # 导出结果
    print("\n[3/3] 导出预测结果...")
    output_path = Path(CONFIG['output_dir'])
    output_path.mkdir(parents=True, exist_ok=True)
    
    if CONFIG['export_json']:
        export_to_json(results, output_path / 'predictions.json')
    
    if CONFIG['export_csv']:
        export_to_csv(results, output_path / 'predictions.csv')
    
    if CONFIG['export_stats']:
        export_statistics(results, output_path / 'statistics.txt')
    
    # 完成
    print("\n" + "=" * 80)
    print("✓ 预测完成!")
    print("=" * 80)
    print(f"\n输出目录: {CONFIG['output_dir']}")
    print(f"  - 标注图像: {output_path}/*.jpg")
    if CONFIG['export_json']:
        print(f"  - JSON结果: {output_path / 'predictions.json'}")
    if CONFIG['export_csv']:
        print(f"  - CSV结果: {output_path / 'predictions.csv'}")
    if CONFIG['export_stats']:
        print(f"  - 统计报告: {output_path / 'statistics.txt'}")
    print("=" * 80)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n用户中断执行")
    except Exception as e:
        print(f"\n\n发生错误: {e}")
        import traceback
        traceback.print_exc()

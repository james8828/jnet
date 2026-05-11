"""
YOLOv7 预测结果导出工具
支持导出为JSON、CSV格式
"""
import json
import csv
from pathlib import Path
from datetime import datetime


def export_to_json(results, output_file='predictions.json'):
    """
    导出预测结果为JSON格式
    
    Args:
        results: 预测结果字典 {image_path: [detections]}
        output_file: 输出文件路径
    """
    export_data = {
        'timestamp': datetime.now().isoformat(),
        'total_images': len(results),
        'total_detections': sum(len(dets) for dets in results.values()),
        'results': []
    }
    
    for img_path, detections in results.items():
        image_result = {
            'image': str(img_path),
            'detections_count': len(detections),
            'detections': []
        }
        
        for det in detections:
            detection_info = {
                'class_id': det['class_id'],
                'class_name': det['class_name'],
                'confidence': round(det['confidence'], 4),
                'bbox': {
                    'x1': det['bbox'][0],
                    'y1': det['bbox'][1],
                    'x2': det['bbox'][2],
                    'y2': det['bbox'][3],
                    'width': det['bbox'][2] - det['bbox'][0],
                    'height': det['bbox'][3] - det['bbox'][1]
                }
            }
            image_result['detections'].append(detection_info)
        
        export_data['results'].append(image_result)
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(export_data, f, indent=2, ensure_ascii=False)
    
    print(f"JSON结果已导出: {output_file}")
    return export_data


def export_to_csv(results, output_file='predictions.csv'):
    """
    导出预测结果为CSV格式
    
    Args:
        results: 预测结果字典 {image_path: [detections]}
        output_file: 输出文件路径
    """
    rows = []
    
    for img_path, detections in results.items():
        for det in detections:
            row = {
                'image': Path(img_path).name,
                'class_id': det['class_id'],
                'class_name': det['class_name'],
                'confidence': round(det['confidence'], 4),
                'x1': det['bbox'][0],
                'y1': det['bbox'][1],
                'x2': det['bbox'][2],
                'y2': det['bbox'][3],
                'width': det['bbox'][2] - det['bbox'][0],
                'height': det['bbox'][3] - det['bbox'][1]
            }
            rows.append(row)
    
    if not rows:
        print("没有检测结果可导出")
        return
    
    # 写入CSV
    fieldnames = ['image', 'class_id', 'class_name', 'confidence', 
                  'x1', 'y1', 'x2', 'y2', 'width', 'height']
    
    with open(output_file, 'w', newline='', encoding='utf-8-sig') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    
    print(f"CSV结果已导出: {output_file}")
    print(f"总检测数: {len(rows)}")


def export_statistics(results, output_file='statistics.txt'):
    """
    导出统计信息
    
    Args:
        results: 预测结果字典
        output_file: 输出文件路径
    """
    # 统计每个类别的检测数量
    class_stats = {}
    confidence_list = []
    
    for img_path, detections in results.items():
        for det in detections:
            class_name = det['class_name']
            if class_name not in class_stats:
                class_stats[class_name] = 0
            class_stats[class_name] += 1
            confidence_list.append(det['confidence'])
    
    # 计算统计指标
    total_detections = len(confidence_list)
    avg_confidence = sum(confidence_list) / total_detections if total_detections > 0 else 0
    min_confidence = min(confidence_list) if confidence_list else 0
    max_confidence = max(confidence_list) if confidence_list else 0
    
    # 写入统计文件
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("=" * 60 + "\n")
        f.write("YOLOv7 预测统计报告\n")
        f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 60 + "\n\n")
        
        f.write(f"总图像数: {len(results)}\n")
        f.write(f"总检测数: {total_detections}\n\n")
        
        f.write("各类别检测数量:\n")
        f.write("-" * 40 + "\n")
        # 使用 key 参数指定按字符串形式比较键
        for class_name, count in sorted(class_stats.items(), key=lambda item: str(item[0])):
            percentage = (count / total_detections * 100) if total_detections > 0 else 0
            # 修复后的代码：使用 str() 确保 class_name 是字符串
            f.write(f"  {str(class_name):15s}: {count:5d} ({percentage:5.1f}%)\n")
        f.write("\n")
        
        f.write("置信度统计:\n")
        f.write("-" * 40 + "\n")
        f.write(f"  平均置信度: {avg_confidence:.4f}\n")
        f.write(f"  最小置信度: {min_confidence:.4f}\n")
        f.write(f"  最大置信度: {max_confidence:.4f}\n\n")
        
        # 每张图像的检测结果数
        f.write("各图像检测数量:\n")
        f.write("-" * 40 + "\n")
        for img_path, detections in sorted(results.items()):
            f.write(f"  {Path(img_path).name}: {len(detections)}\n")
    
    print(f"统计报告已导出: {output_file}")


if __name__ == '__main__':
    # 示例用法
    print("结果导出工具")
    print("请在 predict.py 中调用此模块的导出函数")

"""
YOLO Web API 测试脚本
用于验证 API 接口的功能
"""
import requests
import time
import json
from pathlib import Path

# API 基础 URL
BASE_URL = "http://localhost:8000"


def test_health_check():
    """测试健康检查"""
    print("\n" + "=" * 60)
    print("测试 1: 健康检查")
    print("=" * 60)
    
    try:
        response = requests.get(f"{BASE_URL}/health")
        print(f"✓ 状态码: {response.status_code}")
        print(f"✓ 响应: {json.dumps(response.json(), indent=2)}")
        return True
    except Exception as e:
        print(f"✗ 失败: {e}")
        return False


def test_system_info():
    """测试系统信息"""
    print("\n" + "=" * 60)
    print("测试 2: 系统信息")
    print("=" * 60)
    
    try:
        response = requests.get(f"{BASE_URL}/api/v1/system/info")
        print(f"✓ 状态码: {response.status_code}")
        info = response.json()
        print(f"✓ Python 版本: {info['system']['python_version']}")
        print(f"✓ CUDA 可用: {info['system']['cuda_available']}")
        if info['system']['gpu_count'] > 0:
            print(f"✓ GPU: {info['system']['gpu_names'][0]}")
        return True
    except Exception as e:
        print(f"✗ 失败: {e}")
        return False


def test_create_training_task():
    """测试创建训练任务"""
    print("\n" + "=" * 60)
    print("测试 3: 创建训练任务")
    print("=" * 60)
    
    # 注意：这个测试需要一个真实的数据集配置文件
    train_config = {
        "dataset_yaml": r"E:\doc\system-admin\generated_dataset\data.yaml",
        "epochs": 5,  # 使用较小的 epochs 进行测试
        "batch_size": 2,
        "image_size": 640,
        "device": "cpu",  # 使用 CPU 进行测试
        "weights": "yolov7-tiny.pt",
        "use_adam": True,
        "workers": 0
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/api/v1/training/tasks",
            json=train_config
        )
        
        if response.status_code == 200:
            result = response.json()
            print(f"✓ 任务创建成功")
            print(f"✓ Task ID: {result['task_id']}")
            return result['task_id']
        else:
            print(f"✗ 失败: {response.status_code}")
            print(f"✗ 错误: {response.text}")
            return None
            
    except Exception as e:
        print(f"✗ 异常: {e}")
        return None


def test_get_task_status(task_id):
    """测试获取任务状态"""
    print("\n" + "=" * 60)
    print("测试 4: 获取任务状态")
    print("=" * 60)
    
    try:
        response = requests.get(f"{BASE_URL}/api/v1/training/tasks/{task_id}")
        
        if response.status_code == 200:
            status = response.json()["task"]
            print(f"✓ 状态: {status['status']}")
            print(f"✓ 进度: {status['progress']:.1f}%")
            print(f"✓ Epoch: {status['current_epoch']}/{status['total_epochs']}")
            return True
        else:
            print(f"✗ 失败: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"✗ 异常: {e}")
        return False


def test_list_tasks():
    """测试列出所有任务"""
    print("\n" + "=" * 60)
    print("测试 5: 列出所有任务")
    print("=" * 60)
    
    try:
        response = requests.get(f"{BASE_URL}/api/v1/training/tasks")
        
        if response.status_code == 200:
            result = response.json()
            print(f"✓ 任务总数: {result['count']}")
            for task in result['tasks'][:3]:  # 只显示前3个
                print(f"  - {task['task_id']}: {task['status']} ({task['progress']:.1f}%)")
            return True
        else:
            print(f"✗ 失败: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"✗ 异常: {e}")
        return False


def test_create_predictor():
    """测试创建预测器"""
    print("\n" + "=" * 60)
    print("测试 6: 创建预测器")
    print("=" * 60)
    
    predictor_config = {
        "model_path": r"E:\doc\system-admin\yolo_training_results\best.pt",
        "device": "cpu",
        "conf_thres": 0.25,
        "iou_thres": 0.45
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/api/v1/prediction/predictors",
            json=predictor_config
        )
        
        if response.status_code == 200:
            result = response.json()
            print(f"✓ 预测器创建成功")
            print(f"✓ Predictor ID: {result['predictor_id']}")
            return result['predictor_id']
        else:
            print(f"✗ 失败: {response.status_code}")
            print(f"✗ 错误: {response.text}")
            return None
            
    except Exception as e:
        print(f"✗ 异常: {e}")
        return None


def test_predict_image(predictor_id, image_path):
    """测试图像预测"""
    print("\n" + "=" * 60)
    print("测试 7: 图像预测")
    print("=" * 60)
    
    if not Path(image_path).exists():
        print(f"✗ 测试图像不存在: {image_path}")
        print(f"  请提供一个测试图像路径")
        return None
    
    try:
        with open(image_path, "rb") as f:
            files = {"file": f}
            params = {
                "predictor_id": predictor_id,
                "img_size": 640
            }
            
            response = requests.post(
                f"{BASE_URL}/api/v1/prediction/predict",
                params=params,
                files=files
            )
        
        if response.status_code == 200:
            result = response.json()
            print(f"✓ 预测成功")
            print(f"✓ Prediction ID: {result['prediction_id']}")
            print(f"✓ 检测到 {len(result['detections'])} 个目标")
            print(f"✓ 推理时间: {result['inference_time']:.3f}s")
            
            for det in result['detections'][:5]:  # 只显示前5个
                print(f"  - {det['class_name']}: {det['confidence']:.2f}")
            
            return result['prediction_id']
        else:
            print(f"✗ 失败: {response.status_code}")
            print(f"✗ 错误: {response.text}")
            return None
            
    except Exception as e:
        print(f"✗ 异常: {e}")
        return None


def test_get_prediction_result(prediction_id):
    """测试获取预测结果"""
    print("\n" + "=" * 60)
    print("测试 8: 获取预测结果")
    print("=" * 60)
    
    try:
        response = requests.get(
            f"{BASE_URL}/api/v1/prediction/results/{prediction_id}"
        )
        
        if response.status_code == 200:
            result = response.json()["result"]
            print(f"✓ 结果获取成功")
            print(f"✓ 输入文件: {Path(result['input_file']).name}")
            print(f"✓ 输出图像: {result['output_image']}")
            return True
        else:
            print(f"✗ 失败: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"✗ 异常: {e}")
        return False


def run_all_tests():
    """运行所有测试"""
    print("\n" + "=" * 60)
    print("YOLO Web API 测试套件")
    print("=" * 60)
    
    # 检查服务是否运行
    try:
        response = requests.get(f"{BASE_URL}/health")
        if response.status_code != 200:
            print("✗ API 服务未运行，请先启动服务")
            print("  运行: python yolo_api.py")
            return
    except:
        print("✗ 无法连接到 API 服务")
        print("  请确保服务已启动: python yolo_api.py")
        return
    
    print("✓ API 服务正常运行\n")
    
    # 运行测试
    results = []
    
    # 基础测试
    results.append(("健康检查", test_health_check()))
    results.append(("系统信息", test_system_info()))
    
    # 训练相关测试（可选，需要数据集）
    print("\n提示: 以下训练测试需要有效的数据集配置文件")
    input("按 Enter 继续，或 Ctrl+C 跳过...")
    
    task_id = test_create_training_task()
    if task_id:
        results.append(("创建训练任务", True))
        time.sleep(2)
        results.append(("获取任务状态", test_get_task_status(task_id)))
        results.append(("列出所有任务", test_list_tasks()))
    
    # 预测相关测试（可选，需要模型和测试图像）
    print("\n提示: 以下预测测试需要有效的模型文件和测试图像")
    test_image = input("请输入测试图像路径（留空跳过）: ").strip()
    
    if test_image and Path(test_image).exists():
        predictor_id = test_create_predictor()
        if predictor_id:
            results.append(("创建预测器", True))
            
            prediction_id = test_predict_image(predictor_id, test_image)
            if prediction_id:
                results.append(("图像预测", True))
                results.append(("获取预测结果", test_get_prediction_result(prediction_id)))
    
    # 打印测试结果汇总
    print("\n" + "=" * 60)
    print("测试结果汇总")
    print("=" * 60)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✓ 通过" if result else "✗ 失败"
        print(f"{status} - {test_name}")
    
    print("-" * 60)
    print(f"总计: {passed}/{total} 测试通过")
    print("=" * 60)


if __name__ == "__main__":
    run_all_tests()

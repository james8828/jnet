# YOLO Web API 快速入门指南

## 🎯 5分钟快速开始

### 第一步：安装依赖

```bash
cd e:\doc\jnet\python\yolo\train
pip install -r requirements_api.txt
```

### 第二步：启动服务

**Windows:**
```bash
start_api.bat
```

看到以下输出表示成功：
```
========================================
  YOLO Training & Prediction API Service
========================================
API 文档: http://0.0.0.0:8000/docs
```

### 第三步：访问 API 文档

打开浏览器访问：**http://localhost:8000/docs**

你会看到 Swagger UI 界面，可以在线测试所有 API。

---

## 🚀 快速使用示例

### 示例 1：训练模型

#### 方法 A：使用 Swagger UI（推荐新手）

1. 打开 http://localhost:8000/docs
2. 找到 `POST /api/v1/training/tasks`
3. 点击 "Try it out"
4. 填写配置：
```json
{
  "dataset_yaml": "你的数据集路径/data.yaml",
  "epochs": 10,
  "batch_size": 2,
  "image_size": 640,
  "device": "cpu"
}
```
5. 点击 "Execute"
6. 复制返回的 `task_id`

#### 方法 B：使用 Python

```python
import requests

# 创建训练任务
response = requests.post("http://localhost:8000/api/v1/training/tasks", json={
    "dataset_yaml": "E:/data/dataset/data.yaml",
    "epochs": 10,
    "batch_size": 2,
    "image_size": 640,
    "device": "cpu"
})

task_id = response.json()["task_id"]
print(f"任务ID: {task_id}")

# 启动训练
requests.post(f"http://localhost:8000/api/v1/training/tasks/{task_id}/start")
print("训练已启动！")
```

### 示例 2：图像预测

#### 方法 A：使用 Swagger UI

1. 先创建预测器：`POST /api/v1/prediction/predictors`
```json
{
  "model_path": "你的模型路径/best.pt",
  "device": "cpu"
}
```
2. 复制返回的 `predictor_id`
3. 执行预测：`POST /api/v1/prediction/predict`
   - 上传图像文件
   - 填写 predictor_id
   - 点击 Execute

#### 方法 B：使用 Python

```python
import requests

# 创建预测器
response = requests.post("http://localhost:8000/api/v1/prediction/predictors", json={
    "model_path": "E:/models/best.pt",
    "device": "cpu"
})
predictor_id = response.json()["predictor_id"]

# 执行预测
with open("test.jpg", "rb") as f:
    response = requests.post(
        "http://localhost:8000/api/v1/prediction/predict",
        params={"predictor_id": predictor_id, "img_size": 640},
        files={"file": f}
    )

result = response.json()
print(f"检测到 {len(result['detections'])} 个目标")
for det in result['detections']:
    print(f"  - {det['class_name']}: {det['confidence']:.2f}")
```

#### 方法 C：使用 cURL

```bash
# 创建预测器
curl -X POST "http://localhost:8000/api/v1/prediction/predictors" \
  -H "Content-Type: application/json" \
  -d '{"model_path":"E:/models/best.pt","device":"cpu"}'

# 执行预测
curl -X POST "http://localhost:8000/api/v1/prediction/predict?predictor_id=YOUR_ID&img_size=640" \
  -F "file=@test.jpg"
```

---

## 📊 监控训练进度

### 查看任务状态

```python
import requests
import time

task_id = "你的任务ID"

while True:
    response = requests.get(f"http://localhost:8000/api/v1/training/tasks/{task_id}")
    status = response.json()["task"]
    
    print(f"状态: {status['status']}, 进度: {status['progress']:.1f}%")
    
    if status['status'] in ['completed', 'failed', 'cancelled']:
        break
    
    time.sleep(10)  # 每10秒检查一次
```

### 查看训练日志

```python
# 获取最后100行日志
response = requests.get(f"http://localhost:8000/api/v1/training/tasks/{task_id}/log?lines=100")
print(response.json()["log"])
```

---

## 🔧 常用配置

### 修改端口

编辑 `config.py`:
```python
API_PORT = 8080  # 改为你想要的端口
```

### 使用 GPU

```python
# 创建任务时指定
{
  "device": "0"  # 使用第一个 GPU
}

# 创建预测器时指定
{
  "model_path": "best.pt",
  "device": "0"
}
```

### 调整性能

```python
# CPU 训练 - 降低 batch size
{
  "batch_size": 2,
  "workers": 0  # 禁用多进程加载
}

# GPU 训练 - 提高 batch size
{
  "batch_size": 8,
  "workers": 4
}
```

---

## ❓ 常见问题

### Q1: 服务启动失败？

**检查项：**
1. Python 版本 >= 3.7
2. 依赖是否安装：`pip install -r requirements_api.txt`
3. 端口是否被占用

**解决：**
```bash
# 重新安装依赖
pip install -r requirements_api.txt

# 检查端口
netstat -ano | findstr :8000
```

### Q2: 训练报错 "数据集配置文件不存在"？

确保 `dataset_yaml` 指向正确的 YAML 文件：
```yaml
# data.yaml 示例
train: E:/data/dataset/train/images
val: E:/data/dataset/val/images

nc: 2  # 类别数
names: ['class1', 'class2']  # 类别名称
```

### Q3: 预测报错 "模型加载失败"？

确保模型文件存在且完整：
```python
import os
print(os.path.exists("your_model.pt"))  # 应该返回 True
```

### Q4: 如何停止服务？

在终端按 `Ctrl+C`

---

## 📚 下一步

- 📖 查看完整文档：[API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- 🧪 运行测试：`python test_api.py`
- 🔍 探索 API：http://localhost:8000/docs

---

## 💡 提示

1. **开发环境**：建议使用 Swagger UI 进行接口测试
2. **生产环境**：添加身份认证、HTTPS、速率限制
3. **性能优化**：根据硬件配置调整 batch_size 和 workers
4. **日志查看**：定期检查训练日志定位问题

祝你使用愉快！🎉

# 无 data.yaml 训练接口使用指南

## 📋 概述

新增的 `/api/v1/training/tasks/no-yaml` 接口允许您在不预先准备 `data.yaml` 配置文件的情况下直接启动 YOLO 训练任务。系统会自动根据您提供的参数生成 `data.yaml` 文件。

---

## 🎯 适用场景

- ✅ 快速开始训练，无需手动编写 YAML 配置
- ✅ 动态数据集，类别和路径经常变化
- ✅ 自动化训练流程，需要程序化生成配置
- ✅ 测试和实验阶段，快速验证模型

---

## 🔌 API 接口

### 端点信息

- **URL**: `POST /api/v1/training/tasks/no-yaml`
- **Content-Type**: `application/json`

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| train_dir | string | ✅ | 训练集图片目录路径 |
| val_dir | string | ✅ | 验证集图片目录路径 |
| test_dir | string | ❌ | 测试集图片目录路径（可选） |
| classes | array | ✅ | 类别名称列表 |
| nc | integer | ❌ | 类别数量（默认根据 classes 计算） |
| epochs | integer | ❌ | 训练轮数，默认 300 |
| batch_size | integer | ❌ | 批次大小，默认 4 |
| image_size | integer | ❌ | 图像尺寸，默认 1280 |
| device | string | ❌ | 设备，默认 "0" |
| weights | string | ❌ | 预训练权重，默认 "yolov7x.pt" |
| use_adam | boolean | ❌ | 是否使用 Adam 优化器，默认 false |
| hyp | string | ❌ | 超参数配置文件，默认 "data/hyp.scratch.p5.yaml" |
| workers | integer | ❌ | 数据加载工作进程数，默认 4 |
| cache | boolean | ❌ | 是否缓存图像到内存，默认 false |

---

## 💡 使用示例

### 示例 1: Python requests

```python
import requests

BASE_URL = "http://localhost:8000"

# 创建训练任务（无需 data.yaml）
response = requests.post(
    f"{BASE_URL}/api/v1/training/tasks/no-yaml",
    json={
        "train_dir": "E:/datasets/my_dataset/images/train",
        "val_dir": "E:/datasets/my_dataset/images/val",
        "test_dir": "E:/datasets/my_dataset/images/test",  # 可选
        "classes": ["person", "car", "dog", "cat"],
        "epochs": 100,
        "batch_size": 8,
        "image_size": 640,
        "device": "0"
    }
)

result = response.json()
print(f"任务ID: {result['task_id']}")
print(f"类别: {result['info']['classes']}")
print(f"类别数量: {result['info']['num_classes']}")

# 启动训练
task_id = result['task_id']
requests.post(f"{BASE_URL}/api/v1/training/tasks/{task_id}/start")
```

### 示例 2: cURL

```bash
curl -X POST "http://localhost:8000/api/v1/training/tasks/no-yaml" \
  -H "Content-Type: application/json" \
  -d '{
    "train_dir": "E:/datasets/my_dataset/images/train",
    "val_dir": "E:/datasets/my_dataset/images/val",
    "classes": ["person", "car", "dog", "cat"],
    "epochs": 100,
    "batch_size": 8,
    "image_size": 640,
    "device": "0"
  }'
```

### 示例 3: JavaScript (Fetch)

```javascript
const response = await fetch('http://localhost:8000/api/v1/training/tasks/no-yaml', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    train_dir: 'E:/datasets/my_dataset/images/train',
    val_dir: 'E:/datasets/my_dataset/images/val',
    classes: ['person', 'car', 'dog', 'cat'],
    epochs: 100,
    batch_size: 8,
    image_size: 640,
    device: '0'
  })
});

const result = await response.json();
console.log('Task ID:', result.task_id);
```

---

## 📊 响应格式

### 成功响应

```json
{
  "success": true,
  "task_id": "a1b2c3d4",
  "message": "训练任务已创建（将自动生成 data.yaml），请调用 /start 接口启动训练",
  "info": {
    "train_images": "E:/datasets/my_dataset/images/train",
    "val_images": "E:/datasets/my_dataset/images/val",
    "test_images": "E:/datasets/my_dataset/images/test",
    "classes": ["person", "car", "dog", "cat"],
    "num_classes": 4
  }
}
```

### 错误响应

```json
{
  "detail": "训练集目录不存在: E:/datasets/wrong_path/train"
}
```

---

## 🔄 工作流程

```
1. 调用 /no-yaml 接口
   ↓
2. 系统验证目录是否存在
   ↓
3. 自动生成 data.yaml 文件
   ↓
4. 创建训练任务并返回 task_id
   ↓
5. 调用 /start 接口启动训练
   ↓
6. 训练过程使用自动生成的 data.yaml
```

---

## 📝 自动生成的 data.yaml 示例

系统会自动在任务目录下生成类似以下的 `auto_generated_data.yaml` 文件：

```yaml
train: E:/datasets/my_dataset/images/train
val: E:/datasets/my_dataset/images/val
test: E:/datasets/my_dataset/images/test
nc: 4
names:
- person
- car
- dog
- cat
```

---

## ⚠️ 注意事项

### 1. 目录结构要求

确保您的数据集目录包含正确的图片文件：

```
my_dataset/
├── images/
│   ├── train/          # 训练集图片
│   │   ├── img1.jpg
│   │   ├── img2.jpg
│   │   └── ...
│   ├── val/            # 验证集图片
│   │   ├── img1.jpg
│   │   └── ...
│   └── test/           # 测试集图片（可选）
│       └── ...
└── labels/             # 对应的标签文件（YOLO 格式）
    ├── train/
    │   ├── img1.txt
    │   └── ...
    └── val/
        └── ...
```

### 2. 标签文件格式

标签文件应为 YOLO 格式（`.txt`），每行表示一个目标：

```
<class_id> <x_center> <y_center> <width> <height>
```

例如：
```
0 0.5 0.5 0.3 0.4
1 0.7 0.3 0.2 0.25
```

### 3. 类别索引

- 类别索引从 0 开始
- `classes` 列表的顺序决定类别索引
- 例如：`["person", "car"]` → person=0, car=1

### 4. 路径要求

- 使用绝对路径或相对于工作目录的路径
- Windows 路径使用正斜杠 `/` 或双反斜杠 `\\`
- 确保路径中不包含特殊字符

---

## 🆚 与传统方式对比

### 传统方式（需要 data.yaml）

```python
# 1. 手动创建 data.yaml
# train: E:/datasets/train
# val: E:/datasets/val
# nc: 4
# names: [person, car, dog, cat]

# 2. 调用接口
response = requests.post(
    f"{BASE_URL}/api/v1/training/tasks",
    json={
        "dataset_yaml": "E:/datasets/data.yaml",  # 需要指定 yaml 文件
        "epochs": 100
    }
)
```

### 新方式（无需 data.yaml）

```python
# 直接调用接口，系统自动生成 yaml
response = requests.post(
    f"{BASE_URL}/api/v1/training/tasks/no-yaml",
    json={
        "train_dir": "E:/datasets/train",
        "val_dir": "E:/datasets/val",
        "classes": ["person", "car", "dog", "cat"],
        "epochs": 100
    }
)
```

**优势**：
- ✅ 无需手动创建 YAML 文件
- ✅ 减少配置错误
- ✅ 更适合自动化流程
- ✅ 支持动态类别

---

## 🔍 查看生成的 data.yaml

训练任务创建后，您可以在任务目录下找到自动生成的 `auto_generated_data.yaml` 文件：

```bash
# 获取任务状态
curl http://localhost:8000/api/v1/training/tasks/{task_id}

# 响应中包含 log_file 路径
{
  "task": {
    "log_file": "E:/yolo_workdir/train_tasks/{task_id}/training.log",
    ...
  }
}

# data.yaml 位于同一目录下
# E:/yolo_workdir/train_tasks/{task_id}/auto_generated_data.yaml
```

---

## 🧪 完整测试流程

```python
import requests
import time

BASE_URL = "http://localhost:8000"

# 1. 创建训练任务
print("1. 创建训练任务...")
response = requests.post(
    f"{BASE_URL}/api/v1/training/tasks/no-yaml",
    json={
        "train_dir": "E:/datasets/coco128/images/train2017",
        "val_dir": "E:/datasets/coco128/images/val2017",
        "classes": ["person", "bicycle", "car", "motorcycle"],
        "epochs": 10,
        "batch_size": 4,
        "image_size": 640
    }
)
task_id = response.json()["task_id"]
print(f"   任务ID: {task_id}")

# 2. 启动训练
print("2. 启动训练...")
requests.post(f"{BASE_URL}/api/v1/training/tasks/{task_id}/start")

# 3. 监控训练进度
print("3. 监控训练进度...")
for i in range(5):
    time.sleep(10)
    status_response = requests.get(
        f"{BASE_URL}/api/v1/training/tasks/{task_id}"
    )
    task_info = status_response.json()["task"]
    print(f"   Epoch: {task_info['current_epoch']}/{task_info['total_epochs']}")
    print(f"   进度: {task_info['progress']}%")
    print(f"   状态: {task_info['status']}")
    
    if task_info['status'] in ['completed', 'failed']:
        break

print("训练完成！")
```

---

## ❓ 常见问题

### Q1: 如何确认 data.yaml 已正确生成？

**A**: 查看任务状态响应中的 `log_file` 路径，`auto_generated_data.yaml` 位于同一目录。或者查看训练日志，会显示生成的 YAML 文件路径。

### Q2: 可以修改自动生成的 data.yaml 吗？

**A**: 不建议手动修改。如果需要自定义配置，建议使用传统的 `/api/v1/training/tasks` 接口并提供自己的 data.yaml 文件。

### Q3: 类别顺序会影响训练吗？

**A**: 会的。`classes` 列表的顺序决定了类别索引（0, 1, 2...）。确保标签文件中的类别索引与此顺序一致。

### Q4: 如果 nc 和 classes 长度不一致怎么办？

**A**: 系统会抛出错误。建议不指定 `nc`，让系统自动根据 `classes` 列表长度计算。

### Q5: 支持相对路径吗？

**A**: 支持，但建议使用绝对路径以避免混淆。相对路径是相对于 API 服务的工作目录。

---

## 📞 技术支持

如有问题，请：
1. 检查目录路径是否正确
2. 确认标签文件格式正确
3. 查看训练日志了解详细错误信息
4. 提交 Issue 反馈问题

---

**祝您训练顺利！** 🚀

# YOLO 训练和预测 Web API 服务

## 📋 项目简介

这是一个基于 FastAPI 框架开发的 YOLOv7 训练和预测 Web API 服务，提供 RESTful API 接口用于：
- 🎯 **模型训练**：异步任务管理、实时监控、进度追踪
- 🔍 **图像预测**：单图/批量预测、结果导出、可视化标注
- 📊 **任务管理**：任务队列、状态查询、日志查看
- 🌐 **服务注册**：Nacos 服务发现与治理

---

## 🏗️ 项目结构

```
yolo/
├── 📦 核心模块
│   ├── config.py                  # 统一配置管理（含 Nacos 配置）
│   ├── nacos_registry.py          # Nacos 服务注册模块
│   ├── yolo_trainer.py            # 训练任务管理器
│   ├── yolo_predictor.py          # 预测服务封装
│   └── yolo_api.py                # FastAPI Web 服务入口
│
├── 🧪 测试工具
│   └── test_api.py                # API 自动化测试脚本
│
├── 🚀 启动脚本
│   ├── start_api.bat              # Windows 一键启动
│   └── start_api.sh               # Linux/Mac 启动
│
├── 📋 配置文件
│   └── requirements_api.txt       # Python 依赖包
│
├── 📚 完整文档
│   ├── README.md                  # 本文件 - 项目总览
│   ├── QUICKSTART.md              # 5分钟快速入门
│   ├── API_DOCUMENTATION.md       # 完整 API 文档
│   ├── NACOS_CONFIG.md            # Nacos 配置指南
│   ├── NACOS_INTEGRATION_REPORT.md # Nacos 集成报告
│   └── PROJECT_CHECKLIST.md       # 项目交付清单
│
└── 📁 子目录
    ├── build/                     # 数据集构建工具
    ├── predict/                   # 原有预测脚本（保留）
    └── train/                     # 原有训练脚本（保留）
        └── train_yolov7_final.py  # YOLOv7 训练脚本
```

### 目录说明

- **yolo/** - 新开发的 Web API 服务（主要工作目录）
- **yolo/train/** - 原有的训练脚本（保留供参考）
- **yolo/predict/** - 原有的预测脚本（保留供参考）
- **yolo/build/** - 数据集构建工具

---

## ✨ 核心特性

### 🚀 训练功能
- ✅ 异步训练任务（后台执行，不阻塞 API）
- ✅ 实时进度监控（Epoch、Loss、mAP）
- ✅ 任务队列管理（创建、启动、取消）
- ✅ 训练日志实时查看
- ✅ 多任务并行训练
- ✅ 自动保存模型权重

### 🔍 预测功能
- ✅ 单图实时预测
- ✅ 支持多种图像格式（JPG/PNG/BMP/TIFF）
- ✅ 可配置置信度和 IOU 阈值
- ✅ 检测结果可视化（带标注框）
- ✅ 结果导出（JSON/COCO 格式）
- ✅ 预测器实例管理

### 🌐 服务治理（Nacos）⭐新增
- ✅ 自动服务注册到 Nacos
- ✅ 心跳检测与健康检查
- ✅ 优雅关闭自动注销
- ✅ 服务发现支持
- ✅ 多实例负载均衡
- ✅ 命名空间隔离

### 🛠️ 系统功能
- ✅ RESTful API 设计
- ✅ Swagger UI 交互式文档
- ✅ CORS 跨域支持
- ✅ 健康检查接口
- ✅ 系统信息查询
- ✅ 完善的错误处理

---

## 🚦 快速开始

### 1️⃣ 安装依赖

```bash
cd e:\doc\jnet\python\yolo
pip install -r requirements_api.txt
```

**Nacos SDK 会自动安装**，或手动安装：
```bash
pip install nacos-sdk-python
```

### 2️⃣ 配置 Nacos（可选）

编辑 `config.py`，修改 Nacos 服务器地址：

```python
NACOS_ENABLED = True                      # 启用 Nacos 注册
NACOS_SERVER_ADDR = "192.168.52.108:8848" # Nacos 服务器地址
NACOS_USERNAME = "nacos"                  # 用户名
NACOS_PASSWORD = "nacos"                  # 密码
```

**如果不需要 Nacos，设置：**
```python
NACOS_ENABLED = False
```

### 3️⃣ 启动服务

**Windows:**
```bash
start_api.bat
```

**Linux/Mac:**
```bash
chmod +x start_api.sh
./start_api.sh
```

启动成功后会显示：
```
正在注册到 Nacos...
✓ Nacos 注册成功
  服务地址: http://192.168.1.100:8000
```

### 4️⃣ 访问 API

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **健康检查**: http://localhost:8000/health
- **Nacos 控制台**: http://192.168.52.108:8848/nacos

---

## 📖 使用示例

### Python 示例

```python
import requests

BASE_URL = "http://localhost:8000"

# 1. 创建训练任务
response = requests.post(f"{BASE_URL}/api/v1/training/tasks", json={
    "dataset_yaml": "E:/data/dataset/data.yaml",
    "epochs": 100,
    "batch_size": 4,
    "image_size": 640,
    "device": "0"
})
task_id = response.json()["task_id"]

# 2. 启动训练
requests.post(f"{BASE_URL}/api/v1/training/tasks/{task_id}/start")

# 3. 创建预测器
response = requests.post(f"{BASE_URL}/api/v1/prediction/predictors", json={
    "model_path": "E:/models/best.pt",
    "device": "0"
})
predictor_id = response.json()["predictor_id"]

# 4. 执行预测
with open("test.jpg", "rb") as f:
    response = requests.post(
        f"{BASE_URL}/api/v1/prediction/predict",
        params={"predictor_id": predictor_id},
        files={"file": f}
    )
    print(response.json())
```

### cURL 示例

```bash
# 创建训练任务
curl -X POST "http://localhost:8000/api/v1/training/tasks" \
  -H "Content-Type: application/json" \
  -d '{"dataset_yaml":"E:/data/data.yaml","epochs":100,"batch_size":4}'

# 执行预测
curl -X POST "http://localhost:8000/api/v1/prediction/predict?predictor_id=xxx" \
  -F "file=@test.jpg"
```

---

## 🔌 API 接口概览

### 训练相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/training/tasks` | 创建训练任务 |
| POST | `/api/v1/training/tasks/{id}/start` | 启动训练任务 |
| GET | `/api/v1/training/tasks` | 列出所有任务 |
| GET | `/api/v1/training/tasks/{id}` | 获取任务状态 |
| POST | `/api/v1/training/tasks/{id}/cancel` | 取消任务 |
| GET | `/api/v1/training/tasks/{id}/log` | 获取训练日志 |

### 预测相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/prediction/predictors` | 创建预测器 |
| POST | `/api/v1/prediction/predict` | 执行图像预测 |
| GET | `/api/v1/prediction/results/{id}` | 获取预测结果 |
| GET | `/api/v1/prediction/results` | 列出所有结果 |
| GET | `/api/v1/prediction/results/{id}/download` | 下载结果图像 |

### 系统相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | API 信息（含 Nacos 状态） |
| GET | `/health` | 健康检查（含 Nacos 状态） |
| GET | `/api/v1/system/info` | 系统信息（含 Nacos 配置） |

---

## 🌐 Nacos 服务注册

### 配置说明

在 `config.py` 中配置 Nacos：

```python
# 是否启用 Nacos 注册
NACOS_ENABLED = True

# Nacos 服务器地址
NACOS_SERVER_ADDR = "192.168.52.108:8848"

# 服务信息
SERVICE_NAME = "yolo-training-prediction-service"
SERVICE_GROUP = "DEFAULT_GROUP"
SERVICE_CLUSTER = "DEFAULT"
```

### 查看服务状态

1. **Nacos 控制台**：http://192.168.52.108:8848/nacos
   - 进入 **服务管理** -> **服务列表**
   - 查找服务：`yolo-training-prediction-service`

2. **API 健康检查**：
   ```bash
   curl http://localhost:8000/health
   ```
   
   响应：
   ```json
   {
     "status": "healthy",
     "gpu_available": true,
     "nacos_registered": true
   }
   ```

### 详细配置指南

查看 [NACOS_CONFIG.md](NACOS_CONFIG.md) 获取完整的 Nacos 配置和使用说明。

---

## ⚙️ 配置说明

编辑 `config.py` 自定义配置：

```python
# API 服务配置
API_HOST = "0.0.0.0"
API_PORT = 8000

# Nacos 配置
NACOS_ENABLED = True
NACOS_SERVER_ADDR = "192.168.52.108:8848"

# 训练默认配置
DEFAULT_EPOCHS = 300
DEFAULT_BATCH_SIZE = 4
DEFAULT_IMAGE_SIZE = 1280

# 预测默认配置
DEFAULT_CONF_THRES = 0.25
DEFAULT_IOU_THRES = 0.45

# 文件上传限制
MAX_UPLOAD_SIZE = 100 * 1024 * 1024  # 100MB
```

---

## 🧪 运行测试

```bash
python test_api.py
```

测试脚本会自动验证所有 API 接口的功能。

---

## 📊 架构设计

```
┌─────────────┐
│   Client    │  (浏览器/Python/cURL)
└──────┬──────┘
       │ HTTP/REST
       ▼
┌─────────────┐
│  FastAPI    │  (yolo_api.py)
│   Server    │
└──┬──────┬───┘
   │      │
   ▼      ▼
┌──────┐ ┌──────────┐
│Trainer│ │Predictor │
│Manager│ │ Manager  │
└──┬───┘ └────┬─────┘
   │          │
   ▼          ▼
┌──────┐ ┌──────────┐
│YOLOv7│ │ YOLOv7   │
│Train │ │ Predict  │
└──────┘ └──────────┘
       │
       ▼
┌─────────────┐
│   Nacos     │  (服务注册与发现)
│  Registry   │
└─────────────┘
```

---

## 🔒 生产环境建议

1. **身份认证**：添加 JWT/OAuth2 认证
2. **HTTPS**：启用 SSL/TLS 加密
3. **速率限制**：防止 API 滥用
4. **日志记录**：集成 ELK/Prometheus
5. **数据库**：持久化任务状态
6. **负载均衡**：多实例部署 + Nacos 负载均衡
7. **监控告警**：集成 Sentry/DataDog
8. **Nacos 高可用**：配置多个 Nacos 服务器地址

---

## 🛠️ 技术栈

- **Web 框架**: FastAPI
- **ASGI 服务器**: Uvicorn
- **数据验证**: Pydantic
- **深度学习**: PyTorch + YOLOv7
- **图像处理**: OpenCV + Pillow
- **服务注册**: Nacos SDK
- **文档生成**: Swagger UI / ReDoc

---

## 📝 开发计划

- [ ] 批量预测支持
- [ ] WSI 全片预测
- [ ] 视频流预测
- [ ] 模型自动调优
- [ ] 分布式训练
- [ ] WebSocket 实时推送
- [ ] 用户权限管理
- [ ] 任务调度优化
- [x] Nacos 服务注册 ⭐已完成

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目遵循 MIT 许可证。

---

## 📞 联系方式

- 📧 Email: your-email@example.com
- 💬 Issues: [GitHub Issues](https://github.com/your-repo/issues)

---

## 🙏 致谢

- [FastAPI](https://fastapi.tiangolo.com/)
- [YOLOv7](https://github.com/WongKinYiu/yolov7)
- [Nacos](https://nacos.io/)
- [OpenSlide](https://openslide.org/)

---

**祝你使用愉快！** 🎉

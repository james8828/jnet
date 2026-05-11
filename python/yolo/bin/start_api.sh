#!/bin/bash
# YOLO Web API 启动脚本 (Linux/Mac)

echo "========================================"
echo "  YOLO Training & Prediction API Service"
echo "========================================"
echo ""

# 切换到项目根目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.." || exit 1

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo "[ERROR] Python3 未安装"
    exit 1
fi

echo "[INFO] 检查依赖包..."
if ! python3 -c "import fastapi" 2>/dev/null; then
    echo "[WARN] FastAPI 未安装，正在安装依赖..."
    pip3 install -r bin/requirements_api.txt
    if [ $? -ne 0 ]; then
        echo "[ERROR] 依赖安装失败"
        exit 1
    fi
fi

# 检查 Nacos SDK
if ! python3 -c "import nacos" 2>/dev/null; then
    echo "[WARN] Nacos SDK 未安装，正在安装..."
    pip3 install nacos-sdk-python
    if [ $? -ne 0 ]; then
        echo "[WARN] Nacos SDK 安装失败，将禁用 Nacos 注册功能"
    else
        echo "[INFO] Nacos SDK 安装成功"
    fi
fi

echo ""
echo "[INFO] 启动 API 服务..."
echo "[INFO] API 文档: http://localhost:8000/docs"
echo "[INFO] ReDoc: http://localhost:8000/redoc"
echo "[INFO] 按 Ctrl+C 停止服务"
echo ""

python3 yolo_api.py

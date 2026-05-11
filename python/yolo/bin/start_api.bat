@echo off
REM YOLO Web API 启动脚本 (Windows)

echo ========================================
echo   YOLO Training ^& Prediction API Service
echo ========================================
echo.

REM 切换到项目根目录
cd /d "%~dp0.."

REM 检查 Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python 未安装或未添加到 PATH
    pause
    exit /b 1
)

echo [INFO] 检查依赖包...
pip show fastapi >nul 2>&1
if errorlevel 1 (
    echo [WARN] FastAPI 未安装，正在安装依赖...
    pip install -r bin\requirements_api.txt
    if errorlevel 1 (
        echo [ERROR] 依赖安装失败
        pause
        exit /b 1
    )
)

REM 检查 Nacos SDK
pip show nacos-sdk-python >nul 2>&1
if errorlevel 1 (
    echo [WARN] Nacos SDK 未安装，正在安装...
    pip install nacos-sdk-python
    if errorlevel 1 (
        echo [WARN] Nacos SDK 安装失败，将禁用 Nacos 注册功能
    ) else (
        echo [INFO] Nacos SDK 安装成功
    )
)

echo.
echo [INFO] 启动 API 服务...
echo [INFO] API 文档: http://localhost:8000/docs
echo [INFO] ReDoc: http://localhost:8000/redoc
echo [INFO] 按 Ctrl+C 停止服务
echo.

python yolo_api.py

pause

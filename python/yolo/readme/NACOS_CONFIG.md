# Nacos 服务注册配置指南

## 📋 概述

YOLO Web API 已集成 Nacos 服务注册功能，可以自动将服务注册到 Nacos 注册中心，便于服务发现和负载均衡。

---

## 🔧 配置说明

### 1. Nacos 服务器配置

在 `config.py` 中修改以下配置：

```python
# Nacos 服务注册配置
NACOS_ENABLED = True                      # 是否启用 Nacos 注册
NACOS_SERVER_ADDR = "192.168.52.108:8848" # Nacos 服务器地址
NACOS_NAMESPACE = ""                      # 命名空间ID（空表示public）
NACOS_USERNAME = "nacos"                  # 用户名
NACOS_PASSWORD = "nacos"                  # 密码

# 服务注册信息
SERVICE_NAME = "yolo-training-prediction-service"  # 服务名称
SERVICE_GROUP = "DEFAULT_GROUP"           # 服务分组
SERVICE_CLUSTER = "DEFAULT"               # 集群名称
SERVICE_VERSION = "1.0.0"                 # 服务版本
SERVICE_WEIGHT = 1.0                      # 权重
SERVICE_EPHEMERAL = True                  # 临时实例
```

### 2. 禁用 Nacos 注册

如果不需要 Nacos 注册，设置：

```python
NACOS_ENABLED = False
```

---

## 🚀 快速开始

### 第一步：安装 Nacos SDK

```bash
pip install nacos-sdk-python
```

或使用启动脚本自动安装：

```bash
# Windows
start_api.bat

# Linux/Mac
./start_api.sh
```

### 第二步：配置 Nacos 服务器

编辑 `config.py`，确保 Nacos 服务器地址正确：

```python
NACOS_SERVER_ADDR = "192.168.52.108:8848"
```

### 第三步：启动服务

```bash
python yolo_api.py
```

启动成功后，你会看到类似输出：

```
================================================================================
正在注册到 Nacos...
[NACOS] 正在连接到 Nacos 服务器: 192.168.52.108:8848
[NACOS] ✓ 服务注册成功
  - 服务名称: yolo-training-prediction-service
  - 实例地址: 192.168.1.100:8000
  - 集群: DEFAULT
  - 分组: DEFAULT_GROUP
  - Nacos 服务器: 192.168.52.108:8848
[NACOS] 心跳检测已启动
✓ Nacos 注册成功
  服务地址: http://192.168.1.100:8000
================================================================================
```

---

## 📊 查看服务状态

### 方法 1：Nacos 控制台

1. 打开浏览器访问：http://192.168.52.108:8848/nacos
2. 登录（默认用户名/密码：nacos/nacos）
3. 进入 **服务管理** -> **服务列表**
4. 查找服务：`yolo-training-prediction-service`
5. 查看实例详情和健康状态

### 方法 2：API 查询

```bash
# 查询服务列表
curl -X GET "http://192.168.52.108:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10"

# 查询服务实例
curl -X GET "http://192.168.52.108:8848/nacos/v1/ns/instance/list?serviceName=yolo-training-prediction-service"
```

### 方法 3：通过 API 健康检查接口

```bash
curl http://localhost:8000/health
```

响应中包含 Nacos 注册状态：

```json
{
  "status": "healthy",
  "gpu_available": true,
  "nacos_registered": true
}
```

---

## ⚙️ 高级配置

### 1. 自定义命名空间

如果需要隔离不同环境的服务：

```python
NACOS_NAMESPACE = "dev"      # 开发环境
# 或
NACOS_NAMESPACE = "prod"     # 生产环境
```

在 Nacos 控制台创建对应的命名空间后，使用其 UUID。

### 2. 多实例部署

在同一台机器上运行多个实例（不同端口）：

```bash
# 实例 1
API_PORT=8000 python yolo_api.py

# 实例 2
API_PORT=8001 python yolo_api.py
```

两个实例会自动注册到 Nacos，实现负载均衡。

### 3. 调整心跳间隔

```python
HEALTH_CHECK_INTERVAL = 5  # 心跳间隔（秒）
HEALTH_CHECK_TIMEOUT = 3   # 心跳超时（秒）
```

### 4. 永久实例 vs 临时实例

```python
SERVICE_EPHEMERAL = True   # 临时实例（推荐，服务停止后自动注销）
# 或
SERVICE_EPHEMERAL = False  # 永久实例（需要手动注销）
```

---

## 🔍 故障排查

### 问题 1：注册失败 - 连接超时

**错误信息：**
```
[NACOS] ✗ 服务注册异常: Connection timed out
```

**解决方案：**
1. 检查 Nacos 服务器是否运行：`http://192.168.52.108:8848/nacos`
2. 检查网络连接和防火墙设置
3. 确认 Nacos 服务器地址和端口正确
4. 测试连通性：`telnet 192.168.52.108 8848`

### 问题 2：认证失败

**错误信息：**
```
[NACOS] ✗ 服务注册异常: Authentication failed
```

**解决方案：**
1. 检查用户名和密码是否正确
2. 确认 Nacos 开启了认证（Nacos 2.x 默认开启）
3. 更新配置：
   ```python
   NACOS_USERNAME = "your_username"
   NACOS_PASSWORD = "your_password"
   ```

### 问题 3：SDK 未安装

**错误信息：**
```
[NACOS] ✗ nacos-sdk-python 未安装
```

**解决方案：**
```bash
pip install nacos-sdk-python
```

### 问题 4：服务注册成功但控制台看不到

**可能原因：**
1. 命名空间不匹配
2. 分组名称不匹配

**解决方案：**
1. 确认 `NACOS_NAMESPACE` 与 Nacos 控制台中的命名空间一致
2. 确认 `SERVICE_GROUP` 正确
3. 在 Nacos 控制台切换正确的命名空间查看

---

## 🛡️ 优雅关闭

服务支持优雅关闭，会自动从 Nacos 注销：

### 方法 1：Ctrl+C

在终端按 `Ctrl+C`，服务会：
1. 停止接收新请求
2. 从 Nacos 注销实例
3. 等待现有请求完成
4. 退出进程

### 方法 2：发送信号

```bash
# Linux/Mac
kill -TERM <pid>

# Windows
taskkill /PID <pid> /T
```

---

## 📝 服务元数据

注册到 Nacos 的服务包含以下元数据：

```json
{
  "version": "1.0.0",
  "description": "YOLO Training & Prediction API Service",
  "health_check_path": "/health"
}
```

可以在 Nacos 控制台查看和编辑这些元数据。

---

## 🔄 服务发现示例

### Python 客户端

```python
import nacos

# 创建客户端
client = nacos.NacosClient(
    server_addresses="192.168.52.108:8848",
    namespace="",
    username="nacos",
    password="nacos"
)

# 获取服务实例列表
instances = client.get_naming_instance(
    service_name="yolo-training-prediction-service",
    group_name="DEFAULT_GROUP",
    clusters="DEFAULT"
)

print(f"可用实例数: {len(instances['hosts'])}")
for instance in instances['hosts']:
    print(f"  - {instance['ip']}:{instance['port']} (权重: {instance['weight']})")
```

### Java 客户端

```java
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

NamingService namingService = NamingFactory.createNamingService("192.168.52.108:8848");
List<Instance> instances = namingService.getAllInstances("yolo-training-prediction-service");

for (Instance instance : instances) {
    System.out.println(instance.getIp() + ":" + instance.getPort());
}
```

---

## 💡 最佳实践

1. **生产环境**：
   - 使用永久实例（`SERVICE_EPHEMERAL = False`）
   - 配置多个 Nacos 服务器地址实现高可用
   - 启用 HTTPS

2. **开发环境**：
   - 使用临时实例（`SERVICE_EPHEMERAL = True`）
   - 使用 public 命名空间
   - 简化配置

3. **多环境隔离**：
   - 为每个环境创建独立的命名空间
   - 使用不同的服务分组

4. **监控告警**：
   - 监控 Nacos 注册状态
   - 设置实例健康检查告警
   - 记录服务注册/注销日志

---

## 📞 技术支持

如有问题，请检查：
1. Nacos 服务器日志：`/nacos/logs/nacos.log`
2. 应用日志：查看 `[NACOS]` 标签的日志
3. 网络连通性：`ping 192.168.52.108`
4. 端口可达性：`telnet 192.168.52.108 8848`

---

**配置完成！享受服务治理带来的便利！** 🎉

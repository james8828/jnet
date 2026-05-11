"""
Nacos 服务注册模块
负责将 YOLO API 服务注册到 Nacos 注册中心
"""
import sys
import time
import socket
import logging
from typing import Optional
from pathlib import Path

# 添加当前目录到路径
sys.path.insert(0, str(Path(__file__).parent))

from config import config

logger = logging.getLogger(__name__)


class NacosRegistry:
    """Nacos 服务注册器"""
    
    def __init__(self):
        self.client = None
        self.service_name = config.SERVICE_NAME
        self.ip = self._get_local_ip()
        self.port = config.API_PORT
        self.registered = False
        
    def _get_local_ip(self) -> str:
        """获取本机 IP 地址"""
        try:
            # 创建一个 UDP socket
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            # 如果失败，返回 localhost
            return "127.0.0.1"
    
    def register(self) -> bool:
        """
        注册服务到 Nacos
        
        Returns:
            bool: 是否注册成功
        """
        if not config.NACOS_ENABLED:
            logger.info("[NACOS] Nacos 注册已禁用")
            return False
        
        try:
            import nacos
            
            logger.info(f"[NACOS] 正在连接到 Nacos 服务器: {config.NACOS_SERVER_ADDR}")
            
            # 创建 Nacos 客户端
            self.client = nacos.NacosClient(
                server_addresses=config.NACOS_SERVER_ADDR,
                namespace=config.NACOS_NAMESPACE,
                username=config.NACOS_USERNAME,
                password=config.NACOS_PASSWORD
            )
            
            # 注册服务实例
            success = self.client.add_naming_instance(
                service_name=self.service_name,
                ip=self.ip,
                port=self.port,
                cluster_name=config.SERVICE_CLUSTER,
                group_name=config.SERVICE_GROUP,
                weight=config.SERVICE_WEIGHT,
                ephemeral=config.SERVICE_EPHEMERAL,
                metadata={
                    "version": config.SERVICE_VERSION,
                    "description": "YOLO Training & Prediction API Service",
                    "health_check_path": config.HEALTH_CHECK_PATH
                }
            )
            
            if success:
                self.registered = True
                logger.info(f"[NACOS] ✓ 服务注册成功")
                logger.info(f"  - 服务名称: {self.service_name}")
                logger.info(f"  - 实例地址: {self.ip}:{self.port}")
                logger.info(f"  - 集群: {config.SERVICE_CLUSTER}")
                logger.info(f"  - 分组: {config.SERVICE_GROUP}")
                logger.info(f"  - Nacos 服务器: {config.NACOS_SERVER_ADDR}")
                
                # 启动心跳检测
                self._start_heartbeat()
                
                return True
            else:
                logger.error("[NACOS] ✗ 服务注册失败")
                return False
                
        except ImportError:
            logger.error("[NACOS] ✗ nacos-sdk-python 未安装")
            logger.error("  请运行: pip install nacos-sdk-python")
            return False
            
        except Exception as e:
            logger.error(f"[NACOS] ✗ 服务注册异常: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def deregister(self) -> bool:
        """
        从 Nacos 注销服务
        
        Returns:
            bool: 是否注销成功
        """
        if not self.registered or not self.client:
            return False
        
        try:
            success = self.client.remove_naming_instance(
                service_name=self.service_name,
                ip=self.ip,
                port=self.port,
                cluster_name=config.SERVICE_CLUSTER,
                group_name=config.SERVICE_GROUP,
                ephemeral=config.SERVICE_EPHEMERAL
            )
            
            if success:
                self.registered = False
                logger.info(f"[NACOS] ✓ 服务已注销")
                return True
            else:
                logger.error("[NACOS] ✗ 服务注销失败")
                return False
                
        except Exception as e:
            logger.error(f"[NACOS] ✗ 服务注销异常: {e}")
            return False
    
    def _start_heartbeat(self):
        """启动心跳检测（后台线程）"""
        import threading
        
        def heartbeat_loop():
            while self.registered:
                try:
                    # 发送心跳
                    self.client.send_heartbeat(
                        service_name=self.service_name,
                        ip=self.ip,
                        port=self.port,
                        cluster_name=config.SERVICE_CLUSTER,
                        group_name=config.SERVICE_GROUP,
                        weight=config.SERVICE_WEIGHT
                    )
                    logger.debug("[NACOS] 心跳发送成功")
                except Exception as e:
                    logger.warning(f"[NACOS] 心跳发送失败: {e}")
                
                # 等待下一个心跳周期
                time.sleep(config.HEALTH_CHECK_INTERVAL)
        
        # 启动后台心跳线程
        heartbeat_thread = threading.Thread(target=heartbeat_loop, daemon=True)
        heartbeat_thread.start()
        logger.info("[NACOS] 心跳检测已启动")
    
    def get_service_url(self) -> str:
        """获取服务 URL"""
        return f"http://{self.ip}:{self.port}"
    
    def is_registered(self) -> bool:
        """检查服务是否已注册"""
        return self.registered


# 全局 Nacos 注册器实例
nacos_registry = NacosRegistry()

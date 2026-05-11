"""
YOLO 训练任务管理器
负责管理训练任务的启动、监控和状态查询
"""
import os
import sys
import json
import time
import uuid
import subprocess
import threading
from pathlib import Path
from datetime import datetime
from typing import Optional, Dict, Any
from enum import Enum

from config import config


class TrainingStatus(Enum):
    """训练状态枚举"""
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class TrainingTask:
    """训练任务类"""
    
    def __init__(self, task_id: str, config_dict: Dict[str, Any]):
        self.task_id = task_id
        self.config = config_dict
        self.status = TrainingStatus.PENDING
        self.progress = 0.0  # 0-100
        self.current_epoch = 0
        self.total_epochs = config_dict.get('epochs', 300)
        self.start_time = None
        self.end_time = None
        self.error_message = None
        self.result_dir = None
        self.process = None
        self.log_file = None
        
        # 初始化任务目录
        self.task_dir = config.get_train_task_dir(task_id)
        self.log_file = self.task_dir / "training.log"
        
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "task_id": self.task_id,
            "status": self.status.value,
            "progress": self.progress,
            "current_epoch": self.current_epoch,
            "total_epochs": self.total_epochs,
            "config": self.config,
            "start_time": self.start_time.isoformat() if self.start_time else None,
            "end_time": self.end_time.isoformat() if self.end_time else None,
            "error_message": self.error_message,
            "result_dir": str(self.result_dir) if self.result_dir else None,
            "log_file": str(self.log_file) if self.log_file else None
        }
    
    def save_metadata(self):
        """保存任务元数据"""
        metadata_file = self.task_dir / "metadata.json"
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)


class TrainingManager:
    """训练任务管理器"""
    
    def __init__(self):
        self.tasks: Dict[str, TrainingTask] = {}
        self.lock = threading.Lock()
        
    def create_task(self, training_config: Dict[str, Any]) -> str:
        """
        创建新的训练任务
        
        Args:
            training_config: 训练配置字典
            
        Returns:
            task_id: 任务ID
        """
        task_id = str(uuid.uuid4())[:8]
        
        # 验证配置
        self._validate_config(training_config)
        
        # 创建任务对象
        task = TrainingTask(task_id, training_config)
        
        with self.lock:
            self.tasks[task_id] = task
        
        # 保存元数据
        task.save_metadata()
        
        print(f"[TRAIN] 创建训练任务: {task_id}")
        return task_id
    
    def start_training(self, task_id: str) -> bool:
        """
        启动训练任务
        
        Args:
            task_id: 任务ID
            
        Returns:
            是否成功启动
        """
        with self.lock:
            if task_id not in self.tasks:
                raise ValueError(f"任务不存在: {task_id}")
            
            task = self.tasks[task_id]
            
            if task.status != TrainingStatus.PENDING:
                raise ValueError(f"任务状态不允许启动: {task.status.value}")
        
        # 在后台线程中启动训练
        thread = threading.Thread(target=self._run_training, args=(task_id,), daemon=True)
        thread.start()
        
        return True
    
    def _run_training(self, task_id: str):
        """
        执行训练任务（后台线程）
        
        Args:
            task_id: 任务ID
        """
        task = self.tasks[task_id]
        
        try:
            # 更新状态
            task.status = TrainingStatus.RUNNING
            task.start_time = datetime.now()
            task.save_metadata()
            
            print(f"[TRAIN] 开始训练任务: {task_id}")
            
            # 构建训练命令
            cmd = self._build_training_command(task)
            
            # 启动训练进程
            task.process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
                cwd=str(config.YOLOV7_ROOT)
            )
            
            # 实时监控训练日志
            self._monitor_training_progress(task)
            
            # 等待进程完成
            return_code = task.process.wait()
            
            if return_code == 0:
                task.status = TrainingStatus.COMPLETED
                task.progress = 100.0
                task.result_dir = task.task_dir / "weights"
                print(f"[TRAIN] 训练任务完成: {task_id}")
            else:
                task.status = TrainingStatus.FAILED
                task.error_message = f"训练进程异常退出，返回码: {return_code}"
                print(f"[TRAIN] 训练任务失败: {task_id}, 返回码: {return_code}")
                
        except Exception as e:
            task.status = TrainingStatus.FAILED
            task.error_message = str(e)
            print(f"[TRAIN] 训练任务异常: {task_id}, 错误: {e}")
            
        finally:
            task.end_time = datetime.now()
            task.save_metadata()
    
    def _build_training_command(self, task: TrainingTask) -> list:
        """
        构建训练命令
        
        Args:
            task: 训练任务对象
            
        Returns:
            命令列表
        """
        cfg = task.config
        
        # 检查是否为无 yaml 模式，如果是则生成 data.yaml
        if cfg.get('no_yaml_mode', False):
            dataset_yaml = self._generate_dataset_yaml(task)
        else:
            dataset_yaml = cfg.get('dataset_yaml', str(config.DEFAULT_DATASET_YAML))
        
        cmd = [
            sys.executable,
            str(config.YOLOV7_ROOT / "train.py"),
            '--weights', cfg.get('weights', config.DEFAULT_WEIGHTS),
            '--cfg', 'cfg/training/yolov7.yaml',
            '--data', dataset_yaml,
            '--epochs', str(cfg.get('epochs', config.DEFAULT_EPOCHS)),
            '--batch-size', str(cfg.get('batch_size', config.DEFAULT_BATCH_SIZE)),
            '--img-size', str(cfg.get('image_size', config.DEFAULT_IMAGE_SIZE)),
            str(cfg.get('image_size', config.DEFAULT_IMAGE_SIZE)),
            '--device', cfg.get('device', config.DEFAULT_DEVICE),
            '--project', str(task.task_dir),
            '--name', 'training',
            '--workers', str(cfg.get('workers', 4)),
            '--hyp', cfg.get('hyp', config.DEFAULT_HYP)
        ]
        
        # 可选参数
        if cfg.get('use_adam', False):
            cmd.append('--adam')
        
        if cfg.get('cache', False):
            cmd.append('--cache')
        
        return cmd
    
    def _generate_dataset_yaml(self, task: TrainingTask) -> str:
        """
        自动生成数据集 YAML 配置文件
        
        Args:
            task: 训练任务对象
            
        Returns:
            生成的 data.yaml 文件路径
        """
        import yaml
        
        cfg = task.config
        
        # 构建 data.yaml 内容
        data_config = {
            'train': cfg['train_dir'],
            'val': cfg['val_dir'],
            'nc': cfg['nc'],
            'names': cfg['classes']
        }
        
        # 如果有测试集，添加 test 字段
        if cfg.get('test_dir'):
            data_config['test'] = cfg['test_dir']
        
        # 保存 data.yaml 到任务目录
        yaml_path = task.task_dir / "auto_generated_data.yaml"
        
        with open(yaml_path, 'w', encoding='utf-8') as f:
            yaml.dump(data_config, f, allow_unicode=True, default_flow_style=False)
        
        print(f"[TRAIN] 已自动生成 data.yaml: {yaml_path}")
        print(f"[TRAIN] 类别数量: {cfg['nc']}, 类别: {cfg['classes']}")
        
        return str(yaml_path)
    
    def _monitor_training_progress(self, task: TrainingTask):
        """
        监控训练进度
        
        Args:
            task: 训练任务对象
        """
        log_lines = []
        
        for line in iter(task.process.stdout.readline, ''):
            if not line:
                break
            
            # 写入日志文件
            with open(task.log_file, 'a', encoding='utf-8') as f:
                f.write(line)
            
            log_lines.append(line)
            
            # 解析训练进度
            self._parse_training_log(task, line)
            
            # 定期保存元数据
            if task.current_epoch % 10 == 0:
                task.save_metadata()
    
    def _parse_training_log(self, task: TrainingTask, log_line: str):
        """
        解析训练日志，提取进度信息
        
        Args:
            task: 训练任务对象
            log_line: 日志行
        """
        # 示例日志格式: "Epoch 1/300" 或 "1/300"
        import re
        
        # 匹配 epoch 信息
        epoch_match = re.search(r'Epoch\s+(\d+)/(\d+)', log_line)
        if epoch_match:
            current = int(epoch_match.group(1))
            total = int(epoch_match.group(2))
            task.current_epoch = current
            task.total_epochs = total
            task.progress = (current / total) * 100
        
        # 也匹配简写格式 "1/300"
        elif '/' in log_line and not epoch_match:
            parts = log_line.split('/')
            if len(parts) >= 2:
                try:
                    current = int(parts[0].strip().split()[-1])
                    total = int(parts[1].strip().split()[0])
                    if current <= total:
                        task.current_epoch = current
                        task.total_epochs = total
                        task.progress = (current / total) * 100
                except (ValueError, IndexError):
                    pass
    
    def get_task_status(self, task_id: str) -> Dict[str, Any]:
        """
        获取任务状态
        
        Args:
            task_id: 任务ID
            
        Returns:
            任务状态字典
        """
        with self.lock:
            if task_id not in self.tasks:
                raise ValueError(f"任务不存在: {task_id}")
            
            return self.tasks[task_id].to_dict()
    
    def list_tasks(self, status_filter: Optional[str] = None) -> list:
        """
        列出所有任务
        
        Args:
            status_filter: 状态过滤器（可选）
            
        Returns:
            任务列表
        """
        with self.lock:
            tasks = list(self.tasks.values())
            
            if status_filter:
                tasks = [t for t in tasks if t.status.value == status_filter]
            
            # 按创建时间倒序排列
            tasks.sort(key=lambda t: t.start_time or datetime.min, reverse=True)
            
            return [t.to_dict() for t in tasks]
    
    def cancel_task(self, task_id: str) -> bool:
        """
        取消训练任务
        
        Args:
            task_id: 任务ID
            
        Returns:
            是否成功取消
        """
        with self.lock:
            if task_id not in self.tasks:
                raise ValueError(f"任务不存在: {task_id}")
            
            task = self.tasks[task_id]
            
            if task.status not in [TrainingStatus.PENDING, TrainingStatus.RUNNING]:
                raise ValueError(f"任务状态不允许取消: {task.status.value}")
            
            # 终止进程
            if task.process and task.process.poll() is None:
                task.process.terminate()
                try:
                    task.process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    task.process.kill()
            
            task.status = TrainingStatus.CANCELLED
            task.end_time = datetime.now()
            task.save_metadata()
            
            print(f"[TRAIN] 训练任务已取消: {task_id}")
            return True
    
    def get_training_log(self, task_id: str, lines: int = 100) -> str:
        """
        获取训练日志
        
        Args:
            task_id: 任务ID
            lines: 返回的行数
            
        Returns:
            日志内容
        """
        with self.lock:
            if task_id not in self.tasks:
                raise ValueError(f"任务不存在: {task_id}")
            
            task = self.tasks[task_id]
            
            if not task.log_file.exists():
                return ""
            
            with open(task.log_file, 'r', encoding='utf-8') as f:
                all_lines = f.readlines()
                return ''.join(all_lines[-lines:])
    
    def _validate_config(self, cfg: Dict[str, Any]):
        """
        验证训练配置
        
        Args:
            cfg: 配置字典
        """
        # 验证数据集配置文件
        dataset_yaml = cfg.get('dataset_yaml', str(config.DEFAULT_DATASET_YAML))
        if not Path(dataset_yaml).exists():
            raise FileNotFoundError(f"数据集配置文件不存在: {dataset_yaml}")
        
        # 验证设备
        device = cfg.get('device', config.DEFAULT_DEVICE)
        if not config.validate_device(device):
            raise ValueError(f"无效的设备配置: {device}")
        
        # 验证图像尺寸
        image_size = cfg.get('image_size', config.DEFAULT_IMAGE_SIZE)
        if not config.validate_image_size(image_size):
            raise ValueError(f"无效的图像尺寸: {image_size}（必须是32的倍数，范围32-4096）")
        
        # 验证 batch size
        batch_size = cfg.get('batch_size', config.DEFAULT_BATCH_SIZE)
        if batch_size < 1 or batch_size > 128:
            raise ValueError(f"无效的 batch size: {batch_size}（范围1-128）")
        
        # 验证 epochs
        epochs = cfg.get('epochs', config.DEFAULT_EPOCHS)
        if epochs < 1 or epochs > 10000:
            raise ValueError(f"无效的 epochs: {epochs}（范围1-10000）")


# 全局训练管理器实例
training_manager = TrainingManager()

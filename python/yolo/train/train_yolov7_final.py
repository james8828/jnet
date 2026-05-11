"""
YOLOv7 全片数据集训练脚本

使用方法:
    python train_yolov7_final.py

配置说明:
    - 修改下方的 CONFIG 字典来自定义训练参数
    - GPU训练: DEVICE='0', BATCH_SIZE=8-16
    - CPU训练: DEVICE='cpu', BATCH_SIZE=2-4
"""
import os
import sys
import subprocess
import torch

# ==================== 训练配置 ====================
CONFIG = {
    # YOLOv7 源码目录
    'yolov7_root': r'E:\doc\system-admin\python\yolov7\yolov7-main',
    
    # 数据集配置 (由 build_yolo_dataset.py 生成)
    'dataset_yaml': r'E:\doc\system-admin\generated_dataset\data.yaml',
    
    # 训练参数
    'epochs': 300,           # 训练轮数 (小数据集需要更多轮数)
    'batch_size': 4,         # Batch Size (降低避免内存溢出,Mixup需要双倍内存)
    'image_size': 1280,       # 输入图像尺寸
    'device': '0',           # 设备: '0'=GPU, 'cpu'=CPU
    
    # 模型配置
    'weights': 'yolov7x.pt',  # 预训练权重: yolov7.pt, yolov7x.pt, yolov7-tiny.pt
    'use_adam': True,        # True=Adam优化器(小数据集收敛更好), False=SGD
    
    # 输出配置
    'project_dir': r'E:\doc\system-admin\yolo_training_results',
    'experiment_name': 'pathology_yolov7_v4_small_dataset',  # 实验名称
}
# ================================================

def check_environment():
    """检查训练环境"""
    cfg = CONFIG
    
    # 检查YOLOv7目录
    if not os.path.exists(cfg['yolov7_root']):
        raise FileNotFoundError(f"YOLOv7 目录不存在: {cfg['yolov7_root']}")
    
    # 检查数据集配置
    if not os.path.exists(cfg['dataset_yaml']):
        raise FileNotFoundError(
            f"数据集配置文件不存在: {cfg['dataset_yaml']}\n"
            f"请先运行 python/build_yolo_dataset.py 生成数据集"
        )
    
    # 检查模型配置文件
    cfg_file = os.path.join(cfg['yolov7_root'], 'cfg', 'training', 'yolov7.yaml')
    if not os.path.exists(cfg_file):
        raise FileNotFoundError(f"模型配置文件不存在: {cfg_file}")
    
    # 显示环境信息
    print(f"[INFO] PyTorch 版本: {torch.__version__}")
    print(f"[INFO] CUDA 可用: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"[INFO] GPU 设备: {torch.cuda.get_device_name(0)}")
        print(f"[INFO] GPU 显存: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f} GB")
    
    # 检查数据集
    dataset_dir = os.path.dirname(cfg['dataset_yaml'])
    train_img_dir = os.path.join(dataset_dir, 'train', 'images')
    val_img_dir = os.path.join(dataset_dir, 'val', 'images')
    
    train_count = 0
    val_count = 0
    
    if os.path.exists(train_img_dir):
        train_count = len([f for f in os.listdir(train_img_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))])
        print(f"[INFO] 训练集图像数: {train_count}")
    
    if os.path.exists(val_img_dir):
        val_count = len([f for f in os.listdir(val_img_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))])
        print(f"[INFO] 验证集图像数: {val_count}")
    
    # 数据集大小警告
    total_images = train_count + val_count
    if total_images < 50:
        print(f"\n⚠️  [WARNING] 数据集非常小! (总共{total_images}张图像)")
        print(f"建议:")
        print(f"  - 增加更多训练数据 (至少50-100张)")
        print(f"  - 使用数据增强 (已在YOLOv7中默认启用)")
        print(f"  - 降低Batch Size到2-4 (当前: {cfg['batch_size']})")
        print(f"  - 增加训练轮数 (当前: {cfg['epochs']})")
        print(f"  - 使用Adam优化器 (当前: {'启用' if cfg['use_adam'] else '禁用'})")
        print()
    
    # Batch Size检查
    if cfg['batch_size'] > train_count:
        print(f"⚠️  [WARNING] Batch Size ({cfg['batch_size']}) 大于训练集大小 ({train_count})!")
        print(f"建议将 Batch Size 调整为 {min(4, train_count)}")
        print()

def start_training():
    """启动 YOLOv7 训练"""
    cfg = CONFIG
    check_environment()
    
    # 显示训练配置
    print("\n" + "=" * 80)
    print("YOLOv7 全片数据集训练配置")
    print("=" * 80)
    print(f"数据集:     {cfg['dataset_yaml']}")
    print(f"模型权重:   {cfg['weights']}")
    print(f"训练轮数:   {cfg['epochs']} epochs")
    print(f"Batch Size: {cfg['batch_size']}")
    print(f"图像尺寸:   {cfg['image_size']}x{cfg['image_size']}")
    print(f"设备:       {'GPU (' + cfg['device'] + ')' if cfg['device'] != 'cpu' else 'CPU'}")
    print(f"优化器:     {'Adam' if cfg['use_adam'] else 'SGD (默认)'}")
    print(f"输出目录:   {os.path.join(cfg['project_dir'], cfg['experiment_name'])}")
    print("=" * 80)
    
    # 构造训练命令
    cmd = [
        sys.executable,
        os.path.join(cfg['yolov7_root'], 'train.py'),
        '--weights', cfg['weights'],
        '--cfg', 'cfg/training/yolov7.yaml',
        '--data', cfg['dataset_yaml'],
        '--epochs', str(cfg['epochs']),
        '--batch-size', str(cfg['batch_size']),
        '--img-size', str(cfg['image_size']), str(cfg['image_size']),
        '--device', cfg['device'],
        '--project', cfg['project_dir'],
        '--name', cfg['experiment_name'],
        '--workers', '4',  # Windows内存不足时设为0禁用多进程加载
        '--hyp', 'data/hyp.scratch.p5.yaml'
    ]
    
    # 如果使用Adam优化器
    if cfg['use_adam']:
        cmd.append('--adam')
    
    print(f"\n[INFO] 正在启动 YOLOv7 训练...")
    print(f"[INFO] 命令: {' '.join(cmd)}")
    print("-" * 80 + "\n")
    
    try:
        # 切换到 YOLOv7 目录执行，确保相对路径有效
        process = subprocess.run(cmd, cwd=cfg['yolov7_root'], check=True)
        
        print("\n" + "=" * 80)
        print("[SUCCESS] 训练圆满完成！")
        print("=" * 80)
        output_dir = os.path.join(cfg['project_dir'], cfg['experiment_name'])
        print(f"模型目录:   {output_dir}")
        print(f"最佳权重:   {os.path.join(output_dir, 'weights', 'best.pt')}")
        print(f"最后权重:   {os.path.join(output_dir, 'weights', 'last.pt')}")
        print(f"训练曲线:   {os.path.join(output_dir, 'results.png')}")
        print("=" * 80)
        
    except subprocess.CalledProcessError as e:
        print("\n" + "=" * 80)
        print(f"[ERROR] 训练失败 (退出码: {e.returncode})")
        print("=" * 80)
        print("\n请检查:")
        print("  1. 数据集是否正确生成 (运行 python/build_dataset_v2.py)")
        print("  2. data.yaml 配置是否正确")
        print("  3. Batch Size 是否过大:")
        print("     - CPU训练: 建议 2-4")
        print("     - GPU训练: 建议 8-16 (根据显存调整)")
        print("  4. 显存/内存是否充足")
        print("  5. Windows页面文件是否足够 (建议设置为物理内存的1.5-2倍)")
        print("  6. 查看上方错误日志定位具体问题")
        print("\n常见错误解决:")
        print("  - OSError [WinError 1455]: 页面文件太小")
        print("    解决: 设置 workers=0,或增加Windows虚拟内存")
        print("  - MemoryError: 内存不足")
        print("    解决: 降低batch_size到2,或关闭其他程序")
        print("=" * 80)
        
    except Exception as e:
        print(f"\n[ERROR] 启动训练失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    start_training()

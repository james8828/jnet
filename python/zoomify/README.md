

# Zoomify 瓦片生成工具

## 📁 项目结构

```

zoomify/
├── deepzoom_custom.py      # 核心：自定义 DeepZoom 生成器（扩展 OpenSlide）
├── tile_generator.py       # 基础：基于 OpenSlide 的瓦片生成工具（支持并行）
├── cpu.py                  # 高性能：纯 CPU 模式瓦片生成（cuCIM + OpenCV）
├── gpu.py                  # 高性能：GPU + CPU 混合模式瓦片生成（cuCIM + CUDA）
├── SlideTool.py            # 工具：WSI 元数据导出和区域读取
├── environment.yaml        # Conda 环境配置文件
├── README.md               # 本文件
└── *.svs / *.tif           # 测试用 WSI 图像文件
```
## 🚀 快速开始

### 环境准备

```
bash
# 方式一：使用 Conda 环境（推荐）
conda env create -f environment.yaml
conda activate slide_env

# 方式二：手动安装依赖
pip install openslide-python Pillow pandas numpy opencv-python cucim cupy-cuda12x
```
> **Windows 用户**：需要额外安装 OpenSlide 动态库，下载 OpenSlide binaries 并将 DLL 文件添加到系统 PATH。

### 1. 生成 Zoomify 瓦片（三种模式）

```
bash
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 模式 A：基础模式（基于 OpenSlide，适合通用场景）
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
python tile_generator.py <wsi_file_path> <output_directory> [tile_size]

# 示例
python tile_generator.py "D:\work\WSI\slide.svs" "D:\work\tiles" 512

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 模式 B：纯 CPU 高性能模式（cuCIM + OpenCV 多线程）
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 修改 cpu.py 顶部的 INPUT_FILE / OUTPUT_DIR 等配置后运行
python cpu.py

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 模式 C：GPU + CPU 混合高性能模式（cuCIM CUDA + CPU 后处理）
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 修改 gpu.py 顶部的 INPUT_FILE / OUTPUT_DIR 等配置后运行
python gpu.py
```
### 2. 导出 WSI 元数据

```
bash
python SlideTool.py <wsi_file_path>

# 示例
python SlideTool.py "D:\work\WSI\slide.svs"
```
### 3. 读取 WSI 区域（Python API）

```
python
from SlideTool import slide_read_region, export_meta

# 导出元数据
export_meta("slide.svs", "metadata.xlsx")

# 读取指定区域
region = slide_read_region(
wsi_path="slide.svs",
location=(0, 0),      # 起始坐标 (x, y)
size=(1024, 1024),    # 读取尺寸 (width, height)
level=0,              # 层级
save_path="region.jpg" # 保存路径（None 则不保存）
)

print(f"区域尺寸: {region.shape}")
```
## ✨ 三种生成模式对比

| 特性 | tile_generator.py | cpu.py | gpu.py |
|------|-------------------|--------|--------|
| 底层库 | OpenSlide | cuCIM (CPU) | cuCIM (CUDA) |
| GPU 加速 | ❌ | ❌ | ✅ |
| 批量读取 | ❌ | ❌ | ✅ |
| 自动 fallback | ❌ | — | ✅ GPU→CPU |
| Level 预验证 | ❌ | — | ✅ |
| 适用场景 | 通用 / 无 GPU | 无 GPU / 多核 CPU | 有 NVIDIA GPU |
| 推荐度 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

## 📖 详细说明

### tile_generator.py — 基础模式

基于 OpenSlide + 自定义 DeepZoom 生成器，适合通用场景。

**核心特性：**
- ✅ 多线程并行处理（默认 `(CPU核心数 * 2 + 1)` 个线程）
- ✅ 自动生成 `ImageProperties.xml`
- ✅ 支持自定义瓦片大小（默认 512px）
- ✅ 智能缓存管理（OpenSlideCache 默认 1GB）
- ✅ 透明通道自动处理 + 小瓦片白色背景填充

**函数接口：**

```
python
from tile_generator import generate_zoomify_tiles
import openslide

slide = openslide.OpenSlide("slide.svs")

tile_count = generate_zoomify_tiles(
slide=slide,
output_dir="./output",
tile_size=512,
parallel=True,          # 启用并行处理
max_workers=None        # 自动计算线程数
)

slide.close()
```
### cpu.py — 纯 CPU 高性能模式

使用 cuCIM 作为图像读取后端 + OpenCV resize + 全局 CPU 线程池，充分利用多核 CPU。

**核心特性：**
- ✅ cuCIM `per_process` 缓存，自动缓存已解码瓦片
- ✅ OpenCV `INTER_AREA` 高质量降采样
- ✅ 全局线程池，所有层级共用
- ✅ 按 `native_level` 和 `size` 分组，减少重复读取
- ✅ 自动处理 ICC 颜色配置文件

**配置参数（修改文件顶部常量）：**

```
python
INPUT_FILE = "image.svs"       # 输入 WSI 文件
OUTPUT_DIR = "output/tiles"    # 输出目录
TILE_SIZE = 512                # 瓦片尺寸
JPEG_QUALITY = 90              # JPEG 压缩质量
CPU_MAX_WORKERS = os.cpu_count() * 2  # 线程数
```
**函数接口：**

```
python
from cpu import generate_zoomify_tiles

generate_zoomify_tiles(
img_path="image.svs",
output_dir="output/tiles",
tile_size=512,
quality=90,
cpu_max_workers=16,
)
```
### gpu.py — GPU + CPU 混合高性能模式

GPU 批量读取 + CPU 多线程后处理，自动验证每个 Level 的 GPU 读取能力，失败时自动 fallback 到 CPU。

**核心特性：**
- ✅ GPU 批量读取（cuCIM CUDA 后端）
- ✅ CPU 多线程 resize + JPEG 保存（流水线并行）
- ✅ Level 预验证：启动前检测每个金字塔层级的 GPU 读取能力
- ✅ 自动 fallback：GPU 失败（OOM 等）自动回退到 CPU
- ✅ GPU / CPU 层级并行执行（GPU 读取 + CPU 后处理流水线）

**配置参数（修改文件顶部常量）：**

```
python
INPUT_FILE = "image.svs"
OUTPUT_DIR = "output/tiles"
TILE_SIZE = 512
JPEG_QUALITY = 90
CPU_MAX_WORKERS = os.cpu_count() * 2
```
**函数接口：**

```
python
from gpu import generate_zoomify_tiles

generate_zoomify_tiles(
img_path="image.svs",
output_dir="output/tiles",
tile_size=512,
quality=90,
cpu_max_workers=16,
gpu_batch_size=8,          # GPU 每次批量读取的瓦片数
)
```
### SlideTool.py — WSI 工具集

**工具函数：**

1. **export_meta()** — 导出 WSI 元数据到 Excel
```
python
export_meta("slide.svs", "metadata.xlsx")
```
2. **calculate_md5_from_array()** — 计算图像 MD5 哈希值
```
python
from SlideTool import calculate_md5_from_array
md5 = calculate_md5_from_array(img_array)
```
3. **slide_read_region()** — 读取 WSI 指定区域
```
python
region = slide_read_region("slide.svs", (0, 0), (1024, 1024), level=0)
```
### deepzoom_custom.py — 自定义 DeepZoom 生成器

扩展自 `openslide.deepzoom.DeepZoomGenerator`，主要改进：
- 使用 `tile_size` 而非 1px 计算层级，适配前端显示需求
- 自动处理 ICC 颜色配置文件（转 sRGB）
- 支持 RGBA 转 RGB + 背景色合成

```
python
from deepzoom_custom import DeepZoomGeneratorCustom
import openslide

slide = openslide.OpenSlide("slide.svs")
dz = DeepZoomGeneratorCustom(osr=slide, tile_size=256, overlap=1, limit_bounds=True)

# 获取瓦片
tile = dz.get_tile(level=0, coords=(0, 0))
```
## 🔧 技术细节

### 瓦片命名规则

```

output/
├── 0/                  # Level 0（最高分辨率）
│   ├── 0_0.jpg         # 格式：{y}_{x}.jpg（tile_generator）
│   ├── 0_1.jpg         # 或 {y}-{x}.jpg（cpu）
│   └── ...             # 或 {x}-{y}.jpg（gpu）
├── 1/                  # Level 1
│   └── ...
├── ImageProperties.xml # 仅 tile_generator 自动生成
```
> **注意**：三种模式的瓦片文件命名规则略有不同，请根据使用场景选择对应模式。

### 性能优化

| 优化项 | tile_generator | cpu | gpu |
|--------|---------------|-----|-----|
| 并行策略 | ThreadPoolExecutor | 全局 CPU 线程池 | GPU 批量读 + CPU 线程池 |
| 缓存 | OpenSlideCache 1GB | cuCIM per_process 缓存 | cuCIM per_process 缓存 |
| Resize | Pillow LANCZOS | OpenCV INTER_AREA | OpenCV INTER_AREA |
| 颜色管理 | ICC → sRGB | ICC → sRGB | ICC → sRGB |
| 进度反馈 | 每 10000 瓦片 | 每 10000 瓦片 | 每 500 瓦片 |

### ImageProperties.xml 格式

```
xml
<?xml version="1.0" encoding="UTF-8"?>
<IMAGE_PROPERTIES
WIDTH="4096"
HEIGHT="4096"
NUMTILES="5"
NUMIMAGES="1"
VERSION="1.8"
TILESIZE="256" />
```
## ⚠️ 注意事项

1. **支持的 WSI 格式**：`.svs`（Aperio）、`.tif`（Pyramid TIFF）等 OpenSlide / cuCIM 支持的格式

2. **GPU 模式要求**：
   - NVIDIA GPU + CUDA 12.x
   - `cupy` 和 `cucim` 的 CUDA 版本需匹配
   - 显存不足时自动 fallback 到 CPU

3. **大文件处理**：
   - 建议设置更大的缓存
   - 监控内存 / 显存使用情况
   - 高分辨率 WSI 建议使用 GPU 模式

4. **cuCIM 平台限制**：
   - `cucim` 主要支持 Linux，Windows 支持有限
   - 纯 Windows 环境建议使用 `tile_generator.py`





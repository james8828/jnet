# Zoomify 瓦片生成工具

## 📁 项目结构

```
zoomify/
├── deepzoom_custom.py      # 核心：自定义 DeepZoom 生成器（扩展 OpenSlide）
├── tile_generator.py       # 主要：统一的瓦片生成工具（支持并行处理）
├── SlideTool.py            # 工具：WSI 元数据导出和区域读取
├── README.md               # 本文件
└── *.deprecated            # 废弃文件（已重构整合）
```

## 🚀 快速开始

### 1. 生成 Zoomify 瓦片

```bash
# 基本用法
python tile_generator.py <wsi_file_path> <output_directory>

# 指定瓦片大小
python tile_generator.py slide.svs ./output 512

# 示例
python tile_generator.py "D:\work\WSI\slide.svs" "D:\work\tiles" 256
```

### 2. 导出 WSI 元数据

```bash
# 导出元数据到 Excel
python SlideTool.py <wsi_file_path>

# 示例
python SlideTool.py "D:\work\WSI\slide.svs"
```

### 3. 读取 WSI 区域（Python API）

```python
from SlideTool import slide_read, export_meta

# 导出元数据
export_meta("slide.svs", "metadata.xlsx")

# 读取指定区域
region = slide_read(
    wsi_path="slide.svs",
    location=(0, 0),      # 起始坐标 (x, y)
    size=(1024, 1024),    # 读取尺寸 (width, height)
    level=0,              # 层级
    save_path="region.jpg" # 保存路径（None 则不保存）
)

print(f"区域尺寸: {region.shape}")
```

## ✨ 主要功能

### tile_generator.py

**核心特性：**
- ✅ 并行处理瓦片生成（多线程，自动优化性能）
- ✅ 自动生成 ImageProperties.xml
- ✅ 支持自定义瓦片大小（默认 256px）
- ✅ 智能缓存管理（1GB 默认）
- ✅ 透明通道自动处理
- ✅ 小瓦片自动填充白色背景

**函数接口：**

```python
from tile_generator import generate_zoomify_tiles
import openslide

# 打开 WSI 文件
slide = openslide.OpenSlide("slide.svs")

# 生成瓦片
tile_count = generate_zoomify_tiles(
    slide=slide,
    output_dir="./output",
    tile_size=256,
    parallel=True,          # 启用并行处理
    max_workers=None        # 自动计算线程数
)

slide.close()
```

### SlideTool.py

**工具函数：**

1. **export_meta()** - 导出 WSI 元数据到 Excel
   ```python
   export_meta("slide.svs", "metadata.xlsx")
   ```

2. **calculate_md5_from_array()** - 计算图像 MD5 哈希值
   ```python
   from SlideTool import calculate_md5_from_array
   import numpy as np
   
   img = np.array(...)
   md5 = calculate_md5_from_array(img)
   ```

3. **slide_read()** - 读取 WSI 指定区域
   ```python
   region = slide_read("slide.svs", (0, 0), (1024, 1024), level=0)
   ```

### deepzoom_custom.py

**DeepZoomGeneratorCustom 类：**

扩展自 `openslide.deepzoom.DeepZoomGenerator`，主要改进：
- 使用 `tile_size` 而非 1px 计算层级
- 适配前端显示需求
- 自动处理颜色配置文件
- 支持 RGBA 转 RGB + 背景色合成

```python
from deepzoom_custom import DeepZoomGeneratorCustom
import openslide

slide = openslide.OpenSlide("slide.svs")
dz = DeepZoomGeneratorCustom(
    osr=slide,
    tile_size=256,
    overlap=1,
    limit_bounds=True
)

# 获取瓦片
tile = dz.get_tile(level=0, coords=(0, 0))
```

## 🔧 技术细节

### 瓦片命名规则

```
output/
├── 0/                  # Level 0（最高分辨率）
│   ├── 0_0.jpg
│   ├── 0_1.jpg
│   └── ...
├── 1/                  # Level 1
│   ├── 0_0.jpg
│   └── ...
├── ImageProperties.xml
```

格式：`{y}_{x}.jpg`（先行后列）

### 性能优化

- **并行处理**：使用线程池，默认 `(CPU核心数 * 2 + 1)` 个线程
- **缓存管理**：OpenSlideCache 默认 1GB
- **批量提交**：控制并发任务数量，避免内存溢出
- **进度反馈**：每处理 100 个瓦片输出一次进度

### ImageProperties.xml 格式

```xml
<?xml version="1.0" encoding="UTF-8"?>
<IMAGE_PROPERTIES 
    WIDTH="4096" 
    HEIGHT="4096" 
    NUMTILES="5" 
    NUMIMAGES="1" 
    VERSION="1.8" 
    TILESIZE="256" />
```

## 📝 迁移指南

如果你之前使用 `zoomify_server.py` 或 `tile.py`：

### 从 zoomify_server.py 迁移

```python
# 旧代码
from zoomify_server import output_zoomify_tiles
output_zoomify_tiles(slide, output_dir)

# 新代码
from tile_generator import generate_zoomify_tiles
generate_zoomify_tiles(slide, output_dir, parallel=True)
```

### 从 tile.py 迁移

```python
# 旧代码
from tile import cunrrent_output_zoomify_tiles
cunrrent_output_zoomify_tiles(slide, output_dir)

# 新代码（完全兼容）
from tile_generator import generate_zoomify_tiles
generate_zoomify_tiles(slide, output_dir, parallel=True)
```

## ⚠️ 注意事项

1. **依赖安装**：
   ```bash
   pip install openslide-python Pillow pandas numpy
   ```

2. **OpenSlide DLL**（Windows）：
   - 下载 OpenSlide binaries
   - 将 DLL 文件添加到系统 PATH

3. **大文件处理**：
   - 建议设置更大的缓存：`slide.set_cache(OpenSlideCache(2 * 1024 * 1024 * 1024))`
   - 监控内存使用情况

4. **废弃文件**：
   - `zoomify_server.py.deprecated` - 已整合到 tile_generator.py
   - `tile.py.deprecated` - 已整合到 tile_generator.py
   - 这些文件保留仅供参考，未来版本可能删除

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目遵循原 OpenSlide 项目的许可证。

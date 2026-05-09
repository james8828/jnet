import sys
import os
import time
import openslide
from openslide import OpenSlideCache
from deepzoom_custom import DeepZoomGeneratorCustom
import concurrent.futures
from concurrent.futures import ThreadPoolExecutor

TILE_SIZE = 256


def generate_image_properties(wsi_dimensions, dz_level_count, tile_size=TILE_SIZE):
    """
    生成 ImageProperties.xml 文件内容
    :param wsi_dimensions: WSI 图像尺寸 (width, height)
    :param dz_level_count: DeepZoom 层级数量
    :param tile_size: 瓦片大小
    :return: XML 字符串
    """
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<IMAGE_PROPERTIES WIDTH="{wsi_dimensions[0]}" HEIGHT="{wsi_dimensions[1]}" NUMTILES="{dz_level_count}" NUMIMAGES="1" VERSION="1.8" TILESIZE="{tile_size}" />
"""


def save_tile_to_file(tile, tile_path, tile_size=TILE_SIZE):
    """
    保存瓦片图像到文件（处理透明通道和尺寸）
    :param tile: PIL Image 对象
    :param tile_path: 保存路径
    :param tile_size: 期望的瓦片尺寸
    """
    import numpy as np
    from PIL import Image
    
    if tile.mode == 'RGBA':
        tile = tile.convert('RGB')
    
    tile_np = np.array(tile)
    
    # 如果瓦片尺寸小于预期，填充白色背景
    if tile_np.shape[0] < tile_size or tile_np.shape[1] < tile_size:
        blank_image = np.zeros((tile_size, tile_size, 3), dtype=np.uint8)
        blank_image[:, :] = [255, 255, 255]  # 白色背景
        blank_image[:tile_np.shape[0], :tile_np.shape[1]] = tile_np
        tile = Image.fromarray(blank_image)
    
    tile.save(tile_path, "JPEG", quality=90)


def generate_zoomify_tiles(slide, output_dir, tile_size=TILE_SIZE, parallel=True, max_workers=None):
    """
    生成 Zoomify 格式的瓦片（支持并行处理）
    :param slide: OpenSlide 对象
    :param output_dir: 输出目录
    :param tile_size: 瓦片大小（默认 256）
    :param parallel: 是否使用并行处理（默认 True）
    :param max_workers: 最大工作线程数（None 则自动计算）
    :return: 处理的瓦片总数
    """
    start_time = time.time()

    # 创建输出目录
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # 创建 DeepZoom 生成器
    dz = DeepZoomGeneratorCustom(slide, tile_size, 0, True)
    dz_level_count = dz.level_count

    print(f"WSI dimensions: {slide.dimensions}")
    print(f"DeepZoom levels: {dz_level_count}")
    print(f"Tile size: {tile_size}")
    print(f"Parallel processing: {'Enabled' if parallel else 'Disabled'}")

    tile_count = 0

    if parallel:
        # 并行处理模式
        if max_workers is None:
            max_workers = os.cpu_count() * 2 + 1
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = []
            max_pending_tasks = max_workers * 4

            # 遍历每个层级
            for level in range(dz_level_count):
                level_dir = os.path.join(output_dir, str(level))
                if not os.path.exists(level_dir):
                    os.makedirs(level_dir)

                level_tiles = dz.level_tiles[level]
                print(f"Level {level}: {level_tiles[0]}x{level_tiles[1]} tiles")

                # 生成每个瓦片
                for y in range(level_tiles[1]):
                    for x in range(level_tiles[0]):
                        tile_path = os.path.join(level_dir, f"{y}_{x}.jpg")
                        
                        # 控制并发任务数量
                        if len(futures) >= max_pending_tasks:
                            completed_futures = [f for f in futures if f.done()]
                            for future in completed_futures:
                                futures.remove(future)
                                try:
                                    future.result()
                                    tile_count += 1
                                    if tile_count % 100 == 0:
                                        print(f"Processed {tile_count} tiles at {time.strftime('%Y-%m-%d %H:%M:%S')}")
                                except Exception as e:
                                    print(f"Error processing tile: {e}")
                        
                        # 提交异步任务
                        future = executor.submit(_process_and_save_tile, dz, level, x, y, tile_path, tile_size)
                        futures.append(future)

            # 等待剩余任务完成
            for future in concurrent.futures.as_completed(futures):
                try:
                    future.result()
                    tile_count += 1
                except Exception as e:
                    print(f"Error processing tile: {e}")
    else:
        # 串行处理模式（用于调试或小文件）
        for level in range(dz_level_count):
            level_dir = os.path.join(output_dir, str(level))
            if not os.path.exists(level_dir):
                os.makedirs(level_dir)

            level_tiles = dz.level_tiles[level]
            print(f"Level {level}: {level_tiles[0]}x{level_tiles[1]} tiles")

            for y in range(level_tiles[1]):
                for x in range(level_tiles[0]):
                    tile_path = os.path.join(level_dir, f"{y}_{x}.jpg")
                    try:
                        _process_and_save_tile(dz, level, x, y, tile_path, tile_size)
                        tile_count += 1
                        if tile_count % 100 == 0:
                            print(f"Processed {tile_count} tiles")
                    except Exception as e:
                        print(f"Error processing tile {level}-{x}-{y}: {e}")

    # 生成 ImageProperties.xml
    image_properties = generate_image_properties(slide.dimensions, dz_level_count, tile_size)
    image_properties_path = os.path.join(output_dir, "ImageProperties.xml")
    with open(image_properties_path, "w", encoding='utf-8') as f:
        f.write(image_properties)
    print(f"Created ImageProperties.xml at {image_properties_path}")

    end_time = time.time()
    elapsed_time = end_time - start_time
    print(f"\n{'='*50}")
    print(f"Total tiles processed: {tile_count}")
    print(f"Time taken: {elapsed_time:.2f} seconds")
    print(f"Output directory: {output_dir}")
    print(f"{'='*50}")
    
    return tile_count


def _process_and_save_tile(dz, level, x, y, tile_path, tile_size):
    """
    内部函数：处理并保存单个瓦片
    :param dz: DeepZoomGeneratorCustom 对象
    :param level: 层级
    :param x: x 坐标
    :param y: y 坐标
    :param tile_path: 保存路径
    :param tile_size: 瓦片尺寸
    """
    tile = dz.get_tile(level, (x, y))
    save_tile_to_file(tile, tile_path, tile_size)


def main():
    """主函数：从命令行参数获取输入并生成瓦片"""
    # 获取命令行参数
    args = sys.argv[1:]
    
    if len(args) >= 2:
        wsi_path = args[0]
        output_dir = args[1]
        tile_size = int(args[2]) if len(args) > 2 else TILE_SIZE
    else:
        # 默认测试路径（仅用于开发调试）
        print("警告: 未提供命令行参数，使用默认测试路径")
        wsi_path = 'E:\\需求\\机构版\\bug\\D26-0224-RD 260224001-31 1F.svs'
        output_dir = 'E:\\需求\\机构版\\bug\\D26-0224-RD 260224001-31 1F'
        tile_size = TILE_SIZE
    
    # 检查文件是否存在
    if not os.path.exists(wsi_path):
        print(f"错误: WSI 文件不存在: {wsi_path}")
        sys.exit(1)
    
    print(f"Opening WSI file: {wsi_path}")
    slide = openslide.OpenSlide(wsi_path)
    
    # 设置缓存（1GB）
    cache_capacity = 1 * 1024 * 1024 * 1024
    cache = OpenSlideCache(cache_capacity)
    slide.set_cache(cache)
    
    try:
        # 生成瓦片
        generate_zoomify_tiles(slide, output_dir, tile_size, parallel=True)
    finally:
        slide.close()
        print("WSI file closed.")


if __name__ == "__main__":
    main()

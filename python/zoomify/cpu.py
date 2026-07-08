"""
纯 CPU 模式 Zoomify 金字塔瓦片生成器。

所有 read_region + resize + JPEG 保存均在 CPU 上执行，
通过全局线程池并行处理多个瓦片，充分利用多核 CPU。
"""

import os
import time
import concurrent.futures
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
import cv2
import openslide
from PIL import Image, ImageCms
from cucim import CuImage
from cucim.clara.cache import preferred_memory_capacity


# ============================================================================
# 配置
# ============================================================================
INPUT_FILE = "image.svs"
OUTPUT_DIR = "output/tiles"
TILE_SIZE = 512
JPEG_QUALITY = 90
CPU_MAX_WORKERS = os.cpu_count()*2


# ============================================================================
# 全局变量
# ============================================================================
_cpu_executor: concurrent.futures.ThreadPoolExecutor = None
_color_profile = None


def init_cpu_executor(max_workers=None):
    """初始化全局 CPU 线程池（仅初始化一次）。"""
    global _cpu_executor
    if _cpu_executor is not None:
        return _cpu_executor
    workers = max_workers or CPU_MAX_WORKERS
    _cpu_executor = concurrent.futures.ThreadPoolExecutor(
        max_workers=workers, thread_name_prefix="CPU-Worker"
    )
    print(f"[Init] CPU thread pool: {workers} workers")
    return _cpu_executor


def shutdown_cpu_executor():
    """关闭全局 CPU 线程池。"""
    global _cpu_executor
    if _cpu_executor is not None:
        _cpu_executor.shutdown(wait=True)
        _cpu_executor = None


# ============================================================================
# 数据结构
# ============================================================================
@dataclass(frozen=True)
class TileTask:
    z: int
    x: int
    y: int
    native_level: int
    native_downsample: float
    target_downsample: int
    location: Tuple[int, int]
    size: Tuple[int, int]
    target_size: Tuple[int, int]


# ============================================================================
# 图像加载
# ============================================================================
def load_img(img_path, patch_size=(TILE_SIZE, TILE_SIZE)):
    """加载图像并启用 cuCIM 内部 CPU 缓存。"""
    global _color_profile
    
    cache = CuImage.cache("per_process", memory_capacity=2048)
    img = CuImage(img_path)
    memory_capacity = preferred_memory_capacity(img, patch_size=patch_size)
    print(f"memory_capacity : {memory_capacity} MiB")

    cache = CuImage.cache("per_process", memory_capacity=memory_capacity)
    print("= Cache Info =")
    print(f"       type: {cache.type}({int(cache.type)})")
    print(f"memory_size: {cache.memory_size}/{cache.memory_capacity}")
    print(f"       size: {cache.size}/{cache.capacity}")

    _color_profile = None
    try:
        osr = openslide.OpenSlide(img_path)
        if hasattr(osr, 'color_profile') and osr.color_profile:
            _color_profile = osr.color_profile
            print("= Color Profile =")
            print(f"       found: True")
        osr.close()
    except Exception as e:
        print(f"  [WARN] Failed to get color profile from openslide: {e}")

    return img


# ============================================================================
# 辅助函数
# ============================================================================
def _select_native_level(downsamples, target_downsample):
    """为 Zoomify 目标 downsample 选择最合适的 native pyramid level。"""
    best_level = 0
    best_diff = float("inf")
    for level, d in enumerate(downsamples):
        if d <= target_downsample:
            diff = target_downsample - d
            if diff < best_diff:
                best_diff = diff
                best_level = level
    return best_level


def _resize_to_target(arr, target_size):
    """
    CPU resize 到目标尺寸。

    target_size: (width, height) 格式。
    返回: numpy uint8 array (H, W, C)。
    """
    arr = np.asarray(arr)

    # 尺寸已匹配，直接返回
    if arr.shape[0] == target_size[1] and arr.shape[1] == target_size[0]:
        return arr

    if arr.ndim == 2:
        resized = cv2.resize(arr, target_size, interpolation=cv2.INTER_AREA)
    else:
        arr_bgr = cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
        resized = cv2.resize(arr_bgr, target_size, interpolation=cv2.INTER_AREA)
        resized = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
    
    return resized.astype(np.uint8)


def _save_as_jpeg(arr, output_path, quality=JPEG_QUALITY):
    """将 numpy array 保存为 JPEG。"""
    global _color_profile
    
    if arr.dtype != np.uint8:
        arr = arr.astype(np.uint8)

    if arr.ndim == 3 and arr.shape[-1] == 4:
        arr = arr[..., :3]

    pil_img = Image.fromarray(arr)
    if pil_img.mode == "RGBA":
        pil_img = pil_img.convert("RGB")

    if _color_profile:
        try:
            rgbp = ImageCms.createProfile("sRGB")
            transform = ImageCms.buildTransform(_color_profile, rgbp, "RGB", "RGB")
            pil_img = ImageCms.applyTransform(pil_img, transform)
        except Exception:
            pass

    output_path.parent.mkdir(parents=True, exist_ok=True)
    pil_img.save(output_path, "JPEG", quality=quality)


def _make_output_path(task, output_dir):
    """构造输出路径：{output_dir}/{z}/{y}-{x}.jpg"""
    # return Path(output_dir) / f"{task.z}-{task.x}-{task.y}.jpg"
    return Path(output_dir) / f"{task.z}/{task.y}-{task.x}.jpg"


# ============================================================================
# 单任务处理
# ============================================================================
def process_single_task(img, task, output_dir, quality=JPEG_QUALITY):
    """
    处理单个瓦片任务：CPU read_region → resize → 保存 JPEG。

    返回: (success: bool, error: Exception or None)
    """
    try:
        
        region = img.read_region(
            location=task.location,
            size=task.size,
            level=task.native_level,
            batch_size=1,
            num_workers=0,
            device="cpu",
        )
        arr = _resize_to_target(region, task.target_size)
        output_path = _make_output_path(task, output_dir)
        _save_as_jpeg(arr, output_path, quality=quality)
        return True, None
    except Exception as e:
        return False, e


# ============================================================================
# 层级处理（使用全局线程池）
# ============================================================================
def process_level(img, tasks_by_size, output_dir, quality):
    """
    处理单个 pyramid level 的所有瓦片任务（使用全局 CPU 线程池并行处理）。
    """
    all_tasks = []
    for group_tasks in tasks_by_size.values():
        all_tasks.extend(group_tasks)

    total = len(all_tasks)
    if total == 0:
        return 0, 0

    native_level = all_tasks[0].native_level
    print(f"\n[CPU] Processing Level {native_level}: {total} tasks, "
          f"{len(tasks_by_size)} size groups")
    
    # 检查前几个任务的 location 是否正确
    print(f"  First task: location={all_tasks[0].location}, size={all_tasks[0].size}")
    if len(all_tasks) > 1:
        print(f"  Second task: location={all_tasks[1].location}, size={all_tasks[1].size}")

    executor = init_cpu_executor()
    futures = [
        executor.submit(process_single_task, img, task, output_dir, quality)
        for task in all_tasks
    ]

    completed = 0
    failed = 0
    for i, future in enumerate(concurrent.futures.as_completed(futures)):
        success, error = future.result()
        if success:
            completed += 1
        else:
            failed += 1
            task = all_tasks[i]
            print(f"  [CPU] Error z={task.z} x={task.x} y={task.y}: {error}")

        if completed % 10000 == 0 and completed > 0:
            print(f"  [CPU] Level {native_level} progress: {completed}/{total}")

    print(f"[CPU] Level {native_level} done: {completed}/{total}, failed: {failed}")
    return completed, failed


# ============================================================================
# 任务生成
# ============================================================================
def generate_tile_tasks(img, tile_size):
    """按 Zoomify 逻辑生成瓦片任务，按 native_level 和 size 分组。"""
    level_dimensions = img.resolutions["level_dimensions"]
    level_downsamples = img.resolutions["level_downsamples"]
    level0_width, level0_height = level_dimensions[0]

    max_zoom = int(np.ceil(np.log2(max(level0_width, level0_height) / tile_size)))
    max_zoom = max(max_zoom, 0)

    tasks_by_level: Dict[int, Dict[Tuple[int, int], List[TileTask]]] = {}

    for z in range(max_zoom + 1):
        target_downsample = 2 ** z
        level_w = int(np.ceil(level0_width / target_downsample))
        level_h = int(np.ceil(level0_height / target_downsample))
        num_tiles_x = int(np.ceil(level_w / tile_size))
        num_tiles_y = int(np.ceil(level_h / tile_size))

        native_level = _select_native_level(level_downsamples, target_downsample)
        native_downsample = level_downsamples[native_level]
        
        print(f"z={z}, target_downsample={target_downsample}, native_level={native_level}, native_downsample={native_downsample}")
        
        for y in range(min(2, num_tiles_y)):
            for x in range(min(2, num_tiles_x)):
                tx_start = x * tile_size
                ty_start = y * tile_size
                sx0_start = int(round(tx_start * target_downsample))
                sy0_start = int(round(ty_start * target_downsample))

        if native_level not in tasks_by_level:
            tasks_by_level[native_level] = {}

        for y in range(num_tiles_y):
            for x in range(num_tiles_x):
                tx_start = x * tile_size
                ty_start = y * tile_size
                tx_end = min((x + 1) * tile_size, level_w)
                ty_end = min((y + 1) * tile_size, level_h)

                sx0_start = int(round(tx_start * target_downsample))
                sy0_start = int(round(ty_start * target_downsample))
                sx0_end = int(round(tx_end * target_downsample))
                sy0_end = int(round(ty_end * target_downsample))

                sw_0 = sx0_end - sx0_start
                sh_0 = sy0_end - sy0_start

                sw_native = max(1, int(np.ceil(sw_0 / native_downsample)))
                sh_native = max(1, int(np.ceil(sh_0 / native_downsample)))

                size_key = (sw_native, sh_native)
                if size_key not in tasks_by_level[native_level]:
                    tasks_by_level[native_level][size_key] = []

                target_w = tx_end - tx_start
                target_h = ty_end - ty_start

                tasks_by_level[native_level][size_key].append(
                    TileTask(
                        z=z, x=x, y=y,
                        native_level=native_level,
                        native_downsample=native_downsample,
                        target_downsample=target_downsample,
                        location=(sx0_start, sy0_start),
                        size=size_key,
                        target_size=(target_w, target_h),
                    )
                )

    return tasks_by_level


# ============================================================================
# 主入口
# ============================================================================
def generate_zoomify_tiles(
    img_path,
    output_dir,
    tile_size=TILE_SIZE,
    quality=JPEG_QUALITY,
    cpu_max_workers=CPU_MAX_WORKERS,
):
    """
    主入口：纯 CPU 模式生成 Zoomify 金字塔瓦片。

    架构：
    - 全局 CPU 线程池并行执行所有瓦片的 read_region + resize + JPEG 保存
    - cuCIM per_process 缓存自动缓存已解码的瓦片，重复读取命中缓存
    - 所有层级共享同一个线程池，顺序处理各层级
    """
    print(f"\n{'=' * 60}")
    print(f"STARTING ZOOMIFY TILE GENERATION (CPU only)")
    print(f"{'=' * 60}")
    print(f"Input file:   {img_path}")
    print(f"Output dir:   {output_dir}")
    print(f"Tile size:    {tile_size}x{tile_size}")
    print(f"CPU workers:  {cpu_max_workers}")
    print(f"JPEG quality: {quality}")

    img = load_img(img_path, patch_size=(tile_size, tile_size))
    init_cpu_executor(cpu_max_workers)

    tasks_by_level = generate_tile_tasks(img, tile_size)

    total = sum(
        sum(len(t) for t in lt.values()) for lt in tasks_by_level.values()
    )
    print(f"\nTotal tasks: {total}")
    for lv in sorted(tasks_by_level):
        n = sum(len(t) for t in tasks_by_level[lv].values())
        print(f"  Level {lv}: {n} tasks, {len(tasks_by_level[lv])} size groups")
        
        # 打印前几个任务的详细信息
        count = 0
        for size_key, group_tasks in tasks_by_level[lv].items():
            for task in group_tasks[:3]:
                print(f"    Task z={task.z}, x={task.x}, y={task.y}: location={task.location}, size={task.size}, target_size={task.target_size}")
                count += 1
            if count >= 3:
                break

    start_time = time.perf_counter()
    total_completed = 0
    total_failed = 0

    for lv in sorted(tasks_by_level):
        c, f = process_level(img, tasks_by_level[lv], output_dir, quality)
        total_completed += c
        total_failed += f

    shutdown_cpu_executor()

    elapsed = time.perf_counter() - start_time

    print(f"\n{'=' * 60}")
    print(f"COMPLETED in {elapsed:.2f}s")
    print(f"{'=' * 60}")
    print(f"OK:     {total_completed}")
    print(f"Failed: {total_failed}")
    print(f"Total:  {total_completed}/{total} tiles")
    if elapsed > 0:
        print(f"Throughput: {total_completed / elapsed:.1f} tiles/s")


if __name__ == "__main__":
    generate_zoomify_tiles(
        img_path=INPUT_FILE,
        output_dir=OUTPUT_DIR,
        tile_size=TILE_SIZE,
        quality=JPEG_QUALITY,
        cpu_max_workers=CPU_MAX_WORKERS,
    )

import os
import time
import concurrent.futures
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
import cupy as cp
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
# CPU 线程池最大线程数（用于 resize + JPEG 保存等 CPU 密集任务）
CPU_MAX_WORKERS = os.cpu_count() * 2


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
    device: str


# ============================================================================
# 图像加载
# ============================================================================
def load_img(img_path, patch_size=(TILE_SIZE, TILE_SIZE)):
    """加载图像并启用 cuCIM 内部缓存。"""
    global _color_profile
    
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


def _resize_to_target(region, target_size):
    """
    统一的 resize 方法：自动检测 GPU/CPU 输入，resize 到目标尺寸。

    target_size: (width, height) 格式。
    返回: numpy uint8 array (H, W, C)。
    """
    # 自动检测 GPU 数组并转换到 CPU
    if hasattr(region, '__cuda_array_interface__'):
        arr = cp.asnumpy(cp.asarray(region))
    else:
        arr = np.asarray(region)

    # 尺寸已匹配，直接返回
    if arr.shape[0] == target_size[1] and arr.shape[1] == target_size[0]:
        return arr

    # OpenCV resize: dsize=(width, height)
    # INTER_AREA 降采样质量优于 INTER_LINEAR
    resized = cv2.resize(arr, (target_size[0], target_size[1]),
                         interpolation=cv2.INTER_AREA)
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
    """构造输出路径：{output_dir}/{z}/{x}/{y}.jpg"""
    """ 构造输出路径：{output_dir}/{z}-{x}-{y}.jpg"""
    return Path(output_dir) / f"{task.z}/{task.x}-{task.y}.jpg"


# ============================================================================
# 单任务 CPU 处理（GPU 失败时也调用此方法）
# ============================================================================
def process_single_cpu_task(img, task, output_dir, quality=JPEG_QUALITY):
    """
    CPU 处理单个瓦片任务：read_region → resize → 保存 JPEG。

    GPU 任务失败时也调用此方法作为 fallback。

    返回: (success: bool, error: Exception or None)
    """
    try:
        region = img.read_region(
            location=list(task.location),
            size=list(task.size),
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
# GPU 批量读取 + 多线程后处理
# ============================================================================
def _process_gpu_batch(img, batch_tasks, size_key, native_level, output_dir, quality):
    """
    GPU 批量读取一批瓦片，然后用 CPU 线程池并行执行 resize + 保存。

    返回: (completed, failed)
    """
    locations = [list(task.location) for task in batch_tasks]
    target_sizes = [task.target_size for task in batch_tasks]

    # 1) GPU 批量读取
    regions = img.read_region(
        location=locations,
        size=list(size_key),
        level=native_level,
        batch_size=len(batch_tasks),
        num_workers=20,
        device="cuda",
    )

    # 2) 提取 GPU 数据 → 提交到 CPU 线程池并行 resize + 保存
    #    先从 GPU 迭代器中提取所有数据到 CPU（避免 GPU 内存长时间占用）
    cpu_arrays = []
    if hasattr(regions, '__next__'):
        # 批量模式返回 CuImageIterator
        for batch in regions:
            batch_arr = cp.asnumpy(cp.asarray(batch))  # GPU → CPU
            for j in range(len(batch_tasks)):
                cpu_arrays.append(batch_arr[j])
            del batch_arr
    else:
        # 单个结果
        cpu_arrays.append(cp.asnumpy(cp.asarray(regions)))

    # 3) 用 CPU 线程池并行 resize + 保存
    executor = init_cpu_executor()
    futures = []

    for i, task in enumerate(batch_tasks):
        arr = cpu_arrays[i]
        output_path = _make_output_path(task, output_dir)
        futures.append(executor.submit(
            _resize_and_save_worker, arr, task.target_size, output_path, quality
        ))

    # 等待所有后处理完成
    completed = 0
    failed = 0
    for i, future in enumerate(futures):
        success, error = future.result()
        if success:
            completed += 1
        else:
            failed += 1
            task = batch_tasks[i]
            print(f"      [Post] Error z={task.z} x={task.x} y={task.y}: {error}")

    return completed, failed


def _resize_and_save_worker(arr, target_size, output_path, quality):
    """CPU 工作线程：resize + 保存 JPEG。"""
    try:
        resized = _resize_to_target(arr, target_size)
        _save_as_jpeg(resized, output_path, quality=quality)
        return True, None
    except Exception as e:
        return False, e


def _fallback_batch_to_cpu(img, batch_tasks, output_dir, quality):
    """GPU 批量读取失败时，将整批任务提交到 CPU 线程池处理。"""
    executor = init_cpu_executor()
    futures = [
        executor.submit(process_single_cpu_task, img, task, output_dir, quality)
        for task in batch_tasks
    ]

    completed = 0
    failed = 0
    for i, future in enumerate(futures):
        success, error = future.result()
        if success:
            completed += 1
        else:
            failed += 1
            task = batch_tasks[i]
            print(f"      [CPU fallback] Error z={task.z} x={task.x} y={task.y}: {error}")

    return completed, failed


# ============================================================================
# GPU 层级处理
# ============================================================================
def process_gpu_level(img, tasks_by_size, native_level, output_dir, quality, batch_size):
    """
    处理单个 GPU 层级的所有任务。

    流程：按 size 分组 → 批量 GPU 读取 → 多线程 resize+保存。
    GPU 失败时自动 fallback 到 CPU。
    """
    total = sum(len(tasks) for tasks in tasks_by_size.values())
    completed = 0
    failed = 0

    print(f"\n[GPU] Processing Level {native_level}: {total} tasks")

    for size_key, group_tasks in tasks_by_size.items():
        n_groups = len(group_tasks)
        print(f"    [GPU] Size {size_key}: {n_groups} tasks")

        for i in range(0, n_groups, batch_size):
            batch = group_tasks[i:i + batch_size]

            try:
                c, f = _process_gpu_batch(
                    img, batch, size_key, native_level, output_dir, quality
                )
                completed += c
                failed += f
            except RuntimeError as e:
                err = str(e).lower()
                if "out of memory" in err or "cudamalloc" in err or \
                   "compilation" in err or "cuda_fp8" in err:
                    print(f"    [GPU] OOM/Error → CPU fallback ({len(batch)} tasks)")
                    c, f = _fallback_batch_to_cpu(img, batch, output_dir, quality)
                    completed += c
                    failed += f
                else:
                    failed += len(batch)
                    print(f"    [GPU] Unexpected error: {e}")

        # 进度报告
        if completed % 500 == 0 and completed > 0:
            print(f"    [GPU] Level {native_level} progress: {completed}/{total}")

    print(f"[GPU] Level {native_level} done: {completed}/{total}, failed: {failed}")
    return completed, failed


# ============================================================================
# CPU 层级处理（使用全局线程池）
# ============================================================================
def process_cpu_level(img, tasks_by_size, output_dir, quality):
    """
    处理单个 CPU 层级的所有任务（使用全局 CPU 线程池并行处理）。
    """
    all_tasks = []
    for group_tasks in tasks_by_size.values():
        all_tasks.extend(group_tasks)

    total = len(all_tasks)
    if total == 0:
        return 0, 0

    native_level = all_tasks[0].native_level
    print(f"\n[CPU] Processing Level {native_level}: {total} tasks")

    executor = init_cpu_executor()
    futures = [
        executor.submit(process_single_cpu_task, img, task, output_dir, quality)
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
# Level 验证
# ============================================================================
def validate_levels(img, test_size=256):
    """预验证每个 pyramid level 的 GPU 读取能力。"""
    level_dimensions = img.resolutions["level_dimensions"]
    level_downsamples = img.resolutions["level_downsamples"]
    level_device_map = {}

    print(f"\n{'=' * 60}")
    print(f"LEVEL VALIDATION (testing GPU read capability)")
    print(f"{'=' * 60}")

    for level, dim in enumerate(level_dimensions):
        w, h = dim
        ds = level_downsamples[level]
        test_w = min(test_size, w)
        test_h = min(test_size, h)

        print(f"\n  Level {level}: {w}x{h} (ds={ds:.2f})")

        # 测试 1：单次 GPU 读取
        try:
            region = img.read_region(
                location=[0, 0], size=[test_w, test_h],
                level=level, batch_size=1, num_workers=0, device="cuda",
            )
            arr = cp.asnumpy(cp.asarray(region))
            del region, arr
            single_ok = True
        except RuntimeError as e:
            print(f"    ✗ GPU single read: {e}")
            single_ok = False

        if not single_ok:
            level_device_map[level] = "cpu"
            print(f"    → CPU")
            continue

        # 测试 2：批量 GPU 读取
        try:
            locations = [[0, 0], [test_w, 0]] if w >= test_w * 2 else [[0, 0]]
            regions = img.read_region(
                location=locations, size=[test_w, test_h],
                level=level, batch_size=len(locations),
                num_workers=1, device="cuda",
            )
            for batch in regions:
                arr = cp.asnumpy(cp.asarray(batch))
                del arr
            del regions
            level_device_map[level] = "cuda"
            print(f"    ✓ GPU batch read OK")
        except RuntimeError as e:
            level_device_map[level] = "cpu"
            print(f"    ✗ GPU batch read: {e} → CPU")

    print(f"\n{'=' * 60}")
    print(f"LEVEL VALIDATION SUMMARY:")
    for lv, dev in sorted(level_device_map.items()):
        print(f"  Level {lv}: {dev.upper()}")
    print(f"{'=' * 60}")

    return level_device_map


# ============================================================================
# 任务生成
# ============================================================================
def generate_tile_tasks(img, tile_size, level_device_map=None):
    """按 Zoomify 逻辑生成瓦片任务，按 native_level 和 size 分组。"""
    level_dimensions = img.resolutions["level_dimensions"]
    level_downsamples = img.resolutions["level_downsamples"]
    level0_width, level0_height = level_dimensions[0]

    if level_device_map is None:
        level_device_map = validate_levels(img)

    max_zoom = int(np.ceil(np.log2(max(level0_width, level0_height) / tile_size)))
    max_zoom = max(max_zoom, 0)

    tasks_by_level: Dict[int, Dict[Tuple[int, int], List[TileTask]]] = {}
    gpu_levels: List[int] = []
    cpu_levels: List[int] = []

    for z in range(max_zoom + 1):
        target_downsample = 2 ** z
        level_w = int(np.ceil(level0_width / target_downsample))
        level_h = int(np.ceil(level0_height / target_downsample))
        num_tiles_x = int(np.ceil(level_w / tile_size))
        num_tiles_y = int(np.ceil(level_h / tile_size))

        native_level = _select_native_level(level_downsamples, target_downsample)
        native_downsample = level_downsamples[native_level]
        device = level_device_map.get(native_level, "cpu")

        if native_level not in tasks_by_level:
            tasks_by_level[native_level] = {}
            (gpu_levels if device == "cuda" else cpu_levels).append(native_level)

        for y in range(num_tiles_y):
            for x in range(num_tiles_x):
                sx0 = x * tile_size * target_downsample
                sy0 = y * tile_size * target_downsample
                sw0 = min(tile_size * target_downsample, level0_width - sx0)
                sh0 = min(tile_size * target_downsample, level0_height - sy0)

                if sw0 <= 0 or sh0 <= 0:
                    continue

                sw_native = max(1, int(np.ceil(sw0 / native_downsample)))
                sh_native = max(1, int(np.ceil(sh0 / native_downsample)))

                size_key = (sw_native, sh_native)
                if size_key not in tasks_by_level[native_level]:
                    tasks_by_level[native_level][size_key] = []

                target_w = int(np.ceil(sw0 / target_downsample))
                target_h = int(np.ceil(sh0 / target_downsample))

                tasks_by_level[native_level][size_key].append(
                    TileTask(
                        z=z, x=x, y=y,
                        native_level=native_level,
                        native_downsample=native_downsample,
                        target_downsample=target_downsample,
                        location=(sx0, sy0),
                        size=size_key,
                        target_size=(target_w, target_h),
                        device=device,
                    )
                )

    return {
        "gpu_levels": sorted(set(gpu_levels)),
        "cpu_levels": sorted(set(cpu_levels)),
        "tasks_by_level": tasks_by_level,
    }


# ============================================================================
# 主入口
# ============================================================================
def generate_zoomify_tiles(
    img_path,
    output_dir,
    tile_size=TILE_SIZE,
    quality=JPEG_QUALITY,
    cpu_max_workers=CPU_MAX_WORKERS,
    gpu_batch_size=8,
):
    """
    主入口：生成 Zoomify 金字塔瓦片。

    架构：
    - 全局 CPU 线程池：用于 resize + JPEG 保存（GPU 后处理 & CPU 任务共用）
    - GPU 读取顺序执行（每次只加载一层的 NvJpegProcessor，避免 OOM）
    - CPU 层级并行执行（利用多核）
    - GPU 和 CPU 层级可以并行执行（GPU 读 + CPU 后处理同时运行）
    """
    print(f"\n{'=' * 60}")
    print(f"STARTING ZOOMIFY TILE GENERATION")
    print(f"{'=' * 60}")
    print(f"Input file:     {img_path}")
    print(f"Output dir:     {output_dir}")
    print(f"Tile size:      {tile_size}x{tile_size}")
    print(f"CPU workers:    {cpu_max_workers}")
    print(f"GPU batch size: {gpu_batch_size}")
    print(f"JPEG quality:   {quality}")

    img = load_img(img_path, patch_size=(tile_size, tile_size))
    init_cpu_executor(cpu_max_workers)

    level_device_map = validate_levels(img)
    task_groups = generate_tile_tasks(img, tile_size, level_device_map)
    gpu_levels = task_groups["gpu_levels"]
    cpu_levels = task_groups["cpu_levels"]
    tasks_by_level = task_groups["tasks_by_level"]

    total = sum(
        sum(len(t) for t in lt.values()) for lt in tasks_by_level.values()
    )
    gpu_task_count = sum(
        sum(len(t) for t in tasks_by_level[lv].values()) for lv in gpu_levels
    )
    cpu_task_count = total - gpu_task_count

    print(f"\nTotal tasks: {total}  (GPU: {gpu_task_count}, CPU: {cpu_task_count})")
    for lv in sorted(gpu_levels):
        n = sum(len(t) for t in tasks_by_level[lv].values())
        print(f"  GPU Level {lv}: {n} tasks, {len(tasks_by_level[lv])} size groups")
    for lv in sorted(cpu_levels):
        n = sum(len(t) for t in tasks_by_level[lv].values())
        print(f"  CPU Level {lv}: {n} tasks, {len(tasks_by_level[lv])} size groups")

    start_time = time.perf_counter()
    results = {"gpu": [0, 0], "cpu": [0, 0]}

    # --- GPU 层级处理（必须顺序执行，避免多个 NvJpegProcessor 同时占用显存）---
    def run_gpu_levels():
        for lv in sorted(gpu_levels):
            c, f = process_gpu_level(
                img, tasks_by_level[lv], lv, output_dir, quality, gpu_batch_size
            )
            results["gpu"][0] += c
            results["gpu"][1] += f
            elapsed = time.perf_counter() - start_time
            print(f"  GPU Level {lv} Progress: {c}/{n} in {elapsed:.2f}s")

    # --- CPU 层级处理（使用全局线程池并行）---
    def run_cpu_levels():
        for lv in sorted(cpu_levels):
            c, f = process_cpu_level(img, tasks_by_level[lv], output_dir, quality)
            results["cpu"][0] += c
            results["cpu"][1] += f

    # GPU 和 CPU 层级可并行执行
    # GPU 读取是单线程的，但 resize+保存已提交到 CPU 线程池，两者可以流水线运行
    with concurrent.futures.ThreadPoolExecutor(
        max_workers=1, thread_name_prefix="GPU-Controller"
    ) as gpu_ctrl:
        gpu_future = gpu_ctrl.submit(run_gpu_levels) if gpu_levels else None
        run_cpu_levels() if cpu_levels else None

        if gpu_future:
            try:
                gpu_future.result()
            except Exception as e:
                print(f"[GPU] Fatal error: {e}")

    shutdown_cpu_executor()

    elapsed = time.perf_counter() - start_time
    total_ok = results["gpu"][0] + results["cpu"][0]
    total_fail = results["gpu"][1] + results["cpu"][1]

    print(f"\n{'=' * 60}")
    print(f"COMPLETED in {elapsed:.2f}s")
    print(f"{'=' * 60}")
    print(f"GPU: {results['gpu'][0]} ok, {results['gpu'][1]} failed")
    print(f"CPU: {results['cpu'][0]} ok, {results['cpu'][1]} failed")
    print(f"Total: {total_ok}/{total} tiles")
    if elapsed > 0:
        print(f"Throughput: {total_ok / elapsed:.1f} tiles/s")


if __name__ == "__main__":
    generate_zoomify_tiles(
        img_path=INPUT_FILE,
        output_dir=OUTPUT_DIR,
        tile_size=TILE_SIZE,
        quality=JPEG_QUALITY,
        cpu_max_workers=CPU_MAX_WORKERS,
    )

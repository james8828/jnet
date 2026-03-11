import os
import time
import openslide 
from openslide import OpenSlideCache
import numpy as np
from PIL import Image
import math
from SlideTool import slide_read,exportMeta,calculate_md5_from_array
from openslide.deepzoom import DeepZoomGenerator
from deepzoom_custom import DeepZoomGeneratorCustom

TILE_SIZE = 256

def output_zoomify_tiles(slide, output_dir):
    
    """
    创建 Zoomify 格式的瓦片
    :param slide: OpenSlide 对象
    :param output_dir: 输出目录
    """
    start_time = time.time()
    # 获取 WSI 的基本信息
    wsi_dimensions = slide.dimensions
    level_count = slide.level_count
    level_downsamples = slide.level_downsamples

    print(f"WSI dimensions: {wsi_dimensions}")
    print(f"Number of levels: {level_count}")
    print(f"Downsample factors: {level_downsamples}")
    print(f"Dimensions of each level: {slide.level_dimensions}")

    # 创建输出目录
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    dz = DeepZoomGeneratorCustom(slide,TILE_SIZE,0,True)
    # dz = DeepZoomGeneratorCustom(slide)
    dz_level_count = dz.level_count
    print(f"dz_level_count:{dz_level_count}")


    # 遍历每个层级（level）
    for level in range(dz_level_count):
        level_dir = os.path.join(output_dir, str(level))
        if not os.path.exists(level_dir):
            os.makedirs(level_dir)

        level_tile = dz.level_tiles[level]

        print(f"Level {level}: {level_tile} - {level_tile[0]}x{level_tile[1]} tiles")

        # 生成每个瓦片
        for y in range(level_tile[1]):
            for x in range(level_tile[0]):
                tile_path = os.path.join(level_dir, f"{x}_{y}.jpg")
                print(f"Saved tile {tile_path}")
                # 读取瓦片
                tile = dz.get_tile(level, (x, y))
                tile_path = os.path.join(level_dir, f"{y}_{x}.jpg")
                save_tile_to_file(tile, tile_path)

    # 创建 ImageProperties.xml 文件
    image_properties = f"""<IMAGE_PROPERTIES WIDTH="{wsi_dimensions[0]}" HEIGHT="{wsi_dimensions[1]}" NUMTILES="{dz_level_count}" NUMIMAGES="1" VERSION="1.8" TILESIZE="256" />
    """
    image_properties_path = os.path.join(output_dir, "ImageProperties.xml")
    with open(image_properties_path, "w") as f:
        f.write(image_properties)

    print(f"Created ImageProperties.xml at {image_properties_path}")
    end_time = time.time()  # Record the end time
    elapsed_time = end_time - start_time  # Calculate the elapsed time
    print(f"Time taken to generate tiles: {elapsed_time:.2f} seconds")  # Print the elapsed time

def save_tile_to_file(tile, tile_path):
    """
    保存瓦片图像到文件
    :param tile: 瓦片图像
    :param tile_path: 瓦片保存路径
    """
    if tile.mode == 'RGBA':
        tile = tile.convert('RGB')

    # 1. 生成纯色图像数据
    blank_image = np.zeros((TILE_SIZE, TILE_SIZE, 3), dtype=np.uint8)
    blank_image[:, :] = [255, 255, 255]  # 白色
    tile_np = np.array(tile)
    if (tile_np.shape[0]<TILE_SIZE) or (tile_np.shape[1]<TILE_SIZE):
        # blank_image[:, :tile_np.shape[0]] = [255, 255, 255]
        # tile_np合并到blank_image
        blank_image[:tile_np.shape[0], :tile_np.shape[1]] = tile_np
        # 将 NumPy 数组转换为 PIL 图像
        tile = Image.fromarray(blank_image)
    # 保存瓦片
    tile.save(tile_path, "JPEG")

def get_tile_from_dz(dz,level,x,y):
    
    """
    从 DeepZoomGeneratorCustom 对象中获取指定层级和坐标的瓦片
    :param dz: DeepZoomGeneratorCustom 对象
    :param level: 层级
    :param x: x 坐标
    :param y: y 坐标
    :return: 瓦片图像
    """
    tile = dz.get_tile(level, (x, y))
    
    return tile

def get_tile_from_slide(slide,level,x,y,size):
    """
    从 WSI 文件中获取指定层级和坐标的瓦片
    :param slide: OpenSlide 对象
    :param level: 层级
    :param x: x 坐标
    :param y: y 坐标
    :param size: 瓦片大小
    :return: 瓦片图像
    """
    dz = DeepZoomGeneratorCustom(slide,size,0,True)
    return get_tile_from_dz(dz,level,x,y)

def get_tile_from_file(wsi_path,level,x,y,size):

    """
    从 WSI 文件中获取指定层级和坐标的瓦片
    :param wsi_path: WSI 文件路径
    :param level: 层级
    :param x: x 坐标
    :param y: y 坐标
    :param size: 瓦片大小
    :return: 瓦片图像
    """
    slide = openslide.OpenSlide(wsi_path)
    return get_tile_from_slide(slide,level,x,y,size)


def get_tile_coordinates(slide,level,x,y,size):
    """
    从 WSI 文件中获取指定层级和坐标的瓦片轮廓
    :param slide: OpenSlide 对象
    :param level: 层级
    :param x: x 坐标
    :param y: y 坐标
    :param size: 瓦片大小
    :return: 瓦片轮廓
    """
    dz = DeepZoomGeneratorCustom(slide,size,0,True)
    tile_coordinates = dz.get_tile_coordinates(level, (x, y))
    
    return tile_coordinates

def get_tile_geojson(level, x, y, tile_size=256, image_width=4096, image_height=4096, bounds=None):
    if bounds is None:
        bounds = [0, 0, 100, 100]  # 示例地理范围，可替换为真实值

    zoom_factor = 2 ** level
    px_left = x * tile_size
    px_top = y * tile_size
    px_right = min(px_left + tile_size, image_width)
    px_bottom = min(px_top + tile_size, image_height)

    # 转换为地理坐标
    nw = pixel_to_geo(px_left, px_top, image_width, image_height, bounds)
    ne = pixel_to_geo(px_right, px_top, image_width, image_height, bounds)
    se = pixel_to_geo(px_right, px_bottom, image_width, image_height, bounds)
    sw = pixel_to_geo(px_left, px_bottom, image_width, image_height, bounds)

    geojson = {
        "type": "Feature",
        "properties": {
            "level": level,
            "x": x,
            "y": y
        },
        "geometry": {
            "type": "Polygon",
            "coordinates": [[nw, ne, se, sw, nw]]
        }
    }

    return geojson

def pixel_to_geo(x, y, image_width, image_height, bounds):
    minx, miny, maxx, maxy = bounds
    geo_x = minx + (maxx - minx) * x / image_width
    geo_y = maxy - (maxy - miny) * y / image_height
    return (geo_x, geo_y)






def main():
    # wsi_path = 'path/to/your/wsi_file.svs'
    # wsi_path = 'D:\work\WSI\R24-S030-RD 2312591-3 4F.svs'
    wsi_path = "D:\work\python\\144-v1.0\V004-S001-RD 2269524-4 4M.svs"
    slide = openslide.OpenSlide(wsi_path)
    # Create an OpenSlideCache object with a specified capacity (e.g., 100 MB)
    cache_capacity = 1 * 1024 * 1024 * 1024  # 1GB
    cache = OpenSlideCache(cache_capacity)
    slide.set_cache(cache)
    output_dir = 'path/to/output/zoomify_tiles'
    # output_zoomify_tiles(slide, output_dir)
    tile = get_tile_from_slide(slide,0,0,0,TILE_SIZE)
    print(f"tile:{tile.info}")
    slide.close()

if __name__ == "__main__":
    main()

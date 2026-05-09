import openslide
import numpy as np
import hashlib
import pandas as pd
import sys


def export_meta(wsi_path, output_excel_path='wsi_metadata.xlsx'):
    """
    导出 WSI 元数据到 Excel 文件
    :param wsi_path: WSI 文件路径
    :param output_excel_path: 输出 Excel 文件路径
    """
    slide = openslide.OpenSlide(wsi_path)
    
    # 获取元数据
    metadata = slide.properties
    
    # 将元数据转换为 DataFrame
    metadata_df = pd.DataFrame(list(metadata.items()), columns=['Key', 'Value'])
    
    # 输出到 Excel 文件
    metadata_df.to_excel(output_excel_path, index=False)
    
    slide.close()
    print(f"元数据已保存到 {output_excel_path}")


def calculate_md5_from_array(array):
    """
    计算 NumPy 数组的 MD5 哈希值
    :param array: NumPy 数组
    :return: MD5 哈希字符串
    """
    array_bytes = array.tobytes()
    hash_md5 = hashlib.md5(array_bytes).hexdigest()
    return hash_md5


def slide_read_region(wsi_path, location, size, level=0, save_path=None):
    """
    从 WSI 文件中读取指定区域
    :param wsi_path: WSI 文件路径
    :param location: 读取位置 (x, y)
    :param size: 读取尺寸 (width, height)
    :param level: 层级（默认 0）
    :param save_path: 保存路径（可选，设为 None 则不保存）
    :return: NumPy 数组格式的图像
    """
    slide = openslide.OpenSlide(wsi_path)
    
    print(f"正在读取 {wsi_path}")
    print(f"WSI dimensions: {slide.dimensions}")
    print(f"Number of levels: {slide.level_count}")
    print(f"Downsample factors: {slide.level_downsamples}")
    print(f"Dimensions of level {level}: {slide.level_dimensions[level]}")
    
    # 截取图像
    region = slide.read_region(location, level, size)
    
    # 检查图像模式是否为 RGBA，如果是则转换为 RGB
    if region.mode == 'RGBA':
        region = region.convert('RGB')
    
    # 可选：保存图像
    if save_path:
        region.save(save_path, "JPEG")
        print(f"图像已保存到 {save_path}")
    
    # 将 PIL.Image 转换为 NumPy 数组
    region_np = np.array(region)
    
    slide.close()
    return region_np


if __name__ == '__main__':
    if len(sys.argv) >= 2:
        wsi_path = sys.argv[1]
        print(f"处理文件: {wsi_path}")
        
        # 导出元数据示例
        export_meta(wsi_path, f"{wsi_path}_metadata.xlsx")
        
        # 读取区域示例
        # region = slide_read_region(wsi_path, (0, 0), (1024, 1024), level=0, save_path="output.jpg")
        # print(f"读取区域尺寸: {region.shape}")
    else:
        print("用法: python SlideTool.py <wsi_file_path>")
        print("示例: python SlideTool.py path/to/slide.svs")

import openslide
import matplotlib.pyplot as plt
import numpy as np
import hashlib
import pandas as pd

def exportMeta(wsi_path):
    # 替换为你的 WSI 文件路径
    slide = openslide.OpenSlide(wsi_path)

    # 获取元数据
    metadata = slide.properties

    # 将元数据转换为 DataFrame
    metadata_df = pd.DataFrame(list(metadata.items()), columns=['Key', 'Value'])

    # 输出到 Excel 文件
    output_excel_path = 'wsi_metadata.xlsx'
    metadata_df.to_excel(output_excel_path, index=False)

    print(f"元数据已保存到 {output_excel_path}")

def calculate_md5_from_array(array):
    """计算 NumPy 数组的 MD5 哈希值"""
    # 将 NumPy 数组转换为字节流
    array_bytes = array.tobytes()
    # 计算 MD5 哈希值
    hash_md5 = hashlib.md5(array_bytes).hexdigest()
    return hash_md5


def slide_read(wsi_path,location,size,level = 0):
    # 替换为你的 WSI 文件路径
    slide = openslide.OpenSlide(wsi_path)
    print(f"正在读取 {wsi_path}")
    # 获取 WSI 的基本信息
    print(f"WSI dimensions: {slide.dimensions}")
    print(f"Number of levels: {slide.level_count}")
    print(f"Downsample factors: {slide.level_downsamples}")
    print(f"Dimensions of each level: {slide.level_dimensions}")
    print(f"Dimensions of level {level}: {slide.level_dimensions[level]}")
    print(f"Dimensions of level {level} (in pixels): {slide.level_dimensions[level][0] * slide.level_downsamples[level], slide.level_dimensions[level][1] * slide.level_downsamples[level]}")
    # 截取图像
    region = slide.read_region(location, level, size)

    # 检查图像模式是否为 RGBA，如果是则转换为 RGB
    if region.mode == 'RGBA':
        region = region.convert('RGB')
    region.save("test1.jpg", "JPEG")
    # 将 PIL.Image 转换为 NumPy 数组
    region_np = np.array(region)
    # print(f"region_np:{region_np}")

    # 展示图像
    plt.figure(figsize=(3, 3), facecolor='lightblue')
    plt.imshow(region_np)
    plt.title('Extracted Region from WSI')
    plt.axis('off')  # 关闭坐标轴
    plt.show()
    return region_np

if __name__ == '__main__':
    wsi_path = 'D:\work\WSI\R24-S030-RD 2312591-3 4F.svs'
    slide_read(wsi_path,(1024*64,0),((1598-1024),1024),3)
    # slide_read(wsi_path,(0,0),(1598,1226),3)


    # wsi_path1 = 'D:\work\WSI\slide_data\R19-219-RD 20-1433-12 4M.svs'
    # region_np1 = slide_read(wsi_path1,15835,10162,289,310)
    # print(f"region_np1:{region_np1}")
    # wsi_path2 = 'D:\work\WSI\slide_data\R19-219-RD 20-1433-12 4M.tif'
    # region_np2 = slide_read(wsi_path2,0,0,289,310)
    # print(f"region_np2:{region_np2}")
    # # md5比较
    # md5_1 = calculate_md5_from_array(region_np1)
    # md5_2 = calculate_md5_from_array(region_np2)
    # are_identical = md5_1 == md5_2
    # print(f"MD5 comparison between {wsi_path1} and {wsi_path2}: {'Identical' if are_identical else 'Different'}")
    # exportMeta(wsi_path1)

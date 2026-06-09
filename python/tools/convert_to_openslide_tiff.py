import sys
import cv2
import os
from tifffile import TiffWriter

def convert_jpg_to_openslide_tiff(input_path, output_path):
    """
    将 JPG/PNG 转换为 OpenSlide 兼容的金字塔 TIFF
    :param input_path: 输入文件路径
    :param output_path: 输出文件路径
    """
    try:
        # 1. 读取图像 (BGR)
        img = cv2.imread(input_path)
        if img is None:
            print(f"Error: Cannot read image {input_path}")
            return 1
        
        # 2. 转换为 RGB (OpenSlide 期望 RGB)
        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        
        height, width = img_rgb.shape[:2]
        print(f"Source image size: {width}x{height}")
        
        # 3. 写入金字塔 TIFF
        with TiffWriter(output_path, bigtiff=True) as tif:
            # Level 0: 原始分辨率
            tif.save(
                img_rgb,
                photometric='rgb',
                compression='jpeg',      # 使用 JPEG 压缩以减小体积
                tile=(256, 256),         # 分块存储，OpenSlide 必需
                subsampling=(1, 1),
                subfiletype=0,           # 主图像
                resolution=(1000, 1000), # 模拟 DPI
                metadata={
                    'openslide.mpp-x': '0.25',  # 假设 0.25 um/px (40x)
                    'openslide.mpp-y': '0.25',
                    'scanner_model': 'JNet Converter'
                }
            )
            
            # Level 1 - N: 降采样金字塔
            level = 1
            current_img = img_rgb
            while True:
                # 缩小一半
                h, w = current_img.shape[:2]
                if max(h, w) < 256: break
                
                current_img = cv2.resize(current_img, (w // 2, h // 2), interpolation=cv2.INTER_AREA)
                
                tif.save(
                    current_img,
                    photometric='rgb',
                    compression='jpeg',
                    tile=(256, 256),
                    subsampling=(1, 1),
                    subfiletype=9,       # 9 表示 ReducedResolutionImage
                )
                level += 1
                print(f"Generated pyramid level {level}: {current_img.shape[1]}x{current_img.shape[0]}")
                
        print(f"Success: {output_path}")
        return 0
        
    except Exception as e:
        print(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    # 调试模式：直接使用硬编码路径
    # input_file = r"E:\doc\jnet\imageStore\project_2\dev-batch\sample.jpg"
    # output_file = r"E:\doc\jnet\imageStore\project_2\dev-batch\sample_openslide.tif"
    
    # # 检查输入文件是否存在
    # if not os.path.exists(input_file):
    #     print(f"Error: Input file not found: {input_file}")
    #     sys.exit(1)
    
    # print(f"Converting: {input_file}")
    # print(f"Output to: {output_file}")
    
    # result = convert_jpg_to_openslide_tiff(input_file, output_file)
    # sys.exit(result)
    
    # 命令行模式（注释掉）
    if len(sys.argv) != 3:
        print("Usage: python convert_to_openslide_tiff.py <input.jpg/png> <output.tif>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    sys.exit(convert_jpg_to_openslide_tiff(input_file, output_file))

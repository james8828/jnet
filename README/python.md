```python
import os
import cv2
import yaml
import time
import queue
import torch
import random
import tifffile
import importlib
import threading
import openslide
import numpy as np
from tqdm import tqdm
from typing import List, Tuple
from Write_new_json import save_new_json
from polygraphy.backend.trt import EngineFromBytes, TrtRunner
# 动态引用替换

import ctypes

libgcc_s = ctypes.CDLL('libgcc_s.so.1')

import warnings

# 忽略所有 UserWarning 类型的警告
warnings.filterwarnings("ignore", category=UserWarning)


class Preprocess():
    def __init__(self):
        pass

    def get_coords_with_conts(self, out_conts, conts_ds: int = 1, coords_ds: int = 1, size: int = 640, step: int = 640,
                              foreground_rate: float = 0.01, allow_overflow=True):
        """
        根据外轮廓，获取指定倍率、大小、步长的小图坐标。
        :param out_conts: 脏器外轮廓
        :param conts_ds: 脏器外轮廓的缩放倍率
        :param coords_ds: 输出coord的缩放倍率
        :param size: 图片大小
        :param step: 图片切割步长
        :param foreground_rate: 图片内前景面积最小占比（脏器组织占比）
        :param allow_overflow: 是否允许越界。允许越界，坐标可能超出svs图片边界。不允许越界，则对越界坐标进行回退到最大值-图片大小。
        :return: 指定缩放倍率下的小图坐标列表。[array([[x_min,y_min],[x_max,y_max]]),array([[x_min,y_min],[x_max,y_max]]),...]
        """
        # print(size)
        assert 0 <= foreground_rate <= 1, "foreground_rate 参数,最小前景占比必须在[0,1]内"

        # 计算轮廓的边界框
        x_min, y_min, x_max, y_max = float('inf'), float('inf'), float('-inf'), float('-inf')
        for out_cont in out_conts:
            x0, y0, w0, h0 = cv2.boundingRect(out_cont)
            x_min, y_min = min(x_min, x0), min(y_min, y0)
            x_max, y_max = max(x_max, x0 + w0), max(y_max, y0 + h0)

        # 创建掩膜
        mask_h, mask_w = int(y_max - y_min), int(x_max - x_min)
        mask = np.zeros((mask_h, mask_w), dtype=np.uint8)
        for out_cont in out_conts:
            cv2.drawContours(mask, [out_cont - [x_min, y_min]], -1, 255, -1)

        # 计算内部缩放比例
        level_ds = coords_ds / conts_ds
        size_ = int(size * level_ds)
        step_ = int(step * level_ds)

        coord_list = []
        for x in range(x_min, x_max, step_):
            if x + size_ > x_max and not allow_overflow:
                x = x_max - size_

            for y in range(y_min, y_max, step_):
                if y + size_ > y_max and not allow_overflow:
                    y = y_max - size_

                # 计算当前窗口的前景面积比例
                area = cv2.countNonZero(mask[y - y_min:y - y_min + size_, x - x_min:x - x_min + size_])
                rate_ = area / (size_ * size_)
                if rate_ > foreground_rate:
                    # print([int(x / level_ds + size), int(y / level_ds + size)])
                    coord_list.append(np.array([[int(x / level_ds), int(y / level_ds)],
                                                [int(x / level_ds + size), int(y / level_ds + size)]]))

        return coord_list

    def get_coords_with_mask(self, out_mask, mask_ds: int = 1, coords_ds: int = 1, size: int = 640, step: int = 640,
                             foreground_rate: float = 1.0, allow_overflow=True):
        """
        根据外轮廓mask，获取指定倍率、大小、步长的小图坐标。
        :param out_mask: 脏器外轮廓mask
        :param mask_ds: 脏器外轮廓mask的缩放倍率
        :param coords_ds: 输出coord的缩放倍率
        :param size: 图片大小
        :param step: 图片切割步长
        :param foreground_rate: 图片内前景面积最小占比（脏器组织占比）
        :param allow_overflow: 是否允许越界。允许越界，坐标可能超出svs图片边界。不允许越界，则对越界坐标进行回退到最大值-图片大小。
        :return: 指定缩放倍率下的小图坐标列表。[array([[x_min,y_min],[x_max,y_max]]),array([[x_min,y_min],[x_max,y_max]]),...]
        """
        assert 0 <= foreground_rate <= 1, "foreground_rate 参数,前景占比必须在[0,1]内"
        x_min, y_min = 0, 0
        y_max, x_max = out_mask.shape[:2]

        level_ds = coords_ds / mask_ds
        size_ = int(size * level_ds)
        step_ = int(step * level_ds)

        coord_list = []
        for x in range(x_min, x_max, step_):
            if x + size_ > x_max and not allow_overflow:
                x = x_max - size_

            for y in range(y_min, y_max, step_):
                if y + size_ > y_max and not allow_overflow:
                    y = y_max - size_

                # 计算当前窗口的前景面积比例
                area = cv2.countNonZero(out_mask[y - y_min:y - y_min + size_, x - x_min:x - x_min + size_])
                rate_ = area / (size_ * size_)
                if rate_ > foreground_rate:
                    coord_list.append(np.array([[int(x / level_ds), int(y / level_ds)],
                                                [int(x / level_ds + size), int(y / level_ds + size)]]))
        return coord_list


def read_Wt_conts(Wt_conts):
    """

    :param Wt_conts: {'PA':  # 类别简称
                     [{'amendment': True,  # 是否修改
                       'cont': [[[1, 2]], [[3, -4.444]]]},  # PA轮廓1数组
                      {'amendment': False,
                       'cont': [[[11, 12]], [[13, 14]]]}],  # PA轮廓2数组
                     'OS':
                         [{'amendment': False,  # 是否修改
                           'cont': [[[21, 22]], [[23, 24]]]},  # OS轮廓1数组
                          {'amendment': True,
                           'cont': [[[31, 32]], [[33, 34]]]}],  # OS轮廓2数组
                     }
    :return: wt_cnts_dict_ds1
    """
    wt_cnts_dict_ds1 = {}
    for label, data_list in Wt_conts.items():
        conts = []
        for data in data_list:
            cont = np.abs(np.array(data['cont'], dtype=np.int32).reshape(-1, 1, 2))
            conts.append(cont)
        wt_cnts_dict_ds1[label] = conts
    return wt_cnts_dict_ds1


class CustomDataset:
    def __init__(self, slide, coord_list: list, coord_ds: int, config, transform_func=None,
                 device=torch.device('cuda:0')):
        """
          Initializes the CustomDataset.

          Parameters:
          - slide: The slide object containing image data.
          - coord_list: List of coordinates for regions of interest.
          - coord_ds: Downsampling factor for coordinates.
          - transform_func: Function to transform the image data.
          - device: Device to run transformations on (e.g., 'cuda:0').
          - config: Model configuration parameters
        """
        self.slide = slide
        self.coord_list = coord_list
        self.coord_ds = coord_ds
        self.transform_func = transform_func
        self.device = device
        self.page, self.resize_ds = self._get_page_ds(coord_ds)
        self.config = config

    def _get_page_ds(self, roi_ds):
        """
        Determines the appropriate page and resize downsample factor.

        Parameters:
        - roi_ds: The downsampling factor for the region of interest.

        Returns:
        - Tuple of page index and resize downsample factor.
        """
        for i, ds in enumerate(self.slide.level_downsamples):
            if int(ds) <= int(roi_ds):
                continue
            return i - 1, roi_ds / (self.slide.level_downsamples[i - 1])
        return len(self.slide.level_downsamples) - 1, roi_ds / self.slide.level_downsamples[-1]

    def _get_coord_img(self, coord):
        """
        Retrieves the image for a given set of coordinates.

        Parameters:
        - coord: Coordinates for the region of interest.

        Returns:
        - The image array for the specified coordinates.
        """
        coord = np.array(coord)
        x0, y0 = coord[0]
        w, h = coord[1] - coord[0]

        if self.coord_ds == 1:
            img = np.array(self.slide.read_region((x0, y0), 0, (w, h)))
        else:
            x0 = int(x0 * self.coord_ds)
            y0 = int(y0 * self.coord_ds)
            w = int(w * self.resize_ds)
            h = int(h * self.resize_ds)
            img = np.array(self.slide.read_region((x0, y0), self.page, (w, h)))
            img = cv2.resize(img, (0, 0), fx=1 / self.resize_ds, fy=1 / self.resize_ds,
                             interpolation=cv2.INTER_LANCZOS4)
        return img

    def __len__(self):
        return len(self.coord_list)

    def __getitem__(self, idx):
        coord = self.coord_list[idx]
        img = self._get_coord_img(coord)
        img = img[:, :, :3]

        if self.transform_func:
            img = self.transform_func(img)
            img = img.unsqueeze(0)
        return [coord, img]


class DataLoader:
    def __init__(self, dataset, batch_size=32, shuffle=True, num_workers=1):
        """
        Initializes the DataLoader.

        Parameters:
        - dataset: The dataset to load data from.
        - batch_size: Number of samples per batch.
        - shuffle: Whether to shuffle the data before loading.
        - num_workers: Number of worker threads to use for data loading.
        """
        self.dataset = dataset
        self.batch_size = batch_size
        self.shuffle = shuffle
        self.num_workers = num_workers
        self.data_queue = queue.Queue(maxsize=batch_size * num_workers)
        self.index_queue = queue.Queue()
        self.threads = []

        self._init_workers()

    def _init_workers(self):
        """
        Initializes the worker threads for data loading.
        """
        for _ in range(self.num_workers):
            thread = threading.Thread(target=self._worker)
            thread.daemon = True
            thread.start()
            self.threads.append(thread)

    def _worker(self):
        """
        Worker thread function for fetching data.
        """
        while True:
            index = self.index_queue.get()
            if index is None:
                break
            data = self.dataset[index]
            self.data_queue.put(data)
            self.index_queue.task_done()

    def _load_data(self):
        """
        Loads data indices into the queue, shuffling if necessary.
        """
        indices = list(range(len(self.dataset)))
        if self.shuffle:
            random.shuffle(indices)

        for index in indices:
            self.index_queue.put(index)

        for _ in range(self.num_workers):
            self.index_queue.put(None)

    def __iter__(self):
        self._load_data()
        return self

    def __next__(self):
        coords = []
        imgs = []
        for _ in range(self.batch_size):
            try:
                coord, img = self.data_queue.get(timeout=1)
                imgs.append(img)
                coords.append(np.array(coord))
            except queue.Empty:
                if not any(thread.is_alive() for thread in self.threads):
                    if imgs:
                        return coords, torch.vstack(imgs)
                    raise StopIteration
        return coords, torch.vstack(imgs)

    def __len__(self):
        return (len(self.dataset) + self.batch_size - 1) // self.batch_size


class ImgFunc():

    def get_patch_img(self, img, coord):
        '''
        获取坐标内的小图

        :param img: img
        :param coord: [[xmin,ymin],[xmax,ymax]]
        :return: patch_img
        '''
        patch_img = img[coord[0][1]:coord[1][1], coord[0][0]:coord[1][0]]
        return patch_img

    def get_patch_img_list(self, img, coord_list):
        """
        读取一批图片

        :param img: 原始图
        :param coord_list: [[[xmin,ymin],[xmax,ymax]],
                            [[xmin,ymin],[xmax,ymax]],...]
        :return:
        """
        patch_img_list = []
        for coord in coord_list:
            patch_img = self.get_patch_img(img, coord)
            patch_img_list.append(patch_img)
        return patch_img_list


class TiffImg(ImgFunc):
    def __init__(self, svs_path):
        super().__init__()
        self.tif = tifffile.TiffFile(svs_path)
        self.level_dimensions, self.level_downsamples, self.level = self._get_property()
        self.slide_name = os.path.splitext(os.path.basename(svs_path))[0]

    def _get_property(self):
        '''
        获取常用属性

        :return: 图片维度列表、缩放比例列表、页列表
        '''
        shape = []
        level_ds_list = []
        page_list = []
        for i, page in enumerate(self.tif.pages):
            #             print(str(page.compression)) # 如果读不到图请去掉注释查看编码格式。
            if str(page.compression) == 'COMPRESSION.APERIO_JP2000_RGB' or 'COMPRESSION.JPEG':  # 如果读不到图检查编码格式。更换
                shape.append(page.shape[:2])
                level_ds_list.append(shape[0][0] / page.shape[:2][0])
                page_list.append(int(i))
        return shape, level_ds_list, page_list

    def get_img_ds(self, roi_level_ds):
        """
        读取缩略图

        :param roi_level_ds: 缩小倍数
        :return: 缩略图
        """

        def get_page_ds(roi_level_ds):
            for i, level_ds in enumerate(self.level_downsamples):
                if int(level_ds) <= int(roi_level_ds):
                    continue
                return self.level[i - 1], roi_level_ds / int(self.level_downsamples[i - 1])
            return self.level[-1], roi_level_ds / int(self.level_downsamples[-1])

        page, level_ds = get_page_ds(roi_level_ds)
        img = self.tif.pages[page].asarray()
        if level_ds != 1.:
            resize_img = cv2.resize(img, (0, 0), fx=1 / level_ds, fy=1 / level_ds, interpolation=cv2.INTER_LANCZOS4)
            #             resize_img = cv2.resize(img, (0, 0), fx=1 / level_ds, fy=1 / level_ds, interpolation=cv2.INTER_AREA)
            return resize_img
        return img

    def get_coord_img(self, coord):
        """
        切coord坐标内的图

        :param coord: [[x_min,y_min],[x_max,y_max]]
        :return: 返回图片
        """
        img_ds = self.get_img_ds(1)
        coord_img = self.get_patch_img(img_ds, coord)
        return coord_img

    def get_coord_img_list(self, coord_list):
        '''
        读取一批图片

        :param coord_list: [[[xmin,ymin],[xmax,ymax]],
                            [[xmin,ymin],[xmax,ymax]],...]
        :return: 图片列表
        '''
        img_ds = self.get_img_ds(1)
        img_list = self.get_patch_img_list(img_ds, coord_list)
        return img_list

    def get_cls_img_list(self, coord_list):
        """
        投票分类数据

        :param coord_list:
        :return:
        """
        img_list = []
        for coords in coord_list:
            img_list.append(self.get_coord_img_list(coords))
        return img_list

    def get_img_ds_shape(self, level_ds):
        """
        获取缩小后的图片宽高

        :param level_ds: 缩放比例
        :return: 宽、高
        """

        w0, h0 = self.level_dimensions[0]
        w_ds = int(w0 / level_ds)
        h_ds = int(h0 / level_ds)
        return h_ds, w_ds


def is_svs_file(svs_path):
    try:
        slide = TiffImg(svs_path)
        return True
    except:
        return False


def load_yaml_config(config_str):
    """
    加载并解析YAML配置
    返回：解析后的字典对象
    """
    try:
        with open(config_str, "r") as file:
            config = yaml.safe_load(file)  # 直接传入文件对象
        return config
    except yaml.YAMLError as e:
        raise ValueError(f"YAML解析失败: {str(e)}")


# 辅助函数定义（根据实际项目结构调整位置）
def validate_svs_file(file_path: str) -> None:
    """验证SVS文件有效性"""
    assert file_path.lower().endswith('.svs'), "文件格式错误：必须为.svs格式"
    try:
        with openslide.OpenSlide(file_path) as _:
            pass
    except Exception as e:
        raise RuntimeError(f"SVS文件损坏或不可读: {str(e)}")


def validate_config(model_config):
    required_keys = {'structure_dsf', 'cut_size', 'cut_stride', 'batch_size'}
    actual_keys = set(model_config.keys())
    missing = required_keys - actual_keys

    if missing:
        raise KeyError(f"配置文件缺少必要参数: {', '.join(missing)}")
    print("✅ 配置验证通过")


def array_to_STAI(img, end_path, page_amount=4, sampel=2):
    with tifffile.TiffWriter(end_path, bigtiff=True, append=True) as tif_new:
        tif_new.save(img,
                     photometric='rgb',
                     compress='jpeg',
                     planarconfig='CONTIG',
                     tile=(256, 256),
                     subsampling=(1, 1),
                     subfiletype=9,
                     )
        for i in range(page_amount):
            img = cv2.resize(img, (0, 0), fx=1 / sampel, fy=1 / sampel)
            tif_new.save(img,
                         photometric='rgb',
                         compress='jpeg',
                         planarconfig='CONTIG',
                         tile=(256, 256),
                         subsampling=(1, 1),
                         subfiletype=9,
                         )


def import_and_log_maps(module_path, function_name=None):
    """
    动态导入模块，并记录导入日志
    :param module_path: 模块路径（如'engine.code.LI.cell_nucleus.Pretreatment'）
    :param function_name: 可选，函数/类名（若为None则导入整个模块）
    :return: 导入的模块或函数/类
    """
    try:
        # 导入模块
        module = importlib.import_module(module_path)

        # 若指定了函数/类名，返回具体对象；否则返回模块
        if function_name:
            obj = getattr(module, function_name)
            return obj
        return module
    except Exception as e:
        raise  # 抛出异常，中断流程（或根据需求处理）


def from_model(model_config):
    modeling_path = model_config['Modeling_rt']
    pretreatment_path = model_config['Pretreatment']

    pretreatment_module = import_and_log_maps(module_path=pretreatment_path, function_name=None)
    Pretreatment = getattr(pretreatment_module, 'Pretreatment')

    modeling_module = import_and_log_maps(module_path=modeling_path, function_name=None)
    Modeling_rt = getattr(modeling_module, 'Modeling_rt')
    return Pretreatment, Modeling_rt


def from_final(config, label_code):
    aftercure_path = config[label_code]['Aftercure']
    aftercure_module = import_and_log_maps(module_path=aftercure_path, function_name=None)
    Aftercure = getattr(aftercure_module, 'Aftercure')
    return Aftercure


def Engineering_testing(
        svs_path: str,
        out_conts: List[Tuple[int, int, int, int]],
        model_config: str,
        pre_num_workers: int,
        DEVICE: str,
        Pretreatment,
        Modeling_rt
) -> List[Tuple[int, int, int, int]]:
    # 模型加载与计时
    print(f"[INFO] 正在加载模型: {model_config['model_path']}")
    model_load_start = time.time()

    cell_model = Modeling_rt()
    model = cell_model.Load_model(model_config['model_path'])

    model_load_time = time.time() - model_load_start
    print(f"[TIMING] 模型加载耗时: {model_load_time:.2f}秒")

    # 数据准备流程
    print(f"[INFO] 正在准备数据: {svs_path}")
    slide = openslide.OpenSlide(svs_path)
    preprocess = Preprocess()

    # 获取裁剪坐标（带下采样）
    out_conts_ds = 1
    coord_list = preprocess.get_coords_with_conts(
        out_conts,
        out_conts_ds,  # 结构下采样因子
        model_config['structure_dsf'],
        model_config['cut_size'],  # 裁剪块尺寸
        model_config['cut_stride']  # 滑动步长
    )
    #    print(coord_list)
    # 创建数据集与加载器
    dataset = CustomDataset(
        slide,
        coord_list,
        model_config['structure_dsf'],
        model_config,
        transform_func=Pretreatment,  # 预处理函数
        device=DEVICE
    )

    dataloader = DataLoader(
        dataset,
        batch_size=model_config['batch_size'],
        shuffle=False,
        num_workers=pre_num_workers,
    )

    # 预测流程初始化
    print(f"[INFO] 启动预测流程，总批次: {len(dataloader)}")

    masks = []
    coords_list = []
    save_path = r'./save_img'
    save_mask = r'./save_mask'
    n = 0
    with TrtRunner(model) as runner:  # 使用上下文管理的方式执行trt engine的推理
        for batch_idx, data_batch in tqdm(
                enumerate(dataloader),
                total=len(dataloader),
                desc="预测进度"
        ):
            # 模型预测
            coords, imgs = data_batch
            #

            imgs = imgs.cpu().numpy().astype(np.float32)
            # print(imgs[0].transpose(1, 2, 0).shape)
            #
            predict_np = cell_model.Model_predect(imgs, runner)
            # print('aaaaaaaaaaaaaa',predict_np)
            for predict in predict_np:
                masks.append(predict)
                # cv2.imwrite(f'{save_mask}/{n}.png', predict)
                # cv2.imwrite(f'{save_path}/{n}.png', imgs[0].transpose(1, 2, 0)*255)
                # n += 1
            for coord in coords:
                coords_list.append(coord)

    return masks, coords_list
    # 最终轮廓生成


if __name__ == '__main__':
    # 病理切片文件路径（.svs为常用病理图像格式）
    #    svs_path = './ST18Rm-ES-LA-TR-316-1-000001.svs'
    #    svs_path = '/Engineering_V1.0/algo2/gy/svs/ST16Rf-ES-LA-TR-282-1-000022-1F.svs'
    #     svs_path = '/Engineering_V1.0/algo2/gy/svs/ST20Rm-OE-SE-337-1-4.svs'
    #    svs_path = '/Engineering_V1.0/algo2/gy/svs/ST20Rf-AO-HE-LU-320-1-000018.svs'
    svs_path = '/engine/svs/ST16Rf-ES-LA-TR-282-1-000024-1F.svs'  # 食管

    # 算法配置文件路径（YAML格式，包含模型参数/处理规则等）
    #    yaml_config = './RTR/14D.yaml'
    #     yaml_config = './ROE/13F.yaml'
    #    yaml_config = './RSE/140.yaml'
    #    yaml_config = './RSE/15D.yaml'
    yaml_config = '/engine/engine/code/RES/10F.yaml'

    # 并行处理配置（切图/拼图阶段使用的进程数）
    pre_num_workers = 8

    # 脏器区域坐标（格式：[左上x, 左上y, 右下x, 右下y] 像素坐标系）
    #    roi_coor = [59164, 18984, 68753, 33151]
    # 气管1
    #    roi_coor = [8472, 16873, 21313, 27121]
    # 气管1
    #     roi_coor = [70679, 19179, 79587, 28526] # OE
    #    roi_coor = [61334, 23935, 74102, 37732] # SE
    #    roi_coor = [37496, 69387, 47912, 76482] # AO
    roi_coor = [10188, 6823, 22730, 16862]  # ES

    # 计算设备配置（使用第一块NVIDIA GPU进行加速）
    DEVICE = "cuda:3"

    # 目标解剖结构编码（对应病理数据库中的分类编号）
    #    label_code = '14D036'
    #     label_code = '13F0BA'
    #    label_code = '1400BA'
    #    label_code = '15D113'
    label_code = '10F120'

    # 脏器类型编码（三位数编码系统，如112代表特定脏器）
    #    viscera_label = '14D'
    #     viscera_label = '13F'
    #    viscera_label = '140'
    #    viscera_label = '15D'
    viscera_label = '10F'

    # 结构依赖参数（指定依赖的预训练模型版本号）
    structure_rely = 'depend_model_number'

    config = load_yaml_config(yaml_config)[viscera_label]

    model_list = config[label_code][structure_rely]

    start_time = time.time()
    ## 解析
    min_x, min_y, max_x, max_y = roi_coor  # ds1
    out_conts = [
        np.array(
            [[[min_x, min_y]], [[max_x, min_y]], [[max_x, max_y]], [[min_x, max_y]]]
        )
    ]  # 模拟ds1的粗轮廓
    # 参数配置加载与验证

    all = {}

    for num in model_list:
        model_num = 'model' + num
        model_config = config[model_num]

        validate_config(model_config)  # 新增配置验证函数（需自行实现）

        # 文件格式验证
        validate_svs_file(svs_path)  # 封装断言为验证函数

        Pretreatment, Modeling_rt = from_model(model_config)
        masks, coord_list = Engineering_testing(svs_path, out_conts, model_config, pre_num_workers, DEVICE,
                                                Pretreatment, Modeling_rt)
        all[num] = {'masks': masks, 'coords': coord_list}
    print("[INFO] 生成最终轮廓...")
    Aftercure = from_final(config, label_code)
    cell_after = Aftercure(out_conts, pre_num_workers)
    final_contours = cell_after.run(
        svs_path,
        all,  # 预测结果mask
        #        roi_coor, ##模拟ds1的粗轮廓 gy
    )
    # final_contours, Nested_contour
    save_new_json(svs_path, final_contours, os.path.join('./', label_code + '.json'), label_code, '')
    print('最终轮廓数量：', len(final_contours))
    print('END Time:', time.time() - start_time)#

```
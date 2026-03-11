import { API_PREFIX } from '@constants/api-constants';
import serviceAxios from '@utils/request';
import { R } from "@types/common";
import {Slide,SlidePageQueryVo} from "@types/slide"

// Slide 相关请求前缀
const SLIDE_API = `${API_PREFIX.IMAGE}/slide`;

/**
 * @description 添加切片
 */
export async function addSlide(): Promise<R<string>> {
    return serviceAxios.post(`${SLIDE_API}/add`);
}

/**
 * @description 分页查询切片列表
 * @param req - 查询参数
 */
export async function getSlidePage(req: SlidePageQueryVo): Promise<R<Slide>> {
    return serviceAxios.post(`${SLIDE_API}/page`, req);
}

/**
 * @description 删除切片
 * @param slideId
 */
export async function deleteSlide(slideId: number): Promise<R<boolean>> {
    return serviceAxios.delete(`${SLIDE_API}/delete/${slideId}`);
}

/**
 * @description 获取切片详情
 * @param slideId - 切片ID
 */
export async function getSlideById(slideId: number): Promise<R<Slide>> {
    return serviceAxios.get(`${SLIDE_API}/getSlide/${slideId}`);
}

/**
 * @description 获取缩略图
 * @param slideId - 切片ID
 */
export async function getThumbnailImage(slideId: number): Promise<Blob> {
    return serviceAxios.get(`${SLIDE_API}/getThumbnailImage/${slideId}`, {
        responseType: 'blob',
    });
}




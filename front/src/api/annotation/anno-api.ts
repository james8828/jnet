import {API_PREFIX} from '@constants/api-constants';
import serviceAxios from '@utils/request';
import {
    Annotation,
    AnnotationUpdateVo,
    Geometry,
    AnnotationFeature,
    AnnotationQuery,
} from '@types/anno.ts';
import {R} from '@types/common.ts'

const ANNOTATION_API_PREFIX = API_PREFIX.ANNOTATION + '/annotation';

export const annotationApi = {
    /**
     * 添加标注
     * @param data 请求参数
     */
    addAnnotation(data: Annotation): Promise<R<Annotation>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/insert`, data);
    },

    /**
     * 删除标注
     * @param id 标注ID
     */
    deleteAnnotation(id: number): Promise<R<string>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/delete/${id}`);
    },

    /**
     * 更新标注
     * @param data 请求参数
     */
    updateAnnotation(data: AnnotationUpdateVo): Promise<R<string>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/update`, data);
    },

    /**
     * 填充轮廓
     * @param data 请求参数
     */
    padding(data: AnnotationUpdateVo): Promise<R<string>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/padding`, data);
    },

    /**
     * 复制/粘贴轮廓
     * @param data 请求参数
     */
    stickup(data: AnnotationUpdateVo): Promise<R<string>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/stickup`, data);
    },

    /**
     * 轮廓合并预览
     * @param data 轮廓ID列表
     */
    mergePreview(data: number[]): Promise<R<Geometry>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/mergePreview`, data);
    },

    /**
     * 获取GeoJson数据
     * @param data 查询条件
     */
    selectLists(data: AnnotationQuery): Promise<R<AnnotationFeature[]>> {
        return serviceAxios.post(`${ANNOTATION_API_PREFIX}/selectLists`, data);
    }
};




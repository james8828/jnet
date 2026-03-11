import {
    ChunkUploadVO,
    Attachment,
    AttachmentVO, R
} from '@types/common.ts'
import serviceAxios from '@utils/request.ts'
import {API_PREFIX} from "@constants/api-constants.ts";

const ATTACHMENT_API = `${API_PREFIX.IMAGE}/attachment`;


/**
 * @description 初始化分片上传任务
 */
export async function initiateMultipartUpload(): Promise<R<string>> {
    return serviceAxios.get(`${ATTACHMENT_API}/initiateMultipartUpload`)
}

/**
 * @description 分片上传
 * @param chunkVO 分片上传参数对象
 */
export async function uploadChunk(formData:FormData): Promise<R<boolean>> {
    return serviceAxios.post(`${ATTACHMENT_API}/uploadChunk`, formData,{
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    })
}

/**
 * @description 完成分片上传并合并
 * @param chunkVO 分片上传参数对象
 */
export async function completeMultipartUpload(chunkVO: ChunkUploadVO): Promise<R<Attachment>> {
    return serviceAxios.post(`${ATTACHMENT_API}/completeMultipartUpload`, chunkVO)
}

/**
 * @description 附件上传（单文件）
 * @param name 文件名
 * @param md5 文件MD5
 * @param file 文件对象
 */
export async function uploadAttachment(name: string, md5: string, file: File): Promise<R<Attachment>> {
    const formData = new FormData()
    formData.append('name', name)
    formData.append('md5', md5)
    formData.append('file', file)

    return serviceAxios.post(`${ATTACHMENT_API}/upload`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

/**
 * @description 下载附件
 * @param attachmentVO 查询条件
 */
export async function downloadAttachment(attachmentVO: AttachmentVO): Promise<Blob> {
    const params = new URLSearchParams()
    Object.keys(attachmentVO).forEach(key => {
        const value = (attachmentVO as any)[key]
        if (value !== undefined && value !== null) {
            params.append(key, value)
        }
    })

    return serviceAxios.get(`${ATTACHMENT_API}/download?${params.toString()}`, {
        responseType: 'blob'
    })
}

/**
 * @description 删除附件
 * @param attachmentCode 附件编码
 */
export async function deleteAttachment(attachmentCode: string): Promise<R<void>> {
    return serviceAxios.delete(`${ATTACHMENT_API}/delete?attachmentCode=${attachmentCode}`)
}

/**
 * @description 分页查询附件
 * @param pageNum 当前页码
 * @param pageSize 每页数量
 */
export async function getAttachmentPage(pageNum: number, pageSize: number): Promise<R<Page<Attachment>>> {
    return serviceAxios.get(`${ATTACHMENT_API}/page?pageNum=${pageNum}&pageSize=${pageSize}`)
}

/**
 * @description 条件查询附件列表
 * @param attachmentVO 查询条件
 */
export async function queryAttachments(attachmentVO: AttachmentVO): Promise<R<List<Attachment>>> {
    return serviceAxios.get(`${ATTACHMENT_API}/query`, {
        params: attachmentVO
    })
}

/**
 * @description 根据附件编码集合查询附件
 * @param attachmentCodes 附件编码集合
 */
export async function queryByAttachmentCodes(attachmentCodes: string[]): Promise<R<Attachment[]>> {
    return serviceAxios.post(`${ATTACHMENT_API}/queryByAttachmentCodes`, attachmentCodes)
}

// 类型定义补充（如未定义）
declare type List<T> = T[]
declare type Page<T> = {
    records: T[]
    total: number
    size: number
    current: number
}

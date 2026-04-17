package com.jnet.biz.service;

import com.jnet.biz.dto.ChunkUploadDTO;
import com.jnet.biz.dto.ChunkUploadInitDTO;
import com.jnet.biz.vo.ChunkUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图像分片上传 Service 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public interface IChunkUploadService {

    /**
     * 初始化分片上传
     *
     * @param initDTO 初始化参数
     * @return 上传信息（支持秒传）
     */
    ChunkUploadVO initUpload(ChunkUploadInitDTO initDTO);

    /**
     * 上传分片
     *
     * @param uploadDTO 分片数据
     * @return 是否成功
     */
    Boolean uploadChunk(ChunkUploadDTO uploadDTO);

    /**
     * 合并分片
     *
     * @param fileMd5 文件MD5
     * @param batchId 批次ID
     * @param filename 原始文件名
     * @param pathologyId 病理报告号
     * @param patientId 患者ID
     * @return 图像ID
     */
    Long mergeChunks(String fileMd5, Long batchId, String filename, String pathologyId, String patientId);

    /**
     * 取消上传（清理临时文件）
     *
     * @param fileMd5 文件MD5
     */
    void cancelUpload(String fileMd5);

    /**
     * 检查文件是否已存在（秒传）
     *
     * @param fileMd5 文件MD5
     * @return 是否存在
     */
    Boolean checkFileExists(String fileMd5);
}

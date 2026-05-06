/**
 * 分片上传器 - 支持断点续传、秒传检测
 */
import SparkMD5 from 'spark-md5'
import { initChunkUpload, uploadChunk, mergeChunks, cancelUpload } from '@/api/images'
import type { ChunkUploadInitDTO, ChunkUploadVO } from '@/types/image'

export interface UploadOptions {
  chunkSize?: number // 分片大小（字节），默认5MB
  concurrent?: number // 并发上传数，默认3
  batchId: number // 批次ID
  pathologyId?: string // 病理号
  patientId?: string // 患者ID
  onProgress?: (progress: number) => void // 上传进度回调
  onSuccess?: (imageId: number) => void // 上传成功回调
  onError?: (error: Error) => void // 上传失败回调
  onChunkComplete?: (chunkIndex: number, uploaded: number, total: number) => void // 分片完成回调
  onMd5Progress?: (progress: number) => void // MD5计算进度回调
}

export interface UploadResult {
  success: boolean
  imageId?: number
  type: 'instant' | 'normal' // instant: 秒传, normal: 正常上传
}

class ChunkUploader {
  private file: File
  private options: Required<Omit<UploadOptions, 'pathologyId' | 'patientId'>> & Pick<UploadOptions, 'pathologyId' | 'patientId'>
  private totalChunks: number
  private uploadedChunks: Set<number>
  private fileMd5: string | null = null
  private uploadId: string | null = null
  private isPaused: boolean = false
  private isCancelled: boolean = false

  constructor(file: File, options: UploadOptions) {
    this.file = file
    this.options = {
      chunkSize: options.chunkSize || 5 * 1024 * 1024,
      concurrent: options.concurrent || 3,
      batchId: options.batchId,
      pathologyId: options.pathologyId,
      patientId: options.patientId,
      onProgress: options.onProgress || (() => {}),
      onSuccess: options.onSuccess || (() => {}),
      onError: options.onError || (() => {}),
      onChunkComplete: options.onChunkComplete || (() => {}),
      onMd5Progress: options.onMd5Progress || (() => {})
    }
    
    this.totalChunks = Math.ceil(file.size / this.options.chunkSize)
    this.uploadedChunks = new Set()
  }

  /**
   * 开始上传
   */
  async start(): Promise<UploadResult> {
    try {
      console.log('[ChunkUploader] 开始上传:', this.file.name)
      
      // 1. 计算文件MD5
      this.fileMd5 = await this.calculateMD5()
      console.log('[ChunkUploader] 文件MD5:', this.fileMd5)
      
      // 2. 检查本地是否有保存的进度
      const hasLocalProgress = this.loadProgress()
      if (hasLocalProgress) {
        console.log('[ChunkUploader] 恢复上传进度:', this.uploadedChunks.size, '/', this.totalChunks)
      }
      
      // 3. 初始化上传（获取已上传分片）
      const initRes = await this.initUpload()
      
      // 秒传检测
      if (initRes.exists) {
        console.log('[ChunkUploader] 文件已存在，秒传成功')
        // 如果后端返回了imageId，直接使用
        if (initRes.imageId) {
          this.onSuccess(initRes.imageId)
          this.cleanup()
          return { success: true, imageId: initRes.imageId, type: 'instant' }
        }
      }
      
      this.uploadId = initRes.uploadId
      
      // 4. 合并后端返回的已上传分片和本地保存的进度
      if (initRes.uploadedChunks && initRes.uploadedChunks.length > 0) {
        initRes.uploadedChunks.forEach(index => this.uploadedChunks.add(index))
        console.log(`[ChunkUploader] 后端记录：已完成 ${this.uploadedChunks.size}/${this.totalChunks} 分片`)
      }
      
      // 5. 上传剩余分片
      if (this.uploadedChunks.size < this.totalChunks) {
        await this.uploadRemainingChunks()
      } else {
        console.log('[ChunkUploader] 所有分片已上传，直接合并')
      }
      
      // 6. 合并分片
      const imageId = await this.mergeChunks()
      
      console.log('[ChunkUploader] 上传完成，图像ID:', imageId)
      this.onSuccess(imageId)
      this.cleanup()
      
      return { success: true, imageId, type: 'normal' }
      
    } catch (error) {
      console.error('[ChunkUploader] 上传失败:', error)
      this.onError(error as Error)
      throw error
    }
  }

  /**
   * 暂停上传
   */
  pause() {
    this.isPaused = true
    console.log('[ChunkUploader] 上传已暂停')
  }

  /**
   * 恢复上传
   */
  resume() {
    this.isPaused = false
    console.log('[ChunkUploader] 上传已恢复')
  }

  /**
   * 取消上传
   */
  async cancel() {
    this.isCancelled = true
    this.isPaused = false
    
    if (this.fileMd5) {
      try {
        await cancelUpload(this.fileMd5)
        console.log('[ChunkUploader] 上传已取消')
      } catch (error) {
        console.error('[ChunkUploader] 取消上传失败:', error)
      }
    }
    
    this.cleanup()
  }

  /**
   * 计算文件MD5（优化版：只读取首尾各4MB + 文件大小）
   */
  private calculateMD5(): Promise<string> {
    return new Promise((resolve, reject) => {
      const blobSlice = File.prototype.slice || (File as any).mozSlice || (File as any).webkitSlice
      const spark = new SparkMD5.ArrayBuffer()
      
      console.log(`[MD5] 开始快速计算，文件大小: ${this.file.size} bytes`)
      const startTime = Date.now()

      // 策略：读取文件头部4MB + 尾部4MB + 文件大小作为唯一标识
      const headSize = Math.min(4 * 1024 * 1024, this.file.size) // 头部4MB
      const tailSize = this.file.size > headSize ? Math.min(4 * 1024 * 1024, this.file.size - headSize) : 0 // 尾部4MB
      
      let partsRead = 0
      const totalParts = tailSize > 0 ? 2 : 1 // 头部 + (可选)尾部
      
      const fileReader = new FileReader()
      
      fileReader.onload = (e) => {
        spark.append(e.target!.result as ArrayBuffer)
        partsRead++
        
        const progress = Math.round((partsRead / totalParts) * 100)
        this.options.onMd5Progress(progress)
        console.log(`[MD5] 进度: ${progress}% (${partsRead}/${totalParts})`)
        
        if (partsRead === 1 && tailSize > 0) {
          // 读取尾部
          const tailStart = this.file.size - tailSize
          const tailBlob = blobSlice.call(this.file, tailStart, this.file.size)
          fileReader.readAsArrayBuffer(tailBlob)
        } else {
          // 已读取完成，添加文件大小信息到MD5计算
          // 将文件大小转换为Uint8Array
          const sizeBuffer = new Uint8Array(8)
          const view = new DataView(sizeBuffer.buffer)
          view.setBigUint64(0, BigInt(this.file.size), false) // 大端序
          spark.append(sizeBuffer.buffer)
          
          const md5 = spark.end()
          const elapsed = ((Date.now() - startTime) / 1000).toFixed(3)
          console.log(`[MD5] 快速计算完成: ${md5}, 耗时: ${elapsed}s, 采样: 头部${headSize/1024/1024}MB + 尾部${tailSize/1024/1024}MB + 大小信息`)
          resolve(md5)
        }
      }

      fileReader.onerror = () => {
        reject(new Error('MD5计算失败'))
      }

      // 开始读取头部
      const headBlob = blobSlice.call(this.file, 0, headSize)
      fileReader.readAsArrayBuffer(headBlob)
    })
  }

  /**
   * 初始化上传
   */
  private async initUpload(): Promise<ChunkUploadVO> {
    const initDTO: ChunkUploadInitDTO = {
      fileMd5: this.fileMd5!,
      filename: this.file.name,
      fileSize: this.file.size,
      chunkSize: this.options.chunkSize,
      totalChunks: this.totalChunks,
      batchId: this.options.batchId,
      pathologyId: this.options.pathologyId,
      patientId: this.options.patientId
    }

    return await initChunkUpload(initDTO)
  }

  /**
   * 上传剩余分片（多线程并发）
   */
  private async uploadRemainingChunks() {
    console.log(`[ChunkUploader] 开始并发上传，总分片: ${this.totalChunks}, 已上传: ${this.uploadedChunks.size}`)
    
    // 获取待上传的分片列表
    const pendingChunks: number[] = []
    for (let i = 0; i < this.totalChunks; i++) {
      if (!this.uploadedChunks.has(i)) {
        pendingChunks.push(i)
      }
    }
    
    if (pendingChunks.length === 0) {
      console.log('[ChunkUploader] 所有分片已上传，跳过')
      return
    }
    
    console.log(`[ChunkUploader] 待上传分片数: ${pendingChunks.length}, 并发数: ${this.options.concurrent}`)
    
    // 并发控制：使用信号量模式
    const concurrency = this.options.concurrent || 3
    const activeUploads = new Map<number, Promise<void>>()
    
    let completedCount = this.uploadedChunks.size
    const totalCount = this.totalChunks
    const failedChunks: number[] = []
    
    // 上传单个分片的函数
    const uploadSingleChunk = async (chunkIndex: number): Promise<void> => {
      try {
        // 上传分片（带重试）
        const success = await this.uploadChunkWithRetry(chunkIndex, 3)
        
        if (!success) {
          throw new Error(`分片 ${chunkIndex} 上传失败（重试3次后）`)
        }
        
        // 标记为已上传
        this.uploadedChunks.add(chunkIndex)
        completedCount++
        
        // 保存进度
        this.saveProgress()
        
        // 更新进度
        const progress = Math.round((completedCount / totalCount) * 100)
        this.options.onProgress(progress)
        this.options.onChunkComplete(chunkIndex, completedCount, totalCount)
        
        console.log(`[ChunkUploader] ✓ 分片 ${chunkIndex} 上传成功 (${completedCount}/${totalCount})`)
        
      } catch (error) {
        console.error(`[ChunkUploader] ✗ 分片 ${chunkIndex} 上传失败:`, error)
        failedChunks.push(chunkIndex)
        throw error // 重新抛出，让Promise.all捕获
      }
    }
    
    // 分批并发上传
    for (let i = 0; i < pendingChunks.length; i += concurrency) {
      // 检查是否取消
      if (this.isCancelled) {
        throw new Error('上传已取消')
      }
      
      // 等待恢复
      while (this.isPaused && !this.isCancelled) {
        await this.sleep(100)
      }
      
      // 获取当前批次的分片
      const batch = pendingChunks.slice(i, i + concurrency)
      console.log(`[ChunkUploader] 上传批次 ${Math.floor(i / concurrency) + 1}: 分片 ${batch.join(', ')}`)
      
      // 并发上传当前批次
      const promises = batch.map(index => uploadSingleChunk(index))
      
      try {
        await Promise.all(promises)
      } catch (error) {
        console.error(`[ChunkUploader] 批次上传失败:`, error)
        throw error
      }
    }
    
    console.log(`[ChunkUploader] 所有分片上传完成: ${completedCount}/${totalCount}`)
    
    if (failedChunks.length > 0) {
      console.warn(`[ChunkUploader] 警告: 以下分片上传失败: ${failedChunks.join(', ')}`)
    }
  }

  /**
   * 上传单个分片（带重试）
   */
  private async uploadChunkWithRetry(index: number, maxRetries: number): Promise<boolean> {
    for (let retry = 0; retry < maxRetries; retry++) {
      try {
        const success = await this.uploadChunk(index)
        if (success) {
          return true
        }
      } catch (error) {
        console.warn(`[ChunkUploader] 分片 ${index} 第${retry + 1}次重试失败`, error)
        
        if (retry < maxRetries - 1) {
          // 指数退避
          await this.sleep(1000 * Math.pow(2, retry))
        }
      }
    }
    
    return false
  }

  /**
   * 上传单个分片
   */
  private async uploadChunk(index: number): Promise<boolean> {
    const start = index * this.options.chunkSize
    const end = Math.min(start + this.options.chunkSize, this.file.size)
    const chunk = this.file.slice(start, end)
    
    return await uploadChunk({
      fileMd5: this.fileMd5!,
      chunkIndex: index,
      chunk
    })
  }

  /**
   * 合并分片
   */
  private async mergeChunks(): Promise<number> {
    console.log('[ChunkUploader] 合并分片...')
    
    return await mergeChunks(
      this.fileMd5!,
      this.options.batchId,
      this.file.name,
      this.options.pathologyId,
      this.options.patientId
    )
  }

  /**
   * 保存进度到localStorage
   */
  private saveProgress() {
    const data = {
      uploadId: this.uploadId,
      uploadedChunks: Array.from(this.uploadedChunks),
      timestamp: Date.now()
    }
    
    localStorage.setItem(`upload_${this.fileMd5}`, JSON.stringify(data))
  }

  /**
   * 从localStorage加载进度
   */
  private loadProgress(): boolean {
    if (!this.fileMd5) return false
    
    const saved = localStorage.getItem(`upload_${this.fileMd5}`)
    
    if (saved) {
      try {
        const data = JSON.parse(saved)
        
        // 检查是否过期（24小时）
        if (Date.now() - data.timestamp < 24 * 60 * 60 * 1000) {
          this.uploadId = data.uploadId
          this.uploadedChunks = new Set(data.uploadedChunks)
          return true
        } else {
          // 过期则删除
          localStorage.removeItem(`upload_${this.fileMd5}`)
        }
      } catch (error) {
        console.error('[ChunkUploader] 加载进度失败:', error)
        localStorage.removeItem(`upload_${this.fileMd5}`)
      }
    }
    
    return false
  }

  /**
   * 清理资源
   */
  private cleanup() {
    if (this.fileMd5) {
      localStorage.removeItem(`upload_${this.fileMd5}`)
    }
    this.uploadedChunks.clear()
  }

  /**
   * 睡眠函数
   */
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  // Getter方法
  private get onSuccess() {
    return this.options.onSuccess
  }

  private get onError() {
    return this.options.onError
  }
}

export default ChunkUploader

<template>
  <!--  <div class="multipart-uploader">-->
  <div>
    <el-upload
        action="#"
        :show-file-list="false"
        :http-request="handleUpload"
    >
      <el-button type="primary">选择文件</el-button>
    </el-upload>

    <div v-if="file" class="upload-progress">
      <p>文件名：{{ file.name }}</p>
      <el-progress :percentage="uploadProgress"></el-progress>
      <p v-if="isUploading">上传中... {{ uploadProgress }}%</p>
      <p v-else-if="isCompleted">上传完成！</p>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {addSlide} from "@api/image/image-api"
import {
  uploadChunk as apiUploadChunk,
  completeMultipartUpload as apiCompleteMultipartUpload
} from "@api/attachment/attachment-api"
import {ElMessage, ElMessageBox} from 'element-plus'
import SparkMD5 from 'spark-md5'
import {R, ChunkUploadVO} from "@types/common";

// 状态管理
const file = ref<File | null>(null)
const isUploading = ref(false)
const isCompleted = ref(false)
const uploadProgress = ref(0)


// 计算分片 MD5
async function calculateChunkMD5(chunk: Blob): Promise<string> {
  const chunkSize = 128
  const fileSize = chunk.size

  // 创建一个 256 字节的缓冲区
  // const result = new Uint8Array(256)
  const result = new Int8Array(256)

  // 读取前 128 字节
  const headChunk = chunk.slice(0, chunkSize)
  const headArray = await readBlobAsUint8Array(headChunk, chunkSize)
  result.set(headArray, 0)

  // 读取后 128 字节
  const tailStart = Math.max(fileSize - chunkSize, 0)
  const tailChunk = chunk.slice(tailStart, tailStart + chunkSize)
  const tailArray = await readBlobAsUint8Array(tailChunk, chunkSize)
  result.set(tailArray, 128)
  const spark = new SparkMD5.ArrayBuffer()
  spark.append(result)
  const md5 = spark.end()
  // 计算 MD5
  return md5
}

/**
 * 将 Blob 读取为 Uint8Array
 * @param blob - 输入 Blob
 * @param size - 期望读取的大小（字节）
 * @returns Promise<Uint8Array>
 */
function readBlobAsUint8Array(blob: Blob, size: number): Promise<Uint8Array> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = (event) => {
      const arrayBuffer = event.target?.result as ArrayBuffer
      const uint8Array = new Uint8Array(arrayBuffer)
      resolve(uint8Array)
    }

    reader.onerror = () => {
      reader.abort()
      reject(new Error('读取 Blob 失败'))
    }

    reader.readAsArrayBuffer(blob)
  })
}


// 初始化上传任务
async function initiateMultipartUpload(): Promise<string | undefined> {
  try {
    const res = await addSlide()
    return res.data
  } catch (error) {
    ElMessage.error('初始化上传失败')
    throw error
  }
}

// 上传单个分片
async function uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob, totalChunks: number, chunkSize: number): Promise<boolean> {
  const chunkMd5 = await calculateChunkMD5(chunk)

  const formData = new FormData()
  formData.append('name', file.value!.name)
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', chunkIndex.toString())
  formData.append('chunkTotal', totalChunks.toString())
  formData.append('chunkSize', chunk.size.toString())
  formData.append('chunkMd5', chunkMd5)
  formData.append('chunk', new File([chunk], `${file.value!.name}-part-${chunkIndex}`))

  try {
    const res: R<boolean> = await apiUploadChunk(formData)
    if (!res.data) {
      ElMessage.error(`第 ${chunkIndex} 分片上传失败`)
      return false
    }

    return true
  } catch (error) {
    ElMessage.error(`第 ${chunkIndex} 分片上传失败`)
    return false
  }
}

// 完成分片上传
async function completeMultipartUpload(uploadId: string,name: string, chunkTotal: number, chunkSize: number, chunks: ChunkUploadVO[]) {

  const data: ChunkUploadVO = {
    uploadId: uploadId,
    name: name,
    chunks: chunks,
    chunkSize: chunkSize,
    chunkTotal: chunkTotal
  }

  try {
    const res: R = await apiCompleteMultipartUpload(data)
    if (res.code == 200) {
      ElMessage.success('上传完成')
      isCompleted.value = true
      return res.data.data
    } else {
      ElMessage.error('合并分片失败')
      return null
    }
  } catch (error) {
    ElMessage.error('合并分片失败')
    return null
  }
}

// 处理文件上传
async function handleUpload(options: any) {
  file.value = options.file
  const chunkSize = 1024 * 1024 * 5 // 5MB per chunk
  const fileSize = file.value.size
  const totalChunks = Math.ceil(fileSize / chunkSize)
  const uploadId: string | undefined = await initiateMultipartUpload()
  const completeDatas: ChunkUploadVO[] = []

  isUploading.value = true
  isCompleted.value = false
  uploadProgress.value = 0

  let uploadedChunks = 0

  const uploadNextChunk = async (index: number) => {
    const start = index * chunkSize
    const end = Math.min(start + chunkSize, fileSize)
    const chunk = file.value!.slice(start, end)
    const success = await uploadChunk(uploadId, index, chunk, totalChunks, chunkSize)
    if (success) {
      completeDatas.push({
        name: file.value!.name,
        uploadId: uploadId,
        chunkIndex: index,
        chunkTotal: totalChunks,
        chunkSize: chunk.size,
        chunkMd5: await calculateChunkMD5(chunk)
      })
      uploadedChunks++
      uploadProgress.value = Math.floor((uploadedChunks / totalChunks) * 100)
      if (uploadedChunks === totalChunks) {
        await completeMultipartUpload(uploadId,file.value!.name, totalChunks,chunkSize,completeDatas)
      } else {
        if (index + 1 < totalChunks) {
          uploadNextChunk(index + 1)
        }
      }
    } else {
      ElMessageBox.confirm('上传失败，是否重试？', '提示', {
        confirmButtonText: '重试',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        uploadNextChunk(index)
      }).catch(() => {
        isUploading.value = false
      })
    }
  }

  uploadNextChunk(0)
}

// 验证文件
function handleBeforeUpload(f: File) {
  const isValidType = ['image/jpeg', 'image/png', 'svs'].includes(f.type)
  if (!isValidType) {
    ElMessage.error('只能上传图片或PDF文件')
    return false
  }
  return true
}
</script>

<style scoped>
.multipart-uploader {
  padding: 20px;
}

.upload-progress {
  margin-top: 20px;
}
</style>

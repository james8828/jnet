<template>
  <div>
    <el-upload
        :action="uploadUrl"
        :on-change="handleFileChange"
        :auto-upload="false"
        :show-file-list="false"
        accept=".jpg,.png,.jpeg,.gif,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z,.mp4,.avi,.mov,.wmv,.flv,.mkv,.webm,.svs"
    >
      <el-button type="primary">选择文件</el-button>
    </el-upload>
    <el-progress v-if="isUploading" :percentage="progress" status="active"></el-progress>
    <el-alert v-if="uploadSuccess" title="文件上传成功" type="success" :closable="false"></el-alert>
    <el-alert v-if="uploadError" title="文件上传失败" type="error" :closable="false"></el-alert>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
//import axios from 'axios';
import SparkMD5 from 'spark-md5'
import serviceAxios from '@utils/serviceAxios'

interface FileChunk {
  chunk: Blob;
  chunkIndex: number;
  chunkMd5: string;
  chunkTotal: number;
  chunkSize: number;
  name: string;
  uploadId: string;
}

export default defineComponent({
  name: 'FileChunkUploader',
  setup: function () {
    const uploadUrl = process.env.IMAGE_SERVICE_URL + '/attachment/uploadChunk';
    const file = ref<File | null>(null);
    const isUploading = ref(false);
    const progress = ref(0);
    const uploadSuccess = ref(false);
    const uploadError = ref(false);

    const handleFileChange = (fileEvent: any) => {
      const selectedFile = fileEvent.raw;
      if (selectedFile) {
        file.value = selectedFile;
        startUpload();
      }
    };

    const startUpload = async () => {
      if (!file.value) return;
      uploadError.value = false;
      isUploading.value = true;
      progress.value = 0;

      const fileReader = new FileReader();
      fileReader.readAsArrayBuffer(file.value!);

      fileReader.onload = async () => {
        const fileBlob = new Blob([fileReader.result as ArrayBuffer]);
        const chunkSize = 1024 * 1024 * 10; // 10MB
        const chunks: FileChunk[] = [];
        for (let i = 0; i < fileBlob.size; i += chunkSize) {
          const chunk = fileBlob.slice(i, i + chunkSize);
          const chunkMd5 = await calculateChunkMd5(chunk);
          //const chunkMd5 = await getFileMd5(chunk);
          console.log(chunkMd5)
          chunks.push({chunk, chunkIndex: i/chunkSize, chunkMd5, chunkTotal: 0, chunkSize: chunkSize,name: file.value.name,uploadId:'123'});
        }

        await uploadChunks(chunks);
      };
    };

    const calculateChunkMd5 = async (chunk: Blob): Promise<string> => {
      return new Promise(async (resolve, reject) => {
        const spark = new SparkMD5.ArrayBuffer()
        const fileReader = new FileReader()
        fileReader.onload = function (e) {
          spark.append(e.target.result)
          const _md5 = spark.end()
          resolve(_md5)
        }
        const temp = chunk.slice(0, 4096);
        fileReader.readAsArrayBuffer(temp);
      })
    };

    const uploadChunks = async (chunks: FileChunk[]): Promise<void> => {
      for (let i = 0; i < chunks.length; i++) {
        const chunk = chunks[i];
        const formData = new FormData();
        formData.append('chunk', chunk.chunk);
        formData.append('chunkIndex', chunk.chunkIndex.toString());
        formData.append('chunkMd5', chunk.chunkMd5);
        formData.append('chunkTotal', chunks.length.toString());
        formData.append('chunkSize', chunk.chunkSize.toString());
        formData.append('name', chunk.name);
        formData.append('uploadId', chunk.uploadId);

        try {
          const response = await serviceAxios.post(uploadUrl, formData, {
            headers: {
              'Content-Type': 'multipart/form-data',
            }
          });
          if (response.code === 200) {
            progress.value = (i+1)*100/chunks.length;
          } else {
            throw new Error('Failed to upload chunk.');
          }
        } catch (error) {
          uploadError.value = true;
          isUploading.value = false;
          return;
        }
      }
    };

    return {
      uploadUrl,
      file,
      isUploading,
      progress,
      uploadSuccess,
      uploadError,
      handleFileChange,
    };
  },
});
</script>

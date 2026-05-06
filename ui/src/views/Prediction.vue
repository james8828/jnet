<template>
  <div class="prediction-container">
    <el-row :gutter="20">
      <!-- 左侧配置 -->
      <el-col :span="6">
        <el-card class="config-panel">
          <template #header>
            <span>预测配置</span>
          </template>
          
          <el-form :model="config" label-width="100px">
            <el-form-item label="选择模型">
              <el-select v-model="config.model" placeholder="选择模型" style="width: 100%">
                <el-option
                  v-for="model in models"
                  :key="model.id"
                  :label="model.name"
                  :value="model.id"
                >
                  <div>
                    <div>{{ model.name }}</div>
                    <div style="font-size: 12px; color: #8492a6">mAP: {{ model.mAP }}</div>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="置信度阈值">
              <el-slider v-model="config.confThreshold" :min="0.1" :max="0.9" :step="0.05" show-input />
            </el-form-item>
            
            <el-form-item label="IOU阈值">
              <el-slider v-model="config.iouThreshold" :min="0.1" :max="0.9" :step="0.05" show-input />
            </el-form-item>
            
            <el-form-item label="图像尺寸">
              <el-input-number v-model="config.imgSize" :min="320" :max="1280" :step="32" style="width: 100%" />
            </el-form-item>
            
            <el-divider />
            
            <el-form-item>
              <el-button type="primary" @click="startPrediction" :loading="isPredicting" style="width: 100%">
                <el-icon><Aim /></el-icon>
                {{ isPredicting ? '预测中...' : '开始预测' }}
              </el-button>
            </el-form-item>
          </el-form>
          
          <el-divider content-position="left">批量预测</el-divider>
          
          <el-upload
            drag
            action="#"
            multiple
            :auto-upload="false"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 SVS, JPG, PNG 格式
              </div>
            </template>
          </el-upload>
        </el-card>
      </el-col>

      <!-- 中间预览 -->
      <el-col :span="12">
        <el-card class="preview-panel">
          <template #header>
            <div class="panel-header">
              <span>预测结果预览</span>
              <el-radio-group v-model="viewMode" size="small">
                <el-radio-button label="original">原图</el-radio-button>
                <el-radio-button label="prediction">预测</el-radio-button>
                <el-radio-button label="overlay">叠加</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          
          <div class="preview-container">
            <div v-if="!currentImage" class="empty-state">
              <el-icon :size="64" color="#dcdfe6"><Picture /></el-icon>
              <p>请上传或选择图像进行预测</p>
            </div>
            <div v-else class="image-viewer">
              <img :src="currentImage.url" alt="Preview" />
              <canvas ref="predictionCanvas" class="prediction-overlay"></canvas>
            </div>
          </div>
          
          <div class="viewer-controls">
            <el-button size="small" @click="zoomIn">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
            <el-button size="small" @click="zoomOut">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-slider
              v-model="opacity"
              :min="0"
              :max="100"
              style="width: 200px"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧结果 -->
      <el-col :span="6">
        <el-card class="result-panel">
          <template #header>
            <span>检测结果</span>
          </template>
          
          <div class="detection-stats">
            <div class="stat-item">
              <div class="stat-label">检测目标数</div>
              <div class="stat-value">{{ detections.length }}</div>
            </div>
          </div>
          
          <el-divider />
          
          <div class="detection-list">
            <div
              v-for="(det, index) in detections"
              :key="index"
              class="detection-item"
              @mouseenter="highlightDetection(index)"
            >
              <div class="det-header">
                <el-tag :color="getClassColor(det.class)" effect="dark" size="small">
                  {{ det.className }}
                </el-tag>
                <span class="det-conf">{{ (det.confidence * 100).toFixed(1) }}%</span>
              </div>
              <div class="det-info">
                <span>位置: ({{ det.x }}, {{ det.y }})</span>
                <span>尺寸: {{ det.width }}×{{ det.height }}</span>
              </div>
            </div>
          </div>
          
          <el-divider />
          
          <div class="export-actions">
            <el-button type="success" @click="exportGeoJSON" style="width: 100%">
              <el-icon><Download /></el-icon>
              导出 GeoJSON
            </el-button>
            <el-button @click="exportImage" style="width: 100%; margin-top: 8px">
              <el-icon><Picture /></el-icon>
              导出标注图
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const isPredicting = ref(false)
const currentImage = ref<any>(null)
const viewMode = ref('overlay')
const opacity = ref(70)
const predictionCanvas = ref<HTMLCanvasElement>()

const config = reactive({
  model: '',
  confThreshold: 0.5,
  iouThreshold: 0.45,
  imgSize: 640
})

const models = ref([
  { id: 1, name: 'YOLOv7 - 病理切片 v1', mAP: 0.923 },
  { id: 2, name: 'YOLOv7-tiny - WSI v2', mAP: 0.856 },
  { id: 3, name: 'YOLOv8 - 细胞检测', mAP: 0.945 }
])

const detections = ref([
  { class: 0, className: '正常组织', confidence: 0.95, x: 120, y: 80, width: 150, height: 120 },
  { class: 1, className: '癌变区域', confidence: 0.88, x: 350, y: 200, width: 180, height: 160 },
  { class: 2, className: '炎症区域', confidence: 0.76, x: 600, y: 150, width: 120, height: 100 }
])

const getClassColor = (cls: number) => {
  const colors = ['#67C23A', '#F56C6C', '#E6A23C', '#909399']
  return colors[cls] || '#409EFF'
}

const handleFileChange = (file: any) => {
  currentImage.value = {
    url: URL.createObjectURL(file.raw),
    name: file.name
  }
  ElMessage.success(`已加载: ${file.name}`)
}

const startPrediction = () => {
  if (!currentImage.value) {
    ElMessage.warning('请先上传图像')
    return
  }
  
  isPredicting.value = true
  
  setTimeout(() => {
    isPredicting.value = false
    ElMessage.success('预测完成')
  }, 2000)
}

const zoomIn = () => {}
const zoomOut = () => {}
const highlightDetection = (index: number) => {}
const exportGeoJSON = () => {
  ElMessage.success('GeoJSON 导出成功')
}
const exportImage = () => {
  ElMessage.success('标注图导出成功')
}
</script>

<style scoped lang="scss">
.prediction-container {
  .config-panel, .preview-panel, .result-panel {
    border-radius: 8px;
  }
  
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
  
  .preview-container {
    height: 500px;
    background: #f5f7fa;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    
    .empty-state {
      text-align: center;
      color: #909399;
      
      p {
        margin-top: 16px;
      }
    }
    
    .image-viewer {
      position: relative;
      max-width: 100%;
      max-height: 100%;
      
      img {
        max-width: 100%;
        max-height: 500px;
        display: block;
      }
      
      .prediction-overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
      }
    }
  }
  
  .viewer-controls {
    margin-top: 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    justify-content: center;
  }
  
  .result-panel {
    .detection-stats {
      .stat-item {
        text-align: center;
        padding: 16px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 8px;
        color: white;
        
        .stat-label {
          font-size: 14px;
          opacity: 0.9;
          margin-bottom: 8px;
        }
        
        .stat-value {
          font-size: 36px;
          font-weight: 600;
        }
      }
    }
    
    .detection-list {
      max-height: 300px;
      overflow-y: auto;
      
      .detection-item {
        padding: 12px;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        margin-bottom: 8px;
        cursor: pointer;
        transition: all 0.3s;
        
        &:hover {
          border-color: #409EFF;
          background: #ecf5ff;
        }
        
        .det-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          
          .det-conf {
            font-weight: 600;
            color: #409EFF;
          }
        }
        
        .det-info {
          display: flex;
          flex-direction: column;
          gap: 4px;
          font-size: 12px;
          color: #909399;
        }
      }
    }
    
    .export-actions {
      margin-top: 16px;
    }
  }
}
</style>

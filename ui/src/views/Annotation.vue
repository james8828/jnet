<template>
  <div class="annotation-container">
    <el-row :gutter="20" style="height: 100%">
      <!-- 左侧工具栏 -->
      <el-col :span="3">
        <el-card class="tool-panel">
          <template #header>
            <span>标注工具</span>
          </template>
          
          <div class="tool-buttons">
            <el-button
              v-for="tool in tools"
              :key="tool.name"
              :type="currentTool === tool.name ? 'primary' : ''"
              circle
              @click="selectTool(tool.name)"
            >
              <el-icon><component :is="tool.icon" /></el-icon>
            </el-button>
          </div>
          
          <el-divider />
          
          <div class="label-list">
            <div class="section-title">标签列表</div>
            <el-tag
              v-for="label in labels"
              :key="label.id"
              :color="label.color"
              class="label-tag"
              effect="dark"
            >
              {{ label.name }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 中间画布区域 -->
      <el-col :span="15">
        <el-card class="canvas-panel">
          <template #header>
            <div class="canvas-header">
              <span>{{ currentImage?.originalFilename || '未加载图像' }}</span>
              <div class="canvas-actions">
                <el-button size="small" @click="zoomIn">
                  <el-icon><ZoomIn /></el-icon>
                </el-button>
                <el-button size="small" @click="zoomOut">
                  <el-icon><ZoomOut /></el-icon>
                </el-button>
                <el-button size="small" @click="fitView">
                  <el-icon><FullScreen /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
          
          <div ref="canvasContainer" class="canvas-container">
            <div v-if="!currentImage" class="empty-state">
              <el-icon :size="64" color="#dcdfe6"><Picture /></el-icon>
              <p>请从右侧选择图像开始标注</p>
            </div>
            <canvas v-else ref="annotationCanvas"></canvas>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧图像列表 -->
      <el-col :span="6">
        <el-card class="image-panel">
          <template #header>
            <div class="panel-header">
              <span>图像列表</span>
              <el-button type="primary" size="small" text>批量导入</el-button>
            </div>
          </template>
          
          <el-input
            v-model="searchText"
            placeholder="搜索图像..."
            prefix-icon="Search"
            clearable
            class="search-input"
          />
          
          <div class="image-list">
            <div
              v-for="img in filteredImages"
              :key="img.imageId"
              class="image-item"
              :class="{ active: currentImage?.imageId === img.imageId }"
              @click="loadImage(img)"
            >
              <div class="image-thumb">
                <img :src="getThumbnailUrl(img.imageId, 200)" :alt="img.originalFilename" @error="handleImageError" />
                <div v-if="img.annotationProgress === 100" class="annotated-badge">已标注</div>
              </div>
              <div class="image-info">
                <div class="image-name">{{ img.originalFilename }}</div>
                <div class="image-meta">
                  <span>{{ img.format }}</span>
                  <span>{{ formatFileSize(img.fileSize) }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <el-button type="success" @click="saveAnnotation">
        <el-icon><Check /></el-icon>
        保存标注
      </el-button>
      <el-button @click="exportGeoJSON">
        <el-icon><Download /></el-icon>
        导出 GeoJSON
      </el-button>
      <el-button @click="clearAnnotations">
        <el-icon><Delete /></el-icon>
        清除标注
      </el-button>
      <el-divider direction="vertical" />
      <span class="status-text">
        当前标注数: {{ annotationCount }} | 缩放: {{ zoomLevel }}%
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getImageById, getImageMetadata, getThumbnailUrl } from '@/api/images'
import type { Image, ImageMetadataVO } from '@/types/image'

const route = useRoute()
const currentTool = ref('rectangle')
const currentImage = ref<Image | null>(null)
const imageMetadata = ref<ImageMetadataVO | null>(null)
const searchText = ref('')
const zoomLevel = ref(100)
const annotationCount = ref(0)
const canvasContainer = ref<HTMLElement>()
const annotationCanvas = ref<HTMLCanvasElement>()

// 标注工具
const tools = [
  { name: 'rectangle', icon: 'Crop', label: '矩形' },
  { name: 'polygon', icon: 'Connection', label: '多边形' },
  { name: 'point', icon: 'Pointer', label: '点' },
  { name: 'eraser', icon: 'Delete', label: '橡皮擦' }
]

// 标签列表（应从后端获取）
const labels = [
  { id: 1, name: '正常组织', color: '#67C23A' },
  { id: 2, name: '癌变区域', color: '#F56C6C' },
  { id: 3, name: '炎症区域', color: '#E6A23C' },
  { id: 4, name: '坏死区域', color: '#909399' }
]

// 图像列表（应从当前项目/批次中获取）
const images = ref<Image[]>([])

const filteredImages = computed(() => {
  if (!searchText.value) return images.value
  return images.value.filter(img => 
    img.originalFilename.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

/**
 * 选择标注工具
 */
const selectTool = (tool: string) => {
  currentTool.value = tool
  ElMessage.info(`已选择工具: ${tools.find(t => t.name === tool)?.label}`)
}

/**
 * 加载图像
 */
const loadImage = async (img: Image) => {
  try {
    currentImage.value = img
    ElMessage.success(`已加载图像: ${img.originalFilename}`)
    
    // 获取图像元数据
    imageMetadata.value = await getImageMetadata(img.imageId)
    console.log('图像元数据:', imageMetadata.value)
    
    // TODO: 使用 OpenSeaDragon 初始化 WSI 图像查看器
    // initOpenSeaDragon(img.imageId)
  } catch (error) {
    console.error('加载图像失败:', error)
    ElMessage.error('加载图像失败')
  }
}

/**
 * 放大
 */
const zoomIn = () => {
  zoomLevel.value = Math.min(zoomLevel.value + 10, 500)
  // TODO: 调用 OpenSeaDragon zoomIn
}

/**
 * 缩小
 */
const zoomOut = () => {
  zoomLevel.value = Math.max(zoomLevel.value - 10, 10)
  // TODO: 调用 OpenSeaDragon zoomOut
}

/**
 * 适应视图
 */
const fitView = () => {
  zoomLevel.value = 100
  // TODO: 调用 OpenSeaDragon viewport.fitBounds
}

/**
 * 保存标注
 */
const saveAnnotation = async () => {
  if (!currentImage.value) {
    ElMessage.warning('请先加载图像')
    return
  }
  
  try {
    // TODO: 收集标注数据并发送到后端
    // const annotations = collectAnnotations()
    // await saveAnnotations(currentImage.value.imageId, annotations)
    
    ElMessage.success('标注保存成功')
  } catch (error) {
    console.error('保存标注失败:', error)
    ElMessage.error('保存标注失败')
  }
}

/**
 * 导出 GeoJSON
 */
const exportGeoJSON = () => {
  if (!currentImage.value) {
    ElMessage.warning('请先加载图像')
    return
  }
  
  // TODO: 将标注数据转换为 GeoJSON 格式并下载
  const geojson = {
    type: 'FeatureCollection',
    features: []
  }
  
  const blob = new Blob([JSON.stringify(geojson, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `annotations_${currentImage.value.imageId}.geojson`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('GeoJSON 导出成功')
}

/**
 * 清除标注
 */
const clearAnnotations = () => {
  annotationCount.value = 0
  // TODO: 清除画布上的所有标注
  ElMessage.info('已清除所有标注')
}

/**
 * 初始化 OpenSeaDragon 查看器
 */
const initOpenSeaDragon = (imageId: number) => {
  // TODO: 集成 OpenSeaDragon
  // 参考: https://openseadragon.github.io/
  
  /*
  if (window.OpenSeadragon && canvasContainer.value) {
    const viewer = window.OpenSeadragon({
      element: canvasContainer.value,
      prefixUrl: 'https://openseadragon.github.io/openseadragon/images/',
      tileSources: {
        Image: {
          xmlns: 'http://schemas.microsoft.com/deepzoom/2008',
          Url: getTileUrl(imageId), // 需要实现获取瓦片URL的函数
          Format: 'jpg',
          Overlap: '1',
          TileSize: '256',
          Size: {
            Height: imageMetadata.value?.height || 0,
            Width: imageMetadata.value?.width || 0
          }
        }
      }
    })
  }
  */
}

/**
 * 格式化文件大小
 */
const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) return `${gb.toFixed(1)}GB`
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(1)}MB`
}

/**
 * 图片加载错误处理
 */
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmN2ZhIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzkwOTM5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuaXoOazleWKoOi9veWbvueJhzwvdGV4dD48L3N2Zz4='
}

// 组件挂载时检查路由参数
onMounted(() => {
  const imageId = route.query.imageId
  if (imageId) {
    // TODO: 根据 imageId 加载图像
    console.log('从路由参数加载图像:', imageId)
  }
})
</script>

<style scoped lang="scss">
.annotation-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  
  .tool-panel, .canvas-panel, .image-panel {
    height: 100%;
    border-radius: 8px;
    
    :deep(.el-card__body) {
      height: calc(100% - 60px);
      overflow-y: auto;
      padding: 16px;
    }
  }
  
  .tool-panel {
    .tool-buttons {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .el-button {
        width: 48px;
        height: 48px;
      }
    }
    
    .label-list {
      .section-title {
        font-weight: 600;
        margin-bottom: 12px;
        color: #303133;
      }
      
      .label-tag {
        display: block;
        margin-bottom: 8px;
        cursor: pointer;
      }
    }
  }
  
  .canvas-panel {
    .canvas-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
      
      .canvas-actions {
        display: flex;
        gap: 8px;
      }
    }
    
    .canvas-container {
      height: 100%;
      background: #f5f7fa;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      overflow: hidden;
      
      .empty-state {
        text-align: center;
        color: #909399;
        
        p {
          margin-top: 16px;
          font-size: 14px;
        }
      }
      
      canvas {
        max-width: 100%;
        max-height: 100%;
      }
    }
  }
  
  .image-panel {
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
    }
    
    .search-input {
      margin-bottom: 16px;
    }
    
    .image-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .image-item {
        padding: 12px;
        border: 2px solid #e4e7ed;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.3s;
        
        &:hover {
          border-color: #409EFF;
          box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
        }
        
        &.active {
          border-color: #409EFF;
          background: #ecf5ff;
        }
        
        .image-thumb {
          position: relative;
          width: 100%;
          height: 120px;
          background: #f5f7fa;
          border-radius: 4px;
          overflow: hidden;
          margin-bottom: 8px;
          
          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }
          
          .annotated-badge {
            position: absolute;
            top: 8px;
            right: 8px;
            background: #67C23A;
            color: white;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 12px;
          }
        }
        
        .image-info {
          .image-name {
            font-weight: 500;
            color: #303133;
            margin-bottom: 4px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          
          .image-meta {
            display: flex;
            gap: 12px;
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
  }
  
  .bottom-bar {
    margin-top: 16px;
    padding: 12px 20px;
    background: white;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 12px;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
    
    .status-text {
      margin-left: auto;
      color: #606266;
      font-size: 14px;
    }
  }
}
</style>

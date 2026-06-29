<template>
  <Transition name="slide">
    <div v-if="visible && annotation" class="property-panel" :style="panelStyle">
      <!-- 头部 -->
      <div class="panel-header">
        <span class="panel-title">标注属性</span>
        <el-button 
          class="close-btn" 
          size="small" 
          icon="Close" 
          @click="handleClose"
        />
      </div>
      
      <!-- 内容区域 -->
      <div class="panel-content">
        <div class="property-grid">
          <!-- 标注ID -->
          <div class="property-item">
            <span class="property-label">标注ID</span>
            <span class="property-value">{{ annotation.annotationId }}</span>
          </div>
          
          <!-- 标签名称 -->
          <div class="property-item">
            <span class="property-label">标签</span>
            <span class="property-value tag-value">{{ annotation.tagName || '未设置' }}</span>
          </div>
          
          <!-- 标签ID -->
          <div class="property-item">
            <span class="property-label">标签ID</span>
            <span class="property-value">{{ annotation.tagId }}</span>
          </div>
          
          <!-- 几何类型 -->
          <div class="property-item">
            <span class="property-label">几何类型</span>
            <span class="property-value">{{ formatGeomType(annotation.geomType) }}</span>
          </div>
          
          <!-- 面积 -->
          <div class="property-item">
            <span class="property-label">面积</span>
            <span class="property-value">{{ annotation.area ? annotation.area.toFixed(2) : '-' }} px²</span>
          </div>
          
          <!-- 周长 -->
          <div class="property-item">
            <span class="property-label">周长</span>
            <span class="property-value">{{ annotation.perimeter ? annotation.perimeter.toFixed(2) : '-' }} px</span>
          </div>
          
          <!-- 切片ID -->
          <div class="property-item">
            <span class="property-label">切片ID</span>
            <span class="property-value">{{ annotation.slideId }}</span>
          </div>
          
          <!-- 图像ID -->
          <div class="property-item">
            <span class="property-label">图像ID</span>
            <span class="property-value">{{ annotation.imageId }}</span>
          </div>
          
          <!-- 创建时间 -->
          <div class="property-item">
            <span class="property-label">创建时间</span>
            <span class="property-value">{{ annotation.createdAt || '-' }}</span>
          </div>
          
          <!-- 更新时间 -->
          <div class="property-item">
            <span class="property-label">更新时间</span>
            <span class="property-value">{{ annotation.updatedAt || '-' }}</span>
          </div>
          
          <!-- 创建者 -->
          <div class="property-item">
            <span class="property-label">创建者</span>
            <span class="property-value">{{ annotation.createdBy || '-' }}</span>
          </div>
          
          <!-- 描述 -->
          <div class="property-item description-item">
            <span class="property-label">描述</span>
            <span class="property-value description-value">{{ annotation.description || '-' }}</span>
          </div>
        </div>
      </div>
      
      <!-- 底部操作按钮 -->
      <div class="panel-footer">
        <el-button size="small" type="primary" @click="handleEdit">编辑</el-button>
        <el-button size="small" @click="handleClose">关闭</el-button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Annotation {
  annotationId: number | string
  tagId?: number | string
  tagName?: string
  geomType?: string
  area?: number
  perimeter?: number
  slideId?: number | string
  imageId?: number | string
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  description?: string
}

interface Props {
  visible: boolean
  annotation: Annotation | null
  position?: 'left' | 'right' | 'top' | 'bottom'
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  annotation: null,
  position: 'right'
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'edit', annotation: Annotation): void
}>()

const panelStyle = computed(() => {
  const positions: Record<string, string> = {
    left: 'left: 16px; right: auto;',
    right: 'right: 16px; left: auto;',
    top: 'top: 120px; bottom: auto;',
    bottom: 'bottom: 16px; top: auto;'
  }
  return positions[props.position] || positions['right']
})

const formatGeomType = (geomType?: string) => {
  const typeMap: Record<string, string> = {
    'Point': '点',
    'LineString': '线',
    'Polygon': '多边形',
    'MultiPolygon': '多多边形',
    'MultiPoint': '多点',
    'MultiLineString': '多线'
  }
  return typeMap[geomType || ''] || (geomType || '未知')
}

const handleClose = () => {
  emit('close')
}

const handleEdit = () => {
  if (props.annotation) {
    emit('edit', props.annotation)
  }
}
</script>

<style scoped lang="scss">
.property-panel {
  position: fixed;
  top: 120px;
  width: 320px;
  max-height: calc(100vh - 160px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
    }

    .close-btn {
      color: rgba(255, 255, 255, 0.8);
      border: none;
      padding: 4px;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        color: #fff;
      }
    }
  }

  .panel-content {
    flex: 1;
    padding: 16px;
    overflow-y: auto;

    .property-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .property-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-bottom: 12px;
      border-bottom: 1px solid #f0f0f0;

      &.description-item {
        border-bottom: none;
      }

      .property-label {
        font-size: 12px;
        color: #909399;
      }

      .property-value {
        font-size: 14px;
        color: #303133;
        font-weight: 500;

        &.tag-value {
          display: inline-block;
          padding: 2px 8px;
          background: #ecf5ff;
          color: #409eff;
          border-radius: 4px;
          font-size: 12px;
        }

        &.description-value {
          font-weight: 400;
          color: #606266;
          word-break: break-all;
        }
      }
    }
  }

  .panel-footer {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;

    button {
      flex: 1;
    }
  }
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
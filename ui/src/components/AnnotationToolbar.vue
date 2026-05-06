<template>
  <div class="annotation-toolbar">
    <!-- 工具模式选择 -->
    <div class="toolbar-section">
      <el-button
        :type="currentTool === 'select' ? 'primary' : ''"
        size="small"
        @click="setTool('select')"
      >
        选择
      </el-button>

      <el-button
        :type="currentTool === 'draw-point' ? 'primary' : ''"
        size="small"
        @click="setTool('draw-point')"
      >
        绘点
      </el-button>

      <el-button
        :type="currentTool === 'draw-line' ? 'primary' : ''"
        size="small"
        @click="setTool('draw-line')"
      >
        自由绘线
      </el-button>

      <el-button
        :type="currentTool === 'draw-polygon' ? 'primary' : ''"
        size="small"
        @click="setTool('draw-polygon')"
      >
        自由绘制
      </el-button>

      <el-button
        :type="currentTool === 'edit' ? 'primary' : ''"
        size="small"
        @click="setTool('edit')"
      >
        编辑
      </el-button>
    </div>

    <el-divider direction="vertical" />

    <!-- 几何运算 -->
    <div class="toolbar-section">
      <el-button
        size="small"
        :disabled="!selectedAnnotation"
        @click="handlePadding"
      >
        填充
      </el-button>

      <el-button
        size="small"
        :disabled="!selectedAnnotation"
        @click="handleStickup"
      >
        复制
      </el-button>

      <el-button
        type="danger"
        size="small"
        :disabled="!selectedAnnotation"
        @click="handleDelete"
      >
        删除
      </el-button>
    </div>

    <el-divider direction="vertical" />

    <!-- 撤销/重做 -->
    <div class="toolbar-section">
      <el-button
        size="small"
        :disabled="!canUndo"
        @click="handleUndo"
      >
        撤销
      </el-button>

      <el-button
        size="small"
        :disabled="!canRedo"
        @click="handleRedo"
      >
        重做
      </el-button>
    </div>

    <el-divider direction="vertical" />

    <!-- 编辑操作区（仅在编辑模式下显示） -->
    <div v-if="currentTool === 'edit' && selectedAnnotation" class="toolbar-section edit-actions">
      <el-button type="success" size="small" @click="handleSaveEdit">
        保存
      </el-button>
      <el-button type="info" size="small" @click="handleCancelEdit">
        取消
      </el-button>
    </div>

    <el-divider direction="vertical" v-if="currentTool === 'edit' && selectedAnnotation" />

    <el-divider direction="vertical" />

    <!-- 标签选择 -->
    <div class="toolbar-section">
      <el-select
        v-model="selectedTagId"
        placeholder="选择标签"
        size="small"
        style="width: 120px"
      >
        <el-option
          v-for="tag in tags"
          :key="tag.tagId"
          :label="tag.name"
          :value="tag.tagId"
        />
      </el-select>
    </div>

    <!-- 状态显示 -->
    <div class="toolbar-status">
      <el-tag size="small" type="info">
        标注数: {{ annotationCount }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import type { AnnotationTool, AnnotationDTO } from '@/types/annotation'
import * as annotationApi from '@/api/annotations'

// Props
interface Props {
  slideId?: number
  imageId?: number
  tags?: Array<{ tagId: number; name: string }>
}

const props = withDefaults(defineProps<Props>(), {
  slideId: undefined,
  imageId: undefined,
  tags: () => []
})

// Emits
const emit = defineEmits<{
  (e: 'tool-change', tool: AnnotationTool): void
  (e: 'annotation-select', annotation: any): void
  (e: 'annotations-load', annotations: any[]): void
}>()

// 响应式数据
const currentTool = ref<AnnotationTool>('select')
const selectedTagId = ref<number>()
const selectedAnnotation = ref<any>(null)
const annotationCount = ref(0)
const canUndo = ref(false)
const canRedo = ref(false)

// 监听 tags 变化，自动选择第一个标签作为默认值
watch(() => props.tags, (newTags) => {
  if (newTags && newTags.length > 0 && !selectedTagId.value) {
    selectedTagId.value = newTags[0].tagId
    console.log('[AnnotationToolbar] 自动选择默认标签:', newTags[0])
  }
}, { immediate: true })

// 设置工具模式
const setTool = (tool: AnnotationTool) => {
  console.log('[AnnotationToolbar] 切换工具:', currentTool.value, '->', tool)
  currentTool.value = tool
  emit('tool-change', tool)
}

// 加载标注列表
const loadAnnotations = async () => {
  if (!props.slideId) {
    console.warn('[AnnotationToolbar] slideId 为空，无法加载标注')
    return
  }

  try {
    console.log('[AnnotationToolbar] 开始加载标注, slideId:', props.slideId)
    const res = await annotationApi.getAnnotations(props.slideId)
    
    console.log('[AnnotationToolbar] API 响应:', res)
    console.log('[AnnotationToolbar] res.data:', res.data)
    console.log('[AnnotationToolbar] res.data 类型:', typeof res.data, Array.isArray(res.data))
    
    // 处理不同的响应格式
    let annotations: any[] = []
    
    if (res.data && Array.isArray(res.data)) {
      annotations = res.data
    } else if (res.data && res.data.list && Array.isArray(res.data.list)) {
      // 分页格式
      annotations = res.data.list
    } else if (Array.isArray(res)) {
      // 直接返回数组
      annotations = res
    } else {
      console.warn('[AnnotationToolbar] 未知的数据格式:', res)
    }
    
    console.log('[AnnotationToolbar] 解析后的标注数量:', annotations.length)
    if (annotations.length > 0) {
      console.log('[AnnotationToolbar] 第一个标注示例:', annotations[0])
    }
    
    annotationCount.value = annotations.length
    emit('annotations-load', annotations)

    // 检查撤销/重做状态
    await checkUndoRedoStatus()
    
    console.log('[AnnotationToolbar] 标注加载完成，已触发 annotations-load 事件')
  } catch (error) {
    console.error('[AnnotationToolbar] 加载标注失败:', error)
    ElMessage.error('加载标注失败')
  }
}

// 检查撤销/重做状态
const checkUndoRedoStatus = async () => {
  if (!props.slideId) return

  try {
    const res = await annotationApi.checkUndoRedoStatus(props.slideId)
    const status = res.data
    canUndo.value = status?.undo || false
    canRedo.value = status?.redo || false
  } catch (error) {
    console.error('检查撤销状态失败:', error)
  }
}

// 删除标注
const handleDelete = async () => {
  if (!selectedAnnotation.value) return

  try {
    await ElMessageBox.confirm('确定要删除此标注吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await annotationApi.deleteAnnotation(selectedAnnotation.value.annotationId)
    ElMessage.success('删除成功')

    // 重新加载标注
    await loadAnnotations()
    selectedAnnotation.value = null
    emit('annotation-select', null)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除标注失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 填充标注
const handlePadding = async () => {
  if (!selectedAnnotation.value) return

  try {
    await annotationApi.paddingAnnotation(selectedAnnotation.value.annotationId)
    ElMessage.success('填充成功')
    await loadAnnotations()
  } catch (error: any) {
    console.error('填充标注失败:', error)
    ElMessage.error(error.message || '填充失败')
  }
}

// 复制标注
const handleStickup = async () => {
  if (!selectedAnnotation.value || !props.slideId) {
    console.warn('[AnnotationToolbar] 无法复制：没有选中标注或 slideId')
    return
  }

  try {
    console.log('[AnnotationToolbar] ========== 开始复制标注 ==========')
    console.log('[AnnotationToolbar] selectedAnnotation:', selectedAnnotation.value)
    
    // 获取原始标注ID
    const sourceAnnotationId = selectedAnnotation.value.annotationId || selectedAnnotation.value.properties?.annotationId
    
    if (!sourceAnnotationId) {
      console.error('[AnnotationToolbar] 无法获取原始标注ID')
      ElMessage.error('无法获取标注ID')
      return
    }
    
    // 获取新标签ID（如果用户选择了新标签）
    const newTagId = selectedTagId.value
    
    console.log('[AnnotationToolbar] 原始标注ID:', sourceAnnotationId)
    console.log('[AnnotationToolbar] 新标签ID:', newTagId || '使用原标签')
    console.log('[AnnotationToolbar] 调用 stickupAnnotation API...')

    // 只传递 annotationId 和可选的 tagId，后端负责查询原始数据并创建副本
    await annotationApi.stickupAnnotation(sourceAnnotationId, newTagId)
    
    console.log('[AnnotationToolbar] 复制成功')
    ElMessage.success('复制成功')
    
    // 重新加载标注列表
    await loadAnnotations()
    console.log('[AnnotationToolbar] ========== 复制完成 ==========')
  } catch (error: any) {
    console.error('[AnnotationToolbar] 复制标注失败:', error)
    console.error('[AnnotationToolbar] 错误详情:', error.response?.data || error.message)
    ElMessage.error(error.message || '复制失败')
  }
}

// 撤销
const handleUndo = async () => {
  if (!props.slideId) return

  try {
    await annotationApi.undoAnnotation(props.slideId)
    ElMessage.success('撤销成功')
    await loadAnnotations()
  } catch (error: any) {
    console.error('撤销失败:', error)
    ElMessage.error(error.message || '撤销失败')
  }
}

// 重做
const handleRedo = async () => {
  if (!props.slideId) return

  try {
    await annotationApi.redoAnnotation(props.slideId)
    ElMessage.success('重做成功')
    await loadAnnotations()
  } catch (error: any) {
    console.error('重做失败:', error)
    ElMessage.error(error.message || '重做失败')
  }
}

// 监听slideId变化，自动加载标注
watch(() => props.slideId, (newSlideId) => {
  if (newSlideId) {
    loadAnnotations()
  }
}, { immediate: true })

// 暴露方法给父组件
defineExpose({
  loadAnnotations,
  setTool,
  setSelectedAnnotation: (annotation: any) => {
    selectedAnnotation.value = annotation
    emit('annotation-select', annotation)
  },
  getCurrentTool: () => currentTool.value,
  getSelectedTagId: () => selectedTagId.value,
  selectedTagId
})

// 处理保存编辑
const handleSaveEdit = () => {
  // 调用父组件（Viewer）暴露的方法
  const viewerInstance = (window as any).__viewerInstance
  if (viewerInstance && viewerInstance.submitEdit) {
    viewerInstance.submitEdit()
  }
}

// 处理取消编辑
const handleCancelEdit = () => {
  // 调用父组件（Viewer）暴露的方法
  const viewerInstance = (window as any).__viewerInstance
  if (viewerInstance && viewerInstance.cancelEdit) {
    viewerInstance.cancelEdit()
  }
}
</script>

<style scoped lang="scss">
.annotation-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

  .toolbar-section {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .toolbar-status {
    margin-left: auto;
  }
}
</style>


<template>
  <div class="viewer-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button @click="goBack" size="small">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
      </div>

      <div class="toolbar-right">
        <!-- 鼠标坐标显示 -->
        <div class="coordinate-display" v-if="mouseCoordinates">
          X: {{ mouseCoordinates.x.toFixed(2) }}, Y: {{ mouseCoordinates.y.toFixed(2) }}
        </div>
        
        <el-select v-model="selectedMagnification" @change="handleMagnificationChange" size="small" style="width: 100px">
          <el-option
            v-for="mag in availableMagnifications"
            :key="mag.value"
            :label="mag.label"
            :value="mag.value"
          />
        </el-select>
      </div>
    </div>

    <!-- 标注工具栏 -->
    <AnnotationToolbar
      ref="annotationToolbarRef"
      :slide-id="slideId"
      :image-id="imageId"
      :tags="tags"
      @tool-change="handleToolChange"
      @annotation-select="handleAnnotationSelect"
      @annotations-load="handleAnnotationsLoad"
    />

    <!-- 地图容器 -->
    <div ref="mapContainer" class="map-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import AnnotationToolbar from '@/components/AnnotationToolbar.vue'
import { useMapCore } from '@/composables/useMapCore'
import { useAnnotationInteractions } from '@/composables/useAnnotationInteractions'
import GeoJSON from 'ol/format/GeoJSON'
import Style from 'ol/style/Style'
import Stroke from 'ol/style/Stroke'
import Fill from 'ol/style/Fill'
import Polygon from 'ol/geom/Polygon'
import LineString from 'ol/geom/LineString'
import Feature from 'ol/Feature'
import * as tagApi from '@/api/tags'
import type { Tag } from '@/types/tag'
import '@/styles/viewer.scss'

const route = useRoute()
const router = useRouter()
const mapContainer = ref<HTMLDivElement>()
const annotationToolbarRef = ref<InstanceType<typeof AnnotationToolbar>>()

// 使用 Composables
const {
  map,
  vectorSource,
  vectorLayer,
  availableMagnifications,
  selectedMagnification,
  initMap,
  setMagnification
} = useMapCore()

const slideId = ref<number>()
const imageId = ref<number>()
const tags = ref<Tag[]>([])
const mouseCoordinates = ref<{ x: number; y: number } | null>(null)

// 加载标签列表
const loadTags = async () => {
  try {
    const res = await tagApi.getAllTags()
    // 假设后端返回的是标准响应，数据在 data 字段中
    tags.value = res.data || res || []
  } catch (error) {
    console.error('[Viewer] 加载标签失败:', error)
    // 降级：如果接口失败，使用默认标签
    tags.value = [
      { tagId: 1, name: '肿瘤', category: 'default' },
      { tagId: 2, name: '炎症', category: 'default' },
      { tagId: 3, name: '坏死', category: 'default' }
    ] as any
  }
}

const {
  currentTool,
  isEditing,
  handleToolChange,
  confirmEdit,
  cancelEdit
} = useAnnotationInteractions(map, vectorSource, vectorLayer, slideId, imageId, annotationToolbarRef)

// 返回操作
const goBack = () => router.back()

// 倍率变化处理
const handleMagnificationChange = (zoom: number) => setMagnification(zoom)

// 标注选择处理（简化）
const handleAnnotationSelect = (annotation: any) => {
  // 调用交互层的选择逻辑
}

// 标注加载处理
const handleAnnotationsLoad = (annotations: any[]) => {
  if (!vectorSource.value || !map.value) return

  console.log('[Viewer] 开始加载标注,数量:', annotations.length)
  vectorSource.value.clear()

  // ========== 测试：直接渲染写死的 GeoJSON 坐标 ==========
  // addTestAnnotation()  // 已注释：移除测试标注
  // ============================================

  const geojsonFormat = new GeoJSON()
  const mapProjection = map.value.getView().getProjection()

  annotations.forEach((annotation: any) => {
    try {
      let feature
      // 处理标准 GeoJSON 格式
      if (annotation.type === 'Feature' && annotation.geometry) {
        feature = geojsonFormat.readFeature(annotation, {
          dataProjection: mapProjection,
          featureProjection: mapProjection
        })
        // 设置业务属性
        const props = annotation.properties || {}
        feature.setProperties({
          annotationId: props.annotationId || annotation.id,
          slideId: props.slideId,
          imageId: props.imageId,
          tagId: props.tagId,
          geomType: props.geomType
        })
      } 
      // 处理自定义 geom 格式
      else if (annotation.geom) {
        const geomData = typeof annotation.geom === 'string' ? JSON.parse(annotation.geom) : annotation.geom
        const tempFeature = {
          type: 'Feature',
          geometry: geomData,
          properties: {
            annotationId: annotation.annotationId || annotation.id,
            tagId: annotation.tagId
          }
        }
        feature = geojsonFormat.readFeature(tempFeature, {
          dataProjection: mapProjection,
          featureProjection: mapProjection
        })
      }

      if (feature) {
        vectorSource.value.addFeature(feature)
      }
    } catch (error) {
      console.error('[Viewer] 解析标注失败:', error, annotation)
    }
  })
}

/**
 * 添加测试标注（Y 坐标已取负）
 * @deprecated 已移除测试标注功能
 */
// const addTestAnnotation = () => {
//   if (!vectorSource.value || !map.value) return
//   
//   console.log('[Viewer] ========== 添加测试标注 ==========')
//   
//   // 获取图像实际高度
//   const view = map.value.getView()
//   const extent = view.calculateExtent(map.value.getSize())
//   const imageHeight = Math.abs(extent[3] - extent[1])
//   
//   console.log('[Viewer] 图像高度:', imageHeight)
//   
//   // ========== 测试: LineString 标注 (从 test.json, Y 已取负) ==========
//   let offset = 0
//   const lineCoords: [number, number][] =
//     [
//       [
//         181-offset,
//         0
//       ],
//       [
//         181-offset,
//         -128
//       ],
//       [
//         304-offset,
//         -128
//       ],
//       [
//         304-offset,
//         0
//       ],
//       [
//         181-offset,
//         0
//       ]
//     ]

//   
//   // 翻转 Y 坐标

//   const lineString = new LineString(lineCoords)
//   const lineFeature = new Feature({
//     geometry: lineString,
//     annotationId: 999999,
//     categoryName: 'test-line'
//   })
//   
//   lineFeature.setStyle(new Style({
//     stroke: new Stroke({ color: '#FF0000', width: 2 })
//   }))
//   
//   vectorSource.value.addFeature(lineFeature)
//   console.log('[Viewer] ✅ LineString 标注已添加 (红色)')
//   
//   console.log('[Viewer] ====================================')
//   
//   // 注释掉自动缩放，保持图像以原始尺寸显示
//   // setTimeout(() => {
//   //   if (map.value && lineFeature.getGeometry()) {
//   //     const extent = lineFeature.getGeometry()!.getExtent()
//   //     console.log('[Viewer] 缩放到范围:', extent)
//   //     map.value.getView().fit(extent, {
//   //       padding: [100, 100, 100, 100],
//   //       duration: 1000
//   //     })
//   //     console.log('[Viewer] 🎯 已缩放到标注范围')
//   //   }
//   // }, 500)
// }

// 生命周期
onMounted(async () => {
  if (!mapContainer.value) return
  const imageIdParam = route.params.id as string
  
  // 并行加载标签和地图
  await Promise.all([
    loadTags(),
    (async () => {
      try {
        const meta = await initMap(mapContainer.value, imageIdParam)
        slideId.value = meta.slideId
        imageId.value = meta.imageId
        
        // 初始化键盘和右键监听
        setupEditListeners()
        
        // 初始化鼠标坐标监听
        setupMouseCoordinateTracking()
      } catch (error) {
        console.error('Map init error:', error)
      }
    })()
  ])
  
  // 确保进入页面时处于只读选择模式（在所有资源加载完成后）
  handleToolChange('select')
  
  // 挂载全局实例供调试或工具栏调用
  ;(window as any).__viewerInstance = {
    submitEdit: confirmEdit,
    cancelEdit: cancelEdit,
    getCurrentTool: () => currentTool.value,
    getSelectedTagId: () => annotationToolbarRef.value?.getSelectedTagId(),
    loadAnnotations: () => annotationToolbarRef.value?.loadAnnotations()
  }
})

// 设置编辑监听器（右键保存、快捷键）
const setupEditListeners = () => {
  if (!map.value) return
  
  // 右键保存
  map.value.on('contextmenu', (event) => {
    if (isEditing.value) {
      event.preventDefault()
      confirmEdit()
    }
  })
  
  // 键盘监听
  const handleKeyDown = (e: KeyboardEvent) => {
    if (!isEditing.value) return
    if (e.key === 'Enter') {
      e.preventDefault()
      confirmEdit()
    } else if (e.key === 'Escape') {
      e.preventDefault()
      cancelEdit()
    }
  }
  document.addEventListener('keydown', handleKeyDown)
  
  onBeforeUnmount(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })
}

// 设置鼠标坐标跟踪
const setupMouseCoordinateTracking = () => {
  if (!map.value || !mapContainer.value) return
  
  const handleMouseMove = (event: MouseEvent) => {
    const pixel = map.value!.getEventPixel(event)
    const coordinate = map.value!.getCoordinateFromPixel(pixel)
    
    if (coordinate) {
      // OpenLayers Zoomify 使用像素坐标系，原点在左上角
      // coordinate[0] = x, coordinate[1] = y
      mouseCoordinates.value = {
        x: coordinate[0],
        y: coordinate[1]
      }
    }
  }
  
  mapContainer.value.addEventListener('mousemove', handleMouseMove)
  
  // 清理监听器
  onBeforeUnmount(() => {
    if (mapContainer.value) {
      mapContainer.value.removeEventListener('mousemove', handleMouseMove)
    }
  })
}

onBeforeUnmount(() => {
  ;(window as any).__viewerInstance = null
  if (map.value) {
    map.value.setTarget(undefined)
  }
})

// 暴露方法
defineExpose({
  loadAnnotations: () => annotationToolbarRef.value?.loadAnnotations(),
  setTool: handleToolChange,
  submitEdit: confirmEdit,
  cancelEdit: cancelEdit,
  isEditing: () => isEditing.value
})
</script>

<style scoped lang="scss">
/* 保持原有的样式不变 */
.viewer-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.toolbar {
  height: 50px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.coordinate-display {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  white-space: nowrap;
}
.map-container {
  flex: 1;
  position: relative;
}
</style>

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
import { ElMessage } from 'element-plus'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import Zoomify from 'ol/source/Zoomify'
import OverviewMap from 'ol/control/OverviewMap'
import { defaults as defaultControls } from 'ol/control/defaults.js'
import Draw from 'ol/interaction/Draw'
import Select from 'ol/interaction/Select'
import Modify from 'ol/interaction/Modify'
import Translate from 'ol/interaction/Translate'
import Snap from 'ol/interaction/Snap'
import { click } from 'ol/events/condition'
import GeoJSON from 'ol/format/GeoJSON'
import Style from 'ol/style/Style'
import Stroke from 'ol/style/Stroke'
import Fill from 'ol/style/Fill'
import Circle from 'ol/style/Circle'
import AnnotationToolbar from '@/components/AnnotationToolbar.vue'
import { getThumbnailUrl } from '@/api/images'
import * as annotationApi from '@/api/annotations'
import type { AnnotationTool, AnnotationDTO } from '@/types/annotation'
import '@/styles/viewer.scss'

const route = useRoute()
const router = useRouter()

// 工具函数：递归处理坐标，保留指定小数位数
const roundCoordinates = (geometry: any, decimals: number): any => {
  if (!geometry) return geometry
  
  const multiplier = Math.pow(10, decimals)
  
  // 处理 Point 类型: [x, y]
  if (typeof geometry[0] === 'number' && typeof geometry[1] === 'number') {
    return geometry.map((coord: number) => Math.round(coord * multiplier) / multiplier)
  }
  
  // 处理嵌套数组（LineString, Polygon 等）
  if (Array.isArray(geometry)) {
    return geometry.map((item: any) => roundCoordinates(item, decimals))
  }
  
  // 处理对象（Geometry 对象）
  if (geometry.type && geometry.coordinates) {
    return {
      ...geometry,
      coordinates: roundCoordinates(geometry.coordinates, decimals)
    }
  }
  
  return geometry
}

// 响应式数据
const mapContainer = ref<HTMLDivElement>()
const selectedMagnification = ref<number>(0)
const availableMagnifications = ref<Array<{ label: string; value: number }>>([])
let baseMagnification = 40
let maxZoomLevel = 0

// 标注相关数据
const annotationToolbarRef = ref<InstanceType<typeof AnnotationToolbar>>()
const slideId = ref<number>()
const imageId = ref<number>()
const tags = ref<Array<{ tagId: number; name: string }>>([
  { tagId: 1, name: '肿瘤' },
  { tagId: 2, name: '炎症' },
  { tagId: 3, name: '坏死' }
])

// OpenLayers对象
let map: Map | null = null
let vectorSource: VectorSource | null = null
let vectorLayer: VectorLayer<VectorSource> | null = null
let drawInteraction: Draw | null = null
let selectInteraction: Select | null = null
let modifyInteraction: Modify | null = null
let translateInteraction: Translate | null = null
let snapInteraction: Snap | null = null
let currentTool: AnnotationTool = 'select'

// 返回操作
const goBack = () => {
  router.back()
}

// 清理绘图交互
const clearDrawInteraction = () => {
  if (drawInteraction && map) {
    map.removeInteraction(drawInteraction)
    drawInteraction = null
  }
  if (modifyInteraction && map) {
    map.removeInteraction(modifyInteraction)
    modifyInteraction = null
  }
  if (translateInteraction && map) {
    map.removeInteraction(translateInteraction)
    translateInteraction = null
  }
  if (snapInteraction && map) {
    map.removeInteraction(snapInteraction)
    snapInteraction = null
  }
}

// 设置绘图交互
const setupDrawInteraction = (tool: AnnotationTool) => {
  if (!map || !vectorSource) return

  // 清除现有交互
  clearDrawInteraction()

  // 根据工具类型创建相应的交互
  let drawType: 'Point' | 'LineString' | 'Polygon' | undefined
  let isFreehand = false

  switch (tool) {
    case 'draw-point':
      drawType = 'Point'
      break
    case 'draw-line':
      drawType = 'LineString'
      isFreehand = true // 线条开启自由绘制
      break
    case 'draw-polygon':
      drawType = 'Polygon'
      isFreehand = true // 多边形开启自由绘制（画笔模式）
      break
    default:
      return
  }

  // 获取当前选中的标签颜色
  const currentTagId = annotationToolbarRef.value?.getSelectedTagId()
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  const colorIndex = currentTagId ? (Number(currentTagId) - 1) % colors.length : 0
  const drawColor = colors[colorIndex] || '#409EFF'

  // 创建绘图交互
  drawInteraction = new Draw({
    source: vectorSource,
    type: drawType,
    freehand: isFreehand, // 开启自由绘制模式
    freehandCondition: click, // 按下鼠标即开始绘制
    stopClick: true,
    style: new Style({
      stroke: new Stroke({
        color: drawColor,
        width: 3,
        lineCap: 'round', // 圆头笔触
        lineJoin: 'round' // 圆角连接
      }),
      fill: new Fill({
        color: drawColor + '33' // 20% 透明度
      }),
      image: new Circle({
        radius: 5,
        fill: new Fill({ color: drawColor }),
        stroke: new Stroke({ color: '#fff', width: 2 })
      })
    })
  })

  // 监听绘制完成事件
  drawInteraction.on('drawend', async (event) => {
    const feature = event.feature
    let geometry = feature.getGeometry()

    if (!geometry || !slideId.value) {
      console.warn('[Viewer] drawend: geometry 或 slideId 为空')
      return
    }

    // 优化：简化几何体，减少顶点数量（对于自由绘制的轨迹非常必要）
    // 容差值设为 2 像素，在保持形状的同时大幅减少点数
    if (isFreehand) {
      geometry = geometry.simplify(2)
      feature.setGeometry(geometry)
      console.log('[Viewer] 已简化几何体，减少冗余顶点')
    }

    console.log('[Viewer] ========== 绘制完成 ==========')
    console.log('[Viewer] Geometry type:', geometry.getType())
    console.log('[Viewer] Slide ID:', slideId.value)
    console.log('[Viewer] Image ID:', imageId.value)

    try {
      // 转换为 GeoJSON
      const geojsonFormat = new GeoJSON()
      let geomJson = geojsonFormat.writeGeometryObject(geometry)
      
      // 保留两位小数精度
      geomJson = roundCoordinates(geomJson, 2)
      
      console.log('[Viewer] 转换后的 GeoJSON（保留2位小数）:', JSON.stringify(geomJson))

      // 获取当前选中的标签ID
      const currentTagId = annotationToolbarRef.value?.getSelectedTagId()
      console.log('[Viewer] 当前选中的 tagId:', currentTagId)

      // 验证标签是否已选择
      if (!currentTagId) {
        ElMessage.warning('请先在工具栏选择一个标签')
        console.warn('[Viewer] 未选择标签，删除绘制的要素')
        // 删除绘制失败的要素
        vectorSource?.removeFeature(feature)
        return
      }

      // 构建标注数据
      const annotationData: AnnotationDTO = {
        slideId: slideId.value,
        imageId: imageId.value,
        tagId: currentTagId,
        geom: geomJson,
        geomType: drawType,
        creationSource: 'MANUAL_DRAWING',
        description: ''
      }

      console.log('[Viewer] 准备保存标注:', annotationData)

      // 调用后端API保存标注
      console.log('[Viewer] 调用 addAnnotation API...')
      const result = await annotationApi.addAnnotation(annotationData)
      console.log('[Viewer] addAnnotation 响应:', result)

      ElMessage.success('标注保存成功')

      // 重新加载标注列表（添加短暂延迟，确保数据库事务已提交）
      console.log('[Viewer] 等待 500ms 后重新加载标注...')
      setTimeout(async () => {
        console.log('[Viewer] 开始重新加载标注列表...')
        console.log('[Viewer] annotationToolbarRef:', annotationToolbarRef.value)
        
        if (annotationToolbarRef.value) {
          console.log('[Viewer] 调用 loadAnnotations()')
          await annotationToolbarRef.value.loadAnnotations()
          console.log('[Viewer] loadAnnotations() 调用完成')
        } else {
          console.error('[Viewer] annotationToolbarRef 为空，无法加载标注')
        }
        
        console.log('[Viewer] ========== 绘制完成处理结束 ==========')
      }, 500)

    } catch (error: any) {
      console.error('[Viewer] 保存标注失败:', error)
      console.error('[Viewer] 错误详情:', error.response?.data || error.message)
      ElMessage.error(error.message || '保存标注失败')

      // 删除绘制失败的要素
      console.warn('[Viewer] 删除绘制失败的要素')
      vectorSource?.removeFeature(feature)
    }
  })

  map.addInteraction(drawInteraction)

  // 添加捕捉交互（辅助绘图）
  snapInteraction = new Snap({
    source: vectorSource
  })
  map.addInteraction(snapInteraction)
}

// 编辑状态管理
const editingFeature = ref<any>(null)  // 当前正在编辑的要素
const originalGeometry = ref<any>(null)  // 原始几何（用于取消时恢复）
const isEditing = ref(false)  // 是否处于编辑确认状态

// 设置选择交互
const setupSelectInteraction = () => {
  if (!map || !vectorSource) return

  clearDrawInteraction()

  // 创建选择交互
  selectInteraction = new Select({
    condition: click,
    hitTolerance: 10, // 增加点击容差，方便选中线条（单位：像素）
    layers: [vectorLayer!], // 限定只在标注层进行选择，提高性能
    style: (feature) => {
      const geomType = feature.getGeometry()?.getType()
      
      // 高亮样式（红色）- 使用 Style 实例
      if (geomType === 'Point') {
        return new Style({
          image: new Circle({
            radius: 7,
            fill: new Fill({ color: '#FF0000' }),
            stroke: new Stroke({ color: '#fff', width: 2 })
          })
        })
      } else if (geomType === 'LineString') {
        return new Style({
          stroke: new Stroke({
            color: '#FF0000',
            width: 3
          })
        })
      } else {
        // Polygon 或其他类型
        return new Style({
          stroke: new Stroke({
            color: '#FF0000',
            width: 3
          }),
          fill: new Fill({
            color: 'rgba(255, 0, 0, 0.1)'
          })
        })
      }
    }
  })

  // 监听选择事件
  selectInteraction.on('select', (event) => {
    const selected = event.selected
    if (selected.length > 0) {
      const feature = selected[0]
      const properties = feature.getProperties()
      annotationToolbarRef.value?.setSelectedAnnotation({
        annotationId: properties.annotationId,
        geometry: feature.getGeometry(),
        properties: properties
      })
    } else {
      annotationToolbarRef.value?.setSelectedAnnotation(null)
    }
  })

  map.addInteraction(selectInteraction)

  // 添加悬停反馈：鼠标移到标注上时变为手型
  map.on('pointermove', (event) => {
    const pixel = map.getEventPixel(event.originalEvent)
    const hit = map.hasFeatureAtPixel(pixel, {
      layerFilter: (layer) => layer === vectorLayer
    })
    map.getTargetElement().style.cursor = hit ? 'pointer' : ''
  })

  // 添加修改交互（编辑模式）
  modifyInteraction = new Modify({
    source: vectorSource,
    condition: (event) => {
      // 只有在已经选中了要素，且点击的是该要素时才允许修改
      // 这样可以防止在选择时直接触发编辑
      const selectedFeatures = selectInteraction?.getFeatures()
      return selectedFeatures && selectedFeatures.getLength() > 0
    }
  })
  map.addInteraction(modifyInteraction)

  // 添加平移交互（整体拖拽）
  translateInteraction = new Translate({
    features: selectInteraction.getFeatures(), // 只允许拖拽已选中的要素
    hitTolerance: 10
  })
  
  // 监听平移结束事件 - 触发保存逻辑
  translateInteraction.on('translateend', (event) => {
    const feature = event.features.item(0)
    if (feature) {
      // 标记为正在编辑，并调用确认保存
      editingFeature.value = feature
      isEditing.value = true
      console.log('[Viewer] 标注图形拖拽完成，准备保存')
      // 自动触发保存，或者你可以选择在这里显示“保存/取消”按钮
      confirmEdit()
    }
  })
  
  map.addInteraction(translateInteraction)

  // 监听修改开始事件 - 保存原始几何
  modifyInteraction.on('modifystart', (event) => {
    const features = event.features.getArray()
    if (features.length > 0) {
      const feature = features[0]
      const geojsonFormat = new GeoJSON()
      
      // 保存原始几何（深拷贝）
      originalGeometry.value = geojsonFormat.writeGeometryObject(feature.getGeometry())
      editingFeature.value = feature
      isEditing.value = true
      
      console.log('[Viewer] ========== 开始编辑标注 ==========')
      console.log('[Viewer] annotationId:', feature.getProperties().annotationId)
      console.log('[Viewer] 已保存原始几何，可以调整形状')
      console.log('[Viewer] 操作提示：')
      console.log('[Viewer]   - 右键点击：确认保存')
      console.log('[Viewer]   - 按 Enter 键：确认保存')
      console.log('[Viewer]   - 按 Esc 键：取消编辑')
      
      ElMessage.info('调整形状后，右键点击或按 Enter 确认保存，按 Esc 取消')
    }
  })

  // 监听地图右键点击 - 确认保存
  map.on('contextmenu', async (event) => {
    if (!isEditing.value || !editingFeature.value) return
    
    event.preventDefault()  // 阻止默认右键菜单
    
    console.log('[Viewer] 检测到右键点击，准备保存修改...')
    await confirmEdit()
  })

  // 监听键盘事件 - Enter 确认，Esc 取消
  const handleKeyDown = async (e: KeyboardEvent) => {
    if (!isEditing.value) return
    
    if (e.key === 'Enter') {
      e.preventDefault()
      console.log('[Viewer] 检测到 Enter 键，准备保存修改...')
      await confirmEdit()
    } else if (e.key === 'Escape') {
      e.preventDefault()
      console.log('[Viewer] 检测到 Esc 键，取消编辑...')
      cancelEdit()
    }
  }
  
  // 添加键盘监听
  document.addEventListener('keydown', handleKeyDown)
  
  // 将清理函数保存到组件实例，以便在卸载时移除
  ;(window as any).__viewerKeyHandler = handleKeyDown
}

// 标注工具变化处理
const handleToolChange = (tool: AnnotationTool) => {
  console.log('[Viewer] Tool changed:', tool)
  currentTool = tool

  // 如果切换工具时正在编辑，先取消编辑
  if (isEditing.value && tool !== 'edit') {
    console.log('[Viewer] 切换工具，取消当前编辑')
    cancelEdit()
  }

  // 根据工具类型设置相应的交互
  if (tool === 'select') {
    setupSelectInteraction()
    // 在选择模式下，禁用 Modify 和 Translate 交互
    if (modifyInteraction) modifyInteraction.setActive(false)
    if (translateInteraction) translateInteraction.setActive(false)
  } else if (tool === 'edit') {
    setupSelectInteraction()
    // 在编辑模式下，启用 Modify 和 Translate 交互
    if (modifyInteraction) modifyInteraction.setActive(true)
    if (translateInteraction) translateInteraction.setActive(true)
  } else if (['draw-point', 'draw-line', 'draw-polygon'].includes(tool)) {
    setupDrawInteraction(tool)
    // 绘制模式下，禁用选择和修改
    if (modifyInteraction) modifyInteraction.setActive(false)
  }
}

// 确认编辑 - 保存修改
const confirmEdit = async () => {
  if (!isEditing.value || !editingFeature.value) {
    console.warn('[Viewer] 没有正在编辑的标注')
    return
  }

  const feature = editingFeature.value
  const properties = feature.getProperties()

  if (!properties.annotationId) {
    console.warn('[Viewer] 标注缺少 annotationId，无法更新')
    ElMessage.warning('标注数据异常')
    cancelEdit()
    return
  }

  try {
    console.log('[Viewer] ========== 确认保存标注修改 ==========')
    console.log('[Viewer] annotationId:', properties.annotationId)
    
    // 获取修改后的几何
    const geojsonFormat = new GeoJSON()
    let geomJson = geojsonFormat.writeGeometryObject(feature.getGeometry())
    
    // 保留两位小数精度
    geomJson = roundCoordinates(geomJson, 2)
    
    console.log('[Viewer] 修改后的几何（保留2位小数）:', JSON.stringify(geomJson))

    // 构建更新数据
    const updateData: AnnotationDTO = {
      annotationId: properties.annotationId,
      slideId: properties.slideId || slideId.value,
      imageId: properties.imageId || imageId.value,
      tagId: properties.tagId,
      geom: geomJson,
      geomType: properties.geomType,
      description: properties.description,
      area: properties.area,
      perimeter: properties.perimeter
    }
    
    console.log('[Viewer] 更新数据:', updateData)
    console.log('[Viewer] 调用 updateAnnotation API...')

    // 调用后端API更新标注
    await annotationApi.updateAnnotation(updateData)
    
    console.log('[Viewer] 标注更新成功')
    ElMessage.success('标注更新成功')
    
    // 重置编辑状态
    isEditing.value = false
    editingFeature.value = null
    originalGeometry.value = null
    
    // 重新加载标注列表以同步状态
    console.log('[Viewer] 重新加载标注列表...')
    await annotationToolbarRef.value?.loadAnnotations()
    console.log('[Viewer] ========== 标注更新完成 ==========')
    
  } catch (error: any) {
    console.error('[Viewer] 更新标注失败:', error)
    console.error('[Viewer] 错误详情:', error.response?.data || error.message)
    ElMessage.error(error.message || '更新标注失败')
    
    // 更新失败，恢复原始状态
    console.warn('[Viewer] 恢复原始状态...')
    cancelEdit()
  }
}

// 取消编辑 - 恢复原始形状
const cancelEdit = () => {
  if (!isEditing.value || !editingFeature.value || !originalGeometry.value) {
    console.warn('[Viewer] 没有可以取消的编辑')
    isEditing.value = false
    editingFeature.value = null
    originalGeometry.value = null
    return
  }

  try {
    console.log('[Viewer] 取消编辑，恢复原始形状')
    
    const geojsonFormat = new GeoJSON()
    const geometry = geojsonFormat.readGeometry(originalGeometry.value, {
      dataProjection: map!.getView().getProjection(),
      featureProjection: map!.getView().getProjection()
    })
    
    // 恢复原始几何
    editingFeature.value.setGeometry(geometry)
    
    ElMessage.info('已取消编辑，恢复原状')
    
    // 重置编辑状态
    isEditing.value = false
    editingFeature.value = null
    originalGeometry.value = null
    
  } catch (error) {
    console.error('[Viewer] 恢复原始形状失败:', error)
    // 如果恢复失败，重新加载所有标注
    annotationToolbarRef.value?.loadAnnotations()
    
    isEditing.value = false
    editingFeature.value = null
    originalGeometry.value = null
  }
}

// 标注选择处理
const handleAnnotationSelect = (annotation: any) => {
  console.log('[Viewer] Annotation selected:', annotation)

  // 在地图上高亮选中的标注
  if (!vectorSource || !annotation) return

  // 清除之前的选择
  if (selectInteraction) {
    selectInteraction.getFeatures().clear()
  }

  // 查找并选中对应的要素
  const features = vectorSource.getFeatures()
  const targetFeature = features.find(f =>
    f.getProperties().annotationId === annotation.annotationId
  )

  if (targetFeature && selectInteraction) {
    selectInteraction.getFeatures().push(targetFeature)
  }
}

// 标注加载完成处理
const handleAnnotationsLoad = (annotations: any[]) => {
  console.log('[Viewer] ========== 开始处理标注加载 ==========')
  console.log('[Viewer] Annotations loaded:', annotations.length)
  console.log('[Viewer] Annotations data:', annotations)

  if (!vectorSource || !map) {
    console.warn('[Viewer] vectorSource 或 map 未初始化')
    return
  }

  // 清空现有要素
  vectorSource.clear()
  console.log('[Viewer] 已清空矢量源')

  // 获取地图投影
  const mapProjection = map.getView().getProjection()
  console.log('[Viewer] Map projection:', mapProjection.getCode())

  if (annotations.length === 0) {
    console.log('[Viewer] 没有标注数据，跳过渲染')
    console.log('[Viewer] ========== 标注加载结束 ==========')
    return
  }

  // 添加标注要素到地图
  const geojsonFormat = new GeoJSON()
  let successCount = 0
  let failCount = 0

  annotations.forEach((annotation: any, index: number) => {
    try {
      console.log(`[Viewer] --- 处理标注 ${index + 1}/${annotations.length} ---`)
      console.log('[Viewer] 原始数据:', JSON.stringify(annotation, null, 2))
      
      let feature
      
      // 判断数据格式
      if (annotation.type === 'Feature' && annotation.geometry) {
        console.log('[Viewer] 检测到标准 GeoJSON Feature 格式')
        console.log('[Viewer] Properties:', annotation.properties)
        console.log('[Viewer] Geometry:', JSON.stringify(annotation.geometry))
        
        // 检查坐标是否有效
        const coords = annotation.geometry.coordinates
        const hasInvalidCoords = JSON.stringify(coords).includes('null') || 
                                  JSON.stringify(coords).includes('NaN')
        
        if (hasInvalidCoords) {
          console.error('[Viewer] ⚠️ 坐标数据包含无效值 (null/NaN)')
          console.error('[Viewer] 原始坐标:', coords)
          failCount++
          return
        }
        
        // 标准 GeoJSON Feature 格式
        // 注意：后端返回的是像素坐标，不是经纬度，所以不需要投影转换
        console.log('[Viewer] 使用像素坐标系（不进行投影转换）')
        
        feature = geojsonFormat.readFeature(annotation, {
          dataProjection: mapProjection,  // 直接使用地图坐标系
          featureProjection: mapProjection
        })
        
        // 验证转换后的坐标
        const convertedGeom = feature.getGeometry()
        const convertedCoords = convertedGeom?.getCoordinates()
        console.log('[Viewer] 转换后的坐标:', convertedCoords)
        
        const hasNaNAfterConvert = JSON.stringify(convertedCoords).includes('NaN')
        if (hasNaNAfterConvert) {
          console.error('[Viewer] ⚠️ 坐标仍然包含 NaN，数据可能有问题')
          console.error('[Viewer] 原始坐标:', coords)
          failCount++
          return
        }
        
        // 从 properties 中提取业务属性
        const props = annotation.properties || {}
        console.log('[Viewer] 提取的属性:', props)
        
        feature.setProperties({
          annotationId: props.annotationId || annotation.id,
          slideId: props.slideId,
          imageId: props.imageId,
          tagId: props.tagId,
          geomType: props.geomType,
          area: props.area,
          perimeter: props.perimeter,
          description: props.description,
          creationSource: props.creationSource,
          createBy: props.createBy,
          createTime: props.createTime
        })
        
        console.log('[Viewer] 设置后的 tagId:', feature.get('tagId'))
      } else if (annotation.geom) {
        console.log('[Viewer] 检测到自定义 geom 格式')
        // 直接包含 geom 字段的格式
        const geomData = typeof annotation.geom === 'string' 
          ? JSON.parse(annotation.geom) 
          : annotation.geom
        
        console.log('[Viewer] 解析后的 geometry:', geomData)
        
        // 构建临时 Feature
        const tempFeature = {
          type: 'Feature',
          geometry: geomData,
          properties: {
            annotationId: annotation.annotationId || annotation.id,
            slideId: annotation.slideId,
            tagId: annotation.tagId,
            geomType: annotation.geomType,
            description: annotation.description
          }
        }
        
        // 使用像素坐标系（不转换）
        feature = geojsonFormat.readFeature(tempFeature, {
          dataProjection: mapProjection,
          featureProjection: mapProjection
        })
      } else {
        console.warn('[Viewer] 未知的标注格式，缺少 type/geometry 或 geom 字段:', annotation)
        failCount++
        return
      }
      
      // 设置样式属性（根据标签ID设置不同颜色）
      const tagId = feature.get('tagId')
      console.log(`[Viewer] 标注 ${index + 1} - 获取到的 tagId:`, tagId, '类型:', typeof tagId)
      
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
      const colorIndex = tagId ? (Number(tagId) - 1) % colors.length : 0
      const color = colors[colorIndex] || '#409EFF'
      
      console.log(`[Viewer] 标注 ${index + 1} - 使用颜色: ${color}`)
      
      // 注意：不在这里设置样式，而是在矢量图层中统一设置样式函数
      // feature.setStyle() 需要 Style 实例，不支持字面量
      // 样式将在 vectorLayer 的 style 函数中应用
      
      console.log(`[Viewer] 标注 ${index + 1} - 准备添加到矢量源`)

      vectorSource.addFeature(feature)
      successCount++
      console.log(`[Viewer] ✓ 标注 ${index + 1} 添加成功`)
      
    } catch (error) {
      failCount++
      console.error(`[Viewer] ✗ 添加标注要素 ${index + 1} 失败:`, error)
      console.error('[Viewer] 失败的数据:', annotation)
    }
  })
  
  console.log('[Viewer] ========== 标注加载统计 ==========')
  console.log('[Viewer] 总数:', annotations.length)
  console.log('[Viewer] 成功:', successCount)
  console.log('[Viewer] 失败:', failCount)
  console.log('[Viewer] Total features in vector source:', vectorSource.getFeatures().length)
  
  // 调试：检查要素的坐标范围
  if (vectorSource.getFeatures().length > 0) {
    const features = vectorSource.getFeatures()
    const extent = vectorSource.getExtent()
    console.log('[Viewer] 矢量源范围 (extent):', extent)
    console.log('[Viewer] 地图视图范围:', map.getView().calculateExtent())
    console.log('[Viewer] 地图中心点:', map.getView().getCenter())
    console.log('[Viewer] 地图缩放级别:', map.getView().getZoom())
    
    // 检查范围是否有效
    const isValidExtent = extent.every(val => isFinite(val))
    if (!isValidExtent) {
      console.error('[Viewer] ⚠️ 矢量源范围包含无效值 (Infinity/NaN)')
      console.error('[Viewer] 这通常是因为坐标数据有问题')
      console.error('[Viewer] 请检查后端返回的坐标格式')
      console.log('[Viewer] ========== 标注加载结束 ==========')
      return
    }
    
    // 检查第一个要素的坐标
    const firstFeature = features[0]
    const geom = firstFeature.getGeometry()
    console.log('[Viewer] 第一个要素几何类型:', geom?.getType())
    console.log('[Viewer] 第一个要素坐标:', geom?.getCoordinates())
    console.log('[Viewer] 第一个要素范围:', geom?.getExtent())
    
    // 检查是否在视图范围内
    const viewExtent = map.getView().calculateExtent()
    const featureExtent = geom?.getExtent()
    if (featureExtent && viewExtent) {
      const isInView = !(
        featureExtent[0] > viewExtent[2] || // 要素在视图右侧
        featureExtent[2] < viewExtent[0] || // 要素在视图左侧
        featureExtent[1] > viewExtent[3] || // 要素在视图上方
        featureExtent[3] < viewExtent[1]    // 要素在视图下方
      )
      console.log('[Viewer] 第一个要素是否在视图中:', isInView)
      
      if (!isInView) {
        console.warn('[Viewer] ⚠️ 要素不在当前视图范围内！')
        console.warn('[Viewer] 建议：调整视图以包含要素范围')
        
        // 自动调整视图以显示所有要素
        try {
          console.log('[Viewer] 自动调整视图...')
          map.getView().fit(extent, {
            padding: [50, 50, 50, 50],
            duration: 1000
          })
          console.log('[Viewer] 视图调整成功')
        } catch (error) {
          console.error('[Viewer] 视图调整失败:', error)
          console.error('[Viewer] 可能原因：extent 为空或无效')
        }
      }
    }
  }
  
  console.log('[Viewer] ========== 标注加载结束 ==========')
}

// 倍率选择变化处理
const handleMagnificationChange = (zoom: number) => {
  if (map) {
    map.getView().setZoom(zoom)
  }
}

// 初始化地图
const initMap = async () => {
  if (!mapContainer.value) return

  const imageIdParam = route.params.id as string
  const tileSize = 256

  try {
    // 使用 API 函数获取元数据 URL
    const metadataUrl = getThumbnailUrl(parseInt(imageIdParam), 512).replace('/thumbnail', '/metadata')
    console.log('[Viewer] Fetching metadata from:', metadataUrl)

    const response = await fetch(metadataUrl)
    if (!response.ok) throw new Error('获取图像信息失败')

    const result = await response.json()
    const metadata = result.data || result

    const width = metadata.width || 31509
    const height = metadata.height || 37084
    baseMagnification = metadata.magnification || 40

    // 设置标注相关数据
    imageId.value = metadata.imageId || parseInt(imageIdParam)
    slideId.value = metadata.slideId || imageId.value

    // 创建瓦片源（使用正确的路径）
    const tilesBaseUrl = `/biz/api/v1/images/${imageIdParam}/tiles/`
    console.log('[Viewer] Tiles base URL:', tilesBaseUrl)

    const zoomifySource = new Zoomify({
      url: tilesBaseUrl,
      size: [width, height],
      tileSize: tileSize,
      crossOrigin: 'anonymous',
      zDirection: -1
    })

    // 重写 tileUrlFunction 以添加 tileSize 查询参数
    const originalTileUrlFunction = zoomifySource.getTileUrlFunction()
    zoomifySource.setTileUrlFunction(function(tileCoord, pixelRatio, projection) {
      const url = originalTileUrlFunction.call(this, tileCoord, pixelRatio, projection)
      if (url) {
        const separator = url.includes('?') ? '&' : '?'
        return `${url}${separator}tileSize=${tileSize}`
      }
      return url
    })

    const tileGrid = zoomifySource.getTileGrid()
    maxZoomLevel = tileGrid.getMaxZoom()
    const extent = tileGrid.getExtent()
    const resolutions = tileGrid.getResolutions()

    // 生成倍率选项（只显示整数倍率，去重）
    const magnifications: Array<{ label: string; value: number }> = []
    const seenMagnifications = new Set<number>()

    for (let z = 0; z <= maxZoomLevel; z++) {
      const mag = baseMagnification * Math.pow(2, z - maxZoomLevel)
      const roundedMag = Math.round(mag)

      // 只添加大于0且未出现过的整数倍率
      if (roundedMag > 0 && !seenMagnifications.has(roundedMag)) {
        seenMagnifications.add(roundedMag)
        magnifications.push({
          label: `${roundedMag}x`,
          value: z
        })
      }
    }
    // 反转数组，让大的倍率在前
    availableMagnifications.value = magnifications

    // 设置初始倍率为最大倍率
    selectedMagnification.value = 1

    // 创建矢量图层（用于显示标注）
    vectorSource = new VectorSource()
    vectorLayer = new VectorLayer({
      source: vectorSource,
      style: (feature) => {
        const geomType = feature.getGeometry()?.getType()
        const tagId = feature.get('tagId')
        
        // 根据标签ID设置不同颜色
        const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
        const color = colors[(Number(tagId) - 1) % colors.length] || '#409EFF'
        
        // 根据几何类型返回不同的样式（使用 Style 实例）
        if (geomType === 'Point') {
          return new Style({
            image: new Circle({
              radius: 5,
              fill: new Fill({ color }),
              stroke: new Stroke({ color: '#fff', width: 2 })
            })
          })
        } else if (geomType === 'LineString') {
          return new Style({
            stroke: new Stroke({
              color,
              width: 2
            })
          })
        } else {
          // Polygon 或其他类型
          return new Style({
            stroke: new Stroke({
              color,
              width: 2
            }),
            fill: new Fill({
              color: color + '1A' // 10% 透明度
            })
          })
        }
      }
    })

    // 创建视图
    const view = new View({
      resolutions: resolutions,
      extent: extent,
      constrainOnlyCenter: true,
      center: [width / 2, height / 2],
      zoom: 1
    })

    // 创建概览地图控件
    const overviewMapControl = new OverviewMap({
      layers: [
        new TileLayer({
          source: zoomifySource
        })
      ],
      collapsed: false
    })

    // 创建地图
    map = new Map({
      target: mapContainer.value,
      layers: [
        new TileLayer({
          source: zoomifySource
        }),
        vectorLayer
      ],
      view: view,
      controls: defaultControls().extend([overviewMapControl])
    })

    // 初始化选择交互（默认模式）
    setupSelectInteraction()
    // 确保初始状态下，编辑和拖拽功能是禁用的
    if (modifyInteraction) modifyInteraction.setActive(false)
    if (translateInteraction) translateInteraction.setActive(false)
    currentTool = 'select' // 设置当前工具为选择模式

    // 监听缩放变化，同步更新倍率选择器
    view.on('change:resolution', () => {
      const zoom = view.getZoom()
      if (zoom !== undefined && zoom !== selectedMagnification.value) {
        selectedMagnification.value = Math.round(zoom)
      }
    })

    console.log('[Viewer] Map initialized')

  } catch (error) {
    console.error('[Viewer] Init failed:', error)
  }
}

// 生命周期
onMounted(() => {
  initMap()
  // 将当前实例挂载到 window，方便工具栏调用
  ;(window as any).__viewerInstance = {
    submitEdit: confirmEdit,
    cancelEdit: cancelEdit
  }
})

onBeforeUnmount(() => {
  // 清理全局引用
  ;(window as any).__viewerInstance = null
  
  // 移除键盘监听
  if ((window as any).__viewerKeyHandler) {
    document.removeEventListener('keydown', (window as any).__viewerKeyHandler)
    ;(window as any).__viewerKeyHandler = null
  }
  
  // 清理编辑状态
  isEditing.value = false
  editingFeature.value = null
  originalGeometry.value = null
  
  // 清理资源
  if (map) {
    map.setTarget(undefined)
    map = null
  }
  clearDrawInteraction()
  if (selectInteraction && map) {
    map.removeInteraction(selectInteraction)
    selectInteraction = null
  }
})

// 暴露方法给父组件（必须放在最后）
defineExpose({
  loadAnnotations: () => annotationToolbarRef.value?.loadAnnotations(),
  setTool: (tool: AnnotationTool) => handleToolChange(tool),
  setSelectedAnnotation: (annotation: any) => {
    selectedAnnotation.value = annotation
    emit('annotation-select', annotation)
  },
  getCurrentTool: () => currentTool.value,
  getSelectedTagId: () => annotationToolbarRef.value?.getSelectedTagId(),
  // 新增：提交编辑
  submitEdit: confirmEdit,
  // 新增：取消编辑
  cancelEdit: cancelEdit,
  // 新增：获取编辑状态
  isEditing: () => isEditing.value
})
</script>

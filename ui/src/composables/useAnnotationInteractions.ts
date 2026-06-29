import { ref, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import Map from 'ol/Map'
import VectorSource from 'ol/source/Vector'
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
import CircleStyle from 'ol/style/Circle'
import * as annotationApi from '@/api/annotations'
import type { AnnotationTool, AnnotationDTO } from '@/types/annotation'
import { useAnnotationStyle } from './useAnnotationStyle'

/**
 * 标注交互引擎
 */
export const useAnnotationInteractions = (
  map: Ref<Map | null>,
  vectorSource: Ref<VectorSource | null>,
  vectorLayer: Ref<any | null>, // 新增 vectorLayer 引用
  slideId: Ref<number | undefined>,
  imageId: Ref<number | undefined>,
  toolbarRef: Ref<any>
) => {
  const currentTool = ref<AnnotationTool>('select')
  const isEditing = ref(false)
  
  let drawInteraction: Draw | null = null
  let selectInteraction: Select | null = null
  let modifyInteraction: Modify | null = null
  let translateInteraction: Translate | null = null
  let snapInteraction: Snap | null = null
  
  const editingFeature = ref<any>(null)
  const originalGeometry = ref<any>(null)
  
  const { roundCoordinates, createHighlightStyle, getTagColor } = useAnnotationStyle()

  // --- 清理逻辑 ---
  const clearInteractions = () => {
    if (!map.value) return
    
    // 调试：检查当前地图上的所有交互
    const mapInteractions = map.value.getInteractions()
    const drawCount = mapInteractions.getArray().filter(i => i instanceof Draw).length
    console.log('[Interaction] clearInteractions - 清理前地图上的 Draw 交互数量:', drawCount)
    
    // 将 selectInteraction 加入清理列表
    const interactions = [drawInteraction, selectInteraction, modifyInteraction, translateInteraction, snapInteraction]
    interactions.forEach(interaction => {
      if (interaction) {
        map.value!.removeInteraction(interaction)
        console.log('[Interaction] 已移除交互:', interaction.constructor.name)
      }
    })
    drawInteraction = null
    selectInteraction = null  // 重置 selectInteraction
    modifyInteraction = null
    translateInteraction = null
    snapInteraction = null
    
    // 调试：检查清理后的交互数量
    const afterDrawCount = map.value.getInteractions().getArray().filter(i => i instanceof Draw).length
    console.log('[Interaction] clearInteractions - 清理后地图上的 Draw 交互数量:', afterDrawCount)
  }

  // --- 绘制交互 ---
  const setupDraw = (tool: AnnotationTool) => {
    if (!map.value || !vectorSource.value) return
    clearInteractions()

    let drawType: 'Point' | 'LineString' | 'Polygon' | undefined
    let isFreehand = false

    if (tool === 'draw-point') drawType = 'Point'
    else if (tool === 'draw-line') { drawType = 'LineString'; isFreehand = true }
    else if (tool === 'draw-polygon') { drawType = 'Polygon'; isFreehand = true }
    else return

    const tagId = toolbarRef.value?.getSelectedTagId()
    const color = getTagColor(tagId || 1)

    drawInteraction = new Draw({
      source: vectorSource.value,
      type: drawType,
      freehand: isFreehand,
      freehandCondition: click,
      stopClick: true,
      style: new Style({
        stroke: new Stroke({ color, width: 3, lineCap: 'round', lineJoin: 'round' }),
        fill: new Fill({ color: color + '33' }),
        image: new CircleStyle({ radius: 5, fill: new Fill({ color }), stroke: new Stroke({ color: '#fff', width: 2 }) })
      })
    })

    drawInteraction.on('drawend', async (event) => {
      const feature = event.feature
      let geometry = feature.getGeometry()
      if (!geometry || !slideId.value) return

      // 简化几何体
      if (isFreehand) {
        geometry = geometry.simplify(2)
        feature.setGeometry(geometry)
      }

      try {
        const geojsonFormat = new GeoJSON()
        let geomJson = geojsonFormat.writeGeometryObject(geometry)
        geomJson = roundCoordinates(geomJson, 2)

        const currentTagId = toolbarRef.value?.getSelectedTagId()
        if (!currentTagId) {
          ElMessage.warning('请先选择标签')
          vectorSource.value?.removeFeature(feature)
          return
        }

        const annotationData: AnnotationDTO = {
          slideId: slideId.value,
          imageId: imageId.value,
          tagId: currentTagId,
          geom: geomJson,
          geomType: drawType,
          creationSource: 'MANUAL_DRAWING',
          description: ''
        }

        await annotationApi.addAnnotation(annotationData)
        ElMessage.success('标注保存成功')
        
        // 延迟刷新以同步后端状态
        setTimeout(() => toolbarRef.value?.loadAnnotations(), 500)
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
        vectorSource.value?.removeFeature(feature)
      }
    })

    map.value.addInteraction(drawInteraction)
    console.log('[Interaction] Draw 交互已添加到地图')
    snapInteraction = new Snap({ source: vectorSource.value })
    map.value.addInteraction(snapInteraction)
    
    // 调试：检查添加后的 Draw 交互数量
    const drawCount = map.value.getInteractions().getArray().filter(i => i instanceof Draw).length
    console.log('[Interaction] setupDraw - 地图上的 Draw 交互总数:', drawCount)
  }

  // --- 选择与编辑交互 ---
  const setupSelect = () => {
    if (!map.value || !vectorSource.value) return
    
    // 强制移除地图上所有的 Draw 交互（防止多个 Draw 交互存在）
    const mapInteractions = map.value.getInteractions().getArray()
    mapInteractions.forEach(interaction => {
      if (interaction instanceof Draw) {
        map.value!.removeInteraction(interaction)
        console.log('[Interaction] 强制移除地图上的 Draw 交互')
      }
    })
    drawInteraction = null
    
    // 移除 Snap 交互
    if (snapInteraction) { 
      map.value.removeInteraction(snapInteraction)
      snapInteraction = null 
    }

    // 如果已经存在 Select 交互，先移除再重建（确保样式和配置最新）
    if (selectInteraction) map.value.removeInteraction(selectInteraction)
    if (modifyInteraction) map.value.removeInteraction(modifyInteraction)
    if (translateInteraction) map.value.removeInteraction(translateInteraction)

    selectInteraction = new Select({
      condition: click,
      hitTolerance: 10,
      layers: vectorLayer.value ? [vectorLayer.value] : undefined,
      style: createHighlightStyle
    })

    selectInteraction.on('select', (event) => {
      const selectedFeatures = event.target.getFeatures()
      const selectedFeature = selectedFeatures.getArray()[0]

      if (selectedFeature) {
        const properties = selectedFeature.getProperties()
        const annotation = {
          annotationId: properties.annotationId,
          slideId: properties.slideId,
          imageId: properties.imageId,
          tagId: properties.tagId,
          geomType: properties.geomType,
          properties: properties
        }
        toolbarRef.value?.setSelectedAnnotation(annotation)
      } else {
        toolbarRef.value?.setSelectedAnnotation(null)
      }
    })

    map.value.addInteraction(selectInteraction)

    // 修改交互
    modifyInteraction = new Modify({
      source: vectorSource.value,
      condition: () => selectInteraction!.getFeatures().getLength() > 0
    })
    
    // 根据当前工具决定初始激活状态
    const isEditMode = currentTool.value === 'edit'
    modifyInteraction.setActive(isEditMode)
    
    // 监听修改开始：记录原始状态
    modifyInteraction.on('modifystart', (e) => {
      const feature = e.features.getArray()[0]
      if (feature) {
        const geojsonFormat = new GeoJSON()
        originalGeometry.value = geojsonFormat.writeGeometryObject(feature.getGeometry())
        editingFeature.value = feature
        isEditing.value = true
      }
    })

    // 监听修改结束：触发确认保存
    modifyInteraction.on('modifyend', (e) => {
      if (isEditing.value) {
        console.log('[Interaction] 顶点修改完成，准备保存...')
        confirmEdit()
      }
    })

    map.value.addInteraction(modifyInteraction)

    // 平移交互
    translateInteraction = new Translate({
      features: selectInteraction.getFeatures(),
      hitTolerance: 10
    })
    
    // 根据当前工具决定初始激活状态
    translateInteraction.setActive(isEditMode)
    
    translateInteraction.on('translateend', (event) => {
      const feature = event.features.item(0)
      if (feature) {
        editingFeature.value = feature
        isEditing.value = true
        confirmEdit() // 拖拽结束自动保存
      }
    })
    map.value.addInteraction(translateInteraction)
  }

  // --- 工具切换控制 ---
  const handleToolChange = (tool: AnnotationTool) => {
    console.log('[Interaction] 切换工具:', tool)
    currentTool.value = tool
    
    // 如果切换工具时正在编辑，先取消编辑
    if (isEditing.value && tool !== 'edit') {
      console.log('[Interaction] 切换工具，取消当前编辑状态')
      cancelEdit()
    }

    if (tool === 'select') {
      // 调试：检查切换前的状态
      console.log('[Interaction] 切换到 select 模式 - drawInteraction:', drawInteraction)
      if (map.value) {
        const drawCount = map.value.getInteractions().getArray().filter(i => i instanceof Draw).length
        console.log('[Interaction] 切换到 select 模式 - 地图上的 Draw 交互数量:', drawCount)
      }
      
      // 无论 selectInteraction 是否存在，都强制调用 setupSelect()
      // 确保绘制交互被正确移除，选择交互被正确设置
      setupSelect()
      
      // 调试：检查切换后的状态
      setTimeout(() => {
        if (map.value) {
          const drawCount = map.value.getInteractions().getArray().filter(i => i instanceof Draw).length
          console.log('[Interaction] 切换到 select 模式后 - 地图上的 Draw 交互数量:', drawCount)
        }
        console.log('[Interaction] 切换到 select 模式后 - drawInteraction:', drawInteraction)
      }, 50)

      // 强制进入只读模式：禁用修改和平移
      setTimeout(() => {
        if (modifyInteraction) {
          modifyInteraction.setActive(false)
          console.log('[Interaction] Modify 交互已禁用')
        }
        if (translateInteraction) {
          translateInteraction.setActive(false)
          console.log('[Interaction] Translate 交互已禁用')
        }
      }, 0)
    } else if (tool === 'edit') {
      setupSelect()
      // 进入编辑模式：启用修改和平移
      setTimeout(() => {
        if (modifyInteraction) {
          modifyInteraction.setActive(true)
          console.log('[Interaction] Modify 交互已启用')
        }
        if (translateInteraction) {
          translateInteraction.setActive(true)
          console.log('[Interaction] Translate 交互已启用')
        }
      }, 0)
    } else {
      setupDraw(tool)
    }
  }

  // --- 确认编辑 - 保存修改 ---
  const confirmEdit = async () => {
    if (!isEditing.value || !editingFeature.value) return

    const feature = editingFeature.value
    const properties = feature.getProperties()

    if (!properties.annotationId) {
      ElMessage.warning('标注数据异常')
      cancelEdit()
      return
    }

    try {
      const geojsonFormat = new GeoJSON()
      let geomJson = geojsonFormat.writeGeometryObject(feature.getGeometry())
      geomJson = roundCoordinates(geomJson, 2)

      const updateData: AnnotationDTO = {
        annotationId: properties.annotationId,
        slideId: properties.slideId || slideId.value,
        imageId: properties.imageId || imageId.value,
        tagId: properties.tagId,
        geom: geomJson,
        geomType: properties.geomType,
        description: properties.description
      }

      await annotationApi.updateAnnotation(updateData)
      ElMessage.success('标注形状更新成功')
      
      isEditing.value = false
      editingFeature.value = null
      originalGeometry.value = null
      
      // 刷新列表以同步后端状态（包括面积、周长等计算字段）
      toolbarRef.value?.loadAnnotations()
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
      cancelEdit()
    }
  }

  // --- 取消编辑 - 恢复原始形状 ---
  const cancelEdit = () => {
    if (!isEditing.value || !editingFeature.value || !originalGeometry.value) {
      isEditing.value = false
      editingFeature.value = null
      originalGeometry.value = null
      return
    }

    try {
      const geojsonFormat = new GeoJSON()
      const geometry = geojsonFormat.readGeometry(originalGeometry.value, {
        dataProjection: map.value!.getView().getProjection(),
        featureProjection: map.value!.getView().getProjection()
      })
      
      editingFeature.value.setGeometry(geometry)
      ElMessage.info('已取消编辑')
    } catch (error) {
      console.error('恢复失败:', error)
      toolbarRef.value?.loadAnnotations()
    } finally {
      isEditing.value = false
      editingFeature.value = null
      originalGeometry.value = null
    }
  }

  return {
    currentTool,
    isEditing,
    handleToolChange,
    confirmEdit,
    cancelEdit
  }
}
<template>
  <div class="map-toolbar">
    <el-button-group>
      <el-button
          @click="toggleSelect"
          :type="isActive(MODE.SELECT) ? 'primary' : ''"
      >
        <el-icon><Select /></el-icon>
        选择图形
      </el-button>
      <el-button
          @click="startDrawing(MODE.POINT)"
          :type="isActive(MODE.POINT) ? 'primary' : ''"
      >
        <el-icon><Location /></el-icon>
        绘制点
      </el-button>
      <el-button
          @click="startDrawing(MODE.LINESTRING)"
          :type="isActive(MODE.LINESTRING) ? 'primary' : ''"
      >
        <el-icon><Connection /></el-icon>
        绘制线
      </el-button>
      <el-button
          @click="startDrawing(MODE.POLYGON)"
          :type="isActive(MODE.POLYGON) ? 'primary' : ''"
      >
        <el-icon><Opportunity /></el-icon>
        绘制面
      </el-button>
      <el-button
          @click="startDrawing(MODE.FREE_POLYGON)"
          :type="isActive(MODE.FREE_POLYGON) ? 'primary' : ''"
      >
        <el-icon><Opportunity /></el-icon>
        自由面
      </el-button>
      <el-button @click="clearFeatures">
        <el-icon><Delete /></el-icon>
        清除
      </el-button>
      <el-button @click="zoomIn">
        <el-icon><ZoomIn /></el-icon>
        放大
      </el-button>
      <el-button @click="zoomOut">
        <el-icon><ZoomOut /></el-icon>
        缩小
      </el-button>
    </el-button-group>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import VectorSource from 'ol/source/Vector'
import Draw from 'ol/interaction/draw'
import VectorLayer from 'ol/layer/Vector'
import Select from 'ol/interaction/Select'
import { click } from 'ol/events/condition'
import { Fill, Stroke, Style, Circle } from 'ol/style'
import { annotationApi } from '@api/annotation/anno-api.ts' // 引入你定义的 API 方法
import GeoJSON from 'ol/format/GeoJSON'
import { MODE } from '@/enums/mapMode'
import { FEATURE_STYLE_CONFIG } from './mapFeatureStyle'
import { featureStyle } from './olFeatureStyle'
import { Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection } from 'ol/geom'

import {
  Location,
  Connection,
  Opportunity,
  Delete,
  ZoomIn,
  ZoomOut,
  Select as SelectIcon
} from '@element-plus/icons-vue'

export default {
  components: {
    Location,
    Connection,
    Opportunity,
    Delete,
    ZoomIn,
    ZoomOut,
    Select: SelectIcon
  },
  props: {
    map: {
      type: Object,
      required: true
    },
    layers: {
      type: Object,
      required: true
    },
    slideId: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const MODE_REF = ref(MODE)
    const activeMode = ref(MODE.NONE)
    const vectorSource = new VectorSource()
    const vectorLayer = new VectorLayer({
      source: vectorSource,
      style: (feature) => {
        const geometry = feature.getGeometry()
        return geometry ? featureStyle[geometry.getType()] : null
      }
    })

    let currentInteraction = null
    let drawEndListener = null

    // 判断当前是否激活
    const isActive = (mode) => activeMode.value === mode

    // 清理当前交互
    const stopCurrentInteraction = () => {
      if (!props.map) return
      // 优先移除事件监听器
      if (drawEndListener && currentInteraction) {
        currentInteraction.un('drawend', drawEndListener)
        drawEndListener = null
      }

      // 确保从地图中移除交互
      if (currentInteraction) {
        try {
          currentInteraction.setActive(false)
          props.map.removeInteraction(currentInteraction)
        } catch (e) {
          console.warn('移除交互时出错:', e)
        }
      }

      // 重置状态
      activeMode.value = MODE.NONE
    }

    // 切换选择状态
    const toggleSelect = () => {
      if (!props.map) return

      if (isActive(MODE.SELECT)) {
        stopCurrentInteraction()
      } else {
        startSelect()
      }
    }

    /**
     * 创建一个“鼠标拖动画矩形”的 geometryFunction
     */
    function createBoxGeometryFunction() {
      let start, end

      return function (coordinates, state) {
        const geom = new Polygon([], 'XY')
        let start = []
        if (coordinates.length === 0 && state === 'start') {
          start = coordinates[0]
        } else if (coordinates.length >= 1 && state !== 'abort') {
          end = coordinates[coordinates.length - 1]
          const dx = end[0] - start[0]
          const dy = end[1] - start[1]
          const coords = [
            start,
            [start[0] + dx, start[1]],
            [start[0] + dx, start[1] + dy],
            [start[0], start[1] + dy],
            start
          ]
          geom.setCoordinates([coords])
        }
        return geom
      }
    }

    // 开始选择
    const startSelect = () => {
      stopCurrentInteraction()
      //创建layers数组
      const vectorLayers = props.layers || []
      vectorLayers.push(vectorLayer)
      const selectInteraction = new Select({
        layers: vectorLayers,
        condition: click,
        style: featureStyle.select
      })

      currentInteraction = selectInteraction
      props.map.addInteraction(selectInteraction)
      activeMode.value = MODE.SELECT
    }

    let geometryFunction = null

    // 开始绘制
    const startDrawing = (type) => {
      if (!props.map) return

      stopCurrentInteraction()
      let drawType = type
      if (type === MODE.FREE_POLYGON) {
        drawType = MODE.POLYGON
      }

      const drawInteraction = new Draw({
        source: vectorSource,
        type: drawType,
        style: featureStyle[drawType]
      })

      drawEndListener = drawInteraction.on('drawend', async (event) => {
        const feature = event.feature // 获取绘制完成的 Feature
        console.debug('绘制完成:', feature)

        // 将 feature 转换为后端需要的数据格式（如 GeoJSON）
        const geoJsonFormat = new GeoJSON()
        const geoJsonData = geoJsonFormat.writeFeatureObject(feature)
        // 构造请求参数
        const params = {
          slideId: props.slideId, // 应从 props 或父组件传入
          geometry: geoJsonData.geometry,
          categoryId: 0,
          description: 'test',
          locationType: currentInteraction.mode_
        }

        try {
          const res = await annotationApi.addAnnotation(params)
          console.log('保存成功:', res)
        } catch (error) {
          console.error('保存失败:', error)
        }
      })

      currentInteraction = drawInteraction
      props.map.addInteraction(currentInteraction)
      activeMode.value = type
    }

    // 清除图形
    const clearFeatures = () => {
      vectorSource.clear()
      stopCurrentInteraction()
    }

    // 缩放视图
    const zoomView = (delta) => {
      if (!props.map) return
      const view = props.map.getView()
      view.animate({
        zoom: view.getZoom() + delta,
        duration: 200
      })
    }

    const zoomIn = () => zoomView(0.5)
    const zoomOut = () => zoomView(-0.5)

    // 初始化矢量图层
    onMounted(() => {
      props.map.addLayer(vectorLayer)
    })

    // 清理资源
    onBeforeUnmount(() => {
      stopCurrentInteraction()
      props.map.removeLayer(vectorLayer)
    })

    return {
      activeMode,
      MODE: MODE_REF,
      vectorSource,
      toggleSelect,
      zoomIn,
      zoomOut,
      startDrawing,
      clearFeatures,
      isActive
    }
  }
}
</script>

<style scoped>
.map-toolbar {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  background-color: rgba(255, 255, 255, 0.9);
  padding: 6px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.map-toolbar .el-button-group {
  display: flex;
  gap: 4px;
}
</style>

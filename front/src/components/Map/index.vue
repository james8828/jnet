<template>
  <el-dialog
    v-model="visible"
    :fullscreen="true"
    :close-on-click-modal="false"
    @closed="handleClose"
  >
    <el-container class="layout-container" style="height: 99vh">
      <!-- 引入绘制控件 -->
      <MapDrawControls v-if="map" :map="map" :layers="vectorLayers" :slide-id="slideId"/>
      <el-main class="map-container">
        <div ref="mapWrapperRef" class="map-wrapper" :class="{ 'map-loading': loading }">
          <div v-if="loading" class="loading-indicator">
            <el-icon class="loading-icon">
              <Loading/>
            </el-icon>
            地图加载中...
          </div>
          <div v-if="error" class="error-message">
            <el-icon class="error-icon">
              <Warning/>
            </el-icon>
            {{ error }}
          </div>
        </div>
      </el-main>
    </el-container>
  </el-dialog>
</template>

<script setup>
import {ref, nextTick, onBeforeUnmount, defineExpose, watch} from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import Zoomify from 'ol/source/Zoomify'
import VectorSource from 'ol/source/Vector'
import VectorLayer from 'ol/layer/Vector'
import {getSlideById} from '@api/image/image-api'
import {Loading, Warning} from '@element-plus/icons-vue'
import {annotationApi} from '@api/annotation/anno-api'
import MapDrawControls from './MapDrawControls.vue'
import {createStyledFeature} from './featureUtils'// import MapComponent from './Map.vue'

const emit = defineEmits(['update:modelValue','ready','error'])

// 响应式状态
const mapWrapperRef = ref(null)
const map = ref(null)
const loading = ref(true)
const error = ref(null)
const vectorLayers = []
const slideId = ref(null)
const visible = ref(false)

const handleOpen = async (id) => {
  visible.value = true
  // 使用 nextTick 等待 DOM 更新
  await nextTick()
  slideId.value = id
  // 此时 mapWrapperRef 应该已经绑定到真实 DOM 元素
  if (!mapWrapperRef.value) {
    console.error('地图容器未正确初始化')
    return
  }
  await initMapInstance()
  await renderGeoJSONData()
  emit('ready', map)
}

// 当弹框关闭时通知父组件更新状态
const handleClose = () => {
  emit('update:modelValue', false)
  if (map.value) {
    map.value.setTarget(undefined)
    map.value = null
  }
}

// 初始化地图配置
const initMapConfiguration = async () => {
  try {
    const resp = await getSlideById(slideId.value)
    const {width: imgWidth, height: imgHeight} = resp.data

    const source = new Zoomify({
      url: `image-service/slide/tile/${slideId.value}/`,
      size: [imgWidth, imgHeight],
      crossOrigin: 'anonymous',
      zDirection: -1
    })

    return {
      layer: new TileLayer({
        source,
        className: 'underlay-map-container'
      }),
      viewConfig: {
        resolutions: source.getTileGrid().getResolutions(),
        center: [imgWidth / 2, imgHeight / 2],
        zoom: 1,
        extent: source.getTileGrid().getExtent(),
        constrainOnlyCenter: true
      }
    }
  } catch (err) {
    console.error('地图配置初始化失败:', err)
    throw err
  }
}

// 初始化地图实例
const initMapInstance = async () => {
  try {
    const {layer, viewConfig} = await initMapConfiguration()

    map.value = new Map({
      layers: [layer],
      target: mapWrapperRef.value,
      view: new View(viewConfig),
      controls: []
    })
    loading.value = false
    map.value.updateSize()  // 手动触发地图尺寸更新
  } catch (err) {
    console.error('地图初始化失败:', err)
    error.value = '地图加载失败，请刷新页面重试'
    loading.value = false
  }
}

// 渲染 GeoJSON 数据
const renderGeoJSONData = async () => {
  try {
    const params = {slideId: slideId.value}
    const res = await annotationApi.selectLists(params)
    const data = res.data // 假设返回的是 GeoJSON Feature[] 数组

    if (data && data.length > 0) {
      const features = data.map(item =>
          createStyledFeature(item.geometry.type, item.geometry.coordinates)
      )
      // 创建带样式的矢量图层
      const vectorSource = new VectorSource({features})
      const vectorLayer = new VectorLayer({
        source: vectorSource
      })
      vectorLayers.push(vectorLayer)
      map.value.addLayer(vectorLayer)
    }
  } catch (error) {
    console.error('渲染 GeoJSON 数据失败:', error)
  }
}

onBeforeUnmount(() => {
  if (map.value) {
    map.value.setTarget(undefined)
    map.value = null
  }
})

defineExpose({ handleOpen })

</script>
<style scoped src="./index.css"></style>



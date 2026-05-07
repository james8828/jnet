import { ref, type Ref } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import Zoomify from 'ol/source/Zoomify'
import OverviewMap from 'ol/control/OverviewMap'
import { defaults as defaultControls } from 'ol/control/defaults.js'
import { getThumbnailUrl } from '@/api/images'
import { useAnnotationStyle } from './useAnnotationStyle'

/**
 * 地图内核管理
 */
export const useMapCore = () => {
  const map = ref<Map | null>(null)
  const vectorSource = ref<VectorSource | null>(null)
  const vectorLayer = ref<VectorLayer<VectorSource> | null>(null)
  const availableMagnifications = ref<Array<{ label: string; value: number }>>([])
  const selectedMagnification = ref<number>(0)
  
  const { createFeatureStyle } = useAnnotationStyle()

  /**
   * 初始化地图
   */
  const initMap = async (container: HTMLDivElement, imageIdParam: string) => {
    const tileSize = 256

    try {
      // 获取元数据
      const metadataUrl = getThumbnailUrl(parseInt(imageIdParam), 512).replace('/thumbnail', '/metadata')
      const response = await fetch(metadataUrl)
      if (!response.ok) throw new Error('获取图像信息失败')

      const result = await response.json()
      const metadata = result.data || result
      const width = metadata.width || 31509
      const height = metadata.height || 37084
      const baseMagnification = metadata.magnification || 40

      // 创建瓦片源
      const tilesBaseUrl = `/biz/api/v1/images/${imageIdParam}/tiles/`
      const zoomifySource = new Zoomify({
        url: tilesBaseUrl,
        size: [width, height],
        tileSize: tileSize,
        crossOrigin: 'anonymous',
        zDirection: -1
      })

      // 处理瓦片 URL
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
      const maxZoomLevel = tileGrid.getMaxZoom()
      const extent = tileGrid.getExtent()
      const resolutions = tileGrid.getResolutions()

      // 生成倍率选项
      const magnifications: Array<{ label: string; value: number }> = []
      const seenMagnifications = new Set<number>()
      for (let z = 0; z <= maxZoomLevel; z++) {
        const mag = baseMagnification * Math.pow(2, z - maxZoomLevel)
        const roundedMag = Math.round(mag)
        if (roundedMag > 0 && !seenMagnifications.has(roundedMag)) {
          seenMagnifications.add(roundedMag)
          magnifications.push({ label: `${roundedMag}x`, value: z })
        }
      }
      availableMagnifications.value = magnifications.reverse()
      selectedMagnification.value = magnifications.length > 0 ? magnifications[0].value : 0

      // 创建矢量图层
      vectorSource.value = new VectorSource()
      vectorLayer.value = new VectorLayer({
        source: vectorSource.value,
        style: createFeatureStyle
      })

      // 创建视图
      const view = new View({
        resolutions,
        extent,
        constrainOnlyCenter: true,
        center: [width / 2, height / 2],
        zoom: 1
      })

      // 创建概览图
      const overviewMapControl = new OverviewMap({
        layers: [new TileLayer({ source: zoomifySource })],
        collapsed: false
      })

      // 组装地图
      map.value = new Map({
        target: container,
        layers: [
          new TileLayer({ source: zoomifySource }),
          vectorLayer.value
        ],
        view,
        controls: defaultControls().extend([overviewMapControl])
      })

      // 监听缩放同步倍率
      view.on('change:resolution', () => {
        const zoom = view.getZoom()
        if (zoom !== undefined && zoom !== selectedMagnification.value) {
          selectedMagnification.value = Math.round(zoom)
        }
      })

      return {
        slideId: metadata.slideId || parseInt(imageIdParam),
        imageId: metadata.imageId || parseInt(imageIdParam)
      }

    } catch (error) {
      console.error('[MapCore] Init failed:', error)
      throw error
    }
  }

  /**
   * 设置倍率
   */
  const setMagnification = (zoom: number) => {
    if (map.value) {
      map.value.getView().setZoom(zoom)
    }
  }

  return {
    map,
    vectorSource,
    vectorLayer,
    availableMagnifications,
    selectedMagnification,
    initMap,
    setMagnification
  }
}

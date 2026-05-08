import { ref, type Ref } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import Zoomify from 'ol/source/Zoomify'
import OverviewMap from 'ol/control/OverviewMap'
import { defaults as defaultControls } from 'ol/control/defaults.js'
import { defaults as defaultInteractions } from 'ol/interaction/defaults.js'
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
      const width = metadata.width
      const height = metadata.height
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
      const resolutions = tileGrid.getResolutions()
      
      console.log('[MapCore] === 瓦片网格信息 ===')
      console.log('[MapCore] 最大缩放级别 (maxZoomLevel):', maxZoomLevel)
      console.log('[MapCore] 分辨率数组长度:', resolutions.length)
      console.log('[MapCore] 分辨率数组:', resolutions)
      console.log('[MapCore] TileGrid extent:', tileGrid.getExtent())
      
      // 手动设置正确的 extent（像素坐标系，原点在左上角）
      // COCO 数据集和 OpenLayers Zoomify 都使用相同的坐标系
      const correctExtent = [0, 0, width, height]
      
      console.log('[MapCore] === 图像信息 ===')
      console.log('[MapCore] 图像尺寸:', { width, height })
      console.log('[MapCore] 修正后的 extent:', correctExtent)
      
      // 计算正确的中心点（使用 TileGrid 的 extent）
      const tileGridExtent = tileGrid.getExtent()
      const centerX = (tileGridExtent[0] + tileGridExtent[2]) / 2
      const centerY = (tileGridExtent[1] + tileGridExtent[3]) / 2
      console.log('[MapCore] TileGrid 中心点:', [centerX, centerY])
      
      // OpenLayers Zoomify 使用 Y 轴向下为负的坐标系
      // 图像中心点应该是 [width/2, -height/2]
      const imageCenterX = width / 2
      const imageCenterY = -height / 2
      console.log('[MapCore] 图像中心点 (Y轴向下):', [imageCenterX, imageCenterY])

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
      
      console.log('[MapCore] 可用倍率选项:', availableMagnifications.value)
      
      // 设置默认选中的倍率（与初始 zoom 级别匹配）
      const initialZoom = maxZoomLevel  // 使用最高分辨率
      
      // 确保 initialZoom 在 availableMagnifications 中存在
      const validZoom = availableMagnifications.value.find(m => m.value === initialZoom)
      if (validZoom) {
        selectedMagnification.value = initialZoom
        console.log('[MapCore] 初始 zoom 级别:', initialZoom, '(label:', validZoom.label + ')')
      } else {
        // 如果不存在，使用最接近的 zoom 级别
        const closestMag = availableMagnifications.value.reduce((prev, curr) => {
          return Math.abs(curr.value - initialZoom) < Math.abs(prev.value - initialZoom) ? curr : prev
        })
        selectedMagnification.value = closestMag.value
        console.warn('[MapCore] initialZoom', initialZoom, '不在可用选项中，使用最接近的:', closestMag.value, '(label:', closestMag.label + ')')
      }

      // 创建矢量图层
      vectorSource.value = new VectorSource()
      vectorLayer.value = new VectorLayer({
        source: vectorSource.value,
        style: createFeatureStyle
      })

      // 创建视图
      const view = new View({
        resolutions,
        // extent: correctExtent,  // 移除 extent 限制以允许自由拖拽
        constrainOnlyCenter: true,  // 只限制中心点，允许拖拽查看边界外区域
        center: [imageCenterX, imageCenterY],  // 使用图像中心点（Y轴为负）
        zoom: maxZoomLevel,  // 使用最高分辨率（原始尺寸）
        multiWorld: false,  // 防止世界复制
        showFullExtent: false  // 不强制显示完整范围
      })
      
      console.log('[MapCore] === 视图配置 ===')
      console.log('[MapCore] View 创建完成')
      console.log('[MapCore] 初始缩放级别 (zoom):', view.getZoom())
      console.log('[MapCore] 初始中心点 (center):', view.getCenter())
      console.log('[MapCore] 初始分辨率 (resolution):', view.getResolution())
      console.log('[MapCore] 预期分辨率 (resolutions[maxZoomLevel]):', resolutions[maxZoomLevel])

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
        controls: defaultControls().extend([overviewMapControl]),
        interactions: defaultInteractions()  // 显式添加默认交互（包括拖拽）
      })

      console.log('[MapCore] 地图初始化完成')
      console.log('[MapCore] 图像尺寸:', { width, height })
      console.log('[MapCore] 最大缩放级别:', maxZoomLevel)
      console.log('[MapCore] 初始缩放级别:', view.getZoom())
      console.log('[MapCore] 初始中心点:', view.getCenter())
      
      // 强制设置视图参数，防止被自动调整
      setTimeout(() => {
        if (map.value) {
          const currentView = map.value.getView()
          console.log('[MapCore] === 验证视图状态 ===')
          console.log('[MapCore] 当前 zoom:', currentView.getZoom())
          console.log('[MapCore] 当前 center:', currentView.getCenter())
          console.log('[MapCore] 当前 resolution:', currentView.getResolution())
          console.log('[MapCore] 容器大小:', map.value.getSize())
          
          // 如果视图被改变了，重新设置
          if (currentView.getZoom() !== maxZoomLevel) {
            console.warn('[MapCore] ⚠️ 视图 zoom 被改变，重新设置为:', maxZoomLevel)
            currentView.setZoom(maxZoomLevel)
          }
        }
      }, 200)

      // 监听缩放同步倍率
      view.on('change:resolution', () => {
        const zoom = view.getZoom()
        if (zoom !== undefined) {
          // 将 zoom 四舍五入为整数
          const roundedZoom = Math.round(zoom)
          
          // 检查是否在 availableMagnifications 中存在
          const matchingMag = availableMagnifications.value.find(m => m.value === roundedZoom)
          
          if (matchingMag) {
            // 精确匹配
            if (roundedZoom !== selectedMagnification.value) {
              selectedMagnification.value = roundedZoom
              console.log('[MapCore] 倍率更新:', roundedZoom, '(label:', matchingMag.label + ')')
            }
          } else {
            // 没有精确匹配，找到最接近的选项
            const closestMag = availableMagnifications.value.reduce((prev, curr) => {
              return Math.abs(curr.value - roundedZoom) < Math.abs(prev.value - roundedZoom) ? curr : prev
            })
            
            if (closestMag.value !== selectedMagnification.value) {
              selectedMagnification.value = closestMag.value
              console.log('[MapCore] 倍率更新（最接近）:', closestMag.value, '(label:', closestMag.label + ', 实际 zoom:', roundedZoom + ')')
            }
          }
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

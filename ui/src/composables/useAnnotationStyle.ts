import Style from 'ol/style/Style'
import Stroke from 'ol/style/Stroke'
import Fill from 'ol/style/Fill'
import CircleStyle from 'ol/style/Circle'

/**
 * 标注样式与数据处理工具
 */
export const useAnnotationStyle = () => {
  // 颜色调色板
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']

  /**
   * 根据标签ID获取颜色
   */
  const getTagColor = (tagId: number | string): string => {
    if (!tagId) return colors[0]
    const index = (Number(tagId) - 1) % colors.length
    return colors[index >= 0 ? index : 0]
  }

  /**
   * 递归处理坐标，保留指定小数位数
   */
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

  /**
   * 创建要素样式（用于矢量图层）
   */
  const createFeatureStyle = (feature: any) => {
    const geomType = feature.getGeometry()?.getType()
    const tagId = feature.get('tagId')
    const color = getTagColor(tagId)
    
    if (geomType === 'Point') {
      return new Style({
        image: new CircleStyle({
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
      // Polygon
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

  /**
   * 创建高亮样式（用于 Select 交互）
   */
  const createHighlightStyle = (feature: any) => {
    const geomType = feature.getGeometry()?.getType()
    
    if (geomType === 'Point') {
      return new Style({
        image: new CircleStyle({
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

  return {
    getTagColor,
    roundCoordinates,
    createFeatureStyle,
    createHighlightStyle
  }
}

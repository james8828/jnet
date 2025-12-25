import { Feature } from 'ol'
import { Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection } from 'ol/geom'
import {featureStyle} from "@comps/Map/olFeatureStyle.js";


/**
 * 根据几何类型创建 Feature 并应用样式
 * @param {string} type - 几何类型 ('Point'|'LineString'|'Polygon'|'MultiPoint'|'MultiLineString'|'MultiPolygon'|'GeometryCollection')
 * @param {Array<*>} coordinates - 坐标数组或几何对象数组（根据 type 不同）
 * @returns {Feature}
 */
export const createStyledFeature = (type, coordinates) => {
    let geometry

    switch (type) {
        case 'Point':
            geometry = new Point(coordinates)
            break
        case 'LineString':
            geometry = new LineString(coordinates)
            break
        case 'Polygon':
            geometry = new Polygon(coordinates)
            break
        case 'MultiPoint':
            geometry = new MultiPoint(coordinates.map(coord => new Point(coord)))
            break
        case 'MultiLineString':
            geometry = new MultiLineString(
                coordinates.map(lineCoords => new LineString(lineCoords))
            )
            break
        case 'MultiPolygon':
            geometry = new MultiPolygon(
                coordinates.map(polygonCoords => new Polygon(polygonCoords))
            )
            break
        case 'GeometryCollection':
            // coordinates 应为一个几何对象数组
            geometry = new GeometryCollection(coordinates)
            break
        default:
            throw new Error(`不支持的几何类型: ${type}`)
    }

    const feature = new Feature(geometry)
    const style = featureStyle[type]

    feature.setStyle(style)

    return feature
}

// @/styles/olFeatureStyle.js

import {Style, Circle, Fill, Stroke} from 'ol/style'
import {FEATURE_STYLE_CONFIG} from '@comps/Map/mapFeatureStyle'

export const getFeatureStyle = (feature) => {
    const geometryType = feature.getGeometry().getType()
    const style = featureStyle[geometryType]
    return style || null
}

export const featureStyle = {
    select: new Style({
        image: new Circle({
            radius: FEATURE_STYLE_CONFIG.Selection.radius,
            fill: new Fill({color: FEATURE_STYLE_CONFIG.Selection.fillColor}),
            stroke: new Stroke({
                color: FEATURE_STYLE_CONFIG.Selection.strokeColor,
                width: FEATURE_STYLE_CONFIG.Selection.strokeWidth
            })
        }),
        stroke: new Stroke({
            color: FEATURE_STYLE_CONFIG.Selection.lineStrokeColor,
            width: FEATURE_STYLE_CONFIG.Selection.lineStrokeWidth
        }),
        fill: new Fill({
            color: FEATURE_STYLE_CONFIG.Selection.fill
        })
    }),
    Point: new Style({
        image: new Circle({
            radius: FEATURE_STYLE_CONFIG.Point.radius,
            fill: new Fill({color: FEATURE_STYLE_CONFIG.Point.fillColor}),
            stroke: new Stroke({
                color: FEATURE_STYLE_CONFIG.Point.strokeColor,
                width: FEATURE_STYLE_CONFIG.Point.strokeWidth
            })
        })
    }),
    LineString: new Style({
        stroke: new Stroke({
            color: FEATURE_STYLE_CONFIG.LineString.strokeColor,
            width: FEATURE_STYLE_CONFIG.LineString.strokeWidth
        })
    }),
    Polygon: new Style({
        fill: new Fill({color: FEATURE_STYLE_CONFIG.Polygon.fillColor}),
        stroke: new Stroke({
            color: FEATURE_STYLE_CONFIG.Polygon.strokeColor,
            width: FEATURE_STYLE_CONFIG.Polygon.strokeWidth
        })
    }),
    FreePolygon: new Style({
        fill: new Fill({color: 'rgba(255, 204, 0, 0.5)'}),
        stroke: new Stroke({color: '#ffcc00', width: 2})
    })
}
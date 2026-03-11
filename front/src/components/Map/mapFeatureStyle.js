// @/styles/mapFeatureStyle.js

export const FEATURE_STYLE_CONFIG = {
    Point: {
        radius: 6,
        fillColor: 'rgba(255, 0, 0, 0.8)',
        strokeColor: 'white',
        strokeWidth: 2
    },
    LineString: {
        strokeColor: 'rgba(0, 255, 0, 0.8)',
        strokeWidth: 3
    },
    Polygon: {
        fillColor: 'rgba(0, 0, 255, 0.3)',
        strokeColor: 'blue',
        strokeWidth: 2
    },
    Selection: {
        radius: 7,
        fillColor: 'rgba(255, 255, 0, 0.8)',
        strokeColor: 'red',
        strokeWidth: 2,
        lineStrokeColor: 'yellow',
        lineStrokeWidth: 3,
        fill: 'rgba(255, 255, 0, 0.2)'
    }
}


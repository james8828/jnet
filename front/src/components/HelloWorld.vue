<template>
  <h1>{{ msg }}</h1>
  <el-container>
    <el-header>
      <el-row>
        <el-col :span="4">
          <el-select
              v-model="drawType"
              placeholder="请选择要绘制图形"
              size="large"
              @change="handleSelectChange"
          >
            <el-option
                v-for="item in drawTypeList"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button @click="loadAnno" type="primary" >加载</el-button>
        </el-col>
      </el-row>
    </el-header>
    <el-main><div ref="mapDomRef" class="map" style="width: 2000px;height: 500px"></div></el-main>
  </el-container>
</template>
<script setup lang="ts">
import TileLayer from 'ol/layer/Tile';
import * as Layer from "ol/layer"
import Feature from 'ol/feature'
import View from "ol/View"
import * as Source from 'ol/source'
import * as Format from 'ol/format'
import {fromCircle as polygonFromCircle} from 'ol/geom/Polygon'
import {ElMessage} from "element-plus"
import * as OlStyle from 'ol/style'
import * as Geom from 'ol/geom'
import * as Interaction from "ol/interaction"
import Zoomify from 'ol/source/Zoomify';
import Map from "ol/Map";
import 'ol/ol.css'
import { ref, onMounted } from "vue";
import {createRegularPolygon, createBox, DrawEvent} from "ol/interaction/draw"
import {Geometry} from "ol/geom";
import axios from 'axios'



defineProps<{ msg: string }>()
const drawType = ref<string>('LineString')
const drawTypeList = ref<any[]>([
  {
    value:'Point',
    key:'Point',
    isFreehand:true,
    label:'点'
  },
  {
    value:'LineString',
    key:'LineString',
    isFreehand:true,
    label:'自由线'
  },
  {
    value:'Polygon',
    key:'Polygon',
    isFreehand:true,
    label:'自由多边形'
  },
  {
    value:'Circle',
    key:'Circle',
    isFreehand:true,
    label:'圆'
  },
  {
    value:'_LineString',
    key:'_LineString',
    isFreehand:false,
    label:'标准直线'
  },
  {
    value:'_Polygon',
    key:'_Polygon',
    isFreehand:false,
    label:'标准多边形'
  },
  {
    value:'Rectangle',
    key:'Rectangle',
    isFreehand:false,
    label:'矩形'
  },
  {
    value:'Square',
    key:'Square',
    isFreehand:false,
    label:'正方形'
  },
])
const isOpenDraw = ref<boolean>(true)
const OlMapRef = ref<InstanceType<typeof Map> | null>(null)
// 矢量图层
let vectorSource:any= new Source.Vector()
let vectorLayer:any= null
// 绘制对象
let drawObj:any = ref(null)
// 绘制矢量图层
let drawVectorSource:any= new Source.Vector()
let drawVectorLayer:any= null

const handleSelectChange = (val: any) =>{
  if(!isOpenDraw.value) return
  const obj = drawTypeList.value.find(el=>{
    return el.value === val
  })
  const params = {
    type:val,
    freehand:obj.isFreehand
  }
  drawGeometry(params)
}

function loadAnno(){
  axios.post("/jnet/anno/queryAnno", {})
      .then((res) => {
        console.log(res)
        const style = {
          fillColor: "rgba(0,0,0,0.2)",
          strokeColor: "#409eff",
          strokeWidth: 2
        }
        for (const index in res.data.data){
          const anno = res.data.data[index]
          console.log(res.data.data[index])
          renderGeometry(anno.geometry.type,anno.geometry.coordinates[0],style)
        }
      })
      .catch((err) => {
        return err
      })
}

const imgWidth = 46000;
const imgHeight = 32914;

const zoomifyUrl = 'image/slide/showTileGam/1/14560/1/0/';
const mapDomRef = ref<HTMLElement | null>(null)
const source = new Zoomify({
  url: zoomifyUrl,
  size: [imgWidth, imgHeight],
  crossOrigin: 'anonymous',
  zDirection: -1, // Ensure we get a tile with the screen resolution or higher
  // 确保我们得到屏幕分辨率或更高的切片
})
const extent = source.getTileGrid().getExtent()
const resolutions = source.getTileGrid().getResolutions()
const layer = new TileLayer({
  className: "underlay-map-container",
  source: source
});

const initMap = () => {
  OlMapRef.value = new Map({
    layers: [layer],
    target: mapDomRef.value,
    view: new View({
      // adjust zoom levels to those provided by the source
      resolutions: resolutions,
      center: [imgWidth/2, imgHeight/-2],
      zoom: 3,
      // constrain the center: center cannot be set outside this extent
      extent: extent,
      constrainOnlyCenter: true
    }),
  })
}

const drawFinish = (e:DrawEvent)=>{
  const feature: Feature = e.feature;
  const geometry: Geometry = feature.getGeometry()
  const geoJsonGeom = new Format.GeoJSON();
  if (geometry.getType() === "Circle") {
    feature.setGeometry(polygonFromCircle(geometry))
  }
  const geom = geoJsonGeom.writeFeatureObject(feature);
  const params = {
    name: 'coords.length',
    geometry: geom.geometry
  }
  axios.post("/jnet/anno/addAnno",params)
      .then((res) => {return res})
      .catch((err) => {return err})

  console.log('结束'+e)
}
const drawBegin = (e)=>{
  console.log('开始'+e)
}
/**
 * @description: 绘制自由点线面圆
 * @param {*} type 绘制类型
 * @return {*}
 */
const drawGeometry=(obj:any)=>{
  let {type,freehand}=obj
  let geometryFunction,maxPoints;
  closeDrawFn();
  if(type === '_LineString'){
    type = 'LineString'
  }else if(type === '_Polygon'){
    type = 'Polygon'
  }else if(type === 'Rectangle'){
    type = 'LineString';
    maxPoints = 2;
    geometryFunction = createBox()
  }else if(type === 'Square'){
    type = 'Circle';
    geometryFunction = createRegularPolygon(4)
  }
  drawObj.value = createInteraction({ type, freehand, geometryFunction, maxPoints })
  drawObj.value.on('drawend',drawFinish)
  drawObj.value.on('drawstart',drawBegin)
  OlMapRef.value.addInteraction(drawObj.value)
  if(!drawVectorLayer){
    drawVectorLayer = new Layer.Vector({
      source: drawVectorSource
    });
    OlMapRef.value.addLayer(drawVectorLayer);
  }else{
    OlMapRef.value.updateSize()
  }
}

// 关闭绘制功能
function closeDrawFn(){
  if(drawObj.value){
    OlMapRef.value.removeInteraction(drawObj.value);
  }
}

// 创建自由绘制类对象
function createInteraction(obj:any){
  return new Interaction.Draw({
    source: drawVectorSource,
    type: obj.type,
    freehand: obj.freehand,
    geometryFunction:obj.geometryFunction,
    maxPoints:obj.maxPoints
  });
}

/**
 * @description: 绘制图形
 * @param {*} type <string> Point,LineString,Polygon,Circle,Rectangle,Square
 * @param {*} data <array> 渲染数据
 * @param {*} style 样式对象
 * @return {*}
 */
const renderGeometry = (type: string, data: any[], style: BaseStyleInterface) => {
  let sourceFeatureArr = []
  if (type === 'Point') {
    data.forEach(el => {
      const pointCoord: Array<number> = [el.lng, el.lat]
      let sourceFeature = createPoint(pointCoord)
      const styleObj = createVectorStyle(style)
      sourceFeature.setStyle(styleObj)
      sourceFeatureArr.push(sourceFeature)
    })
  } else if (type === 'LineString') {
    if (data.length < 2) {
      ElMessage.warning('设置点位小于2个，无法连线')
      return
    }
    const lastIndex = data.length - 1
    const handleArr = [[data[0].lng, data[1].lat], [data[lastIndex].lng, data[lastIndex].lat]]
    let sourceFeature = createLine(handleArr)
    const styleObj = createVectorStyle(style)
    sourceFeature.setStyle(styleObj)
    sourceFeatureArr.push(sourceFeature)
  } else if (type === 'Polygon') {
    // 多边形
    if (data.length < 3) {
      ElMessage.warning('设置点位小于3个，无法绘制多边形')
      return
    }
    let handleArr: any[] = [];
    handleArr = data
    /*data.forEach(el => {
      let arr = [el.lng, el.lat]
      handleArr.push(arr)
    })*/
    let sourceFeature = createPolygon(handleArr)
    const styleObj = createVectorStyle(style)
    sourceFeature.setStyle(styleObj)
    sourceFeatureArr.push(sourceFeature)
  } else if (type === 'Circle') {
    data.forEach(el => {
      let sourceFeature = createCircle([el.lng, el.lat], el)
      const styleObj = createVectorStyle(style)
      sourceFeature.setStyle(styleObj)
      sourceFeatureArr.push(sourceFeature)
    })
  } else {
    return
  }
  //矢量标注的数据源
  vectorSource.addFeatures(sourceFeatureArr)
  //矢量标注图层
  if (!vectorLayer) {
    vectorLayer = new Layer.Vector({
      source: vectorSource
    });
    OlMapRef.value.addLayer(vectorLayer);
  } else {
    OlMapRef.value.updateSize()
  }
}

// 创建点要素
function createPoint(arr: Array<number>, obj: any = {}) {
  return new Feature({
    geometry: new Geom.Point(arr),
    attribute: obj
  });
}

// 创建线要素
function createLine(arr: any[], obj: any = {}) {
  const handleArr = arr.map((el: number[]) => {
    return el
  })
  return new Feature({
    geometry: new Geom.LineString(handleArr),
    attribute: obj
  });
}

// 创建多边形要素
function createPolygon(arr: any[], obj: any = {}) {
  const handleArr = arr.map((el: number[]) => {
    return el
  })
  return new Feature({
    geometry: new Geom.Polygon([handleArr]),
    attribute: obj
  });
}

// 创建圆要素
function createCircle(arr: Array<number>, obj: any = {}) {
  return new Feature({
    geometry: new Geom.Circle(arr, obj.radius)
  });
}

// 创建文字标注以及文字样式
function createText(style: LabelStyleInterface, text: any) {
  return new OlStyle.Text({
    //位置
    textAlign: style.textAlign || 'center',
    //基准线
    textBaseline: style.textBaseline || 'middle',
    //文字样式
    font: style.font || 'normal 14px 微软雅黑',
    //文本内容
    text: text || '测试',
    //文本填充样式（即文字颜色）
    fill: new OlStyle.Fill({color: style.fontColor || '#aa3300'}),
    stroke: new OlStyle.Stroke({color: style.strokeColor || '#ffcc33', width: style.strokeWidth || 2})
  })
}

// 创建样式
function createVectorStyle(style: BaseStyleInterface) {
  return new OlStyle.Style({
    //填充色
    fill: new OlStyle.Fill({
      color: style.fillColor
    }),
    //边线颜色
    stroke: new OlStyle.Stroke({
      color: style.strokeColor,
      width: style.strokeWidth
    }),
    //形状
    image: new OlStyle.Circle({
      radius: style.imageCircleRadius || 1,
      fill: new OlStyle.Fill({
        color: style.imageCircleFillColor || '#000'
      })
    })
  })
}

// 创建图标标注
function createIcon(url: any) {
  return new OlStyle.Icon({
    anchor: [0.5, 1],
    anchorOrigin: "top-right",
    anchorXUnits: "fraction",
    anchorYUnits: "fraction",
    offsetOrigin: "top-right",
    //透明度
    opacity: 0.75,
    //图标的url
    src: url
  })
}

onMounted(() => {
  initMap()
  OlMapRef.value?.getInteractions()

  //mapZoom.getView().fit(extent);
})

</script>

<style scoped>

</style>

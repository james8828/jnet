# 病理图像智能分析系统

基于 Vue 3 + TypeScript + Element Plus 开发的医学PACS风格图像标注、训练、预测系统。

## 🎯 功能特性

### 1. 工作台 (Dashboard)
- 数据统计卡片（标注图像、训练模型、预测结果、数据集）
- 类别分布饼图
- 训练进度曲线
- 最近活动时间线

### 2. 图像标注 (Annotation)
- 支持矩形、多边形、点标注工具
- 标签管理（正常组织、癌变区域、炎症区域、坏死区域）
- SVS/JPG/PNG 多格式支持
- OpenSeadragon 高清图像查看器
- Fabric.js 交互式标注画布
- GeoJSON 格式导出
- 图像列表管理与搜索

### 3. 模型训练 (Training)
- YOLOv7/YOLOv7-tiny/YOLOv8 模型选择
- 超参数配置（Epochs、Batch Size、学习率、图像尺寸）
- 数据增强选项（Mosaic、MixUp、HSV、翻转）
- 实时训练监控（mAP、Precision、Recall、Loss）
- 训练曲线可视化（ECharts）
- 实时日志输出
- 历史训练记录管理

### 4. 智能预测 (Prediction)
- 模型选择与配置
- 置信度/IOU阈值调节
- 批量上传预测
- 原图/预测/叠加三种查看模式
- 检测结果列表展示
- GeoJSON 和标注图导出

### 5. 数据管理 (Dataset)
- 数据集列表管理
- 多维度筛选（类型、状态）
- 批量导入/导出
- 标注进度统计
- 存储占用分析

## 🛠️ 技术栈

- **前端框架**: Vue 3.4 + TypeScript 5.3
- **构建工具**: Vite 5.0
- **UI组件库**: Element Plus 2.5
- **状态管理**: Pinia 2.1
- **路由管理**: Vue Router 4.2
- **图表库**: ECharts 5.4 + vue-echarts
- **图像处理**: 
  - OpenSeadragon 4.1 (SVS高清查看)
  - Fabric.js 5.3 (Canvas标注)
- **HTTP客户端**: Axios 1.6
- **样式方案**: SCSS

## 📦 安装与运行

### 1. 安装依赖
```bash
cd ui
npm install
```

### 2. 开发模式运行
```bash
npm run dev
```
访问 http://localhost:3000

### 3. 生产环境构建
```bash
npm run build
```

### 4. 预览构建结果
```bash
npm run preview
```

## 📁 项目结构

```
ui/
├── src/
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue
│   ├── views/            # 页面视图
│   │   ├── Dashboard.vue    # 工作台
│   │   ├── Annotation.vue   # 图像标注
│   │   ├── Training.vue     # 模型训练
│   │   ├── Prediction.vue   # 智能预测
│   │   └── Dataset.vue      # 数据管理
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── styles/           # 全局样式
│   │   └── main.scss
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 🎨 设计风格

### PACS医学风格特点
- **配色方案**: 深蓝色侧边栏 (#001529 → #002140)，白色内容区，医疗蓝主题色 (#1890ff)
- **界面布局**: 左侧导航 + 顶部面包屑 + 主内容区的经典PACS布局
- **视觉元素**: 
  - 圆角卡片设计 (border-radius: 8px)
  - 柔和阴影效果
  - 渐变色统计卡片
  - 清晰的图标系统

### 响应式设计
- 适配不同屏幕尺寸
- 弹性网格布局
- 自适应图表大小

## 🔌 API集成

当前为前端演示版本，需要对接后端API：

```typescript
// 示例：在 src/api 目录下创建接口文件
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 标注相关接口
export const annotationApi = {
  loadImage: (id: number) => api.get(`/annotation/image/${id}`),
  saveAnnotation: (data: any) => api.post('/annotation/save', data),
  exportGeoJSON: (id: number) => api.get(`/annotation/export/${id}`)
}

// 训练相关接口
export const trainingApi = {
  startTraining: (config: any) => api.post('/training/start', config),
  getProgress: (taskId: number) => api.get(`/training/progress/${taskId}`),
  getHistory: () => api.get('/training/history')
}

// 预测相关接口
export const predictionApi = {
  predict: (formData: FormData) => api.post('/prediction/predict', formData),
  getResult: (id: number) => api.get(`/prediction/result/${id}`)
}
```

## 🚀 后续优化建议

1. **性能优化**
   - 虚拟滚动处理大量图像列表
   - Web Worker 处理图像预处理
   - Canvas 分层渲染优化

2. **功能增强**
   - WebSocket 实时推送训练进度
   - 协作标注功能
   - 标注版本管理
   - 批注评论系统

3. **用户体验**
   - 快捷键支持
   - 操作撤销/重做
   - 标注模板保存
   - 批量操作优化

4. **数据可视化**
   - 更多统计图表
   - 3D可视化支持
   - 热力图展示

## 📝 开发规范

- 使用 TypeScript 严格模式
- 组件采用 `<script setup>` 语法
- 遵循 Element Plus 设计规范
- 统一的错误处理和用户提示
- 响应式数据使用 `ref` 和 `reactive`

## 📄 License

MIT License

<template>
  <div class="training-container">
    <el-row :gutter="20">
      <!-- 左侧配置面板 -->
      <el-col :span="8">
        <el-card class="config-panel">
          <template #header>
            <span>训练配置</span>
          </template>
          
          <el-form :model="config" label-width="100px" size="default">
            <el-form-item label="数据集">
              <el-select v-model="config.dataset" placeholder="选择数据集" style="width: 100%">
                <el-option
                  v-for="ds in datasets"
                  :key="ds.id"
                  :label="ds.name"
                  :value="ds.id"
                >
                  <span>{{ ds.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">
                    {{ ds.images }} 张
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="模型架构">
              <el-select v-model="config.model" placeholder="选择模型" style="width: 100%">
                <el-option label="YOLOv7" value="yolov7" />
                <el-option label="YOLOv7-tiny" value="yolov7-tiny" />
                <el-option label="YOLOv8" value="yolov8" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="预训练权重">
              <el-upload
                action="#"
                :auto-upload="false"
                :limit="1"
              >
                <el-button type="primary" plain>
                  <el-icon><Upload /></el-icon>
                  选择文件
                </el-button>
              </el-upload>
            </el-form-item>
            
            <el-divider content-position="left">超参数配置</el-divider>
            
            <el-form-item label="Epochs">
              <el-slider v-model="config.epochs" :min="10" :max="500" :step="10" show-input />
            </el-form-item>
            
            <el-form-item label="Batch Size">
              <el-input-number v-model="config.batchSize" :min="1" :max="64" />
            </el-form-item>
            
            <el-form-item label="学习率">
              <el-input-number v-model="config.learningRate" :min="0.0001" :max="0.1" :step="0.0001" :precision="4" />
            </el-form-item>
            
            <el-form-item label="图像尺寸">
              <el-input-number v-model="config.imgSize" :min="320" :max="1280" :step="32" />
            </el-form-item>
            
            <el-divider content-position="left">数据增强</el-divider>
            
            <el-form-item>
              <el-checkbox v-model="config.augmentation.mosaic">Mosaic</el-checkbox>
              <el-checkbox v-model="config.augmentation.mixup">MixUp</el-checkbox>
              <el-checkbox v-model="config.augmentation.hsv">HSV增强</el-checkbox>
              <el-checkbox v-model="config.augmentation.flip">随机翻转</el-checkbox>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="startTraining" :loading="isTraining" style="width: 100%">
                <el-icon v-if="!isTraining"><VideoPlay /></el-icon>
                {{ isTraining ? '训练中...' : '开始训练' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧监控面板 -->
      <el-col :span="16">
        <el-card class="monitor-panel">
          <template #header>
            <div class="panel-header">
              <span>训练监控</span>
              <el-tag v-if="isTraining" type="success" effect="dark">
                <el-icon class="is-loading"><Loading /></el-icon>
                训练中 - Epoch {{ currentEpoch }}/{{ config.epochs }}
              </el-tag>
              <el-tag v-else type="info">未开始</el-tag>
            </div>
          </template>
          
          <!-- 实时指标 -->
          <el-row :gutter="16" class="metrics-row">
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">mAP@0.5</div>
                <div class="metric-value">{{ metrics.mAP.toFixed(3) }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Precision</div>
                <div class="metric-value">{{ metrics.precision.toFixed(3) }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Recall</div>
                <div class="metric-value">{{ metrics.recall.toFixed(3) }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Loss</div>
                <div class="metric-value">{{ metrics.loss.toFixed(4) }}</div>
              </div>
            </el-col>
          </el-row>
          
          <!-- 训练曲线图 -->
          <div class="charts-container">
            <v-chart class="chart" :option="lossChartOption" autoresize />
            <v-chart class="chart" :option="metricsChartOption" autoresize />
          </div>
          
          <!-- 训练日志 -->
          <div class="log-container">
            <div class="log-header">
              <span>训练日志</span>
              <el-button size="small" text @click="clearLogs">
                <el-icon><Delete /></el-icon>
                清空
              </el-button>
            </div>
            <div ref="logContainer" class="log-content">
              <div v-for="(log, index) in logs" :key="index" class="log-line">
                <span class="log-time">{{ log.time }}</span>
                <span :class="`log-${log.level}`">{{ log.message }}</span>
              </div>
            </div>
          </div>
        </el-card>
        
        <!-- 历史训练记录 -->
        <el-card class="history-panel" style="margin-top: 20px">
          <template #header>
            <span>历史训练记录</span>
          </template>
          
          <el-table :data="trainingHistory" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="dataset" label="数据集" width="150" />
            <el-table-column prop="model" label="模型" width="120" />
            <el-table-column prop="epochs" label="Epochs" width="80" />
            <el-table-column prop="mAP" label="mAP@0.5" width="100" />
            <el-table-column prop="duration" label="耗时" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">
                  {{ row.status === 'completed' ? '已完成' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" type="primary" text>查看</el-button>
                <el-button size="small" type="success" text>导出</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const isTraining = ref(false)
const currentEpoch = ref(0)
const logContainer = ref<HTMLElement>()

const config = reactive({
  dataset: '',
  model: 'yolov7',
  epochs: 100,
  batchSize: 16,
  learningRate: 0.01,
  imgSize: 640,
  augmentation: {
    mosaic: true,
    mixup: false,
    hsv: true,
    flip: true
  }
})

const datasets = ref([
  { id: 1, name: '病理切片数据集 v1', images: 1248 },
  { id: 2, name: 'WSI标注数据集 v2', images: 856 },
  { id: 3, name: '细胞检测数据集', images: 2340 }
])

const metrics = reactive({
  mAP: 0,
  precision: 0,
  recall: 0,
  loss: 0
})

const logs = ref<any[]>([])

const trainingHistory = ref([
  { id: 12, dataset: '病理切片 v1', model: 'YOLOv7', epochs: 100, mAP: 0.923, duration: '2h 15m', status: 'completed' },
  { id: 11, dataset: 'WSI标注 v2', model: 'YOLOv7-tiny', epochs: 50, mAP: 0.856, duration: '45m', status: 'completed' },
  { id: 10, dataset: '细胞检测', model: 'YOLOv8', epochs: 150, mAP: 0.945, duration: '3h 30m', status: 'completed' }
])

const lossChartOption = computed(() => ({
  title: { text: '损失函数曲线', left: 'center' },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: Array.from({ length: currentEpoch.value }, (_, i) => `Epoch ${i + 1}`) },
  yAxis: { type: 'value', name: 'Loss' },
  series: [{
    data: Array.from({ length: currentEpoch.value }, () => Math.random() * 0.5),
    type: 'line',
    smooth: true,
    itemStyle: { color: '#F56C6C' },
    areaStyle: { opacity: 0.3 }
  }]
}))

const metricsChartOption = computed(() => ({
  title: { text: '评估指标', left: 'center' },
  tooltip: { trigger: 'axis' },
  legend: { data: ['mAP', 'Precision', 'Recall'], bottom: 0 },
  xAxis: { type: 'category', data: Array.from({ length: currentEpoch.value }, (_, i) => `Epoch ${i + 1}`) },
  yAxis: { type: 'value', min: 0, max: 1 },
  series: [
    { name: 'mAP', data: Array.from({ length: currentEpoch.value }, () => Math.random() * 0.3 + 0.6), type: 'line', smooth: true },
    { name: 'Precision', data: Array.from({ length: currentEpoch.value }, () => Math.random() * 0.3 + 0.6), type: 'line', smooth: true },
    { name: 'Recall', data: Array.from({ length: currentEpoch.value }, () => Math.random() * 0.3 + 0.6), type: 'line', smooth: true }
  ]
}))

const startTraining = () => {
  if (!config.dataset) {
    ElMessage.warning('请先选择数据集')
    return
  }
  
  isTraining.value = true
  currentEpoch.value = 0
  
  addLog('info', '训练任务已启动')
  addLog('info', `数据集: ${datasets.value.find(d => d.id === config.dataset)?.name}`)
  addLog('info', `模型: ${config.model}, Epochs: ${config.epochs}, Batch Size: ${config.batchSize}`)
  
  // 模拟训练过程
  const interval = setInterval(() => {
    if (currentEpoch.value >= config.epochs) {
      clearInterval(interval)
      isTraining.value = false
      addLog('success', '训练完成!')
      ElMessage.success('模型训练完成')
      return
    }
    
    currentEpoch.value++
    metrics.mAP = Math.min(metrics.mAP + Math.random() * 0.01, 0.95)
    metrics.precision = Math.min(metrics.precision + Math.random() * 0.01, 0.95)
    metrics.recall = Math.min(metrics.recall + Math.random() * 0.01, 0.95)
    metrics.loss = Math.max(metrics.loss - Math.random() * 0.01, 0.1)
    
    addLog('info', `Epoch ${currentEpoch.value}/${config.epochs} - Loss: ${metrics.loss.toFixed(4)}, mAP: ${metrics.mAP.toFixed(3)}`)
  }, 1000)
}

const addLog = (level: string, message: string) => {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  logs.value.push({ time, level, message })
  
  // 自动滚动到底部
  setTimeout(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  }, 0)
}

const clearLogs = () => {
  logs.value = []
}
</script>

<style scoped lang="scss">
.training-container {
  .config-panel, .monitor-panel, .history-panel {
    border-radius: 8px;
  }
  
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
  
  .metrics-row {
    margin-bottom: 20px;
    
    .metric-card {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
      border-radius: 8px;
      color: white;
      text-align: center;
      
      .metric-label {
        font-size: 14px;
        opacity: 0.9;
        margin-bottom: 8px;
      }
      
      .metric-value {
        font-size: 28px;
        font-weight: 600;
      }
    }
  }
  
  .charts-container {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    margin-bottom: 20px;
    
    .chart {
      height: 300px;
      background: #f9fafc;
      border-radius: 8px;
      padding: 16px;
    }
  }
  
  .log-container {
    background: #1e1e1e;
    border-radius: 8px;
    overflow: hidden;
    
    .log-header {
      padding: 12px 16px;
      background: #2d2d2d;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: #fff;
      font-size: 14px;
    }
    
    .log-content {
      height: 250px;
      overflow-y: auto;
      padding: 12px 16px;
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
      
      .log-line {
        margin-bottom: 4px;
        line-height: 1.6;
        
        .log-time {
          color: #858585;
          margin-right: 12px;
        }
        
        .log-info { color: #4fc3f7; }
        .log-success { color: #66bb6a; }
        .log-warning { color: #ffa726; }
        .log-error { color: #ef5350; }
      }
    }
  }
}
</style>

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="32"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>类别分布统计</span>
            </div>
          </template>
          <v-chart class="chart" :option="classDistributionOption" autoresize />
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>训练进度</span>
            </div>
          </template>
          <v-chart class="chart" :option="trainingProgressOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
              <el-button type="primary" size="small" text>查看全部</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="activity in recentActivities"
              :key="activity.id"
              :timestamp="activity.time"
              :type="activity.type"
            >
              {{ activity.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent])

const stats = ref([
  { title: '标注图像', value: 1248, icon: 'Picture', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { title: '训练模型', value: 12, icon: 'Cpu', color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { title: '预测结果', value: 3567, icon: 'Aim', color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { title: '数据集', value: 8, icon: 'Folder', color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' }
])

const classDistributionOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%', left: 'center' },
  series: [
    {
      name: '类别分布',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
      data: [
        { value: 450, name: '正常组织' },
        { value: 320, name: '癌变区域' },
        { value: 280, name: '炎症区域' },
        { value: 198, name: '坏死区域' }
      ]
    }
  ]
})

const trainingProgressOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['Epoch 1', 'Epoch 2', 'Epoch 3', 'Epoch 4', 'Epoch 5'] },
  yAxis: { type: 'value' },
  series: [
    {
      name: '准确率',
      data: [0.65, 0.72, 0.81, 0.87, 0.92],
      type: 'line',
      smooth: true,
      itemStyle: { color: '#409EFF' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ]
        }
      }
    }
  ]
})

const recentActivities = ref([
  { id: 1, content: '完成批次 C001 的自动标注，共 248 张图像', time: '2024-01-15 14:30', type: 'success' },
  { id: 2, content: '启动 YOLOv7 模型训练任务 #12', time: '2024-01-15 10:20', type: 'primary' },
  { id: 3, content: '导入新的 SVS 切片数据 185 张', time: '2024-01-14 16:45', type: 'warning' },
  { id: 4, content: '完成病理切片 WSI_2024_001 的智能预测', time: '2024-01-14 09:15', type: 'info' }
])
</script>

<style scoped lang="scss">
.dashboard {
  .stats-row {
    margin-bottom: 24px;
    
    .stat-card {
      border-radius: 8px;
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      
      :deep(.el-card__body) {
        padding: 20px;
      }
      
      .stat-content {
        display: flex;
        align-items: center;
        gap: 16px;
        
        .stat-icon {
          width: 64px;
          height: 64px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
        }
        
        .stat-info {
          flex: 1;
          
          .stat-value {
            font-size: 28px;
            font-weight: 600;
            color: #303133;
            margin-bottom: 4px;
          }
          
          .stat-title {
            font-size: 14px;
            color: #909399;
          }
        }
      }
    }
  }
  
  .chart-row {
    margin-bottom: 24px;
    
    .chart-card {
      border-radius: 8px;
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 600;
        color: #303133;
      }
      
      .chart {
        height: 350px;
      }
    }
  }
}
</style>

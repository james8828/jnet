/**
 * 任务进度监控组合式函数
 * 通过WebSocket实时获取任务进度更新
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'

export interface TaskProgressData {
  taskId: number
  taskNo: string
  progress: number
  currentStep: string
  stepDetail?: any
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  updateTime: string
  elapsedSeconds?: number
  estimatedRemaining?: number
}

export function useTaskProgress(taskId: number | null) {
  const progress = ref(0)
  const currentStep = ref('')
  const stepDetail = ref<any>(null)
  const status = ref<'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'>('PENDING')
  const wsStatus = ref<'CONNECTING' | 'OPEN' | 'CLOSING' | 'CLOSED'>('CLOSED')
  const lastUpdateTime = ref<string>('')
  const elapsedSeconds = ref(0)
  const estimatedRemaining = ref(0)
  
  let ws: WebSocket | null = null
  let reconnectTimer: number | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 5
  const reconnectDelay = 1000 // 初始重连延迟1秒

  const connect = () => {
    if (!taskId) return
    
    // 关闭已有连接
    if (ws) {
      ws.close()
    }
    
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/task/progress/${taskId}`
    
    console.log(`[TaskProgress] 连接WebSocket: ${wsUrl}`)
    
    try {
      ws = new WebSocket(wsUrl)
      
      ws.onopen = () => {
        console.log('[TaskProgress] WebSocket连接成功')
        wsStatus.value = 'OPEN'
        reconnectAttempts = 0
      }
      
      ws.onmessage = (event) => {
        try {
          const data: TaskProgressData = JSON.parse(event.data)
          
          console.log('[TaskProgress] 收到进度更新:', data)
          
          progress.value = data.progress
          currentStep.value = data.currentStep
          stepDetail.value = data.stepDetail
          status.value = data.status
          lastUpdateTime.value = data.updateTime
          elapsedSeconds.value = data.elapsedSeconds || 0
          estimatedRemaining.value = data.estimatedRemaining || 0
          
          // 如果任务已完成或失败，关闭连接
          if (data.status === 'SUCCESS' || data.status === 'FAILED' || data.status === 'CANCELLED') {
            console.log(`[TaskProgress] 任务状态为 ${data.status}，关闭连接`)
            setTimeout(() => {
              close()
            }, 2000)
          }
        } catch (error) {
          console.error('[TaskProgress] 解析消息失败:', error)
        }
      }
      
      ws.onerror = (error) => {
        console.error('[TaskProgress] WebSocket错误:', error)
      }
      
      ws.onclose = (event) => {
        console.log(`[TaskProgress] WebSocket连接关闭: code=${event.code}, reason=${event.reason}`)
        wsStatus.value = 'CLOSED'
        
        // 如果任务还未完成，尝试重连
        if (status.value === 'RUNNING' && reconnectAttempts < maxReconnectAttempts) {
          attemptReconnect()
        }
      }
    } catch (error) {
      console.error('[TaskProgress] 创建WebSocket连接失败:', error)
      attemptReconnect()
    }
  }
  
  const attemptReconnect = () => {
    reconnectAttempts++
    const delay = reconnectDelay * Math.pow(2, reconnectAttempts - 1) // 指数退避
    
    console.log(`[TaskProgress] ${delay}ms后尝试第${reconnectAttempts}次重连...`)
    
    reconnectTimer = window.setTimeout(() => {
      connect()
    }, delay)
  }
  
  const close = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    
    if (ws) {
      ws.close()
      ws = null
    }
    
    wsStatus.value = 'CLOSED'
  }
  
  // 监听taskId变化，重新连接
  watch(() => taskId, (newTaskId) => {
    if (newTaskId) {
      // 重置状态
      progress.value = 0
      currentStep.value = ''
      stepDetail.value = null
      status.value = 'PENDING'
      reconnectAttempts = 0
      
      connect()
    } else {
      close()
    }
  })
  
  onMounted(() => {
    if (taskId) {
      connect()
    }
  })
  
  onUnmounted(() => {
    close()
  })
  
  return {
    progress,
    currentStep,
    stepDetail,
    status,
    wsStatus,
    lastUpdateTime,
    elapsedSeconds,
    estimatedRemaining,
    reconnect: connect,
    close
  }
}

/**
 * 批量任务进度监控
 * 同时监控多个任务的进度
 */
export function useBatchTaskProgress(taskIds: number[]) {
  const tasks = ref<Map<number, TaskProgressData>>(new Map())
  
  const updateTask = (taskId: number, data: Partial<TaskProgressData>) => {
    const existing = tasks.value.get(taskId) || {
      taskId,
      taskNo: '',
      progress: 0,
      currentStep: '',
      status: 'PENDING',
      updateTime: new Date().toISOString()
    }
    
    tasks.value.set(taskId, { ...existing, ...data })
  }
  
  const removeTask = (taskId: number) => {
    tasks.value.delete(taskId)
  }
  
  const clearTasks = () => {
    tasks.value.clear()
  }
  
  return {
    tasks,
    updateTask,
    removeTask,
    clearTasks
  }
}

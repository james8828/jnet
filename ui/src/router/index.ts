import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Monitor' }
      },
      {
        path: 'annotation',
        name: 'Annotation',
        component: () => import('@/views/Annotation.vue'),
        meta: { title: '图像标注', icon: 'Edit' }
      },
      {
        path: 'training',
        name: 'Training',
        component: () => import('@/views/Training.vue'),
        meta: { title: '模型训练', icon: 'Cpu' }
      },
      {
        path: 'prediction',
        name: 'Prediction',
        component: () => import('@/views/Prediction.vue'),
        meta: { title: '智能预测', icon: 'Aim' }
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('@/views/Projects.vue'),
        meta: { title: '项目管理', icon: 'Briefcase' }
      },
      {
        path: 'dataset',
        name: 'Dataset',
        component: () => import('@/views/Dataset.vue'),
        meta: { title: '数据管理', icon: 'Folder', keepAlive: true }
      }
    ]
  },
  {
    path: '/viewer/:id',
    name: 'Viewer',
    component: () => import('@/views/Viewer.vue'),
    meta: { title: '图像查看器', hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

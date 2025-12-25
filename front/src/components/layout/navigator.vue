<script setup lang="ts">

import {Setting,List} from "@element-plus/icons-vue";
import { useRouter } from "vue-router"
const router = useRouter()

const navigate = (path: string) => {
  router.push({ path: path });
}
</script>

<template>
  <el-scrollbar>
    <el-menu :default-openeds="['1']">
      <el-sub-menu index="1">
        <template #title>
          <el-icon><List /></el-icon> slide manage
        </template>
        <el-menu-item index="1-1" @click="navigate('/viewer')">viewer</el-menu-item>
        <el-menu-item index="1-2" @click="navigate('/slide')">slide info</el-menu-item>
      </el-sub-menu>
      <el-sub-menu index="2">
        <template #title>
          <el-icon><setting /></el-icon>system setting
        </template>
        <el-menu-item-group>
          <el-menu-item index="2-1" @click="navigate('/user')">用户</el-menu-item>
          <el-menu-item index="2-2" @click="navigate('/role')">角色</el-menu-item>
          <el-menu-item index="2-3" @click="navigate('/menu')">菜单</el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>
    </el-menu>
  </el-scrollbar>
</template>
<style scoped>
.el-scrollbar {
  height: 100%;
  padding-right: 10px; /* 可选：增加右侧内边距以留出竖线空间 */
}

.el-menu {
  border-right: none; /* 移除 Element Plus 默认的右边框 */
}

/* 添加竖线分割 */
.el-scrollbar::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background-color: #e4e7ed; /* 竖线颜色 */
}
</style>

<script setup lang="ts">

import {Setting,MapLocation,List} from "@element-plus/icons-vue";
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
        <el-menu-item index="1-1" @click="navigate('/viewer')">slide list</el-menu-item>
        <el-menu-item index="1-2" @click="navigate('/grid')">table</el-menu-item>
        <el-menu-item index="1-3" @click="navigate('/slide')">grid</el-menu-item>
      </el-sub-menu>
      <el-sub-menu index="2">
        <template #title>
          <el-icon><MapLocation /></el-icon>map
        </template>
        <el-menu-item-group>
          <template #title>Group 1</template>
          <el-menu-item index="2-1">Option 1</el-menu-item>
          <el-menu-item index="2-2">Option 2</el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="Group 2">
          <el-menu-item index="2-3">Option 3</el-menu-item>
        </el-menu-item-group>
        <el-sub-menu index="2-4">
          <template #title>Option 4</template>
          <el-menu-item index="2-4-1">Option 4-1</el-menu-item>
        </el-sub-menu>
      </el-sub-menu>
      <el-sub-menu index="3">
        <template #title>
          <el-icon><setting /></el-icon>system control
        </template>
        <el-menu-item-group>
          <template #title>Group 1</template>
          <el-menu-item index="3-1">Option 1</el-menu-item>
          <el-menu-item index="3-2">Option 2</el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="Group 2">
          <el-menu-item index="3-3">Option 3</el-menu-item>
        </el-menu-item-group>
        <el-sub-menu index="3-4">
          <template #title>Option 4</template>
          <el-menu-item index="3-4-1">Option 4-1</el-menu-item>
        </el-sub-menu>
      </el-sub-menu>
    </el-menu>
  </el-scrollbar>
</template>

<style scoped>

</style>
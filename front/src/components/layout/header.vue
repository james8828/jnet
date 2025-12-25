<template>
  <div class="toolbar">
    <el-dropdown>
      <el-icon style="margin-right: 8px; margin-top: 1px">
        <setting />
      </el-icon>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item>info</el-dropdown-item>
          <el-dropdown-item @click="logout('/')">退出</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <span>Tom</span>
  </div>
</template>

<script setup lang="ts">

import {Setting} from "@element-plus/icons-vue";
import { useRouter } from "vue-router"
const router = useRouter()

const logout = (path: string) => {
  router.push({ path: path });
}
</script>
<style scoped>

</style>
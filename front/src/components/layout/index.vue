<template>
  <el-container class="layout-container-demo" style="height: 100vh;" :class="{ 'no-aside': !isAsideVisible }">
    <el-aside v-if="isAsideVisible" width="200px">
      <navigator />
    </el-aside>

    <el-container>
      <el-header style="text-align: right; font-size: 12px">
        <Header />
        <el-button
            style="position: absolute; left: 10px; top: 8px;"
            @click="toggleAside"
            :icon="isAsideVisible ? ArrowLeftBold : ArrowRightBold"
            circle
            size="small"
        />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>



<script lang="ts" setup>
import Navigator from "@comps/layout/navigator.vue"
import Header from "@comps/layout/header.vue"
import { ref } from 'vue'
import { ArrowLeftBold, ArrowRightBold } from '@element-plus/icons-vue'

const isAsideVisible = ref(true)

const toggleAside = () => {
  isAsideVisible.value = !isAsideVisible.value
}
</script>


<style scoped>

.layout-container-demo .el-header {
  position: relative;
  color: var(--el-text-color-primary);
}

.layout-container-demo .el-aside {
  color: var(--el-text-color-primary);
}

.layout-container-demo .el-menu {
  border-right: none;
}

.layout-container-demo.no-aside .el-main {
  margin-left: 0 !important;
}

.layout-container-demo .el-main {
  padding: 0;
  height: calc(100vh - 60px);
  overflow: auto;

  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

/* Webkit 浏览器（Chrome、Edge、Safari）滚动条隐藏 */
.layout-container-demo .el-main::-webkit-scrollbar {
  display: none;
}

.layout-container-demo .toolbar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  right: 20px;
}

</style>

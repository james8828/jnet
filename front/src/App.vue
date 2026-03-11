<script setup lang="ts">
import HelloWorld from './components/HelloWorld.vue'
import Layout from '@/components/layout'
/*
import Upload from './components/Upload.vue'
*/
</script>

<template>
<!--  <HelloWorld msg="view" />-->
  <router-view></router-view>
<!--  <Upload/>-->
</template>

<!--<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>-->

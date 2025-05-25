<template>
  <div id="app">
    <Navigation v-if="showNavigation" />
    <div :class="{ 'page-content': showNavigation }">
      <router-view/>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Navigation from './components/Navigation.vue'

export default {
  name: 'App',
  components: {
    Navigation
  },
  setup() {
    const route = useRoute()

    // 在登录页面不显示导航栏
    const showNavigation = computed(() => {
      return route.path !== '/login'
    })

    return {
      showNavigation
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

.page-content {
  margin-top: 60px;
}

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
}
</style>
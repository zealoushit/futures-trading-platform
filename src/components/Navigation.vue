<template>
  <div class="navigation">
    <el-menu
      :default-active="activeIndex"
      mode="horizontal"
      @select="handleSelect"
      background-color="#545c64"
      text-color="#fff"
      active-text-color="#ffd04b"
    >
      <div class="nav-brand">
        <h3>期货交易平台</h3>
      </div>

      <div class="nav-menu">
        <el-menu-item index="/market">
          <el-icon><TrendCharts /></el-icon>
          <span>行情中心</span>
        </el-menu-item>

        <el-menu-item index="/trade">
          <el-icon><Money /></el-icon>
          <span>交易中心</span>
        </el-menu-item>
      </div>

      <div class="nav-user">
        <el-dropdown v-if="isLoggedIn" @command="handleCommand">
          <span class="el-dropdown-link">
            <el-icon><User /></el-icon>
            {{ user.name }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button v-else type="primary" @click="goToLogin">
          <el-icon><User /></el-icon>
          登录
        </el-button>
      </div>
    </el-menu>
  </div>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import {
  TrendCharts,
  Money,
  User,
  ArrowDown
} from '@element-plus/icons-vue'

export default {
  name: 'AppNavigation',
  components: {
    TrendCharts,
    Money,
    User,
    ArrowDown
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()

    const activeIndex = ref(route.path)

    // 监听路由变化
    watch(() => route.path, (newPath) => {
      activeIndex.value = newPath
    })

    // 从store获取用户信息
    const user = computed(() => store.state.user)
    const isLoggedIn = computed(() => store.getters.isLoggedIn)

    // 处理菜单选择
    const handleSelect = (index) => {
      if (!isLoggedIn.value && (index === '/market' || index === '/trade')) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }

      router.push(index)
    }

    // 处理用户下拉菜单
    const handleCommand = (command) => {
      switch (command) {
        case 'profile':
          ElMessage.info('个人资料功能开发中')
          break
        case 'settings':
          ElMessage.info('设置功能开发中')
          break
        case 'logout':
          store.dispatch('logout')
          ElMessage.success('已退出登录')
          router.push('/login')
          break
      }
    }

    // 跳转到登录页
    const goToLogin = () => {
      router.push('/login')
    }

    return {
      activeIndex,
      user,
      isLoggedIn,
      handleSelect,
      handleCommand,
      goToLogin
    }
  }
}
</script>

<style scoped>
.navigation {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.el-menu {
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.nav-brand {
  margin-right: auto;
}

.nav-brand h3 {
  color: #fff;
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.nav-menu {
  display: flex;
  flex: 1;
  justify-content: center;
}

.nav-user {
  margin-left: auto;
}

.el-dropdown-link {
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 4px;
}

.el-dropdown-link:hover {
  color: #ffd04b;
}

/* 调整菜单项样式 */
.el-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.el-menu-item .el-icon {
  font-size: 16px;
}

/* 为页面内容留出导航栏空间 */
:global(.page-content) {
  margin-top: 60px;
}
</style>

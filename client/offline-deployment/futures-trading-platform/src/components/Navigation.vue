<template>
  <div class="navigation">
    <el-menu
      :default-active="activeIndex"
      mode="horizontal"
      @select="handleSelect"
      background-color="#545c64"
      text-color="#fff"
      active-text-color="#ffd04b"
      class="main-menu"
    >
      <!-- 行情中心 -->
      <el-menu-item index="/market">
        <el-icon><TrendCharts /></el-icon>
        <span>行情中心</span>
      </el-menu-item>

      <!-- 交易操作 -->
      <el-menu-item index="/trade">
        <el-icon><Money /></el-icon>
        <span>交易操作</span>
      </el-menu-item>

      <!-- 交易数据查询子菜单 -->
      <el-sub-menu index="trade-data">
        <template #title>
          <el-icon><Document /></el-icon>
          <span>交易数据查询</span>
        </template>
        <el-menu-item index="/query/positions">查询持仓</el-menu-item>
        <el-menu-item index="/query/funds">查询资金</el-menu-item>
        <el-menu-item index="/query/orders">查询委托</el-menu-item>
      </el-sub-menu>

      <!-- 基础数据查询子菜单 -->
      <el-sub-menu index="basic-data">
        <template #title>
          <el-icon><Search /></el-icon>
          <span>基础数据查询</span>
        </template>
        <el-menu-item index="/query/investor-info">查询投资者信息</el-menu-item>
        <el-menu-item index="/query/investor-funds">查询投资者资金</el-menu-item>
      </el-sub-menu>

      <!-- 用户区域 -->
      <div class="nav-user">
        <el-dropdown
          v-if="isLoggedIn"
          @command="handleCommand"
          placement="bottom-end"
          :show-timeout="100"
          :hide-timeout="200"
          trigger="hover"
          :teleported="false"
          popper-class="user-dropdown-popper"
        >
          <span class="el-dropdown-link">
            <el-icon><User /></el-icon>
            {{ user.name }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="user-dropdown-menu">
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人资料
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>
                设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
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
  ArrowDown,
  Document,
  Search,
  Setting,
  SwitchButton
} from '@element-plus/icons-vue'

export default {
  name: 'AppNavigation',
  components: {
    TrendCharts,
    Money,
    User,
    ArrowDown,
    Document,
    Search,
    Setting,
    SwitchButton
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
      // 检查是否需要登录
      const protectedRoutes = [
        '/market',
        '/trade',
        '/query/trade-data',
        '/query/basic-data',
        '/query/positions',
        '/query/funds',
        '/query/orders',
        '/query/investor-info',
        '/query/investor-funds'
      ]

      if (!isLoggedIn.value && protectedRoutes.includes(index)) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }

      // 处理查询页面路由
      if (index.startsWith('/query/')) {
        // 暂时显示开发中提示，后续可以创建对应的页面
        const routeNames = {
          '/query/trade-data': '交易数据查询',
          '/query/basic-data': '基础数据查询',
          '/query/positions': '查询持仓',
          '/query/funds': '查询资金',
          '/query/orders': '查询委托',
          '/query/investor-info': '查询投资者信息',
          '/query/investor-funds': '查询投资者资金'
        }
        ElMessage.info(`${routeNames[index]}功能开发中`)
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
          store.dispatch('logout').then(() => {
            ElMessage.success('已退出登录')
            router.push('/login')
          })
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

.main-menu {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  justify-content: space-between;
}

.nav-user {
  margin-left: auto;
  position: relative;
  display: flex;
  align-items: center;
  height: 60px;
  z-index: 1001;
}

.el-dropdown-link {
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.el-dropdown-link:hover {
  color: #ffd04b;
  background-color: rgba(255, 255, 255, 0.1);
}

/* 调整菜单项样式 */
.el-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
  margin: 0;
  border-bottom: none;
}

.el-menu-item .el-icon {
  font-size: 16px;
}

/* 移除子菜单样式，因为现在都是平铺菜单 */

/* 为页面内容留出导航栏空间 */
:global(.page-content) {
  margin-top: 60px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .nav-menu {
    gap: 0;
  }

  .el-menu-item {
    padding: 0 15px;
  }
}

@media (max-width: 992px) {
  .main-menu {
    padding: 0 15px;
  }

  .el-menu-item {
    padding: 0 10px;
    font-size: 14px;
  }

  .el-menu-item .el-icon {
    font-size: 14px;
  }
}
</style>

<!-- 全局样式，修复用户下拉菜单显示问题 -->
<style>
/* 确保下拉菜单正常显示 */
.el-dropdown-menu {
  min-width: 150px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 3000 !important;
}

.user-dropdown-menu {
  min-width: 160px;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  border: 1px solid #e4e7ed;
  background: #fff;
  padding: 4px 0;
}

.el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.2s;
}

.el-dropdown-menu__item .el-icon {
  font-size: 16px;
}

.el-dropdown-menu__item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

/* 确保导航栏在正确的层级 */
.navigation {
  z-index: 1000;
}

/* 用户下拉菜单特殊样式 */
.user-dropdown-popper {
  z-index: 3000 !important;
  position: absolute !important;
}

.user-dropdown-popper .el-popper__arrow {
  display: block !important;
}

/* 确保popper在正确位置 */
.el-popper {
  z-index: 3000 !important;
}

/* 子菜单样式 */
.el-sub-menu {
  position: relative;
}

.el-sub-menu .el-sub-menu__title {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.el-sub-menu .el-sub-menu__title:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ffd04b;
}

.el-sub-menu__popup {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e4e7ed;
  padding: 4px 0;
  min-width: 160px;
}

.el-sub-menu__popup .el-menu-item {
  height: 40px;
  line-height: 40px;
  padding: 0 16px;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}

.el-sub-menu__popup .el-menu-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

/* 菜单项响应式调整 */
@media (max-width: 1400px) {
  .el-menu-item span {
    font-size: 13px;
  }

  .el-menu-item {
    padding: 0 12px;
  }
}

@media (max-width: 1200px) {
  .el-menu-item span {
    font-size: 12px;
  }

  .el-menu-item {
    padding: 0 8px;
  }
}
</style>

<template>
  <div class="login-container">
    <div class="login-form">
      <h2>期货交易终端</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            size="large"
            style="width: 100%"
          >登录</el-button>
        </el-form-item>
      </el-form>

      <div class="login-tips">
        <p>演示账号: admin / 123456</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

export default {
  name: 'LoginView',
  components: {
    User,
    Lock
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)

    const loginForm = reactive({
      username: 'admin',
      password: '123456'
    })

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }

    const handleLogin = () => {
      loginFormRef.value.validate(async (valid) => {
        if (valid) {
          loading.value = true
          try {
            await store.dispatch('login', loginForm)
            router.push('/market')
            ElMessage.success('登录成功')
          } catch (error) {
            ElMessage.error('登录失败: ' + error.message)
          } finally {
            loading.value = false
          }
        }
      })
    }

    return {
      loginFormRef,
      loginForm,
      rules,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.login-form {
  width: 350px;
  padding: 35px;
  border-radius: 5px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
}

.login-form h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}

.login-tips {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}
</style>
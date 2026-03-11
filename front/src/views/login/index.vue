<template>
  <div class="login-container">
    <div class="login-card">
      <h2>欢迎登录</h2>
      <p>请输入账号密码以继续</p>

      <form>
        <el-form label-position="top" :model="loginForm" ref="formRef">

          <!-- 用户名 -->
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable>
              <template #prefix>
                <i class="el-icon-user"></i>
              </template>
            </el-input>
          </el-form-item>

          <!-- 密码 -->
          <el-form-item label="密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleSubmit"
            >
              <template #prefix>
                <i class="el-icon-lock"></i>
              </template>
            </el-input>
          </el-form-item>

          <!-- 提交按钮 -->
          <el-button type="primary" @click="handleSubmit" :loading="loading" style="width: 100%">
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>
      </form>
    </div>
  </div>
</template>


<script setup lang="ts">
import { ref } from 'vue'
import serviceAxios from '@utils/request'
import router from '@/router'
import { ElMessage } from 'element-plus'

const loginForm = ref({
  username: 'admin',
  password: '123456'
})

const loading = ref(false)

const handleSubmit = async () => {
  loading.value = true
  try {
    localStorage.clear()

    const res = await serviceAxios.post(
      process.env.AUTH_SERVICE_URL + '/oauth2/authorize',
      `grant_type=password&username=${loginForm.value.username}&password=${loginForm.value.password}&client_id=front-client`,
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    )

    if (res.code === 200) {
      ElMessage.success(res.msg)
      localStorage.setItem('access_token', res.data.access_token)
      localStorage.setItem('token_type', res.data.token_type)
      localStorage.setItem('expires_in', res.data.expires_in)
      router.push({ name: 'Home' })
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>


<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 20px;
}

.login-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  padding: 30px;
  width: 100%;
  max-width: 400px;
  box-sizing: border-box;
  animation: fadeInUp 0.5s ease-in-out;
}

.login-card h2 {
  margin-bottom: 10px;
  font-size: 24px;
  color: #333;
}

.login-card p {
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.el-form-item__label {
  font-weight: 500;
}

.el-button--primary {
  transition: all 0.3s ease;
}

.el-button--primary:hover {
  opacity: 0.9;
}

@keyframes fadeInUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>

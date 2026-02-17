<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const phone = ref('')

const loading = ref(false)

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度至少6位'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== password.value) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

async function onSubmit() {
  if (!username.value.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }
  if (password.value.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  loading.value = true
  try {
    await register({ username: username.value, password: password.value, phone: phone.value })
    ElMessage.success('注册成功！请登录')
    router.push({ name: 'login' })
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '注册失败'
    ElMessage.error(errMsg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <el-card class="registerCard" shadow="hover">
      <template #header>
        <div class="cardHeader">
          <el-icon class="headerIcon"><EditPen /></el-icon>
          <span class="title">创建账号</span>
        </div>
      </template>
      
      <el-form :model="{ username, password, confirmPassword, phone }" @submit.prevent="onSubmit">
        <el-form-item>
          <el-input
            v-model="username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        
        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入密码（至少6位）"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-input
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-input
            v-model="phone"
            placeholder="手机号（可选）"
            :prefix-icon="Phone"
            size="large"
            clearable
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submitBtn"
            @click="onSubmit"
          >
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="tips">
        已有账号？
        <router-link to="/login">立即登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 420px;
  margin: 40px auto 0;
  padding: 0 16px;
}

.registerCard {
  border-radius: 16px;
}

.cardHeader {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.headerIcon {
  font-size: 28px;
  color: var(--primary);
}

.title {
  font-size: 22px;
  font-weight: 700;
}

.submitBtn {
  width: 100%;
}

.tips {
  text-align: center;
  font-size: 14px;
  color: var(--muted);
}

.tips a {
  color: var(--primary);
  font-weight: 500;
}

.tips a:hover {
  text-decoration: underline;
}
</style>

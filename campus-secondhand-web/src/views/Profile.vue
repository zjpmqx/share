<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { me, updateAvatar, uploadFile } from '../services/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const okMsg = ref('')

const my = ref(null)

const file = ref(null)
const previewUrl = ref('')

function onPick(e) {
  const f = e?.target?.files?.[0]
  file.value = f || null
  okMsg.value = ''
  errorMsg.value = ''
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = f ? URL.createObjectURL(f) : ''
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await me()
    my.value = resp.data
  } catch (e) {
    my.value = null
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!file.value) {
    errorMsg.value = '请先选择头像图片'
    return
  }

  saving.value = true
  okMsg.value = ''
  errorMsg.value = ''
  try {
    const up = await uploadFile(file.value)
    const avatarUrl = up?.data
    if (!avatarUrl) throw new Error('上传失败：未返回头像 URL')

    await updateAvatar({ avatarUrl })
    okMsg.value = '头像已更新'
    ElMessage.success('头像更新成功')
    await load()

    file.value = null
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value)
      previewUrl.value = ''
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
onUnmounted(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<template>
  <div class="profilePage">
    <section class="hero">
      <div>
        <div class="heroTag">MY PROFILE</div>
        <h1>个人资料中心</h1>
        <p>维护你的头像和基本信息，让互动展示更清晰、更有辨识度。</p>
      </div>
      <el-button class="refreshBtn" :disabled="loading" @click="load">刷新资料</el-button>
    </section>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="okMsg" class="ok">{{ okMsg }}</div>

    <el-card v-if="loading" class="card" shadow="hover">
      <el-skeleton :rows="4" animated />
    </el-card>

    <el-card v-else class="card" shadow="hover">
      <div class="topRow">
        <div class="avatarWrap">
          <img v-if="previewUrl || my?.avatarUrl" :src="previewUrl || my?.avatarUrl" alt="" />
          <div v-else class="avatarFallback">
            <el-icon><User /></el-icon>
          </div>
        </div>

        <div class="info">
          <div class="name">{{ my?.username || '-' }}</div>
          <div class="meta">角色：{{ my?.role || '-' }}</div>
          <div class="meta">手机号：{{ my?.phone || '未填写' }}</div>
        </div>
      </div>

      <el-divider />

      <div class="uploadArea">
        <div class="sectionTitle">更新头像</div>
        <div class="uploader">
          <input class="file" type="file" accept="image/*" @change="onPick" />
          <el-button type="primary" :loading="saving" :disabled="saving" @click="save">
            {{ saving ? '保存中...' : '保存头像' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.profilePage {
  max-width: 860px;
  margin: 0 auto;
}

.hero {
  margin: 4px 0 14px;
  border-radius: 18px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #7c3aed, #4f46e5 58%, #2563eb);
  color: #eef2ff;
  box-shadow: 0 14px 30px rgba(79, 70, 229, 0.26);
}

.heroTag {
  display: inline-flex;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.15);
}

.hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.2;
  color: #f8fafc;
}

.hero p {
  margin: 0;
  color: rgba(238, 242, 255, 0.92);
}

.refreshBtn {
  border-radius: 12px;
}

.card {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.24);
}

.topRow {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatarWrap {
  width: 86px;
  height: 86px;
  border-radius: 999px;
  overflow: hidden;
  flex: 0 0 auto;
  border: 2px solid rgba(99, 102, 241, 0.22);
  background: #f8fafc;
}

.avatarWrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatarFallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 26px;
  color: #6366f1;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.14), rgba(59, 130, 246, 0.12));
}

.info {
  min-width: 0;
}

.name {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.meta {
  margin-top: 6px;
  color: var(--muted);
  font-size: 14px;
}

.sectionTitle {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.uploader {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file {
  flex: 1;
}

.error {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.18);
  border-radius: 14px;
  color: var(--danger);
  font-size: 13px;
}

.ok {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.22);
  border-radius: 14px;
  color: rgba(22, 163, 74, 1);
  font-size: 13px;
}

@media (max-width: 760px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero h1 {
    font-size: 24px;
  }

  .uploader {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

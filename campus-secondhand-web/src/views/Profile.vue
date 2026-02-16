<script setup>
import { onMounted, ref } from 'vue'
import { me, updateAvatar, uploadFile } from '../services/api'

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
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div>
        <div class="title">个人资料</div>
        <div class="sub">上传头像后，评论/留言会显示你的头像与用户名。</div>
      </div>
      <button class="ghost" type="button" :disabled="loading" @click="load">刷新</button>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="okMsg" class="ok">{{ okMsg }}</div>

    <div v-if="loading" class="muted">加载中...</div>

    <div v-else class="card">
      <div class="row">
        <div class="avatar">
          <img v-if="previewUrl || my?.avatarUrl" :src="previewUrl || my?.avatarUrl" alt="" />
          <div v-else class="avatarFallback"></div>
        </div>

        <div class="info">
          <div class="name">{{ my?.username || '-' }}</div>
          <div class="meta">角色：{{ my?.role || '-' }}</div>
        </div>
      </div>

      <div class="uploader">
        <input class="file" type="file" accept="image/*" @change="onPick" />
        <button class="primary" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存头像' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 720px;
  margin: 0 auto;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin: 6px 0 14px;
}

.title {
  font-size: 20px;
  font-weight: 900;
}

.sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  padding: 14px;
  box-shadow: var(--shadow);
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid var(--border-2);
  background: var(--surface-2);
  flex: 0 0 auto;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatarFallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(34, 197, 94, 0.18));
}

.info {
  min-width: 0;
}

.name {
  font-weight: 900;
  font-size: 16px;
}

.meta {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.uploader {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 520px) {
  .uploader {
    flex-direction: column;
    align-items: stretch;
  }
}

.file {
  flex: 1;
}

.primary {
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.ghost {
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid var(--border-2);
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
}

.muted {
  color: var(--muted);
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
  background: rgba(34, 197, 94, 0.10);
  border: 1px solid rgba(34, 197, 94, 0.22);
  border-radius: 14px;
  color: rgba(22, 163, 74, 1);
  font-size: 13px;
}
</style>

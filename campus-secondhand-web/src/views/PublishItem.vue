<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createItem, uploadFile } from '../services/api'

const router = useRouter()

const title = ref('')
const description = ref('')
const category = ref('BOOK')
const price = ref(0)
const conditionLevel = ref('9')
const coverImageUrl = ref('')

const coverFile = ref(null)
const uploading = ref(false)
const uploadError = ref('')

const loading = ref(false)
const errorMsg = ref('')

async function submit() {
  errorMsg.value = ''
  loading.value = true
  try {
    const resp = await createItem({
      title: title.value,
      description: description.value,
      category: category.value,
      price: Number(price.value),
      conditionLevel: conditionLevel.value,
      coverImageUrl: coverImageUrl.value,
    })
    const id = resp?.data?.id
    if (id) {
      router.push(`/items/${id}`)
    } else {
      router.push('/')
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '发布失败'
  } finally {
    loading.value = false
  }
}

async function onSelectFile(e) {
  uploadError.value = ''
  const file = e?.target?.files?.[0]
  coverFile.value = file || null
}

async function doUpload() {
  if (!coverFile.value) return
  uploadError.value = ''
  uploading.value = true
  try {
    const resp = await uploadFile(coverFile.value)
    const url = resp?.data
    if (!url) {
      throw new Error('上传失败：未返回图片地址')
    }
    coverImageUrl.value = url
  } catch (e) {
    uploadError.value = e?.response?.data?.message || e?.message || '上传失败'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <h1 class="title">发布商品（提交后进入审核）</h1>

    <div class="card">
      <div class="field">
        <div class="label">标题</div>
        <input v-model="title" class="input" placeholder="例：高数教材 / 蓝牙耳机" />
      </div>

      <div class="field">
        <div class="label">描述</div>
        <textarea v-model="description" class="textarea" placeholder="补充说明、使用情况等"></textarea>
      </div>

      <div class="row">
        <div class="field">
          <div class="label">分类</div>
          <input v-model="category" class="input" placeholder="BOOK / DIGITAL / LIFE" />
        </div>
        <div class="field">
          <div class="label">成色</div>
          <input v-model="conditionLevel" class="input" placeholder="9 / 8 / NEW" />
        </div>
      </div>

      <div class="row">
        <div class="field">
          <div class="label">价格</div>
          <input v-model="price" type="number" class="input" placeholder="20" />
        </div>
        <div class="field">
          <div class="label">封面图URL（可选）</div>
          <input v-model="coverImageUrl" class="input" placeholder="https://..." />
        </div>
      </div>

      <div class="field">
        <div class="label">上传本地封面图（推荐）</div>
        <div class="uploadRow">
          <input class="file" type="file" accept="image/*" @change="onSelectFile" />
          <button class="ghost" type="button" :disabled="!coverFile || uploading" @click="doUpload">
            {{ uploading ? '上传中...' : '上传图片' }}
          </button>
        </div>
        <div v-if="uploadError" class="error">{{ uploadError }}</div>
        <div v-if="coverImageUrl" class="preview">
          <img :src="coverImageUrl" alt="" />
        </div>
        <div class="tips muted">上传成功后会自动填写封面图URL，并用于首页/详情页展示。</div>
      </div>

      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

      <button class="primary" type="button" :disabled="loading" @click="submit">
        {{ loading ? '提交中...' : '发布' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 800px;
  margin: 0 auto;
}

.title {
  margin: 10px 0 12px;
  font-size: 22px;
  font-weight: 800;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  padding: 14px;
  box-shadow: var(--shadow);
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 760px) {
  .row {
    grid-template-columns: 1fr;
  }
}

.field {
  margin-bottom: 12px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--surface);
}

.textarea {
  width: 100%;
  min-height: 120px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--surface);
}

.primary {
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.primary:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

.error {
  margin: 8px 0;
  color: var(--danger);
  font-size: 13px;
}

.uploadRow {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.file {
  flex: 1;
  min-width: 240px;
}

.ghost {
  background: var(--surface);
  border: 1px solid var(--border);
}

.preview {
  margin-top: 10px;
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface-2);
}

.preview img {
  width: 100%;
  height: 240px;
  object-fit: contain;
  display: block;
}
</style>

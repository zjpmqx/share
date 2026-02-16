<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createShare, uploadFile } from '../services/api'

const router = useRouter()

const title = ref('')
const content = ref('')

const coverFile = ref(null)
const coverUrl = ref('')
const uploadingCover = ref(false)
const coverError = ref('')
const coverInput = ref(null)

const mediaType = ref('NONE')
const mediaUrl = ref('')
const linkUrl = ref('')

const file = ref(null)
const uploading = ref(false)
const uploadError = ref('')
const uploadProgress = ref(0)

const MAX_UPLOAD_BYTES = 4294967296

const loading = ref(false)
const errorMsg = ref('')

const showUpload = computed(() => mediaType.value === 'IMAGE' || mediaType.value === 'VIDEO' || mediaType.value === 'FILE')
const showLink = computed(() => mediaType.value === 'URL')

const mediaNeedsUrl = computed(() => mediaType.value === 'IMAGE' || mediaType.value === 'VIDEO' || mediaType.value === 'FILE')

const canSubmit = computed(() => {
  if (!String(title.value || '').trim()) return false
  if (loading.value) return false
  if (uploading.value || uploadingCover.value) return false

  if (mediaType.value === 'URL') {
    return !!String(linkUrl.value || '').trim()
  }

  if (mediaNeedsUrl.value) {
    return !!String(mediaUrl.value || '').trim()
  }

  return true
})

const acceptValue = computed(() => {
  if (mediaType.value === 'IMAGE') return 'image/*'
  if (mediaType.value === 'VIDEO') return 'video/*'
  if (mediaType.value === 'FILE') return '.zip,.rar,.7z,.tar,.gz,.tgz'
  return ''
})

const uploadLabel = computed(() => {
  if (mediaType.value === 'IMAGE') return '上传文件（图片）'
  if (mediaType.value === 'VIDEO') return '上传文件（视频）'
  if (mediaType.value === 'FILE') return '上传文件（压缩包）'
  return '上传文件'
})

async function onPickFile(e) {
  uploadError.value = ''
  const f = e?.target?.files?.[0]
  if (!f) {
    file.value = null
    return
  }

  if (f.size > MAX_UPLOAD_BYTES) {
    uploadError.value = '文件过大（最大 4GB）'
    file.value = null
    return
  }
  file.value = f
}

async function onPickCover(e) {
  coverError.value = ''
  const f = e?.target?.files?.[0]
  if (!f) {
    coverFile.value = null
    return
  }
  if (!String(f.type || '').toLowerCase().startsWith('image/')) {
    coverError.value = '请选择图片文件'
    coverFile.value = null
    return
  }
  coverFile.value = f
  await doUploadCover()
}

function openCoverPicker() {
  try {
    if (coverInput.value) coverInput.value.click()
  } catch {
  }
}

async function doUploadCover() {
  if (!coverFile.value) return
  uploadingCover.value = true
  coverError.value = ''
  try {
    const resp = await uploadFile(coverFile.value)
    const url = resp?.data
    if (!url) throw new Error('上传失败：未返回地址')
    coverUrl.value = url
  } catch (e) {
    coverUrl.value = ''
    coverError.value = e?.response?.data?.message || e?.message || '上传失败'
  } finally {
    uploadingCover.value = false
  }
}

function clearCover() {
  coverFile.value = null
  coverUrl.value = ''
  coverError.value = ''
  try {
    if (coverInput.value) coverInput.value.value = ''
  } catch {
  }
}

async function doUpload() {
  if (!file.value) return
  uploading.value = true
  uploadError.value = ''
  uploadProgress.value = 0
  try {
    const isBrowser = typeof window !== 'undefined'
    const host = isBrowser ? window.location.hostname : ''
    const isLocalhost = host === 'localhost' || host === '127.0.0.1'
    const isHttps = isBrowser ? window.location.protocol === 'https:' : false
    const conservative = isHttps && !isLocalhost

    const resp = await uploadFile(file.value, {
      ...(mediaType.value === 'VIDEO'
        ? {
            // 线上 https 环境（尤其走隧道/代理）更容易在“单次大直传”时被中途断开。
            // 这里对视频强制更保守：更低的直传阈值 + 更小并发，优先走分片以提高稳定性。
            // 单个 chunk 如果上传耗时过长（上行慢）会触发代理 524，所以这里继续走小分片 + 低并发
            chunkSize: conservative ? 1 * 1024 * 1024 : 16 * 1024 * 1024,
            chunkThreshold: conservative ? 1 * 1024 * 1024 : 32 * 1024 * 1024,
            concurrency: conservative ? 1 : 4,
          }
        : null),
      onProgress: (pct) => {
        uploadProgress.value = Math.min(Math.max(Number(pct) || 0, 0), 100)
      },
    })
    const url = resp?.data
    if (!url) throw new Error('上传失败：未返回地址')
    mediaUrl.value = url
  } catch (e) {
    uploadError.value = e?.response?.data?.message || e?.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

function resetPayloadByType() {
  if (mediaType.value === 'NONE') {
    mediaUrl.value = ''
    linkUrl.value = ''
    file.value = null
  }
  if (mediaType.value === 'URL') {
    mediaUrl.value = ''
    file.value = null
  }
  if (mediaType.value === 'IMAGE' || mediaType.value === 'VIDEO' || mediaType.value === 'FILE') {
    linkUrl.value = ''
  }
}

async function submit() {
  errorMsg.value = ''

  // 前端先拦截必填校验，避免发起请求后才看到 400
  if (!String(title.value || '').trim()) {
    errorMsg.value = '标题不能为空'
    return
  }
  if (mediaType.value === 'URL' && !String(linkUrl.value || '').trim()) {
    errorMsg.value = '链接不能为空'
    return
  }
  if (mediaNeedsUrl.value && !String(mediaUrl.value || '').trim()) {
    errorMsg.value = '请先上传文件并等待合并完成（出现预览后再发布）'
    return
  }
  if (uploading.value || uploadingCover.value) {
    errorMsg.value = '正在上传中，请稍后再发布'
    return
  }

  loading.value = true
  try {
    const rawText = content.value || ''
    const nextContent = coverUrl.value ? `[[cover:${coverUrl.value}]]\n${rawText}`.trim() : rawText
    const payload = {
      title: title.value,
      content: nextContent,
      mediaType: mediaType.value,
      mediaUrl: mediaUrl.value,
      linkUrl: linkUrl.value,
    }
    const resp = await createShare(payload)
    const id = resp?.data?.id
    if (id) {
      router.push(`/shares/${id}`)
    } else {
      router.push('/shares')
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '发布失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <h1 class="title">发布好物分享</h1>

    <div class="card">
      <div class="field">
        <div class="label">封面（可选）</div>
        <div class="coverRow">
          <input ref="coverInput" class="coverPick" type="file" accept="image/*" @change="onPickCover" />
          <button class="ghost" type="button" :disabled="uploadingCover" @click="openCoverPicker">
            {{ uploadingCover ? '上传中' : (coverUrl ? '更换封面' : '上传封面') }}
          </button>
          <button v-if="coverUrl" class="ghost" type="button" :disabled="uploadingCover" @click="clearCover">移除</button>
        </div>
        <div v-if="coverError" class="error">{{ coverError }}</div>
        <div v-if="coverUrl" class="coverPreview">
          <img :src="coverUrl" alt="" />
        </div>
      </div>

      <div class="field">
        <div class="label">标题</div>
        <input v-model="title" class="input" placeholder="例：免费送一套教材 / 推荐一个开源工具" />
      </div>

      <div class="field">
        <div class="label">内容</div>
        <textarea v-model="content" class="textarea" placeholder="补充说明、使用体验、领取方式等"></textarea>
      </div>

      <div class="field">
        <div class="label">分享类型</div>
        <select v-model="mediaType" class="select" @change="resetPayloadByType">
          <option value="NONE">纯文字</option>
          <option value="IMAGE">图片</option>
          <option value="VIDEO">视频</option>
          <option value="URL">链接</option>
          <option value="FILE">文件（压缩包）</option>
        </select>
      </div>

      <div v-if="showUpload" class="field">
        <div class="label">{{ uploadLabel }}</div>
        <div class="uploadRow">
          <input class="file" type="file" :accept="acceptValue" @change="onPickFile" />
          <button :class="['ghost', uploading ? '' : 'uploadYellow']" type="button" :disabled="!file || uploading" @click="doUpload">
            {{ uploading ? '上传中...' : '上传' }}
          </button>
        </div>
        <div v-if="uploadError" class="error">{{ uploadError }}</div>
        <div v-if="uploading" class="progress">
          <div class="bar" :style="{ width: uploadProgress + '%' }"></div>
        </div>
        <div v-if="uploading" class="muted progressText">{{ uploadProgress }}%</div>
        <div v-if="mediaUrl" class="preview">
          <img v-if="mediaType === 'IMAGE'" :src="mediaUrl" alt="" />
          <video v-else-if="mediaType === 'VIDEO'" controls :src="mediaUrl"></video>
          <a
            v-else
            class="fileLink"
            :href="mediaUrl"
            target="_blank"
            rel="noopener noreferrer"
            :download="(mediaUrl || '').split('/').pop() || 'attachment'"
          >
            下载附件
          </a>
        </div>
        <div class="tips muted">上传成功后会生成可访问地址，并作为分享内容展示。</div>
      </div>

      <div v-if="showLink" class="field">
        <div class="label">URL 链接</div>
        <input v-model="linkUrl" class="input" placeholder="https://..." />
      </div>

      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

      <button class="primary" type="button" :disabled="!canSubmit" @click="submit">
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

.field {
  margin-bottom: 12px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.input,
.select {
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
}

.primary:hover {
  background: var(--primary-hover);
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
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.uploadYellow {
  background: #fbbf24;
  border-color: #fbbf24;
  color: #111827;
}

.uploadYellow:hover {
  background: #f59e0b;
  border-color: #f59e0b;
}

.uploadYellow:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.coverRow {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.coverPick {
  display: none;
}

.coverPreview {
  margin-top: 10px;
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface-2);
}

.coverPreview img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.error {
  margin: 8px 0;
  color: var(--danger);
  font-size: 13px;
}

.preview {
  margin-top: 10px;
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface-2);
}

.preview img,
.preview video {
  width: 100%;
  height: 280px;
  object-fit: contain;
  display: block;
}

.fileLink {
  padding: 12px;
  color: var(--primary);
  word-break: break-all;
  display: block;
}

.tips {
  margin-top: 10px;
  font-size: 12px;
}

.progress {
  margin-top: 10px;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--surface-2);
  border: 1px solid var(--border-2);
}

.bar {
  height: 100%;
  background: var(--primary);
  transition: width 0.1s linear;
}

.progressText {
  margin-top: 6px;
  font-size: 12px;
}
</style>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createShare, uploadFile } from '../services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const title = ref('')
const content = ref('')

const coverFile = ref(null)
const coverUrl = ref('')
const uploadingCover = ref(false)
const coverError = ref('')
const coverInput = ref(null)
const coverUploadProgress = ref(0)

const mediaType = ref('NONE')
const mediaUrl = ref('')
const linkUrl = ref('')

const file = ref(null)
const uploading = ref(false)
const uploadError = ref('')
const uploadProgress = ref(0)

const MAX_UPLOAD_BYTES = 4294967296

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const loading = ref(false)
const errorMsg = ref('')

const mediaTypeOptions = [
  { value: 'NONE', label: '纯文字', icon: 'Document' },
  { value: 'IMAGE', label: '图片', icon: 'Picture' },
  { value: 'VIDEO', label: '视频', icon: 'VideoCamera' },
  { value: 'URL', label: '链接', icon: 'Link' },
  { value: 'FILE', label: '文件（压缩包）', icon: 'Folder' }
]

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

async function onPickFile(fileObj) {
  uploadError.value = ''
  mediaUrl.value = ''
  const f = fileObj?.raw
  if (!f) {
    file.value = null
    return
  }

  if (f.size > MAX_UPLOAD_BYTES) {
    uploadError.value = `文件过大（${formatFileSize(f.size)}），最大支持 4GB`
    file.value = null
    ElMessage.error(uploadError.value)
    return
  }
  
  if (f.size === 0) {
    uploadError.value = '文件为空，请选择有效文件'
    file.value = null
    ElMessage.error(uploadError.value)
    return
  }
  
  file.value = f
  await doUpload()
}

async function onPickCover(e) {
  coverError.value = ''
  coverUrl.value = ''
  const f = e?.target?.files?.[0]
  if (!f) {
    coverFile.value = null
    return
  }
  if (!String(f.type || '').toLowerCase().startsWith('image/')) {
    coverError.value = '请选择图片文件'
    coverFile.value = null
    ElMessage.error(coverError.value)
    return
  }
  if (f.size > MAX_UPLOAD_BYTES) {
    coverError.value = `文件过大（${formatFileSize(f.size)}），最大支持 4GB`
    coverFile.value = null
    ElMessage.error(coverError.value)
    return
  }
  if (f.size === 0) {
    coverError.value = '文件为空，请选择有效图片'
    coverFile.value = null
    ElMessage.error(coverError.value)
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
  coverUploadProgress.value = 0
  
  try {
    const resp = await uploadFile(coverFile.value, {
      onProgress: (pct) => {
        coverUploadProgress.value = Math.min(Math.max(Number(pct) || 0, 0), 100)
      },
    })
    
    if (resp?.code !== 0) {
      throw new Error(resp?.message || '上传失败')
    }
    
    const url = resp?.data
    if (!url || typeof url !== 'string') {
      throw new Error('上传失败：未返回有效地址')
    }
    
    coverUrl.value = url
    coverUploadProgress.value = 100
    ElMessage.success('封面上传成功')
  } catch (e) {
    console.error('封面上传失败:', e)
    coverUrl.value = ''
    coverUploadProgress.value = 0
    coverError.value = e?.response?.data?.message || e?.message || '上传失败'
    ElMessage.error(coverError.value)
  } finally {
    uploadingCover.value = false
  }
}

function clearCover() {
  coverFile.value = null
  coverUrl.value = ''
  coverError.value = ''
  coverUploadProgress.value = 0
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
    const resp = await uploadFile(file.value, {
      onProgress: (pct) => {
        uploadProgress.value = Math.min(Math.max(Number(pct) || 0, 0), 100)
      },
    })
    
    if (resp?.code !== 0) {
      throw new Error(resp?.message || '上传失败')
    }
    
    const url = resp?.data
    if (!url || typeof url !== 'string') {
      throw new Error('上传失败：未返回有效地址')
    }
    
    mediaUrl.value = url
    uploadProgress.value = 100
    ElMessage.success('文件上传成功')
  } catch (e) {
    console.error('上传失败:', e)
    uploadError.value = e?.response?.data?.message || e?.message || '上传失败'
    uploadProgress.value = 0
    mediaUrl.value = ''
    ElMessage.error(uploadError.value)
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
  uploadProgress.value = 0
}

async function submit() {
  errorMsg.value = ''

  if (!String(title.value || '').trim()) {
    errorMsg.value = '标题不能为空'
    ElMessage.warning(errorMsg.value)
    return
  }
  if (mediaType.value === 'URL' && !String(linkUrl.value || '').trim()) {
    errorMsg.value = '链接不能为空'
    ElMessage.warning(errorMsg.value)
    return
  }
  if (mediaNeedsUrl.value && !String(mediaUrl.value || '').trim()) {
    errorMsg.value = '请先上传文件并等待合并完成（出现预览后再发布）'
    ElMessage.warning(errorMsg.value)
    return
  }
  if (uploading.value || uploadingCover.value) {
    errorMsg.value = '正在上传中，请稍后再发布'
    ElMessage.warning(errorMsg.value)
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
    ElMessage.success('发布成功！')
    if (id) {
      router.push(`/shares/${id}`)
    } else {
      router.push('/shares')
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '发布失败'
    ElMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap publishPage">
    <el-page-header @back="() => router.back()" content="发布好物分享" style="margin-bottom: 14px;" />

    <div class="hero">
      <div>
        <div class="heroTitle">把你的宝藏分享给大家</div>
        <div class="heroSub">支持图片、视频、链接和文件上传，发布前可实时预览。</div>
      </div>
      <div class="heroTips">
        <span>步骤 1：填写信息</span>
        <span>步骤 2：上传媒体</span>
        <span>步骤 3：发布</span>
      </div>
    </div>

    <el-card class="card" shadow="never">
      <el-form label-position="top">
        <div class="sectionTitle">基础信息</div>

        <el-form-item label="封面（可选）">
          <div class="coverRow">
            <input ref="coverInput" class="coverPick" type="file" accept="image/*" @change="onPickCover" />
            <el-button :icon="Upload" :loading="uploadingCover" @click="openCoverPicker">
              {{ uploadingCover ? '上传中' : (coverUrl ? '更换封面' : '上传封面') }}
            </el-button>
            <el-button v-if="coverUrl" :icon="Delete" :disabled="uploadingCover" @click="clearCover">
              移除
            </el-button>
          </div>
          <el-alert v-if="coverError" type="error" :closable="false" style="margin-top: 10px;">
            {{ coverError }}
          </el-alert>
          <div v-if="uploadingCover" class="progressWrapper">
            <div class="uploadInfo">
              <span class="fileName">{{ coverFile?.name || '上传中...' }}</span>
              <span class="fileSize" v-if="coverFile">{{ formatFileSize(coverFile.size) }}</span>
            </div>
            <el-progress :percentage="coverUploadProgress" :stroke-width="16" :color="coverUploadProgress === 100 ? '#67c23a' : '#409eff'" />
            <div class="progressText muted" style="margin-top: 6px; text-align: center;">
              {{ coverUploadProgress === 100 ? '处理中...' : `上传中 ${coverUploadProgress}%` }}
            </div>
          </div>
          <div v-if="coverUrl" class="coverPreview">
            <el-image :src="coverUrl" fit="cover" />
          </div>
        </el-form-item>

        <el-form-item label="标题" required>
          <el-input
            v-model="title"
            placeholder="例：免费送一套教材 / 推荐一个开源工具"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容">
          <el-input
            v-model="content"
            type="textarea"
            :rows="4"
            placeholder="补充说明、使用体验、领取方式等"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <div class="sectionTitle">分享形式</div>
        <el-form-item label="分享类型">
          <el-radio-group v-model="mediaType" @change="resetPayloadByType" class="typeGroup">
            <el-radio-button v-for="type in mediaTypeOptions" :key="type.value" :value="type.value">
              <el-icon><component :is="type.icon" /></el-icon>
              {{ type.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div v-if="showUpload">
          <el-form-item :label="uploadLabel" required>
            <div class="uploadRow">
              <el-upload
                class="fileUpload"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="onPickFile"
                :accept="acceptValue"
                drag
                :disabled="uploading"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">
                  将文件拖到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    {{ mediaType === 'FILE' ? '支持 zip/rar/7z/tar/gz/tgz 格式' : '' }}
                    {{ mediaType === 'IMAGE' ? '支持 jpg/png/gif/webp 等图片格式' : '' }}
                    {{ mediaType === 'VIDEO' ? '支持 mp4/webm/avi/mov 等视频格式' : '' }}
                  </div>
                </template>
              </el-upload>
            </div>

            <div v-if="uploading" class="progressWrapper">
              <div class="uploadInfo">
                <span class="fileName">{{ file?.name || '上传中...' }}</span>
                <span class="fileSize" v-if="file">{{ formatFileSize(file.size) }}</span>
              </div>
              <el-progress :percentage="uploadProgress" :stroke-width="16" :color="uploadProgress === 100 ? '#67c23a' : '#409eff'" />
              <div class="progressText muted" style="margin-top: 6px; text-align: center;">
                {{ uploadProgress === 100 ? '处理中...' : `上传中 ${uploadProgress}%` }}
              </div>
            </div>

            <el-alert v-if="uploadError" type="error" :closable="false" style="margin-top: 10px;">
              {{ uploadError }}
            </el-alert>

            <div v-if="mediaUrl" class="preview">
              <el-image
                v-if="mediaType === 'IMAGE'"
                :src="mediaUrl"
                fit="contain"
                :preview-src-list="[mediaUrl]"
                lazy
              />
              <video v-else-if="mediaType === 'VIDEO'" controls :src="mediaUrl" style="width: 100%; max-height: 360px; border-radius: 8px;"></video>
              <a
                v-else
                class="fileLink"
                :href="mediaUrl"
                target="_blank"
                rel="noopener noreferrer"
                :download="(mediaUrl || '').split('/').pop() || 'attachment'"
              >
                <el-icon><Document /></el-icon>
                {{ (mediaUrl || '').split('/').pop() || 'attachment' }}
              </a>
              <el-tag type="success" style="margin-top: 10px;">
                <el-icon><CircleCheck /></el-icon>
                上传成功
              </el-tag>
            </div>
          </el-form-item>
        </div>

        <div v-if="showLink">
          <el-form-item label="URL 链接" required>
            <el-input
              v-model="linkUrl"
              placeholder="https://..."
              prefix-icon="Link"
            />
          </el-form-item>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :icon="Upload"
            :disabled="!canSubmit"
            :loading="loading"
            @click="submit"
            style="width: 100%;"
            class="publishBtn"
          >
            {{ loading ? '提交中...' : '发布分享' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.publishPage {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.14), rgba(59, 130, 246, 0.14));
  border: 1px solid rgba(148, 163, 184, 0.24);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.heroTitle {
  font-size: 20px;
  font-weight: 800;
}

.heroSub {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.heroTips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.heroTips span {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.26);
}

.card {
  border-radius: 14px;
}

.sectionTitle {
  font-size: 14px;
  font-weight: 700;
  color: var(--muted);
  margin-bottom: 8px;
}

.typeGroup {
  display: flex;
  flex-wrap: wrap;
}

.coverRow {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.coverPick {
  display: none;
}

.coverPreview {
  margin-top: 12px;
  border-radius: 10px;
  overflow: hidden;
}

.coverPreview :deep(.el-image) {
  width: 100%;
  height: 220px;
}

.uploadRow {
  width: 100%;
}

.fileUpload {
  width: 100%;
}

.fileUpload :deep(.el-upload-dragger) {
  width: 100%;
  border-radius: 12px;
}

.preview {
  margin-top: 14px;
  padding: 14px;
  background: var(--surface-2);
  border-radius: 10px;
  border: 1px solid var(--border-2);
}

.preview :deep(.el-image) {
  width: 100%;
  max-height: 360px;
}

.fileLink {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: var(--primary);
  word-break: break-all;
  background: var(--surface);
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.15s ease;
}

.fileLink:hover {
  background: var(--primary);
  color: #fff;
}

.progressWrapper {
  margin-top: 10px;
  padding: 12px;
  background: var(--surface-2);
  border-radius: 10px;
  border: 1px solid var(--border-2);
}

.uploadInfo {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.fileName {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}

.fileSize {
  font-size: 12px;
  color: var(--muted);
}

.progressText {
  font-size: 13px;
}

.publishBtn {
  border-radius: 10px;
}

@media (max-width: 640px) {
  .heroTitle {
    font-size: 18px;
  }

  .typeGroup {
    width: 100%;
  }

  .typeGroup :deep(.el-radio-button__inner) {
    font-size: 12px;
    padding: 8px 10px;
  }
}
</style>

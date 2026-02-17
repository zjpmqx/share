<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getShare, listShareComments, postShareComment, uploadFile } from '../services/api'
import { getToken } from '../services/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()

const loading = ref(false)
const errorMsg = ref('')
const share = ref(null)

const comments = ref([])
const commentsLoading = ref(false)

const content = ref('')
const imageFile = ref(null)
const imageUrl = ref('')
const uploadingImage = ref(false)
const uploadImageError = ref('')
const imageInput = ref(null)
const posting = ref(false)
const postError = ref('')

const COMMENT_IMG_RE = /\[\[img:([^\]]+)\]\]/g

const COVER_RE = /\[\[cover:([^\]]+)\]\]/g

function parseShareContent(raw) {
  const s = String(raw || '')
  let cover = ''
  let m
  while ((m = COVER_RE.exec(s)) !== null) {
    const url = (m[1] || '').trim()
    if (url) cover = url
  }
  const text = s.replace(COVER_RE, '').trim()
  return { cover, text }
}

function parseCommentContent(raw) {
  const s = String(raw || '')
  const images = []
  let m
  while ((m = COMMENT_IMG_RE.exec(s)) !== null) {
    const url = (m[1] || '').trim()
    if (url) images.push(url)
  }
  const text = s.replace(COMMENT_IMG_RE, '').trim()
  return { text, images }
}

async function onPickImage(e) {
  uploadImageError.value = ''
  const f = e?.target?.files?.[0]
  if (!f) {
    imageFile.value = null
    return
  }
  if (!String(f.type || '').toLowerCase().startsWith('image/')) {
    uploadImageError.value = '请选择图片文件'
    imageFile.value = null
    ElMessage.error(uploadImageError.value)
    return
  }
  imageFile.value = f
  await doUploadImage()
}

function openImagePicker() {
  try {
    if (imageInput.value) imageInput.value.click()
  } catch {
  }
}

async function doUploadImage() {
  if (!imageFile.value) return
  uploadingImage.value = true
  uploadImageError.value = ''
  try {
    const resp = await uploadFile(imageFile.value)
    const url = resp?.data
    if (!url) throw new Error('上传失败：未返回地址')
    imageUrl.value = url
    ElMessage.success('图片上传成功')
  } catch (e) {
    imageUrl.value = ''
    uploadImageError.value = e?.response?.data?.message || e?.message || '上传失败'
    ElMessage.error(uploadImageError.value)
  } finally {
    uploadingImage.value = false
  }
}

function clearImage() {
  imageFile.value = null
  imageUrl.value = ''
  uploadImageError.value = ''
  try {
    if (imageInput.value) imageInput.value.value = ''
  } catch {
  }
}

function mediaKind(it) {
  const t = (it?.mediaType || 'NONE').toUpperCase()
  if (t === 'IMAGE' && it.mediaUrl) return 'image'
  if (t === 'VIDEO' && it.mediaUrl) return 'video'
  if (t === 'FILE' && it.mediaUrl) return 'file'
  if (t === 'URL' && it.linkUrl) return 'link'
  return 'none'
}

function showCover(it) {
  if (!it?._cover) return false
  const t = (it?.mediaType || 'NONE').toUpperCase()
  return t !== 'IMAGE' && t !== 'VIDEO'
}

async function loadShare() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await getShare(route.params.id)
    const data = resp.data
    const parts = parseShareContent(data?.content)
    share.value = { ...data, _cover: parts.cover, _text: parts.text }
  } catch (e) {
    share.value = null
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
    ElMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  try {
    const resp = await listShareComments(route.params.id)
    comments.value = (resp.data || []).map((m) => ({
      ...m,
      _parts: parseCommentContent(m?.content),
    }))
  } finally {
    commentsLoading.value = false
  }
}

async function send() {
  if (!content.value.trim() && !imageUrl.value) {
    ElMessage.warning('请输入评论内容或上传图片')
    return
  }
  postError.value = ''
  posting.value = true
  try {
    const baseText = content.value.trim()
    const nextContent = imageUrl.value ? `${baseText}\n[[img:${imageUrl.value}]]`.trim() : baseText
    await postShareComment(route.params.id, { content: nextContent })
    content.value = ''
    clearImage()
    ElMessage.success('评论发送成功')
    await loadComments()
  } catch (e) {
    postError.value = e?.response?.data?.message || e?.message || '发送失败'
    ElMessage.error(postError.value)
  } finally {
    posting.value = false
  }
}

function getTypeIcon(type) {
  const t = (type || 'NONE').toUpperCase()
  switch (t) {
    case 'IMAGE': return 'Picture'
    case 'VIDEO': return 'VideoCamera'
    case 'URL': return 'Link'
    case 'FILE': return 'Document'
    default: return 'Document'
  }
}

function getTypeTagType(type) {
  const t = (type || 'NONE').toUpperCase()
  switch (t) {
    case 'IMAGE': return 'success'
    case 'VIDEO': return 'warning'
    case 'URL': return 'primary'
    case 'FILE': return 'danger'
    default: return 'info'
  }
}

onMounted(async () => {
  await Promise.all([loadShare(), loadComments()])
})
</script>

<template>
  <div class="page">
    <el-skeleton v-if="loading" :rows="8" animated />
    
    <div v-else-if="errorMsg" class="errorState">
      <el-result icon="error" title="加载失败" :sub-title="errorMsg">
        <template #extra>
          <el-button type="primary" @click="loadShare">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <div v-else-if="share" class="layout">
      <div class="left">
        <el-card class="mediaCard">
          <template #header>
            <div class="mediaHeader">
              <span class="mediaTitle">分享内容</span>
              <el-tag :type="getTypeTagType(share.mediaType)" size="small">
                <el-icon><component :is="getTypeIcon(share.mediaType)" /></el-icon>
                {{ share.mediaType || 'NONE' }}
              </el-tag>
            </div>
          </template>
          
          <div class="mediaBox">
            <el-image
              v-if="showCover(share)"
              :src="share._cover"
              fit="contain"
              :preview-src-list="[share._cover]"
              lazy
            />

            <el-image
              v-else-if="mediaKind(share) === 'image'"
              :src="share.mediaUrl"
              fit="contain"
              :preview-src-list="[share.mediaUrl]"
              lazy
            />

            <video v-else-if="mediaKind(share) === 'video'" controls :src="share.mediaUrl"></video>

            <div v-else class="emptyMedia">
              <el-icon><Document /></el-icon>
              <span>纯文字分享</span>
            </div>
          </div>

          <div v-if="mediaKind(share) === 'file'" class="mediaActions">
            <el-button type="primary" :icon="Download">
              <a
                :href="share.mediaUrl"
                target="_blank"
                rel="noopener noreferrer"
                :download="(share?.mediaUrl || '').split('/').pop() || 'attachment'"
                style="color: inherit; text-decoration: none;"
              >
                下载附件
              </a>
            </el-button>
          </div>

          <div v-else-if="mediaKind(share) === 'link'" class="mediaActions">
            <el-button type="primary" :icon="Link">
              <a :href="share.linkUrl" target="_blank" rel="noopener noreferrer" style="color: inherit; text-decoration: none;">
                打开链接
              </a>
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="right">
        <el-card class="infoCard">
          <template #header>
            <h1 class="title">{{ share.title }}</h1>
          </template>
          
          <div class="desc">{{ share._text || '（无内容）' }}</div>
        </el-card>

        <el-card class="commentsCard" style="margin-top: 14px;">
          <template #header>
            <div class="commentsHeader">
              <span class="commentsTitle">评论 ({{ comments.length }})</span>
            </div>
          </template>

          <el-skeleton v-if="commentsLoading" :rows="4" animated />

          <div v-else class="msgs">
            <div v-for="m in comments" :key="m.id" class="msg">
              <div class="msgHead">
                <el-avatar :size="32">
                  <img v-if="m.avatarUrl" :src="m.avatarUrl" alt="" />
                  <template v-else>
                    <el-icon><User /></el-icon>
                  </template>
                </el-avatar>
                <div class="who">{{ m.userName || ('用户' + (m.userId || '')) }}</div>
              </div>
              
              <div class="msgContent">
                <div v-if="m?._parts?.text" class="msgText">{{ m._parts.text }}</div>
                <div v-if="m?._parts?.images?.length" class="msgImages">
                  <el-image
                    v-for="(u, idx) in m._parts.images"
                    :key="idx"
                    :src="u"
                    :preview-src-list="m._parts.images"
                    :initial-index="idx"
                    fit="cover"
                    lazy
                    class="msgImg"
                  />
                </div>
              </div>
              
              <div class="msgTime">{{ m.createdAt || '' }}</div>
            </div>
            
            <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="80" />
          </div>

          <div class="composer">
            <el-input
              v-model="content"
              type="textarea"
              :rows="2"
              placeholder="说点什么..."
              :disabled="!getToken() || posting"
            />

            <input ref="imageInput" class="imgPick" type="file" accept="image/*" @change="onPickImage" />

            <div class="actions">
              <el-button :icon="Picture" :disabled="!getToken() || posting || uploadingImage" @click="openImagePicker">
                {{ uploadingImage ? '上传中' : '图片' }}
              </el-button>
              <el-button type="primary" :icon="ChatDotRound" :disabled="!getToken() || posting || uploadingImage" @click="send" :loading="posting">
                {{ posting ? '发送中...' : '发送' }}
              </el-button>
            </div>
          </div>

          <div v-if="uploadImageError || imageUrl" class="composerMeta">
            <el-alert v-if="uploadImageError" type="error" :closable="false" style="margin-bottom: 10px;">
              {{ uploadImageError }}
            </el-alert>
            
            <div v-if="imageUrl" class="imgPreview">
              <el-image :src="imageUrl" class="imgPreviewImg" fit="cover" />
              <span class="imgPreviewText">已添加图片</span>
              <el-button size="small" :disabled="posting || uploadingImage" @click="clearImage">移除</el-button>
            </div>
          </div>
          
          <el-alert v-if="!getToken()" type="warning" :closable="false" style="margin-top: 10px;">
            登录后才能评论
          </el-alert>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 14px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.mediaCard {
  overflow: hidden;
}

.mediaHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mediaTitle {
  font-weight: 600;
}

.mediaBox {
  width: 100%;
  min-height: 320px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.10), rgba(59, 130, 246, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
}

.mediaBox :deep(.el-image) {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.mediaBox video {
  width: 100%;
  max-height: 420px;
  display: block;
  border-radius: 8px;
}

.emptyMedia {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 14px;
}

.emptyMedia .el-icon {
  font-size: 48px;
}

.mediaActions {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

.title {
  font-size: 20px;
  margin: 0;
  font-weight: 700;
}

.desc {
  padding: 10px 0;
  line-height: 1.8;
  color: var(--text);
  white-space: pre-wrap;
  word-break: break-word;
}

.commentsHeader {
  display: flex;
  align-items: center;
}

.commentsTitle {
  font-weight: 600;
}

.msgs {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 14px;
}

.msg {
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  border-radius: 12px;
  padding: 12px;
}

.msgHead {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.who {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.msgContent {
  font-size: 14px;
}

.msgText {
  white-space: pre-wrap;
  word-break: break-word;
}

.msgImages {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

@media (max-width: 520px) {
  .msgImages {
    grid-template-columns: repeat(3, 1fr);
  }
}

.msgImg {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface);
  cursor: pointer;
}

.msgTime {
  font-size: 12px;
  color: var(--muted-2);
  margin-top: 8px;
}

.composer {
  border-top: 1px solid var(--border-2);
  padding-top: 14px;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  justify-content: flex-end;
}

.imgPick {
  display: none;
}

.composerMeta {
  margin-top: 10px;
}

.imgPreview {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px;
  background: var(--surface-2);
  border-radius: 8px;
}

.imgPreviewText {
  font-size: 13px;
  color: var(--muted);
}

.imgPreviewImg {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.errorState {
  margin-top: 40px;
}
</style>

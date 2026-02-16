<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getShare, listShareComments, postShareComment, uploadFile } from '../services/api'
import { getToken } from '../services/auth'

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
  } catch (e) {
    imageUrl.value = ''
    uploadImageError.value = e?.response?.data?.message || e?.message || '上传失败'
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
  if (!content.value.trim() && !imageUrl.value) return
  postError.value = ''
  posting.value = true
  try {
    const baseText = content.value.trim()
    const nextContent = imageUrl.value ? `${baseText}\n[[img:${imageUrl.value}]]`.trim() : baseText
    await postShareComment(route.params.id, { content: nextContent })
    content.value = ''
    clearImage()
    await loadComments()
  } catch (e) {
    postError.value = e?.response?.data?.message || e?.message || '发送失败'
  } finally {
    posting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadShare(), loadComments()])
})
</script>

<template>
  <div class="page">
    <div v-if="loading" class="muted">加载中...</div>
    <div v-else-if="errorMsg" class="error">{{ errorMsg }}</div>

    <div v-else-if="share" class="layout">
      <div class="left">
        <div class="card media">
          <div class="mediaBox">
            <img v-if="showCover(share)" :src="share._cover" alt="" loading="lazy" />

            <img v-else-if="mediaKind(share) === 'image'" :src="share.mediaUrl" alt="" loading="lazy" />
            <video v-else-if="mediaKind(share) === 'video'" controls :src="share.mediaUrl"></video>

            <div v-else class="emptyMedia">纯文字分享</div>
          </div>

          <div v-if="mediaKind(share) === 'file'" class="mediaActions">
            <a
              class="actionBtn"
              :href="share.mediaUrl"
              target="_blank"
              rel="noopener noreferrer"
              :download="(share?.mediaUrl || '').split('/').pop() || 'attachment'"
            >
              下载附件
            </a>
          </div>

          <div v-else-if="mediaKind(share) === 'link'" class="mediaActions">
            <a class="actionBtn" :href="share.linkUrl" target="_blank" rel="noopener noreferrer">打开链接</a>
          </div>
        </div>
      </div>

      <div class="right">
        <div class="card">
          <div class="head">
            <h1 class="title">{{ share.title }}</h1>
            <span class="chip">{{ share.mediaType || 'NONE' }}</span>
          </div>
          <div class="desc">{{ share._text || '（无内容）' }}</div>
        </div>

        <div class="card" style="margin-top: 12px;">
          <h2 class="sub">评论</h2>

          <div v-if="commentsLoading" class="muted">加载评论中...</div>
          <div v-else class="msgs">
            <div v-for="m in comments" :key="m.id" class="msg">
              <div class="msgHead">
                <div class="avatar">
                  <img v-if="m.avatarUrl" :src="m.avatarUrl" alt="" />
                  <div v-else class="avatarFallback"></div>
                </div>
                <div class="who">{{ m.userName || ('用户' + (m.userId || '')) }}</div>
              </div>
              <div class="msgContent">
                <div v-if="m?._parts?.text" class="msgText">{{ m._parts.text }}</div>
                <div v-if="m?._parts?.images?.length" class="msgImages">
                  <a
                    v-for="(u, idx) in m._parts.images"
                    :key="idx"
                    class="msgImgLink"
                    :href="u"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <img class="msgImg" :src="u" alt="" loading="lazy" />
                  </a>
                </div>
              </div>
              <div class="msgTime">{{ m.createdAt || '' }}</div>
            </div>
            <div v-if="comments.length === 0" class="empty">暂无评论</div>
          </div>

          <div class="composer">
            <input v-model="content" class="input" placeholder="说点什么..." :disabled="!getToken() || posting" />

            <input ref="imageInput" class="imgPick" type="file" accept="image/*" @change="onPickImage" />

            <div class="actions">
              <button class="ghost" type="button" :disabled="!getToken() || posting || uploadingImage" @click="openImagePicker">
                {{ uploadingImage ? '上传中' : '图片' }}
              </button>
              <button class="primary" type="button" :disabled="!getToken() || posting || uploadingImage" @click="send">
                {{ posting ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>

          <div v-if="uploadImageError || imageUrl" class="composerMeta">
            <div v-if="uploadImageError" class="error">{{ uploadImageError }}</div>
            <div v-if="imageUrl" class="imgPreview">
              <img class="imgPreviewImg" :src="imageUrl" alt="" />
              <div class="imgPreviewText muted">已添加图片</div>
              <button class="ghost" type="button" :disabled="posting || uploadingImage" @click="clearImage">移除</button>
            </div>
          </div>
          <div v-if="!getToken()" class="hint">登录后才能评论</div>
          <div v-if="postError" class="error" style="margin-top: 8px;">{{ postError }}</div>
        </div>
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

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  padding: 14px;
  box-shadow: var(--shadow-sm);
}

.media {
  overflow: hidden;
}

.mediaBox {
  width: 100%;
  height: 420px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.10), rgba(59, 130, 246, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 900px) {
  .mediaBox {
    height: 320px;
  }
}

@media (max-width: 520px) {
  .mediaBox {
    height: 260px;
  }
}

.mediaBox img,
.mediaBox video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.emptyMedia {
  color: var(--muted);
  font-size: 13px;
}

.link {
  padding: 12px;
  color: var(--primary);
  word-break: break-all;
}

.mediaActions {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.actionBtn {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 999px;
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  color: var(--primary);
  text-decoration: none;
  font-size: 13px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 20px;
  margin: 0;
}

.chip {
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--muted);
}

.desc {
  padding: 10px 0;
  line-height: 1.6;
  color: var(--text);
}

.sub {
  margin: 0 0 10px;
  font-size: 16px;
}

.msgs {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.msg {
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  border-radius: 12px;
  padding: 10px;
}

.msgHead {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.who {
  font-size: 13px;
  color: var(--muted);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid var(--border-2);
  background: var(--surface);
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

.msgContent {
  font-size: 14px;
}

.msgText {
  white-space: pre-wrap;
  word-break: break-word;
}

.msgImages {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(3, 88px);
  gap: 8px;
}

@media (max-width: 520px) {
  .msgImages {
    grid-template-columns: repeat(3, 1fr);
  }

  .msgImg {
    width: 100%;
  }
}

.msgImgLink {
  display: inline-block;
  border: 1px solid var(--border-2);
  border-radius: 10px;
  overflow: hidden;
  background: var(--surface-2);
}

.msgImg {
  width: 88px;
  height: 88px;
  object-fit: cover;
  display: block;
}

.msgTime {
  font-size: 12px;
  color: var(--muted-2);
  margin-top: 6px;
}

.empty {
  color: var(--muted);
  font-size: 13px;
}

.composer {
  display: flex;
  gap: 10px;
  align-items: center;
}

.actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.imgPick {
  display: none;
}

@media (max-width: 520px) {
  .composer {
    flex-direction: column;
    align-items: stretch;
  }

  .composer .primary {
    width: 100%;
  }

  .actions {
    width: 100%;
  }

  .actions .ghost,
  .actions .primary {
    flex: 1;
  }
}

.input {
  flex: 1;
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

.ghost {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.composerMeta {
  margin-top: 10px;
}

.imgPreview {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.imgPreviewText {
  font-size: 12px;
}

.imgPreviewImg {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid var(--border-2);
  background: var(--surface-2);
}

.hint {
  font-size: 12px;
  color: var(--muted);
}

.error {
  font-size: 13px;
  color: var(--danger);
}
</style>

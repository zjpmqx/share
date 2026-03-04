<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createItem, uploadFile } from '../services/api'
import { ElMessage } from 'element-plus'

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

const categoryOptions = [
  { label: '书籍教材', value: 'BOOK' },
  { label: '数码电子', value: 'DIGITAL' },
  { label: '生活用品', value: 'LIFE' },
  { label: '服饰鞋包', value: 'FASHION' },
  { label: '其他', value: 'OTHER' }
]

const conditionOptions = [
  { label: '全新', value: 'NEW' },
  { label: '9成新', value: '9' },
  { label: '8成新', value: '8' },
  { label: '7成新', value: '7' },
  { label: '6成新及以下', value: '6' }
]

async function submit() {
  if (!title.value.trim()) {
    ElMessage.warning('请填写商品标题')
    return
  }
  if (Number(price.value) < 0) {
    ElMessage.warning('价格不能为负数')
    return
  }

  errorMsg.value = ''
  loading.value = true
  try {
    const resp = await createItem({
      title: title.value,
      description: description.value,
      category: category.value,
      price: Number(price.value),
      conditionLevel: conditionLevel.value,
      coverImageUrl: coverImageUrl.value
    })
    const id = resp?.data?.id
    ElMessage.success('发布成功，已提交审核')
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

function onSelectFile(e) {
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

    if (resp?.code !== 0) {
      throw new Error(resp?.message || '上传失败')
    }

    const url = resp?.data
    if (!url || typeof url !== 'string') {
      throw new Error('上传失败：未返回有效图片地址')
    }

    coverImageUrl.value = url
    ElMessage.success('封面上传成功')
  } catch (e) {
    uploadError.value = e?.response?.data?.message || e?.message || '上传失败'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="publishPage">
    <div class="hero">
      <div>
        <div class="heroTag">PUBLISH ITEM</div>
        <h1>发布你的闲置好物</h1>
        <p>信息越完整，越容易快速成交。提交后将进入审核流程。</p>
      </div>
      <div class="heroTips">
        <span>建议上传清晰实拍图</span>
        <span>描述真实使用情况</span>
      </div>
    </div>

    <el-card class="card" shadow="hover">
      <div class="sectionTitle">基础信息</div>

      <el-form label-position="top" @submit.prevent="submit">
        <div class="gridOne">
          <el-form-item label="标题">
            <el-input v-model="title" placeholder="例：高数教材 / 蓝牙耳机" maxlength="80" show-word-limit />
          </el-form-item>

          <el-form-item label="描述">
            <el-input
              v-model="description"
              type="textarea"
              :rows="4"
              placeholder="补充说明、购入时间、配件情况、是否支持小刀等"
              maxlength="600"
              show-word-limit
            />
          </el-form-item>
        </div>

        <div class="gridTwo">
          <el-form-item label="分类">
            <el-select v-model="category" placeholder="请选择分类">
              <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="成色">
            <el-select v-model="conditionLevel" placeholder="请选择成色">
              <el-option v-for="item in conditionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>

        <div class="gridTwo">
          <el-form-item label="价格（元）">
            <el-input-number v-model="price" :min="0" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>

          <el-form-item label="封面图 URL（可选）">
            <el-input v-model="coverImageUrl" placeholder="https://..." />
          </el-form-item>
        </div>

        <el-divider />

        <div class="sectionTitle">上传本地封面图</div>
        <div class="uploadBar">
          <input class="file" type="file" accept="image/*" @change="onSelectFile" />
          <el-button type="primary" plain :disabled="!coverFile || uploading" :loading="uploading" @click="doUpload">
            {{ uploading ? '上传中...' : '上传图片' }}
          </el-button>
        </div>

        <div v-if="uploadError" class="error">{{ uploadError }}</div>

        <div v-if="coverImageUrl" class="preview">
          <img :src="coverImageUrl" alt="" />
        </div>

        <div class="tips">上传成功后会自动填入封面图 URL，用于首页和详情页展示。</div>

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

        <el-button class="submitBtn" type="primary" size="large" :loading="loading" @click="submit">
          {{ loading ? '提交中...' : '发布商品' }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.publishPage {
  max-width: 980px;
  margin: 0 auto;
}

.hero {
  margin: 4px 0 14px;
  border-radius: 18px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  background: linear-gradient(135deg, #1d4ed8, #0284c7 62%, #06b6d4);
  color: #eff6ff;
  box-shadow: 0 14px 32px rgba(30, 64, 175, 0.25);
}

.heroTag {
  display: inline-flex;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.18);
}

.hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.2;
}

.hero p {
  margin: 0;
  color: rgba(239, 246, 255, 0.95);
}

.heroTips {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.heroTips span {
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.16);
}

.card {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.24);
}

.sectionTitle {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.gridOne,
.gridTwo {
  display: grid;
  gap: 12px;
}

.gridTwo {
  grid-template-columns: 1fr 1fr;
}

.uploadBar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.file {
  flex: 1;
  min-width: 230px;
}

.preview {
  margin-top: 10px;
  border: 1px solid var(--border-2);
  border-radius: 14px;
  overflow: hidden;
  background: #f8fafc;
}

.preview img {
  width: 100%;
  height: 260px;
  object-fit: contain;
  display: block;
}

.tips {
  margin-top: 8px;
  font-size: 13px;
  color: var(--muted);
}

.error {
  margin: 8px 0;
  border-radius: 12px;
  padding: 8px 10px;
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.2);
  color: var(--danger);
  font-size: 13px;
}

.submitBtn {
  width: 100%;
  margin-top: 12px;
  border-radius: 12px;
}

@media (max-width: 820px) {
  .hero {
    flex-direction: column;
  }

  .hero h1 {
    font-size: 24px;
  }

  .heroTips {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .gridTwo {
    grid-template-columns: 1fr;
  }
}
</style>

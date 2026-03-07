<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CATEGORY_OPTIONS, CONDITION_OPTIONS, createItem, uploadFile } from '../services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const categoryOptions = computed(() => CATEGORY_OPTIONS.filter((item) => item.value))
const conditionOptions = computed(() => CONDITION_OPTIONS.filter((item) => item.value))

const title = ref('')
const description = ref('')
const category = ref('BOOK')
const price = ref(0)
const conditionLevel = ref('LIKE_NEW')
const coverImageUrl = ref('')

const coverFile = ref(null)
const uploading = ref(false)
const uploadError = ref('')

const loading = ref(false)
const errorMsg = ref('')

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
      coverImageUrl: coverImageUrl.value,
    })
    const id = resp?.data?.id
    ElMessage.success('发布成功，商品已上架')
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
    <section class="hero surfacePanel">
      <div class="heroMain">
        <div class="heroTag">PUBLISH ITEM</div>
        <h1>发布你的闲置好物</h1>
        <p>信息越完整，越容易快速成交。提交后会直接上架并展示到首页。</p>
        <div class="heroTips">
          <span>建议上传清晰实拍图</span>
          <span>描述真实使用情况</span>
          <span>价格填写更直观</span>
        </div>
      </div>

      <div class="heroStats">
        <div class="statCard">
          <strong>基础信息</strong>
          <span>标题、价格、分类一站完成</span>
        </div>
        <div class="statCard">
          <strong>封面展示</strong>
          <span>支持直接填写链接或上传本地图</span>
        </div>
      </div>
    </section>

    <el-card class="card" shadow="hover">
      <div class="cardHead">
        <div>
          <div class="cardEyebrow">商品信息填写</div>
          <div class="cardTitle">完善发布内容</div>
        </div>
        <div class="cardHint">提交后立即上架</div>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <div class="sectionBlock">
          <div class="sectionTitle">基础信息</div>

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
        </div>

        <div class="dividerLine"></div>

        <div class="sectionBlock uploadSection">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">上传本地封面图</div>
              <div class="sectionDesc">上传成功后会自动填入封面图 URL，用于首页和详情页展示。</div>
            </div>
            <div class="sectionChip">可选步骤</div>
          </div>

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
        </div>

        <div v-if="errorMsg" class="error submitError">{{ errorMsg }}</div>

        <el-button class="submitBtn" type="primary" size="large" :loading="loading" @click="submit">
          {{ loading ? '提交中...' : '发布商品' }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.publishPage {
  max-width: 1040px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.surfacePanel {
  position: relative;
  overflow: hidden;
}

.hero {
  padding: 26px;
  border-radius: 26px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 28%),
    linear-gradient(135deg, #1d4ed8, #0284c7 62%, #06b6d4);
  color: #eff6ff;
  box-shadow: 0 20px 42px rgba(30, 64, 175, 0.2);
}

.heroTag {
  display: inline-flex;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  letter-spacing: 0.06em;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.hero h1 {
  margin: 16px 0 10px;
  font-size: 34px;
  line-height: 1.16;
}

.hero p {
  margin: 0;
  max-width: 540px;
  color: rgba(239, 246, 255, 0.94);
  line-height: 1.8;
}

.heroTips {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.heroTips span {
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.heroStats {
  display: grid;
  gap: 12px;
  align-content: center;
}

.statCard {
  min-height: 110px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
}

.statCard strong {
  display: block;
  font-size: 16px;
  color: #f8fafc;
}

.statCard span {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(239, 246, 255, 0.92);
}

.card {
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.cardHead {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.cardEyebrow {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.06em;
}

.cardTitle {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 900;
  color: #0f172a;
}

.cardHint {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.sectionBlock {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.sectionTitle {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.gridOne {
  display: grid;
  grid-template-columns: 1fr;
}

.gridTwo {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.dividerLine {
  margin: 18px 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(148, 163, 184, 0), rgba(148, 163, 184, 0.28), rgba(148, 163, 184, 0));
}

.sectionHeader {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.sectionDesc {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.sectionChip {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.08);
  color: #0369a1;
  font-size: 12px;
  font-weight: 700;
}

.uploadBar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.file {
  max-width: 100%;
}

.preview {
  margin-top: 14px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #f8fafc;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.preview img {
  width: 100%;
  max-height: 420px;
  object-fit: contain;
  display: block;
}

.error {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.18);
  color: #b91c1c;
  font-size: 13px;
}

.submitError {
  margin-bottom: 14px;
}

.submitBtn {
  margin-top: 20px;
  width: 100%;
  min-height: 48px;
  border-radius: 16px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .cardHead,
  .sectionHeader {
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .hero {
    padding: 20px;
    border-radius: 22px;
  }

  .hero h1 {
    font-size: 28px;
  }

  .card,
  .sectionBlock {
    border-radius: 20px;
  }

  .gridTwo {
    grid-template-columns: 1fr;
  }
}
</style>

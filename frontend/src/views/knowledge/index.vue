<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listDocumentsApi,
  uploadDocumentApi,
  deleteDocumentApi,
  searchKnowledgeApi
} from '@/api/knowledge'

const documents = ref([])
const loading = ref(false)

const categories = [
  { value: 'policy', label: '制度规范' },
  { value: 'faq', label: '常见问题' },
  { value: 'process', label: '办事流程' },
  { value: 'other', label: '其他' }
]
const categoryMap = Object.fromEntries(categories.map(c => [c.value, c.label]))

const statusMap = {
  READY: { text: '已就绪', type: 'success' },
  PROCESSING: { text: '解析中', type: 'warning' },
  FAILED: { text: '解析失败', type: 'danger' }
}

const formatTime = (v) => {
  if (!v) return '-'
  return String(v).replace('T', ' ').substring(0, 19)
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const r = await listDocumentsApi()
    if (r.code === 1) documents.value = r.data || []
    else ElMessage.error(r.msg || '加载失败')
  } finally {
    loading.value = false
  }
}

// ---------- 上传 ----------
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadCategory = ref('policy')

const openUpload = () => {
  uploadCategory.value = 'policy'
  uploadVisible.value = true
}

const doUpload = async (options) => {
  const fd = new FormData()
  fd.append('file', options.file)
  fd.append('category', uploadCategory.value)
  uploading.value = true
  try {
    const r = await uploadDocumentApi(fd)
    if (r.code === 1) {
      ElMessage.success(`「${r.data.fileName}」解析完成`)
      uploadVisible.value = false
      loadDocuments()
    } else {
      ElMessage.error(r.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败：' + (e.message || '网络异常'))
  } finally {
    uploading.value = false
  }
}

const delDocument = (row) => {
  ElMessageBox.confirm(
    `确定删除「${row.fileName}」吗？该文档的 ${row.chunkCount || 0} 个切片会一并删除。`,
    '删除文档',
    { type: 'warning' }
  ).then(async () => {
    const r = await deleteDocumentApi(row.id)
    if (r.code === 1) {
      ElMessage.success('已删除')
      loadDocuments()
    } else {
      ElMessage.error(r.msg || '删除失败')
    }
  }).catch(() => ElMessage.info('取消删除'))
}

// ---------- 检索预览 ----------
const searchVisible = ref(false)
const searchQ = ref('')
const searching = ref(false)
const searchResults = ref([])

const openSearch = () => {
  searchVisible.value = true
  searchResults.value = []
  searchQ.value = ''
}

const doSearch = async () => {
  if (!searchQ.value.trim()) return ElMessage.warning('请输入检索内容')
  searching.value = true
  try {
    const r = await searchKnowledgeApi(searchQ.value.trim())
    if (r.code === 1) {
      searchResults.value = r.data || []
      if (!searchResults.value.length) ElMessage.info('没有命中任何切片')
    } else {
      ElMessage.error(r.msg || '检索失败')
    }
  } finally {
    searching.value = false
  }
}

onMounted(loadDocuments)
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>知识库管理</h2>
        <span class="page-subtitle">AI 回答制度类问题时会优先检索这里的文档</span>
      </div>
      <div>
        <el-button class="btn-add" @click="openUpload">+ 上传文档</el-button>
        <el-button @click="openSearch">检索预览</el-button>
        <el-button @click="loadDocuments">刷新</el-button>
      </div>
    </div>

    <div class="table-card" style="margin-top: 16px">
      <el-table v-loading="loading" :data="documents" border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
        <el-table-column label="分类" width="110" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ categoryMap[row.category] || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="90" align="center" />
        <el-table-column label="上传时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="delDocument(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !documents.length" description="还没有文档，点右上角上传制度文件" />
    </div>

    <!-- 上传 -->
    <el-dialog v-model="uploadVisible" title="上传知识库文档" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="文档分类">
          <el-select v-model="uploadCategory" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            drag
            :show-file-list="false"
            :http-request="doUpload"
            :disabled="uploading"
            accept=".txt,.md,.docx,.pdf"
            style="width: 100%"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 txt / md / docx / pdf，单文件不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false" :disabled="uploading">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 检索预览 -->
    <el-dialog v-model="searchVisible" title="检索预览" width="720px">
      <div style="display: flex; gap: 12px; margin-bottom: 16px">
        <el-input v-model="searchQ" placeholder="输入问题，看看会命中哪些切片" @keydown.enter="doSearch" clearable />
        <el-button type="primary" :loading="searching" @click="doSearch">检索</el-button>
      </div>

      <div v-if="searchResults.length" class="chunk-list">
        <div v-for="(c, i) in searchResults" :key="i" class="chunk-item">
          <div class="chunk-head">
            <span class="chunk-index">片段 {{ c.chunkIndex }}</span>
            <el-tag size="small" effect="plain">得分 {{ Number(c.score || 0).toFixed(2) }}</el-tag>
          </div>
          <div class="chunk-content">{{ c.content }}</div>
        </div>
      </div>
      <el-empty v-else description="输入问题后点击检索" :image-size="80" />
    </el-dialog>
  </div>
</template>

<style scoped>
.page-subtitle {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}

.chunk-list {
  max-height: 460px;
  overflow-y: auto;
}

.chunk-item {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  background: #fafafa;
}

.chunk-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chunk-index {
  font-size: 12px;
  color: #888;
}

.chunk-content {
  font-size: 13px;
  line-height: 1.7;
  color: #333;
  white-space: pre-wrap;
}
</style>

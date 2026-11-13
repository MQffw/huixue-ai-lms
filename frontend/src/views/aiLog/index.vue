<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAiLogsApi, aiLogDetailApi } from '@/api/aiLog'

const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filters = ref({ keyword: '', intent: '', status: '' })

const intentOptions = [
  { value: 'DATA_STATS', label: '数据统计' },
  { value: 'DATA_QUERY', label: '数据查询' },
  { value: 'KNOWLEDGE_RAG', label: '知识库问答' },
  { value: 'TEXT_GEN', label: '文本生成' },
  { value: 'CHAT', label: '通用对话' },
  { value: 'GREETING', label: '寒暄' }
]
const intentMap = Object.fromEntries(intentOptions.map(i => [i.value, i.label]))

const fmt = (v) => (v ? String(v).replace('T', ' ').substring(0, 19) : '-')

const load = async () => {
  loading.value = true
  try {
    const r = await pageAiLogsApi({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.value.keyword || undefined,
      intent: filters.value.intent || undefined,
      status: filters.value.status || undefined
    })
    if (r.code === 1) {
      rows.value = r.data.rows || []
      total.value = r.data.total || 0
    } else {
      ElMessage.error(r.msg || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const reset = () => {
  filters.value = { keyword: '', intent: '', status: '' }
  page.value = 1
  load()
}

// ---------- 详情 ----------
const drawerVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

const openDetail = async (row) => {
  drawerVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    const r = await aiLogDetailApi(row.traceId)
    if (r.code === 1) detail.value = r.data
    else ElMessage.error(r.msg || '详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const pretty = (json) => {
  if (!json) return '-'
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch (e) {
    return json
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>AI 调用日志</h2>
        <span class="page-subtitle">每次对话一条 trace，可下钻到工具调用链</span>
      </div>
      <div>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true">
        <el-form-item label="问题关键字">
          <el-input v-model="filters.keyword" placeholder="模糊匹配问题" clearable style="width: 220px" @keydown.enter="search" />
        </el-form-item>
        <el-form-item label="意图">
          <el-select v-model="filters.intent" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="i in intentOptions" :key="i.value" :label="i.label" :value="i.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">清空</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="时间" width="170" align="center">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="userId" label="用户" width="70" align="center" />
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column label="意图" width="110" align="center">
          <template #default="{ row }">{{ intentMap[row.intent] || row.intent || '-' }}</template>
        </el-table-column>
        <el-table-column prop="modelType" label="模型" width="100" align="center" />
        <el-table-column prop="toolCalls" label="工具" width="70" align="center" />
        <el-table-column label="RAG" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.ragHit" size="small" type="success">命中</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ row.latencyMs }} ms</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="search"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="调用详情" size="520px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Trace ID">{{ detail.trace_id }}</el-descriptions-item>
            <el-descriptions-item label="问题">{{ detail.question }}</el-descriptions-item>
            <el-descriptions-item label="意图">{{ intentMap[detail.intent] || detail.intent }}</el-descriptions-item>
            <el-descriptions-item label="模型">{{ detail.model_type }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detail.latency_ms }} ms</el-descriptions-item>
            <el-descriptions-item label="工具调用次数">{{ detail.tool_calls }}</el-descriptions-item>
            <el-descriptions-item label="RAG 命中">{{ detail.rag_hit ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.error_msg" label="错误">{{ detail.error_msg }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ fmt(detail.create_time) }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">工具调用</h4>
          <el-table :data="detail.toolLogs || []" border size="small">
            <el-table-column prop="toolName" label="工具" min-width="160" />
            <el-table-column prop="durationMs" label="耗时(ms)" width="90" align="center" />
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                  {{ row.success ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="callTime" label="时间" width="160" align="center" />
          </el-table>

          <h4 class="section-title">上下文快照</h4>
          <pre class="json-box">{{ pretty(detail.detail_json) }}</pre>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-subtitle {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}

.section-title {
  margin: 20px 0 10px;
  font-size: 14px;
  color: #333;
}

.json-box {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>

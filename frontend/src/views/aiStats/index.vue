<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const overview = ref({
  totalTokens: 0,
  totalPromptTokens: 0,
  totalCompletionTokens: 0,
  totalSessions: 0,
  totalUsers: 0
})

const hourlyData = ref([])
const modelData = ref([])
const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChart = null
let pieChart = null

const formatNumber = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

const fetchOverview = async () => {
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')
    const resp = await fetch('/api/ai-stats/overview', {
      headers: { 'token': loginUser.token || '' }
    })
    const json = await resp.json()
    if (json.code === 200 || json.code === 1) {
      overview.value = json.data
    }
  } catch (e) { /* 静默 */ }
}

const fetchHourlyData = async () => {
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')
    const resp = await fetch('/api/ai-stats/tokens-24h', {
      headers: { 'token': loginUser.token || '' }
    })
    const json = await resp.json()
    if (json.code === 200 || json.code === 1) {
      hourlyData.value = json.data
      renderBarChart()
    }
  } catch (e) { /* 静默 */ }
}

const fetchModelData = async () => {
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')
    const resp = await fetch('/api/ai-stats/model-distribution', {
      headers: { 'token': loginUser.token || '' }
    })
    const json = await resp.json()
    if (json.code === 200 || json.code === 1) {
      modelData.value = json.data
      renderPieChart()
    }
  } catch (e) { /* 静默 */ }
}

const renderBarChart = () => {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const hours = hourlyData.value.map(d => d.hour)
  const totals = hourlyData.value.map(d => d.totalTokens || 0)

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const d = hourlyData.value[params[0].dataIndex]
        return `${params[0].axisValue}<br/>
          Total: <b>${formatNumber(d.totalTokens || 0)}</b><br/>
          Prompt: ${formatNumber(d.promptTokens || 0)}<br/>
          Completion: ${formatNumber(d.completionTokens || 0)}`
      }
    },
    grid: { top: 30, right: 20, bottom: 40, left: 60 },
    xAxis: {
      type: 'category',
      data: hours,
      axisLabel: { color: '#666', fontSize: 11 },
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666',
        formatter: (v) => formatNumber(v)
      },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      type: 'bar',
      data: totals,
      barWidth: '60%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#007fa4' },
          { offset: 1, color: '#00d072' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#00a5cc' },
            { offset: 1, color: '#00e882' }
          ])
        }
      }
    }]
  })
}

const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  const colors = ['#007fa4', '#00d072', '#5470c6', '#91cc75', '#fac858', '#ee6666']

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>Tokens: <b>{c}</b><br/>占比: <b>{d}%</b>'
    },
    legend: {
      orient: 'vertical',
      right: 20,
      top: 'center',
      textStyle: { color: '#555', fontSize: 13 }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}\n{d}%',
        fontSize: 11,
        color: '#555'
      },
      labelLine: { length: 10, length2: 15 },
      data: modelData.value.map((d, i) => ({
        name: d.model || 'unknown',
        value: d.tokenCount || 0,
        sessionCount: d.sessionCount || 0,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })
}

const refreshAll = async () => {
  await Promise.all([fetchOverview(), fetchHourlyData(), fetchModelData()])
}

const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await nextTick()
  await nextTick()
  await refreshAll()
  window.addEventListener('resize', handleResize)
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>AI 用量监控</h2>
        <div class="page-subtitle">Token 消耗统计 · 模型分布 · 会话分析</div>
      </div>
    </div>

    <!-- 顶部概览卡片 -->
    <div class="overview-row">
      <div class="stat-card total">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(overview.totalTokens) }}</div>
          <div class="stat-label">总 Token</div>
        </div>
      </div>
      <div class="stat-card prompt">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(overview.totalPromptTokens) }}</div>
          <div class="stat-label">Prompt Tokens</div>
        </div>
      </div>
      <div class="stat-card completion">
        <div class="stat-icon">💬</div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(overview.totalCompletionTokens) }}</div>
          <div class="stat-label">Completion Tokens</div>
        </div>
      </div>
      <div class="stat-card sessions">
        <div class="stat-icon">🗂️</div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalSessions || 0 }}</div>
          <div class="stat-label">总会话数</div>
        </div>
      </div>
      <div class="stat-card users">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalUsers || 0 }}</div>
          <div class="stat-label">用户数</div>
        </div>
      </div>
    </div>

    <!-- 中部图表 -->
    <div class="charts-row">
      <div class="chart-card bar-card">
        <div class="chart-header">
          <h3> 24 小时 Token 用量</h3>
          <el-button size="small" @click="fetchHourlyData" circle>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
        <div ref="barChartRef" class="chart-container"></div>
      </div>

      <div class="chart-card pie-card">
        <div class="chart-header">
          <h3> 模型 Token 占比</h3>
          <el-button size="small" @click="fetchModelData" circle>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
        <div ref="pieChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 底部模型明细表格 -->
    <div class="detail-card" v-if="modelData.length > 0">
      <div class="chart-header">
        <h3> 模型明细</h3>
      </div>
      <el-table :data="modelData" stripe style="width: 100%">
        <el-table-column prop="model" label="模型" min-width="160" />
        <el-table-column label="Token 数" min-width="140" align="right">
          <template #default="{ row }">
            <span class="token-cell">{{ formatNumber(row.tokenCount || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="会话数" min-width="100" align="right">
          <template #default="{ row }">
            {{ row.sessionCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="占比" min-width="180">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="modelData[0]?.tokenCount ? Math.round((row.tokenCount / modelData[0].tokenCount) * 100) : 0"
                :stroke-width="8"
                :color="['#007fa4', '#00b88a', '#00d072']"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
/* page-container 已在全局定义，此处不需要重复 */

/* 概览卡片 */
.overview-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  font-size: 32px;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #e6f7f5, #d4f1ec);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

/* 图表行 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chart-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.chart-container {
  width: 100%;
  height: 320px;
}

/* 明细表格 */
.detail-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.token-cell {
  font-weight: 600;
  color: #007fa4;
}

.progress-cell {
  padding: 0 10px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .overview-row {
    grid-template-columns: repeat(3, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

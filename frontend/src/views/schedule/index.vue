<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listScheduleApi } from '@/api/schedule'
import { queryAllApi as queryAllClazzApi } from '@/api/clazz'

const today = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const clazzOptions = ref([])
const clazzId = ref(null)
const date = ref(today())
const rows = ref([])
const loading = ref(false)

const loadClazz = async () => {
  try {
    const r = await queryAllClazzApi()
    if (r.code === 1) clazzOptions.value = r.data || []
  } catch (e) {
    console.warn('班级列表加载失败', e)
  }
}

const load = async () => {
  loading.value = true
  try {
    const params = { date: date.value }
    if (clazzId.value) params.clazzId = clazzId.value
    const r = await listScheduleApi(params)
    if (r.code === 1) rows.value = r.data || []
    else ElMessage.error(r.msg || '加载失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  clazzId.value = null
  date.value = today()
  load()
}

const hhmm = (t) => (t ? String(t).substring(0, 5) : '')

onMounted(async () => {
  await loadClazz()
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>课表查询</h2>
        <span class="page-subtitle">排课由教务导入，本系统只读</span>
      </div>
      <div>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true">
        <el-form-item label="日期">
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width: 160px" @change="load" />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="clazzId" placeholder="全部班级" clearable style="width: 200px" @change="load">
            <el-option v-for="c in clazzOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">今天</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="classDate" label="日期" width="120" align="center" />
        <el-table-column label="时间" width="140" align="center">
          <template #default="{ row }">{{ hhmm(row.startTime) }} - {{ hhmm(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="clazzName" label="班级" min-width="170" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" min-width="160" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="教师" width="110" align="center" />
        <el-table-column prop="room" label="教室" width="100" align="center" />
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="该日期暂无排课" />
    </div>
  </div>
</template>

<style scoped>
.page-subtitle {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}
</style>

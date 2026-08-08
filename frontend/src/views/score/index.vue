<script setup>
import { ref, onMounted, watch } from 'vue'
import { queryPageApi, addApi, updateApi, deleteApi, deleteBatchApi, queryExamRankingApi, queryExamStatsApi } from '@/api/score'
import { queryAllApi as queryAllExamApi } from '@/api/exam'
import { queryAllApi as queryAllStuApi } from '@/api/stu'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = ref({ examId: null, studentName: '' })
const tableData = ref([]); const examList = ref([]); const stuList = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  const params = { page: pagination.value.currentPage, pageSize: pagination.value.pageSize }
  if (searchForm.value.examId) params.examId = searchForm.value.examId
  if (searchForm.value.studentName) params.studentName = searchForm.value.studentName
  queryPageApi(params).then(r => {
    if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total }
  })
}
const clear = () => { searchForm.value = { examId: null, studentName: '' }; queryPage() }

const deleteByIds = () => {
  if (!selectIds.value.length) return ElMessage.warning('请选择成绩')
  ElMessageBox.confirm(`确定删除选中成绩？`, '提示', { type: 'warning' }).then(async () => {
    await deleteBatchApi(selectIds.value.join(',')); ElMessage.success('删除成功'); queryPage()
  }).catch(() => {})
}
const delById = (id) => ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => { await deleteApi(id); ElMessage.success('已删除'); queryPage() }).catch(() => {})

// --- 弹窗 ---
const dialogVisible = ref(false); const formTitle = ref('')
const score = ref({ id: null, examId: null, studentId: null, score: 0, remark: '' })
const rules = {
  examId: [{ required: true, message: '请选择考试', trigger: 'change' }],
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  score: [{ required: true, message: '请输入分数', trigger: 'blur' }]
}
const scoreFormRef = ref()
const addScore = () => { score.value = { id: null, examId: null, studentId: null, score: 0, remark: '' }; dialogVisible.value = true; formTitle.value = '新增成绩' }
const editScore = (row) => { score.value = { ...row }; dialogVisible.value = true; formTitle.value = '编辑成绩' }
// 选学员→自动显示班级
const selectedStuInfo = ref('')
watch(() => score.value.studentId, (newVal) => {
  if (newVal) {
    const s = stuList.value.find(s => s.id === newVal)
    selectedStuInfo.value = s ? (s.name + ' - ' + (s.clazzName || '')) : ''
  } else { selectedStuInfo.value = '' }
})

const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = score.value.id ? updateApi(score.value) : addApi(score.value)
      const r = await api
      if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}

onMounted(async () => {
  const er = await queryAllExamApi(); if (er.code) examList.value = er.data
  const sr = await queryAllStuApi(); if (sr.code) stuList.value = sr.data
  queryPage()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>成绩查询</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addScore()">+ 新增成绩</el-button>
        <el-button type="danger" @click="deleteByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="考试"><el-select v-model="searchForm.examId" clearable placeholder="全部" style="width:200px"><el-option v-for="e in examList" :key="e.id" :label="e.name" :value="e.id" /></el-select></el-form-item>
        <el-form-item label="学员"><el-input v-model="searchForm.studentName" clearable placeholder="姓名" style="width:140px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="studentName" label="学员" min-width="80" />
        <el-table-column prop="examName" label="考试" min-width="180" show-overflow-tooltip />
        <el-table-column prop="score" label="分数" width="90" align="center">
          <template #default="{ row }"><b :style="{ color: row.score < 60 ? '#E60012' : '#1a1a1a' }">{{ row.score }}</b></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="editScore(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" @size-change="queryPage" @current-change="queryPage" />

    <el-dialog v-model="dialogVisible" :title="formTitle" width="35%">
      <el-form :model="score" ref="scoreFormRef" :rules="rules" label-width="80px">
        <el-form-item label="考试" prop="examId"><el-select v-model="score.examId" style="width:100%"><el-option v-for="e in examList" :key="e.id" :label="e.name" :value="e.id" /></el-select></el-form-item>
        <el-form-item label="学员" prop="studentId"><el-select v-model="score.studentId" filterable style="width:100%"><el-option v-for="s in stuList" :key="s.id" :label="s.name + ' (' + (s.clazzName||'') + ')'" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="分数" prop="score"><el-input-number v-model="score.score" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="score.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save(scoreFormRef)">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
</style>

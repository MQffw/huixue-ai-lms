<script setup>
import { ref, onMounted, watch } from 'vue'
import { queryPageApi, queryClazzListApi, addApi, updateApi, deleteApi } from '@/api/attendance'
import { queryAllApi as queryAllStuApi } from '@/api/stu'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = { 1: '正常', 2: '迟到', 3: '早退', 4: '请假', 5: '旷课' }
const statusOpts = Object.entries(statusMap).map(([v, l]) => ({ value: Number(v), label: l }))
const statusTag = { 1: '', 2: 'warning', 3: 'warning', 4: 'info', 5: 'danger' }

const searchForm = ref({ clazzId: null, studentName: '', status: null, startDate: '', endDate: '' })
const tableData = ref([]); const clazzList = ref([]); const stuList = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  const params = { page: pagination.value.currentPage, pageSize: pagination.value.pageSize }
  const f = searchForm.value
  if (f.clazzId) params.clazzId = f.clazzId
  if (f.studentName) params.studentName = f.studentName
  if (f.status) params.status = f.status
  if (f.startDate) params.startDate = f.startDate
  if (f.endDate) params.endDate = f.endDate
  queryPageApi(params).then(r => {
    if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total }
  })
}
const clear = () => { searchForm.value = { clazzId: null, studentName: '', status: null, startDate: '', endDate: '' }; queryPage() }

// --- 新增/编辑（选学员自动带班级）---
const dialogVisible = ref(false); const formTitle = ref('')
const att = ref({ id: null, studentId: null, clazzId: null, attendDate: '', status: null, remark: '' })
const rules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  attendDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const attFormRef = ref()

// 选学员→自动带班级
watch(() => att.value.studentId, (newVal) => {
  if (newVal) {
    const s = stuList.value.find(s => s.id === newVal)
    if (s) att.value.clazzId = s.clazzId
  }
})

const addAtt = () => { att.value = { id: null, studentId: null, clazzId: null, attendDate: new Date().toISOString().slice(0,10), status: null, remark: '' }; dialogVisible.value = true; formTitle.value = '新增考勤' }
const editAtt = (row) => { att.value = { ...row }; dialogVisible.value = true; formTitle.value = '编辑考勤' }
const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = att.value.id ? updateApi(att.value) : addApi(att.value)
      const r = await api
      if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}
const delById = (id) => ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => { await deleteApi(id); ElMessage.success('已删除'); queryPage() }).catch(() => {})
const delByIds = () => {
  if (selectIds.value.length === 0) { ElMessage.warning('请选择要删除的记录'); return }
  ElMessageBox.confirm('确定删除选中的考勤记录吗？', '批量删除', { type: 'warning' }).then(async () => {
    await deleteApi(selectIds.value.join(','))
    ElMessage.success('批量删除成功')
    queryPage()
  }).catch(() => {})
}

onMounted(async () => {
  const cr = await queryClazzListApi(); if (cr.code) clazzList.value = cr.data
  const sr = await queryAllStuApi(); if (sr.code) stuList.value = sr.data
  queryPage()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>考勤记录</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addAtt()">+ 新增考勤</el-button>
        <el-button type="danger" @click="delByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="班级"><el-select v-model="searchForm.clazzId" clearable placeholder="全部" style="width:160px"><el-option v-for="c in clazzList" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="学员"><el-input v-model="searchForm.studentName" placeholder="姓名" clearable style="width:140px" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="searchForm.status" clearable placeholder="全部" style="width:100px"><el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="searchForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始" style="width:140px" /> - <el-date-picker v-model="searchForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束" style="width:140px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="studentName" label="学员" min-width="80" />
        <el-table-column prop="clazzName" label="班级" min-width="110" />
        <el-table-column prop="attendDate" label="日期" width="110" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="statusTag[row.status]" size="small">{{ statusMap[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="editAtt(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" @size-change="queryPage" @current-change="queryPage" />

    <el-dialog v-model="dialogVisible" :title="formTitle" width="35%">
      <el-form :model="att" ref="attFormRef" :rules="rules" label-width="80px">
        <el-form-item label="学员" prop="studentId"><el-select v-model="att.studentId" filterable style="width:100%"><el-option v-for="s in stuList" :key="s.id" :label="s.name + ' (' + (s.clazzName||'') + ')'" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="班级"><el-input :model-value="clazzList.find(c=>c.id===att.clazzId)?.name||''" disabled /></el-form-item>
        <el-form-item label="日期" prop="attendDate"><el-date-picker v-model="att.attendDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="att.status" style="width:100%"><el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="att.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save(attFormRef)">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
</style>

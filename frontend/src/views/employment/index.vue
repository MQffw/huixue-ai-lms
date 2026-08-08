<script setup>
import { ref, onMounted, watch } from 'vue'
import { queryPageApi, addApi, updateApi, deleteApi, deleteBatchApi } from '@/api/employment'
import { queryAllApi as queryAllClazzApi } from '@/api/clazz'
import { queryAllApi as queryAllStuApi } from '@/api/stu'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = { 1: '在职', 2: '已离职', 3: '试用期' }
const statusOpts = Object.entries(statusMap).map(([v, l]) => ({ value: Number(v), label: l }))
const statusTag = { 1: '', 2: 'info', 3: 'warning' }

const searchForm = ref({ clazzId: null, status: null, studentName: '' })
const tableData = ref([]); const clazzList = ref([]); const stuList = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  const p = { page: pagination.value.currentPage, pageSize: pagination.value.pageSize }
  if (searchForm.value.clazzId) p.clazzId = searchForm.value.clazzId
  if (searchForm.value.status) p.status = searchForm.value.status
  if (searchForm.value.studentName) p.studentName = searchForm.value.studentName
  queryPageApi(p).then(r => { if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total } })
}
const clear = () => { searchForm.value = { clazzId: null, status: null, studentName: '' }; queryPage() }

const deleteByIds = () => {
  if (!selectIds.value.length) return ElMessage.warning('请选择记录')
  ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteBatchApi(selectIds.value.join(',')); ElMessage.success('已删除'); queryPage()
  }).catch(() => {})
}
const delById = (id) => ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => { await deleteApi(id); ElMessage.success('已删除'); queryPage() }).catch(() => {})

// --- 弹窗 ---
const dialogVisible = ref(false); const formTitle = ref(''); const formRef = ref()
const emp = ref({ id: null, studentId: null, clazzId: null, company: '', position: '', salary: 0, city: '', employmentDate: '', status: 1 })
const rules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  company: [{ required: true, message: '请输入公司', trigger: 'blur' }],
  position: [{ required: true, message: '请输入职位', trigger: 'blur' }],
  salary: [{ required: true, message: '请输入薪资', trigger: 'blur' }]
}
const addEmp = () => {
  emp.value = { id: null, studentId: null, clazzId: null, company: '', position: '', salary: 0, city: '', employmentDate: new Date().toISOString().slice(0,10), status: 3 }
  dialogVisible.value = true; formTitle.value = '新增就业'
}
const editEmp = (row) => { emp.value = { ...row }; dialogVisible.value = true; formTitle.value = '编辑就业' }
// 选学员→自动带班级
watch(() => emp.value.studentId, (newVal) => {
  if (newVal) {
    const s = stuList.value.find(s => s.id === newVal)
    if (s) emp.value.clazzId = s.clazzId
  }
})

const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = emp.value.id ? updateApi(emp.value) : addApi(emp.value)
      const r = await api; if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}

onMounted(async () => {
  const cr = await queryAllClazzApi(); if (cr.code) clazzList.value = cr.data
  const sr = await queryAllStuApi(); if (sr.code) stuList.value = sr.data
  queryPage()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>就业管理</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addEmp()">+ 新增就业</el-button>
        <el-button type="danger" @click="deleteByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="班级"><el-select v-model="searchForm.clazzId" clearable placeholder="全部" style="width:160px"><el-option v-for="c in clazzList" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="学员"><el-input v-model="searchForm.studentName" clearable placeholder="姓名" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="searchForm.status" clearable placeholder="全部" style="width:100px"><el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="studentName" label="学员" min-width="80" />
        <el-table-column prop="clazzName" label="班级" min-width="100" />
        <el-table-column prop="company" label="公司" min-width="120" />
        <el-table-column prop="position" label="职位" />
        <el-table-column label="薪资" width="120" align="center">
          <template #default="{ row }"><b>¥{{ (row.salary || 0).toLocaleString() }}</b></template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="statusTag[row.status]" size="small">{{ statusMap[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="editEmp(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" @size-change="queryPage" @current-change="queryPage" />

    <el-dialog v-model="dialogVisible" :title="formTitle" width="40%">
      <el-form :model="emp" ref="formRef" :rules="rules" label-width="80px">
        <el-form-item label="学员" prop="studentId"><el-select v-model="emp.studentId" filterable style="width:100%"><el-option v-for="s in stuList" :key="s.id" :label="s.name + ' (' + (s.clazzName||'') + ')'" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="班级"><el-input :model-value="clazzList.find(c=>c.id===emp.clazzId)?.name||''" disabled /></el-form-item>
        <el-form-item label="公司" prop="company"><el-input v-model="emp.company" /></el-form-item>
        <el-form-item label="职位" prop="position"><el-input v-model="emp.position" /></el-form-item>
        <el-form-item label="薪资" prop="salary"><el-input-number v-model="emp.salary" :min="0" :step="500" style="width:100%" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="emp.city" /></el-form-item>
        <el-form-item label="入职日期"><el-date-picker v-model="emp.employmentDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="emp.status" style="width:100%"><el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save(formRef)">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
</style>

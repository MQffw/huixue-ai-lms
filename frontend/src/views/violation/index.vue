<script setup>
import { ref, onMounted, watch } from 'vue'
import { queryPageApi, addApi, updateApi, deleteApi, deleteBatchApi } from '@/api/violation'
import { queryAllApi as queryAllStuApi } from '@/api/stu'
import { ElMessage, ElMessageBox } from 'element-plus'

const violationTypes = ['迟到', '旷课', '早退', '作弊', '打架', '抽烟', '辱骂', '其他']
const searchForm = ref({ studentName: '', violationType: '', startDate: '', endDate: '' })
const tableData = ref([]); const stuList = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  const p = { page: pagination.value.currentPage, pageSize: pagination.value.pageSize }
  if (searchForm.value.studentName) p.studentName = searchForm.value.studentName
  if (searchForm.value.violationType) p.violationType = searchForm.value.violationType
  if (searchForm.value.startDate) p.startDate = searchForm.value.startDate
  if (searchForm.value.endDate) p.endDate = searchForm.value.endDate
  queryPageApi(p).then(r => { if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total } })
}
const clear = () => { searchForm.value = { studentName: '', violationType: '', startDate: '', endDate: '' }; queryPage() }

const deleteByIds = () => {
  if (!selectIds.value.length) return ElMessage.warning('请选择记录')
  ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteBatchApi(selectIds.value.join(',')); ElMessage.success('已删除'); queryPage()
  }).catch(() => {})
}
const delById = (id) => ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => { await deleteApi(id); ElMessage.success('已删除'); queryPage() }).catch(() => {})

// --- 弹窗 ---
const dialogVisible = ref(false); const formTitle = ref(''); const formRef = ref()
const v = ref({ id: null, studentId: null, violationType: '迟到', violationDate: '', deductScore: 2, description: '' })
const rules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  violationType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  violationDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}
const addV = () => { v.value = { id: null, studentId: null, violationType: '迟到', violationDate: new Date().toISOString().slice(0,10), deductScore: 2, description: '' }; dialogVisible.value = true; formTitle.value = '新增违纪' }
const editV = (row) => { v.value = { ...row }; dialogVisible.value = true; formTitle.value = '编辑违纪' }
const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = v.value.id ? updateApi(v.value) : addApi(v.value)
      const r = await api; if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}

onMounted(async () => { const r = await queryAllStuApi(); if (r.code) stuList.value = r.data; queryPage() })
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>违纪记录</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addV()">+ 新增违纪</el-button>
        <el-button type="danger" @click="deleteByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="学员"><el-input v-model="searchForm.studentName" clearable placeholder="姓名" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="searchForm.violationType" clearable placeholder="全部" style="width:110px"><el-option v-for="t in violationTypes" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="searchForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始" style="width:140px" /> - <el-date-picker v-model="searchForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束" style="width:140px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="studentName" label="学员" min-width="80" />
        <el-table-column label="违纪类型" width="90" align="center">
          <template #default="{ row }"><el-tag :type="['作弊','打架'].includes(row.violationType)?'danger':'warning'" size="small">{{ row.violationType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="violationDate" label="日期" width="110" align="center" />
        <el-table-column label="扣分" width="70" align="center">
          <template #default="{ row }"><b style="color:#E60012">-{{ row.deductScore }}</b></template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column prop="handlerName" label="处理人" min-width="80" align="center" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="editV(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" @size-change="queryPage" @current-change="queryPage" />

    <el-dialog v-model="dialogVisible" :title="formTitle" width="38%">
      <el-form :model="v" ref="formRef" :rules="rules" label-width="80px">
        <el-form-item label="学员" prop="studentId"><el-select v-model="v.studentId" filterable style="width:100%"><el-option v-for="s in stuList" :key="s.id" :label="s.name + ' (' + (s.clazzName||'') + ')'" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="违纪类型" prop="violationType"><el-select v-model="v.violationType" style="width:100%"><el-option v-for="t in violationTypes" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="日期" prop="violationDate"><el-date-picker v-model="v.violationDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="扣分"><el-input-number v-model="v.deductScore" :min="1" :max="30" style="width:100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="v.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save(formRef)">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
</style>

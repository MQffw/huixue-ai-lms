<script setup>
import { ref, onMounted, watch } from 'vue'
import { queryPageApi, addApi, updateApi, deleteApi, deleteBatchApi } from '@/api/payment'
import { queryAllApi as queryAllStuApi } from '@/api/stu'
import { queryAllApi as queryAllClazzApi } from '@/api/clazz'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = { 1: '已缴费', 2: '待确认', 3: '已退款' }
const statusOpts = Object.entries(statusMap).map(([v, l]) => ({ value: Number(v), label: l }))
const statusTag = { 1: '', 2: 'warning', 3: 'danger' }
const payTypes = ['学费', '住宿费', '教材费', '押金']
const payMethods = ['现金', '微信', '支付宝', '银行转账']

const searchForm = ref({ studentName: '', status: null })
const tableData = ref([]); const stuList = ref([]); const clazzList = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  const params = { page: pagination.value.currentPage, pageSize: pagination.value.pageSize }
  if (searchForm.value.studentName) params.studentName = searchForm.value.studentName
  if (searchForm.value.status) params.status = searchForm.value.status
  queryPageApi(params).then(r => {
    if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total }
  })
}
const clear = () => { searchForm.value = { studentName: '', status: null }; queryPage() }

const deleteByIds = () => {
  if (!selectIds.value.length) return ElMessage.warning('请选择记录')
  ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteBatchApi(selectIds.value.join(',')); ElMessage.success('已删除'); queryPage()
  }).catch(() => {})
}
const delById = (id) => ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => { await deleteApi(id); ElMessage.success('已删除'); queryPage() }).catch(() => {})

// --- 弹窗 ---
const dialogVisible = ref(false); const formTitle = ref('')
const payment = ref({ id: null, studentId: null, amount: 0, paymentType: '学费', paymentMethod: '微信', paymentDate: '', status: 1, remark: '' })
const rules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  paymentDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}
const formRef = ref()

// 选学员→自动显示班级
const selectedClazzName = ref('')
watch(() => payment.value.studentId, (newVal) => {
  if (newVal) {
    const s = stuList.value.find(s => s.id === newVal)
    selectedClazzName.value = s ? (s.clazzName || '') : ''
  } else { selectedClazzName.value = '' }
})

const addPayment = () => { payment.value = { id: null, studentId: null, amount: 0, paymentType: '学费', paymentMethod: '微信', paymentDate: new Date().toISOString().slice(0,10), status: 1, remark: '' }; selectedClazzName.value = ''; dialogVisible.value = true; formTitle.value = '新增缴费' }
const editPayment = (row) => {
  payment.value = { ...row }
  const s = stuList.value.find(s => s.id === row.studentId)
  selectedClazzName.value = s ? (s.clazzName || '') : ''
  dialogVisible.value = true; formTitle.value = '编辑缴费'
}
const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = payment.value.id ? updateApi(payment.value) : addApi(payment.value)
      const r = await api
      if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}

onMounted(async () => {
  const sr = await queryAllStuApi(); if (sr.code) stuList.value = sr.data
  const cr = await queryAllClazzApi(); if (cr.code) clazzList.value = cr.data
  queryPage()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>缴费管理</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addPayment()">+ 新增缴费</el-button>
        <el-button type="danger" @click="deleteByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="学员"><el-input v-model="searchForm.studentName" clearable placeholder="姓名" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="searchForm.status" clearable placeholder="全部" style="width:120px"><el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="studentName" label="学员" min-width="80" />
        <el-table-column prop="clazzName" label="班级" min-width="100" />
        <el-table-column prop="paymentDate" label="日期" width="110" align="center" />
        <el-table-column prop="paymentType" label="费用类型" width="90" align="center" />
        <el-table-column label="金额" width="120" align="center">
          <template #default="{ row }"><b>¥{{ row.amount?.toLocaleString() }}</b></template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="方式" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="statusTag[row.status]" size="small">{{ statusMap[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="editPayment(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" @size-change="queryPage" @current-change="queryPage" />

    <el-dialog v-model="dialogVisible" :title="formTitle" width="38%">
      <el-form :model="payment" ref="formRef" :rules="rules" label-width="80px">
        <el-form-item label="学员" prop="studentId"><el-select v-model="payment.studentId" filterable style="width:100%"><el-option v-for="s in stuList" :key="s.id" :label="s.name + ' (' + (s.clazzName||'') + ')'" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="班级"><el-input :model-value="selectedClazzName" disabled /></el-form-item>
        <el-form-item label="费用类型"><el-select v-model="payment.paymentType" style="width:100%"><el-option v-for="t in payTypes" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="金额" prop="amount"><el-input-number v-model="payment.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="方式"><el-select v-model="payment.paymentMethod" style="width:100%"><el-option v-for="m in payMethods" :key="m" :label="m" :value="m" /></el-select></el-form-item>
        <el-form-item label="日期" prop="paymentDate"><el-date-picker v-model="payment.paymentDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="payment.status" style="width:100%"><el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="payment.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save(formRef)">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
</style>

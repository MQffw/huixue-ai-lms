<script setup>
import { ref, onMounted } from 'vue'
import { queryPageApi, addApi, queryInfoApi, updateApi, deleteApi, deleteBatchApi } from '@/api/course'
import { ElMessage, ElMessageBox } from 'element-plus'

const subjects = [{ name: 'Java', value: 1 },{ name: '前端', value: 2 },{ name: '大数据', value: 3 },{ name: 'Python', value: 4 },{ name: 'Go', value: 5 },{ name: '嵌入式', value: 6 }]
const subMap = Object.fromEntries(subjects.map(s => [s.value, s.name]))

const searchForm = ref({ name: '', subject: null })
const tableData = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  queryPageApi(searchForm.value.name || undefined, searchForm.value.subject || undefined, pagination.value.currentPage, pagination.value.pageSize).then(r => {
    if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total }
  })
}
const clear = () => { searchForm.value = { name: '', subject: null }; queryPage() }

const deleteByIds = () => {
  if (!selectIds.value.length) return ElMessage.warning('请选择课程')
  ElMessageBox.confirm(`确定删除选中的 ${selectIds.value.length} 门课程吗？关联排课记录也将删除。`, '提示', { type: 'warning' }).then(async () => {
    await deleteBatchApi(selectIds.value.join(','))
    ElMessage.success('删除成功'); queryPage()
  }).catch(() => ElMessage.info('取消删除'))
}

const delById = (id) => {
  ElMessageBox.confirm('确定删除该课程吗？关联排课记录也将删除。', '删除课程', { type: 'warning' }).then(async () => {
    await deleteApi(id); ElMessage.success('删除成功'); queryPage()
  }).catch(() => ElMessage.info('取消删除'))
}

// --- 新增/编辑弹窗 ---
const dialogVisible = ref(false)
const formTitle = ref('')
const courseFormRef = ref()
const course = ref({ id: null, name: '', subject: null, hours: 80, description: '' })
const rules = {
  name: [{ required: true, message: '课程名称为必填项', trigger: 'blur' }, { min: 2, max: 50, message: '2-50字', trigger: 'blur' }],
  subject: [{ required: true, message: '请选择学科', trigger: 'change' }],
  hours: [{ required: true, message: '课时数不能为空', trigger: 'blur' }]
}

const resetForm = (f) => { if (f) f.resetFields() }
const addCourse = () => { course.value = { id: null, name: '', subject: null, hours: 80, description: '' }; dialogVisible.value = true; formTitle.value = '新增课程' }
const editCourse = async (id) => {
  const r = await queryInfoApi(id)
  if (r.code) { course.value = r.data; dialogVisible.value = true; formTitle.value = '编辑课程' }
}
const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = course.value.id ? updateApi(course.value) : addApi(course.value)
      const r = await api
      if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}

onMounted(() => queryPage())
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>课程管理</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addCourse(); resetForm(courseFormRef)">+ 新增课程</el-button>
        <el-button type="danger" @click="deleteByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="课程名称"><el-input v-model="searchForm.name" placeholder="请输入" clearable /></el-form-item>
        <el-form-item label="学科">
          <el-select v-model="searchForm.subject" placeholder="全部" clearable style="width:140px">
            <el-option v-for="s in subjects" :key="s.value" :label="s.name" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="180" />
        <el-table-column label="学科" width="100" align="center">
          <template #default="{ row }">{{ subMap[row.subject] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="课时" width="90" align="center">
          <template #default="{ row }">{{ row.hours }}课时</template>
        </el-table-column>
        <el-table-column prop="description" label="课程简介" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="editCourse(row.id); resetForm(courseFormRef)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
      :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total"
      @size-change="queryPage" @current-change="queryPage" />

    <el-dialog v-model="dialogVisible" :title="formTitle" width="40%">
      <el-form :model="course" ref="courseFormRef" :rules="rules" label-width="80px">
        <el-form-item label="课程名称" prop="name"><el-input v-model="course.name" /></el-form-item>
        <el-form-item label="所属学科" prop="subject">
          <el-select v-model="course.subject" style="width:100%">
            <el-option v-for="s in subjects" :key="s.value" :label="s.name" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="课时数" prop="hours"><el-input-number v-model="course.hours" :min="1" :max="500" style="width:100%" /></el-form-item>
        <el-form-item label="课程简介"><el-input v-model="course.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false; resetForm(courseFormRef)">取消</el-button>
        <el-button type="primary" @click="save(courseFormRef)">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>

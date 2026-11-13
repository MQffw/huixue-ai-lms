<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExamsApi, examsByClazzApi, addExamApi, updateExamApi, deleteExamApi } from '@/api/exam'
import { queryAllApi as queryAllClazzApi } from '@/api/clazz'
import { queryAllApi as queryAllCourseApi } from '@/api/course'

const exams = ref([])
const clazzOptions = ref([])
const courseOptions = ref([])
const filterClazzId = ref(null)
const loading = ref(false)

const dialogVisible = ref(false)
const formTitle = ref('新增考试')
const saving = ref(false)
const formRef = ref()
const form = ref({ id: null, name: '', clazzId: null, courseId: null, examDate: '', fullScore: 100, passScore: 60 })

const rules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  clazzId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  fullScore: [{ required: true, message: '请输入满分', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入及格分', trigger: 'blur' }]
}

const loadOptions = async () => {
  try {
    const [c, co] = await Promise.all([queryAllClazzApi(), queryAllCourseApi()])
    if (c.code === 1) clazzOptions.value = c.data || []
    if (co.code === 1) courseOptions.value = co.data || []
  } catch (e) {
    console.warn('下拉数据加载失败', e)
  }
}

const loadExams = async () => {
  loading.value = true
  try {
    const r = filterClazzId.value
      ? await examsByClazzApi(filterClazzId.value)
      : await listExamsApi()
    if (r.code === 1) exams.value = r.data || []
    else ElMessage.error(r.msg || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = { id: null, name: '', clazzId: null, courseId: null, examDate: '', fullScore: 100, passScore: 60 }
  formRef.value?.clearValidate()
}

const openAdd = () => {
  resetForm()
  formTitle.value = '新增考试'
  dialogVisible.value = true
}

const openEdit = (row) => {
  form.value = { ...row }
  formTitle.value = '编辑考试'
  dialogVisible.value = true
}

const save = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const api = form.value.id ? updateExamApi : addExamApi
      const r = await api(form.value)
      if (r.code === 1) {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        loadExams()
      } else {
        ElMessage.error(r.msg || '保存失败')
      }
    } finally {
      saving.value = false
    }
  })
}

const remove = (row) => {
  ElMessageBox.confirm(`确定删除考试「${row.name}」吗？`, '删除考试', { type: 'warning' })
    .then(async () => {
      const r = await deleteExamApi(row.id)
      if (r.code === 1) {
        ElMessage.success('已删除')
        loadExams()
      } else {
        ElMessage.error(r.msg || '删除失败')
      }
    })
    .catch(() => {})
}

onMounted(async () => {
  await loadOptions()
  loadExams()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>考试管理</h2>
        <span class="page-subtitle">考试与成绩、统计联动</span>
      </div>
      <div>
        <el-button class="btn-add" @click="openAdd">+ 新增考试</el-button>
        <el-button @click="loadExams">刷新</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true">
        <el-form-item label="班级">
          <el-select v-model="filterClazzId" placeholder="全部" clearable style="width: 220px" @change="loadExams">
            <el-option v-for="c in clazzOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="exams" border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="考试名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="clazzName" label="班级" min-width="160" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" min-width="140" show-overflow-tooltip />
        <el-table-column label="考试日期" width="120" align="center">
          <template #default="{ row }">{{ row.examDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="满分 / 及格" width="120" align="center">
          <template #default="{ row }">{{ row.fullScore ?? '-' }} / {{ row.passScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !exams.length" description="暂无考试，点右上角新增" />
    </div>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="考试名称" prop="name">
          <el-input v-model="form.name" placeholder="如：Java基础阶段测试" />
        </el-form-item>
        <el-form-item label="班级" prop="clazzId">
          <el-select v-model="form.clazzId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="c in clazzOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="form.courseId" placeholder="可选" clearable style="width: 100%">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试日期" prop="examDate">
          <el-date-picker v-model="form.examDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="满分" prop="fullScore">
          <el-input-number v-model="form.fullScore" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="及格分" prop="passScore">
          <el-input-number v-model="form.passScore" :min="0" :max="form.fullScore" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-subtitle {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}
</style>

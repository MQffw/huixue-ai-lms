<script setup>
import { ref, onMounted } from 'vue'
import { queryPageApi, addApi, queryInfoApi, updateApi, deleteApi, deleteBatchApi } from '@/api/notice'
import { ElMessage, ElMessageBox } from 'element-plus'

const typeMap = { 1: '通知', 2: '公告', 3: '制度' }
const typeOpts = Object.entries(typeMap).map(([v, l]) => ({ value: Number(v), label: l }))
const typeTag = { 1: 'primary', 2: 'warning', 3: 'success' }

const searchForm = ref({ keyword: '', type: null })
const tableData = ref([])
const selectIds = ref([])
const handleSelectionChange = (val) => { selectIds.value = val.map(v => v.id) }
const pagination = ref({ currentPage: 1, pageSize: 10, total: 0 })

const queryPage = () => {
  const p = { page: pagination.value.currentPage, pageSize: pagination.value.pageSize }
  if (searchForm.value.keyword) p.keyword = searchForm.value.keyword
  if (searchForm.value.type) p.type = searchForm.value.type
  queryPageApi(p).then(r => { if (r.code) { tableData.value = r.data.rows; pagination.value.total = r.data.total } })
}
const clear = () => { searchForm.value = { keyword: '', type: null }; queryPage() }

const deleteByIds = () => {
  if (!selectIds.value.length) return ElMessage.warning('请选择公告')
  ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteBatchApi(selectIds.value.join(',')); ElMessage.success('已删除'); queryPage()
  }).catch(() => {})
}
const delById = (id) => ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => { await deleteApi(id); ElMessage.success('已删除'); queryPage() }).catch(() => {})

// --- 弹窗 ---
const dialogVisible = ref(false); const formTitle = ref(''); const formRef = ref()
const notice = ref({ id: null, title: '', content: '', type: 1, targetAudience: '全体', isTop: 0 })
const rules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }, { min: 4, max: 100, message: '4-100字', trigger: 'blur' }],
  content: [{ required: true, message: '内容不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}
// --- 查看详情 ---
const detailVisible = ref(false)
const detailNotice = ref({})
const viewDetail = async (id) => {
  const r = await queryInfoApi(id)
  if (r.code) { detailNotice.value = r.data; detailVisible.value = true }
}

const addNotice = () => { notice.value = { id: null, title: '', content: '', type: 1, targetAudience: '全体', isTop: 0 }; dialogVisible.value = true; formTitle.value = '新增公告' }
const editNotice = async (id) => {
  const r = await queryInfoApi(id); if (r.code) { notice.value = r.data; dialogVisible.value = true; formTitle.value = '编辑公告' }
}
const save = (f) => {
  if (!f) return
  f.validate(async (valid) => {
    if (valid) {
      const api = notice.value.id ? updateApi(notice.value) : addApi(notice.value)
      const r = await api; if (r.code) { ElMessage.success('保存成功'); dialogVisible.value = false; queryPage() } else ElMessage.error(r.msg)
    }
  })
}

onMounted(() => queryPage())
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>通知公告</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="addNotice(); $nextTick(() => formRef?.resetFields())">+ 新增公告</el-button>
        <el-button type="danger" @click="deleteByIds">- 批量删除</el-button>
      </div>
    </div>

    <div class="filter-card">
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="关键词"><el-input v-model="searchForm.keyword" clearable placeholder="标题/内容" /></el-form-item>
      <el-form-item label="类型"><el-select v-model="searchForm.type" clearable placeholder="全部" style="width:100px"><el-option v-for="o in typeOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="queryPage">查询</el-button><el-button type="danger" @click="clear">清空</el-button></el-form-item>
    </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column label="类型" width="70" align="center">
          <template #default="{ row }"><el-tag :type="typeTag[row.type]" size="small">{{ typeMap[row.type] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="viewDetail(row.id)" :style="{ fontWeight: row.isTop ? 'bold' : 'normal' }">
              {{ row.isTop ? '🔝 ' : '' }}{{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }">
            <span style="color:#999;cursor:pointer" @click="viewDetail(row.id)">{{ (row.content || '').substring(0, 60) }}{{ (row.content || '').length > 60 ? '…' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" min-width="80" align="center" />
        <el-table-column prop="publishTime" label="发布时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" class="btn-lookup" @click="viewDetail(row.id)">查看</el-button>
            <el-button class="btn-edit" size="small" @click="editNotice(row.id)">编辑</el-button>
            <el-button type="danger" size="small" @click="delById(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" @size-change="queryPage" @current-change="queryPage" />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="formTitle" width="45%">
      <el-form :model="notice" ref="formRef" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="notice.title" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-select v-model="notice.type" style="width:100%"><el-option v-for="o in typeOpts" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="目标受众"><el-input v-model="notice.targetAudience" /></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="notice.content" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="置顶"><el-switch v-model="notice.isTop" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save(formRef)">保存</el-button></template>
    </el-dialog>

    <!-- 查看详情弹窗（只读） -->
    <el-dialog v-model="detailVisible" :title="detailNotice.title" width="55%">
      <div style="margin-bottom:16px;color:#909399;font-size:13px;line-height:2">
        <el-tag :type="typeTag[detailNotice.type]" size="small">{{ typeMap[detailNotice.type] }}</el-tag>
        <span style="margin-left:12px">发布人：{{ detailNotice.publisherName || '未知' }}</span>
        <span style="margin-left:12px">发布时间：{{ detailNotice.publishTime }}</span>
        <span style="margin-left:12px">目标受众：{{ detailNotice.targetAudience }}</span>
        <el-tag v-if="detailNotice.isTop" type="danger" size="small" style="margin-left:12px">置顶</el-tag>
      </div>
      <div style="background:#f5f7fa;padding:20px;border-radius:8px;white-space:pre-wrap;line-height:1.8;font-size:14px;min-height:200px;max-height:60vh;overflow-y:auto">{{ detailNotice.content }}</div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>

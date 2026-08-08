<template>
  <el-dialog
    append-to-body
    top="6vh"
    :model-value="props.modelValue"
    :title="mode === 'form' ? (isEdit ? '编辑模型' : '新增模型') : 'AI 模型配置'"
    width="920px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @open="mode === 'list' && loadModels()"
  >
    <!-- 列表模式 -->
    <div v-if="mode === 'list'">
      <div class="mm-header">
        <span class="mm-tip">新增 / 修改后立即生效，无需重启</span>
        <el-button class="btn-add" :icon="Plus" @click="openAdd">新增模型</el-button>
      </div>
      <el-table :data="models" v-loading="loading" max-height="420">
        <el-table-column prop="type" label="标识" width="95" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="model" label="模型名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="apiKey" label="Key" width="95">
          <template #default="{ row }">
            <span class="mm-key">{{ row.apiKey || '未配置' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="72">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button class="btn-edit" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="removeModel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 表单模式 -->
    <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="95px" class="mm-form">
      <el-form-item label="模型标识" prop="type">
        <el-input v-model="form.type" :disabled="isEdit" placeholder="如 deepseek / qwen / kimi" />
      </el-form-item>
      <el-form-item label="显示名称" prop="name">
        <el-input v-model="form.name" placeholder="如 DeepSeek V4 Pro" />
      </el-form-item>
      <el-form-item label="接口地址" prop="baseUrl">
        <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com（OpenAI 兼容）" />
      </el-form-item>
      <el-form-item label="API Key" prop="apiKey">
        <el-input v-model="form.apiKey" type="password" show-password
                  :placeholder="isEdit ? '留空表示不修改' : '请输入密钥'" />
      </el-form-item>
      <el-form-item label="模型名称" prop="model">
        <el-input v-model="form.model" placeholder="如 deepseek-v4-pro" />
      </el-form-item>
      <el-form-item label="Temperature">
        <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
      </el-form-item>
      <el-form-item label="Max Tokens">
        <el-input-number v-model="form.maxTokens" :min="1" :max="8192" :step="256" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <template v-if="mode === 'form'">
        <el-button @click="backToList">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
      <el-button v-else @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:visible', 'changed'])

const mode = ref('list')
const models = ref([])
const loading = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  type: '', name: '', baseUrl: '', apiKey: '', model: '',
  temperature: 0.3, maxTokens: 1024, enabled: true
})
const form = reactive(defaultForm())

const rules = {
  type: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
}

const loadModels = async () => {
  loading.value = true
  try {
    const res = await request.get('/ai/models')
    if (res.code === 1) models.value = res.data || []
  } catch (e) {
    ElMessage.error('模型列表加载失败')
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, defaultForm())
  mode.value = 'form'
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    type: row.type, name: row.name, baseUrl: row.baseUrl, apiKey: '',
    model: row.model, temperature: row.temperature, maxTokens: row.maxTokens, enabled: row.enabled
  })
  mode.value = 'form'
}

const backToList = () => {
  mode.value = 'list'
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value && !payload.apiKey) {
      delete payload.apiKey
    }
    const res = await request.post('/ai/models', payload)
    if (res.code === 1) {
      ElMessage.success(isEdit.value ? '模型已更新' : '模型已添加')
      mode.value = 'list'
      loadModels()
      emit('changed')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.msg || e.message))
  } finally {
    submitting.value = false
  }
}

const removeModel = (row) => {
  ElMessageBox.confirm(`确定删除模型「${row.name}」吗？删除后立即不可用。`, '提示', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
  }).then(async () => {
    const res = await request.delete('/ai/models/' + row.type)
    if (res.code === 1) {
      ElMessage.success('已删除')
      loadModels()
      emit('changed')
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  }).catch(() => {})
}

onMounted(loadModels)
</script>

<style scoped>
.mm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.mm-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
.mm-key {
  font-size: 12px;
  color: #909399;
}
.mm-form {
  padding-top: 8px;
  font-size: 14px;
}
.mm-form :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
}
.mm-form :deep(.el-input__inner),
.mm-form :deep(.el-textarea__inner) {
  font-size: 14px;
}
.mm-form :deep(.el-input-number) {
  width: 180px;
}
:deep(.el-table) {
  font-size: 14px;
}
</style>
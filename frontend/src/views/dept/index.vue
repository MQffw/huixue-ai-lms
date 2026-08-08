<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryAllApi, addDeptApi, queryInfoApi, updateDeptApi, deleteDeptApi, deleteBatchDeptApi } from '@/api/dept'

// 声明列表展示数据
let tableData = ref([])

//复选框 - 选中的ID
let selectIds = ref([])
const handleSelectionChange = (selection) => {
  selectIds.value = selection.map(item => item.id)
}

//批量删除
const deleteByIds = () => {
  if (selectIds.value.length === 0) {
    ElMessage.warning('请选择要删除的部门')
    return
  }
  ElMessageBox.confirm('确定删除选中的部门吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const result = await deleteBatchDeptApi(selectIds.value)
    if (result.code) {
      ElMessage.success('删除成功')
      queryAll()
    } else {
      ElMessage.error(result.msg)
    }
  })
}

// 动态加载数据 - 查询部门
const queryAll = async () => {
  const result = await queryAllApi()
  tableData.value = result.data
}

// 钩子函数
onMounted(() => {
  queryAll()
})

const formTitle = ref('')
//新增部门
const add = () => {
  formTitle.value = '新增部门'
  showDialog.value = true
  deptForm.value = {name: ''}
}

// 编辑部门 - 根据ID查询回显数据
const handleEdit = async (row) => {
  console.log(`Edit item with ID ${row.id}`);
  formTitle.value = '修改部门'
  showDialog.value = true
  deptForm.value = {name: ''}

  const result = await queryInfoApi(row.id)
  if(result.code){
    deptForm.value = result.data
  }
};

// 删除部门 - 根据ID删除部门
const handleDelete = (row) => {
  console.log(`Delete item with ID ${row.id}`);
  //删除部门时, 需要弹出一个确认框, 如果是确认, 则删除部门
  ElMessageBox.confirm('此操作将永久删除该部门, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // 删除部门
    const result = await deleteDeptApi(row.id)
    if(result.code){
      ElMessage.success('删除成功')
      queryAll()
    }
  })
};

// 新增部门对话框的状态
const showDialog = ref(false)
// 表单数据
const deptForm = ref({name: ''})
// 表单验证规则
const formRules = ref({
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ]
})

// 表单引用
const deptFormRef = ref(null)

// 重置表单
const resetForm = () => {
  deptFormRef.value.resetFields()
}

// 提交表单
const save = async () => {
  await deptFormRef.value.validate(async valid => {
    if (!valid) return
    // 提交表单
    let result = null;
    if(deptForm.value.id){
      result = await updateDeptApi(deptForm.value) // 修改
    }else {
      result = await addDeptApi(deptForm.value) // 新增
    } 
    if(result.code){
      ElMessage.success('操作成功')
      // 关闭对话框
      showDialog.value = false
      // 重置表单
      resetForm()
      // 重新加载数据
      queryAll()
    }else {
      ElMessage.error(result.msg)
    }
  })
}
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>部门管理</h2>
      </div>
      <div>
        <el-button class="btn-add" @click="add()"> + 新增部门</el-button>
        <el-button type="danger" @click="deleteByIds"> - 批量删除</el-button>
      </div>
    </div>

    <!-- 数据展示表格 -->
    <div class="table-card">
      <el-table :data="tableData" border style="width: 100%;" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column type="index" label="序号" width="80" align="center"/>
        <el-table-column prop="name" label="部门名称" width="300" align="center"/>
        <el-table-column prop="updateTime" label="最后修改时间" width="300" align="center"/>
        <el-table-column fixed="right" label="操作" align="center">
          <template #default="{ row }">
            <el-button size="small" class="btn-edit" @click="handleEdit(row)">修改</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增部门的对话框 -->
    <el-dialog v-model="showDialog" :title="formTitle" width="30%" @close="resetForm">
      <el-form :model="deptForm" :rules="formRules" ref="deptFormRef">
        <el-form-item label="部门名称" prop="name" label-width="80px">
          <el-input v-model="deptForm.name" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>

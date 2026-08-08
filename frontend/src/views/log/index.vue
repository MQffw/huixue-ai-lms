<script setup>
import { onMounted, ref } from 'vue'
import { queryPageApi} from '@/api/log'
import { ElMessage } from 'element-plus'

//列表展示数据
let tableData = ref([])

//钩子函数 - 页面加载时触发
onMounted(() => {
  queryPage()
})

//分页组件
const pagination = ref({currentPage: 1, pageSize: 15, total: 0})
//每页展示记录数发生变化时触发
const handleSizeChange = (pageSize) => {
  pagination.value.pageSize = pageSize
  queryPage()
}
//当前页码发生变化时触发
const handleCurrentChange = (page) => {
  pagination.value.currentPage = page
  queryPage()
}

//分页条件查询
const queryPage = async () => {
  try {
    const result = await queryPageApi(pagination.value.currentPage, pagination.value.pageSize);

    if(result && result.code === 1) {
      tableData.value = result.data?.rows || []
      pagination.value.total = result.data?.total || 0
    } else {
      tableData.value = []
      pagination.value.total = 0
      if (result && result.msg) {
        ElMessage.warning(result.msg)
      }
    }
  } catch (error) {
    console.error('查询日志失败:', error)
    tableData.value = []
    pagination.value.total = 0
    ElMessage.error('加载日志数据失败，请稍后重试')
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header-bar">
      <div class="page-title-group">
        <h2><span class="title-accent"></span>日志管理</h2>
      </div>
    </div>

    <div class="table-card">
    <!-- 列表展示 -->
    <el-table :data="tableData" border style="width: 100%" fit size="small" v-loading="false">
      <el-table-column prop="operateEmpName" label="操作人" align="center" width="80px"/>
      <el-table-column prop="operateTime" label="操作时间" align="center" width="150px"/>
      <el-table-column prop="className" label="类名" align="center" width="300px" />
      <el-table-column prop="methodName" label="方法名" align="center" width="100px" />
      <el-table-column prop="costTime"  label="操作耗时(ms)" align="center" width="100px"/>
      <el-table-column prop="methodParams" label="请求参数" align="center" width="280px">
        <template #default="scope">
          <el-popover effect="light" trigger="hover" placement="top" width="auto" popper-style="font-size:12px">
            <template #default>
              <div>参数: {{ scope.row.methodParams }}</div>
            </template>
            <template #reference>
              <el-tag v-if="scope.row.methodParams && scope.row.methodParams.length <= 30">{{ scope.row.methodParams}}</el-tag>
              <el-tag v-else>{{ scope.row.methodParams ? scope.row.methodParams.substring(0,30) + '...' : '' }}</el-tag>
            </template>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column prop="returnValue"  label="返回值" align="center"></el-table-column>
    </el-table>
    </div>

    <!-- 分页组件Pagination -->
    <el-pagination
      v-model:current-page="pagination.currentPage"
      v-model:page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>


<style scoped>
</style>

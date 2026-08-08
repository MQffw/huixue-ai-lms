<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter, useRoute } from 'vue-router'
import { updatePasswordApi } from '@/api/login'

const router = useRouter()
const route = useRoute()

const loginName = ref('')
onMounted(() => {
  let loginUser = JSON.parse(localStorage.getItem('loginUser'))
  if (loginUser) {
    loginName.value = loginUser.name
  }
})

const logout = () => {
  ElMessageBox.confirm('确认退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('退出登录成功')
    localStorage.removeItem('loginUser')
    router.push('/login')
  })
}

// 修改密码
const pwdDialogVisible = ref(false)
const pwdFormRef = ref(null)
const pwdSubmitting = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const openPwdDialog = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

const submitPassword = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdSubmitting.value = true
  try {
    const res = await updatePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.code === 1) {
      ElMessage.success('密码修改成功，请重新登录')
      pwdDialogVisible.value = false
      localStorage.removeItem('loginUser')
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e) {
    ElMessage.error('网络异常，请稍后重试')
  } finally {
    pwdSubmitting.value = false
  }
}
</script>

<template>
  <div class="common-layout">
    <el-container>
      <!-- Header 区域 -->
      <el-header class="header">
        <div class="header-left">
          <div class="logo">
            <el-icon :size="22"><Monitor /></el-icon>
          </div>
          <span class="title">慧学<span>通</span> · 智能教学管理平台</span>
        </div>
        <div class="header-right">
          <a href="javascript:void(0)" @click="openPwdDialog" class="header-link">
            <el-icon><EditPen /></el-icon> 修改密码
          </a>
          <span class="divider">|</span>
          <a href="javascript:void(0)" @click="logout" class="header-link">
            <el-icon><SwitchButton /></el-icon> 退出登录
            <span class="username">{{ loginName }}</span>
          </a>
        </div>
      </el-header>

      <el-container>
        <!-- 左侧菜单 -->
        <el-aside width="220px" class="aside">
          <el-menu router :default-active="route.path">
            <!-- 首页菜单 -->
            <el-menu-item index="/index">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>

            <!-- AI 智能助手 -->
            <el-menu-item index="/ai">
              <el-icon><ChatLineRound /></el-icon>
              <span>AI 助手</span>
            </el-menu-item>

            <!-- AI 用量监控 -->
            <el-menu-item index="/ai-stats">
              <el-icon><DataBoard /></el-icon>
              <span>AI 用量监控</span>
            </el-menu-item>

            <!-- 教务业务管理 -->
            <el-sub-menu index="/edu">
              <template #title>
                <el-icon><Reading /></el-icon>
                <span>教务业务管理</span>
              </template>
              <el-menu-item index="/course">
                <el-icon><Collection /></el-icon>
                <span>课程管理</span>
              </el-menu-item>
              <el-menu-item index="/attendance">
                <el-icon><Calendar /></el-icon>
                <span>考勤记录</span>
              </el-menu-item>
              <el-menu-item index="/score">
                <el-icon><TrendCharts /></el-icon>
                <span>成绩查询</span>
              </el-menu-item>
              <el-menu-item index="/payment">
                <el-icon><Money /></el-icon>
                <span>缴费管理</span>
              </el-menu-item>
              <el-menu-item index="/employment">
                <el-icon><Briefcase /></el-icon>
                <span>就业管理</span>
              </el-menu-item>
              <el-menu-item index="/violation">
                <el-icon><Warning /></el-icon>
                <span>违纪记录</span>
              </el-menu-item>
              <el-menu-item index="/notice">
                <el-icon><Bell /></el-icon>
                <span>通知公告</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 班级管理菜单 -->
            <el-sub-menu index="/manage">
              <template #title>
                <el-icon><Menu /></el-icon>
                <span>班级学员管理</span>
              </template>
              <el-menu-item index="/clazz">
                <el-icon><HomeFilled /></el-icon>
                <span>班级管理</span>
              </el-menu-item>
              <el-menu-item index="/stu">
                <el-icon><UserFilled /></el-icon>
                <span>学员管理</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 系统信息管理 -->
            <el-sub-menu index="/system">
              <template #title>
                <el-icon><Tools /></el-icon>
                <span>系统信息管理</span>
              </template>
              <el-menu-item index="/dept">
                <el-icon><HelpFilled /></el-icon>
                <span>部门管理</span>
              </el-menu-item>
              <el-menu-item index="/emp">
                <el-icon><Avatar /></el-icon>
                <span>员工管理</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 数据统计管理 -->
            <el-sub-menu index="/report">
              <template #title>
                <el-icon><Histogram /></el-icon>
                <span>数据统计管理</span>
              </template>
              <el-menu-item index="/report/emp">
                <el-icon><InfoFilled /></el-icon>
                <span>员工信息统计</span>
              </el-menu-item>
              <el-menu-item index="/report/stu">
                <el-icon><Share /></el-icon>
                <span>学员信息统计</span>
              </el-menu-item>
              <el-menu-item index="/log">
                <el-icon><Document /></el-icon>
                <span>日志信息统计</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-aside>

        <!-- 主展示区域 -->
        <el-main class="main-content">
          <router-view v-slot="{ Component }">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </router-view>
        </el-main>
      </el-container>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.common-layout {
  height: 100vh;
  overflow: hidden;
}

/* 顶部导航：灰色毛玻璃 */
.header {
  background: rgba(230, 230, 235, 0.6);
  backdrop-filter: blur(32px);
  -webkit-backdrop-filter: blur(32px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 64px !important;
  z-index: 100;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #E60012 0%, #FF6D00 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(230, 0, 18, 0.25);
}

.title {
  color: #1a1a1a;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.title span {
  color: #E60012;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-link {
  color: #555;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  transition: color 0.2s;
}

.header-link:hover {
  color: #E60012;
}

.divider {
  color: #ddd;
}

.username {
  background: var(--bg-page-alt);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  color: var(--brand-red);
  font-weight: 500;
  margin-left: 4px;
}

/* 左侧菜单 */
.aside {
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  overflow-y: auto;
  height: calc(100vh - 64px);
}

.aside::-webkit-scrollbar {
  width: 6px;
}

.aside::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

.aside::-webkit-scrollbar-thumb:hover {
  background: #aaa;
}

.el-menu {
  border-right: none;
  padding: 8px 0;
}

.el-menu-item {
  height: 48px;
  line-height: 48px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: #333;
}

.el-menu-item:hover {
  background: #f3f4f6;
  color: #374151;
}

.el-menu-item.is-active {
  background: linear-gradient(135deg, #4b5563 0%, #6b7280 100%);
  color: white;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.el-menu-item.is-active .el-icon {
  color: white;
}

:deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
  margin: 2px 8px;
  border-radius: 8px;
  color: #333;
}

:deep(.el-sub-menu__title:hover) {
  background: #f3f4f6;
  color: #374151;
}

/* 主内容区：子页面自行控制滚动 */
.main-content {
  background: #f8f9fa;
  padding: 0;
  overflow: hidden;
  height: calc(100vh - 64px);
}
</style>

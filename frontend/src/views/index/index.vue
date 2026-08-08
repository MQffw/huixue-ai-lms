<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound, OfficeBuilding, User, DataBoard, Notebook,
  Calendar, Tickets, Money, Briefcase, Bell, Warning, Reading,
  TrendCharts, Document, School
} from '@element-plus/icons-vue'

const router = useRouter()
const loginName = ref('')

onMounted(() => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  if (loginUser) {
    loginName.value = loginUser.name
  }
})

// 大按钮 - 各占一行
const featuredCards = [
  {
    title: 'AI 智能助手',
    desc: '自然语言查询教务数据，AI Agent 自动调用工具回答',
    icon: ChatDotRound,
    color: '#64748b',
    bg: 'linear-gradient(135deg, #334155 0%, #475569 100%)',
    path: '/ai'
  },
  {
    title: '数据统计',
    desc: '多维度数据分析报表、可视化统计图表',
    icon: DataBoard,
    color: '#64748b',
    bg: 'linear-gradient(135deg, #475569 0%, #64748b 100%)',
    path: '/report/emp'
  }
]

// 常规功能卡片 - 3行4列
const featureCards = [
  {
    title: '课程管理',
    desc: '管理课程信息，查看课程列表与详情',
    icon: Reading,
    color: '#e6a23c',
    bg: 'linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%)',
    path: '/course'
  },
  {
    title: '考勤记录',
    desc: '查看学员出勤情况，统计出勤率与异常',
    icon: Calendar,
    color: '#67c23a',
    bg: 'linear-gradient(135deg, #529b2e 0%, #95d475 100%)',
    path: '/attendance'
  },
  {
    title: '成绩查询',
    desc: '查看考试成绩、排名与统计分析',
    icon: TrendCharts,
    color: '#e0405b',
    bg: 'linear-gradient(135deg, #c0324b 0%, #f89898 100%)',
    path: '/score'
  },
  {
    title: '缴费管理',
    desc: '查看学员缴费记录、欠费情况与统计',
    icon: Money,
    color: '#409eff',
    bg: 'linear-gradient(135deg, #2c6fce 0%, #79bbff 100%)',
    path: '/payment'
  },
  {
    title: '就业管理',
    desc: '跟踪学员就业情况、薪资统计与就业率',
    icon: Briefcase,
    color: '#9b59b6',
    bg: 'linear-gradient(135deg, #7b3f9e 0%, #c39bd3 100%)',
    path: '/employment'
  },
  {
    title: '违纪记录',
    desc: '查看违纪明细、扣分统计与异常预警',
    icon: Warning,
    color: '#f56c6c',
    bg: 'linear-gradient(135deg, #d4380d 0%, #ffa39e 100%)',
    path: '/violation'
  },
  {
    title: '通知公告',
    desc: '查看学校通知、制度文档与最新公告',
    icon: Bell,
    color: '#f39c12',
    bg: 'linear-gradient(135deg, #d48806 0%, #ffe58f 100%)',
    path: '/notice'
  },
  {
    title: '班级管理',
    desc: '管理班级信息、分配学员与班主任',
    icon: Notebook,
    color: '#409eff',
    bg: 'linear-gradient(135deg, #2c6fce 0%, #79bbff 100%)',
    path: '/clazz'
  },
  {
    title: '学员管理',
    desc: '管理学员档案、查询学员详细信息',
    icon: User,
    color: '#67c23a',
    bg: 'linear-gradient(135deg, #529b2e 0%, #95d475 100%)',
    path: '/stu'
  },
  {
    title: '部门管理',
    desc: '管理组织架构、部门信息与维护',
    icon: OfficeBuilding,
    color: '#e6a23c',
    bg: 'linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%)',
    path: '/dept'
  },
  {
    title: '员工管理',
    desc: '管理员工信息、工作经历与入职记录',
    icon: School,
    color: '#9b59b6',
    bg: 'linear-gradient(135deg, #7b3f9e 0%, #c39bd3 100%)',
    path: '/emp'
  },
  {
    title: '操作日志',
    desc: '系统操作记录、行为审计与追踪',
    icon: Document,
    color: '#909399',
    bg: 'linear-gradient(135deg, #606266 0%, #b1b3b8 100%)',
    path: '/log'
  }
]

const navigateTo = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="dashboard">
    <!-- 顶部欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-text">
        <h1>欢迎回来，{{ loginName }}</h1>
        <p>慧学通智能教学管理系统 — AI 驱动的全业务链教学管理平台</p>
      </div>
      <div class="banner-stats">
        <div class="stat-item">
          <span class="stat-num">{{ featuredCards.length + featureCards.length }}</span>
          <span class="stat-label">功能模块</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">AI</span>
          <span class="stat-label">智能助手</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">19</span>
          <span class="stat-label">数据表</span>
        </div>
      </div>
    </div>

    <!-- 大按钮：AI智能助手 & 数据统计 -->
    <div class="featured-section">
      <div v-for="card in featuredCards" :key="card.title"
           class="featured-card" @click="navigateTo(card.path)">
        <div class="featured-bg" :style="{ background: card.bg }"></div>
        <div class="featured-content">
          <div class="featured-icon">
            <el-icon :size="36" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="featured-info">
            <h3>{{ card.title }}</h3>
            <p>{{ card.desc }}</p>
          </div>
          <div class="featured-arrow">
            <el-icon :size="22"><component :is="'ArrowRight'" /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能卡片网格 -->
    <div class="section">
      <h3 class="section-title">
        <el-icon><DataBoard /></el-icon> 功能模块
      </h3>
      <div class="feature-grid">
        <div v-for="card in featureCards" :key="card.title"
             class="feature-card" @click="navigateTo(card.path)">
          <div class="card-icon" :style="{ background: card.bg }">
            <el-icon :size="28" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="card-info">
            <h4>{{ card.title }}</h4>
            <p>{{ card.desc }}</p>
          </div>
          <div class="card-arrow">
            <el-icon><component :is="'ArrowRight'" /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  height: calc(100vh - 64px);
  overflow-y: auto;
  padding: 28px;
  background: #f8f9fa;
  position: relative;
}

/* 背景彩色光斑 - 呼应登录页 */
.dashboard::before {
  content: '';
  position: fixed;
  top: 20%;
  left: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(230, 0, 18, 0.06) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.dashboard::after {
  content: '';
  position: fixed;
  bottom: 10%;
  right: -5%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(63, 81, 181, 0.06) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.dashboard::-webkit-scrollbar {
  width: 6px;
}

.dashboard::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

/* 欢迎横幅：白底 + 红色装饰 */
.welcome-banner {
  background: white;
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 28px;
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;
  z-index: 1;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
}


.banner-text h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.banner-text p {
  color: rgba(0,0,0,0.5);
  margin: 0;
  font-size: 14px;
}

.banner-stats {
  display: flex;
  gap: 36px;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 38px;
  font-weight: 800;
  color: #E60012;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0,0,0,0.45);
  margin-top: 6px;
}

/* 分区标题 */
.section {
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
  padding-left: 10px;
  border-left: 3px solid #E60012;
}

.section-title .el-icon {
  color: #E60012;
}

/* 大卡片区域 */
.featured-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

.featured-card {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 100px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.featured-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
}

.featured-bg {
  position: absolute;
  inset: 0;
  opacity: 0.95;
}

.featured-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  height: 100%;
  box-sizing: border-box;
}

.featured-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(8px);
}

.featured-info {
  flex: 1;
}

.featured-info h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.featured-info p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.featured-arrow {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
}

.featured-card:hover .featured-arrow {
  color: #fff;
  transform: translateX(6px);
}

/* 功能卡片网格 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  position: relative;
  z-index: 1;
}

.feature-card {
  background: white;
  border-radius: 12px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: transparent;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-info h4 {
  margin: 0 0 2px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.card-info p {
  margin: 0;
  font-size: 11px;
  color: #999;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-arrow {
  flex-shrink: 0;
  color: #ccc;
  transition: all 0.25s ease;
}

.feature-card:hover .card-arrow {
  color: #E60012;
  transform: translateX(4px);
}

/* 响应式 */
@media (max-width: 1400px) {
  .feature-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1000px) {
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .featured-grid {
    grid-template-columns: 1fr;
  }
  .welcome-content {
    flex-direction: column;
    text-align: center;
    gap: 20px;
    align-items: center;
  }
}
</style>

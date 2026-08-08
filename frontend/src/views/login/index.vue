<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { loginApi } from '@/api/login'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'

let loginForm = ref({ username: '', password: '' })
let router = useRouter()

let scrollProgress = ref(0)
let targetProgress = 0
let mouseX = ref(50)
let mouseY = ref(50)
const eyeX = ref(0)
const eyeY = ref(0)
const isHovering = ref(false)
const isPasswordFocus = ref(false)
const handleAvatarMove = () => {}
const handleWindowMouseMove = (e) => {
  const target = document.querySelector('.login-avatar-center')
  if (!target) return
  const rect = target.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const dx = e.clientX - cx
  const dy = e.clientY - cy
  const dist = Math.sqrt(dx * dx + dy * dy)
  const maxDist = Math.max(window.innerWidth, window.innerHeight) / 2
  eyeX.value = dist > 0 ? (dx / dist) * Math.min(5, dist / maxDist * 8) : 0
  eyeY.value = dist > 0 ? (dy / dist) * Math.min(5, dist / maxDist * 8) : 0
}
const handleAvatarEnter = () => { isHovering.value = true }
const handleAvatarLeave = () => { isHovering.value = false }

const login = async () => {
  try {
    const result = await loginApi(loginForm.value)
    if (result.code) {
      ElMessage.success('登录成功')
      localStorage.setItem('loginUser', JSON.stringify(result.data))
      router.push('/')
    } else {
      ElMessage.error(result.msg)
    }
  } catch (error) {
    console.error('登录请求失败:', error)
    ElMessage.error('登录请求失败，请检查网络或服务是否启动')
  }
}

const cancel = () => {
  loginForm.value = { username: '', password: '' }
}

const handleWheel = (e) => {
  e.preventDefault()
  targetProgress += e.deltaY * 0.1
  targetProgress = Math.max(0, Math.min(100, targetProgress))
}

const handleMouseMove = (e) => {
  mouseX.value = (e.clientX / window.innerWidth) * 100
  mouseY.value = (e.clientY / window.innerHeight) * 100
}

let rafId = null
const animate = () => {
  scrollProgress.value += (targetProgress - scrollProgress.value) * 0.06
  rafId = requestAnimationFrame(animate)
}

onMounted(() => {
  window.addEventListener('wheel', handleWheel, { passive: false })
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mousemove', handleWindowMouseMove)
  rafId = requestAnimationFrame(animate)
})

onUnmounted(() => {
  window.removeEventListener('wheel', handleWheel)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mousemove', handleWindowMouseMove)
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<template>
  <div class="login-container">
    <!-- 背景：多彩渐变球，随滚轮放大 + 跟随鼠标 -->
    <div
      class="bg-sphere"
      :style="{
        transform: `scale(${1 + scrollProgress * 0.015}) translate(${(mouseX - 50) * 0.3}px, ${(mouseY - 50) * 0.3}px)`,
        background: `
          radial-gradient(circle at ${mouseX}% ${mouseY}%,
            rgba(230, 0, 18, ${0.4 + scrollProgress * 0.008}) 0%,
            rgba(255, 87, 34, ${0.3 + scrollProgress * 0.006}) 15%,
            rgba(156, 39, 176, ${0.25 + scrollProgress * 0.005}) 35%,
            rgba(63, 81, 181, ${0.2 + scrollProgress * 0.004}) 55%,
            rgba(255, 255, 255, 0) 100%)
        `
      }"
    ></div>

    <!-- 第二层：反向偏移的多彩光晕 -->
    <div
      class="bg-sphere-2"
      :style="{
        transform: `scale(${1 + scrollProgress * 0.008}) translate(${(50 - mouseX) * 0.2}px, ${(50 - mouseY) * 0.2}px)`,
        background: `
          radial-gradient(circle at ${100 - mouseX}% ${100 - mouseY}%,
            rgba(0, 188, 212, ${0.25 + scrollProgress * 0.005}) 0%,
            rgba(255, 235, 59, ${0.2 + scrollProgress * 0.004}) 25%,
            rgba(76, 175, 80, ${0.15 + scrollProgress * 0.003}) 50%,
            rgba(255, 255, 255, 0) 80%)
        `
      }"
    ></div>

    <!-- 网格纹理 -->
    <div class="bg-grid" :style="{ opacity: 0.5 - scrollProgress * 0.003 }"></div>

    <!-- 顶部光条已移除 -->

    <!-- 中央 AI 图标 -->
    <div class="login-avatar-center" @mousemove="handleAvatarMove" @mouseenter="handleAvatarEnter" @mouseleave="handleAvatarLeave">
      <svg viewBox="0 0 100 100" width="180" height="180" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="loginGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#6366f1"/>
            <stop offset="100%" stop-color="#a855f7"/>
          </linearGradient>
        </defs>
        <rect x="8" y="8" width="84" height="84" rx="22" fill="url(#loginGrad)"/>
        <!-- 左眼 -->
        <template v-if="!isPasswordFocus">
          <circle cx="36" cy="42" r="10" fill="#fff"/>
          <circle :cx="36 + eyeX" :cy="42 + eyeY" r="5" fill="#1e1b4b"/>
          <circle :cx="38 + eyeX" :cy="40 + eyeY" r="2" fill="#fff"/>
        </template>
        <path v-else d="M 28 42 Q 36 48 44 42" stroke="#1e1b4b" stroke-width="3" fill="none" stroke-linecap="round"/>
        <!-- 右眼 -->
        <template v-if="!isPasswordFocus">
          <circle cx="64" cy="42" r="10" fill="#fff"/>
          <circle :cx="64 + eyeX" :cy="42 + eyeY" r="5" fill="#1e1b4b"/>
          <circle :cx="66 + eyeX" :cy="40 + eyeY" r="2" fill="#fff"/>
        </template>
        <path v-else d="M 56 42 Q 64 48 72 42" stroke="#1e1b4b" stroke-width="3" fill="none" stroke-linecap="round"/>
        <!-- 嘴巴 -->
        <path v-if="!isHovering" d="M 40 65 Q 50 75 60 65" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round"/>
        <ellipse v-else cx="50" cy="70" rx="8" ry="6" fill="#fff"/>
      </svg>
    </div>

    <!-- 左侧品牌区 -->
    <div class="brand-side" :style="{ transform: `translateY(${(mouseY - 50) * -0.15}px)` }">
      <div class="brand-tag">智能教学管理平台</div>
      <h1 class="brand-title">
        慧学通<span class="title-dot">.</span>
      </h1>
      <div class="brand-red-line"></div>
      <p class="brand-sub">让教学更智能 · 让管理更高效</p>
      <div class="brand-features">
        <div class="feature"><span class="feature-icon"></span><span>AI 智能助手</span></div>
        <div class="feature"><span class="feature-icon"></span><span>全业务数据管理</span></div>
        <div class="feature"><span class="feature-icon"></span><span>智能分析洞察</span></div>
      </div>
    </div>

    <!-- 右侧登录卡片：毛玻璃 + 边缘泛光 -->
    <div class="login-side">
      <div class="login-card" :style="{ transform: `translateY(${(mouseY - 50) * -0.2}px) translateX(${(mouseX - 50) * 0.1}px)` }">
        <div class="glow-border"></div>
        <div class="card-content">
          <h2 class="login-title">账户登录</h2>
          <p class="login-hint">欢迎使用慧学通智能教学管理系统</p>

          <el-form class="login-form" label-width="0">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input type="password" v-model="loginForm.password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="login" @focus="isPasswordFocus = true" @blur="isPasswordFocus = false" />
            </el-form-item>
            <el-form-item>
              <el-button class="login-btn" @click="login">登 录</el-button>
            </el-form-item>
            <div class="extra-row">
              <el-button class="reset-btn" @click="cancel">重 置</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <div class="scroll-hint" :class="{ faded: scrollProgress > 3 }">滚动鼠标 · 移动鼠标 · 感受变化</div>
    <div class="progress-num">{{ Math.round(scrollProgress) }}</div>
  </div>
</template>

<style scoped>
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #ffffff;
  color: #1a1a1a;
}

/* 背景渐变球：随鼠标+滚轮 */
.bg-sphere {
  position: absolute;
  width: 130vw;
  height: 130vw;
  top: 50%;
  left: 50%;
  margin-top: -65vw;
  margin-left: -65vw;
  border-radius: 50%;
  z-index: 1;
  will-change: transform, background;
  transition: background 0.3s ease;
  filter: blur(40px);
  pointer-events: none;
}

.bg-sphere-2 {
  position: absolute;
  width: 100vw;
  height: 100vw;
  top: 50%;
  left: 50%;
  margin-top: -50vw;
  margin-left: -50vw;
  border-radius: 50%;
  z-index: 2;
  pointer-events: none;
  will-change: transform, background;
  transition: background 0.3s ease;
  filter: blur(60px);
  opacity: 0.8;
}

.bg-spotlight {
  position: absolute;
  inset: 0;
  z-index: 3;
  will-change: background;
  transition: background 0.2s ease-out;
  pointer-events: none;
}

/* 网格纹理 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,0,0,0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.025) 1px, transparent 1px);
  background-size: 60px 60px;
  z-index: 0;
  pointer-events: none;
}

/* 顶部彩光条已移除 */

/* 左侧品牌区 */
.brand-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 8%;
  z-index: 10;
  will-change: transform;
  transition: transform 0.3s ease-out;
}
.login-avatar-center {
  position: fixed;
  top: 45%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 15;
  filter: drop-shadow(0 8px 32px rgba(99, 102, 241, 0.4));
  transition: transform 0.3s ease;
  cursor: pointer;
}
.login-avatar-center:hover {
  transform: translate(-50%, -50%) scale(1.08);
}

.brand-tag {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.4);
  letter-spacing: 2px;
  margin-bottom: 24px;
  font-weight: 500;
}

.brand-title {
  font-size: 96px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
  line-height: 1;
  letter-spacing: 4px;
  display: flex;
  align-items: baseline;
}

.title-dot {
  color: #E60012;
}

.brand-red-line {
  width: 60px;
  height: 4px;
  background: #E60012;
  margin: 28px 0;
  border-radius: 2px;
}

.brand-sub {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.5);
  margin: 0 0 48px;
  letter-spacing: 1px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.55);
}

.feature-icon {
  width: 6px;
  height: 6px;
  background: #E60012;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(230, 0, 18, 0.4);
}

/* 右侧登录区 */
.login-side {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  position: relative;
}

/* 登录卡片：毛玻璃 + 边缘泛光 */
.login-card {
  position: relative;
  width: 380px;
  border-radius: 20px;
  overflow: hidden;
  will-change: transform;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 边缘泛光层 */
.glow-border {
  position: absolute;
  inset: -2px;
  border-radius: 22px;
  background: linear-gradient(135deg,
    rgba(230, 0, 18, 0.3) 0%,
    rgba(255, 255, 255, 0.1) 40%,
    rgba(0, 0, 0, 0.05) 60%,
    rgba(230, 0, 18, 0.2) 100%);
  z-index: -1;
  filter: blur(8px);
  opacity: 0.8;
}

/* 卡片内容：毛玻璃 */
.card-content {
  position: relative;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  padding: 48px 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.login-hint {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.4);
  margin: 0 0 36px;
}

/* 表单 */
.login-form :deep(.el-form-item) { margin-bottom: 24px; }

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.5);
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  box-shadow: none;
  transition: all 0.3s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(230, 0, 18, 0.4);
  background: rgba(255, 255, 255, 0.7);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #E60012;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 4px rgba(230, 0, 18, 0.08);
}

.login-form :deep(.el-input__inner) { color: #1a1a1a; height: 46px; font-size: 15px; }
.login-form :deep(.el-input__inner::placeholder) { color: rgba(0, 0, 0, 0.3); }
.login-form :deep(.el-input__prefix-inner .el-icon) { color: rgba(0, 0, 0, 0.3); }

/* 登录按钮：黑底 hover 变红 */
.login-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
  color: #ffffff;
  background: #1a1a1a;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.4s ease;
}

.login-btn:hover {
  background: #E60012;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(230, 0, 18, 0.3);
  letter-spacing: 8px;
}

.login-btn:active { transform: translateY(0); background: #cc0010; }

/* 重置按钮：与登录按钮同尺寸 */
.extra-row { margin-top: 16px; }
.reset-btn {
  width: 100%;
  height: 50px;
  border: 1.5px solid rgba(0, 0, 0, 0.15);
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
  color: #1a1a1a;
  background: transparent;
  cursor: pointer;
  transition: all 0.4s ease;
}
.reset-btn:hover {
  border-color: rgba(0, 0, 0, 0.3);
  background: rgba(0, 0, 0, 0.03);
  color: #1a1a1a;
  letter-spacing: 8px;
}
.reset-btn:active { background: rgba(0, 0, 0, 0.06); }

/* 底部提示 */
.scroll-hint {
  position: fixed;
  bottom: 30px; left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  color: rgba(0, 0, 0, 0.25);
  font-size: 11px;
  letter-spacing: 3px;
  transition: opacity 0.5s ease;
}

.scroll-hint.faded { opacity: 0; }

/* 右下角进度 */
.progress-num {
  position: fixed;
  bottom: 30px; right: 30px;
  z-index: 20;
  color: rgba(230, 0, 18, 0.4);
  font-size: 13px;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 900px) {
  .brand-side { display: none; }
  .login-side { width: 100%; }
}
</style>
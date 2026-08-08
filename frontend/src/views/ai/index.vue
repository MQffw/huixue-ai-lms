<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { User, ChatDotRound, Promotion, Plus } from '@element-plus/icons-vue'
import ModelManageDialog from './ModelManageDialog.vue'

const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const selectedModel = ref('longcat')
const modelManageVisible = ref(false)
const lastModel = ref('longcat')
const onModelSelect = (v) => {
  if (v === '__manage__') {
    selectedModel.value = lastModel.value
    modelManageVisible.value = true
  } else {
    lastModel.value = v
  }
}

const scrollProgress = ref(0)
let targetProgress = 0
const mouseX = ref(50)
const mouseY = ref(50)
const eyeX = ref(0)
const eyeY = ref(0)
const avatarRef = ref(null)
const handleWheel = (e) => {
  targetProgress += e.deltaY * 0.05
  targetProgress = Math.max(0, Math.min(100, targetProgress))
}
const handleMouseMove = (e) => {
  mouseX.value = (e.clientX / window.innerWidth) * 100
  mouseY.value = (e.clientY / window.innerHeight) * 100
}
const mouthY = ref(0)
const isHovering = ref(false)
const handleAvatarMove = (e) => {
  if (!avatarRef.value) return
  const rect = avatarRef.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const dx = e.clientX - centerX
  const dy = e.clientY - centerY
  const dist = Math.sqrt(dx * dx + dy * dy)
  const maxEye = 4
  eyeX.value = dist > 0 ? (dx / dist) * maxEye : 0
  eyeY.value = dist > 0 ? (dy / dist) * maxEye : 0
  mouthY.value = dy > 0 ? Math.min(8, dy * 0.15) : 0
}
const handleAvatarEnter = () => { isHovering.value = true }
const handleAvatarLeave = () => { isHovering.value = false; mouthY.value = 0 }
let rafId = null
const animate = () => {
  scrollProgress.value += (targetProgress - scrollProgress.value) * 0.06
  rafId = requestAnimationFrame(animate)
}

// 从 localStorage 恢复 sessionId，切换页面不丢失
const STORAGE_KEY = 'ai_session_id'
const getOrCreateSessionId = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) return saved
  const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36)
  localStorage.setItem(STORAGE_KEY, id)
  return id
}
const sessionId = ref(getOrCreateSessionId())

// 模型列表：优先从后端注册表动态加载，失败时回退到默认列表
const modelOptions = ref([
  { value: 'deepseek', label: 'DeepSeek V4 Pro' },
  { value: 'mimo', label: 'Mimo V2.5 Pro' },
  { value: 'longcat', label: 'LongCat 2.0' }
])

const loadModels = async () => {
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser'))
    const res = await fetch('/api/ai/models', {
      headers: { 'token': loginUser?.token || '' }
    })
    const data = await res.json()
    if (data && data.code === 1 && Array.isArray(data.data) && data.data.length > 0) {
      const list = data.data
        .filter(m => m.enabled !== false)
        .map(m => ({ value: m.type, label: m.name || m.type }))
      if (list.length > 0) {
        modelOptions.value = list
        if (!list.some(m => m.value === selectedModel.value)) {
          selectedModel.value = list[0].value
        }
      }
    }
  } catch (e) {
    console.warn('模型列表加载失败，使用默认列表', e)
  }
}

onMounted(loadModels)

// 快捷问题列表
const quickQuestions = [
  { icon: '🏢', text: '有多少个部门？' },
  { icon: '👥', text: '目前有多少员工？' },
  { icon: '📊', text: '学生男女比例如何？' },
  { icon: '📚', text: '查询所有班级信息' },
  { icon: '🎓', text: '各学历学生人数统计' },
  { icon: '🔍', text: '有没有宋江这个人？' }
]

// 页面加载时从后端恢复历史对话
onMounted(async () => {
  window.addEventListener('wheel', handleWheel, { passive: false })
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mousemove', handleAvatarMove)
  rafId = requestAnimationFrame(animate)
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser'))
    const resp = await fetch(`/api/ai/history?sessionId=${sessionId.value}`, {
      headers: { 'token': loginUser?.token || '' }
    })
    const result = await resp.json()
    if (result.code === 200 && result.data && result.data.length > 0) {
      messages.value = result.data.map(m => ({ role: m.role, content: m.content }))
    }
  } catch (e) { /* 静默失败 */ }
})
onUnmounted(() => {
  window.removeEventListener('wheel', handleWheel)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mousemove', handleAvatarMove)
  if (rafId) cancelAnimationFrame(rafId)
})

const sendQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

const clearChat = async () => {
  messages.value = []
  const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36)
  sessionId.value = id
  localStorage.setItem(STORAGE_KEY, id)
  // 清除后端缓存
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')
    await fetch('/api/ai/cache/clear', { method: 'POST', headers: { 'token': loginUser?.token || '' } })
  } catch (e) { /* 静默 */ }
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return

  messages.value.push({ role: 'user', content })
  inputMessage.value = ''
  loading.value = true

  const aiMsg = { role: 'assistant', content: '' }
  messages.value.push(aiMsg)
  scrollToBottom()

  try {
    const history = messages.value.slice(0, -1).slice(-10).map(m => ({
      role: m.role,
      content: m.content
    }))

    const loginUser = JSON.parse(localStorage.getItem('loginUser'))
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': loginUser?.token || ''
      },
      body: JSON.stringify({ message: content, history, modelType: selectedModel.value, sessionId: sessionId.value })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let received = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      received = true
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:') && !line.includes('[DONE]')) {
          let data = line.substring(5).trim()
          if (data) {
            data = data.replace(/\[\[TOOL\]\].*?\[\[END_TOOL\]\]/g, '')
            data = data.replace(/<longcat_tool_call>.*?<\/longcat_tool_call>/g, '')
            data = data.replace(/<longcat_tool_call>.*$/g, '')
            data = data.replace(/<longcat_arg_key>.*?<\/longcat_arg_key>/g, '')
            data = data.replace(/\*\*/g, '')
            data = data.replace(/##/g, '')
            data = data.replace(/```sql/g, '')
            data = data.replace(/```/g, '')
            data = data.replace(/^\s*\|[-| :]+\|\s*$/gm, '')
            data = data.replace(/^\|(.+)\|$/gm, (m, p1) => p1.replace(/\|/g, '  '))

            if (data) {
              aiMsg.content += data
              scrollToBottom()
            }
          }
        }
      }
    }

    // 收尾清理：先还原换行占位符
    aiMsg.content = aiMsg.content.replace(/§n§/g, '\n')
    // 剥离可能残留的工具调用标记
    aiMsg.content = aiMsg.content.replace(/\[\[TOOL\]\][\s\S]*?\[\[END_TOOL\]\]/g, '')
    if (aiMsg.content) {
      aiMsg.content = aiMsg.content
        .replace(/^(我来|让我|正在|请稍候|稍等).{0,50}(查询|查看|搜索|获取|检索|加载|调用).{0,20}\n*/gm, '')
        .trim()
    }
    // 检测无效响应（SQL碎片/纯废话）
    const broken = aiMsg.content && (
      aiMsg.content.length < 10 ||
      /\b(SELECT|FROM|WHERE|COUNT|INSERT|UPDATE|DELETE)\b/gi.test(aiMsg.content)
    )
    if (!received || !aiMsg.content || broken) {
      aiMsg.content = '模型未返回有效响应，请稍后重试或切换其他模型。'
    }
  } catch (error) {
    console.error('AI请求失败:', error)
    aiMsg.content = '请求失败，请检查网络或稍后重试。'
  } finally {
    // 确保加载指示器至少显示500ms，避免闪烁
    await new Promise(r => setTimeout(r, 500))
    loading.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<template>
  <div class="ai-chat">
    <!-- 动态背景 -->
    <div class="ai-bg" :style="{
      background: `radial-gradient(circle at ${mouseX}% ${mouseY}%,
        rgba(255, 107, 53, ${0.2 + scrollProgress * 0.003}) 0%,
        rgba(255, 167, 38, ${0.15 + scrollProgress * 0.002}) 20%,
        rgba(156, 39, 176, ${0.1 + scrollProgress * 0.002}) 50%,
        rgba(63, 81, 181, ${0.05 + scrollProgress * 0.001}) 80%)`
    }"></div>
    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <!-- 欢迎页 -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon" ref="avatarRef" @mousemove="handleAvatarMove" @mouseenter="handleAvatarEnter" @mouseleave="handleAvatarLeave">
          <svg viewBox="0 0 100 100" width="80" height="80" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="iconGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#6366f1"/>
                <stop offset="100%" stop-color="#a855f7"/>
              </linearGradient>
            </defs>
            <!-- 圆角方形底座 -->
            <rect x="8" y="8" width="84" height="84" rx="22" fill="url(#iconGrad)"/>
            <!-- 左眼白 -->
            <circle cx="36" cy="42" r="10" fill="#fff"/>
            <!-- 左眼瞳孔（动态） -->
            <circle :cx="36 + eyeX" :cy="42 + eyeY" r="5" fill="#1e1b4b"/>
            <circle :cx="38 + eyeX" :cy="40 + eyeY" r="2" fill="#fff"/>
            <!-- 右眼白 -->
            <circle cx="64" cy="42" r="10" fill="#fff"/>
            <!-- 右眼瞳孔（动态） -->
            <circle :cx="64 + eyeX" :cy="42 + eyeY" r="5" fill="#1e1b4b"/>
            <circle :cx="66 + eyeX" :cy="40 + eyeY" r="2" fill="#fff"/>
            <!-- 嘴巴（hover时张开） -->
            <path v-if="!isHovering" d="M 40 65 Q 50 75 60 65" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round"/>
            <ellipse v-else cx="50" :cy="70 + mouthY" rx="8" :ry="5 + mouthY * 0.3" fill="#fff"/>
          </svg>
        </div>
        <h2>你好，我是慧学通AI助手</h2>
        <p>我可以帮你查询业务数据、回答各种问题</p>

        <div class="quick-grid">
          <div v-for="(q, i) in quickQuestions" :key="i"
               class="quick-card" @click="sendQuickQuestion(q.text)">
            <span class="quick-icon">{{ q.icon }}</span>
            <span class="quick-text">{{ q.text }}</span>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-for="(msg, index) in messages" :key="index"
           :class="['message', msg.role]"
           v-show="!(msg.role === 'assistant' && !msg.content && loading)">
        <div class="message-avatar">
          <el-icon v-if="msg.role === 'user'"><User /></el-icon>
          <el-icon v-else><ChatDotRound /></el-icon>
        </div>
        <div class="message-content">{{ msg.content }}</div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading && (!messages.length || messages[messages.length - 1]?.role !== 'assistant' || messages[messages.length - 1]?.content === '')" class="message assistant">
        <div class="message-avatar">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="message-content typing">
          <span>正在思考中</span>
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <!-- 快捷问题栏 -->
    <div class="quick-bar" v-if="messages.length > 0">
      <div v-for="(q, i) in quickQuestions" :key="i"
           class="quick-tag" @click="sendQuickQuestion(q.text)">
        <span>{{ q.icon }} {{ q.text }}</span>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <el-button :icon="Plus" circle @click="clearChat" :disabled="loading" title="开启新对话" class="new-chat-btn" />
      <el-select v-model="selectedModel" class="model-select" :disabled="loading" @change="onModelSelect">
        <el-option v-for="m in modelOptions" :key="m.value" :label="m.label" :value="m.value" />
        <el-option label="⚙ AI 模型配置" value="__manage__" />
      </el-select>
      <ModelManageDialog v-model="modelManageVisible" @changed="loadModels" />
      <el-input v-model="inputMessage"
                :placeholder="loading ? 'AI 正在思考中，请稍候...' : '输入你的问题，按回车发送...'"
                @keydown.enter="sendMessage"
                :disabled="loading">
        <template #prefix>
          <el-icon><ChatDotRound /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" :icon="Promotion" @click="sendMessage"
                 :disabled="loading || !inputMessage.trim()"
                 :loading="loading"
                 class="send-btn">
        {{ loading ? '思考中' : '发送' }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: #f8f9fa;
  overflow: hidden;
  position: relative;
}
.ai-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  transition: background 0.3s ease;
}
.ai-chat > *:not(.ai-bg) {
  position: relative;
  z-index: 1;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

/* 欢迎页 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
}

.welcome-icon {
  width: 88px;
  height: 88px;
  border-radius: 24px;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(230, 0, 18, 0.35);
}

.welcome h2 {
  color: #1a1a1a;
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
}

.welcome p {
  color: #666;
  margin: 0 0 36px;
  font-size: 15px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  max-width: 520px;
  width: 100%;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(230, 0, 18, 0.15);
  border-color: #E60012;
  color: #E60012;
}

.quick-card:nth-child(4),
.quick-card:nth-child(5) {
  grid-column: span 1;
}

.quick-icon {
  font-size: 20px;
}

.quick-text {
  font-size: 13px;
  color: #333;
}

.quick-card:hover .quick-text {
  color: #E60012;
}

/* 消息 */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #E60012, #c0392b);
  color: white;
}

.message.assistant .message-avatar {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(24px);
  color: #1a1a1a;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.message-content {
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 16px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.message.user .message-content {
  background: linear-gradient(135deg, #E60012, #c0392b);
  color: white;
  border-top-right-radius: 4px;
}

.message.assistant .message-content {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  color: #333;
  border-top-left-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* 加载动画 */
.typing {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 14px 20px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #E60012;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 快捷问题栏 */
.quick-bar {
  display: flex;
  gap: 8px;
  padding: 10px 16px;
  overflow-x: auto;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.quick-bar::-webkit-scrollbar {
  height: 0;
}

.quick-tag {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 13px;
  color: #666;
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.quick-tag:hover {
  background: #fff;
  color: #E60012;
  border-color: #E60012;
}

/* 输入区域 */
.chat-input {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.new-chat-btn {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.08);
  color: #606266;
}

.new-chat-btn:hover {
  background: #E60012;
  border-color: #E60012;
  color: white;
}

.model-select {
  width: 180px;
  flex-shrink: 0;
}

.model-select :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.chat-input .el-input {
  flex: 1;
}

.chat-input .el-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding-left: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  background: rgba(255, 255, 255, 0.8);
}

.chat-input .el-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 12px rgba(230, 0, 18, 0.15);
}

.chat-input .el-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(230, 0, 18, 0.2), 0 2px 12px rgba(230, 0, 18, 0.15);
}

.send-btn {
  border-radius: 12px;
  padding: 0 24px;
  background: #1a1a1a;
  border: none;
  font-weight: 500;
}

.send-btn:hover {
  background: #E60012;
  border-color: #E60012;
}
</style>

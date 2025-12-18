<template>
  <div class="chat-container">
    <!-- 头部栏 -->
    <div class="chat-header">
      <a-button type="link" @click="goBack">
        返回
      </a-button>
      
      <div class="header-content">
        <div class="room-info">
          <span class="room-label">房间号:</span>
          <span class="room-id">{{ roomId }}</span>
        </div>
        
        <div class="game-controls">
          <a-button 
            :disabled="gameStarted || gameEnded" 
            @click="handleStart"
            type="primary"
            size="middle"
            class="control-button"
          >
            开始
          </a-button>
          <a-button 
            :disabled="gameEnded || !gameStarted" 
            @click="handleEnd"
            danger
            size="middle"
            class="control-button"
          >
            结束
          </a-button>
          <a-button 
            @click="handleDeleteRoom"
            type="link"
            danger
            size="middle"
            class="control-button delete-btn"
            :loading="deleting"
          >
            删除房间
          </a-button>
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <!-- 欢迎消息 -->
      <div v-if="showWelcome" class="message-container ai-message">
        <div class="message ai-message">
          <div class="avatar">
            🤖
          </div>
          <div class="message-content">
            <div class="message-bubble ai-bubble">
              <div class="message-text">欢迎来到AI脑筋急转弯！点击"开始"按钮或输入"开始"来开始游戏。</div>
              <div class="message-timestamp">{{ formatTime(new Date()) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 历史消息 -->
      <div 
        v-for="(message, index) in messages" 
        :key="index" 
        class="message-container"
        :class="message.sender"
      >
        <!-- AI消息 -->
        <div v-if="message.sender === 'ai'" class="message ai-message">
          <div class="avatar">
            🤖
          </div>
          <div class="message-content">
            <div class="message-bubble ai-bubble">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-timestamp">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
        </div>

        <!-- 用户消息 -->
        <div v-else class="message user-message">
          <div class="message-content">
            <div class="message-bubble user-bubble">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-timestamp">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
          <div class="avatar">
            👤
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-message">
        <a-spin size="small" />
        <span>AI正在思考...</span>
      </div>

      <!-- 游戏结束提示 -->
      <div v-if="gameEnded" class="end-message">
        <div class="end-bubble">
          ✅ 游戏已结束
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-container">
        <a-textarea
          v-model:value="inputText"
          :placeholder="placeholderText"
          :rows="3"
          :disabled="gameEnded"
          @pressEnter="handleSend"
          :autoSize="{ minRows: 3, maxRows: 5 }"
        />
        <div class="input-actions">
          <a-tooltip :title="gameEnded ? '游戏已结束' : '按Enter发送，Shift+Enter换行'">
            <a-button 
              type="primary" 
              @click="handleSend"
              :disabled="!inputText.trim() || gameEnded"
              :loading="loading"
              class="send-button"
            >
              {{ loading ? '发送中...' : '发送' }}
            </a-button>
          </a-tooltip>
        </div>
      </div>
      <div class="input-tips">
        <span class="tip-text">💡 提示：输入"开始"开始游戏，输入"结束"结束游戏</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { sendMessage, generateRoomId, deleteRoom } from '../services/api'

const router = useRouter()
const route = useRoute()
const messagesContainer = ref<HTMLElement>()

// 响应式数据
const roomId = ref<number>(parseInt(route.params.roomId as string) || generateRoomId())
const messages = ref<any[]>([])
const inputText = ref('')
const loading = ref(false)
const deleting = ref(false)
const gameStarted = ref(false)
const gameEnded = ref(false)
const showWelcome = ref(true)

// 计算属性：解决字符串转义问题
const placeholderText = computed(() => {
  return gameEnded.value 
    ? '游戏已结束，无法继续发送消息' 
    : '输入消息，输入"开始"来启动游戏...'
})

// 监听路由参数变化
watch(() => route.params.roomId, (newRoomId) => {
  if (newRoomId) {
    roomId.value = parseInt(newRoomId as string)
    resetGame()
  }
})

// 重置游戏状态
const resetGame = () => {
  messages.value = []
  gameStarted.value = false
  gameEnded.value = false
  showWelcome.value = true
  inputText.value = ''
}

// 格式化时间
const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit',
    hour12: false 
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 发送消息
const handleSend = async (e?: KeyboardEvent) => {
  if (e && e.shiftKey) {
    return
  }
  
  const text = inputText.value.trim()
  if (!text) return

  // 处理开始命令
  if (text === '开始' && !gameStarted.value) {
    inputText.value = ''
    await handleStart()
    return
  }

  // 处理结束命令
  if (text === '结束' && gameStarted.value && !gameEnded.value) {
    inputText.value = ''
    await handleEnd()
    return
  }

  // 添加用户消息
  const userMessage = {
    sender: 'user',
    content: text,
    timestamp: new Date()
  }
  messages.value.push(userMessage)

  inputText.value = ''
  loading.value = true
  showWelcome.value = false
  scrollToBottom()

  try {
    // 发送到后端
    const response = await sendMessage(roomId.value, text)
    const aiResponse = response.data

    // 添加AI回复
    const aiMessage = {
      sender: 'ai',
      content: aiResponse,
      timestamp: new Date()
    }
    messages.value.push(aiMessage)

    // 检查游戏是否结束
    const isGameEnd = aiResponse.includes('【游戏已结束】') || 
                      aiResponse.includes('游戏结束') ||
                      aiResponse.includes('游戏已结束')
    
    if (isGameEnd) {
      gameEnded.value = true
      message.info('游戏已结束！')
    }

  } catch (error: any) {
    console.error('发送消息失败:', error)
    
    let errorMessage = '发送消息失败，请重试'
    if (error.response?.status === 404) {
      errorMessage = '房间不存在或已被删除'
    } else if (error.response?.status === 500) {
      errorMessage = '服务器错误，请稍后重试'
    }
    
    message.error(errorMessage)
    
    // 添加错误提示
    messages.value.push({
      sender: 'ai',
      content: '抱歉，出现了一些问题，请稍后重试。',
      timestamp: new Date()
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 开始游戏
const handleStart = async () => {
  if (gameStarted.value || gameEnded.value) return

  gameStarted.value = true
  loading.value = true
  showWelcome.value = false

  try {
    // 发送开始命令
    const response = await sendMessage(roomId.value, '开始')
    const aiResponse = response.data

    messages.value.push({
      sender: 'ai',
      content: aiResponse,
      timestamp: new Date()
    })

    message.success('游戏开始！祝你好运！')
  } catch (error: any) {
    console.error('开始游戏失败:', error)
    
    let errorMessage = '开始游戏失败，请重试'
    if (error.response?.status === 404) {
      errorMessage = '房间不存在或已被删除'
      // 如果房间不存在，跳转回首页
      setTimeout(() => {
        router.push('/')
      }, 2000)
    }
    
    message.error(errorMessage)
    gameStarted.value = false
    
    // 添加错误提示
    messages.value.push({
      sender: 'ai',
      content: '抱歉，无法开始游戏，请检查网络连接或房间状态。',
      timestamp: new Date()
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 结束游戏
const handleEnd = async () => {
  if (gameEnded.value || !gameStarted.value) return

  loading.value = true

  try {
    const response = await sendMessage(roomId.value, '结束')
    const aiResponse = response.data

    // 添加用户消息
    messages.value.push({
      sender: 'user',
      content: '结束',
      timestamp: new Date()
    })

    // 添加AI回复
    messages.value.push({
      sender: 'ai',
      content: aiResponse,
      timestamp: new Date()
    })

    gameEnded.value = true
    message.info('游戏已结束！')

  } catch (error: any) {
    console.error('结束游戏失败:', error)
    
    let errorMessage = '结束游戏失败'
    if (error.response?.status === 404) {
      errorMessage = '房间不存在或已被删除'
    }
    
    message.error(errorMessage)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 删除房间
const handleDeleteRoom = async () => {
  Modal.confirm({
    title: '确认删除房间',
    content: `确定要删除房间 ${roomId.value} 吗？删除后将无法恢复对话记录。`,
    okText: '确认删除',
    cancelText: '取消',
    okType: 'danger',
    async onOk() {
      deleting.value = true
      try {
        await deleteRoom(roomId.value)
        message.success('房间删除成功')
        // 删除成功后跳转回首页
        router.push('/')
      } catch (error: any) {
        console.error('删除房间失败:', error)
        
        let errorMessage = '删除房间失败'
        if (error.response?.status === 404) {
          errorMessage = '房间不存在'
        } else if (error.response?.status === 500) {
          errorMessage = '服务器错误，删除失败'
        }
        
        message.error(errorMessage)
      } finally {
        deleting.value = false
      }
    }
  })
}

// 返回首页
const goBack = () => {
  router.push('/')
}

// 组件挂载时滚动到底部
onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.chat-header {
  background: white;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
  border-bottom: 1px solid #e8e8e8;
}

.header-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-left: 20px;
}

.room-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: #f0f5ff;
  border-radius: 16px;
  border: 1px solid #d6e4ff;
}

.room-label {
  color: #666;
  font-weight: 500;
}

.room-id {
  font-weight: bold;
  font-size: 1.2rem;
  color: #1890ff;
  background: white;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid #91d5ff;
}

.game-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.control-button {
  min-width: 80px;
  font-weight: 500;
}

.delete-btn {
  color: #ff4d4f;
  border-color: #ffa39e;
}

.delete-btn:hover {
  background: #fff1f0;
  color: #ff7875;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: rgba(255, 255, 255, 0.8);
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-message {
  justify-content: flex-start;
}

.user-message {
  justify-content: flex-end;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.ai-message .avatar {
  background: linear-gradient(135deg, #f0f0f0 0%, #d9d9d9 100%);
  color: #666;
}

.user-message .avatar {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
}

.message-content {
  max-width: 70%;
  min-width: 200px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  word-wrap: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.ai-bubble {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 0 18px 18px 18px;
  border-left: 4px solid #52c41a;
}

.user-bubble {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  border-radius: 18px 0 18px 18px;
}

.message-text {
  line-height: 1.5;
  font-size: 14px;
  margin-bottom: 4px;
}

.message-timestamp {
  font-size: 11px;
  opacity: 0.7;
  text-align: right;
  margin-top: 4px;
}

.user-bubble .message-timestamp {
  color: rgba(255, 255, 255, 0.8);
}

.loading-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: #666;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  align-self: flex-start;
  max-width: 200px;
}

.end-message {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.end-bubble {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
  padding: 8px 16px;
  border-radius: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.chat-input-area {
  background: white;
  padding: 16px 24px;
  border-top: 1px solid #e8e8e8;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
}

.send-button {
  min-width: 120px;
  height: 40px;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
}

.send-button:hover {
  box-shadow: 0 4px 8px rgba(24, 144, 255, 0.4);
}

.input-tips {
  margin-top: 12px;
  text-align: center;
}

.tip-text {
  font-size: 12px;
  color: #666;
  opacity: 0.8;
}
</style>
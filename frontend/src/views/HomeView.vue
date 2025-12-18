<template>
  <div class="home-container">
    <div class="content-wrapper">
      <!-- 标题区域 -->
      <div class="title-section">
        <h1 class="main-title">AI脑筋急转弯</h1>
        <p class="subtitle">挑战AI主持人的脑筋急转弯</p>
      </div>

      <!-- 开始游戏按钮 -->
      <div class="action-section">
        <a-button 
          type="primary" 
          size="large" 
          class="start-button"
          @click="startNewGame"
        >
          点击开始
        </a-button>
      </div>

      <!-- 历史记录区域 -->
      <div class="history-section">
        <h2 class="section-title">往期对话记录</h2>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <a-spin size="large" />
          <span class="loading-text">加载中...</span>
        </div>
        
        <!-- 房间列表 -->
        <div v-else-if="rooms.length > 0" class="rooms-container">
          <div 
            v-for="room in sortedRooms" 
            :key="room.id"
            class="room-card"
          >
            <div class="room-header">
              <div class="room-info-left" @click="enterRoom(room.id)">
                <div class="room-id-section">
                  <span class="room-label">房间号:</span>
                  <span class="room-id">{{ room.id }}</span>
                </div>
                <div class="room-status-section">
                  <a-tag :color="getStatusColor(room.active)" class="status-tag">
                    {{ room.active ? '进行中' : '已结束' }}
                  </a-tag>
                </div>
              </div>
              
              <div class="room-actions">
                <a-tooltip title="进入房间">
                  <a-button 
                    type="primary" 
                    size="small"
                    @click="enterRoom(room.id)"
                    class="action-btn enter-btn"
                  >
                    进入
                  </a-button>
                </a-tooltip>
                
                <a-tooltip title="删除房间">
                  <a-button 
                    type="primary" 
                    danger
                    size="small"
                    @click.stop="handleDeleteRoom(room.id)"
                    class="action-btn delete-btn"
                    :loading="deletingRoomId === room.id"
                  >
                    删除
                  </a-button>
                </a-tooltip>
              </div>
            </div>
            
            <div class="room-details">
              <div class="room-time">
                创建时间: {{ formatTime(room.createTime) }}
              </div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-else class="empty-container">
          <a-empty description="暂无历史对话记录">
            <template #description>
              <span>暂无历史对话记录</span>
              <br />
              <span style="font-size: 12px; color: #999;">点击上方按钮开始新游戏</span>
            </template>
          </a-empty>
        </div>
      </div>
      
      <!-- 页脚 -->
      <div class="footer-section">
        <p class="footer-text">
          AI脑筋急转弯 ©2024 · 与AI主持人一起挑战你的思维能力
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getRooms, generateRoomId, deleteRoom } from '../services/api'

const router = useRouter()
const rooms = ref<any[]>([])
const loading = ref(true)
const deletingRoomId = ref<number | null>(null)

// 计算排序后的房间列表（按创建时间倒序）
const sortedRooms = computed(() => {
  return [...rooms.value].sort((a, b) => {
    const timeA = new Date(a.createTime || 0).getTime()
    const timeB = new Date(b.createTime || 0).getTime()
    return timeB - timeA
  })
})

// 开始新游戏
const startNewGame = () => {
  const roomId = generateRoomId()
  router.push(`/chat/${roomId}`)
}

// 进入历史房间
const enterRoom = (roomId: number) => {
  router.push(`/chat/${roomId}`)
}

// 获取房间列表
const fetchRooms = async () => {
  loading.value = true
  try {
    const response = await getRooms()
    rooms.value = response.data || []
  } catch (error: any) {
    console.error('获取房间列表失败:', error)
    
    let errorMessage = '加载历史记录失败'
    if (error.response?.status === 500) {
      errorMessage = '服务器错误，请稍后重试'
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '请求超时，请检查网络连接'
    }
    
    message.error(errorMessage)
    rooms.value = []
  } finally {
    loading.value = false
  }
}

// 删除房间
const handleDeleteRoom = async (roomId: number) => {
  Modal.confirm({
    title: '确认删除房间',
    content: `确定要删除房间 ${roomId} 吗？删除后将无法恢复对话记录。`,
    okText: '确认删除',
    cancelText: '取消',
    okType: 'danger',
    async onOk() {
      deletingRoomId.value = roomId
      try {
        await deleteRoom(roomId)
        message.success('房间删除成功')
        // 从列表中移除已删除的房间
        rooms.value = rooms.value.filter(room => room.id !== roomId)
      } catch (error: any) {
        console.error('删除房间失败:', error)
        
        let errorMessage = '删除房间失败'
        if (error.response?.status === 404) {
          errorMessage = '房间不存在或已被删除'
        } else if (error.response?.status === 500) {
          errorMessage = '服务器错误，删除失败'
        }
        
        message.error(errorMessage)
      } finally {
        deletingRoomId.value = null
      }
    }
  })
}

// 获取状态颜色
const getStatusColor = (active: boolean) => {
  return active ? 'green' : 'default'
}

// 格式化时间
const formatTime = (timestamp: string | number) => {
  if (!timestamp) return '未知时间'
  
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffMins < 60) {
    return `${diffMins}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

onMounted(() => {
  fetchRooms()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-wrapper {
  background: white;
  border-radius: 24px;
  padding: 40px;
  max-width: 900px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.title-section {
  text-align: center;
  margin-bottom: 40px;
}

.main-title {
  font-size: 3.5rem;
  font-weight: 800;
  color: #1890ff;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #1890ff 0%, #722ed1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.subtitle {
  font-size: 1.2rem;
  color: #666;
  opacity: 0.9;
}

.action-section {
  text-align: center;
  margin-bottom: 40px;
  padding: 20px;
  background: linear-gradient(135deg, #f6f8ff 0%, #f0f5ff 100%);
  border-radius: 16px;
  border: 1px solid #d6e4ff;
}

.start-button {
  height: 60px;
  width: 220px;
  font-size: 1.3rem;
  border-radius: 30px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
  transition: all 0.3s ease;
}

.start-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(24, 144, 255, 0.4);
}

.section-title {
  font-size: 1.5rem;
  color: #333;
  margin-bottom: 20px;
  text-align: center;
  font-weight: 600;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 16px;
}

.loading-text {
  color: #666;
  font-size: 14px;
}

.rooms-container {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 10px;
}

.rooms-container::-webkit-scrollbar {
  width: 6px;
}

.rooms-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.rooms-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.rooms-container::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

.room-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.room-card:hover {
  background: #f9f9f9;
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
  transform: translateY(-2px);
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.room-info-left {
  flex: 1;
  cursor: pointer;
}

.room-info-left:hover .room-id {
  color: #40a9ff;
}

.room-id-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.room-label {
  color: #666;
  font-weight: 500;
}

.room-id {
  font-weight: bold;
  font-size: 1.3rem;
  color: #1890ff;
  transition: color 0.3s ease;
}

.room-status-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tag {
  margin: 0;
  font-weight: 500;
}

.room-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.enter-btn {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border: none;
}

.delete-btn {
  background: linear-gradient(135deg, #ff4d4f 0%, #d9363e 100%);
  border: none;
}

.room-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.room-time {
  display: flex;
  align-items: center;
}

.empty-container {
  padding: 40px 20px;
  text-align: center;
}

.footer-section {
  margin-top: 40px;
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e8e8e8;
}

.footer-text {
  color: #999;
  font-size: 12px;
  opacity: 0.8;
}
</style>
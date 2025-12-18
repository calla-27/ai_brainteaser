import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 发送消息
export const sendMessage = (roomId: number, userPrompt: string) => {
  return api.post(`/${roomId}/chat`, null, {
    params: { userPrompt }
  })
}

// 获取房间列表
export const getRooms = () => {
  return api.get('/rooms')
}

// 删除房间
export const deleteRoom = (roomId: number) => {
  return api.delete(`/rooms/${roomId}`)
}

// 生成随机房间号
export const generateRoomId = (): number => {
  return Math.floor(Math.random() * 90000) + 10000
}
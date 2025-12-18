package com.xyyzhang.ai_brainteaser.service.impl;

import com.xyyzhang.ai_brainteaser.model.ChatRoom;
import com.xyyzhang.ai_brainteaser.service.AiManage;
import com.xyyzhang.ai_brainteaser.service.ChatService;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private AiManage aiManager;

    Map<Long,List<ChatMessage>> chatHistories = new HashMap<>();

    @Override
    public String doChat(long roomId, String userPrompt) {
        // 极简系统提示词
        String systemPrompt = "你是脑筋急转弯主持人。用户说开始时出题，之后只回答是/否/无关。";

        List<ChatMessage> messages;

        // 创建用户消息
        final ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(userPrompt)
                .build();

        // 处理对话历史
        if (!chatHistories.containsKey(roomId)) {
            // 新对话，创建系统消息和历史记录
            messages = new ArrayList<>();
            final ChatMessage systemMessage = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build();
            messages.add(systemMessage);
            chatHistories.put(roomId, messages);
        } else {
            // 获取现有对话历史
            messages = chatHistories.get(roomId);
        }

        // 添加用户消息到历史记录
        messages.add(userMessage);

        // 调用AI获取回复
        String answer = aiManager.doChat(messages);

        // 添加AI回复到历史记录
        final ChatMessage answerMessage = ChatMessage.builder()
                .role(ChatMessageRole.ASSISTANT)
                .content(answer)
                .build();
        messages.add(answerMessage);

        // 如果游戏结束，清理历史记录
//        if (answer.contains("游戏结束")) {
//            chatHistories.remove(roomId);
//        }

        return answer;
    }

    @Override
    public List<ChatRoom> getChatRooms() {
        List<ChatRoom> chatRoomList = new ArrayList<>();
        for(Map.Entry<Long, List<ChatMessage>> entry : chatHistories.entrySet()){
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setRoomId(entry.getKey());
            // 只返回非系统消息，避免重复显示系统提示
            List<ChatMessage> messages = entry.getValue();
            List<ChatMessage> userMessages = new ArrayList<>();
            for(ChatMessage msg : messages) {
                if(!msg.getRole().equals(ChatMessageRole.SYSTEM)) {
                    userMessages.add(msg);
                }
            }
            chatRoom.setChatMessage(userMessages);
            chatRoomList.add(chatRoom);
        }
        return chatRoomList;
    }

    @Override
    public boolean deleteRoom(long roomId) {
        if (chatHistories.containsKey(roomId)) {
            chatHistories.remove(roomId);
            return true;
        }
        return false;
    }
}

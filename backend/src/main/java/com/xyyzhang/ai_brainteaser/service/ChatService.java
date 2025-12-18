package com.xyyzhang.ai_brainteaser.service;

import com.xyyzhang.ai_brainteaser.model.ChatRoom;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;

import java.util.List;

public interface ChatService
{
    //输入房间号，与特定用户对话
    String doChat(long rommId,String userPrompt);

    //当前所有聊天对话
    List<ChatRoom> getChatRooms();

    //删除指定房间
    boolean deleteRoom(long roomId);
}

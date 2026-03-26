import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { conversationAPI } from '../../services/api/apiClient';
import { socketService } from '../../services/socketService';
import { ConversationDisplayState, MessageDTO, ConversationRes, ConversationType } from '../../types';

interface ChatState {
    conversations: ConversationDisplayState[];
    activeConversationId: number | null;
    messages: Record<number, MessageDTO[]>;
    loading: boolean;
    error: string | null;
}

const initialState: ChatState = {
    conversations: [],
    activeConversationId: null,
    messages: {},
    loading: false,
    error: null,
};

export const fetchConversations = createAsyncThunk(
    'chat/fetchConversations',
    async (userId: string) => {
        const response = await conversationAPI.getActiveConversations(userId);
        return response.data?.data || response.data || [];
    }
);

export const fetchMessages = createAsyncThunk(
    'chat/fetchMessages',
    async (conversationId: number, { getState }) => {
        const state = getState() as any;
        const currentMessages = state.chat.messages[conversationId] || [];
        
        // Find max sequence ID if any
        let maxSequenceId = 0;
        if (currentMessages.length > 0) {
            maxSequenceId = Math.max(...currentMessages.map((m: any) => m.sequenceId || 0));
        }

        const response = await conversationAPI.pullMessages(conversationId, maxSequenceId);
        let newMessages = response.data?.data?.content || response.data?.content || [];
        
        // Normalize timestamps to numbers (ms)
        newMessages = newMessages.map((msg: any) => ({
            ...msg,
            timestamp: msg.timestamp ? new Date(msg.timestamp).getTime() : Date.now()
        }));

        return { conversationId, messages: newMessages, isIncremental: maxSequenceId > 0 };
    }
);

export const sendMessage = createAsyncThunk(
    'chat/sendMessage',
    async ({ conversationId, content, type }: { conversationId: number, content: string, type?: string }) => {
        const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
        return response.data?.data || response.data; // Return the created message DTO
    }
);

/**
 * 通过Socket发送消息
 * 这是Web端的Socket消息发送实装，参考Android端AndroidSocketClient的实现
 */
export const sendMessageViaSocket = createAsyncThunk(
    'chat/sendMessageViaSocket',
    async ({ conversationId, content, type, msgDto }: {
        conversationId: number,
        content: string,
        type?: string,
        msgDto?: MessageDTO
    }, { getState }) => {
        const state = getState() as any; // Using any to avoid circular import or complex type casting here
        const currentUser = state.auth.user;
        const currentUserId = currentUser?.userId;

        try {
            // 首先检查Socket是否连接
            const socketActive = await socketService.isActive();

            if (!socketActive) {
                // 如果长连接 没有开启 那么就尝试 使用http 发送
                console.warn('Socket not active, falling back to HTTP');
                const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
                return response.data?.data || response.data;
            }

            // 构建消息负载对象
            const payload = buildSocketPayload(conversationId, content, type || 'TEXT', currentUser, msgDto);

            // 通过Socket发送负载
            const sendSuccess = await socketService.sendPayload(payload);

            if (sendSuccess) {
                console.log('Message sent via Socket successfully');
                // 如果通过Socket发送成功，不再调用HTTP API，直接返回一个带有clientMsgId的乐观DTO
                if (msgDto) {
                    return { ...msgDto, clientMsgId: payload.message.clientMsgId };
                }

                return {
                    // msgId: -Date.now(), // 临时本地的消息吗不需要传入msgId
                    conversationId: conversationId,
                    content: content,
                    fromAccountId: Number(currentUserId),
                    type: (type as any) || 'TEXT',
                    timestamp: Date.now(),  // 应该使用时间戳  毫秒级别即可
                    clientMsgId: payload.message.clientMsgId
                } as MessageDTO;
            } else {
                console.warn('Socket send failed, trying HTTP fallback');
                const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
                return response.data?.data || response.data;
            }
        } catch (error) {
            console.error('Error in sendMessageViaSocket:', error);
            const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
            return response.data?.data || response.data;
        }
    }
);

/**
 * 构建Socket消息负载对象
 * 匹配 protobuf 中的 BaseMessagePkg 结构
 */
function buildSocketPayload(
    conversationId: number,
    content: string,
    type: string,
    currentUser: any,
    msgDto?: MessageDTO
): any {
    const timestamp = msgDto?.timestamp ? new Date(msgDto.timestamp).getTime() : Date.now();

    // Ensure messageType matches enum format (TEXT, IMAGE, FILE, VOICE, VIDEO)
    const messageType = type ? type.toUpperCase() : 'TEXT';

    // 默认使用 crypto.randomUUID() 作为 clientMsgId，这是 UUID 标准实现
    const clientMsgId = msgDto?.clientMsgId || (window.crypto && window.crypto.randomUUID ? window.crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36));

    return {
        message: {
            conversationId: conversationId,
            content: content,
            type: messageType,
            clientTimeStamp: timestamp,
            clientMsgId: clientMsgId,
            fromUser: {
                userId: currentUser?.userId,
                username: currentUser?.username
            }
        }
    };
}

export const createPrivateChat = createAsyncThunk(
    'chat/createPrivateChat',
    async ({ userId, friendId }: { userId: string, friendId: number }) => {
        const response = await conversationAPI.createPrivateChat(userId, friendId);
        return response.data?.data || response.data;
    }
);

const chatSlice = createSlice({
    name: 'chat',
    initialState,
    reducers: {
        setActiveConversation(state, action: PayloadAction<number | null>) {
            state.activeConversationId = action.payload;
            if (action.payload) {
                // 当进入会话时，清除该会话的未读数
                const conv = state.conversations.find(c => c.conversation.conversationId === action.payload);
                if (conv) {
                    conv.unreadCount = 0;
                }
            }
        },
        addMessage(state, action: PayloadAction<MessageDTO>) {
            const { conversationId, fromAccountId, msgId, clientMsgId } = action.payload;
            const currentUserId = (state as any).auth?.user?.userId;

            if (!state.messages[conversationId]) {
                state.messages[conversationId] = [];
            }

            // Normalize timestamp for incoming socket message
            const normalizedMsg = {
                ...action.payload,
                timestamp: action.payload.timestamp ? new Date(action.payload.timestamp).getTime() : Date.now()
            };

            // 去重逻辑：通过 msgId 或 clientMsgId 判断
            const existingIndex = state.messages[conversationId].findIndex(m =>
                (m.msgId === normalizedMsg.msgId && m.msgId > 0) ||
                (m.clientMsgId && normalizedMsg.clientMsgId && m.clientMsgId === normalizedMsg.clientMsgId)
            );

            if (existingIndex !== -1) {
                // 更新现有消息（例如从临时状态变为服务器确认状态）
                state.messages[conversationId][existingIndex] = normalizedMsg;
            } else {
                state.messages[conversationId].push(normalizedMsg);
                // 每次新增消息后重新排序，确保渲染一致
                state.messages[conversationId].sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));

                // 未读数逻辑：不是自己发的消息，且不是当前活跃会话
                if (fromAccountId.toString() !== currentUserId && conversationId !== state.activeConversationId) {
                    const conv = state.conversations.find(c => c.conversation.conversationId === conversationId);
                    if (conv) {
                        conv.unreadCount += 1;
                        conv.lastMessage = normalizedMsg.content;
                    }
                } else if (conversationId === state.activeConversationId) {
                    // 如果是当前活跃会话的消息，更新最后一条消息预览
                    const conv = state.conversations.find(c => c.conversation.conversationId === conversationId);
                    if (conv) {
                        conv.lastMessage = normalizedMsg.content;
                    }
                    // 虽然当前在聊天室，但根据产品定义，这里可以调用 markAsRead (通过 thunk 发送 ACK)
                }
            }
        }
    },
    extraReducers: (builder) => {
        builder
            .addCase(fetchConversations.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchConversations.fulfilled, (state, action) => {
                state.loading = false;
                state.conversations = action.payload.map((conv: ConversationRes) => ({
                    conversation: conv,
                    lastMessage: conv.lastMessage?.content || '',
                    displayDateTime: new Date(conv.createAt).toLocaleTimeString(),
                    unreadCount: 0
                }));
            })
            .addCase(fetchMessages.fulfilled, (state, action) => {
                const { conversationId, messages, isIncremental } = action.payload;
                
                if (!state.messages[conversationId]) {
                    state.messages[conversationId] = [];
                }

                if (isIncremental) {
                    // 合并新旧消息，并通过 msgId 或 clientMsgId 去重
                    const existingMessages = state.messages[conversationId];
                    const newMessages = messages.filter((nMsg: MessageDTO) => 
                        !existingMessages.some(eMsg => 
                            (eMsg.msgId > 0 && eMsg.msgId === nMsg.msgId) || 
                            (eMsg.clientMsgId && eMsg.clientMsgId === nMsg.clientMsgId)
                        )
                    );
                    state.messages[conversationId] = [...existingMessages, ...newMessages].sort((a: MessageDTO, b: MessageDTO) => Number(a.timestamp || 0) - Number(b.timestamp || 0));
                } else {
                    state.messages[conversationId] = messages.sort((a: MessageDTO, b: MessageDTO) => Number(a.timestamp || 0) - Number(b.timestamp || 0));
                }
            })
            .addCase(sendMessage.fulfilled, (state, action: PayloadAction<MessageDTO>) => {
                const { conversationId } = action.payload;
                if (!state.messages[conversationId]) {
                    state.messages[conversationId] = [];
                }

                const existingIndex = state.messages[conversationId].findIndex(m =>
                    (m.msgId === action.payload.msgId && m.msgId > 0) ||
                    (m.clientMsgId && action.payload.clientMsgId && m.clientMsgId === action.payload.clientMsgId)
                );

                if (existingIndex !== -1) {
                    state.messages[conversationId][existingIndex] = action.payload;
                } else {
                    state.messages[conversationId].push(action.payload);
                }
            })
            .addCase(sendMessageViaSocket.pending, (state) => {
                // Socket发送时不显示loading，保持UI响应性
            })
            .addCase(sendMessageViaSocket.fulfilled, (state, action: PayloadAction<MessageDTO>) => {
                const { conversationId } = action.payload;
                if (!state.messages[conversationId]) {
                    state.messages[conversationId] = [];
                }

                const existingIndex = state.messages[conversationId].findIndex(m =>
                    (m.msgId === action.payload.msgId && m.msgId > 0) ||
                    (m.clientMsgId && action.payload.clientMsgId && m.clientMsgId === action.payload.clientMsgId)
                );

                if (existingIndex !== -1) {
                    state.messages[conversationId][existingIndex] = action.payload;
                } else {
                    state.messages[conversationId].push(action.payload);
                }
            })
            .addCase(sendMessageViaSocket.rejected, (state, action) => {
                state.error = action.error.message || '发送消息失败';
            })
            .addCase(createPrivateChat.fulfilled, (state, action: PayloadAction<ConversationRes>) => {
                const newConv = action.payload;
                state.activeConversationId = newConv.conversationId;

                // Add to conversations list if not already there
                const existing = state.conversations.find(c => c.conversation.conversationId === newConv.conversationId);
                if (!existing) {
                    state.conversations.unshift({
                        conversation: newConv,
                        lastMessage: '',
                        displayDateTime: new Date().toLocaleTimeString(),
                        unreadCount: 0
                    });
                }
            });
    },
});

export const { setActiveConversation, addMessage } = chatSlice.actions;
export default chatSlice.reducer;

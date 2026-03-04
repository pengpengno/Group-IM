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
    async (conversationId: number) => {
        const response = await conversationAPI.pullMessages(conversationId);
        // Ensure response data mapping matches MessageDTO structure
        const messages = response.data?.data?.content || response.data?.content || [];
        return { conversationId, messages };
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
        const currentUserId = state.auth.user?.userId;

        try {
            // 首先检查Socket是否连接
            const socketActive = await socketService.isActive();

            if (!socketActive) {
                console.warn('Socket not active, falling back to HTTP');
                const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
                return response.data?.data || response.data;
            }

            // 构建消息负载对象
            const payload = buildSocketPayload(conversationId, content, type, msgDto);

            // 通过Socket发送负载
            const sendSuccess = await socketService.sendPayload(payload);

            if (sendSuccess) {
                console.log('Message sent via Socket successfully');
                // 如果通过Socket发送成功，不再调用HTTP API，直接返回一个带有clientMsgId的乐观DTO
                if (msgDto) {
                    return { ...msgDto, clientMsgId: payload.message.clientMsgId };
                }

                return {
                    msgId: -Date.now(), // 使用负数ID作为临时标识
                    conversationId: conversationId,
                    content: content,
                    fromAccountId: Number(currentUserId),
                    type: (type as any) || 'TEXT',
                    timestamp: new Date().toISOString(),
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
    type?: string,
    msgDto?: MessageDTO
): any {
    const timestamp = msgDto?.timestamp ? new Date(msgDto.timestamp).getTime() : Date.now();

    return {
        message: {
            conversationId: conversationId,
            content: content,
            type: type || 'TEXT',
            clientTimeStamp: timestamp,
            clientMsgId: msgDto?.msgId?.toString() || Math.random().toString(36).substring(7),
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
        },
        addMessage(state, action: PayloadAction<MessageDTO>) {
            const { conversationId } = action.payload;
            if (!state.messages[conversationId]) {
                state.messages[conversationId] = [];
            }

            // 去重逻辑：通过 msgId 或 clientMsgId 判断
            const existingIndex = state.messages[conversationId].findIndex(m =>
                (m.msgId === action.payload.msgId && m.msgId > 0) ||
                (m.clientMsgId && action.payload.clientMsgId && m.clientMsgId === action.payload.clientMsgId)
            );

            if (existingIndex !== -1) {
                // 更新现有消息（例如从临时状态变为服务器确认状态）
                state.messages[conversationId][existingIndex] = action.payload;
            } else {
                state.messages[conversationId].push(action.payload);
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
                state.messages[action.payload.conversationId] = action.payload.messages;
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

import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { conversationAPI } from '../../services/api/apiClient';
import { socketService } from '../../services/socketService';
import { ConversationDisplayState, MessageDTO, ConversationRes, GroupConversationPayload } from '../../types';

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

function normalizeConversation(conv: ConversationRes): ConversationRes {
    return {
        ...conv,
        members: Array.isArray(conv.members) ? conv.members : []
    };
}

function getMessageDisplayText(content: string, type?: string): string {
    const msgType = (type || 'TEXT').toUpperCase();
    switch (msgType) {
        case 'IMAGE': return '[Image]';
        case 'FILE': return '[File]';
        case 'VOICE': return '[Voice]';
        case 'VIDEO': return '[Video]';
        case 'MEETING': return '[Meeting]';
        default: return content || '';
    }
}


function buildConversationDisplayState(conv: ConversationRes): ConversationDisplayState {
    const normalized = normalizeConversation(conv);
    const lastMessageText = normalized.lastMessage
        ? getMessageDisplayText(normalized.lastMessage.content, normalized.lastMessage.type)
        : '';

    let displayDateTime = '';
    const ts = normalized.lastMessage?.timestamp || normalized.createAt;
    if (ts) {
        const date = new Date(ts);
        const now = new Date();
        const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
        if (diffDays === 0 && date.getDate() === now.getDate()) {
            displayDateTime = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else if (diffDays <= 1 && date.getDate() === now.getDate() - 1) {
            displayDateTime = '昨天';
        } else if (diffDays < 7) {
            const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
            displayDateTime = weekdays[date.getDay()];
        } else {
            displayDateTime = `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
        }
    }

    return {
        conversation: normalized,
        lastMessage: lastMessageText,
        displayDateTime,
        unreadCount: 0
    };
}

function upsertConversation(state: ChatState, conversation: ConversationRes) {
    const displayState = buildConversationDisplayState(conversation);
    const existingIndex = state.conversations.findIndex(
        (item) => item.conversation.conversationId === displayState.conversation.conversationId
    );

    if (existingIndex >= 0) {
        const unreadCount = state.conversations[existingIndex].unreadCount;
        state.conversations[existingIndex] = {
            ...displayState,
            unreadCount
        };
        const [updated] = state.conversations.splice(existingIndex, 1);
        state.conversations.unshift(updated);
        return;
    }

    state.conversations.unshift(displayState);
}

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

        let maxSequenceId = 0;
        if (currentMessages.length > 0) {
            maxSequenceId = Math.max(...currentMessages.map((m: any) => m.sequenceId || 0));
        }

        const response = await conversationAPI.pullMessages(conversationId, maxSequenceId);
        let newMessages = response.data?.data?.content || response.data?.content || [];

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
        return response.data?.data || response.data;
    }
);

function buildSocketPayload(
    conversationId: number,
    content: string,
    type: string,
    currentUser: any,
    msgDto?: MessageDTO
): any {
    const timestamp = msgDto?.timestamp ? new Date(msgDto.timestamp).getTime() : Date.now();
    const messageType = type ? type.toUpperCase() : 'TEXT';
    const clientMsgId = msgDto?.clientMsgId || (window.crypto && window.crypto.randomUUID
        ? window.crypto.randomUUID()
        : Math.random().toString(36).substring(2) + Date.now().toString(36));

    return {
        message: {
            conversationId,
            content,
            type: messageType,
            clientTimeStamp: timestamp,
            clientMsgId,
            fromUser: {
                userId: currentUser?.userId,
                username: currentUser?.username
            }
        }
    };
}

export const sendMessageViaSocket = createAsyncThunk(
    'chat/sendMessageViaSocket',
    async ({ conversationId, content, type, msgDto, clientMsgId }: {
        conversationId: number,
        content: string,
        type?: string,
        msgDto?: MessageDTO,
        clientMsgId?: string
    }, { getState }) => {
        const state = getState() as any;
        const currentUser = state.auth.user;
        const currentUserId = currentUser?.userId;

        // Use provided clientMsgId or from msgDto or generate new
        const finalClientMsgId = clientMsgId || msgDto?.clientMsgId ||
            (window.crypto && window.crypto.randomUUID ? window.crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36));

        try {
            const socketActive = await socketService.isActive();
            if (!socketActive) {
                console.warn('Socket not active, falling back to HTTP');
                const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
                const result = response.data?.data || response.data;
                return { ...result, clientMsgId: finalClientMsgId, sendingStatus: 'success' } as MessageDTO;
            }

            const payload = buildSocketPayload(conversationId, content, type || 'TEXT', currentUser, { ...msgDto, clientMsgId: finalClientMsgId } as any);
            const sendSuccess = await socketService.sendPayload(payload);

            if (sendSuccess) {
                return {
                    conversationId,
                    content,
                    fromAccountId: Number(currentUserId),
                    type: (type as any) || 'TEXT',
                    timestamp: Date.now(),
                    clientMsgId: finalClientMsgId,
                    sendingStatus: 'success'
                } as MessageDTO;
            }

            console.warn('Socket send failed, trying HTTP fallback');
            const response = await conversationAPI.sendMessage(conversationId, content, type || 'TEXT');
            const result = response.data?.data || response.data;
            return { ...result, clientMsgId: finalClientMsgId, sendingStatus: 'success' } as MessageDTO;
        } catch (error) {
            console.error('Error in sendMessageViaSocket:', error);
            // Re-throw if it's a real failure so we can catch it in rejected
            throw error;
        }
    }
);

export const createPrivateChat = createAsyncThunk(
    'chat/createPrivateChat',
    async ({ userId, friendId }: { userId: string, friendId: number }) => {
        const response = await conversationAPI.createPrivateChat(userId, friendId);
        return response.data?.data || response.data;
    }
);

export const createGroupConversation = createAsyncThunk(
    'chat/createGroupConversation',
    async (payload: GroupConversationPayload) => {
        const response = await conversationAPI.createGroup(payload);
        return response.data?.data || response.data;
    }
);

export const addConversationMembers = createAsyncThunk(
    'chat/addConversationMembers',
    async ({ conversationId, userIds }: { conversationId: number; userIds: number[] }, { getState }) => {
        await conversationAPI.addGroupMembers(conversationId, userIds);
        const state = getState() as { auth: { user: { userId: string } | null } };
        const currentUserId = state.auth.user?.userId;

        if (!currentUserId) {
            throw new Error('User context is missing');
        }

        const response = await conversationAPI.getActiveConversations(currentUserId);
        const conversations = response.data?.data || response.data || [];
        const updatedConversation = conversations.find((conversation: ConversationRes) => conversation.conversationId === conversationId);

        if (!updatedConversation) {
            throw new Error('Updated conversation not found');
        }

        return updatedConversation;
    }
);

const chatSlice = createSlice({
    name: 'chat',
    initialState,
    reducers: {
        setActiveConversation(state, action: PayloadAction<number | null>) {
            state.activeConversationId = action.payload;
            if (action.payload) {
                const conv = state.conversations.find(c => c.conversation.conversationId === action.payload);
                if (conv) {
                    conv.unreadCount = 0;
                }
            }
        },
        addMessage(state, action: PayloadAction<MessageDTO>) {
            const { conversationId, fromAccountId } = action.payload;
            const currentUserId = (state as any).auth?.user?.userId;

            if (!state.messages[conversationId]) {
                state.messages[conversationId] = [];
            }

            const normalizedMsg = {
                ...action.payload,
                timestamp: action.payload.timestamp ? new Date(action.payload.timestamp).getTime() : Date.now()
            };

            const existingIndex = state.messages[conversationId].findIndex(m =>
                (m.msgId === normalizedMsg.msgId && m.msgId > 0) ||
                (m.clientMsgId && normalizedMsg.clientMsgId && m.clientMsgId === normalizedMsg.clientMsgId)
            );

            if (existingIndex !== -1) {
                state.messages[conversationId][existingIndex] = normalizedMsg;
            } else {
                state.messages[conversationId].push(normalizedMsg);
                state.messages[conversationId].sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));

                const conv = state.conversations.find(c => c.conversation.conversationId === conversationId);
                if (conv) {
                    conv.lastMessage = getMessageDisplayText(normalizedMsg.content, normalizedMsg.type);
                    if (fromAccountId.toString() !== currentUserId && conversationId !== state.activeConversationId) {
                        conv.unreadCount += 1;
                    }
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
                state.conversations = action.payload.map((conv: ConversationRes) => buildConversationDisplayState(conv));
            })
            .addCase(fetchMessages.fulfilled, (state, action) => {
                const { conversationId, messages, isIncremental } = action.payload;

                if (!state.messages[conversationId]) {
                    state.messages[conversationId] = [];
                }

                if (isIncremental) {
                    const existingMessages = state.messages[conversationId];
                    const newMessages = messages.filter((nMsg: MessageDTO) =>
                        !existingMessages.some(eMsg =>
                            (eMsg.msgId > 0 && eMsg.msgId === nMsg.msgId) ||
                            (eMsg.clientMsgId && eMsg.clientMsgId === nMsg.clientMsgId)
                        )
                    );
                    state.messages[conversationId] = [...existingMessages, ...newMessages]
                        .sort((a: MessageDTO, b: MessageDTO) => Number(a.timestamp || 0) - Number(b.timestamp || 0));
                } else {
                    state.messages[conversationId] = messages
                        .sort((a: MessageDTO, b: MessageDTO) => Number(a.timestamp || 0) - Number(b.timestamp || 0));
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
            .addCase(sendMessageViaSocket.pending, (state, action) => {
                const { conversationId, content, type } = action.meta.arg;
                const currentUserId = (state as any).auth?.user?.userId;

                // Construct a temporary clientMsgId for the pending state if not provided
                // This must match what we use in the thunk if not provided in arg
                const clientMsgId = action.meta.arg.clientMsgId || action.meta.requestId;

                const tempMsg: MessageDTO = {
                    msgId: -1,
                    conversationId,
                    content,
                    type: (type as any) || 'TEXT',
                    fromAccountId: Number(currentUserId),
                    timestamp: Date.now(),
                    clientMsgId: clientMsgId,
                    sendingStatus: 'sending'
                };

                if (!state.messages[conversationId]) {
                    state.messages[conversationId] = [];
                }

                // Only add if it doesn't exist already (e.g. from a previous attempt)
                const existingIndex = state.messages[conversationId].findIndex(m => m.clientMsgId === clientMsgId);
                if (existingIndex === -1) {
                    state.messages[conversationId].push(tempMsg);
                } else {
                    state.messages[conversationId][existingIndex].sendingStatus = 'sending';
                }
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
                    state.messages[conversationId][existingIndex] = {
                        ...action.payload,
                        sendingStatus: 'success'
                    };
                } else {
                    state.messages[conversationId].push({
                        ...action.payload,
                        sendingStatus: 'success'
                    });
                }
            })
            .addCase(sendMessageViaSocket.rejected, (state, action) => {
                state.error = action.error.message || '发送消息失败?'
                const { conversationId, clientMsgId } = action.meta.arg;

                if (conversationId && state.messages[conversationId]) {
                    const finalClientMsgId = clientMsgId || action.meta.requestId;
                    const existingIndex = state.messages[conversationId].findIndex(m => m.clientMsgId === finalClientMsgId);
                    if (existingIndex !== -1) {
                        state.messages[conversationId][existingIndex].sendingStatus = 'failed';
                    }
                }
            })
            .addCase(createPrivateChat.fulfilled, (state, action: PayloadAction<ConversationRes>) => {
                const newConv = normalizeConversation(action.payload);
                state.activeConversationId = newConv.conversationId;
                upsertConversation(state, newConv);
            })
            .addCase(createGroupConversation.fulfilled, (state, action: PayloadAction<ConversationRes>) => {
                const newConv = normalizeConversation(action.payload);
                state.activeConversationId = newConv.conversationId;
                upsertConversation(state, newConv);
            })
            .addCase(addConversationMembers.fulfilled, (state, action: PayloadAction<ConversationRes>) => {
                const updatedConversation = normalizeConversation(action.payload);
                upsertConversation(state, updatedConversation);
            });
    },
});

export const { setActiveConversation, addMessage } = chatSlice.actions;
export default chatSlice.reducer;






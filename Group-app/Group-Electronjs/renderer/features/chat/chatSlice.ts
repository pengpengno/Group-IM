import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { conversationAPI } from '../../services/api/apiClient';
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
            state.messages[conversationId].push(action.payload);
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
                state.messages[conversationId].push(action.payload);
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

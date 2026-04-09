import { ConversationRes, ConversationType, ApiUser } from '../types';

/**
 * Checks if a conversation is a group chat.
 */
export const isGroupConversation = (conversation?: ConversationRes): boolean => {
    if (!conversation) return false;
    return conversation.conversationType === ConversationType.GROUP || (conversation as any).type === ConversationType.GROUP;
};

/**
 * Gets the display name for a conversation.
 * For groups: uses groupName or name.
 * For private chats: uses the other member's username.
 */
export const getConversationDisplayName = (conversation: ConversationRes, currentUserId?: string): string => {
    if (!conversation) return '未知会话';

    if (isGroupConversation(conversation)) {
        return conversation.groupName || (conversation as any).name || '无名群组';
    } else {
        const members = Array.isArray(conversation.members) ? conversation.members : [];
        const otherUser = members.find((m: ApiUser) => m.userId.toString() !== currentUserId);
        return otherUser?.username || '未知用户';
    }
};

/**
 * Gets the avatar text for a conversation.
 */
export const getConversationAvatarText = (conversation: ConversationRes, currentUserId?: string): string => {
    const name = getConversationDisplayName(conversation, currentUserId);
    return name.charAt(0).toUpperCase();
};

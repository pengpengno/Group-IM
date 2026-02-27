import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../../store';
import { fetchConversations, setActiveConversation } from './chatSlice';
import { ConversationDisplayState, ApiUser } from '../../types';
import './ChatList.css';

interface ChatListProps {
  onVideoCallStart?: (userId: string) => void;
}

const ChatList: React.FC<ChatListProps> = ({ onVideoCallStart }) => {
  const dispatch = useDispatch<AppDispatch>();
  const { conversations, loading, activeConversationId } = useSelector((state: RootState) => state.chat);
  const { user } = useSelector((state: RootState) => state.auth);

  useEffect(() => {
    if (user?.userId) {
      dispatch(fetchConversations(user.userId));
    }
  }, [dispatch, user]);

  const handleSelectConversation = (id: number) => {
    dispatch(setActiveConversation(id));
  };

  if (loading && conversations.length === 0) {
    return <div className="chat-list-loading">正在拉取会话...</div>;
  }

  return (
    <div className="chat-list-container">
      <div className="chat-list-header">
        <h2>最近会话</h2>
      </div>

      <div className="conversations-scroll">
        {conversations.length === 0 ? (
          <div className="empty-chats">
            <p>暂无活跃会话</p>
          </div>
        ) : (
          conversations.map((item: ConversationDisplayState) => {
            const isGroup = item.conversation.type === 'GROUP';
            const displayName = isGroup
              ? item.conversation.groupName
              : item.conversation.members.find((m: ApiUser) => m.userId.toString() !== user?.userId)?.username || '未知用户';

            return (
              <div
                key={item.conversation.conversationId}
                className={`chat-item-premium ${activeConversationId === item.conversation.conversationId ? 'active' : ''}`}
                onClick={() => handleSelectConversation(item.conversation.conversationId)}
              >
                <div className="chat-item-avatar">
                  {displayName?.charAt(0).toUpperCase()}
                  {!isGroup && <span className="online-status"></span>}
                </div>

                <div className="chat-item-info">
                  <div className="chat-item-top">
                    <span className="chat-item-name">{displayName}</span>
                    <span className="chat-item-time">{item.displayDateTime}</span>
                  </div>
                  <div className="chat-item-bottom">
                    <span className="chat-item-msg">{item.lastMessage || '点击开始聊天'}</span>
                    {item.unreadCount > 0 && (
                      <span className="chat-item-badge">{item.unreadCount}</span>
                    )}
                  </div>
                </div>

                <div className="chat-item-actions">
                  <button
                    className="quick-call-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (!isGroup && onVideoCallStart) {
                        const target = item.conversation.members.find((m: ApiUser) => m.userId.toString() !== user?.userId);
                        if (target) onVideoCallStart(target.userId.toString());
                      }
                    }}
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                      <polygon points="23 7 16 12 23 17 23 7"></polygon>
                      <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                    </svg>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default ChatList;
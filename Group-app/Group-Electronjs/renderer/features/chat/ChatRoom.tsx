import React, { useState, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../../store';
import { fetchMessages, sendMessageViaSocket } from './chatSlice';
import type { ConversationRes, Message, MessageDTO } from '../../types';
import type { AuthState } from '../auth/authSlice';
import Notification, { NotificationType } from '../../components/common/Notification';
import './ChatRoom.css';

interface ChatRoomProps {
  conversation: ConversationRes;
  onVideoCall?: (userId: string) => void;
}

// 消息项组件
const MessageBubble: React.FC<{
  message: Message;
  isOwnMessage: boolean;
}> = ({ message, isOwnMessage }) => {
  const formatTime = (timestamp: any) => {
    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className={`message-row ${isOwnMessage ? 'own-message' : 'other-message'}`}>
      {!isOwnMessage && (
        <div
          className="msg-avatar"
          style={{ backgroundColor: getColorFromString(message.senderId) }}
        >
          {message.senderId.charAt(0).toUpperCase()}
        </div>
      )}
      <div className="msg-content-wrapper">
        <div className="msg-bubble">
          <div className="msg-text">{message.content}</div>
          <div className="msg-meta">
            <span className="msg-time">{formatTime(message.timestamp)}</span>
            {isOwnMessage && (
              <span className={`msg-status ${message.status}`}>
                {message.status === 'sent' && '✓'}
                {message.status === 'read' && '✓✓'}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

const ChatRoom: React.FC<ChatRoomProps> = ({ conversation, onVideoCall }) => {
  const dispatch = useDispatch<AppDispatch>();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);
  const { messages: allMessages, loading: chatLoading } = useSelector((state: RootState) => state.chat);

  const messages = allMessages[conversation.conversationId] || [];
  const [inputText, setInputText] = useState('');
  const [toast, setToast] = useState<{ message: string; type: NotificationType } | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 获取会话名称
  const getRoomName = () => {
    if (conversation.type === 'GROUP') {
      return conversation.groupName || '群组';
    } else {
      const otherUser = conversation.members.find(m => m.userId.toString() !== user?.userId);
      return otherUser?.username || '未知用户';
    }
  };

  // 获取对方用户ID（用于视频通话）
  const getOtherUserId = () => {
    if (conversation.type === 'PRIVATE_CHAT') {
      const otherUser = conversation.members.find(m => m.userId.toString() !== user?.userId);
      return otherUser?.userId.toString();
    }
    return null;
  };

  const showToast = (message: string, type: NotificationType = 'error') => {
    setToast({ message, type });
  };

  // 加载消息
  const loadMessages = async () => {
    try {
      await dispatch(fetchMessages(conversation.conversationId)).unwrap();
    } catch (err: any) {
      console.error('Failed to load messages:', err);
      showToast(err.message || '无法获取历史消息');
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputText.trim()) return;

    const content = inputText.trim();
    setInputText('');

    try {
      await dispatch(sendMessageViaSocket({
        conversationId: conversation.conversationId,
        content: content,
        type: 'TEXT'
      })).unwrap();

      scrollToBottom();
    } catch (err: any) {
      console.error('Failed to send message:', err);
      // 处理错误，但不破坏UI结构
      showToast(err.message || '消息发送失败，请检查网络');
      // 可以在此处将消息标为失败，或者放回输入框
      setInputText(content);
    }
  };

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 初始化加载
  useEffect(() => {
    loadMessages();
  }, [conversation]);

  // 消息更新时自动滚动
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  return (
    <div className="chat-room-premium">
      {/* Toast Notification */}
      {toast && (
        <Notification
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {/* 顶部导航栏 */}
      <div className="chatroom-header">
        <div className="chatroom-header-left">
          <div
            className="room-avatar"
            style={{ backgroundColor: getColorFromString(getRoomName() || '') }}
          >
            {(getRoomName() || '?').charAt(0)}
          </div>
          <div className="room-info">
            <h2 className="room-name">{getRoomName()}</h2>
            <div className="room-status">
              <span className="status-indicator online"></span>
              响应中
            </div>
          </div>
        </div>

        <div className="chatroom-header-actions">
          {getOtherUserId() && onVideoCall && (
            <button
              className="action-icon-btn"
              onClick={() => onVideoCall(getOtherUserId()!)}
              title="语音通话"
            >
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
            </button>
          )}
          <button className="action-icon-btn" title="更多">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="1"></circle>
              <circle cx="12" cy="5" r="1"></circle>
              <circle cx="12" cy="19" r="1"></circle>
            </svg>
          </button>
        </div>
      </div>

      {/* 消息列表 */}
      <div className="messages-viewport">
        {chatLoading && messages.length === 0 ? (
          <div className="viewport-loading">
            <div className="spinner-medium"></div>
          </div>
        ) : (
          <div className="messages-list-desktop">
            {messages.map((msg: MessageDTO) => {
              const uiMsg: Message = {
                id: msg.msgId.toString(),
                senderId: msg.fromAccountId.toString(),
                receiverId: '',
                content: msg.content,
                timestamp: new Date(msg.timestamp),
                type: msg.type.toLowerCase(),
                status: 'sent'
              };
              return (
                <MessageBubble
                  key={uiMsg.id}
                  message={uiMsg}
                  isOwnMessage={msg.fromAccountId.toString() === user?.userId}
                />
              );
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* 输入区域 */}
      <div className="message-input-bar">
        <div className="toolbar">
          <button className="tool-btn" title="表情">😊</button>
          <button className="tool-btn" title="上传图片">🖼️</button>
          <button className="tool-btn" title="发送文件">📁</button>
        </div>
        <div className="input-row">
          <textarea
            className="desktop-textarea"
            placeholder="输入消息，Enter 发送..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
              }
            }}
            rows={1}
          />
          <button
            className="send-btn-primary"
            onClick={sendMessage}
            disabled={!inputText.trim() || chatLoading}
          >
            发送
          </button>
        </div>
      </div>
    </div>
  );
};

// 根据字符串生成颜色的辅助函数
const getColorFromString = (str: string): string => {
  const colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
};

export default ChatRoom;
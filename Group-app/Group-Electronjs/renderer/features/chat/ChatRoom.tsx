import React, { useState, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../../store';
import { fetchMessages, sendMessage as sendMessageAction } from './chatSlice';
import type { ConversationRes, Message, MessageDTO } from '../../types';
import { getElectronAPI } from '../../services/api/electronAPI';
import './ChatRoom.css';
import type { AuthState } from '../auth/authSlice';

interface ChatRoomProps {
  conversation: ConversationRes;
  onBack: () => void;
  onVideoCall?: (userId: string) => void;
}

// 消息项组件
const MessageBubble: React.FC<{
  message: Message;
  isOwnMessage: boolean;
  currentUser: any;
}> = ({ message, isOwnMessage, currentUser }) => {
  const formatTime = (timestamp: any) => {
    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className={`message-bubble ${isOwnMessage ? 'own-message' : 'other-message'}`}>
      {!isOwnMessage && (
        <div className="sender-info">
          <div
            className="sender-avatar"
            style={{ backgroundColor: getColorFromString(message.senderId) }}
          >
            {message.senderId.charAt(0).toUpperCase()}
          </div>
          <span className="sender-name">{message.senderId}</span>
        </div>
      )}

      <div className="message-content">
        <div className="message-text">{message.content}</div>
        <div className="message-meta">
          <span className="message-time">{formatTime(message.timestamp)}</span>
          {isOwnMessage && (
            <span className={`message-status ${message.status}`}>
              {message.status === 'sent' && '✓'}
              {message.status === 'delivered' && '✓✓'}
              {message.status === 'read' && '✓✓'}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

const ChatRoom: React.FC<ChatRoomProps> = ({ conversation, onBack, onVideoCall }) => {
  const dispatch = useDispatch<AppDispatch>();
  const electronAPI = getElectronAPI();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);
  const { messages: allMessages, loading: chatLoading } = useSelector((state: RootState) => state.chat);

  const messages = allMessages[conversation.conversationId] || [];
  const [inputText, setInputText] = useState('');
  const [error, setError] = useState<string | null>(null);
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

  // 加载消息
  const loadMessages = async () => {
    try {
      setError(null);
      await dispatch(fetchMessages(conversation.conversationId)).unwrap();
    } catch (err: any) {
      console.error('Failed to load messages:', err);
      setError(err.message || '加载消息失败');
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputText.trim()) return;

    try {
      setError(null);
      const content = inputText.trim();
      setInputText('');

      await dispatch(sendMessageAction({
        conversationId: conversation.conversationId,
        content: content
      })).unwrap();

      scrollToBottom();
    } catch (err: any) {
      console.error('Failed to send message:', err);
      setError(err.message || '发送消息失败');
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
    <div className="chat-room">
      {/* 顶部导航栏 */}
      <div className="chat-header">
        <button className="back-button" onClick={onBack} aria-label="返回">
          ←
        </button>

        <div className="chat-title">
          <div
            className="chat-avatar"
            style={{ backgroundColor: getColorFromString(getRoomName() || '') }}
          >
            {(getRoomName() || '?').charAt(0)}
          </div>
          <div className="chat-info">
            <h2 className="chat-name">{getRoomName()}</h2>
            <p className="chat-status">在线</p>
          </div>
        </div>

        {getOtherUserId() && onVideoCall && (
          <button
            className="video-call-button"
            onClick={() => onVideoCall(getOtherUserId()!)}
            aria-label="视频通话"
          >
            📹
          </button>
        )}
      </div>

      {/* 消息列表 */}
      <div className="messages-container">
        {chatLoading ? (
          <div className="loading-messages">
            <div className="spinner"></div>
            <p>加载消息中...</p>
          </div>
        ) : error ? (
          <div className="error-messages">
            <div className="error-icon">❌</div>
            <p>{error}</p>
            <button className="retry-button" onClick={loadMessages}>
              重新加载
            </button>
          </div>
        ) : (
          <div className="messages-list">
            {messages.map((msg: MessageDTO) => {
              // Convert MessageDTO to Message format for MessageBubble
              const uiMsg: Message = {
                id: msg.msgId.toString(),
                senderId: msg.fromAccountId.toString(),
                receiverId: '', // Not used in bubble
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
                  currentUser={user}
                />
              );
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* 输入区域 */}
      <div className="input-area">
        <div className="input-container">
          <input
            type="text"
            className="message-input"
            placeholder="输入消息..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
            disabled={chatLoading}
          />
          <button
            className="send-button"
            onClick={sendMessage}
            disabled={!inputText.trim() || chatLoading}
            aria-label="发送消息"
          >
            发送
          </button>
        </div>

        {/* 多媒体按钮 */}
        <div className="media-buttons">
          <button className="media-button" aria-label="发送图片">📷</button>
          <button className="media-button" aria-label="发送文件">📎</button>
          <button className="media-button" aria-label="语音消息">🎤</button>
        </div>
      </div>
    </div>
  );
};

// 根据字符串生成颜色的辅助函数
const getColorFromString = (str: string): string => {
  const colors = ['#1976d2', '#4caf50', '#ff9800', '#9c27b0', '#f44336', '#00bcd4', '#8bc34a'];
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
};

export default ChatRoom;
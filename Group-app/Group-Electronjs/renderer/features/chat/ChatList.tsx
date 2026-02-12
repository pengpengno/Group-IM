import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import type {  } from '../../types';
import { getElectronAPI } from '../../api/electronAPI';
import './ChatList.css';
import type { AuthState } from '../../types';

interface ChatListProps {
  onVideoCallStart?: (userId: string) => void;
  onChatStart?: (userId: string) => void;
}

interface Conversation {
  id: string;
  name: string;
  lastMessage: string;
  timestamp: string;
  unreadCount: number;
  userId: string;
  isOnline?: boolean;
  avatarColor?: string;
}

const ChatList: React.FC<ChatListProps> = ({ onVideoCallStart, onChatStart }) => {
  const electronAPI = getElectronAPI();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);
  
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Mock conversation data for demonstration - enhanced with more realistic data
  const mockConversations: Conversation[] = [
    {
      id: '1',
      name: '张三',
      lastMessage: '你好，在吗？项目进展怎么样了？',
      timestamp: '14:30',
      unreadCount: 2,
      userId: 'user_1',
      isOnline: true,
      avatarColor: '#1976d2'
    },
    {
      id: '2',
      name: '李四',
      lastMessage: '好的，我这边已经完成了前端部分',
      timestamp: '13:45',
      unreadCount: 0,
      userId: 'user_2',
      isOnline: false,
      avatarColor: '#4caf50'
    },
    {
      id: '3',
      name: '王五',
      lastMessage: '会议时间改到下午3点，大家注意一下',
      timestamp: '12:20',
      unreadCount: 5,
      userId: 'user_3',
      isOnline: true,
      avatarColor: '#ff9800'
    },
    {
      id: '4',
      name: '赵六',
      lastMessage: '[文件] 项目需求文档已发送',
      timestamp: '11:15',
      unreadCount: 1,
      userId: 'user_4',
      isOnline: true,
      avatarColor: '#9c27b0'
    },
    {
      id: '5',
      name: '钱七',
      lastMessage: '明天的演示准备好了吗？',
      timestamp: '昨天',
      unreadCount: 0,
      userId: 'user_5',
      isOnline: false,
      avatarColor: '#f44336'
    }
  ];

  // Simulate loading conversations from API
  useEffect(() => {
    const loadConversations = async () => {
      try {
        setIsRefreshing(true);
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 800));
        setConversations(mockConversations);
      } catch (error) {
        console.error('Failed to load conversations:', error);
      } finally {
        setIsRefreshing(false);
      }
    };

    loadConversations();
  }, []);

  // Filter conversations based on search query
  const filteredConversations = conversations.filter(conv => 
    conv.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    conv.lastMessage.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Handle video call start
  const handleVideoCall = (userId: string) => {
    if (onVideoCallStart) {
      onVideoCallStart(userId);
    }
  };

  // Handle chat start
  const handleChat = (userId: string) => {
    if (onChatStart) {
      onChatStart(userId);
    }
  };

  // Handle pull to refresh
  const handleRefresh = async () => {
    if (!isRefreshing) {
      setIsRefreshing(true);
      try {
        // Simulate refresh
        await new Promise(resolve => setTimeout(resolve, 1000));
        setConversations([...mockConversations]); // Refresh data
      } finally {
        setIsRefreshing(false);
      }
    }
  };

  return (
    <div className="chat-list">
      {/* Header with offline status */}
      {isRefreshing && (
        <div className="refresh-indicator">
          <div className="refresh-spinner"></div>
          <span>正在刷新...</span>
        </div>
      )}

      {/* Search Box - Enhanced Material Design style */}
      <div className="search-container">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            className="search-input"
            placeholder="搜索联系人或消息..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button 
              className="clear-search"
              onClick={() => setSearchQuery('')}
              aria-label="Clear search"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Conversations List */}
      <div className="conversations-list">
        {filteredConversations.length > 0 ? (
          filteredConversations.map((conversation) => (
            <div 
              key={conversation.id} 
              className="chat-item"
              onClick={() => handleChat(conversation.userId)}
            >
              <div className="avatar-container">
                <div 
                  className="user-avatar"
                  style={{ backgroundColor: conversation.avatarColor }}
                >
                  {conversation.name.charAt(0)}
                </div>
                {conversation.isOnline && (
                  <div className="online-indicator"></div>
                )}
                {conversation.unreadCount > 0 && (
                  <div className="unread-badge">
                    {conversation.unreadCount > 9 ? '9+' : conversation.unreadCount}
                  </div>
                )}
              </div>
              
              <div className="chat-content">
                <div className="chat-header">
                  <h3 className="contact-name">{conversation.name}</h3>
                  <span className="timestamp">{conversation.timestamp}</span>
                </div>
                <p className="last-message">{conversation.lastMessage}</p>
              </div>

              {/* Action Buttons */}
              <div className="chat-actions">
                <button 
                  className="action-button video-call-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleVideoCall(conversation.userId);
                  }}
                  title="视频通话"
                >
                  📹
                </button>
                <button 
                  className="action-button chat-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleChat(conversation.userId);
                  }}
                  title="发送消息"
                >
                  💬
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="empty-state">
            <div className="empty-icon">💬</div>
            <h3>暂无聊天会话</h3>
            <p>您可以搜索联系人开始聊天</p>
          </div>
        )}
      </div>

      {/* Floating Action Buttons */}
      <div className="floating-actions">
        <button 
          className="floating-btn add-contact-btn"
          title="添加联系人"
        >
          +
        </button>
        <button 
          className="floating-btn video-call-btn"
          onClick={() => handleVideoCall('random_contact')}
          title="发起视频通话"
        >
          📹
        </button>
      </div>
    </div>
  );
};

export default ChatList;
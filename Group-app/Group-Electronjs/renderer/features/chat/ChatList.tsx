import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import type { User } from '../../types';
import { getElectronAPI } from '../../api/electronAPI';
import './ChatList.css';
import type { AuthState } from '../../types';

interface ChatListProps {
  onVideoCallStart?: (userId: string) => void;
  onChatStart?: (userId: string) => void;
}

const ChatList: React.FC<ChatListProps> = ({ onVideoCallStart, onChatStart }) => {
  const electronAPI = getElectronAPI();
  const { user } = useSelector((state: { auth:  AuthState }) => state.auth);
  
  const [conversations, setConversations] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  // Mock conversation data for demonstration
  const mockConversations = [
    {
      id: '1',
      name: '张三',
      lastMessage: '你好，在吗？',
      timestamp: '14:30',
      unreadCount: 2,
      userId: 'user_1'
    },
    {
      id: '2',
      name: '李四',
      lastMessage: '项目进展如何？',
      timestamp: '13:45',
      unreadCount: 0,
      userId: 'user_2'
    },
    {
      id: '3',
      name: '王五',
      lastMessage: '会议时间改了吗？',
      timestamp: '12:20',
      unreadCount: 5,
      userId: 'user_3'
    },
    {
      id: '4',
      name: '赵六',
      lastMessage: '文件已发送',
      timestamp: '11:15',
      unreadCount: 1,
      userId: 'user_4'
    }
  ];

  // Filter conversations based on search query
  const filteredConversations = mockConversations.filter(conv => 
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

  return (
    <div className="chat-list">
      {/* Search Box */}
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
        </div>
      </div>

      {/* Conversations List */}
      <div className="conversations-list">
        {filteredConversations.map((conversation) => (
          <div key={conversation.id} className="chat-item">
            <div className="avatar-container">
              <div className="user-avatar">
                {conversation.name.charAt(0)}
              </div>
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
                onClick={() => handleVideoCall(conversation.userId)}
                title="视频通话"
              >
                📹
              </button>
              <button 
                className="action-button chat-btn"
                onClick={() => handleChat(conversation.userId)}
                title="发送消息"
              >
                💬
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Floating Video Call Button */}
      <button 
        className="floating-video-btn"
        onClick={() => handleVideoCall('random_contact')}
        title="发起视频通话"
      >
        📹
      </button>
    </div>
  );
};

export default ChatList;
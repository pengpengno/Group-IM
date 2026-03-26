import React, { useState, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../../store';
import { fetchMessages, sendMessage, sendMessageViaSocket } from './chatSlice';
import { BASE_URL } from '../../services/api/apiClient';
import { useAppSelector } from '../../hooks';
import axios from 'axios';
import type { ConversationRes, MessageDTO } from '../../types';
import type { AuthState } from '../auth/authSlice';
import Notification, { NotificationType } from '../../components/common/Notification';
import './ChatRoom.css';

// 定义 Electron 接口扩展 (防止 TS 报错)
declare global {
  interface Window {
    electronAPI: any;
  }
}

interface ChatRoomProps {
  conversation: ConversationRes;
  onVideoCall?: (userId: string) => void;
}

// 消息项组件
/**
 * 带鉴权的图片组件
 */
/**
 * Authenticated Media Hook to handle blob URLs with token
 */
const useAuthenticatedMedia = (url: string, token?: string) => {
    const [mediaSrc, setMediaSrc] = useState<string>('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
        let objectUrl = '';
        const fetchMedia = async () => {
            if (!url) return;
            setLoading(true);
            try {
                const response = await axios.get(url, {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                    responseType: 'blob'
                });
                objectUrl = URL.createObjectURL(response.data);
                setMediaSrc(objectUrl);
                setError(false);
            } catch (err) {
                console.error('Failed to load authenticated media:', err);
                setError(true);
            } finally {
                setLoading(false);
            }
        };

        fetchMedia();

        return () => {
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [url, token]);

    return { mediaSrc, loading, error };
};

/**
 * Beautiful Custom Audio Player
 */
const CustomAudioPlayer: React.FC<{ url: string; token?: string }> = ({ url, token }) => {
    const { mediaSrc, loading } = useAuthenticatedMedia(url, token);
    const audioRef = useRef<HTMLAudioElement>(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [progress, setProgress] = useState(0);

    const togglePlay = () => {
        if (!audioRef.current) return;
        if (isPlaying) {
            audioRef.current.pause();
        } else {
            audioRef.current.play();
        }
        setIsPlaying(!isPlaying);
    };

    const onTimeUpdate = () => {
        if (!audioRef.current) return;
        const p = (audioRef.current.currentTime / audioRef.current.duration) * 100;
        setProgress(p);
    };

    if (loading) return <div className="audio-skeleton">Loading audio...</div>;

    return (
        <div className="custom-audio-player">
            <audio 
                ref={audioRef} 
                src={mediaSrc} 
                onTimeUpdate={onTimeUpdate} 
                onEnded={() => setIsPlaying(false)}
            />
            <button className="audio-play-btn" onClick={togglePlay}>
                {isPlaying ? (
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                ) : (
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                )}
            </button>
            <div className="audio-progress-bar">
                <div className="audio-progress-fill" style={{ width: `${progress}%` }}></div>
            </div>
            <span className="audio-time-label">Voice</span>
        </div>
    );
};

const AuthenticatedImage: React.FC<{
  url: string;
  token?: string;
  className?: string;
  onClick?: () => void;
}> = ({ url, token, className, onClick }) => {
  const { mediaSrc, loading, error } = useAuthenticatedMedia(url, token);

  if (loading) return (
    <div className={`${className} media-placeholder`}>
        <div className="spinner-small"></div>
    </div>
  );
  
  if (error) return (
      <div className={`${className} media-placeholder error`}>
          <span>Failed to load</span>
      </div>
  );

  return <img src={mediaSrc} className={className} onClick={onClick} alt="Chat media" />;
};

const MessageBubble: React.FC<{
  message: MessageDTO;
  isOwnMessage: boolean;
  onImageClick?: (url: string, type: string) => void;
}> = ({ message, isOwnMessage, onImageClick }) => {
  const token = useAppSelector((state: RootState) => state.auth.user?.token);

  const formatTime = (timestamp: any) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleDownload = async (fileId: string, fileName: string) => {
    if (window.electronAPI && window.electronAPI.downloadFile) {
        const url = `${BASE_URL}/api/files/download/${fileId}`;
        try {
            const result = await window.electronAPI.downloadFile(url, fileName, token);
            if (result.success) {
                console.log('File downloaded to:', result.filePath);
            } else if (!result.canceled) {
                alert('Download failed: ' + result.error);
            }
        } catch (err) {
            console.error('Download error:', err);
        }
    } else {
        window.open(`${BASE_URL}/api/files/download/${fileId}`);
    }
  };

  const renderContent = () => {
    const getFileUrl = (fileId: string) => `${BASE_URL}/api/files/download/${fileId}`;
    const type = message.type.toUpperCase();
    
    switch (type) {
      case 'IMAGE': {
        const url = getFileUrl(message.content);
        return (
          <div className="msg-media-container msg-image-container">
            <AuthenticatedImage
              url={url}
              token={token}
              className="msg-img-preview"
              onClick={() => onImageClick && onImageClick(url, 'IMAGE')}
            />
          </div>
        );
      }
      case 'VIDEO': {
        const url = getFileUrl(message.content);
        return (
          <div className="msg-media-container msg-video-container" onClick={() => onImageClick && onImageClick(url, 'VIDEO')}>
            <div className="video-overlay-play">
                <svg viewBox="0 0 24 24" width="40" height="40" fill="white"><path d="M8 5v14l11-7z"/></svg>
            </div>
            <div className="video-placeholder-thumb">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="white" opacity="0.5">
                    <path d="M21 7L17 11V7C17 6.45 16.55 6 16 6H5C4.45 6 4 6.45 4 7V17C4 17.55 4.45 18 5 18H16C16.55 18 17 17.55 17 17V13L21 17V7Z"/>
                </svg>
            </div>
          </div>
        );
      }
      case 'FILE': {
        const fileName = message.payload?.filename || message.payload?.fileName || 'Document';
        return (
          <div className="msg-file-card" onClick={() => handleDownload(message.content, fileName)}>
            <div className="file-icon-box">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                    <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
                </svg>
            </div>
            <div className="file-detail">
              <span className="file-name" title={fileName}>{fileName}</span>
              <span className="file-action">Download</span>
            </div>
          </div>
        );
      }
      case 'VOICE': {
          const url = getFileUrl(message.content);
          return (
              <CustomAudioPlayer url={url} token={token} />
          );
      }
      default:
        return <div className="msg-text">{message.content}</div>;
    }
  };

  return (
    <div className={`message-row ${isOwnMessage ? 'own-message' : 'other-message'}`}>
      {!isOwnMessage && (
        <div
          className="msg-avatar"
          style={{ backgroundColor: getColorFromString(message.fromAccount?.username || message.fromAccountId.toString()) }}
        >
          {(message.fromAccount?.username || '?').charAt(0).toUpperCase()}
        </div>
      )}
      <div className="msg-content-wrapper">
        <div className="msg-bubble">
          {renderContent()}
          <div className="msg-meta">
            <span className="msg-time">{formatTime(message.timestamp)}</span>
            {isOwnMessage && <span className="msg-status">✓</span>}
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
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showScreenPicker, setShowScreenPicker] = useState(false);
  const [screenSources, setScreenSources] = useState<any[]>([]);
  const [selectedSourceId, setSelectedSourceId] = useState<string | null>(null);
  const [previewMedia, setPreviewMedia] = useState<{ url: string; type: string } | null>(null);
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

  // 处理文件选取和上传
  const handleFileSelect = async (isImage: boolean = false) => {
    try {
      const options = isImage ? {
        filters: [{ name: 'Images', extensions: ['jpg', 'jpeg', 'png', 'gif', 'webp'] }]
      } : {};

      const result = await (window as any).electronAPI.selectFile(options);

      if (result.canceled) return;

      showToast('正在上传文件...', 'info' as any);

      // 上传文件
      const uploadRes = await (window as any).electronAPI.uploadFile(result.filePath);

      if (uploadRes.success) {
        // 发送文件消息
        // 假设服务器返回的url在 uploadRes.data.url
        const fileUrl = uploadRes.data.url || uploadRes.data.path || result.fileName;

        await dispatch(sendMessageViaSocket({
          conversationId: conversation.conversationId,
          content: fileUrl,
          type: isImage ? 'IMAGE' : 'FILE'
        })).unwrap();

        showToast('发送成功', 'success' as any);
        scrollToBottom();
      } else {
        showToast(uploadRes.error || '文件上传失败');
      }
    } catch (err: any) {
      console.error('File selection/upload error:', err);
      showToast(err.message || '操作失败');
    }
  };

  // 处理表情选择
  const handleEmojiSelect = (emoji: string) => {
    setInputText(prev => prev + emoji);
    setShowEmojiPicker(false);
  };

  // 开启桌面分享选择器
  const startScreenShare = async () => {
    try {
      const sources = await (window as any).electronAPI.getDesktopSources();
      setScreenSources(sources);
      setShowScreenPicker(true);
    } catch (err: any) {
      console.error('Failed to get screen sources:', err);
      showToast('获取屏幕资源失败');
    }
  };

  // 完成屏幕录制选择并发送
  const handleScreenShareConfirm = async () => {
    if (!selectedSourceId) return;

    const source = screenSources.find(s => s.id === selectedSourceId);
    if (source) {
      showToast('准备开始桌面分享: ' + source.name, 'success' as any);
      await dispatch(sendMessageViaSocket({
        conversationId: conversation.conversationId,
        content: `Desktop Sharing: ${source.name}`,
        type: 'TEXT'
      })).unwrap();
    }

    setShowScreenPicker(false);
    setSelectedSourceId(null);
  };

  const EMOJIS = ['😊', '😂', '🤣', '❤️', '👍', '🔥', '✨', '🙌', '🙏', '🎉', '💡', '✅', '❌', '👀', '👋', '💬'];

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handlePreviewClose = () => setPreviewMedia(null);

  const handleFileDownload = async (fileId: string, fileName: string) => {
    if (window.electronAPI && window.electronAPI.downloadFile) {
        const url = `${BASE_URL}/api/files/download/${fileId}`;
        try {
            const result = await window.electronAPI.downloadFile(url, fileName, user?.token);
            if (result.success) {
                console.log('File downloaded to:', result.filePath);
            } else if (!result.canceled) {
                alert('Download failed: ' + result.error);
            }
        } catch (err) {
            console.error('Download error:', err);
        }
    } else {
        window.open(`${BASE_URL}/api/files/download/${fileId}`);
    }
  };

  // 初始化加载
  useEffect(() => {
    loadMessages();
  }, [conversation]);

  // 消息更新时自动滚动且标记已读
  useEffect(() => {
    scrollToBottom();
    
    // 如果有对方的消息，标记为已读
    if (messages.length > 0 && !chatLoading) {
      const lastOtherMsg = [...messages].reverse().find(m => m.fromAccountId.toString() !== user?.userId);
      if (lastOtherMsg && lastOtherMsg.msgId > 0) {
        import('../../services/socketService').then(({ socketService }) => {
          socketService.markAsRead(conversation.conversationId, lastOtherMsg.msgId);
        });
      }
    }
  }, [messages, chatLoading]);

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
      <div className="messages-viewport" onClick={() => setShowEmojiPicker(false)}>
        {chatLoading && messages.length === 0 ? (
          <div className="viewport-loading">
            <div className="spinner-medium"></div>
          </div>
        ) : (
          <div className="messages-list-desktop">
            {messages.map((msg: MessageDTO) => {
              return (
                <MessageBubble
                  key={msg.clientMsgId || msg.msgId.toString()}
                  message={msg}
                  isOwnMessage={msg.fromAccountId.toString() === user?.userId}
                  onImageClick={(url, type) => setPreviewMedia({ url, type })}
                />
              );
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* 表情包选择器 */}
      {showEmojiPicker && (
        <div className="emoji-picker-popover">
          {EMOJIS.map(emoji => (
            <div
              key={emoji}
              className="emoji-item"
              onClick={() => handleEmojiSelect(emoji)}
            >
              {emoji}
            </div>
          ))}
        </div>
      )}

      {/* 桌面分享选择器弹窗 */}
      {showScreenPicker && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>选择分享的内容</h3>
              <button className="close-btn" onClick={() => setShowScreenPicker(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="source-grid">
                {screenSources.map(source => (
                  <div
                    key={source.id}
                    className={`source-item ${selectedSourceId === source.id ? 'selected' : ''}`}
                    onClick={() => setSelectedSourceId(source.id)}
                  >
                    <img src={source.thumbnail} alt={source.name} className="source-thumbnail" />
                    <div className="source-name">{source.name}</div>
                  </div>
                ))}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowScreenPicker(false)}>取消</button>
              <button
                className="btn-primary"
                onClick={handleScreenShareConfirm}
                disabled={!selectedSourceId}
              >
                开始分享
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 媒体预览 Modal - Enhanced with Glassmorphism */}
      {previewMedia && (
          <div className="media-preview-overlay" onClick={handlePreviewClose}>
              <div className="media-preview-container" onClick={e => e.stopPropagation()}>
                  <div className="media-preview-header">
                      <div className="media-info">
                        <span className="media-type-badge">{previewMedia.type}</span>
                      </div>
                      <button className="media-close-btn" onClick={handlePreviewClose}>
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                      </button>
                  </div>
                  <div className="media-preview-body">
                      {previewMedia.type === 'IMAGE' ? (
                          <AuthenticatedImage 
                            url={previewMedia.url} 
                            token={user?.token} 
                            className="active-media-preview" 
                          />
                      ) : (
                          <video 
                            controls 
                            autoPlay 
                            className="active-media-preview"
                          >
                            <source src={previewMedia.url} />
                            Your browser does not support video playback.
                          </video>
                      )}
                  </div>
              </div>
          </div>
      )}

      {/* 输入区域 */}
      <div className="message-input-bar">
        <div className="toolbar">
          <button
            className="tool-btn"
            title="表情"
            onClick={() => setShowEmojiPicker(!showEmojiPicker)}
          >
            😊
          </button>
          <button
            className="tool-btn"
            title="上传图片"
            onClick={() => handleFileSelect(true)}
          >
            🖼️
          </button>
          <button
            className="tool-btn"
            title="发送文件"
            onClick={() => handleFileSelect(false)}
          >
            📁
          </button>
          <button
            className="tool-btn"
            title="屏幕分享"
            onClick={startScreenShare}
          >
            💻
          </button>
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
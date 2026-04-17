import React, { useState, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import ParticipantPicker from './ParticipantPicker';
import ScheduleMeetingDialog from './ScheduleMeetingDialog';
import { RootState, AppDispatch } from '../../store';
import { fetchMessages, sendMessageViaSocket } from './chatSlice';
import { BASE_URL, meetingAPI } from '../../services/api/apiClient';
import { useAppSelector } from '../../hooks';
import { getElectronAPI, isElectronEnvironment } from '../../services/api/electronAPI';
import axios from 'axios';
import type { ConversationRes, MessageDTO, MeetingMessagePayload } from '../../types';
import type { AuthState } from '../auth/authSlice';
import Notification, { NotificationType } from '../../components/common/Notification';
import { isGroupConversation, getConversationDisplayName, getConversationAvatarText } from '../../utils/conversationUtils';
import './ChatRoom.css';
import { MessageType } from '../../types';

// 定义 Electron 接口扩展 (防止 TS 报错)
declare global {
  interface Window {
    electronAPI: any;
  }
}

interface ChatRoomProps {
  conversation: ConversationRes;
  onVideoCall?: (userId: string) => void;
  onStartMeeting?: (participants: Array<{ userId: string; userName?: string }>, roomId?: string) => void;
  onJoinMeeting?: (roomId: string) => void;
}

// 消息项组件
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
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" /></svg>
        ) : (
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
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

const parseMeetingPayload = (message: MessageDTO): MeetingMessagePayload | null => {
  const payload = message.payload as MeetingMessagePayload | undefined;
  if (payload && (payload.roomId || payload.meetingId)) {
    return payload;
  }

  if (typeof message.content === 'string' && message.content.trim().startsWith('{')) {
    try {
      return JSON.parse(message.content) as MeetingMessagePayload;
    } catch {
      return null;
    }
  }

  return null;
};

const MessageBubble: React.FC<{
  message: MessageDTO;
  isOwnMessage: boolean;
  onImageClick?: (url: string, type: string) => void;
  onResend?: (message: MessageDTO) => void;
  onJoinMeeting?: (roomId: string) => void;
}> = ({ message, isOwnMessage, onImageClick, onResend, onJoinMeeting }) => {
  const token = useAppSelector((state: RootState) => state.auth.user?.token);

  const formatTime = (timestamp: any) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleDownload = async (fileId: string, fileName: string) => {
    const api = getElectronAPI();
    if (isElectronEnvironment() && (api as any).downloadFile) {
      const url = `${BASE_URL}/api/files/download/${fileId}`;
      try {
        const result = await (api as any).downloadFile(url, fileName, token);
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
      case MessageType.IMAGE: {
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
      case MessageType.VIDEO: {
        const url = getFileUrl(message.content);
        return (
          <div className="msg-media-container msg-video-container" onClick={() => onImageClick && onImageClick(url, 'VIDEO')}>
            <div className="video-overlay-play">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="white"><path d="M8 5v14l11-7z" /></svg>
            </div>
            <div className="video-placeholder-thumb">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="white" opacity="0.5">
                <path d="M21 7L17 11V7C17 6.45 16.55 6 16 6H5C4.45 6 4 6.45 4 7V17C4 17.55 4.45 18 5 18H16C16.55 18 17 17.55 17 17V13L21 17V7Z" />
              </svg>
            </div>
          </div>
        );
      }
      case MessageType.FILE: {
        const fileName = message.payload?.filename || message.payload?.fileName || 'Document';
        return (
          <div className="msg-file-card" onClick={() => handleDownload(message.content, fileName)}>
            <div className="file-icon-box">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
              </svg>
            </div>
            <div className="file-detail">
              <span className="file-name" title={fileName}>{fileName}</span>
              <span className="file-action">Download</span>
            </div>
          </div>
        );
      }
      case MessageType.VOICE: {
        const url = getFileUrl(message.content);
        return (
          <CustomAudioPlayer url={url} token={token} />
        );
      }
      case MessageType.MEETING: {
        const payload = parseMeetingPayload(message);
        const title = payload?.title || '会议';
        const count = payload?.participantCount ?? payload?.participantIds?.length ?? 0;
        const roomId = payload?.roomId;
        const isScheduled = payload?.action === 'SCHEDULE';
        const scheduledTime = payload?.scheduledAt ? new Date(payload.scheduledAt).toLocaleString() : '';

        return (
          <div className={`msg-meeting-card ${isScheduled ? 'scheduled' : ''}`}>
            <div className="meeting-title">{isScheduled ? `📅 预定会议: ${title}` : title}</div>
            {isScheduled && <div className="meeting-time">时间: {scheduledTime}</div>}
            <div className="meeting-meta">参会人数: {count}</div>
            {!isScheduled && (
              <button
                className="meeting-join-btn"
                onClick={() => roomId && onJoinMeeting && onJoinMeeting(roomId)}
                disabled={!roomId || !onJoinMeeting}
              >
                加入会议
              </button>
            )}
          </div>
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
            {isOwnMessage && (
              <span className={`msg-status-indicator ${message.sendingStatus || 'success'}`}>
                {message.sendingStatus === 'sending' && <span className="spinner-loading-tiny"></span>}
                {message.sendingStatus === 'failed' && (
                  <button className="resend-btn" onClick={() => onResend && onResend(message)} title="点击重发">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
                    </svg>
                  </button>
                )}
                {(message.sendingStatus === 'success' || !message.sendingStatus) && <span className="sent-check">✓</span>}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

const ChatRoom: React.FC<ChatRoomProps> = ({ conversation, onVideoCall, onStartMeeting, onJoinMeeting }) => {
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
  const [isOnline, setIsOnline] = useState(false);
  const [showParticipantPicker, setShowParticipantPicker] = useState(false);
  const [showScheduleMeeting, setShowScheduleMeeting] = useState(false);

  // Recording State
  const [isRecording, setIsRecording] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const recordingIntervalRef = useRef<any>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const viewportRef = useRef<HTMLDivElement>(null);
  const currentUserId = user?.userId ? Number(user.userId) : null;
  const currentSenderSnapshot = user ? {
    userId: Number(user.userId),
    username: user.username,
    email: user.email || '',
    phoneNumber: user.phoneNumber || ''
  } : undefined;

  // 获取会话名称
  const getRoomName = () => {
    return getConversationDisplayName(conversation, user?.userId);
  };

  // 获取对方用户ID（用于视频通话）
  const getOtherUserId = () => {
    if (!isGroupConversation(conversation)) {
      const otherUser = (Array.isArray(conversation.members) ? conversation.members : []).find(m => m.userId.toString() !== user?.userId);
      return otherUser?.userId.toString();
    }
    return null;
  };

  const getGroupParticipants = () => {
    if (!isGroupConversation(conversation)) {
      return [];
    }

    return (Array.isArray(conversation.members) ? conversation.members : [])
      .filter((member) => member.userId.toString() !== user?.userId)
      .map((member) => ({
        userId: member.userId.toString(),
        userName: member.username
      }));
  };

  const showToast = (message: string, type: NotificationType = 'error') => {
    setToast({ message, type });
  };

  const isOwnMessage = (message: MessageDTO) => {
    if (currentUserId == null) return false;
    if (Number(message.fromAccountId) === currentUserId) return true;
    if (Number(message.fromAccount?.userId) === currentUserId) return true;
    return false;
  };

  const startMeetingFromChat = async (selectedParticipants?: string[]) => {
    if (!onStartMeeting) return;
    
    // If we haven't selected participants yet, show the picker
    if (!selectedParticipants) {
      setShowParticipantPicker(true);
      return;
    }

    const participants = getGroupParticipants().filter(p => selectedParticipants.includes(p.userId));
    if (!participants.length) return;

    try {
      const response = await meetingAPI.create({
        conversationId: conversation.conversationId,
        title: conversation.groupName,
        participantIds: participants.map((p) => Number(p.userId))
      });
      const meeting = response.data?.data || response.data;
      onStartMeeting(participants, meeting?.roomId);
      setShowParticipantPicker(false);
    } catch (err: any) {
      console.error('Failed to create meeting:', err);
      showToast(err?.message || '创建会议失败');
    }
  };

  const handleScheduleConfirm = async (data: { title: string; scheduledAt: string; participantIds: string[] }) => {
    try {
      await meetingAPI.create({
        conversationId: conversation.conversationId,
        title: data.title,
        scheduledAt: data.scheduledAt,
        participantIds: data.participantIds.map(id => Number(id))
      });
      showToast('会议预定成功', 'success' as any);
      setShowScheduleMeeting(false);
    } catch (err: any) {
      console.error('Failed to schedule meeting:', err);
      showToast(err?.message || '预定会议失败');
    }
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

    // Define a clientMsgId to track the message through optimistic update
    const clientMsgId = window.crypto && window.crypto.randomUUID
      ? window.crypto.randomUUID()
      : Math.random().toString(36).substring(2) + Date.now().toString(36);

    try {
      // Async dispatch, don't await unwrap if we want immediate UI
      dispatch(sendMessageViaSocket({
        conversationId: conversation.conversationId,
        content: content,
        type: 'TEXT',
        clientMsgId,
        senderSnapshot: currentSenderSnapshot
      }));

      scrollToBottom();
    } catch (err: any) {
      console.error('Failed to send message async:', err);
    }
  };

  const handleResendMessage = (message: MessageDTO) => {
    dispatch(sendMessageViaSocket({
      conversationId: message.conversationId,
      content: message.content,
      type: message.type || 'TEXT',
      clientMsgId: message.clientMsgId,
      msgDto: message,
      senderSnapshot: currentSenderSnapshot
    }));
  };

  // 处理文件选取和上传
  const handleFileSelect = async (isImage: boolean = false) => {
    try {
      const api = getElectronAPI();
      const options = isImage ? {
        filters: [{ name: 'Images', extensions: ['jpg', 'jpeg', 'png', 'gif', 'webp'] }]
      } : {};

      const result = await api.selectFile(options);

      if (result.canceled) return;

      showToast('准备上传...', 'info' as any);

      // 1. 获取文件元数据并申请 UploadId
      let fileName = '';
      let fileSize = 0;

      if (result.file) {
        fileName = result.file.name;
        fileSize = result.file.size;
      } else if (result.filePaths && result.filePaths.length > 0) {
        // Electron 环境
        fileName = (result as any).fileName || result.filePaths[0].split(/[\\/]/).pop() || 'file';
        fileSize = (result as any).fileSize || 0;
      }

      const idRes = await api.getUploadId({
        fileName: fileName,
        size: fileSize
      });
      console.log(idRes)
      if (!idRes || !idRes.id) {
        showToast('无法初始化上传');
        return;
      }

      const fileId = idRes.id;

      // OPTIMISTIC: Send message immediately after getting uploadId
      // The content is the fileId (UUID), the message type is IMAGE/FILE
      // The recipient will see a loading state if they try to fetch a file that is still UPLOADING
      const clientMsgId = window.crypto && window.crypto.randomUUID
        ? window.crypto.randomUUID()
        : Math.random().toString(36).substring(2) + Date.now().toString(36);

      dispatch(sendMessageViaSocket({
        conversationId: conversation.conversationId,
        content: fileId,
        type: isImage ? MessageType.IMAGE : MessageType.FILE,
        clientMsgId,
        senderSnapshot: currentSenderSnapshot
      }));

      scrollToBottom();
      showToast('正在后台上传文件...', 'info' as any);

      // 2. Background Upload
      const fileToUpload = result.file || result.filePaths[0];
      api.uploadFile(fileToUpload, fileId).then((uploadRes) => {
        if (uploadRes && (uploadRes.id || uploadRes.fileMeta)) {
          showToast('文件上传完成', 'success' as any);
          // Optional: we could dispatch an update if the server gives us a permanent URL
          // but usually the fileId is enough as the CDN/proxy handles it
        } else {
          showToast('文件上传失败，消息已发送但无法查看');
        }
      }).catch(err => {
        console.error('Background upload error:', err);
        showToast('后台上传出错');
      });

    } catch (err: any) {
      console.error('File selection/upload error:', err);
      showToast(err.message || '操作失败');
    }
  };

  // Voice Recording Logic
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;
      audioChunksRef.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunksRef.current.push(e.data);
      };

      recorder.onstop = async () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        const api = getElectronAPI();
        const duration = recordingTime * 1000; // ms

        showToast('正在处理语音...', 'info' as any);

        try {
          // 1. 获取 UploadId
          const idRes = await api.getUploadId({
            fileName: `voice_${Date.now()}.webm`,
            size: audioBlob.size,
            duration: duration
          });

          if (!idRes || !idRes.id) {
            showToast('语音初始化失败');
            return;
          }

          const fileId = idRes.id;

          // OPTIMISTIC: Send voice message immediately
          const clientMsgId = window.crypto && window.crypto.randomUUID
            ? window.crypto.randomUUID()
            : Math.random().toString(36).substring(2) + Date.now().toString(36);

          dispatch(sendMessageViaSocket({
            conversationId: conversation.conversationId,
            content: fileId,
            type: 'VOICE',
            clientMsgId,
            senderSnapshot: currentSenderSnapshot
          }));

          // 2. Background Upload
          api.uploadFile(audioBlob as any, fileId, duration).then(res => {
            if (res && (res.id || res.fileMeta)) {
              console.log('Voice uploaded successfully');
            } else {
              showToast('语音上传失败');
            }
          }).catch(err => {
            console.error('Background voice upload error:', err);
          });
        } catch (err) {
          console.error('Audio upload error:', err);
          showToast('无法发送语音消息');
        }

        // Stop all tracks
        stream.getTracks().forEach(t => t.stop());
      };

      recorder.start();
      setIsRecording(true);
      setRecordingTime(0);

      recordingIntervalRef.current = setInterval(() => {
        setRecordingTime(prev => prev + 1);
      }, 1000);

    } catch (err) {
      console.error('Failed to start recording:', err);
      showToast('无法访问麦克风，请检查权限');
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      clearInterval(recordingIntervalRef.current);
    }
  };

  const formatRecordingTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  // 处理表情选择
  const handleEmojiSelect = (emoji: string) => {
    setInputText(prev => prev + emoji);
    setShowEmojiPicker(false);
  };

  // 开启桌面分享选择器
  const startScreenShare = async () => {
    try {
      const api = getElectronAPI();

      if (isElectronEnvironment()) {
        const sources = await (api as any).getDesktopSources();
        setScreenSources(sources);
        setShowScreenPicker(true);
      } else {
        // Web fallback using getDisplayMedia
        if (navigator.mediaDevices && (navigator.mediaDevices as any).getDisplayMedia) {
          const stream = await (navigator.mediaDevices as any).getDisplayMedia({ video: true });
          showToast('桌面分享已开启 (仅预览)', 'success' as any);
          // Stop immediately as it is just a demo for now without real RTC signaling
          stream.getTracks().forEach((track: any) => track.stop());
        } else {
          showToast('当前浏览器不支持桌面分享');
        }
      }
    } catch (err: any) {
      console.error('Failed to get screen sources:', err);
      showToast('操作被取消或失败');
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
        type: 'TEXT',
        senderSnapshot: currentSenderSnapshot
      })).unwrap();
    }

    setShowScreenPicker(false);
    setSelectedSourceId(null);
  };

  const EMOJIS = [
    '😊', '😂', '🤣', '❤️', '👍', '🔥', '✨', '🙌', '🙏', '🎉', '💡', '✅', '❌', '👀', '👋', '💬',
    '😍', '🥰', '😘', '😋', '😜', '😎', '🤔', '🧐', '🙄', '😤', '😭', '🤯', '😱', '🥳', '😴', '😷',
    '🌟', '🌙', '☀️', '☁️', '❄️', '☔', '⚡', '🌈', '🎈', '🎁', '🎂', '🎨', '🎬', '🎧', '🎮', '🚗',
    '🍎', '🍕', '🍔', '🍦', '☕', '🍺', '🌍', '🐱', '🐶', '🦊', '🐨', '🦁', '🦄', '🐝', '🍀', '🌸'
  ];

  // 滚动到底部
  const scrollToBottom = () => {
    if (viewportRef.current) {
      viewportRef.current.scrollTop = viewportRef.current.scrollHeight;
    }
  };

  const handlePreviewClose = () => setPreviewMedia(null);

  // 查询在线状态
  useEffect(() => {
    let interval: any;
    const checkOnline = async () => {
      const otherUserId = getOtherUserId();
      if (otherUserId && !isGroupConversation(conversation)) {
        try {
          const res = await import('../../services/api/apiClient').then(m => m.authAPI.isUserOnline(otherUserId));
          // Accessing res.data.data because UserController returns ApiResponse<Boolean>
          setIsOnline(res.data?.data === true);
        } catch (err) {
          console.warn('Failed to check online status');
        }
      } else {
        setIsOnline(false);
      }
    };

    checkOnline();
    interval = setInterval(checkOnline, 10000); // 10s polling

    return () => clearInterval(interval);
  }, [conversation]);

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

  // 工具栏点击辅助：点击其他工具时关闭表情
  const handleToolAction = (action: () => void) => {
    setShowEmojiPicker(false);
    action();
  };

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
            {getConversationAvatarText(conversation, user?.userId)}
          </div>
          <div className="room-info">
            <h2 className="room-name">{getRoomName()}</h2>
            <div className="room-status">
              {isGroupConversation(conversation) ? (
                <span>{conversation.members?.length || 0} 位成员</span>
              ) : (
                <>
                  <span className={`status-indicator ${isOnline ? 'online' : 'offline'}`}></span>
                  {isOnline ? '在线' : '离线'}
                </>
              )}
            </div>
          </div>
        </div>

        <div className="chatroom-header-actions">
          {isGroupConversation(conversation) && onStartMeeting && getGroupParticipants().length > 0 && (
            <>
              <button
                className="action-icon-btn"
                onClick={() => setShowScheduleMeeting(true)}
                title="Schedule Meeting"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
              </button>
              <button
                className="action-icon-btn"
                onClick={() => startMeetingFromChat()}
                title="Start Meeting"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
              </button>
            </>
          )}
          {getOtherUserId() && onVideoCall && (
            <>
              <button
                className="action-icon-btn"
                onClick={() => onVideoCall(getOtherUserId()!)}
                title="语音通话"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                </svg>
              </button>
              <button
                className="action-icon-btn"
                onClick={() => onVideoCall(getOtherUserId()!)}
                title="视频通话"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <polygon points="23 7 16 12 23 17 23 7"></polygon>
                  <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                </svg>
              </button>
            </>
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
      <div className="messages-viewport" ref={viewportRef} onClick={() => setShowEmojiPicker(false)}>
        {chatLoading && messages.length === 0 ? (
          <div className="viewport-loading">
            <div className="spinner-medium"></div>
          </div>
        ) : (
          <div className="messages-list-desktop">
            {messages.map((msg: MessageDTO) => (
              <MessageBubble
                key={msg.clientMsgId || msg.msgId.toString()}
                message={msg}
                isOwnMessage={isOwnMessage(msg)}
                onImageClick={(url, type) => setPreviewMedia({ url, type })}
                onResend={handleResendMessage}
                onJoinMeeting={onJoinMeeting}
              />
            ))}
            <div ref={messagesEndRef} style={{ height: '1px' }} />
          </div>
        )}
      </div>

      {showParticipantPicker && (
        <ParticipantPicker
          title="发起多人会议"
          members={getGroupParticipants()}
          onConfirm={(selectedIds) => startMeetingFromChat(selectedIds)}
          onCancel={() => setShowParticipantPicker(false)}
        />
      )}

      {showScheduleMeeting && (
        <ScheduleMeetingDialog
          members={getGroupParticipants()}
          onConfirm={handleScheduleConfirm}
          onCancel={() => setShowScheduleMeeting(false)}
        />
      )}

      {/* 表情包选择器 */}
      {showEmojiPicker && (
        <div className="emoji-picker-popover">
          <div className="emoji-grid">
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
                <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" /></svg>
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
        <div className="toolbar-premium">
          <button
            className="tool-action-btn"
            title="表情"
            onClick={() => setShowEmojiPicker(!showEmojiPicker)}
          >
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
              <line x1="9" y1="9" x2="9.01" y2="9"></line>
              <line x1="15" y1="9" x2="15.01" y2="9"></line>
            </svg>
          </button>
          <button
            className="tool-action-btn"
            title="上传图片"
            onClick={() => handleToolAction(() => handleFileSelect(true))}
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
              <circle cx="8.5" cy="8.5" r="1.5"></circle>
              <polyline points="21 15 16 10 5 21"></polyline>
            </svg>
          </button>
          <button
            className="tool-action-btn"
            title="发送文件"
            onClick={() => handleToolAction(() => handleFileSelect(false))}
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
              <polyline points="13 2 13 9 20 9"></polyline>
            </svg>
          </button>
          <button
            className="tool-action-btn"
            title="屏幕分享"
            onClick={() => handleToolAction(startScreenShare)}
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
              <line x1="8" y1="21" x2="16" y2="21"></line>
              <line x1="12" y1="17" x2="12" y2="21"></line>
            </svg>
          </button>
          <div className="toolbar-divider"></div>
          <button
            className={`tool-action-btn ${isRecording ? 'recording' : ''}`}
            title={isRecording ? '停止录音' : '语音消息'}
            onClick={() => {
              setShowEmojiPicker(false);
              if (isRecording) stopRecording();
              else startRecording();
            }}
          >
            {isRecording ? (
              <div className="recording-indicator">
                <span className="rec-dot"></span>
                <span className="rec-time">{formatRecordingTime(recordingTime)}</span>
              </div>
            ) : (
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                <line x1="12" y1="19" x2="12" y2="23"></line>
                <line x1="8" y1="23" x2="16" y2="23"></line>
              </svg>
            )}
          </button>
        </div>
        <div className="input-row-modern">
          <textarea
            className="textarea-modern"
            placeholder="键入消息..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onFocus={() => setShowEmojiPicker(false)}
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

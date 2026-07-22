import React, { useState, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import ParticipantPicker from './ParticipantPicker';
import ScheduleMeetingDialog from './ScheduleMeetingDialog';
import { RootState, AppDispatch } from '../../store';
import { addMessage, fetchMessages, sendMessageViaSocket, updateMessageAttachmentState } from './chatSlice';
import { BASE_URL, aiBotAPI, authAPI, meetingAPI } from '../../services/api/apiClient';
import { buildPreviewUrl, getMediaPolicy, loadMediaPolicy } from '../../services/mediaPolicyService';
import { useAppSelector } from '../../hooks';
import { getElectronAPI, isElectronEnvironment } from '../../services/api/electronAPI';
import { socketService } from '../../services/socketService';
import axios from 'axios';
import type { ConversationRes, MessageDTO, MeetingMessagePayload } from '../../types';
import type { AuthState } from '../auth/authSlice';
import Notification, { NotificationType } from '../../components/common/Notification';
import { isGroupConversation, getConversationDisplayName, getConversationAvatarText } from '../../utils/conversationUtils';
import './ChatRoom.css';
import { MessageType } from '../../types';

type SocketConnectionState = 'disconnected' | 'connecting' | 'reconnecting' | 'connected';

// 瀹氫箟 Electron 鎺ュ彛鎵╁睍 (闃叉 TS 鎶ラ敊)
declare global {
  interface Window {
    electronAPI: any;
  }
}

interface ChatRoomProps {
  conversation: ConversationRes;
  onVideoCall?: (
    userId: string,
    userName?: string,
    conversationId?: number,
    callKind?: 'VIDEO_CALL' | 'VOICE_CALL'
  ) => void;
  onStartMeeting?: (participants: Array<{ userId: string; userName?: string }>, roomId?: string) => void;
  onJoinMeeting?: (roomId: string) => void;
}

type MessageSenderDisplay = {
  displayName: string;
  avatarText: string;
  avatarSeed: string;
};

type AiBotReplyDto = {
  content?: string;
  messageType?: string;
  metadata?: Record<string, any> | null;
};

type BotCardData = {
  title?: string;
  summary?: string;
  sections?: Array<{ title?: string; text?: string }>;
  actions?: Array<{ label?: string; url?: string; value?: string }>;
};

const BOT_CARD_PREFIX = '[[BOT_CARD]]';
const BOT_TRIGGER_PATTERN = /^@(机器人|AI助手|AI\s*助手)\s*/i;
const AI_ASSISTANT_CONVERSATION_ID = -20260720;
const GROUP_BOT_SETTINGS_KEY = 'group.bot.settings';

type GroupBotSettings = {
  enabled: boolean;
  promptTemplate: string;
  webhookHint: string;
};

const defaultGroupBotSettings: GroupBotSettings = {
  enabled: true,
  promptTemplate: '你是当前群聊的智能助手，请优先回答与当前会话相关的问题，保持简洁清晰。',
  webhookHint: '可接入 /api/ai-bot/webhook/{token}，支持 content、markdown.text、html.content。'
};

const localMediaCache = new Map<string, string>();
const localVoiceUploadCache = new Map<string, { blob: Blob; duration: number }>();

/**
 * Centralize file-id extraction so list thumbnails and preview modals can share
 * the same cached blob URL instead of downloading the same media twice.
 */
const getMediaCacheKey = (url: string) => url || '';

const canCompressBrowserImage = (file: File) => {
  if (!file.type.startsWith('image/')) return false;
  const lowerName = file.name.toLowerCase();
  return !lowerName.endsWith('.gif') && !lowerName.endsWith('.svg');
};

const compressImageForUpload = async (file: File): Promise<File> => {
  const policy = getMediaPolicy();
  if (!policy.uploadCompressionEnabled || !canCompressBrowserImage(file) || file.size <= policy.uploadCompressMinSizeKb * 1024) {
    return file;
  }

  const bitmap = await createImageBitmap(file);
  try {
    const maxEdge = policy.uploadMaxImageEdge;
    const largestEdge = Math.max(bitmap.width, bitmap.height);
    const scale = largestEdge > maxEdge ? maxEdge / largestEdge : 1;
    const targetWidth = Math.max(1, Math.round(bitmap.width * scale));
    const targetHeight = Math.max(1, Math.round(bitmap.height * scale));

    const canvas = document.createElement('canvas');
    canvas.width = targetWidth;
    canvas.height = targetHeight;
    const context = canvas.getContext('2d');
    if (!context) {
      return file;
    }
    context.drawImage(bitmap, 0, 0, targetWidth, targetHeight);

    const targetType = file.type === 'image/png' ? 'image/png' : 'image/jpeg';
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, targetType, policy.uploadJpegQuality / 100)
    );
    if (!blob || blob.size >= file.size) {
      return file;
    }

    const nextName = targetType === file.type
      ? file.name
      : file.name.replace(/\.[^.]+$/, '.jpg');
    return new File([blob], nextName, { type: targetType, lastModified: file.lastModified });
  } finally {
    bitmap.close();
  }
};

const IMAGE_EXTENSION_PATTERN = /\.(jpg|jpeg|png|gif|webp|bmp|heic|heif|avif|svg)$/i;
const VIDEO_EXTENSION_PATTERN = /\.(mp4|mov|mkv|avi|webm|m4v|3gp)$/i;

const isImageAttachment = (fileName?: string, mimeType?: string) =>
  !!mimeType?.startsWith('image/') || (!!fileName && IMAGE_EXTENSION_PATTERN.test(fileName));

const isVideoAttachment = (fileName?: string, mimeType?: string) =>
  !!mimeType?.startsWith('video/') || (!!fileName && VIDEO_EXTENSION_PATTERN.test(fileName));

/**
 * Authenticated Media Hook to handle blob URLs with token
 */
const useAuthenticatedMedia = (
  url: string,
  token?: string,
  options?: {
    preferredSrc?: string;
    disableRemoteFetch?: boolean;
  }
) => {
  const [mediaSrc, setMediaSrc] = useState<string>('');
  const [loading, setLoading] = useState(!!url);
  const [error, setError] = useState(false);
  const preferredSrc = options?.preferredSrc;
  const disableRemoteFetch = options?.disableRemoteFetch;

  useEffect(() => {
    let objectUrl = '';
    let shouldRevokeObjectUrl = false;

    if (preferredSrc) {
      setMediaSrc(preferredSrc);
      setLoading(false);
      setError(false);
      return;
    }

    if (!url || disableRemoteFetch) {
      setMediaSrc('');
      setLoading(false);
      setError(false);
      return;
    }

    const cacheKey = getMediaCacheKey(url);
    if (cacheKey && localMediaCache.has(cacheKey)) {
      setMediaSrc(localMediaCache.get(cacheKey) || '');
      setLoading(false);
      setError(false);
      return;
    }

    const fetchMedia = async () => {
      setLoading(true);
      try {
        const response = await axios.get(url, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
          responseType: 'blob'
        });
        objectUrl = URL.createObjectURL(response.data);
        if (cacheKey) {
          localMediaCache.set(cacheKey, objectUrl);
        } else {
          shouldRevokeObjectUrl = true;
        }
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
      if (shouldRevokeObjectUrl && objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [disableRemoteFetch, preferredSrc, token, url]);

  return { mediaSrc, loading, error };
};

/**
 * Beautiful Custom Audio Player
 */
const createWaveformBars = (seed: string, count = 34) => {
  let value = Array.from(seed).reduce((sum, char) => ((sum * 31) + char.charCodeAt(0)) >>> 0, 7);
  return Array.from({ length: count }, (_, index) => {
    value = (value * 1664525 + 1013904223) >>> 0;
    const contour = Math.sin((index / Math.max(1, count - 1)) * Math.PI) * 0.24;
    return Math.min(1, 0.24 + contour + ((value >>> 16) / 0xffff) * 0.56);
  });
};

const CustomAudioPlayer: React.FC<{
  url: string;
  token?: string;
  localSrc?: string;
  pending?: boolean;
  failed?: boolean;
}> = ({ url, token, localSrc, pending = false, failed = false }) => {
  const { mediaSrc, loading } = useAuthenticatedMedia(url, token, {
    preferredSrc: localSrc,
    disableRemoteFetch: pending && !!localSrc
  });
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [waveform, setWaveform] = useState(() => createWaveformBars(`${url}:${localSrc || ''}`));

  useEffect(() => {
    if (!mediaSrc || !window.AudioContext) return;
    let cancelled = false;
    const context = new AudioContext();
    void fetch(mediaSrc)
      .then(response => response.arrayBuffer())
      .then(buffer => context.decodeAudioData(buffer))
      .then(audioBuffer => {
        if (cancelled) return;
        const samples = audioBuffer.getChannelData(0);
        const bars = 34;
        const bucketSize = Math.max(1, Math.floor(samples.length / bars));
        const peaks = Array.from({ length: bars }, (_, index) => {
          const start = index * bucketSize;
          const end = Math.min(samples.length, start + bucketSize);
          let peak = 0;
          for (let sampleIndex = start; sampleIndex < end; sampleIndex += 1) {
            peak = Math.max(peak, Math.abs(samples[sampleIndex]));
          }
          return peak;
        });
        const maxPeak = Math.max(...peaks, 0.01);
        setWaveform(peaks.map(peak => Math.max(0.16, Math.min(1, peak / maxPeak))));
      })
      .catch(() => {
        // Some codecs cannot be decoded by Web Audio. The player remains usable with the fallback bars.
      })
      .finally(() => void context.close());
    return () => {
      cancelled = true;
      void context.close();
    };
  }, [mediaSrc]);

  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) {
      audioRef.current.pause();
    } else {
      void audioRef.current.play().catch(() => setIsPlaying(false));
    }
    setIsPlaying(!isPlaying);
  };

  const onTimeUpdate = () => {
    if (!audioRef.current) return;
    const total = audioRef.current.duration;
    if (Number.isFinite(total) && total > 0) {
      setProgress((audioRef.current.currentTime / total) * 100);
    }
  };

  const seek = (event: React.MouseEvent<HTMLDivElement>) => {
    const audio = audioRef.current;
    if (!audio || !Number.isFinite(audio.duration) || audio.duration <= 0) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    const nextProgress = Math.max(0, Math.min(1, (event.clientX - bounds.left) / bounds.width));
    audio.currentTime = audio.duration * nextProgress;
    setProgress(nextProgress * 100);
  };

  if (loading) return <div className="audio-skeleton">{pending ? '正在准备语音…' : '正在加载语音…'}</div>;

  return (
    <div className={`custom-audio-player ${pending ? 'is-uploading' : ''} ${failed ? 'is-failed' : ''}`}>
      <audio
        ref={audioRef}
        src={mediaSrc}
        onTimeUpdate={onTimeUpdate}
        onLoadedMetadata={() => setDuration(audioRef.current?.duration || 0)}
        onEnded={() => setIsPlaying(false)}
      />
      <button className="audio-play-btn" onClick={togglePlay} disabled={failed} aria-label={isPlaying ? '暂停语音' : '播放语音'}>
        {isPlaying ? (
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" /></svg>
        ) : (
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
        )}
      </button>
      <div className="audio-waveform" onClick={seek} role="slider" aria-label="语音播放进度" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
        {waveform.map((height, index) => (
          <span key={index} className={index / waveform.length * 100 <= progress ? 'played' : ''} style={{ height: `${Math.round(height * 100)}%` }} />
        ))}
      </div>
      <span className="audio-time-label">{failed ? '重试中断' : pending ? '发送中' : duration ? `${Math.ceil(duration)}″` : '语音'}</span>
    </div>
  );
};

const AuthenticatedImage: React.FC<{
  url: string;
  token?: string;
  className?: string;
  onClick?: (resolvedUrl: string) => void;
  localSrc?: string;
  pending?: boolean;
}> = ({ url, token, className, onClick, localSrc, pending = false }) => {
  const { mediaSrc, loading, error } = useAuthenticatedMedia(url, token, {
    preferredSrc: localSrc,
    disableRemoteFetch: pending && !!localSrc
  });

  if (loading) return (
    <div className={`${className} media-placeholder`}>
      <div className="spinner-small"></div>
    </div>
  );

  if (error) return (
    <div className={`${className} media-placeholder error`}>
      <span>{pending ? 'Preparing image...' : 'Failed to load'}</span>
    </div>
  );

  return <img src={mediaSrc} className={className} onClick={() => onClick?.(mediaSrc || url)} alt="Chat media" />;
};

const AuthenticatedVideo: React.FC<{
  url: string;
  token?: string;
  className?: string;
  controls?: boolean;
  autoPlay?: boolean;
  localSrc?: string;
  pending?: boolean;
}> = ({ url, token, className, controls = false, autoPlay = false, localSrc, pending = false }) => {
  const { mediaSrc, loading, error } = useAuthenticatedMedia(url, token, {
    preferredSrc: localSrc,
    disableRemoteFetch: pending && !!localSrc
  });

  if (loading) return (
    <div className={`${className} media-placeholder`}>
      <div className="spinner-small"></div>
    </div>
  );

  if (error) return (
    <div className={`${className} media-placeholder error`}>
      <span>{pending ? 'Preparing video...' : 'Failed to load'}</span>
    </div>
  );

  return (
    <video
      controls={controls}
      autoPlay={autoPlay}
      className={className}
      src={mediaSrc}
    >
      Your browser does not support video playback.
    </video>
  );
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

const parseBotCard = (content: string): BotCardData | null => {
  if (!content?.startsWith(BOT_CARD_PREFIX)) {
    return null;
  }

  try {
    return JSON.parse(content.slice(BOT_CARD_PREFIX.length)) as BotCardData;
  } catch {
    return null;
  }
};

const renderRichText = (content: string): React.ReactNode => {
  const normalized = (content || '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/(p|div|h\d|blockquote|pre|ul|ol)>/gi, '\n')
    .replace(/<(strong|b)>(.*?)<\/(strong|b)>/gi, '**$2**')
    .replace(/<(em|i)>(.*?)<\/(em|i)>/gi, '*$2*')
    .replace(/<code>(.*?)<\/code>/gi, '`$1`')
    .replace(/<pre[^>]*>/gi, '```\n')
    .replace(/<\/pre>/gi, '\n```')
    .replace(/<li[^>]*>/gi, '- ')
    .replace(/<\/li>/gi, '\n')
    .replace(/<a\s+[^>]*href=['"]([^'"]+)['"][^>]*>(.*?)<\/a>/gi, '[$2]($1)')
    .replace(/<[^>]+>/g, '')
    .trim();

  const lines = normalized.split('\n').filter((line) => line.trim().length > 0);
  const inCodeBlock = normalized.startsWith('```') && normalized.endsWith('```');

  if (inCodeBlock) {
    return <pre className="msg-code-block">{normalized.replace(/^```/, '').replace(/```$/, '').trim()}</pre>;
  }

  return (
    <div className="msg-rich-text">
      {lines.map((line, index) => {
        const trimmed = line.trim();
        const numbered = trimmed.match(/^(\d+)\.\s+(.*)$/);
        const bullet = trimmed.match(/^[-*]\s+(.*)$/);
        const heading = trimmed.match(/^#+\s+(.*)$/);
        const quote = trimmed.match(/^>\s?(.*)$/);

        let className = 'msg-rich-line';
        let text = trimmed;
        if (heading) {
          className += ' heading';
          text = heading[1];
        } else if (numbered) {
          className += ' list';
          text = `${numbered[1]}. ${numbered[2]}`;
        } else if (bullet) {
          className += ' list';
          text = `鈥?${bullet[1]}`;
        } else if (quote) {
          className += ' quote';
          text = quote[1];
        }

        const segments = text.split(/(https?:\/\/[^\s]+|\[[^\]]+\]\((https?:\/\/[^)]+)\)|`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*)/g).filter(Boolean);
        return (
          <div key={`${index}-${text}`} className={className}>
            {segments.map((segment, segmentIndex) => {
              const markdownLink = segment.match(/^\[([^\]]+)]\((https?:\/\/[^)]+)\)$/);
              if (markdownLink) {
                return <a key={segmentIndex} href={markdownLink[2]} target="_blank" rel="noreferrer">{markdownLink[1]}</a>;
              }
              if (/^https?:\/\/[^\s]+$/.test(segment)) {
                return <a key={segmentIndex} href={segment} target="_blank" rel="noreferrer">{segment}</a>;
              }
              if (/^`[^`]+`$/.test(segment)) {
                return <code key={segmentIndex}>{segment.slice(1, -1)}</code>;
              }
              if (/^\*\*[^*]+\*\*$/.test(segment)) {
                return <strong key={segmentIndex}>{segment.slice(2, -2)}</strong>;
              }
              if (/^\*[^*]+\*$/.test(segment)) {
                return <em key={segmentIndex}>{segment.slice(1, -1)}</em>;
              }
              return <React.Fragment key={segmentIndex}>{segment}</React.Fragment>;
            })}
          </div>
        );
      })}
    </div>
  );
};

const BotCard: React.FC<{ card: BotCardData; isOwnMessage: boolean }> = ({ card, isOwnMessage }) => {
  return (
    <div className={`bot-card ${isOwnMessage ? 'own' : ''}`}>
      {card.title && <div className="bot-card-title">{card.title}</div>}
      {card.summary && <div className="bot-card-summary">{card.summary}</div>}
      {!!card.sections?.length && (
        <div className="bot-card-sections">
          {card.sections.map((section, index) => (
            <div className="bot-card-section" key={`${section.title || 'section'}-${index}`}>
              {section.title && <div className="bot-card-section-title">{section.title}</div>}
              {section.text && <div className="bot-card-section-text">{section.text}</div>}
            </div>
          ))}
        </div>
      )}
      {!!card.actions?.length && (
        <div className="bot-card-actions">
          {card.actions.map((action, index) => {
            const label = action.label || '鎿嶄綔';
            const key = `${label}-${index}`;
            if (action.url) {
              return (
                <a key={key} className="bot-card-action" href={action.url} target="_blank" rel="noreferrer">
                  {label}
                </a>
              );
            }
            return (
              <button
                key={key}
                type="button"
                className="bot-card-action secondary"
                onClick={() => action.value && navigator.clipboard?.writeText(action.value)}
              >
                {label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

/**
 * 瀹炴椂娑堟伅閫氳繃 socket 杩涘叆鑱婂ぉ椤垫椂锛屼笉涓€瀹氫細闄勫甫瀹屾暣鐨?fromAccount銆? * 杩欓噷浼樺厛浣跨敤娑堟伅閲岀殑鍙戦€佽€呬俊鎭紝缂哄け鏃跺啀閫€鍥炲埌浼氳瘽鎴愬憳鍒楄〃锛? * 杩欐牱椤甸潰鍙互绗竴鏃堕棿鎶娾€滄槸璋佸彂鏉ョ殑鈥濇覆鏌撳嚭鏉ワ紝鑰屼笉鏄樉绀烘垚闂彿銆? */
const resolveMessageSenderDisplay = (
  message: MessageDTO,
  conversation: ConversationRes
): MessageSenderDisplay => {
  const senderId = String(message.fromAccountId ?? '');
  const members = Array.isArray(conversation.members) ? conversation.members : [];
  const senderMember = members.find((member) => String(member.userId) === senderId);
  const displayName =
    message.fromAccount?.username ||
    senderMember?.username ||
    senderId ||
    '鏈煡鐢ㄦ埛';

  return {
    displayName,
    avatarText: displayName.charAt(0).toUpperCase() || '?',
    avatarSeed: displayName || senderId || 'unknown-user'
  };
};

const MessageBubble: React.FC<{
  message: MessageDTO;
  conversation: ConversationRes;
  isOwnMessage: boolean;
  onImageClick?: (url: string, type: string) => void;
  onResend?: (message: MessageDTO) => void;
  onJoinMeeting?: (roomId: string) => void;
}> = ({ message, conversation, isOwnMessage, onImageClick, onResend, onJoinMeeting }) => {
  const token = useAppSelector((state: RootState) => state.auth.user?.token);
  const senderDisplay = resolveMessageSenderDisplay(message, conversation);
  const attachmentStatus = message.attachmentStatus;
  const isAttachmentPending = attachmentStatus === 'local' || attachmentStatus === 'uploading';
  const localPreviewUrl = message.localPreviewUrl;
  const localFileName =
    message.localFileName ||
    message.payload?.filename ||
    message.payload?.fileName ||
    'Document';

  const formatTime = (timestamp: any) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleDownload = async (fileId: string, fileName: string) => {
    const api = getElectronAPI();
    const url = `${BASE_URL}/api/files/download/${fileId}`;
    try {
      const result = await api.downloadFile(url, fileName, token);
      if (result.success) {
        console.log('File downloaded to:', result.filePath || fileName);
      } else if (!result.canceled) {
        alert('Download failed: ' + result.error);
      }
    } catch (err) {
      console.error('Download error:', err);
    }
  };

  const renderContent = () => {
    const getFileUrl = (fileId: string) => `${BASE_URL}/api/files/download/${fileId}`;
    const getPreviewUrl = (fileId: string, width?: number, quality?: number) =>
      buildPreviewUrl(BASE_URL, fileId, width, quality);
    const type = message.type.toUpperCase();
    const botCard = parseBotCard(message.content);

    if (botCard) {
      return <BotCard card={botCard} isOwnMessage={isOwnMessage} />;
    }

    switch (type) {
      case MessageType.IMAGE: {
        const previewUrl = getPreviewUrl(message.content, 480, 75);
        const originalUrl = isAttachmentPending && localPreviewUrl ? localPreviewUrl : getFileUrl(message.content);
        return (
          <div className="msg-media-container msg-image-container">
            <AuthenticatedImage
              url={previewUrl}
              token={token}
              className="msg-img-preview"
              localSrc={localPreviewUrl}
              pending={isAttachmentPending}
              onClick={() => onImageClick && onImageClick(originalUrl, 'IMAGE')}
            />
            {isAttachmentPending && <div className="attachment-pending-badge">Uploading...</div>}
          </div>
        );
      }
      case MessageType.VIDEO: {
        const previewUrl = getPreviewUrl(message.content, 480, 75);
        const url = getFileUrl(message.content);
        return (
          <div className="msg-media-container msg-video-container" onClick={() => onImageClick && onImageClick(url, 'VIDEO')}>
            {isAttachmentPending ? (
              <AuthenticatedVideo
                url={url}
                token={token}
                className="msg-img-preview"
                localSrc={localPreviewUrl}
                pending={isAttachmentPending}
              />
            ) : (
              <AuthenticatedImage
                url={previewUrl}
                token={token}
                className="msg-img-preview"
              />
            )}
            <div className="video-overlay-play">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="white"><path d="M8 5v14l11-7z" /></svg>
            </div>
            {isAttachmentPending && <div className="attachment-pending-badge">Uploading...</div>}
          </div>
        );
      }
      case MessageType.FILE: {
        return (
          <div className={`msg-file-card ${isAttachmentPending ? 'pending' : ''}`} onClick={() => !isAttachmentPending && handleDownload(message.content, localFileName)}>
            <div className="file-icon-box">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
              </svg>
            </div>
            <div className="file-detail">
              <span className="file-name" title={localFileName}>{localFileName}</span>
              <span className="file-action">{isAttachmentPending ? 'Uploading...' : 'Download'}</span>
            </div>
          </div>
        );
      }
      case MessageType.VOICE: {
        const url = getFileUrl(message.content);
        return (
          <CustomAudioPlayer
            url={url}
            token={token}
            localSrc={localPreviewUrl}
            pending={isAttachmentPending}
            failed={attachmentStatus === 'failed'}
          />
        );
      }
      case MessageType.MEETING: {
        const payload = parseMeetingPayload(message);
        const category = payload?.category || 'MEETING';
        const title = payload?.title || (category === 'VIDEO_CALL' ? '瑙嗛閫氳瘽' : category === 'VOICE_CALL' ? '璇煶閫氳瘽' : '浼氳');
        const count = payload?.participantCount ?? payload?.participantIds?.length ?? 0;
        const roomId = payload?.roomId;
        const isScheduled = payload?.action === 'SCHEDULE';
        const isCallSummary = payload?.action === 'CALL_SUMMARY';
        const scheduledTime = payload?.scheduledAt ? new Date(payload.scheduledAt).toLocaleString() : '';

        if (isCallSummary) {
          return (
            <div className="msg-meeting-card">
              <div className="meeting-title">{title}</div>
              <div className="meeting-meta">{payload?.summary || '通话已结束'}</div>
              {typeof payload?.durationSeconds === 'number' && (
                <div className="meeting-meta">鏃堕暱: {Math.floor(payload.durationSeconds / 60).toString().padStart(2, '0')}:{(payload.durationSeconds % 60).toString().padStart(2, '0')}</div>
              )}
            </div>
          );
        }

        return (
          <div className={`msg-meeting-card ${isScheduled ? 'scheduled' : ''}`}>
            <div className="meeting-title">{isScheduled ? `预定会议: ${title}` : title}</div>
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
        return <div className="msg-text">{renderRichText(message.content)}</div>;
    }
  };

  return (
    <div className={`message-row ${isOwnMessage ? 'own-message' : 'other-message'}`}>
      {!isOwnMessage && (
        <div
          className="msg-avatar"
          style={{ backgroundColor: getColorFromString(senderDisplay.avatarSeed) }}
          title={senderDisplay.displayName}
        >
          {senderDisplay.avatarText}
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
                  <button className="resend-btn" onClick={() => onResend && onResend(message)} title="鐐瑰嚮閲嶅彂">
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
  const [socketConnectionState, setSocketConnectionState] = useState<SocketConnectionState>(socketService.getConnectionState());
  const [showParticipantPicker, setShowParticipantPicker] = useState(false);
  const [showScheduleMeeting, setShowScheduleMeeting] = useState(false);
  const [showBotConfig, setShowBotConfig] = useState(false);
  const [groupBotSettings, setGroupBotSettings] = useState<GroupBotSettings>(defaultGroupBotSettings);

  // Recording State
  const [isRecording, setIsRecording] = useState(false);
  const [isVoiceFinalizing, setIsVoiceFinalizing] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [recordingLevel, setRecordingLevel] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const recordingIntervalRef = useRef<any>(null);
  const recordingStartedAtRef = useRef(0);
  const recordingStreamRef = useRef<MediaStream | null>(null);
  const recordingAudioContextRef = useRef<AudioContext | null>(null);
  const recordingAnimationFrameRef = useRef<number | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const viewportRef = useRef<HTMLDivElement>(null);
  const currentUserId = user?.userId ? Number(user.userId) : null;
  const currentSenderSnapshot = user ? {
    userId: Number(user.userId),
    username: user.username,
    email: user.email || '',
    phoneNumber: user.phoneNumber || ''
  } : undefined;
  const isAiAssistantConversation = conversation.conversationId === AI_ASSISTANT_CONVERSATION_ID;

  const logChatRoom = (scope: string, details?: Record<string, unknown>) => {
    console.log('[ChatRoom]', {
      scope,
      conversationId: conversation.conversationId,
      currentUserId,
      socketConnectionState,
      isElectron: isElectronEnvironment(),
      ...(details || {})
    });
  };

  // 鑾峰彇浼氳瘽鍚嶇О
  const getRoomName = () => {
    return getConversationDisplayName(conversation, user?.userId);
  };

  /**
   * 绉佽亰鍦烘櫙涓嬬粺涓€瑙ｆ瀽鈥滃鏂圭敤鎴封€濄€?   * 鍚庣画澶村儚銆佸湪绾跨姸鎬併€佹嫧鎵撻煶瑙嗛閮戒粠杩欓噷鍙栧€硷紝閬垮厤鍚勫閲嶅鏌?members銆?   */
  const getOtherUser = () => {
    if (isGroupConversation(conversation)) {
      return null;
    }

    return (Array.isArray(conversation.members) ? conversation.members : [])
      .find((member) => member.userId.toString() !== user?.userId) || null;
  };

  const getOtherUserId = () => getOtherUser()?.userId.toString() || null;
  const getOtherUserName = () => getOtherUser()?.username;

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

  const getGroupBotSettingsStorageKey = () => `${GROUP_BOT_SETTINGS_KEY}.${conversation.conversationId}`;

  const shouldTriggerBotReply = (content: string) =>
    isGroupConversation(conversation) &&
    groupBotSettings.enabled &&
    BOT_TRIGGER_PATTERN.test(content.trim());

  const extractBotPrompt = (content: string) => {
    const trimmed = content.trim();
    if (isAiAssistantConversation) {
      return trimmed;
    }
    return trimmed.replace(BOT_TRIGGER_PATTERN, '').trim();
  };

  const toBotRenderableContent = (reply: AiBotReplyDto): string => {
    if ((reply.messageType || '').toLowerCase() === 'card' && reply.metadata) {
      return `${BOT_CARD_PREFIX}${JSON.stringify(reply.metadata)}`;
    }
    return reply.content?.trim() || '我暂时没有组织出合适的回复。';
  };

  const appendLocalBotReply = (content: string) => {
    dispatch(addMessage({
      msgId: -(Date.now()),
      conversationId: conversation.conversationId,
      content,
      fromAccountId: -20260721,
      fromAccount: {
        userId: -20260721,
        username: 'AI 鍔╂墜',
        email: 'ai-assistant@local.group',
        phoneNumber: ''
      },
      type: MessageType.TEXT,
      timestamp: Date.now(),
      clientMsgId: `web-bot-${Date.now()}`,
      sendingStatus: 'success'
    }));
  };

  const requestBotReply = async (sourceContent: string) => {
    const prompt = extractBotPrompt(sourceContent);
    if (!prompt || !currentUserId) {
      return;
    }

    const requestContent = isGroupConversation(conversation) && !isAiAssistantConversation
      ? `${groupBotSettings.promptTemplate}\n\n用户问题：${prompt}`
      : prompt;

    try {
      const response = await aiBotAPI.sendMessage({
        content: requestContent,
        conversationId: conversation.conversationId,
        fromAccountId: currentUserId,
        clientMsgId: window.crypto?.randomUUID?.() || `web-ai-${Date.now()}`
      });
      const reply = (response.data?.data || response.data) as AiBotReplyDto;
      appendLocalBotReply(toBotRenderableContent(reply));
      scrollToBottom();
    } catch (error) {
      console.error('Failed to request bot reply:', error);
      appendLocalBotReply(isAiAssistantConversation ? 'AI 助手暂时无法响应，请稍后重试。' : 'AI 助手暂时无法响应这次 @ 提问，请稍后重试。');
    }
  };

  const saveGroupBotSettings = () => {
    try {
      window.localStorage.setItem(getGroupBotSettingsStorageKey(), JSON.stringify(groupBotSettings));
      showToast('群机器人配置已保存', 'success' as any);
      setShowBotConfig(false);
    } catch (error) {
      console.error('Failed to save group bot settings:', error);
      showToast('保存群机器人配置失败');
    }
  };

  const getSocketStatusText = () => {
    switch (socketConnectionState) {
      case 'connected':
        return '实时连接正常';
      case 'connecting':
        return '实时连接中';
      case 'reconnecting':
        return '重连中，消息会自动补发';
      default:
        return '实时连接已断开，消息会暂存';
    }
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
      showToast(err?.message || '鍒涘缓浼氳澶辫触');
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
      showToast('浼氳棰勫畾鎴愬姛', 'success' as any);
      setShowScheduleMeeting(false);
    } catch (err: any) {
      console.error('Failed to schedule meeting:', err);
      showToast(err?.message || '棰勫畾浼氳澶辫触');
    }
  };

  // 鍔犺浇娑堟伅
  const loadMessages = async () => {
    if (isAiAssistantConversation) {
      return;
    }
    try {
      await dispatch(fetchMessages(conversation.conversationId)).unwrap();
    } catch (err: any) {
      console.error('Failed to load messages:', err);
      showToast(err.message || '鏃犳硶鑾峰彇鍘嗗彶娑堟伅');
    }
  };

  const sendMessage = async () => {
    if (!inputText.trim()) return;

    const content = inputText.trim();
    setInputText('');

    const clientMsgId = window.crypto && window.crypto.randomUUID
      ? window.crypto.randomUUID()
      : Math.random().toString(36).substring(2) + Date.now().toString(36);

    try {
      logChatRoom('send-message-dispatch', {
        clientMsgId,
        contentLength: content.length,
        preview: content.slice(0, 80),
        isAiAssistantConversation
      });

      if (isAiAssistantConversation) {
        dispatch(addMessage({
          msgId: -(Date.now() + 1),
          conversationId: conversation.conversationId,
          content,
          fromAccountId: currentUserId || 0,
          fromAccount: currentSenderSnapshot,
          type: MessageType.TEXT,
          timestamp: Date.now(),
          clientMsgId,
          sendingStatus: 'success'
        }));
        void requestBotReply(content);
        scrollToBottom();
        return;
      }

      dispatch(sendMessageViaSocket({
        conversationId: conversation.conversationId,
        content,
        type: 'TEXT',
        clientMsgId,
        senderSnapshot: currentSenderSnapshot
      }));

      if (shouldTriggerBotReply(content)) {
        void requestBotReply(content);
      }

      scrollToBottom();
    } catch (err: any) {
      console.error('Failed to send message async:', err);
    }
  };

  const handleResendMessage = (message: MessageDTO) => {
    const cachedVoice = message.type === MessageType.VOICE
      ? localVoiceUploadCache.get(message.content)
      : undefined;
    if (cachedVoice && message.attachmentStatus === 'failed') {
      const api = getElectronAPI();
      dispatch(updateMessageAttachmentState({
        conversationId: message.conversationId,
        clientMsgId: message.clientMsgId,
        patch: { attachmentStatus: 'uploading', sendingStatus: 'sending' }
      }));
      void api.uploadFile(cachedVoice.blob as any, message.content, cachedVoice.duration)
        .then(result => {
          if (!result || !(result.id || result.fileMeta)) throw new Error('Voice upload did not return file metadata');
          dispatch(updateMessageAttachmentState({
            conversationId: message.conversationId,
            clientMsgId: message.clientMsgId,
            patch: { attachmentStatus: 'ready', sendingStatus: 'success', payload: result.fileMeta }
          }));
          showToast('语音已重新发送');
        })
        .catch(error => {
          console.error('Voice retry upload error:', error);
          dispatch(updateMessageAttachmentState({
            conversationId: message.conversationId,
            clientMsgId: message.clientMsgId,
            patch: { attachmentStatus: 'failed', sendingStatus: 'failed' }
          }));
          showToast('重试失败，请检查网络后再试');
        });
      return;
    }
    logChatRoom('resend-message-dispatch', {
      clientMsgId: message.clientMsgId,
      msgId: message.msgId,
      type: message.type
    });
    dispatch(sendMessageViaSocket({
      conversationId: message.conversationId,
      content: message.content,
      type: message.type || 'TEXT',
      clientMsgId: message.clientMsgId,
      msgDto: message,
      senderSnapshot: currentSenderSnapshot
    }));
  };

  const handleFileSelect = async (isImage: boolean = false) => {
    try {
      const api = getElectronAPI();
      const options = isImage ? {
        filters: [{ name: 'Images', extensions: ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'heic', 'heif', 'avif', 'svg'] }]
      } : {};

      const result = await api.selectFile(options);

      if (result.canceled) return;

      const preparedBrowserFile = result.file && isImage
        ? await compressImageForUpload(result.file)
        : result.file;

      showToast('鍑嗗涓婁紶...', 'info' as any);

      // 1. 鑾峰彇鏂囦欢鍏冩暟鎹苟鐢宠 UploadId
      let fileName = '';
      let fileSize = 0;

      if (preparedBrowserFile) {
        fileName = preparedBrowserFile.name;
        fileSize = preparedBrowserFile.size;
      } else if (result.filePaths && result.filePaths.length > 0) {
        // Electron 鐜
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
      const mimeType = preparedBrowserFile?.type || (result as any).mimeType || '';
      const isImageFile = isImage || isImageAttachment(fileName, mimeType);
      const isVideoFile = isVideoAttachment(fileName, mimeType);
      const messageType = isImageFile ? MessageType.IMAGE : isVideoFile ? MessageType.VIDEO : MessageType.FILE;
      const clientMsgId = window.crypto && window.crypto.randomUUID
        ? window.crypto.randomUUID()
        : Math.random().toString(36).substring(2) + Date.now().toString(36);
      let localPreviewUrl: string | undefined;

      // Cache local file preview to avoid redundant download
      if (preparedBrowserFile) {
        localPreviewUrl = URL.createObjectURL(preparedBrowserFile);
      } else if (result.filePaths && result.filePaths.length > 0) {
        if ((isImageFile || isVideoFile) && api.readFileAsDataURL) {
          api.readFileAsDataURL(result.filePaths[0]).then(dataUrl => {
            if (dataUrl) {
              localMediaCache.set(fileId, dataUrl);
              dispatch(updateMessageAttachmentState({
                conversationId: conversation.conversationId,
                clientMsgId,
                patch: {
                  localPreviewUrl: dataUrl
                }
              }));
            }
          }).catch(err => {
            console.error('Failed to pre-cache electron file:', err);
          });
        }
      }

      // OPTIMISTIC: Send message immediately after getting uploadId
      // The content is the fileId (UUID), the message type is IMAGE/FILE
      // The recipient will see a loading state if they try to fetch a file that is still UPLOADING
      dispatch(sendMessageViaSocket({
        conversationId: conversation.conversationId,
        content: fileId,
        type: messageType,
        clientMsgId,
        msgDto: {
          msgId: -1,
          conversationId: conversation.conversationId,
          content: fileId,
          fromAccountId: currentUserId || 0,
          type: messageType,
          timestamp: Date.now(),
          clientMsgId,
          sendingStatus: 'sending',
          localPreviewUrl,
          localFileName: fileName,
          localFileSize: fileSize,
          localMimeType: mimeType,
          attachmentStatus: 'uploading',
          payload: idRes.fileMeta
        },
        senderSnapshot: currentSenderSnapshot
      }));

      scrollToBottom();
      showToast('姝ｅ湪鍚庡彴涓婁紶鏂囦欢...', 'info' as any);

      // 2. Background Upload
      const fileToUpload = preparedBrowserFile || result.filePaths[0];
      api.uploadFile(fileToUpload, fileId).then((uploadRes) => {
        if (uploadRes && (uploadRes.id || uploadRes.fileMeta)) {
          dispatch(updateMessageAttachmentState({
            conversationId: conversation.conversationId,
            clientMsgId,
            patch: {
              attachmentStatus: 'ready',
              payload: uploadRes.fileMeta
            }
          }));
          showToast('鏂囦欢涓婁紶瀹屾垚', 'success' as any);
        } else {
          dispatch(updateMessageAttachmentState({
            conversationId: conversation.conversationId,
            clientMsgId,
            patch: {
              attachmentStatus: 'failed',
              sendingStatus: 'failed'
            }
          }));
          showToast('鏂囦欢涓婁紶澶辫触锛屾秷鎭凡鍙戦€佷絾鏃犳硶鏌ョ湅');
        }
      }).catch(err => {
        console.error('Background upload error:', err);
        dispatch(updateMessageAttachmentState({
          conversationId: conversation.conversationId,
          clientMsgId,
          patch: {
            attachmentStatus: 'failed',
            sendingStatus: 'failed'
          }
        }));
        showToast('鍚庡彴涓婁紶鍑洪敊');
      });

    } catch (err: any) {
      console.error('File selection/upload error:', err);
      showToast(err.message || '鎿嶄綔澶辫触');
    }
  };

  // Voice Recording Logic
  const playVoiceCue = (kind: 'start' | 'stop' | 'error') => {
    try {
      const AudioContextConstructor = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContextConstructor) return;
      const context = new AudioContextConstructor();
      const oscillator = context.createOscillator();
      const gain = context.createGain();
      const now = context.currentTime;
      const frequency = kind === 'start' ? 660 : kind === 'stop' ? 440 : 220;
      oscillator.frequency.setValueAtTime(frequency, now);
      if (kind === 'start') oscillator.frequency.linearRampToValueAtTime(880, now + 0.08);
      if (kind === 'stop') oscillator.frequency.linearRampToValueAtTime(330, now + 0.1);
      gain.gain.setValueAtTime(0.0001, now);
      gain.gain.exponentialRampToValueAtTime(0.055, now + 0.012);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.13);
      oscillator.connect(gain).connect(context.destination);
      oscillator.start(now);
      oscillator.stop(now + 0.14);
      oscillator.onended = () => void context.close();
    } catch {
      // Feedback is intentionally best-effort: recording must never depend on audio output permissions.
    }
  };

  const releaseRecordingResources = () => {
    if (recordingIntervalRef.current) clearInterval(recordingIntervalRef.current);
    if (recordingAnimationFrameRef.current !== null) cancelAnimationFrame(recordingAnimationFrameRef.current);
    recordingIntervalRef.current = null;
    recordingAnimationFrameRef.current = null;
    recordingStreamRef.current?.getTracks().forEach(track => track.stop());
    recordingStreamRef.current = null;
    if (recordingAudioContextRef.current) void recordingAudioContextRef.current.close();
    recordingAudioContextRef.current = null;
    setRecordingLevel(0);
  };

  const startRecording = async () => {
    try {
      if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        showToast('当前设备不支持语音录制');
        return;
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
      });
      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;
      recordingStreamRef.current = stream;
      audioChunksRef.current = [];
      recordingStartedAtRef.current = Date.now();

      const audioContext = new AudioContext();
      const analyser = audioContext.createAnalyser();
      analyser.fftSize = 128;
      audioContext.createMediaStreamSource(stream).connect(analyser);
      recordingAudioContextRef.current = audioContext;
      const samples = new Uint8Array(analyser.fftSize);
      const readLevel = () => {
        analyser.getByteTimeDomainData(samples);
        const energy = samples.reduce((sum, sample) => sum + Math.abs(sample - 128), 0) / samples.length;
        setRecordingLevel(Math.min(1, energy / 28));
        recordingAnimationFrameRef.current = requestAnimationFrame(readLevel);
      };
      readLevel();

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunksRef.current.push(e.data);
      };

      recorder.onstop = async () => {
        const mimeType = recorder.mimeType || 'audio/webm';
        const audioBlob = new Blob(audioChunksRef.current, { type: mimeType });
        const api = getElectronAPI();
        const duration = Math.max(0, Date.now() - recordingStartedAtRef.current);
        releaseRecordingResources();
        setIsVoiceFinalizing(false);

        if (duration < 500 || audioBlob.size === 0) {
          playVoiceCue('error');
          showToast('语音太短，按住或录制至少 1 秒');
          return;
        }

        showToast('语音已录好，正在秒速发送…', 'info' as any);

        try {
          // 1. 鑾峰彇 UploadId
          const idRes = await api.getUploadId({
            fileName: `voice_${Date.now()}.${mimeType.includes('ogg') ? 'ogg' : 'webm'}`,
            size: audioBlob.size,
            duration: duration
          });

          if (!idRes || !idRes.id) {
            showToast('语音初始化失败');
            return;
          }

          const fileId = idRes.id;
          localVoiceUploadCache.set(fileId, { blob: audioBlob, duration });

          // Cache local audio preview URL to avoid redundant download
          const localUrl = URL.createObjectURL(audioBlob);
          localMediaCache.set(fileId, localUrl);

          // OPTIMISTIC: Send voice message immediately
          const clientMsgId = window.crypto && window.crypto.randomUUID
            ? window.crypto.randomUUID()
            : Math.random().toString(36).substring(2) + Date.now().toString(36);

          dispatch(sendMessageViaSocket({
            conversationId: conversation.conversationId,
            content: fileId,
            type: 'VOICE',
            clientMsgId,
            msgDto: {
              msgId: -1,
              conversationId: conversation.conversationId,
              content: fileId,
              fromAccountId: currentUserId || 0,
              type: MessageType.VOICE,
              timestamp: Date.now(),
              clientMsgId,
              sendingStatus: 'sending',
              localPreviewUrl: localUrl,
              localFileName: `voice_${Date.now()}.${mimeType.includes('ogg') ? 'ogg' : 'webm'}`,
              localFileSize: audioBlob.size,
              localMimeType: audioBlob.type,
              attachmentStatus: 'uploading'
            },
            senderSnapshot: currentSenderSnapshot
          }));

          // 2. Background Upload
          api.uploadFile(audioBlob as any, fileId, duration).then(res => {
            if (res && (res.id || res.fileMeta)) {
              dispatch(updateMessageAttachmentState({
                conversationId: conversation.conversationId,
                clientMsgId,
                patch: {
                  attachmentStatus: 'ready',
                  payload: res.fileMeta
                }
              }));
            } else {
              dispatch(updateMessageAttachmentState({
                conversationId: conversation.conversationId,
                clientMsgId,
                patch: {
                  attachmentStatus: 'failed',
                  sendingStatus: 'failed'
                }
              }));
            showToast('璇煶涓婁紶澶辫触');
            }
          }).catch(err => {
            console.error('Background voice upload error:', err);
            dispatch(updateMessageAttachmentState({
              conversationId: conversation.conversationId,
              clientMsgId,
              patch: {
                attachmentStatus: 'failed',
                sendingStatus: 'failed'
              }
            }));
            showToast('发送未完成，语音气泡会保留失败状态');
          });
        } catch (err) {
          console.error('Audio upload error:', err);
          showToast('无法发送语音消息');
        }

      };

      recorder.start(250);
      setIsRecording(true);
      setRecordingTime(0);
      playVoiceCue('start');

      recordingIntervalRef.current = setInterval(() => {
        setRecordingTime(prev => prev + 1);
      }, 1000);

    } catch (err) {
      console.error('Failed to start recording:', err);
      releaseRecordingResources();
      playVoiceCue('error');
      showToast('无法访问麦克风，请检查系统权限');
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      setIsVoiceFinalizing(true);
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      playVoiceCue('stop');
    }
  };

  useEffect(() => () => releaseRecordingResources(), []);

  const formatRecordingTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  // 澶勭悊琛ㄦ儏閫夋嫨
  const handleEmojiSelect = (emoji: string) => {
    setInputText(prev => prev + emoji);
    setShowEmojiPicker(false);
  };

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
          showToast('妗岄潰鍒嗕韩宸插紑鍚?(浠呴瑙?', 'success' as any);
          // Stop immediately as it is just a demo for now without real RTC signaling
          stream.getTracks().forEach((track: any) => track.stop());
        } else {
          showToast('褰撳墠娴忚鍣ㄤ笉鏀寔妗岄潰鍒嗕韩');
        }
      }
    } catch (err: any) {
      console.error('Failed to get screen sources:', err);
      showToast('鎿嶄綔琚彇娑堟垨澶辫触');
    }
  };

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

  const scrollToBottom = () => {
    if (viewportRef.current) {
      viewportRef.current.scrollTop = viewportRef.current.scrollHeight;
    }
  };

  const handlePreviewClose = () => setPreviewMedia(null);

  useEffect(() => {
    let interval: any;
    const checkOnline = async () => {
      if (isAiAssistantConversation) {
        setIsOnline(true);
        return;
      }
      const otherUserId = getOtherUserId();
      if (otherUserId && !isGroupConversation(conversation)) {
        try {
          const res = await authAPI.isUserOnline(otherUserId);
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
  }, [conversation, isAiAssistantConversation]);

  useEffect(() => {
    if (!isGroupConversation(conversation)) {
      setGroupBotSettings(defaultGroupBotSettings);
      setShowBotConfig(false);
      return;
    }

    try {
      const saved = window.localStorage.getItem(getGroupBotSettingsStorageKey());
      if (saved) {
        setGroupBotSettings({ ...defaultGroupBotSettings, ...JSON.parse(saved) });
      } else {
        setGroupBotSettings(defaultGroupBotSettings);
      }
    } catch (error) {
      console.error('Failed to load group bot settings:', error);
      setGroupBotSettings(defaultGroupBotSettings);
    }
  }, [conversation.conversationId]);

  useEffect(() => {
    void loadMediaPolicy();
  }, []);

  useEffect(() => {
    loadMessages();
  }, [conversation.conversationId]);

  useEffect(() => {
    setSocketConnectionState(socketService.getConnectionState());
    return socketService.onConnectionStateChange((state) => {
      logChatRoom('socket-state-change', { nextState: state });
      setSocketConnectionState(state);
    });
  }, []);

  // 娑堟伅鏇存柊鏃惰嚜鍔ㄦ粴鍔ㄤ笖鏍囪宸茶
  useEffect(() => {
    scrollToBottom();

    // 濡傛灉鏈夊鏂圭殑娑堟伅锛屾爣璁颁负宸茶
    if (messages.length > 0 && !chatLoading) {
      const lastOtherMsg = [...messages].reverse().find(m => m.fromAccountId.toString() !== user?.userId);
      if (lastOtherMsg && lastOtherMsg.msgId > 0) {
        import('../../services/socketService').then(({ socketService }) => {
          logChatRoom('mark-read-dispatch', { lastMsgId: lastOtherMsg.msgId });
          socketService.markAsRead(conversation.conversationId, lastOtherMsg.msgId);
        });
      }
    }
  }, [messages, chatLoading]);

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

      {/* 椤堕儴瀵艰埅鏍?*/}
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
              {isAiAssistantConversation ? (
                <span>AI 鍦ㄧ嚎 路 鏀寔闂瓟銆乄ebhook銆丮arkdown/Card 鍥炲</span>
              ) : isGroupConversation(conversation) ? (
                <span>{conversation.members?.length || 0} 浣嶆垚鍛?路 {getSocketStatusText()}</span>
              ) : (
                <>
                  <span className={`status-indicator ${isOnline ? 'online' : 'offline'}`}></span>
                  {isOnline ? '鍦ㄧ嚎' : '绂荤嚎'} 路 {getSocketStatusText()}
                </>
              )}
            </div>
          </div>
        </div>

        <div className="chatroom-header-actions">
          {isGroupConversation(conversation) && (
            <button
              className="action-icon-btn"
              onClick={() => setShowBotConfig(true)}
              title="机器人配置"
            >
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 8V4H8"></path>
                <rect x="4" y="8" width="16" height="12" rx="2"></rect>
                <path d="M9 16h6"></path>
                <path d="M9 12h.01"></path>
                <path d="M15 12h.01"></path>
              </svg>
            </button>
          )}
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
          {!isAiAssistantConversation && getOtherUserId() && onVideoCall && (
            <>
              <button
                className="action-icon-btn"
                onClick={() => onVideoCall(
                  getOtherUserId()!,
                  getOtherUserName(),
                  conversation.conversationId,
                  'VOICE_CALL'
                )}
                title="璇煶閫氳瘽"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                </svg>
              </button>
              <button
                className="action-icon-btn"
                onClick={() => onVideoCall(
                  getOtherUserId()!,
                  getOtherUserName(),
                  conversation.conversationId,
                  'VIDEO_CALL'
                )}
                title="瑙嗛閫氳瘽"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <polygon points="23 7 16 12 23 17 23 7"></polygon>
                  <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                </svg>
              </button>
            </>
          )}
          <button className="action-icon-btn" title="鏇村">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="1"></circle>
              <circle cx="12" cy="5" r="1"></circle>
              <circle cx="12" cy="19" r="1"></circle>
            </svg>
          </button>
        </div>
      </div>

      {/* 娑堟伅鍒楄〃 */}
      <div className="messages-viewport" ref={viewportRef} onClick={() => setShowEmojiPicker(false)}>
        {socketConnectionState !== 'connected' && (
          <div className={`socket-status-banner ${socketConnectionState}`}>
            {getSocketStatusText()}
          </div>
        )}
        {chatLoading && messages.length === 0 ? (
          <div className="viewport-loading">
            <div className="spinner-medium"></div>
          </div>
        ) : (
          <div className="messages-list-desktop">
            {messages.map((msg: MessageDTO, index: number) => {
              const uniqueKey = msg.msgId > 0
                ? `server-${msg.msgId}`
                : `client-${msg.clientMsgId || `${msg.timestamp}-${index}`}-${index}`;
              return (
                <MessageBubble
                  key={uniqueKey}
                  message={msg}
                  conversation={conversation}
                  isOwnMessage={isOwnMessage(msg)}
                  onImageClick={(url, type) => setPreviewMedia({ url, type })}
                  onResend={handleResendMessage}
                  onJoinMeeting={onJoinMeeting}
                />
              );
            })}
            <div ref={messagesEndRef} style={{ height: '1px' }} />
          </div>
        )}
      </div>

      {showParticipantPicker && (
        <ParticipantPicker
          title="鍙戣捣澶氫汉浼氳"
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

      {/* 琛ㄦ儏鍖呴€夋嫨鍣?*/}
      {showBotConfig && isGroupConversation(conversation) && (
        <div className="modal-overlay">
          <div className="modal-content bot-config-modal">
            <div className="modal-header">
              <h3>群机器人配置</h3>
              <button className="close-btn" onClick={() => setShowBotConfig(false)}>×</button>
            </div>
            <div className="modal-body bot-config-body">
              <label className="bot-switch-row">
                <div>
                  <div className="bot-switch-title">启用群机器人</div>
                  <div className="bot-switch-desc">群成员使用 `@机器人`、`@AI助手` 时自动触发回复。</div>
                </div>
                <input
                  type="checkbox"
                  checked={groupBotSettings.enabled}
                  onChange={(e) => setGroupBotSettings((prev) => ({ ...prev, enabled: e.target.checked }))}
                />
              </label>
              <div className="bot-config-grid">
                <label>
                  <span>群提示词</span>
                  <textarea
                    value={groupBotSettings.promptTemplate}
                    onChange={(e) => setGroupBotSettings((prev) => ({ ...prev, promptTemplate: e.target.value }))}
                    rows={4}
                  />
                </label>
                <label>
                  <span>Webhook 提示</span>
                  <textarea
                    value={groupBotSettings.webhookHint}
                    onChange={(e) => setGroupBotSettings((prev) => ({ ...prev, webhookHint: e.target.value }))}
                    rows={3}
                  />
                </label>
              </div>
              <div className="bot-config-tips">
                <div className="bot-config-tip">支持 Markdown、HTML、Card 类型回复在当前消息气泡中渲染。</div>
                <div className="bot-config-tip">{groupBotSettings.webhookHint}</div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowBotConfig(false)}>取消</button>
              <button className="btn-primary" onClick={saveGroupBotSettings}>保存配置</button>
            </div>
          </div>
        </div>
      )}

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

      {/* 妗岄潰鍒嗕韩閫夋嫨鍣ㄥ脊绐?*/}
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

      {/* 濯掍綋棰勮 Modal - Enhanced with Glassmorphism */}
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
                <AuthenticatedVideo
                  url={previewMedia.url}
                  token={user?.token}
                  className="active-media-preview"
                  controls
                  autoPlay
                />
              )}
            </div>
          </div>
        </div>
      )}

      {/* 杈撳叆鍖哄煙 */}
      <div className="message-input-bar">
        <div className="toolbar-premium">
          <button
            className="tool-action-btn"
            title="琛ㄦ儏"
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
            title="涓婁紶鍥剧墖"
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
            title="灞忓箷鍒嗕韩"
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
            className={`tool-action-btn voice-record-button ${isRecording ? 'recording' : ''}`}
            title={isVoiceFinalizing ? '正在处理语音' : isRecording ? '结束并发送语音' : '录制语音消息'}
            disabled={isVoiceFinalizing}
            onClick={() => {
              setShowEmojiPicker(false);
              if (isRecording) stopRecording();
              else if (!isVoiceFinalizing) startRecording();
            }}
          >
            {isRecording ? (
              <div className="recording-indicator">
                <span className="rec-dot"></span>
                <span className="recording-level" style={{ transform: `scaleX(${0.2 + recordingLevel * 0.8})` }}></span>
                <span className="rec-time">{formatRecordingTime(recordingTime)}</span>
              </div>
            ) : isVoiceFinalizing ? (
              <div className="voice-sending-spinner" aria-label="正在处理语音"></div>
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
            placeholder={isAiAssistantConversation ? '直接向 AI 助手提问...' : '键入消息...'}
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

// 鏍规嵁瀛楃涓茬敓鎴愰鑹茬殑杈呭姪鍑芥暟
const getColorFromString = (str: string): string => {
  const colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
};

export default ChatRoom;

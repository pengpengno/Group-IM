import React, { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import Loading from '../../components/common/Loading';
import { meetingAPI } from '../../services/api/apiClient';
import type { ConversationRes, MeetingDTO, RootState } from '../../types';
import './MeetingList.css';

interface MeetingListProps {
  onJoin: (roomId: string) => void;
  highlightedRoomId?: string | null;
}

type ComposerMode = 'instant' | 'scheduled';
type StatusFilter = 'ALL' | 'ACTIVE' | 'SCHEDULED' | 'ENDED';

type GroupConversation = ConversationRes & {
  members: NonNullable<ConversationRes['members']>;
};

const unwrapPayload = <T,>(payload: any): T => payload?.data?.data ?? payload?.data ?? payload;

const formatDateTime = (value?: string) => {
  if (!value) {
    return 'Not scheduled';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
};

const getMeetingStatus = (status?: string) => {
  switch (status) {
    case 'ACTIVE':
      return { text: 'In Progress', tone: 'active' as const };
    case 'SCHEDULED':
      return { text: 'Scheduled', tone: 'scheduled' as const };
    case 'ENDED':
      return { text: 'Ended', tone: 'ended' as const };
    default:
      return { text: status || 'Unknown', tone: 'ended' as const };
  }
};

const buildDefaultSchedule = () => {
  const now = new Date();
  now.setMinutes(0, 0, 0);
  now.setHours(now.getHours() + 1);
  const offset = now.getTimezoneOffset();
  const local = new Date(now.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 16);
};

interface ComposerDialogProps {
  conversations: GroupConversation[];
  currentUserId?: number;
  mode: ComposerMode;
  submitting: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (payload: { conversationId: number; title: string; participantIds: number[]; scheduledAt?: string }) => void;
}

const MeetingComposerDialog: React.FC<ComposerDialogProps> = ({
  conversations,
  currentUserId,
  mode,
  submitting,
  error,
  onClose,
  onSubmit
}) => {
  const [conversationId, setConversationId] = useState<number>(conversations[0]?.conversationId ?? 0);
  const [title, setTitle] = useState(conversations[0]?.groupName || '');
  const [scheduledAt, setScheduledAt] = useState(buildDefaultSchedule());

  useEffect(() => {
    if (!conversations.length) {
      setConversationId(0);
      setTitle('');
      return;
    }

    const first = conversations[0];
    setConversationId((prev) => (prev && conversations.some((item) => item.conversationId === prev) ? prev : first.conversationId));
  }, [conversations]);

  const selectedConversation = useMemo(
    () => conversations.find((item) => item.conversationId === conversationId) ?? conversations[0],
    [conversations, conversationId]
  );

  useEffect(() => {
    if (selectedConversation && !title.trim()) {
      setTitle(selectedConversation.groupName || '');
    }
  }, [selectedConversation, title]);

  const participants = (selectedConversation?.members || []).filter((member) => member.userId !== currentUserId);

  const handleSubmit = () => {
    if (!selectedConversation) {
      return;
    }

    const finalTitle = title.trim() || selectedConversation.groupName || 'Meeting';
    onSubmit({
      conversationId: selectedConversation.conversationId,
      title: finalTitle,
      participantIds: participants.map((member) => member.userId),
      scheduledAt: mode === 'scheduled' ? scheduledAt : undefined
    });
  };

  return (
    <div className="meeting-composer-overlay" onClick={onClose}>
      <div className="meeting-composer-card" onClick={(event) => event.stopPropagation()}>
        <div className="meeting-composer-header">
          <div>
            <h3>{mode === 'scheduled' ? 'Schedule Meeting' : 'Start Meeting'}</h3>
            <p>{mode === 'scheduled' ? 'Create a reservation for a group conversation.' : 'Launch an instant room from a group conversation.'}</p>
          </div>
          <button className="meeting-close-btn" onClick={onClose} disabled={submitting} aria-label="Close">
            ×
          </button>
        </div>

        {!conversations.length ? (
          <div className="meeting-composer-empty">
            <h4>No group conversations yet</h4>
            <p>Create a group chat first, then you can start or schedule meetings from it.</p>
          </div>
        ) : (
          <>
            <label className="meeting-field">
              <span>Group conversation</span>
              <select value={conversationId} onChange={(event) => setConversationId(Number(event.target.value))}>
                {conversations.map((conversation) => (
                  <option key={conversation.conversationId} value={conversation.conversationId}>
                    {conversation.groupName || `Group #${conversation.conversationId}`}
                  </option>
                ))}
              </select>
            </label>

            <label className="meeting-field">
              <span>Meeting title</span>
              <input
                type="text"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Weekly sync, launch review, product demo..."
              />
            </label>

            {mode === 'scheduled' && (
              <label className="meeting-field">
                <span>Scheduled time</span>
                <input
                  type="datetime-local"
                  value={scheduledAt}
                  onChange={(event) => setScheduledAt(event.target.value)}
                />
              </label>
            )}

            <div className="meeting-field">
              <span>Participants ({participants.length})</span>
              <div className="meeting-member-preview">
                {participants.length === 0 ? (
                  <p>This group has no other members to invite yet.</p>
                ) : (
                  participants.map((member) => (
                    <div key={member.userId} className="meeting-member-chip">
                      <strong>{member.username?.charAt(0)?.toUpperCase() || '?'}</strong>
                      <span>{member.username}</span>
                    </div>
                  ))
                )}
              </div>
            </div>

            {error && <div className="meeting-feedback error">{error}</div>}
          </>
        )}

        <div className="meeting-composer-footer">
          <button className="meeting-secondary-btn" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button
            className="meeting-primary-btn"
            onClick={handleSubmit}
            disabled={
              submitting
              || !selectedConversation
              || participants.length === 0
              || (mode === 'scheduled' && !scheduledAt)
            }
          >
            {submitting ? 'Submitting...' : mode === 'scheduled' ? 'Schedule meeting' : 'Start now'}
          </button>
        </div>
      </div>
    </div>
  );
};

const MeetingList: React.FC<MeetingListProps> = ({ onJoin, highlightedRoomId }) => {
  const { conversations } = useSelector((state: RootState) => state.chat);
  const currentUserId = useSelector((state: RootState) => state.auth.user?.userId);

  const groupConversations = useMemo(
    () =>
      conversations
        .map((item) => item.conversation)
        .filter((conversation): conversation is GroupConversation =>
          conversation.conversationType === 'GROUP' && Array.isArray(conversation.members)
        ),
    [conversations]
  );

  const [meetings, setMeetings] = useState<MeetingDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [queryMode, setQueryMode] = useState<'my' | 'conversation'>('my');
  const [conversationFilter, setConversationFilter] = useState<number | 'all'>('all');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [searchText, setSearchText] = useState('');
  const [composerMode, setComposerMode] = useState<ComposerMode | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchMeetings = async (options?: { silent?: boolean; mode?: 'my' | 'conversation'; conversationId?: number | 'all' }) => {
    const nextMode = options?.mode ?? queryMode;
    const nextConversationId = options?.conversationId ?? conversationFilter;
    const silent = options?.silent ?? false;

    if (silent) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError(null);
    try {
      const response = nextMode === 'conversation' && nextConversationId !== 'all'
        ? await meetingAPI.listByConversation(Number(nextConversationId))
        : await meetingAPI.listMyMeetings();
      setMeetings(unwrapPayload<MeetingDTO[]>(response));
    } catch (fetchError: any) {
      console.error('Failed to fetch meetings:', fetchError);
      setError(fetchError?.message || 'Failed to load meetings.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchMeetings();
  }, []);

  const handleRefresh = async () => {
    await fetchMeetings({ silent: true });
  };

  const handleQueryModeChange = async (mode: 'my' | 'conversation') => {
    setQueryMode(mode);
    const nextConversationId = mode === 'conversation' && conversationFilter === 'all'
      ? (groupConversations[0]?.conversationId ?? 'all')
      : conversationFilter;
    setConversationFilter(nextConversationId);
    await fetchMeetings({ mode, conversationId: nextConversationId });
  };

  const handleConversationFilterChange = async (value: number | 'all') => {
    setConversationFilter(value);
    if (queryMode === 'conversation') {
      await fetchMeetings({ mode: 'conversation', conversationId: value });
    }
  };

  const handleCreateMeeting = async (payload: { conversationId: number; title: string; participantIds: number[]; scheduledAt?: string }) => {
    setSubmitting(true);
    setFeedback(null);
    setError(null);

    try {
      const response = await meetingAPI.create(payload);
      const meeting = unwrapPayload<MeetingDTO>(response);
      setComposerMode(null);
      setFeedback(payload.scheduledAt ? 'Meeting scheduled successfully.' : 'Meeting created successfully.');
      await fetchMeetings({ silent: true });

      if (!payload.scheduledAt && meeting?.roomId) {
        onJoin(meeting.roomId);
      }
    } catch (submitError: any) {
      console.error('Failed to create meeting:', submitError);
      setError(submitError?.message || 'Failed to create meeting.');
    } finally {
      setSubmitting(false);
    }
  };

  const visibleMeetings = useMemo(() => {
    const keyword = searchText.trim().toLowerCase();
    return meetings.filter((meeting) => {
      const matchesStatus = statusFilter === 'ALL' || meeting.status === statusFilter;
      const matchesKeyword = !keyword
        || (meeting.title || '').toLowerCase().includes(keyword)
        || (meeting.roomId || '').toLowerCase().includes(keyword);
      return matchesStatus && matchesKeyword;
    });
  }, [meetings, searchText, statusFilter]);

  const summary = useMemo(() => ({
    total: meetings.length,
    active: meetings.filter((meeting) => meeting.status === 'ACTIVE').length,
    scheduled: meetings.filter((meeting) => meeting.status === 'SCHEDULED').length
  }), [meetings]);

  if (loading) {
    return (
      <div className="meeting-list-shell">
        <Loading text="Preparing meeting workspace..." />
      </div>
    );
  }

  return (
    <div className="meeting-list-shell">
      {composerMode && (
        <MeetingComposerDialog
          conversations={groupConversations}
          currentUserId={currentUserId ? Number(currentUserId) : undefined}
          mode={composerMode}
          submitting={submitting}
          error={error}
          onClose={() => {
            if (!submitting) {
              setComposerMode(null);
              setError(null);
            }
          }}
          onSubmit={handleCreateMeeting}
        />
      )}

      <div className="meeting-hero">
        <div>
          <p className="meeting-overline">Meetings</p>
          <h2>Meeting control center</h2>
          <p className="meeting-subtitle">Create, schedule, refresh, and inspect meeting records across your group conversations.</p>
        </div>

        <div className="meeting-hero-actions">
          <button className="meeting-ghost-btn" onClick={handleRefresh} disabled={refreshing}>
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
          <button className="meeting-secondary-btn" onClick={() => setComposerMode('scheduled')}>
            Schedule
          </button>
          <button className="meeting-primary-btn" onClick={() => setComposerMode('instant')}>
            Start meeting
          </button>
        </div>
      </div>

      <div className="meeting-stats">
        <div className="meeting-stat-card">
          <span>Total</span>
          <strong>{summary.total}</strong>
        </div>
        <div className="meeting-stat-card">
          <span>Live now</span>
          <strong>{summary.active}</strong>
        </div>
        <div className="meeting-stat-card">
          <span>Scheduled</span>
          <strong>{summary.scheduled}</strong>
        </div>
      </div>

      <div className="meeting-toolbar">
        <div className="meeting-toggle-group">
          <button
            className={queryMode === 'my' ? 'active' : ''}
            onClick={() => handleQueryModeChange('my')}
          >
            My meetings
          </button>
          <button
            className={queryMode === 'conversation' ? 'active' : ''}
            onClick={() => handleQueryModeChange('conversation')}
          >
            Query by group
          </button>
        </div>

        <div className="meeting-toolbar-controls">
          {queryMode === 'conversation' && (
            <select
              className="meeting-select"
              value={conversationFilter}
              onChange={(event) => handleConversationFilterChange(event.target.value === 'all' ? 'all' : Number(event.target.value))}
            >
              {groupConversations.length === 0 && <option value="all">No group conversations</option>}
              {groupConversations.map((conversation) => (
                <option key={conversation.conversationId} value={conversation.conversationId}>
                  {conversation.groupName || `Group #${conversation.conversationId}`}
                </option>
              ))}
            </select>
          )}

          <select
            className="meeting-select"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
          >
            <option value="ALL">All status</option>
            <option value="ACTIVE">In progress</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="ENDED">Ended</option>
          </select>

          <div className="meeting-search">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              type="text"
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
              placeholder="Search by title or room ID"
            />
          </div>
        </div>
      </div>

      {feedback && <div className="meeting-feedback success">{feedback}</div>}
      {error && !composerMode && <div className="meeting-feedback error">{error}</div>}

      {visibleMeetings.length === 0 ? (
        <div className="meeting-empty-state">
          <div className="meeting-empty-icon">
            <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <polygon points="23 7 16 12 23 17 23 7"></polygon>
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
            </svg>
          </div>
          <h3>No meetings match this view</h3>
          <p>Try changing the group filter, status filter, or create a new meeting from the buttons above.</p>
        </div>
      ) : (
        <div className="meetings-grid">
          {visibleMeetings.map((meeting) => {
            const status = getMeetingStatus(meeting.status);
            const isHighlighted = !!highlightedRoomId && meeting.roomId === highlightedRoomId;
            return (
              <div
                key={meeting.meetingId}
                className={`meeting-card meeting-card-${status.tone} ${isHighlighted ? 'meeting-card-highlighted' : ''}`}
              >
                <div className="meeting-card-top">
                  <span className={`meeting-status-badge ${status.tone}`}>{status.text}</span>
                  <span className="meeting-time">{formatDateTime(meeting.scheduledAt || meeting.startedAt)}</span>
                </div>

                <h4>{meeting.title || 'Untitled meeting'}</h4>
                <p className="meeting-room-text">
                  Room ID <code>{meeting.roomId}</code>
                </p>

                <div className="meeting-participants">
                  <div className="meeting-avatar-stack">
                    {(meeting.participants || []).slice(0, 4).map((participant) => (
                      <div key={participant.userId} className="meeting-avatar">
                        {participant.username?.charAt(0)?.toUpperCase() || '?'}
                      </div>
                    ))}
                  </div>
                  <span>{meeting.participants?.length || 0} participants</span>
                </div>

                <div className="meeting-card-footer">
                  <div className="meeting-conversation-id">Conversation #{meeting.conversationId}</div>
                  <button
                    className="meeting-join-btn"
                    disabled={meeting.status === 'ENDED'}
                    onClick={() => onJoin(meeting.roomId)}
                  >
                    {meeting.status === 'SCHEDULED' ? 'Enter lobby' : meeting.status === 'ENDED' ? 'Closed' : 'Join meeting'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default MeetingList;

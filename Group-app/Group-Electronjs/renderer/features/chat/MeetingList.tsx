import React, { useState, useEffect } from 'react';
import { meetingAPI } from '../../services/api/apiClient';
import type { MeetingDTO } from '../../types';
import './ChatRoom.css'; // Reuse some message bubble styles

interface MeetingListProps {
  onJoin: (roomId: string) => void;
}

const MeetingList: React.FC<MeetingListProps> = ({ onJoin }) => {
  const [meetings, setMeetings] = useState<MeetingDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMeetings();
  }, []);

  const fetchMeetings = async () => {
    setLoading(true);
    try {
      const response = await meetingAPI.listMyMeetings();
      setMeetings(response.data?.data || response.data || []);
    } catch (error) {
      console.error('Failed to fetch meetings:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'ACTIVE': return { text: '进行中', color: '#10b981', bg: '#ecfdf5' };
      case 'SCHEDULED': return { text: '已预定', color: '#3b82f6', bg: '#eff6ff' };
      case 'ENDED': return { text: '已结束', color: '#6b7280', bg: '#f3f4f6' };
      default: return { text: status, color: '#6b7280', bg: '#f3f4f6' };
    }
  };

  if (loading) {
    return (
      <div className="meetings-loading" style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
        <p>Loading meetings...</p>
      </div>
    );
  }

  return (
    <div className="meetings-list-container" style={{ padding: '24px', height: '100%', overflowY: 'auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: '800', margin: 0, color: '#111827' }}>我的会议</h2>
        <button className="action-btn" onClick={fetchMeetings} title="Refresh">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
          </svg>
        </button>
      </div>

      {meetings.length === 0 ? (
        <div className="empty-view-placeholder" style={{ marginTop: '100px' }}>
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#ddd" strokeWidth="1.5">
            <polygon points="23 7 16 12 23 17 23 7"></polygon>
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
          </svg>
          <h3>暂无会议记录</h3>
          <p>您参与的会议或预定的会议将显示在这里。</p>
        </div>
      ) : (
        <div className="meetings-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
          {meetings.map((meeting) => {
            const status = getStatusLabel(meeting.status || 'UNKNOWN');
            const isScheduled = meeting.status === 'SCHEDULED';
            
            return (
              <div 
                key={meeting.meetingId} 
                className="meeting-card" 
                style={{ 
                  background: 'white', 
                  borderRadius: '16px', 
                  border: '1px solid #f3f4f6',
                  padding: '20px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  position: 'relative',
                  overflow: 'hidden'
                }}
              >
                <div style={{ position: 'absolute', top: 0, left: 0, width: '4px', height: '100%', background: status.color }}></div>
                
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                  <span style={{ 
                    padding: '4px 10px', 
                    borderRadius: '20px', 
                    fontSize: '12px', 
                    fontWeight: '700', 
                    color: status.color, 
                    background: status.bg 
                  }}>
                    {status.text}
                  </span>
                  {meeting.scheduledAt && (
                    <span style={{ fontSize: '12px', color: '#6b7280', fontWeight: '500' }}>
                      📅 {new Date(meeting.scheduledAt).toLocaleString()}
                    </span>
                  )}
                </div>

                <h4 style={{ margin: '0 0 8px 0', fontSize: '18px', fontWeight: '700', color: '#1f2937' }}>{meeting.title || '无标题会议'}</h4>
                <p style={{ margin: '0 0 16px 0', fontSize: '13px', color: '#6b7280' }}>
                  会议室 ID: <code style={{ background: '#f3f4f6', padding: '2px 4px', borderRadius: '4px' }}>{meeting.roomId}</code>
                </p>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                   <div style={{ display: 'flex', marginLeft: '4px' }}>
                      {meeting.participants?.slice(0, 3).map((p, idx) => (
                        <div key={p.userId} style={{ 
                          width: '28px', 
                          height: '28px', 
                          borderRadius: '50%', 
                          background: '#e5e7eb', 
                          border: '2px solid white',
                          marginLeft: idx === 0 ? 0 : '-8px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '10px',
                          fontWeight: '700'
                        }}>
                          {p.username?.charAt(0).toUpperCase()}
                        </div>
                      ))}
                      {(meeting.participants?.length || 0) > 3 && (
                        <div style={{ 
                          width: '28px', 
                          height: '28px', 
                          borderRadius: '50%', 
                          background: '#f3f4f6', 
                          border: '2px solid white',
                          marginLeft: '-8px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '10px',
                          color: '#6b7280'
                        }}>
                          +{(meeting.participants?.length || 0) - 3}
                        </div>
                      )}
                   </div>
                   <span style={{ fontSize: '13px', color: '#6b7280' }}>{meeting.participants?.length || 0} 人参与</span>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  {meeting.status !== 'ENDED' ? (
                    <button 
                      className="premium-action-btn" 
                      style={{ 
                        flex: 1, 
                        padding: '10px', 
                        fontSize: '13px',
                        background: isScheduled ? '#6366f1' : '#10b981'
                      }}
                      onClick={() => onJoin(meeting.roomId)}
                    >
                      {isScheduled ? '进入准备' : '加入会议'}
                    </button>
                  ) : (
                    <button 
                      className="premium-action-btn" 
                      style={{ 
                        flex: 1, 
                        padding: '10px', 
                        fontSize: '13px',
                        background: '#9ca3af',
                        cursor: 'default'
                      }}
                      disabled
                    >
                      会议已结束
                    </button>
                  )}
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

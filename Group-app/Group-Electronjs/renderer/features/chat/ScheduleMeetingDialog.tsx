import React, { useState } from 'react';
import './ParticipantPicker.css'; // Reuse existing picker styles for modal overlay

interface UserInfo {
  userId: string;
  userName?: string;
  username?: string;
  email?: string;
}

interface ScheduleMeetingDialogProps {
  members: UserInfo[];
  onConfirm: (data: { title: string; scheduledAt: string; participantIds: string[] }) => void;
  onCancel: () => void;
}

const ScheduleMeetingDialog: React.FC<ScheduleMeetingDialogProps> = ({
  members,
  onConfirm,
  onCancel
}) => {
  const [title, setTitle] = useState('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [selectedIds, setSelectedIds] = useState<string[]>(members.map(m => m.userId));

  const toggleMember = (id: string) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  };

  const handleConfirm = () => {
    if (!title || !scheduledAt || selectedIds.length === 0) {
      alert('请填写会议标题、时间并至少选择一位成员');
      return;
    }
    onConfirm({ title, scheduledAt, participantIds: selectedIds });
  };

  return (
    <div className="modal-overlay">
      <div className="participant-picker-card" style={{ 
        width: '480px', 
        background: 'rgba(255, 255, 255, 0.9)', 
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.4)',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
      }}>
        <div className="picker-header" style={{ padding: '24px', borderBottom: '1px solid rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ 
              width: '40px', 
              height: '40px', 
              borderRadius: '12px', 
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white'
            }}>
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="16" y1="2" x2="16" y2="6"></line>
                <line x1="8" y1="2" x2="8" y2="6"></line>
                <line x1="3" y1="10" x2="21" y2="10"></line>
              </svg>
            </div>
            <h3 style={{ margin: 0, fontSize: '20px', fontWeight: '800', color: '#111827' }}>预定会议</h3>
          </div>
          <button className="close-btn" onClick={onCancel} style={{ fontSize: '24px' }}>×</button>
        </div>
        
        <div className="picker-body" style={{ padding: '24px' }}>
          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600', color: '#4b5563' }}>会议标题</label>
            <input 
              type="text" 
              className="form-control"
              placeholder="例如: 季度业务回顾会议"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{ 
                width: '100%', 
                padding: '12px 16px', 
                borderRadius: '12px', 
                border: '1px solid #e5e7eb',
                fontSize: '15px',
                outline: 'none',
                transition: 'border-color 0.2s',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600', color: '#4b5563' }}>预定时间</label>
            <input 
              type="datetime-local" 
              className="form-control"
              value={scheduledAt}
              onChange={(e) => setScheduledAt(e.target.value)}
              style={{ 
                width: '100%', 
                padding: '12px 16px', 
                borderRadius: '12px', 
                border: '1px solid #e5e7eb',
                fontSize: '15px',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <label style={{ margin: 0, fontSize: '14px', fontWeight: '600', color: '#4b5563' }}>参与人员 ({selectedIds.length})</label>
            <span style={{ fontSize: '12px', color: '#3b82f6', cursor: 'pointer', fontWeight: '600' }} onClick={() => setSelectedIds(members.map(m => m.userId))}>全选</span>
          </div>
          
          <div className="member-list" style={{ 
            maxHeight: '220px', 
            overflowY: 'auto', 
            border: '1px solid #f3f4f6', 
            borderRadius: '14px',
            background: '#f9fafb'
          }}>
            {members.map(member => (
              <div 
                key={member.userId} 
                className={`member-item ${selectedIds.includes(member.userId) ? 'selected' : ''}`}
                onClick={() => toggleMember(member.userId)}
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  padding: '12px 16px',
                  cursor: 'pointer',
                  borderBottom: '1px solid rgba(0,0,0,0.03)',
                  transition: 'background 0.2s'
                }}
              >
                <div style={{ 
                  width: '36px', 
                  height: '36px', 
                  borderRadius: '10px', 
                  background: '#3b82f6', 
                  color: 'white',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: '700',
                  fontSize: '14px',
                  marginRight: '12px'
                }}>
                  {(member.username || member.userName || '?').charAt(0).toUpperCase()}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: '14px', fontWeight: '600', color: '#1f2937' }}>{member.username || member.userName}</div>
                </div>
                <div className="checkbox-wrapper">
                  <input 
                    type="checkbox" 
                    readOnly 
                    checked={selectedIds.includes(member.userId)}
                    style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
        
        <div className="picker-footer" style={{ padding: '20px 24px', background: '#f8fafc', borderRadius: '0 0 24px 24px', display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
          <button 
            className="btn-secondary" 
            onClick={onCancel}
            style={{ 
              padding: '10px 20px', 
              borderRadius: '12px', 
              border: '1px solid #e2e8f0', 
              background: 'white',
              fontWeight: '600',
              cursor: 'pointer'
            }}
          >
            取消
          </button>
          <button 
            className="btn-primary" 
            onClick={handleConfirm}
            style={{ 
              padding: '10px 24px', 
              borderRadius: '12px', 
              border: 'none', 
              background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
              color: 'white',
              fontWeight: '700',
              cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(37, 99, 235, 0.2)'
            }}
          >
            确认预定
          </button>
        </div>
      </div>
    </div>
  );
};

export default ScheduleMeetingDialog;

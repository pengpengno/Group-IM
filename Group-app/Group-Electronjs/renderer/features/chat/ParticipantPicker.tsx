import React, { useState } from 'react';
import './ParticipantPicker.css';

interface UserInfo {
  userId: string;
  userName?: string;
  username?: string;
  email?: string;
}

interface ParticipantPickerProps {
  title?: string;
  members: UserInfo[];
  initialSelected?: string[];
  onConfirm: (selectedIds: string[]) => void;
  onCancel: () => void;
}

const ParticipantPicker: React.FC<ParticipantPickerProps> = ({
  title = "选择参会者",
  members,
  initialSelected = [],
  onConfirm,
  onCancel
}) => {
  const [selectedIds, setSelectedIds] = useState<string[]>(initialSelected);

  const toggleMember = (id: string) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  };

  return (
    <div className="modal-overlay">
      <div className="participant-picker-card">
        <div className="picker-header">
          <h3>{title}</h3>
          <button className="close-btn" onClick={onCancel}>×</button>
        </div>
        
        <div className="picker-body">
          <div className="member-list">
            {members.map(member => (
              <div 
                key={member.userId} 
                className={`member-item ${selectedIds.includes(member.userId) ? 'selected' : ''}`}
                onClick={() => toggleMember(member.userId)}
              >
                <div className="member-avatar">
                  {(member.username || member.userName || '?').charAt(0).toUpperCase()}
                </div>
                <div className="member-info">
                  <div className="member-name">{member.username || member.userName}</div>
                  <div className="member-email">{member.email}</div>
                </div>
                <div className="checkbox-wrapper">
                  <input 
                    type="checkbox" 
                    readOnly 
                    checked={selectedIds.includes(member.userId)} 
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
        
        <div className="picker-footer">
          <button className="btn-secondary" onClick={onCancel}>取消</button>
          <button 
            className="btn-primary" 
            onClick={() => onConfirm(selectedIds)}
            disabled={selectedIds.length === 0}
          >
            确定 ({selectedIds.length})
          </button>
        </div>
      </div>
    </div>
  );
};

export default ParticipantPicker;

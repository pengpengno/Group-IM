import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../../store';
import type { UserInfo, OrganizationNode, ContactUser } from '../../types/index';
import { getElectronAPI } from '../../api/electronAPI';
import './ContactsScreen.css';
import { AuthState} from '../../features/auth/authSlice'
const ContactsScreen: React.FC = () => {
  const electronAPI = getElectronAPI();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);
  
  const [contacts, setContacts] = useState<ContactUser[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  // Mock contact data for demonstration
  const mockContacts: ContactUser[] = [
    { userId: 1, username: '张三', email: 'zhangsan@example.com', phoneNumber: '13800138001' },
    { userId: 2, username: '李四', email: 'lisi@example.com', phoneNumber: '13800138002' },
    { userId: 3, username: '王五', email: 'wangwu@example.com', phoneNumber: '13800138003' },
    { userId: 4, username: '赵六', email: 'zhaoliu@example.com', phoneNumber: '13800138004' }
  ];

  useEffect(() => {
    // Load contacts
    setContacts(mockContacts);
  }, []);

  const filteredContacts = contacts.filter(contact =>
    contact.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
    contact.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleContactSelect = (contact: ContactUser) => {
    // Handle contact selection - could open chat or video call
    console.log('Selected contact:', contact);
  };

  return (
    <div className="contacts-screen">
      {/* Search Box */}
      <div className="search-container">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            className="search-input"
            placeholder="搜索联系人..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Contacts List */}
      <div className="contacts-list">
        {filteredContacts.map((contact) => (
          <div key={contact.userId} className="contact-item" onClick={() => handleContactSelect(contact)}>
            <div className="avatar-container">
              <div className="user-avatar">
                {contact.username.charAt(0)}
              </div>
            </div>
            
            <div className="contact-info">
              <h3 className="contact-name">{contact.username}</h3>
              <p className="contact-email">{contact.email}</p>
            </div>

            <div className="contact-actions">
              <button className="action-button chat-btn" title="发送消息">
                💬
              </button>
              <button className="action-button video-call-btn" title="视频通话">
                📹
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ContactsScreen;
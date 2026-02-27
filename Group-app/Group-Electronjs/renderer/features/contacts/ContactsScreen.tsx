import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { AppDispatch, RootState } from '../../store';
import { ApiUser } from '../../types';
import ContactsList from './ContactsList';
import { createPrivateChat } from '../chat/chatSlice';
import './ContactsScreen.css';

type ContactCategory = 'organization' | 'groups' | 'favorites' | 'online';

interface ContactsScreenProps {
  onStartChat?: () => void;
}

const ContactsScreen: React.FC<ContactsScreenProps> = ({ onStartChat }) => {
  const dispatch = useDispatch<AppDispatch>();
  const currentUser = useSelector((state: RootState) => state.auth.user);
  const [activeCategory, setActiveCategory] = useState<ContactCategory>('organization');
  const [selectedUser, setSelectedUser] = useState<ApiUser | null>(null);

  const categories = [
    {
      id: 'organization', name: 'Organization', icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
          <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
        </svg>
      )
    },
    {
      id: 'groups', name: 'Groups', icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
      )
    },
    {
      id: 'favorites', name: 'Favorites', icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
        </svg>
      )
    },
    {
      id: 'online', name: 'Online Users', icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
      )
    }
  ];

  const renderMainContent = () => {
    if (selectedUser) {
      return (
        <div className="contact-detail-view">
          <div className="detail-avatar-large">
            {selectedUser.username.charAt(0).toUpperCase()}
          </div>
          <h2 className="detail-name">{selectedUser.username}</h2>
          <div className="detail-status">
            <span className="status-dot"></span>
            Online
          </div>

          <div className="detail-info-grid">
            <div className="info-item">
              <span className="info-label">Email Address</span>
              <span className="info-value">{selectedUser.email || 'Not available'}</span>
            </div>
            <div className="info-item">
              <span className="info-label">Phone Number</span>
              <span className="info-value">{selectedUser.phoneNumber || 'Not available'}</span>
            </div>
            <div className="info-item">
              <span className="info-label">Department</span>
              <span className="info-value">Product Design</span>
            </div>
          </div>

          <div className="detail-actions">
            <button
              className="primary-action-btn"
              onClick={async () => {
                if (!currentUser || !selectedUser) return;
                try {
                  await dispatch(createPrivateChat({
                    userId: currentUser.userId.toString(),
                    friendId: selectedUser.userId
                  })).unwrap();
                  if (onStartChat) onStartChat();
                } catch (e) {
                  console.error('Failed to start chat:', e);
                }
              }}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
              Start Chat
            </button>
            <button className="secondary-action-btn">Video Call</button>
          </div>

          <button
            className="secondary-action-btn"
            style={{ marginTop: '24px' }}
            onClick={() => setSelectedUser(null)}
          >
            Back to List
          </button>
        </div>
      );
    }

    switch (activeCategory) {
      case 'organization':
        return <ContactsList onSelectUser={(user) => setSelectedUser(user)} />;
      default:
        return (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#94a3b8' }}>
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <p style={{ marginTop: '16px', fontSize: '18px' }}>Coming Soon</p>
            <p style={{ fontSize: '14px' }}>This category is under development.</p>
          </div>
        );
    }
  };

  return (
    <div className="contacts-screen">
      <div className="contacts-sidebar">
        <div className="sidebar-category">
          <h3>Directory</h3>
          {categories.map((cat) => (
            <div
              key={cat.id}
              className={`sidebar-item ${activeCategory === cat.id ? 'active' : ''}`}
              onClick={() => {
                setActiveCategory(cat.id as ContactCategory);
                setSelectedUser(null);
              }}
            >
              <span className="sidebar-item-icon">{cat.icon}</span>
              {cat.name}
            </div>
          ))}
        </div>

        <div className="sidebar-category">
          <h3>Personal</h3>
          <div className="sidebar-item">
            <span className="sidebar-item-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="8.5" cy="7" r="4"></circle>
                <polyline points="17 11 19 13 23 9"></polyline>
              </svg>
            </span>
            Friend Requests
          </div>
          <div className="sidebar-item">
            <span className="sidebar-item-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </span>
            Blacklist
          </div>
        </div>
      </div>

      <div className="contacts-main-content">
        {renderMainContent()}
      </div>
    </div>
  );
};

export default ContactsScreen;
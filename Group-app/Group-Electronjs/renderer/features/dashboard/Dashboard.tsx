import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../auth/authSlice';
import { getElectronAPI } from '../../services/api/electronAPI';
import type { User, ApiUser, ActiveTab } from '../../types';
import { RootState, AppDispatch } from '../../store';
import { webRTCService } from '../../services/WebRTCService';
import { VideoCallStatus } from '../video-call/videoCallSlice';
import VideoCallScreen from '../video-call/VideoCallScreen';
import ChatList from '../chat/ChatList';
import ChatRoom from '../chat/ChatRoom';
import ContactsList from '../contacts/ContactsList';
import ContactsScreen from '../contacts/ContactsScreen';
import AdminPanel from '../admin/AdminPanel';
import { setCurrentCompany, loginSuccess, loginFailure, loginStart } from '../auth/authSlice';
import './Dashboard.css';
import { useVideoCall } from '../video-call/useVideoCall';

interface DashboardProps {
    user: any;
}

const Dashboard: React.FC<DashboardProps> = ({ user }) => {
    const dispatch = useDispatch();
    const [activeTab, setActiveTab] = useState<string>('home');
    const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<User[]>([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSwitchingCompany, setIsSwitchingCompany] = useState(false);
    
    // Connect to Video Call Service
    const { state: callState } = useVideoCall();

    const { activeConversationId, conversations } = useSelector((state: RootState) => state.chat);
    const activeConversation = conversations.find(c => c.conversation.conversationId === activeConversationId)?.conversation;

    const electronAPI = getElectronAPI();

    const handleLogout = () => {
        dispatch(logout());
    };

    const handleSearch = async (query: string) => {
        setSearchQuery(query);
        if (!query.trim() || !electronAPI) {
            setSearchResults([]);
            return;
        }

        setIsSearching(true);
        try {
            // Small delay to prevent flickering and simulate network request properly
            const result = await electronAPI.searchUsers(query);
            let users: User[] = [];

            if (result.success && result.data) {
                const dataObj = result.data as any;
                if (dataObj.users && Array.isArray(dataObj.users)) {
                    users = dataObj.users.map((u: any) => ({
                        id: u.userId?.toString() || u.id || '',
                        username: u.username || '',
                        email: u.email || '',
                        phoneNumber: u.phoneNumber || '',
                        status: u.status || 'online'
                    })) as User[];
                } else if (dataObj.content && Array.isArray(dataObj.content)) {
                    users = (dataObj.content as ApiUser[]).map(apiUser => ({
                        id: apiUser.userId.toString(),
                        username: apiUser.username,
                        email: apiUser.email || '',
                        phoneNumber: apiUser.phoneNumber || '',
                        status: 'online'
                    }));
                }
            }
            setSearchResults(users);
        } catch (error) {
            console.error('Search error:', error);
        } finally {
            setIsSearching(false);
        }
    };

    const handleCall = (targetUserId: string, targetUserName?: string) => {
        console.log('Initiating call to:', targetUserId);
        webRTCService.initiateCall(targetUserId);
    };

    const handleSwitchCompany = async (company: any) => {
        if (!electronAPI || isSwitchingCompany) return;
        if (user.currentCompany?.companyId === company.companyId) return;

        setIsSwitchingCompany(true);
        dispatch(loginStart());

        try {
            // In a real scenario, we might want to call an API to get a new token for the company
            // Or if the backend uses the token to identify the user and then looks up the company
            // we just need to update the client-side context.
            
            // For now, we update the Redux state which persisted to localStorage
            dispatch(setCurrentCompany(company));
            
            // Simulate a small delay for the "switching" feel
            await new Promise(resolve => setTimeout(resolve, 800));
            
            // Refresh to apply new company context across all services
            window.location.reload(); 
        } catch (error) {
            dispatch(loginFailure('切换公司失败'));
            setIsSwitchingCompany(false);
        }
    };

    // Connect to signaling on mount
    useEffect(() => {
        if (user?.userId) {
            // Use host from window location or env
            const host = window.location.hostname || 'localhost';
            const port = 8080; // Signaling port
            const token = localStorage.getItem('token') || '';
            webRTCService.connectSignaling(host, port, user.userId, token);
        }
    }, [user?.userId]);

    // Debounce search
    useEffect(() => {
        const timer = setTimeout(() => {
            if (searchQuery) handleSearch(searchQuery);
        }, 300);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    return (
        <div className={`dashboard-container ${isSidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
            {/* Sidebar */}
            <div
                className="dashboard-sidebar"
                onMouseEnter={() => setIsSidebarCollapsed(false)}
                onMouseLeave={() => setIsSidebarCollapsed(true)}
            >

                <div className="user-profile-section">
                    <div className="user-avatar" onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}>
                        {user?.username?.charAt(0).toUpperCase() || 'U'}
                    </div>
                    {!isSidebarCollapsed && (
                        <div className="user-info">
                            <div className="user-name" title={user?.username}>{user?.username || 'User'}</div>
                            <div className="company-switcher-container">
                                <div className="company-active-badge" onClick={(e) => {
                                    const dropdown = e.currentTarget.nextElementSibling;
                                    dropdown?.classList.toggle('show');
                                }}>
                                    <span>{user.currentCompany?.name || '选择公司'}</span>
                                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" strokeWidth="3">
                                        <polyline points="6 9 12 15 18 9"></polyline>
                                    </svg>
                                </div>
                                <div className="company-dropdown-list">
                                    {user.companies?.map((c: any) => (
                                        <div 
                                            key={c.companyId} 
                                            className={`company-option ${user.currentCompany?.companyId === c.companyId ? 'active' : ''}`}
                                            onClick={() => handleSwitchCompany(c)}
                                        >
                                            <div className="company-icon">{c.name.charAt(0)}</div>
                                            <div className="company-details">
                                                <div className="name">{c.name}</div>
                                                <div className="code">{c.code}</div>
                                            </div>
                                            {user.currentCompany?.companyId === c.companyId && (
                                                <div className="check-icon">
                                                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="3">
                                                        <polyline points="20 6 9 17 4 12"></polyline>
                                                    </svg>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                <div className="sidebar-nav">
                    <div
                        className={`nav-item ${activeTab === 'home' ? 'active' : ''}`}
                        onClick={() => setActiveTab('home')}
                        title={isSidebarCollapsed ? 'Home' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                            <polyline points="9 22 9 12 15 12 15 22"></polyline>
                        </svg>
                        {!isSidebarCollapsed && <span>Home</span>}
                    </div>
                    <div
                        className={`nav-item ${activeTab === 'chats' ? 'active' : ''}`}
                        onClick={() => setActiveTab('chats')}
                        title={isSidebarCollapsed ? 'Chats' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                        </svg>
                        {!isSidebarCollapsed && <span>Chats</span>}
                    </div>
                    <div
                        className={`nav-item ${activeTab === 'contacts' ? 'active' : ''}`}
                        onClick={() => setActiveTab('contacts')}
                        title={isSidebarCollapsed ? 'Contacts' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                        </svg>
                        {!isSidebarCollapsed && <span>Contacts</span>}
                    </div>
                    <div
                        className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
                        onClick={() => setActiveTab('settings')}
                        title={isSidebarCollapsed ? 'Settings' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="3"></circle>
                            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                        </svg>
                        {!isSidebarCollapsed && <span>Settings</span>}
                    </div>
                    {user?.username === 'admin' && (
                        <div
                            className={`nav-item ${activeTab === 'admin' ? 'active' : ''}`}
                            onClick={() => setActiveTab('admin')}
                            title={isSidebarCollapsed ? 'Admin' : ''}
                        >
                            <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                            </svg>
                            {!isSidebarCollapsed && <span>Admin</span>}
                        </div>
                    )}
                </div>

                <div className="sidebar-footer">
                    <button className="logout-button" onClick={handleLogout} title={isSidebarCollapsed ? 'Logout' : ''}>
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                            <polyline points="16 17 21 12 16 7"></polyline>
                            <line x1="21" y1="12" x2="9" y2="12"></line>
                        </svg>
                        {!isSidebarCollapsed && <span>Log Out</span>}
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="dashboard-main">
                {/* Header - Only show for home or settings */}
                {(activeTab === 'home' || activeTab === 'settings') && (
                    <div className="dashboard-header">
                        <div className="header-title">
                            <h1>Group IM Workspace</h1>
                        </div>
                        <div className="header-actions">
                            <button className="action-btn" title="Notifications">
                                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                                </svg>
                            </button>
                            <button className="action-btn" title="Create Group">
                                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                            </button>
                        </div>
                    </div>
                )}

                {/* Content View */}
                <div className="content-view">
                    {activeTab === 'home' && (
                        <div className="home-view-container">
                            <div className="welcome-hero-banner">
                                <div className="banner-content">
                                    <h2>Welcome back, {user?.username}</h2>
                                    <p>Search for colleagues, start a video call, or create a group chat directly from your workspace.</p>
                                </div>
                                <div className="banner-icon">
                                    <svg viewBox="0 0 24 24" width="60" height="60" fill="white">
                                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="9" cy="7" r="4"></circle>
                                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                    </svg>
                                </div>
                            </div>

                            <div className="search-section-home">
                                <h3>Quick Search</h3>
                                <div className="premium-search-bar">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                    <input
                                        type="text"
                                        placeholder="Find people..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                    />
                                </div>

                                {/* Search Results Dropdown */}
                                {(searchResults.length > 0) && (
                                    <div className="results-list">
                                        {searchResults.map(result => (
                                            <div className="result-item" key={'id' in result ? result.id : (result as any).userId}>
                                                <div className="result-avatar">
                                                    {result.username.charAt(0).toUpperCase()}
                                                </div>
                                                <div className="result-info">
                                                    <div className="result-name">{result.username}</div>
                                                    <div className="result-detail">{result.email}</div>
                                                </div>
                                                <button
                                                    className="call-action-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        const userId = 'id' in result ? result.id : (result as any).userId.toString();
                                                        handleCall(userId);
                                                    }}
                                                    title="Start Video Call"
                                                >
                                                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                        <polygon points="23 7 16 12 23 17 23 7"></polygon>
                                                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                                                    </svg>
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'chats' && (
                        <div className="chats-view-container">
                            <div className="chats-list-sidebar">
                                <ChatList onVideoCallStart={handleCall} />
                            </div>
                            <div className="chat-room-area">
                                {activeConversation ? (
                                    <ChatRoom
                                        conversation={activeConversation}
                                    />
                                ) : (
                                    <div className="empty-view-placeholder">
                                        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                                        </svg>
                                        <h3>No conversation selected</h3>
                                        <p>Pick a colleague from the list to start messaging.</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'contacts' && (
                        <div className="contacts-view-container">
                            <ContactsScreen />
                        </div>
                    )}

                    {activeTab === 'settings' && (
                        <div className="empty-view-placeholder">
                            <div style={{ textAlign: 'center' }}>
                                <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                    <circle cx="12" cy="12" r="3"></circle>
                                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                                </svg>
                                <h3>Settings Coming Soon</h3>
                                <p>We're working on making this space customizable.</p>
                            </div>
                        </div>
                    )}

                    {activeTab === 'admin' && (
                        <div className="admin-view-container" style={{ height: '100%', overflow: 'hidden' }}>
                            <AdminPanel />
                        </div>
                    )}
                </div>
            </div>

            {/* Switching Overlay */}
            {isSwitchingCompany && (
                <div className="switching-overlay">
                    <div className="switching-content">
                        <div className="switching-loader"></div>
                        <h3>Switching Workspace...</h3>
                        <p>Preparing your environment for {user.currentCompany?.name}</p>
                    </div>
                </div>
            )}

            {/* Global Video Call Overlay */}
            {callState.callStatus !== VideoCallStatus.IDLE && (
                <VideoCallScreen 
                    remoteUserId={callState.remoteUserId} 
                    onCallEnd={() => {
                        // The service handles cleanup, we just need the UI to hide
                    }} 
                />
            )}
        </div>
    );
};

export default Dashboard;

import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { logout, setCompanies, loginSuccess, loginFailure, loginStart, setCurrentCompany } from '../auth/authSlice';
import { getElectronAPI, isElectronEnvironment } from '../../services/api/electronAPI';
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
import { createPrivateChat } from '../chat/chatSlice';
import { authAPI } from '../../services/api/apiClient';
import './Dashboard.css';
import { useVideoCall } from '../video-call/useVideoCall';
import { meetingAPI } from '../../services/api/apiClient';

const Dashboard: React.FC = () => {
    const dispatch = useDispatch<AppDispatch>();
    const { user } = useSelector((state: RootState) => state.auth);
    const fetchInitiated = React.useRef(false);
    const [activeTab, setActiveTab] = useState<ActiveTab>('home');
    const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<User[]>([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSwitchingCompany, setIsSwitchingCompany] = useState(false);
    const [showWorkspacePopover, setShowWorkspacePopover] = useState(false);

    // DEBUG: Monitor Auth State
    useEffect(() => {
        console.log('[DEBUG-DASHBOARD] User State Updated:', {
            activeUserId: user?.userId,
            username: user?.username,
            currentCompany: user?.currentCompany,
            companiesCount: user?.companies?.length,
            companies: user?.companies
        });
    }, [user]);
    // Connect to Video Call Service
    const { state: callState, startMeeting, joinMeeting } = useVideoCall();

    const { activeConversationId, conversations } = useSelector((state: RootState) => state.chat);
    const activeConversation = conversations.find(c => c.conversation.conversationId === activeConversationId)?.conversation;

    const electronAPI = getElectronAPI();

    const getSignalingConfig = () => {
        const fallbackHost = window.location.hostname || 'localhost';
        const fallbackPort = window.location.port
            ? Number(window.location.port)
            : (window.location.protocol === 'https:' ? 443 : 80);
        const fallbackProtocol = window.location.protocol || 'http:';

        if (!isElectronEnvironment()) {
            return { host: fallbackHost, port: fallbackPort, protocol: fallbackProtocol };
        }

        if (!__SIGNAL_BASE__) {
            return { host: fallbackHost, port: fallbackPort, protocol: fallbackProtocol };
        }

        try {
            const url = new URL(__SIGNAL_BASE__);
            const isHttps = url.protocol === 'https:';
            return {
                host: url.host,
                port: url.port ? Number(url.port) : (isHttps ? 443 : 80),
                protocol: url.protocol
            };
        } catch (error) {
            console.warn('Invalid __SIGNAL_BASE__, falling back to window.location:', __SIGNAL_BASE__, error);
            return { host: fallbackHost, port: fallbackPort, protocol: fallbackProtocol };
        }
    };

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
        webRTCService.initiateCall(targetUserId, targetUserName);
    };

    const handleStartMeeting = (participants: Array<{ userId: string; userName?: string }>, roomId?: string) => {
        if (!participants.length) return;
        const finalRoomId = roomId || (activeConversation ? `meeting_${activeConversation.conversationId}` : undefined);
        startMeeting(participants, finalRoomId);
    };

    const handleJoinMeeting = async (roomId: string) => {
        try {
            await meetingAPI.join(roomId);
        } catch (error) {
            console.error('Failed to join meeting:', error);
        }
        joinMeeting(roomId);
    };

    const handleStartMessage = async (targetUserId: string) => {
        if (!user?.userId) return;
        try {
            await dispatch(createPrivateChat({
                userId: user.userId.toString(),
                friendId: Number(targetUserId)
            })).unwrap();
            setActiveTab('chats');
        } catch (err) {
            console.error('Failed to start chat from search:', err);
        }
    };

    const handleSwitchCompany = async (company: any) => {
        if (isSwitchingCompany || !user) return;
        if (user.currentCompany?.companyId === company.companyId) {
            setShowWorkspacePopover(false);
            return;
        }

        setIsSwitchingCompany(true);
        setShowWorkspacePopover(false);

        try {
            const response = await authAPI.switchCompany(company.companyId);

            if (response.data && response.data.success) {
                const refreshedUser = response.data.data;

                dispatch(loginSuccess({
                    user: refreshedUser,
                    token: refreshedUser.token || user?.token || '',
                    refreshToken: refreshedUser.refreshToken || user?.refreshToken || '',
                    companies: user?.companies,
                    currentCompany: company
                }));

                // Force reload to refresh all data for new company context
                setTimeout(() => {
                    window.location.reload();
                }, 500);
            } else {
                throw new Error(response.data?.message || 'Switch failed');
            }
        } catch (error: any) {
            console.error('Failed to switch company:', error);
            dispatch(loginFailure(error.message || '切换公司失败'));
            setIsSwitchingCompany(false);
        }
    };

    // Connect to signaling on mount
    useEffect(() => {
        if (user?.userId) {
            const { host, port, protocol } = getSignalingConfig();
            const token = localStorage.getItem('token') || '';
            webRTCService.connectSignaling(host, port, user.userId, token, protocol);
        }
    }, [user?.userId]);

    // Fetch companies if missing and handle current company initialization
    useEffect(() => {
        const fetchCompanies = async () => {
            if (!user?.userId || fetchInitiated.current) return;

            // Only fetch if companies list is empty
            if (!user.companies || user.companies.length === 0) {
                fetchInitiated.current = true;
                try {
                    console.log('[DEBUG-DASHBOARD] Fetching companies for user:', user.username);
                    const response = await authAPI.getMyCompanies();
                    console.log('[DEBUG-DASHBOARD] getMyCompanies Response:', response.data);

                    if (response.data && response.data.code == 200) {
                        const companies = response.data.data;
                        console.log('[DEBUG-DASHBOARD] Setting companies:', companies);
                        dispatch(setCompanies(companies));

                        // If current company not set, try to find it from the list using ID
                        if (!user.currentCompany) {
                            const targetCompanyId = user.currentLoginCompanyId;
                            console.log('[DEBUG-DASHBOARD] Auto-selecting company. Target ID:', targetCompanyId);
                            let current = null;

                            if (targetCompanyId) {
                                // Use flexible comparison (double equals) in case of string/number mismatch
                                current = companies.find((c: any) => c.companyId == targetCompanyId);
                            }

                            // Fallback to first company if no match or no ID provided
                            if (!current && companies.length > 0) {
                                current = companies[0];
                            }

                            if (current) {
                                console.log('[DEBUG-DASHBOARD] Setting current company:', current);
                                dispatch(setCurrentCompany(current));
                            } else {
                                console.warn('[DEBUG-DASHBOARD] No company could be selected as current');
                            }
                        }
                    } else {
                        console.error('[DEBUG-DASHBOARD] API Success was false:', response.data);
                    }
                } catch (err) {
                    console.error('[DEBUG-DASHBOARD] Failed to fetch companies error:', err);
                } finally {
                    fetchInitiated.current = false;
                }
            }
        };
        fetchCompanies();
    }, [user?.userId, user?.currentLoginCompanyId]);

    // Debounce search
    useEffect(() => {
        const timer = setTimeout(() => {
            if (searchQuery) handleSearch(searchQuery);
        }, 300);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    return (
        <div className="dashboard-container">
            <div className="dashboard-background">
                <div className="sphere sphere-1"></div>
                <div className="sphere sphere-2"></div>
            </div>

            {/* Main Navigation Sidebar */}
            <div
                className={`dashboard-sidebar ${isSidebarCollapsed ? 'sidebar-collapsed' : ''}`}
                onMouseEnter={() => setIsSidebarCollapsed(false)}
                onMouseLeave={() => {
                    if (!showWorkspacePopover) {
                        setIsSidebarCollapsed(true);
                    }
                }}
            >
                <div className="sidebar-app-title">
                    <div className="app-logo">G</div>
                    {!isSidebarCollapsed && <h2>Group IM</h2>}
                </div>

                {/* Workspace Switcher Component */}
                <div className="workspace-switcher-container">
                    <div
                        className={`workspace-current ${fetchInitiated.current ? 'loading' : ''}`}
                        onClick={() => setShowWorkspacePopover(!showWorkspacePopover)}
                    >
                        <div className="workspace-icon">
                            {user?.currentCompany?.name ? user.currentCompany.name.charAt(0) : (user?.username ? user.username.charAt(0).toUpperCase() : 'W')}
                            {fetchInitiated.current && <div className="icon-loader"></div>}
                        </div>
                        <div className="workspace-info">
                            <span className="workspace-name">{user?.currentCompany?.name || 'My Workspace'}</span>
                            <span className="workspace-type">
                                {user?.companies && user.companies.length > 0
                                    ? `${user.companies.length} Workplaces Available`
                                    : (fetchInitiated.current ? 'Loading workspaces...' : 'Personal Workspace')}
                            </span>
                        </div>
                        <svg className="workspace-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ transform: showWorkspacePopover ? 'rotate(180deg)' : 'rotate(0deg)' }}>
                            <polyline points="6 9 12 15 18 9"></polyline>
                        </svg>
                    </div>

                    {showWorkspacePopover && !isSidebarCollapsed && (
                        <div className="workspace-popover">
                            <div className="popover-section">
                                <div className="popover-header">
                                    <span>Current Workplace</span>
                                </div>
                                {user?.currentCompany && (
                                    <div className="company-option active current-selection">
                                        <div className="opt-icon">{user.currentCompany.name.charAt(0)}</div>
                                        <div className="opt-info">
                                            <span className="opt-name">{user.currentCompany.name}</span>
                                            <span className="opt-status">Active now</span>
                                        </div>
                                        <div className="active-dot"></div>
                                    </div>
                                )}
                            </div>

                            <div className="popover-divider"></div>

                            <div className="popover-section">
                                <div className="popover-header">
                                    <span>Switch to Workplace</span>
                                </div>
                                <div className="workspace-list premium-scrollbar">
                                    {user?.companies?.filter((c: any) => c.companyId !== user.currentCompany?.companyId).map((c: any) => (
                                        <div
                                            key={c.companyId}
                                            className="company-option workplace-switch-btn"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleSwitchCompany(c);
                                            }}
                                        >
                                            <div className="opt-icon">{c.name.charAt(0)}</div>
                                            <div className="opt-info">
                                                <span className="opt-name">{c.name}</span>
                                            </div>
                                            <div className="switch-hint">Switch</div>
                                        </div>
                                    ))}

                                    {(!user?.companies || user.companies.length <= 1) && !fetchInitiated.current && (
                                        <div className="empty-workspace-hint">
                                            No other workspaces found
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="popover-footer">
                                <div className="company-option add-workspace">
                                    <div className="opt-icon">+</div>
                                    <div className="opt-info">
                                        <span className="opt-name">Add New Workplace</span>
                                    </div>
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
                        <span>Home</span>
                    </div>
                    <div
                        className={`nav-item ${activeTab === 'chats' ? 'active' : ''}`}
                        onClick={() => setActiveTab('chats')}
                        title={isSidebarCollapsed ? 'Chats' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                        </svg>
                        <span>Chats</span>
                    </div>
                    <div
                        className={`nav-item ${activeTab === 'meetings' ? 'active' : ''}`}
                        onClick={() => setActiveTab('meetings')}
                        title={isSidebarCollapsed ? 'Meetings' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polygon points="23 7 16 12 23 17 23 7"></polygon>
                            <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                        </svg>
                        <span>Meetings</span>
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
                        <span>Contacts</span>
                    </div>
                    <div
                        className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
                        onClick={() => setActiveTab('settings')}
                        title={isSidebarCollapsed ? 'Settings' : ''}
                    >
                        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="3"></circle>
                            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                        </svg>
                        <span>Settings</span>
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
                            <span>Admin</span>
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
                        <span>Log Out</span>
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
                                    <h2>Welcome back, {user?.username || 'User'}</h2>
                                    <p>Search for colleagues, start a video call, or create a group chat directly from your workspace.</p>
                                </div>
                                <div className="banner-icon">
                                    <svg viewBox="0 0 24 24" width="60" height="60" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="12" cy="7" r="4"></circle>
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
                                                <div className="result-actions">
                                                    <button
                                                        className="message-action-btn"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            const userId = 'id' in result ? result.id : (result as any).userId.toString();
                                                            handleStartMessage(userId);
                                                        }}
                                                        title="Send Message"
                                                    >
                                                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                                                        </svg>
                                                    </button>
                                                    <button
                                                        className="call-action-btn"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            const userId = 'id' in result ? result.id : (result as any).userId.toString();
                                                            const userName = result.username || '';
                                                            handleCall(userId, userName);
                                                        }}
                                                        title="Start Video Call"
                                                    >
                                                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <polygon points="23 7 16 12 23 17 23 7"></polygon>
                                                            <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                                                        </svg>
                                                    </button>
                                                </div>
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
                                        onStartMeeting={handleStartMeeting}
                                        onJoinMeeting={handleJoinMeeting}
                                        onVideoCall={handleCall}
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

                    {activeTab === 'meetings' && (
                        <div className="meetings-view-container">
                            <div className="empty-view-placeholder">
                                <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                    <polygon points="23 7 16 12 23 17 23 7"></polygon>
                                    <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                                </svg>
                                <h3>Team Meetings</h3>
                                <p>Join or start a multi-party video conference with your team.</p>
                                <button
                                    className="premium-action-btn"
                                    style={{ marginTop: '20px' }}
                                    onClick={() => setActiveTab('chats')}
                                >
                                    Start from Chat
                                </button>
                            </div>
                        </div>
                    )}

                    {activeTab === 'contacts' && (
                        <div className="contacts-view-container">
                            <ContactsScreen onStartChat={() => setActiveTab('chats')} />
                        </div>
                    )}

                    {activeTab === 'settings' && (
                        <div className="empty-view-placeholder">
                            <div style={{ textAlign: 'center' }}>
                                <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                    <circle cx="12" cy="12" r="3"></circle>
                                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
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
                        <p>Preparing your environment for {user?.currentCompany?.name}</p>
                    </div>
                </div>
            )}

            {/* Global Video Call Overlay */}
            {callState.callStatus !== VideoCallStatus.IDLE && (
                <VideoCallScreen
                    remoteUserId={callState.remoteUserId} remoteUserName={callState.remoteUserName} remoteAvatar={callState.remoteAvatar}
                    onCallEnd={() => {
                        // The service handles cleanup
                    }}
                />
            )}
        </div>
    );
};

export default Dashboard;

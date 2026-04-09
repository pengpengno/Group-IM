import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../../store';
import { fetchConversations, setActiveConversation, createGroupConversation, addConversationMembers } from './chatSlice';
import { fetchOrgStructure } from '../contacts/contactsSlice';
import { ConversationDisplayState, ApiUser, OrgTreeNode, ConversationRes } from '../../types';
import './ChatList.css';

interface ChatListProps {
  onVideoCallStart?: (userId: string, userName?: string) => void;
}

type GroupModalMode = 'create' | 'add-members' | null;

const flattenUsers = (nodes: OrgTreeNode[]): ApiUser[] => {
  const users: ApiUser[] = [];
  const walk = (items: OrgTreeNode[]) => {
    items.forEach((item) => {
      if (item.type === 'USER' && item.userInfo) {
        users.push(item.userInfo);
      }
      if (item.children?.length) {
        walk(item.children);
      }
    });
  };
  walk(nodes);

  const unique = new Map<number, ApiUser>();
  users.forEach((user) => unique.set(user.userId, user));
  return Array.from(unique.values());
};

const ChatList: React.FC<ChatListProps> = ({ onVideoCallStart }) => {
  const dispatch = useDispatch<AppDispatch>();
  const { conversations, loading, activeConversationId } = useSelector((state: RootState) => state.chat);
  const { user } = useSelector((state: RootState) => state.auth);
  const { orgTree, loading: contactsLoading } = useSelector((state: RootState) => state.contacts);

  const [searchQuery, setSearchQuery] = useState('');
  const [groupModalMode, setGroupModalMode] = useState<GroupModalMode>(null);
  const [groupName, setGroupName] = useState('');
  const [groupDescription, setGroupDescription] = useState('');
  const [memberSearch, setMemberSearch] = useState('');
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (user?.userId) {
      dispatch(fetchConversations(user.userId));
    }
  }, [dispatch, user]);

  const activeConversation = useMemo(
    () => conversations.find((item) => item.conversation.conversationId === activeConversationId)?.conversation,
    [conversations, activeConversationId]
  );

  const availableContacts = useMemo(() => {
    const currentUserId = Number(user?.userId);
    const currentGroupMemberIds = new Set(
      (Array.isArray(activeConversation?.members) ? activeConversation.members : []).map((member) => member.userId)
    );

    return flattenUsers(orgTree)
      .filter((contact) => contact.userId !== currentUserId)
      .filter((contact) => groupModalMode !== 'add-members' || !currentGroupMemberIds.has(contact.userId))
      .filter((contact) => {
        if (!memberSearch.trim()) {
          return true;
        }
        const keyword = memberSearch.trim().toLowerCase();
        return contact.username.toLowerCase().includes(keyword)
          || (contact.email || '').toLowerCase().includes(keyword)
          || (contact.phoneNumber || '').toLowerCase().includes(keyword);
      });
  }, [orgTree, user?.userId, activeConversation?.members, groupModalMode, memberSearch]);

  const handleSelectConversation = (id: number) => {
    dispatch(setActiveConversation(id));
  };

  const openGroupModal = async (mode: Exclude<GroupModalMode, null>) => {
    setGroupModalMode(mode);
    setSelectedMemberIds([]);
    setMemberSearch('');
    if (mode === 'create') {
      setGroupName('');
      setGroupDescription('');
    }
    if (!orgTree.length) {
      await dispatch(fetchOrgStructure());
    }
  };

  const closeGroupModal = () => {
    setGroupModalMode(null);
    setSelectedMemberIds([]);
    setMemberSearch('');
    setSubmitting(false);
  };

  const toggleMember = (memberId: number) => {
    setSelectedMemberIds((prev) => prev.includes(memberId)
      ? prev.filter((id) => id !== memberId)
      : [...prev, memberId]);
  };

  const handleSubmitGroupAction = async () => {
    if (!user?.userId || selectedMemberIds.length === 0) {
      return;
    }

    setSubmitting(true);
    try {
      if (groupModalMode === 'create') {
        const members = availableContacts.filter((member) => selectedMemberIds.includes(member.userId));
        await dispatch(createGroupConversation({
          groupName: groupName.trim() || `${user.username}的群聊`,
          description: groupDescription.trim(),
          members
        })).unwrap();
      }

      if (groupModalMode === 'add-members' && activeConversation) {
        await dispatch(addConversationMembers({
          conversationId: activeConversation.conversationId,
          userIds: selectedMemberIds
        })).unwrap();
      }

      closeGroupModal();
    } catch (error) {
      console.error('Failed to update group conversation:', error);
      setSubmitting(false);
    }
  };

  const filteredConversations = conversations.filter(item => {
    const isGroup = item?.conversation.conversationType === 'GROUP';
    // DEBUG: Trace Group Info
    if (isGroup) {
      console.log(`[DEBUG-CHATLIST] Group Conv ${item.conversation.conversationId}:`, {
        groupName: item.conversation.groupName,
        name: (item.conversation as any).name,
        full: item.conversation
      });
    }

    const displayName = isGroup
      ? (item.conversation.groupName || (item.conversation as any).name || '无名群组')
      : (Array.isArray(item.conversation.members) ? item.conversation.members : []).find((m: ApiUser) => m.userId.toString() !== user?.userId)?.username || '未知用户';
    return displayName?.toLowerCase().includes(searchQuery.toLowerCase());
  });

  if (loading && conversations.length === 0) {
    return <div className="chat-list-loading">
      <div className="spinner-small"></div>
      正在同步会话...
    </div>;
  }

  return (
    <>
      <div className="chat-list-container">
        <div className="chat-list-header">
          <div className="header-top">
            <h2>消息</h2>
            <div className="header-actions">
              {activeConversation?.conversationType === 'GROUP' && (
                <button className="icon-btn" title="添加群成员" onClick={() => openGroupModal('add-members')}>
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M19 8h4"></path>
                    <path d="M21 6v4"></path>
                  </svg>
                </button>
              )}
              <button className="icon-btn" title="发起群聊" onClick={() => openGroupModal('create')}>
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 5v14M5 12h14"></path>
                </svg>
              </button>
            </div>
          </div>

          <div className="search-bar">
            <div className="search-input-wrapper">
              <svg className="search-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                type="text"
                placeholder="搜索联系人或群组..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="conversations-scroll">
          {filteredConversations.length === 0 ? (
            <div className="empty-chats">
              <div className="empty-icon">会话</div>
              <p>{searchQuery ? '未找到相关会话' : '暂无最近会话'}</p>
            </div>
          ) : (
            filteredConversations.map((item: ConversationDisplayState) => {
              const isGroup = isGroupConversation(item.conversation);
              const displayName = item.conversation.groupName
                ? (item.conversation.groupName || (item.conversation as any).name || '无名群组')
                : (Array.isArray(item.conversation.members) ? item.conversation.members : []).find((m: ApiUser) => m.userId.toString() !== user?.userId)?.username || '未知用户';

              return (
                <div
                  key={item.conversation.conversationId}
                  className={`chat-item-premium ${activeConversationId === item.conversation.conversationId ? 'active' : ''}`}
                  onClick={() => handleSelectConversation(item.conversation.conversationId)}
                >
                  <div className="chat-item-avatar">
                    {displayName?.charAt(0).toUpperCase()}
                    {!isGroup && <span className="online-status"></span>}
                  </div>

                  <div className="chat-item-info">
                    <div className="chat-item-top">
                      <span className="chat-item-name">{displayName}</span>
                      <span className="chat-item-time">{item.displayDateTime}</span>
                    </div>
                    <div className="chat-item-bottom">
                      <span className="chat-item-msg">{item.lastMessage || '点击开始聊天'}</span>
                      {item.unreadCount > 0 && (
                        <span className="chat-item-badge">{item.unreadCount}</span>
                      )}
                    </div>
                  </div>

                  <div className="chat-item-actions">
                    <button
                      className="quick-call-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!isGroup && onVideoCallStart) {
                          const target = (Array.isArray(item.conversation.members) ? item.conversation.members : []).find((m: ApiUser) => m.userId.toString() !== user?.userId);
                          if (target) onVideoCallStart(target.userId.toString(), target.username);
                        }
                      }}
                    >
                      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                        <polygon points="23 7 16 12 23 17 23 7"></polygon>
                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                      </svg>
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {groupModalMode && (
        <div className="group-modal-overlay" onClick={closeGroupModal}>
          <div className="group-modal" onClick={(e) => e.stopPropagation()}>
            <div className="group-modal-header">
              <div>
                <h3>{groupModalMode === 'create' ? '创建群聊' : '邀请成员入群'}</h3>
                <p>{groupModalMode === 'create' ? '从组织架构中选择成员，立即创建群聊。' : '将联系人加入当前群聊。'}</p>
              </div>
              <button className="modal-close-btn" onClick={closeGroupModal}>×</button>
            </div>

            {groupModalMode === 'create' && (
              <div className="group-form-grid">
                <input
                  className="group-text-input"
                  placeholder="群聊名称"
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                />
                <textarea
                  className="group-textarea"
                  placeholder="群聊描述（可选）"
                  value={groupDescription}
                  onChange={(e) => setGroupDescription(e.target.value)}
                  rows={3}
                />
              </div>
            )}

            <div className="group-member-toolbar">
              <div className="search-input-wrapper member-search-wrapper">
                <svg className="search-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input
                  type="text"
                  placeholder="搜索联系人"
                  value={memberSearch}
                  onChange={(e) => setMemberSearch(e.target.value)}
                />
              </div>
              <span className="member-count">已选 {selectedMemberIds.length} 人</span>
            </div>

            <div className="group-member-list">
              {contactsLoading ? (
                <div className="group-empty-state">正在加载联系人...</div>
              ) : availableContacts.length === 0 ? (
                <div className="group-empty-state">没有可选择的联系人</div>
              ) : availableContacts.map((member) => {
                const checked = selectedMemberIds.includes(member.userId);
                return (
                  <label key={member.userId} className={`group-member-item ${checked ? 'selected' : ''}`}>
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleMember(member.userId)}
                    />
                    <div className="group-member-avatar">{member.username.charAt(0).toUpperCase()}</div>
                    <div className="group-member-info">
                      <span className="group-member-name">{member.username}</span>
                      <span className="group-member-detail">{member.email || member.phoneNumber || `ID: ${member.userId}`}</span>
                    </div>
                  </label>
                );
              })}
            </div>

            <div className="group-modal-footer">
              <button className="group-secondary-btn" onClick={closeGroupModal}>取消</button>
              <button
                className="group-primary-btn"
                onClick={handleSubmitGroupAction}
                disabled={submitting || selectedMemberIds.length === 0 || (groupModalMode === 'create' && !groupName.trim())}
              >
                {submitting ? '处理中...' : groupModalMode === 'create' ? '创建群聊' : '邀请加入'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};


function isGroupConversation(conv: ConversationRes) {
  return conv?.conversationType === 'GROUP';
}

export default ChatList;

import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../../store';
import type { ExtendedUserInfo, OrganizationNode, ContactUser } from '../../types/index';
import { getElectronAPI } from '../../api/electronAPI';
import { companyAPI } from '../../services/api/apiClient';
import './ContactsScreen.css';
import { AuthState} from '../../features/auth/authSlice'

interface ContactsScreenProps {
  onChatStart?: (userId: string) => void;
}

interface DepartmentNode {
  departmentId: number;
  name: string;
  description?: string;
  companyId: number;
  parentId?: number;
  status: boolean;
  children?: DepartmentNode[];
  members?: ContactUser[];
  isExpanded?: boolean;
}

const ContactsScreen: React.FC<ContactsScreenProps> = ({ onChatStart }) => {
  const electronAPI = getElectronAPI();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);
  
  const [organizationTree, setOrganizationTree] = useState<(DepartmentNode | ContactUser)[]>([]);
  const [expandedDepartments, setExpandedDepartments] = useState<Set<number>>(new Set());
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 加载组织架构数据
  const loadOrganizationStructure = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await companyAPI.getOrganizationStructure();
      const structure = response.data;
      
      // 转换数据结构以适应UI显示
      const treeData = convertToTreeStructure(structure);
      setOrganizationTree(treeData);
    } catch (err: any) {
      console.error('Failed to load organization structure:', err);
      setError(err.response?.data?.message || err.message || '加载组织架构失败');
    } finally {
      setIsLoading(false);
    }
  };

  // 转换组织架构数据为树形结构
  const convertToTreeStructure = (structure: any): (DepartmentNode | ContactUser)[] => {
    const result: (DepartmentNode | ContactUser)[] = [];
    
    // 添加根部门
    if (structure.name) {
      const rootNode: DepartmentNode = {
        departmentId: structure.departmentId,
        name: structure.name,
        description: structure.description,
        companyId: structure.companyId,
        status: structure.status,
        children: structure.children || [],
        members: structure.members?.map((member: any) => ({
          userId: member.userId,
          username: member.username,
          email: member.email,
          phoneNumber: member.phoneNumber,
          department: structure.name,
          position: '成员',
          avatarColor: getColorFromString(member.username)
        })) || [],
        isExpanded: true
      };
      result.push(rootNode);
      
      // 递归处理子部门
      if (structure.children) {
        structure.children.forEach((child: any) => {
          processDepartmentNode(child, result, structure.name);
        });
      }
    }
    
    return result;
  };

  // 处理部门节点
  const processDepartmentNode = (node: any, parentArray: any[], parentName: string) => {
    const departmentNode: DepartmentNode = {
      departmentId: node.departmentId,
      name: node.name,
      description: node.description,
      companyId: node.companyId,
      parentId: node.parentId,
      status: node.status,
      children: node.children || [],
      members: node.members?.map((member: any) => ({
        userId: member.userId,
        username: member.username,
        email: member.email,
        phoneNumber: member.phoneNumber,
        department: node.name,
        position: '成员',
        avatarColor: getColorFromString(member.username)
      })) || [],
      isExpanded: false
    };
    
    parentArray.push(departmentNode);
    
    // 处理子部门
    if (node.children) {
      node.children.forEach((child: any) => {
        processDepartmentNode(child, parentArray, node.name);
      });
    }
  };

  // 根据字符串生成颜色
  const getColorFromString = (str: string): string => {
    const colors = ['#1976d2', '#4caf50', '#ff9800', '#9c27b0', '#f44336', '#00bcd4', '#8bc34a'];
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  };

  // 切换部门展开状态
  const toggleDepartment = (departmentId: number) => {
    setExpandedDepartments(prev => {
      const newSet = new Set(prev);
      if (newSet.has(departmentId)) {
        newSet.delete(departmentId);
      } else {
        newSet.add(departmentId);
      }
      return newSet;
    });
  };

  // 过滤显示的数据
  const getFilteredData = (): (DepartmentNode | ContactUser)[] => {
    if (!searchQuery.trim()) {
      return organizationTree;
    }
    
    const filtered: (DepartmentNode | ContactUser)[] = [];
    
    const searchInNode = (node: DepartmentNode | ContactUser): boolean => {
      if ('userId' in node) {
        // 用户节点
        return node.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
               node.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
               (node.department && node.department.toLowerCase().includes(searchQuery.toLowerCase())) ||
               false;
      } else {
        // 部门节点
        const matches = node.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                       (node.description && node.description.toLowerCase().includes(searchQuery.toLowerCase())) ||
                       false;
        
        // 检查子节点
        let hasMatchingChildren = false;
        const children = [...(node.children || []), ...(node.members || [])];
        children.forEach(child => {
          if (searchInNode(child)) {
            hasMatchingChildren = true;
          }
        });
        
        return matches || hasMatchingChildren;
      }
    };
    
    organizationTree.forEach(node => {
      if (searchInNode(node)) {
        filtered.push(node);
      }
    });
    
    return filtered;
  };

  const handleContactSelect = (contact: ContactUser) => {
    console.log('Selected contact:', contact);
  };

  const handleChatStart = (contact: ContactUser) => {
    if (onChatStart) {
      onChatStart(contact.userId.toString());
    }
  };

  const handleVideoCall = (contact: ContactUser) => {
    console.log('Initiating video call with:', contact.username);
  };

  // 组件挂载时加载数据
  useEffect(() => {
    loadOrganizationStructure();
  }, []);

  // 渲染部门节点
  const renderDepartmentNode = (node: DepartmentNode, level: number = 0) => {
    const isExpanded = expandedDepartments.has(node.departmentId);
    
    return (
      <div key={`dept-${node.departmentId}`}>
        <div 
          className="department-item"
          style={{ paddingLeft: `${level * 24 + 16}px` }}
          onClick={() => toggleDepartment(node.departmentId)}
        >
          <div className="department-header">
            <span className="expand-icon">
              {isExpanded ? '▼' : '▶'}
            </span>
            <div className="department-icon">
              🏢
            </div>
            <div className="department-info">
              <h3 className="department-name">{node.name}</h3>
              {node.description && (
                <p className="department-description">{node.description}</p>
              )}
            </div>
          </div>
        </div>
        
        {isExpanded && (
          <>
            {/* 渲染成员 */}
            {node.members?.map(member => (
              <div 
                key={`user-${member.userId}`} 
                className="contact-item"
                style={{ paddingLeft: `${level * 24 + 40}px` }}
                onClick={() => handleContactSelect(member)}
              >
                <div className="avatar-container">
                  <div 
                    className="user-avatar"
                    style={{ backgroundColor: member.avatarColor || '#1976d2' }}
                  >
                    {member.username.charAt(0)}
                  </div>
                </div>
                
                <div className="contact-info">
                  <h3 className="contact-name">{member.username}</h3>
                  <p className="contact-email">{member.email}</p>
                  {member.department && member.position && (
                    <p className="contact-position">
                      {member.department} · {member.position}
                    </p>
                  )}
                </div>

                <div className="contact-actions">
                  <button 
                    className="action-button chat-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleChatStart(member);
                    }}
                    title="发送消息"
                  >
                    💬
                  </button>
                  <button 
                    className="action-button video-call-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleVideoCall(member);
                    }}
                    title="视频通话"
                  >
                    📹
                  </button>
                </div>
              </div>
            ))}
            
            {/* 渲染子部门 */}
            {node.children?.map(child => renderDepartmentNode(child, level + 1))}
          </>
        )}
      </div>
    );
  };

  // 渲染用户节点
  const renderUserNode = (node: ContactUser) => {
    return (
      <div key={`user-${node.userId}`} className="contact-item">
        <div className="avatar-container">
          <div 
            className="user-avatar"
            style={{ backgroundColor: node.avatarColor || '#1976d2' }}
          >
            {node.username.charAt(0)}
          </div>
        </div>
        
        <div className="contact-info">
          <h3 className="contact-name">{node.username}</h3>
          <p className="contact-email">{node.email}</p>
          {node.department && node.position && (
            <p className="contact-position">
              {node.department} · {node.position}
            </p>
          )}
        </div>

        <div className="contact-actions">
          <button 
            className="action-button chat-btn"
            onClick={(e) => {
              e.stopPropagation();
              handleChatStart(node);
            }}
            title="发送消息"
          >
            💬
          </button>
          <button 
            className="action-button video-call-btn"
            onClick={(e) => {
              e.stopPropagation();
              handleVideoCall(node);
            }}
            title="视频通话"
          >
            📹
          </button>
        </div>
      </div>
    );
  };

  if (isLoading) {
    return (
      <div className="contacts-screen">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>加载组织架构中...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="contacts-screen">
        <div className="error-container">
          <div className="error-icon">❌</div>
          <h3>加载失败</h3>
          <p>{error}</p>
          <button className="retry-button" onClick={loadOrganizationStructure}>
            重新加载
          </button>
        </div>
      </div>
    );
  }

  const filteredData = getFilteredData();

  return (
    <div className="contacts-screen">
      {/* Search Box - Enhanced Material Design */}
      <div className="search-container">
        <div className="search-box">
          <span className="search-icon">👥</span>
          <input
            type="text"
            className="search-input"
            placeholder="搜索部门、人员或职位..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button 
              className="clear-search"
              onClick={() => setSearchQuery('')}
              aria-label="Clear search"
            >
              ✕
            </button>
          )}
          <button 
            className="refresh-button"
            onClick={loadOrganizationStructure}
            title="刷新"
            disabled={isLoading}
          >
            🔄
          </button>
        </div>
      </div>

      {/* Organization Tree */}
      <div className="contacts-list">
        {filteredData.length > 0 ? (
          filteredData.map(node => 
            'userId' in node 
              ? renderUserNode(node)
              : renderDepartmentNode(node)
          )
        ) : (
          <div className="empty-state">
            <div className="empty-icon">👥</div>
            <h3>暂无数据</h3>
            <p>{searchQuery ? '没有找到匹配的结果' : '组织架构为空'}</p>
          </div>
        )}
      </div>

      {/* Floating Add Contact Button */}
      <button className="floating-add-btn" title="添加联系人">
        +
      </button>
    </div>
  );
};

export default ContactsScreen;
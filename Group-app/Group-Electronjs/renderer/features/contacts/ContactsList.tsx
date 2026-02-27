import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../../store';
import { fetchOrgStructure } from './contactsSlice';
import { OrgTreeNode, ApiUser } from '../../types';
import './ContactsList.css';

interface ContactsListProps {
    onSelectUser?: (user: ApiUser) => void;
}

const ContactsList: React.FC<ContactsListProps> = ({ onSelectUser }) => {
    const dispatch = useDispatch<AppDispatch>();
    const { orgTree = [], loading = false, error = null } = useSelector((state: RootState) => state.contacts || {});
    const [expandedNodes, setExpandedNodes] = useState<Set<number>>(new Set());
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        dispatch(fetchOrgStructure());
    }, [dispatch]);

    // Automatically expand nodes when searching
    useEffect(() => {
        if (searchQuery.length > 0) {
            const allIds = new Set<number>();
            const collectIds = (nodes: OrgTreeNode[]) => {
                nodes.forEach(node => {
                    if (node.type === 'DEPARTMENT') {
                        allIds.add(node.id);
                        if (node.children) collectIds(node.children);
                    }
                });
            };
            collectIds(orgTree);
            setExpandedNodes(allIds);
        }
    }, [searchQuery, orgTree]);

    const toggleExpand = (id: number) => {
        const newSet = new Set(expandedNodes);
        if (newSet.has(id)) {
            newSet.delete(id);
        } else {
            newSet.add(id);
        }
        setExpandedNodes(newSet);
    };

    const filterTree = (nodes: OrgTreeNode[]): OrgTreeNode[] => {
        if (!searchQuery) return nodes;

        return nodes.reduce((acc: OrgTreeNode[], node) => {
            const matches = node.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                (node.userInfo?.email || '').toLowerCase().includes(searchQuery.toLowerCase());

            if (node.type === 'DEPARTMENT') {
                const filteredChildren = filterTree(node.children || []);
                if (matches || filteredChildren.length > 0) {
                    acc.push({ ...node, children: filteredChildren });
                }
            } else if (matches) {
                acc.push(node);
            }
            return acc;
        }, []);
    };

    const renderNode = (node: OrgTreeNode, depth: number = 0) => {
        if (!node) return null;
        const isExpanded = expandedNodes.has(node.id);

        return (
            <div key={`${node.type}-${node.id}`} className="org-node-container">
                <div
                    className={`org-node ${node.type === 'USER' ? 'user-node' : 'dept-node'}`}
                    style={{ paddingLeft: `${depth * 16 + 12}px` }}
                    onClick={() => {
                        if (node.type === 'DEPARTMENT') {
                            toggleExpand(node.id);
                        } else if (node.type === 'USER' && node.userInfo && onSelectUser) {
                            onSelectUser(node.userInfo);
                        }
                    }}
                >
                    {node.type === 'DEPARTMENT' && (
                        <span className={`expand-icon ${isExpanded ? 'expanded' : ''}`}>
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="9 18 15 12 9 6"></polyline>
                            </svg>
                        </span>
                    )}

                    <div className="node-icon">
                        {node.type === 'USER' ? (
                            <div className="user-avatar-sm" style={{
                                background: `linear-gradient(135deg, ${getRandomColor(node.name)})`
                            }}>
                                {(node.name || '?').charAt(0).toUpperCase()}
                            </div>
                        ) : (
                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke={isExpanded ? "#3b82f6" : "#64748b"} strokeWidth="2">
                                <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                            </svg>
                        )}
                    </div>

                    <div className="node-info">
                        <span className="node-name">{node.name}</span>
                        {node.type === 'USER' && (
                            <span className="node-detail">{node.userInfo?.email || 'No email provided'}</span>
                        )}
                    </div>

                    {node.type === 'USER' && (
                        <button className="node-action-btn" title="Send Message">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5">
                                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                            </svg>
                        </button>
                    )}
                </div>

                {node.type === 'DEPARTMENT' && isExpanded && node.children &&
                    node.children.map((child: OrgTreeNode) => renderNode(child, depth + 1))
                }
            </div>
        );
    };

    const getRandomColor = (name: string) => {
        const colors = [
            '#3b82f6, #2563eb',
            '#10b981, #059669',
            '#f59e0b, #d97706',
            '#ef4444, #dc2626',
            '#8b5cf6, #7c3aed',
            '#ec4899, #db2777'
        ];
        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = name.charCodeAt(i) + ((hash << 5) - hash);
        }
        return colors[Math.abs(hash) % colors.length];
    };

    if (loading && orgTree.length === 0) {
        return <div className="contacts-loading">Synchronizing structure...</div>;
    }

    if (error) {
        return <div className="contacts-error">Error: {error}</div>;
    }

    const filteredTree = filterTree(orgTree);

    return (
        <div className="contacts-list-container">
            <div className="contacts-header">
                <h2>Organization</h2>
                <p>Browse departments and colleagues</p>
            </div>

            <div className="contacts-search-box">
                <div className="search-icon-tree">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </div>
                <input
                    type="text"
                    placeholder="Search by name or email..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
            </div>

            <div className="org-tree">
                {filteredTree.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
                        No results found for "{searchQuery}"
                    </div>
                ) : (
                    filteredTree.map((rootNode: OrgTreeNode) => renderNode(rootNode))
                )}
            </div>
        </div>
    );
};

export default ContactsList;

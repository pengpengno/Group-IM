import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { orgAPI, authAPI } from '../../services/api/apiClient';
import { RootState } from '../../store';
import { CompanyDTO } from '../../types';
import './AdminPanel.css';

const AdminPanel: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'tenants' | 'users' | 'add-user'>('tenants');
  const [companies, setCompanies] = useState<CompanyDTO[]>([]);
  const [loading, setLoading] = useState(false);
  
  // Tenant Form
  const [newTenant, setNewTenant] = useState({ name: '', code: '' });
  
  // User Form
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | ''>('');
  const [importFile, setImportFile] = useState<File | null>(null);

  // Add User Form
  const [newUser, setNewUser] = useState({
    username: '',
    password: '',
    email: '',
    phoneNumber: '',
    companyCode: ''
  });

  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUser.username || !newUser.password || !newUser.companyCode) {
      alert('请填写必填项');
      return;
    }

    setLoading(true);
    try {
      const response = await authAPI.register({
        username: newUser.username,
        password: newUser.password,
        email: newUser.email,
        phoneNumber: newUser.phoneNumber,
        companyCode: newUser.companyCode
      });
      if (response.status === 201 || response.status === 200) {
        alert('用户添加成功');
        setNewUser({ username: '', password: '', email: '', phoneNumber: '', companyCode: '' });
      } else {
        alert('添加失败: ' + (response.data?.message || '未知错误'));
      }
    } catch (error: any) {
      alert('添加异常: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCompanies();
  }, []);

  const fetchCompanies = async () => {
    setLoading(true);
    try {
      const response = await orgAPI.getAllCompanies();
      if (response.data?.code === 200) {
        setCompanies(response.data.data);
      }
    } catch (error) {
      console.error('Failed to fetch companies', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterCompany = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTenant.name || !newTenant.code) return;

    setLoading(true);
    try {
      const response = await authAPI.registerCompany(newTenant.name, newTenant.code);
      if (response.data?.code === 200) {
        alert('公司注册成功');
        setNewTenant({ name: '', code: '' });
        fetchCompanies();
      } else {
        alert('注册失败: ' + (response.data?.message || '未知错误'));
      }
    } catch (error: any) {
      alert('注册异常: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleImportUsers = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCompanyId || !importFile) {
      alert('请选择公司并上传文件');
      return;
    }

    setLoading(true);
    try {
      const response = await orgAPI.importUsers(Number(selectedCompanyId), importFile);
      if (response.data?.code === 200) {
        alert('用户导入成功');
        setImportFile(null);
      } else {
        alert('导入失败: ' + (response.data?.message || '未知错误'));
      }
    } catch (error: any) {
      alert('导入异常: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  const downloadTemplate = async () => {
    try {
      const response = await orgAPI.getImportTemplate();
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'user_import_template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      alert('下载模板失败');
    }
  };

  return (
    <div className="admin-panel">
      <div className="admin-nav">
        <button 
          className={activeTab === 'tenants' ? 'active' : ''} 
          onClick={() => setActiveTab('tenants')}
        >
          租户管理
        </button>
        <button 
          className={activeTab === 'users' ? 'active' : ''} 
          onClick={() => setActiveTab('users')}
        >
          批量导入
        </button>
        <button 
          className={activeTab === 'add-user' ? 'active' : ''} 
          onClick={() => setActiveTab('add-user')}
        >
          添加用户
        </button>
      </div>

      <div className="admin-content">
        {activeTab === 'tenants' && (
          <div className="admin-section">
            <h3>注册新租户</h3>
            <form onSubmit={handleRegisterCompany} className="admin-form">
              <div className="form-group">
                <label>公司名称</label>
                <input 
                  type="text" 
                  value={newTenant.name} 
                  onChange={e => setNewTenant({...newTenant, name: e.target.value})}
                  placeholder="如: 阿里巴巴"
                />
              </div>
              <div className="form-group">
                <label>公司代码 (Schema)</label>
                <input 
                  type="text" 
                  value={newTenant.code} 
                  onChange={e => setNewTenant({...newTenant, code: e.target.value})}
                  placeholder="仅限字母数字，如: alibaba"
                />
              </div>
              <button type="submit" disabled={loading} className="admin-btn">
                {loading ? '注册中...' : '注册公司'}
              </button>
            </form>

            <div className="company-list">
              <h3>已有租户</h3>
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>名称</th>
                    <th>代码</th>
                  </tr>
                </thead>
                <tbody>
                  {companies.map(c => (
                    <tr key={c.companyId}>
                      <td>{c.companyId}</td>
                      <td>{c.name}</td>
                      <td>{c.code}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'users' && (
          <div className="admin-section">
            <h3>批量导入用户</h3>
            <div className="template-box" onClick={downloadTemplate}>
              <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
              </svg>
              <span>下载 Excel 模板</span>
            </div>

            <form onSubmit={handleImportUsers} className="admin-form">
              <div className="form-group">
                <label>选择公司</label>
                <select 
                  value={selectedCompanyId} 
                  onChange={e => setSelectedCompanyId(e.target.value ? Number(e.target.value) : '')}
                >
                  <option value="">-- 请选择 --</option>
                  {companies.map(c => (
                    <option key={c.companyId} value={c.companyId}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>上传文件</label>
                <input 
                  type="file" 
                  accept=".xlsx, .xls"
                  onChange={e => setImportFile(e.target.files?.[0] || null)}
                />
              </div>
              <button type="submit" disabled={loading || !importFile} className="admin-btn">
                {loading ? '导入中...' : '开始导入'}
              </button>
            </form>
          </div>
        )}

        {activeTab === 'add-user' && (
          <div className="admin-section">
            <h3>添加单个用户</h3>
            <form onSubmit={handleAddUser} className="admin-form">
              <div className="form-group">
                <label>用户名 *</label>
                <input 
                  type="text" 
                  value={newUser.username} 
                  onChange={e => setNewUser({...newUser, username: e.target.value})}
                  placeholder="用户名"
                  required
                />
              </div>
              <div className="form-group">
                <label>密码 *</label>
                <input 
                  type="password" 
                  value={newUser.password} 
                  onChange={e => setNewUser({...newUser, password: e.target.value})}
                  placeholder="密码"
                  required
                />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input 
                  type="email" 
                  value={newUser.email} 
                  onChange={e => setNewUser({...newUser, email: e.target.value})}
                  placeholder="example@mail.com"
                />
              </div>
              <div className="form-group">
                <label>手机号</label>
                <input 
                  type="text" 
                  value={newUser.phoneNumber} 
                  onChange={e => setNewUser({...newUser, phoneNumber: e.target.value})}
                  placeholder="138..."
                />
              </div>
              <div className="form-group">
                <label>归属公司代码 *</label>
                <select 
                  value={newUser.companyCode} 
                  onChange={e => setNewUser({...newUser, companyCode: e.target.value})}
                  required
                >
                  <option value="">-- 请选择 --</option>
                  {companies.map(c => (
                    <option key={c.companyId} value={c.code}>{c.name} ({c.code})</option>
                  ))}
                </select>
              </div>
              <button type="submit" disabled={loading} className="admin-btn">
                {loading ? '添加中...' : '确认添加'}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminPanel;

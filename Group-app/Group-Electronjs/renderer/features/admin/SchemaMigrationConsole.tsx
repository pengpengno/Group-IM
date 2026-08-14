import React, { useEffect, useState } from 'react';
import type { CompanyDTO } from '../../types';
import { orgAPI } from '../../services/api/apiClient';
import './SchemaMigrationConsole.css';

interface Props { companies: CompanyDTO[]; loading: boolean; onSync: (all: boolean, ids?: number[]) => Promise<void>; }
interface SchemaStatus { companyId: number; currentFingerprint?: string; targetVersion: string; publishedAt: string; status: 'SYNCED' | 'OUTDATED' | 'CONFLICT' | 'ERROR'; differences: string[]; }

const TARGET_RELEASE = {
  version: '2026.08.14.01',
  publishedAt: '2026-08-14 02:10',
  description: '补齐会话成员角色字段',
};

const SchemaMigrationConsole: React.FC<Props> = ({ companies, loading, onSync }) => {
  const [scope, setScope] = useState<'all' | 'selected'>('all');
  const [ids, setIds] = useState<number[]>([]);
  const [confirming, setConfirming] = useState(false);
  const [acknowledged, setAcknowledged] = useState(false);
  const [statuses, setStatuses] = useState<Record<number, SchemaStatus>>({});
  const [statusError, setStatusError] = useState<string | null>(null);
  const targetCount = scope === 'all' ? companies.length : ids.length;
  const toggle = (id: number) => setIds(value => value.includes(id) ? value.filter(item => item !== id) : [...value, id]);
  const refresh = async () => {
    try {
      const response = await orgAPI.getSchemaSyncStatus();
      const rows: SchemaStatus[] = response.data?.data || [];
      setStatuses(Object.fromEntries(rows.map(row => [row.companyId, row])));
      setStatusError(null);
    } catch (error: any) { setStatusError(error.response?.data?.message || '无法读取租户 Schema 状态'); }
  };
  useEffect(() => { void refresh(); }, []);

  return <section className="schema-console" aria-labelledby="schema-title">
    <header><div><p className="schema-kicker">系统管理 / 数据库</p><h2 id="schema-title">Schema 迁移</h2><p>向指定租户提交表结构同步请求；版本和逐租户执行状态将在迁移服务接入后显示。</p></div><span className="schema-pending">迁移服务待接入</span></header>
    <div className="schema-release"><span>目标发布</span><div><strong>V{TARGET_RELEASE.version}</strong><small>发布于 {TARGET_RELEASE.publishedAt}</small></div><p>{TARGET_RELEASE.description}</p><button type="button" onClick={() => void refresh()}>重新检查</button></div>
    <div className="schema-notice"><strong>安全规则：</strong>仅自动补齐已有表中“可空且无默认值”的缺失字段。任意字段定义冲突、缺表或可能影响数据的变更都会阻断同步。</div>
    {statusError && <div className="schema-notice" role="alert">{statusError}</div>}
    <div className="schema-grid"><main>
      <div className="schema-section"><h3>选择迁移范围</h3><div className="schema-scope">
        <label className={scope === 'all' ? 'active' : ''}><input type="radio" checked={scope === 'all'} onChange={() => setScope('all')} /> <span><strong>所有活跃租户</strong><small>将处理 {companies.length} 个已登记租户</small></span></label>
        <label className={scope === 'selected' ? 'active' : ''}><input type="radio" checked={scope === 'selected'} onChange={() => setScope('selected')} /> <span><strong>选定租户</strong><small>仅处理手动勾选的租户</small></span></label>
      </div></div>
      <div className="schema-table"><table><thead><tr><th>租户</th><th>Schema</th><th>当前指纹</th><th>目标版本</th><th>状态</th><th>选择</th></tr></thead><tbody>{companies.map(company => { const status = statuses[company.companyId]; return <tr key={company.companyId}><td><strong>{company.name}</strong><small>#{company.companyId}</small></td><td><code>{company.code}</code></td><td><span className="schema-unknown">{status?.currentFingerprint || '等待检测'}</span></td><td><strong>V{status?.targetVersion || TARGET_RELEASE.version}</strong><small>{status?.publishedAt || TARGET_RELEASE.publishedAt}</small></td><td><span className={`schema-status schema-status--${status?.status || 'UNKNOWN'}`}>{status?.status || '等待检测'}</span>{status?.differences?.length ? <small>{status.differences[0]}</small> : null}</td><td><input aria-label={`选择 ${company.name}`} type="checkbox" disabled={scope === 'all' || status?.status === 'CONFLICT'} checked={ids.includes(company.companyId)} onChange={() => toggle(company.companyId)} /></td></tr>; })}{!loading && !companies.length && <tr><td colSpan={6}>暂无可同步租户。</td></tr>}</tbody></table></div>
    </main><aside><p className="schema-kicker">提交前检查</p><h3>同步摘要</h3><dl><div><dt>目标租户</dt><dd>{targetCount}</dd></div><div><dt>目标版本</dt><dd>V{TARGET_RELEASE.version}</dd></div><div><dt>发布时间</dt><dd>{TARGET_RELEASE.publishedAt}</dd></div><div><dt>执行模式</dt><dd>现有同步接口</dd></div></dl><button disabled={loading || !targetCount} onClick={() => { setAcknowledged(false); setConfirming(true); }}>提交同步请求</button></aside></div>
    {confirming && <div className="schema-backdrop"><div className="schema-dialog" role="dialog" aria-modal="true" aria-labelledby="schema-confirm"><p className="schema-kicker">高风险操作</p><h3 id="schema-confirm">确认安全同步</h3><p>只会新增可空且无默认值的缺失字段；任何结构冲突都会终止同步且不执行 DDL。</p><label><input type="checkbox" checked={acknowledged} onChange={event => setAcknowledged(event.target.checked)} /> 我已确认目标范围。</label><footer><button onClick={() => setConfirming(false)}>取消</button><button className="danger" disabled={!acknowledged || loading} onClick={async () => { try { await onSync(scope === 'all', ids); await refresh(); setConfirming(false); } catch (error: any) { setStatusError(error.response?.data?.message || error.message); } }}>确认并提交</button></footer></div></div>}
  </section>;
};

export default SchemaMigrationConsole;

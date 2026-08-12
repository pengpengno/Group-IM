import React, { useEffect, useState } from 'react';
import { automationAPI } from '../../services/api/apiClient';
import './AutomationCenter.css';

type Rule = { ruleId: string; conversationId: string; enabled: boolean; configuration: string };
type Execution = { executionId: string; summary: string; status: string; resultSummary?: string; createdAt?: string };

export const AutomationCenter: React.FC<{ conversationId: number; onClose: () => void }> = ({ conversationId, onClose }) => {
  const [rules, setRules] = useState<Rule[]>([]);
  const [executions, setExecutions] = useState<Execution[]>([]);
  const [contains, setContains] = useState('');
  const [replyText, setReplyText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const load = async () => {
    setBusy(true); setError('');
    try {
      const [ruleRes, executionRes] = await Promise.all([automationAPI.listRules(), automationAPI.listExecutions()]);
      setRules((ruleRes.data?.data || ruleRes.data || []).filter((rule: Rule) => Number(rule.conversationId) === conversationId));
      setExecutions(executionRes.data?.data || executionRes.data || []);
    } catch { setError('无法加载自动化数据，请稍后重试。'); } finally { setBusy(false); }
  };
  useEffect(() => { void load(); }, [conversationId]);
  const create = async (event: React.FormEvent) => {
    event.preventDefault(); if (!replyText.trim()) return;
    setBusy(true); setError('');
    try { await automationAPI.createReplyRule({ conversationId, contains: contains.trim(), replyText: replyText.trim() }); setContains(''); setReplyText(''); await load(); }
    catch { setError('创建规则失败。'); setBusy(false); }
  };
  const toggle = async (rule: Rule) => { setBusy(true); try { await automationAPI.setRuleEnabled(rule.ruleId, !rule.enabled); await load(); } catch { setError('更新规则失败。'); setBusy(false); } };
  return <section className="automation-center" aria-label="自动化中心">
    <header><div><p className="automation-kicker">自动化中心</p><h3>本群自动回复</h3></div><button type="button" className="close-btn" onClick={onClose} aria-label="关闭自动化中心">×</button></header>
    <form onSubmit={create}><label>触发关键词（可选）<input value={contains} onChange={e => setContains(e.target.value)} placeholder="例如：日报" /></label><label>自动回复 <textarea value={replyText} onChange={e => setReplyText(e.target.value)} required placeholder="例如：请在 17:00 前提交日报。" /></label><button type="submit" disabled={busy}>{busy ? '处理中…' : '创建规则'}</button></form>
    {error && <p role="alert" className="automation-error">{error}</p>}
    <h4>已启用/已停用规则</h4>{!rules.length && !busy ? <p className="automation-empty">当前群还没有自动回复规则。</p> : <ul>{rules.map(rule => <li key={rule.ruleId}><span>{rule.enabled ? '已启用' : '已停用'}</span><code>{rule.configuration}</code><button type="button" onClick={() => void toggle(rule)} disabled={busy}>{rule.enabled ? '停用' : '启用'}</button></li>)}</ul>}
    <h4>最近执行</h4>{!executions.length && !busy ? <p className="automation-empty">尚无执行记录。</p> : <ul>{executions.map(item => <li key={item.executionId}><span>{item.status}</span><div>{item.summary}<small>{item.resultSummary || '处理中'}</small></div></li>)}</ul>}
  </section>;
};

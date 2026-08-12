import React from 'react';
import './AutomationProposalCard.css';

export type AutomationProposal = {
  title: string;
  summary: string;
  scope?: string;
  expiresAt?: string;
  affectedCount?: number;
};

export const AutomationProposalCard: React.FC<{
  proposal: AutomationProposal;
  pending?: boolean;
  onApprove: () => void;
  onDecline: () => void;
}> = ({ proposal, pending = false, onApprove, onDecline }) => (
  <section className="automation-proposal-card" aria-label="自动化操作确认">
    <div className="automation-proposal-eyebrow">需确认</div>
    <h3>{proposal.title}</h3>
    <p>{proposal.summary}</p>
    {proposal.scope && <p className="automation-proposal-meta">影响范围：{proposal.scope}</p>}
    {typeof proposal.affectedCount === 'number' && <p className="automation-proposal-meta">将影响 {proposal.affectedCount} 个对象</p>}
    {proposal.expiresAt && <p className="automation-proposal-meta">有效至：{proposal.expiresAt}</p>}
    <div className="automation-proposal-actions">
      <button type="button" className="automation-secondary-action" disabled={pending} onClick={onDecline}>拒绝</button>
      <button type="button" className="automation-primary-action" disabled={pending} onClick={onApprove}>{pending ? '处理中…' : '批准执行'}</button>
    </div>
  </section>
);

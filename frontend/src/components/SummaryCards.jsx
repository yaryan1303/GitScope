import React from 'react';
import { fmt, signed } from '../format.js';
import { IconCommit, IconUsers, IconPlus, IconMinus, IconFiles, IconCalendar } from './Icons.jsx';

function Kpi({ icon, color, tint, label, value, sub }) {
  return (
    <div className="kpi" style={{ '--kpi-color': color, '--kpi-tint': tint }}>
      <div className="kpi-icon">{icon}</div>
      <div className="kpi-label">{label}</div>
      <div className="kpi-value num">{value}</div>
      {sub && <div className="kpi-sub">{sub}</div>}
    </div>
  );
}

export default function SummaryCards({ summary }) {
  if (!summary) return null;
  const net = (summary.totalLinesAdded || 0) - (summary.totalLinesRemoved || 0);

  return (
    <div className="kpi-grid">
      <Kpi
        icon={<IconCommit />} color="#4f46e5" tint="#eef0ff"
        label="Total Commits" value={fmt(summary.totalCommits)} sub="merge commits excluded"
      />
      <Kpi
        icon={<IconUsers />} color="#8b5cf6" tint="#f3eefe"
        label="Contributors" value={fmt(summary.totalAuthors)} sub="duplicate identities merged"
      />
      <Kpi
        icon={<IconPlus />} color="#10b981" tint="#e7f7f1"
        label="Lines Added" value={fmt(summary.totalLinesAdded)}
        sub={<>net <strong style={{ color: net >= 0 ? 'var(--pos)' : 'var(--neg)' }}>{signed(net)}</strong></>}
      />
      <Kpi
        icon={<IconMinus />} color="#f43f5e" tint="#fdeaee"
        label="Lines Removed" value={fmt(summary.totalLinesRemoved)} sub="across all history"
      />
      <Kpi
        icon={<IconFiles />} color="#0ea5e9" tint="#e6f5fd"
        label="Tracked Files" value={fmt(summary.totalFiles)} sub="at current HEAD"
      />
      <Kpi
        icon={<IconCalendar />} color="#f59e0b" tint="#fdf2e1"
        label="Active Days" value={fmt(summary.activeDays)}
        sub={summary.firstCommitDate ? `since ${summary.firstCommitDate}` : null}
      />
    </div>
  );
}

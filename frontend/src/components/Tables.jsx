import React from 'react';
import { fmt, avatarFor, initials } from '../format.js';

function Identity({ name }) {
  return (
    <span className="identity">
      <span className="avatar" style={avatarFor(name)}>{initials(name)}</span>
      <span className="author-name">{name}</span>
    </span>
  );
}

export function BusFactorTable({ data }) {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Bus Factor Risk</h2>
        <span className="sub">files owned 100% by one contributor</span>
        <span className="tag">{(data || []).length} files</span>
      </div>
      {(!data || data.length === 0) ? (
        <p className="hint">No single-owner files — knowledge is well distributed. ✓</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>File</th><th>Sole owner</th><th className="num">Commits</th></tr>
            </thead>
            <tbody>
              {data.map((b) => (
                <tr key={b.path}>
                  <td className="path">{b.path}</td>
                  <td><Identity name={b.author} /></td>
                  <td className="num warn">{fmt(b.commits)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export function LargestCommitsTable({ data }) {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Largest Commits</h2>
        <span className="sub">top 15 by lines changed</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th style={{ width: 110 }}>Commit</th><th>Author</th><th className="num">Lines</th><th>Subject</th></tr>
          </thead>
          <tbody>
            {(data || []).map((c) => (
              <tr key={c.hash}>
                <td><span className="hash-chip">{c.hash.slice(0, 7)}</span></td>
                <td><Identity name={c.author} /></td>
                <td className="num"><strong>{fmt(c.total)}</strong></td>
                <td>{c.subject}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export function WeekdayByAuthorTable({ data }) {
  const rows = (data || []).slice(0, 10);
  let max = 1;
  rows.forEach((r) => DAYS.forEach((d) => { max = Math.max(max, r.counts?.[d] || 0); }));

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Weekly Activity Heatmap</h2>
        <span className="sub">commits per weekday, by contributor</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Contributor</th>
              {DAYS.map((d) => <th key={d} className="num" style={{ textAlign: 'center' }}>{d}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.author}>
                <td><Identity name={r.author} /></td>
                {DAYS.map((d) => {
                  const v = r.counts?.[d] || 0;
                  const t = v === 0 ? 0 : 0.16 + 0.74 * (v / max);
                  const dark = t > 0.55;
                  return (
                    <td key={d} style={{ padding: '5px 4px' }}>
                      <div
                        className="heat-cell"
                        style={{ background: v ? `rgba(79,70,229,${t.toFixed(3)})` : 'var(--surface-2)', color: dark ? '#fff' : 'var(--text-2)' }}
                      >
                        {v || '·'}
                      </div>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

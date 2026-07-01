import React, { useMemo, useState } from 'react';
import { fmt, signed, avatarFor, initials } from '../format.js';

const COLUMNS = [
  { key: 'net', label: 'Net lines' },
  { key: 'commits', label: 'Commits' },
  { key: 'filesTouched', label: 'Files' },
];

export default function AuthorLeaderboard({ authors }) {
  const [sortKey, setSortKey] = useState('net');
  const [expanded, setExpanded] = useState(() => new Set());

  const totalCommits = useMemo(
    () => (authors || []).reduce((s, a) => s + (a.commits || 0), 0) || 1,
    [authors]
  );

  const rows = useMemo(() => {
    const copy = [...(authors || [])];
    copy.sort((a, b) => (b[sortKey] ?? 0) - (a[sortKey] ?? 0));
    return copy;
  }, [authors, sortKey]);

  function toggle(name) {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(name) ? next.delete(name) : next.add(name);
      return next;
    });
  }

  function header(col) {
    const active = sortKey === col.key;
    return (
      <th
        key={col.key}
        className="sortable num"
        onClick={() => setSortKey(col.key)}
        aria-sort={active ? 'descending' : 'none'}
        title={`Sort by ${col.label.toLowerCase()}`}
      >
        {col.label}
        {active && <span className="arrow">▼</span>}
      </th>
    );
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Contributor Leaderboard</h2>
        <span className="sub">click a column to sort · expand a row for merged aliases</span>
        <span className="tag">sorted by {sortKey === 'filesTouched' ? 'files' : sortKey}</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>Contributor</th>
              <th style={{ width: 150 }}>Commit share</th>
              {COLUMNS.map(header)}
              <th className="num">Added</th>
              <th className="num">Removed</th>
              <th className="num">Active days</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((a, i) => {
              const hasAliases = a.aliases && a.aliases.length > 1;
              const isOpen = expanded.has(a.name);
              const pct = (a.commits / totalCommits) * 100;
              return (
                <React.Fragment key={a.name + i}>
                  <tr>
                    <td><span className={'rank' + (i === 0 ? ' top' : '')}>{i + 1}</span></td>
                    <td>
                      {hasAliases ? (
                        <button className="expand-btn" aria-expanded={isOpen} onClick={() => toggle(a.name)} title="Show merged aliases">
                          <span className="caret">▶</span>
                          <span className="identity">
                            <span className="avatar" style={avatarFor(a.name)}>{initials(a.name)}</span>
                            <span>
                              <span className="author-name">{a.name}</span>{' '}
                              <span className="alias-count">{a.aliases.length} aliases</span>
                            </span>
                          </span>
                        </button>
                      ) : (
                        <span className="identity">
                          <span className="avatar" style={avatarFor(a.name)}>{initials(a.name)}</span>
                          <span className="author-name">{a.name}</span>
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="share">
                        <div className="bar"><div className="bar-fill" style={{ width: `${pct}%` }} /></div>
                        <span className="pct">{pct.toFixed(0)}%</span>
                      </div>
                    </td>
                    <td className={'num ' + (a.net >= 0 ? 'pos' : 'neg')}>{signed(a.net)}</td>
                    <td className="num">{fmt(a.commits)}</td>
                    <td className="num">{fmt(a.filesTouched)}</td>
                    <td className="num pos">{fmt(a.added)}</td>
                    <td className="num neg">{fmt(a.removed)}</td>
                    <td className="num">{fmt(a.activeDays)}</td>
                  </tr>
                  {isOpen && hasAliases && (
                    <tr className="alias-row">
                      <td />
                      <td colSpan={8}>
                        <div className="alias-list">
                          {a.aliases.map((al) => (
                            <span className="chip" key={al}>{al}</span>
                          ))}
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}

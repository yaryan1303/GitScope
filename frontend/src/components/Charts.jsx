import React from 'react';
import {
  ResponsiveContainer, AreaChart, Area, BarChart, Bar,
  PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip,
} from 'recharts';
import { COLORS, fmt } from '../format.js';

function Panel({ title, sub, tag, signature, children }) {
  return (
    <section className={'panel' + (signature ? ' signature' : '')}>
      <div className="panel-head">
        <h2>{title}</h2>
        {sub && <span className="sub">{sub}</span>}
        {tag && <span className="tag">{tag}</span>}
      </div>
      <div className="chart-wrap">{children}</div>
    </section>
  );
}

function GsTooltip({ active, payload, label, unit }) {
  if (!active || !payload || !payload.length) return null;
  return (
    <div className="gs-tooltip">
      <div className="t-label">{label}</div>
      <div className="t-value">{fmt(payload[0].value)} {unit}</div>
    </div>
  );
}

const axisProps = { tick: { fill: COLORS.axis, fontSize: 11.5 }, tickLine: false, axisLine: { stroke: COLORS.grid } };

/* ---- Signature: monthly commit trend ---- */
export function MonthlyTrend({ data }) {
  return (
    <Panel title="Commit Activity Over Time" sub="commits per month" signature>
      <ResponsiveContainer width="100%" height={280}>
        <AreaChart data={data} margin={{ top: 10, right: 18, left: -8, bottom: 0 }}>
          <defs>
            <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={COLORS.primary400} stopOpacity={0.32} />
              <stop offset="100%" stopColor={COLORS.primary400} stopOpacity={0.02} />
            </linearGradient>
            <linearGradient id="trendStroke" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor={COLORS.primary400} />
              <stop offset="100%" stopColor={COLORS.primary600} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="4 4" stroke={COLORS.grid} vertical={false} />
          <XAxis dataKey="key" {...axisProps} minTickGap={26} />
          <YAxis {...axisProps} allowDecimals={false} width={38} />
          <Tooltip content={<GsTooltip unit="commits" />} cursor={{ stroke: COLORS.primary400, strokeDasharray: '4 4' }} />
          <Area
            type="monotone" dataKey="count" stroke="url(#trendStroke)" strokeWidth={3}
            fill="url(#trendFill)" dot={false}
            activeDot={{ r: 5, fill: COLORS.primary, stroke: '#fff', strokeWidth: 2 }}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </Panel>
  );
}

/* ---- Generic vertical bar ---- */
function VBar({ title, sub, data, dataKey = 'count', labelKey = 'key', color, unit, gradId }) {
  return (
    <Panel title={title} sub={sub}>
      <ResponsiveContainer width="100%" height={230}>
        <BarChart data={data} margin={{ top: 8, right: 10, left: -12, bottom: 0 }}>
          <defs>
            <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.95} />
              <stop offset="100%" stopColor={color} stopOpacity={0.55} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="4 4" stroke={COLORS.grid} vertical={false} />
          <XAxis dataKey={labelKey} {...axisProps} interval={0} />
          <YAxis {...axisProps} allowDecimals={false} width={38} />
          <Tooltip content={<GsTooltip unit={unit} />} cursor={{ fill: 'rgba(99,102,241,0.06)' }} />
          <Bar dataKey={dataKey} fill={`url(#${gradId})`} radius={[6, 6, 0, 0]} maxBarSize={42} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </Panel>
  );
}

/* ---- Generic horizontal bar ---- */
function HBar({ title, sub, data, dataKey = 'count', labelKey = 'key', color, unit, gradId, height = 360, width = 160 }) {
  return (
    <Panel title={title} sub={sub}>
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 22, left: 6, bottom: 4 }}>
          <defs>
            <linearGradient id={gradId} x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor={color} stopOpacity={0.6} />
              <stop offset="100%" stopColor={color} stopOpacity={0.95} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="4 4" stroke={COLORS.grid} horizontal={false} />
          <XAxis type="number" {...axisProps} allowDecimals={false} />
          <YAxis type="category" dataKey={labelKey} {...axisProps} width={width} interval={0} />
          <Tooltip content={<GsTooltip unit={unit} />} cursor={{ fill: 'rgba(99,102,241,0.06)' }} />
          <Bar dataKey={dataKey} fill={`url(#${gradId})`} radius={[0, 6, 6, 0]} maxBarSize={22} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </Panel>
  );
}

export function HourChart({ data }) {
  return <VBar title="Commits by Hour" sub="when work happens (24h)" data={data} color={COLORS.series[1]} unit="commits" gradId="gHour" />;
}
export function WeekdayChart({ data }) {
  return <VBar title="Commits by Weekday" sub="weekly rhythm" data={data} color={COLORS.series[2]} unit="commits" gradId="gWeek" />;
}
export function FileChurnChart({ data }) {
  const rows = (data || []).map((d) => ({ ...d, short: shorten(d.path) }));
  return <HBar title="File Churn" sub="top 20 most-changed files" data={rows} dataKey="changes" labelKey="short" color={COLORS.series[0]} unit="changes" gradId="gChurn" width={180} />;
}
export function VerbChart({ data }) {
  return <HBar title="Commit Verbs" sub="top 15 leading words" data={data} color={COLORS.series[4]} unit="commits" gradId="gVerb" width={110} height={320} />;
}

/* ---- File-type donut ---- */
export function FileTypeChart({ data }) {
  const top = (data || []).slice(0, 8);
  const total = top.reduce((s, d) => s + (d.count || 0), 0) || 1;
  return (
    <Panel title="File Types" sub="by extension at HEAD">
      <div className="donut-wrap">
        <ResponsiveContainer width="100%" height={240}>
          <PieChart>
            <Pie
              data={top} dataKey="count" nameKey="key"
              cx="50%" cy="50%" innerRadius={62} outerRadius={96}
              paddingAngle={2} stroke="none" isAnimationActive={false}
            >
              {top.map((_, i) => (
                <Cell key={i} fill={COLORS.series[i % COLORS.series.length]} />
              ))}
            </Pie>
            <Tooltip content={<GsTooltip unit="files" />} />
          </PieChart>
        </ResponsiveContainer>
        <div className="legend">
          {top.map((d, i) => (
            <div className="legend-item" key={d.key}>
              <span className="legend-dot" style={{ background: COLORS.series[i % COLORS.series.length] }} />
              <span className="lg-name">.{d.key}</span>
              <span className="lg-val">{((d.count / total) * 100).toFixed(0)}%</span>
            </div>
          ))}
        </div>
      </div>
    </Panel>
  );
}

function shorten(path) {
  if (!path) return '';
  if (path.length <= 30) return path;
  const file = path.split('/').pop();
  return file.length >= 28 ? '…' + file.slice(-27) : '…/' + file;
}

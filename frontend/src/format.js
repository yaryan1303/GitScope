// Number/label formatting helpers and the shared color palette.

export function fmt(n) {
  if (n === null || n === undefined) return '—';
  return Number(n).toLocaleString('en-US');
}

export function compact(n) {
  if (n === null || n === undefined) return '—';
  return Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(Number(n));
}

export function signed(n) {
  if (n === null || n === undefined) return '—';
  const v = Number(n);
  return (v > 0 ? '+' : '') + v.toLocaleString('en-US');
}

// Professional, presentation-friendly palette.
export const COLORS = {
  primary: '#4f46e5',
  primary400: '#6366f1',
  primary600: '#4338ca',
  grid: '#eceff5',
  axis: '#8a97ad',
  pos: '#10b981',
  neg: '#f43f5e',
  series: ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6', '#f43f5e', '#0ea5a4', '#ec4899'],
};

// Deterministic avatar gradient for an author name.
const AVATARS = [
  ['#6366f1', '#4338ca'],
  ['#0ea5e9', '#0369a1'],
  ['#10b981', '#047857'],
  ['#f59e0b', '#b45309'],
  ['#8b5cf6', '#6d28d9'],
  ['#f43f5e', '#be123c'],
  ['#0ea5a4', '#0f766e'],
  ['#ec4899', '#be185d'],
];

export function avatarFor(name) {
  let h = 0;
  for (let i = 0; i < (name || '').length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
  const [a, b] = AVATARS[h % AVATARS.length];
  return { background: `linear-gradient(135deg, ${a}, ${b})` };
}

export function initials(name) {
  const parts = (name || '?').trim().split(/[\s._-]+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

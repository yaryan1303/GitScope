// Thin client for the GitScope backend. Vite proxies /api -> :8080 in dev.

export async function analyzeRepo({ mode, value }) {
  const body = mode === 'path' ? { path: value } : { url: value };

  const res = await fetch('/api/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  let payload = null;
  try {
    payload = await res.json();
  } catch {
    // Non-JSON response (e.g. proxy error).
  }

  if (!res.ok) {
    const message = (payload && payload.error) || `Request failed (${res.status})`;
    throw new Error(message);
  }
  return payload;
}

export async function checkHealth() {
  const res = await fetch('/api/health');
  if (!res.ok) throw new Error('Backend not reachable');
  return res.json();
}

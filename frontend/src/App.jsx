import React, { useState } from 'react';
import { analyzeRepo } from './api.js';
import RepoInput from './components/RepoInput.jsx';
import Dashboard from './components/Dashboard.jsx';
import { IconLogo, IconSearch } from './components/Icons.jsx';

export default function App() {
  const [mode, setMode] = useState('path');
  const [value, setValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  async function handleAnalyze() {
    const trimmed = value.trim();
    if (!trimmed) {
      setError(mode === 'path' ? 'Enter a local repository path.' : 'Enter a remote clone URL.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await analyzeRepo({ mode, value: trimmed });
      setResult(data);
    } catch (e) {
      setError(e.message || 'Analysis failed.');
      setResult(null);
    } finally {
      setLoading(false);
    }
  }

  const summary = result?.summary;

  return (
    <div className="app">
      <header className="masthead">
        <div className="brand">
          <div className="brand-mark"><IconLogo /></div>
          <div>
            <h1>GitScope</h1>
            <div className="subtitle">Repository analytics &amp; contributor insights</div>
          </div>
        </div>
        {summary && (
          <span className="meta-chip">
            {summary.name} · {summary.firstCommitDate} → {summary.lastCommitDate}
          </span>
        )}
      </header>

      <RepoInput
        mode={mode}
        setMode={setMode}
        value={value}
        setValue={setValue}
        loading={loading}
        onAnalyze={handleAnalyze}
      />

      {loading && (
        <div className="banner loading" role="status">
          <span className="spinner" aria-hidden="true" />
          Analyzing commit history — this can take a moment for large repositories.
        </div>
      )}

      {error && !loading && (
        <div className="banner error" role="alert">⚠ {error}</div>
      )}

      {!result && !loading && !error && (
        <div className="empty">
          <div className="empty-mark"><IconSearch /></div>
          Point GitScope at a repository to generate the dashboard.
        </div>
      )}

      {result && !loading && <Dashboard data={result} />}
    </div>
  );
}

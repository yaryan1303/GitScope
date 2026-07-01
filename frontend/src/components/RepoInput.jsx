import React from 'react';

export default function RepoInput({ mode, setMode, value, setValue, loading, onAnalyze }) {
  function handleSubmit(e) {
    e.preventDefault();
    if (!loading) onAnalyze();
  }

  const isPath = mode === 'path';

  return (
    <div className="repo-bar">
      <form onSubmit={handleSubmit}>
        <div className="toggle" role="group" aria-label="Repository source">
          <button
            type="button"
            aria-pressed={isPath}
            onClick={() => setMode('path')}
          >
            Local path
          </button>
          <button
            type="button"
            aria-pressed={!isPath}
            onClick={() => setMode('url')}
          >
            Remote URL
          </button>
        </div>

        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder={
            isPath
              ? 'C:\\path\\to\\repo  or  /home/me/project'
              : 'https://github.com/user/repo.git'
          }
          aria-label={isPath ? 'Local repository path' : 'Remote clone URL'}
          spellCheck="false"
          autoComplete="off"
        />

        <button className="analyze" type="submit" disabled={loading}>
          {loading ? 'Analyzing…' : 'Analyze'}
        </button>
      </form>
      <p className="hint">
        {isPath
          ? 'Opens the repo directly from disk — no copy made.'
          : 'Cloned into a temp directory, analyzed, then deleted.'}
      </p>
    </div>
  );
}

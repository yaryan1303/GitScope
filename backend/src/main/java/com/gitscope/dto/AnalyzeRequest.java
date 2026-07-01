package com.gitscope.dto;

/**
 * Request body for POST /api/analyze. Exactly one of {@code path} (a local repo
 * directory on disk) or {@code url} (a remote clone URL) should be supplied.
 */
public class AnalyzeRequest {

    private String path;
    private String url;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

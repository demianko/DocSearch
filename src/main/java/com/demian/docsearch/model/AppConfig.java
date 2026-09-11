package com.demian.docsearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class AppConfig {
    private String directory = "";
    private String pattern = "";
    private String extension = "";
    private String sortOrder = "modified";
    private String publisher = "";
    private int limit = 0;
    private String filterResult = "";

    public AppConfig() {
    }

    public AppConfig(String directory, String pattern, String extension, String sortOrder, String publisher, int limit, String filterResult) {
        this.directory = directory != null ? directory : "";
        this.pattern = pattern != null ? pattern : "";
        this.extension = extension != null ? extension : "";
        this.sortOrder = sortOrder != null ? sortOrder : "modified";
        this.publisher = publisher != null ? publisher : "";
        this.limit = Math.max(0, limit);
        this.filterResult = filterResult != null ? filterResult : "";
    }

    public String getDirectory() {
        return this.directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getPattern() {
        return this.pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getExtension() {
        return this.extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getSortOrder() {
        return this.sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getFilterResult() {
        return this.filterResult;
    }

    public void setFilterResult(String filterResult) {
        this.filterResult = filterResult;
    }
}


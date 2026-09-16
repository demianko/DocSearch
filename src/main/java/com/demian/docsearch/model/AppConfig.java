package com.demian.docsearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    private String directory = "";
    private String pattern = "";
    private String extension = "";
    private String sortOrder = "modified";
    private String publisher = "";
    private int limit = 0;

    public static final String DEFAULT_AI_SYSTEM_PROMPT = """
            You are an intelligent document and file search engine assistant.
            Given a user's natural language search query and a list of file metadata candidates, identify all files that match the user's intent.
            Consider file name semantics, synonyms, abbreviations, file extensions, extracted years, publishers, and parent directory context.
            
            You MUST return a JSON object with a single key "matched_ids" containing an array of integer IDs of the matching files:
            {"matched_ids": [101, 105, 120]}
            If no files match the user's intent, return:
            {"matched_ids": []}
            """.trim();

    private String aiBaseUrl = "";
    private String aiApiKey = "";
    private String aiModel = "";
    private double aiTemperature = 0.1;
    private int aiTimeoutSeconds = 30;
    private int aiParallelRequests = 4;
    private String aiSystemPrompt = DEFAULT_AI_SYSTEM_PROMPT;

    public AppConfig() {
    }

    public AppConfig(String directory, String pattern, String extension, String sortOrder, String publisher, int limit) {
        this.directory = directory != null ? directory : "";
        this.pattern = pattern != null ? pattern : "";
        this.extension = extension != null ? extension : "";
        this.sortOrder = sortOrder != null ? sortOrder : "modified";
        this.publisher = publisher != null ? publisher : "";
        this.limit = Math.max(0, limit);
    }

    public boolean isAiConfigured() {
        return StringUtils.isNotBlank(this.aiBaseUrl) && StringUtils.isNotBlank(this.aiApiKey);
    }

    public String getAiSystemPrompt() {
        return StringUtils.defaultIfBlank(this.aiSystemPrompt, DEFAULT_AI_SYSTEM_PROMPT);
    }

    public void setAiSystemPrompt(String aiSystemPrompt) {
        this.aiSystemPrompt = StringUtils.defaultIfBlank(aiSystemPrompt, DEFAULT_AI_SYSTEM_PROMPT);
    }

    public String getAiBaseUrl() {
        return this.aiBaseUrl != null ? this.aiBaseUrl : "";
    }

    public void setAiBaseUrl(String aiBaseUrl) {
        this.aiBaseUrl = aiBaseUrl != null ? aiBaseUrl.trim() : "";
    }

    public String getAiApiKey() {
        return this.aiApiKey != null ? this.aiApiKey : "";
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey != null ? aiApiKey.trim() : "";
    }

    public String getAiModel() {
        return this.aiModel != null ? this.aiModel : "";
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel != null ? aiModel.trim() : "";
    }

    public double getAiTemperature() {
        return this.aiTemperature >= 0.0 ? this.aiTemperature : 0.1;
    }

    public void setAiTemperature(double aiTemperature) {
        this.aiTemperature = Math.max(0.0, Math.min(2.0, aiTemperature));
    }

    public int getAiTimeoutSeconds() {
        return this.aiTimeoutSeconds > 0 ? this.aiTimeoutSeconds : 30;
    }

    public void setAiTimeoutSeconds(int aiTimeoutSeconds) {
        this.aiTimeoutSeconds = aiTimeoutSeconds > 0 ? aiTimeoutSeconds : 30;
    }

    public int getAiParallelRequests() {
        return this.aiParallelRequests > 0 ? this.aiParallelRequests : 4;
    }

    public void setAiParallelRequests(int aiParallelRequests) {
        this.aiParallelRequests = Math.max(1, Math.min(32, aiParallelRequests));
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
}

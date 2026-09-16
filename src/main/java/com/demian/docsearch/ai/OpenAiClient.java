package com.demian.docsearch.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class OpenAiClient {
    private static final String USER_AGENT = "DocSearch-Pro/1.0.0 (Java HttpClient)";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenAiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), new ObjectMapper());
    }

    public OpenAiClient(HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public static String normalizeChatCompletionsUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) return "https://api.openai.com/v1/chat/completions";
        String trimmed = baseUrl.trim();

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String authority = uri.getRawAuthority();
            String path = uri.getRawPath() != null ? uri.getRawPath() : "";
            String query = uri.getRawQuery();

            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            if (path.isEmpty()) {
                path = "/v1/chat/completions";
            } else if (!path.endsWith("/chat/completions")) {
                if (path.endsWith("/models")) {
                    path = path.substring(0, path.length() - "/models".length());
                } else if (path.endsWith("/completions")) {
                    path = path.substring(0, path.length() - "/completions".length());
                }
                while (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (path.isEmpty()) {
                    path = "/v1/chat/completions";
                } else {
                    path = path + "/chat/completions";
                }
            }

            StringBuilder sb = new StringBuilder();
            if (scheme != null && authority != null) {
                sb.append(scheme).append("://").append(authority);
            }
            sb.append(path);
            if (StringUtils.isNotBlank(query)) {
                sb.append("?").append(query);
            }
            return sb.toString();
        } catch (Exception e) {
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (trimmed.endsWith("/chat/completions")) {
                return trimmed;
            }
            return trimmed + "/v1/chat/completions";
        }
    }

    public static String normalizeModelsUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) return "https://api.openai.com/v1/models";
        String trimmed = baseUrl.trim();

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String authority = uri.getRawAuthority();
            String path = uri.getRawPath() != null ? uri.getRawPath() : "";
            String query = uri.getRawQuery();

            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            if (path.isEmpty()) {
                path = "/v1/models";
            } else {
                if (path.endsWith("/chat/completions")) {
                    path = path.substring(0, path.length() - "/chat/completions".length());
                } else if (path.endsWith("/completions")) {
                    path = path.substring(0, path.length() - "/completions".length());
                }

                while (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                if (path.isEmpty()) {
                    path = "/v1/models";
                } else if (!path.endsWith("/models")) {
                    path = path + "/models";
                }
            }

            StringBuilder sb = new StringBuilder();
            if (scheme != null && authority != null) {
                sb.append(scheme).append("://").append(authority);
            }
            sb.append(path);
            if (StringUtils.isNotBlank(query)) {
                sb.append("?").append(query);
            }
            return sb.toString();
        } catch (Exception e) {
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (trimmed.endsWith("/chat/completions")) {
                trimmed = trimmed.substring(0, trimmed.length() - "/chat/completions".length());
            }
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (trimmed.endsWith("/models")) {
                return trimmed;
            }
            return trimmed + "/v1/models";
        }
    }

    public static List<String> buildCandidateChatCompletionsUrls(String baseUrl) {
        List<String> candidates = new ArrayList<>();
        String primary = normalizeChatCompletionsUrl(baseUrl);
        candidates.add(primary);

        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String authority = uri.getRawAuthority();
            String query = uri.getRawQuery();

            if (authority != null) {
                String hostBase = scheme + "://" + authority;

                String v1Chat = hostBase + "/v1/chat/completions" + (StringUtils.isNotBlank(query) ? "?" + query : "");
                if (!candidates.contains(v1Chat)) {
                    candidates.add(v1Chat);
                }

                String rootChat = hostBase + "/chat/completions" + (StringUtils.isNotBlank(query) ? "?" + query : "");
                if (!candidates.contains(rootChat)) {
                    candidates.add(rootChat);
                }
            }
        } catch (Exception ignored) {
        }

        return candidates;
    }

    public static List<String> buildCandidateModelUrls(String baseUrl) {
        List<String> candidates = new ArrayList<>();
        String primary = normalizeModelsUrl(baseUrl);
        candidates.add(primary);

        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String authority = uri.getRawAuthority();
            String query = uri.getRawQuery();

            if (authority != null) {
                String hostBase = scheme + "://" + authority;

                String v1Models = hostBase + "/v1/models" + (StringUtils.isNotBlank(query) ? "?" + query : "");
                if (!candidates.contains(v1Models)) {
                    candidates.add(v1Models);
                }

                String ollamaTags = hostBase + "/api/tags" + (StringUtils.isNotBlank(query) ? "?" + query : "");
                if (!candidates.contains(ollamaTags)) {
                    candidates.add(ollamaTags);
                }

                String rootModels = hostBase + "/models" + (StringUtils.isNotBlank(query) ? "?" + query : "");
                if (!candidates.contains(rootModels)) {
                    candidates.add(rootModels);
                }
            }
        } catch (Exception ignored) {
        }

        return candidates;
    }

    public List<String> parseModelsFromJson(String jsonBody) {
        Set<String> modelSet = new LinkedHashSet<>();
        try {
            JsonNode root = this.mapper.readTree(jsonBody);

            if (root.isArray()) {
                this.extractModelsFromArray(root, modelSet);
            } else if (root.isObject()) {
                String[] arrayKeys = new String[]{"data", "models", "value", "result", "items"};
                boolean found = false;
                for (String key : arrayKeys) {
                    if (root.has(key) && root.get(key).isArray()) {
                        this.extractModelsFromArray(root.get(key), modelSet);
                        found = true;
                    }
                }

                if (!found) {
                    root.fields().forEachRemaining(entry -> {
                        if (entry.getValue().isArray()) {
                            this.extractModelsFromArray(entry.getValue(), modelSet);
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }

        List<String> result = new ArrayList<>(modelSet);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void extractModelsFromArray(JsonNode arrayNode, Set<String> modelSet) {
        for (JsonNode item : arrayNode) {
            if (item.isTextual()) {
                String val = item.asText().trim();
                if (StringUtils.isNotBlank(val)) modelSet.add(val);
            } else if (item.isObject()) {
                String[] idKeys = new String[]{"id", "name", "model", "model_name"};
                for (String key : idKeys) {
                    if (item.has(key) && item.get(key).isTextual()) {
                        String val = item.get(key).asText().trim();
                        if (StringUtils.isNotBlank(val)) {
                            modelSet.add(val);
                            break;
                        }
                    }
                }
            }
        }
    }

    public List<String> fetchAvailableModels(String baseUrl, String apiKey) throws IOException, InterruptedException {
        List<String> candidateUrls = buildCandidateModelUrls(baseUrl);
        IOException lastException = null;

        for (String endpoint : candidateUrls) {
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .GET();

                if (StringUtils.isNotBlank(apiKey)) {
                    reqBuilder.header("Authorization", "Bearer " + apiKey.trim());
                }

                HttpResponse<String> response = this.httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    List<String> models = this.parseModelsFromJson(response.body());
                    if (!models.isEmpty()) {
                        return models;
                    }
                } else if (status != 404 && status != 405) {
                    String errorDetail = response.body();
                    try {
                        JsonNode errorJson = this.mapper.readTree(response.body());
                        if (errorJson.has("error") && errorJson.get("error").has("message")) {
                            errorDetail = errorJson.get("error").get("message").asText();
                        }
                    } catch (Exception ignored) {
                    }
                    lastException = new IOException("HTTP " + status + ": " + errorDetail);
                }
            } catch (IOException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new IOException("Failed to fetch models from endpoint: no models returned from " + normalizeModelsUrl(baseUrl));
    }

    public record ConnectionTestResult(boolean success, String message) {
    }

    public ConnectionTestResult testConnection(String baseUrl, String apiKey, String model) {
        if (StringUtils.isBlank(baseUrl)) return new ConnectionTestResult(false, "Base URL cannot be empty.");
        String modelName = StringUtils.defaultIfBlank(model, "gpt-4o-mini");

        List<String> candidateEndpoints = buildCandidateChatCompletionsUrls(baseUrl);
        String lastError = null;

        for (String endpoint : candidateEndpoints) {
            try {
                ObjectNode rootNode = this.mapper.createObjectNode();
                rootNode.put("model", modelName);
                rootNode.put("max_tokens", 5);

                ArrayNode messages = rootNode.putArray("messages");
                ObjectNode msg = messages.addObject();
                msg.put("role", "user");
                msg.put("content", "ping");

                String jsonPayload = this.mapper.writeValueAsString(rootNode);

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", USER_AGENT);

                if (StringUtils.isNotBlank(apiKey)) {
                    reqBuilder.header("Authorization", "Bearer " + apiKey.trim());
                }

                HttpRequest request = reqBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return new ConnectionTestResult(true, "Successfully connected to " + modelName + " (" + status + " OK)");
                }

                String errorDetail = response.body();
                try {
                    JsonNode errorJson = this.mapper.readTree(response.body());
                    if (errorJson.has("error") && errorJson.get("error").has("message")) {
                        errorDetail = errorJson.get("error").get("message").asText();
                    }
                } catch (Exception ignored) {
                }

                lastError = "HTTP " + status + ": " + errorDetail;
                if (status == 404 || errorDetail.contains("Unexpected endpoint")) {
                    continue;
                }
                return new ConnectionTestResult(false, lastError);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ConnectionTestResult(false, "Connection test interrupted: " + e.getMessage());
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        return new ConnectionTestResult(false, "Failed to connect: " + lastError);
    }

    public String sendChatCompletion(
            String baseUrl, String apiKey, String model, int timeoutSeconds,
            String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        return this.sendChatCompletion(baseUrl, apiKey, model, 0.1, timeoutSeconds, systemPrompt, userPrompt);
    }

    public String sendChatCompletion(
            String baseUrl, String apiKey, String model, double temperature, int timeoutSeconds,
            String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        List<String> candidateEndpoints = buildCandidateChatCompletionsUrls(baseUrl);
        String modelName = StringUtils.defaultIfBlank(model, "gpt-4o-mini");
        int timeout = timeoutSeconds > 0 ? timeoutSeconds : 30;

        IOException lastException = null;

        for (String endpoint : candidateEndpoints) {
            try {
                try {
                    return this.executeChatCompletionRequest(endpoint, apiKey, modelName, temperature, timeout, systemPrompt, userPrompt, true);
                } catch (IOException e) {
                    if (e.getMessage() != null && (e.getMessage().contains("400") || e.getMessage().contains("422") || e.getMessage().contains("response_format"))) {
                        return this.executeChatCompletionRequest(endpoint, apiKey, modelName, temperature, timeout, systemPrompt, userPrompt, false);
                    }
                    throw e;
                }
            } catch (IOException e) {
                lastException = e;
                if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("Unexpected endpoint"))) {
                    continue;
                }
                throw e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new IOException("Failed to connect to AI completion endpoint: " + candidateEndpoints.get(0));
    }

    private String executeChatCompletionRequest(
            String endpoint, String apiKey, String modelName, double temperature, int timeout,
            String systemPrompt, String userPrompt, boolean includeResponseFormat) throws IOException, InterruptedException {

        ObjectNode rootNode = this.mapper.createObjectNode();
        rootNode.put("model", modelName);
        rootNode.put("temperature", Math.max(0.0, Math.min(2.0, temperature)));

        if (includeResponseFormat) {
            ObjectNode respFormat = rootNode.putObject("response_format");
            respFormat.put("type", "json_object");
        }

        ArrayNode messages = rootNode.putArray("messages");
        if (StringUtils.isNotBlank(systemPrompt)) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
        }

        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userPrompt);

        String jsonPayload = this.mapper.writeValueAsString(rootNode);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT);

        if (StringUtils.isNotBlank(apiKey)) {
            reqBuilder.header("Authorization", "Bearer " + apiKey.trim());
        }

        HttpRequest request = reqBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String errorDetail = response.body();
            try {
                JsonNode errorJson = this.mapper.readTree(response.body());
                if (errorJson.has("error") && errorJson.get("error").has("message")) {
                    errorDetail = errorJson.get("error").get("message").asText();
                }
            } catch (Exception ignored) {
            }
            throw new IOException("AI API Error (" + status + "): " + errorDetail);
        }

        return this.extractContentFromResponse(response.body());
    }

    public String extractContentFromResponse(String responseBody) throws IOException {
        if (StringUtils.isBlank(responseBody)) {
            throw new IOException("Empty response received from AI API.");
        }

        JsonNode resJson;
        try {
            resJson = this.mapper.readTree(responseBody);
        } catch (Exception e) {
            if (responseBody.trim().startsWith("{") || responseBody.trim().startsWith("[")) {
                return responseBody.trim();
            }
            throw new IOException("AI API returned non-JSON response: " + responseBody);
        }

        if (resJson.has("error")) {
            JsonNode errNode = resJson.get("error");
            String errMsg = errNode.isTextual() ? errNode.asText() :
                    (errNode.has("message") ? errNode.get("message").asText() : errNode.toString());
            throw new IOException("AI API Error: " + errMsg);
        }

        if (resJson.has("choices") && resJson.get("choices").isArray() && !resJson.get("choices").isEmpty()) {
            JsonNode choice = resJson.get("choices").get(0);

            if (choice.has("message") && choice.get("message").isObject()) {
                JsonNode msg = choice.get("message");

                if (msg.has("content") && !msg.get("content").isNull()) {
                    JsonNode contentNode = msg.get("content");
                    if (contentNode.isTextual()) {
                        String text = contentNode.asText();
                        if (StringUtils.isNotBlank(text)) return text;
                    } else if (contentNode.isArray() && !contentNode.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode part : contentNode) {
                            if (part.has("text") && !part.get("text").isNull()) {
                                sb.append(part.get("text").asText());
                            } else if (part.isTextual()) {
                                sb.append(part.asText());
                            }
                        }
                        if (sb.length() > 0) return sb.toString();
                    } else if (contentNode.isObject()) {
                        return contentNode.toString();
                    }
                }

                if (msg.has("reasoning_content") && !msg.get("reasoning_content").isNull()) {
                    String reasoning = msg.get("reasoning_content").asText();
                    if (StringUtils.isNotBlank(reasoning)) return reasoning;
                }
            }

            if (choice.has("text") && !choice.get("text").isNull()) {
                String text = choice.get("text").asText();
                if (StringUtils.isNotBlank(text)) return text;
            }

            if (choice.has("delta") && choice.get("delta").has("content") && !choice.get("delta").get("content").isNull()) {
                String delta = choice.get("delta").get("content").asText();
                if (StringUtils.isNotBlank(delta)) return delta;
            }
        }

        if (resJson.has("response") && !resJson.get("response").isNull()) {
            String resp = resJson.get("response").asText();
            if (StringUtils.isNotBlank(resp)) return resp;
        }

        String[] textKeys = new String[]{"output", "generated_text", "result", "message", "text", "content"};
        for (String key : textKeys) {
            if (resJson.has(key) && !resJson.get(key).isNull()) {
                JsonNode node = resJson.get(key);
                if (node.isTextual() && StringUtils.isNotBlank(node.asText())) {
                    return node.asText();
                } else if (node.has("text") && !node.get("text").isNull()) {
                    return node.get("text").asText();
                } else if (node.has("response") && !node.get("response").isNull()) {
                    return node.get("response").asText();
                } else if (node.isObject()) {
                    return node.toString();
                }
            }
        }

        if (resJson.has("matched_ids")) {
            return responseBody;
        }

        String truncatedBody = responseBody.length() > 300 ? responseBody.substring(0, 300) + "..." : responseBody;
        throw new IOException("Unexpected response format received from AI API: " + truncatedBody);
    }
}

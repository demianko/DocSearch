package com.demian.docsearch.ai;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiClientTest {

    @Test
    void testNormalizeChatCompletionsUrl() {
        assertThat(OpenAiClient.normalizeChatCompletionsUrl("https://api.openai.com/v1"))
                .isEqualTo("https://api.openai.com/v1/chat/completions");

        assertThat(OpenAiClient.normalizeChatCompletionsUrl("https://api.openai.com/v1/"))
                .isEqualTo("https://api.openai.com/v1/chat/completions");

        assertThat(OpenAiClient.normalizeChatCompletionsUrl("https://api.openai.com/v1/chat/completions"))
                .isEqualTo("https://api.openai.com/v1/chat/completions");

        assertThat(OpenAiClient.normalizeChatCompletionsUrl("http://localhost:11434/v1"))
                .isEqualTo("http://localhost:11434/v1/chat/completions");

        // Bare host without /v1 should automatically append /v1/chat/completions for Ollama/standard OpenAI compatibility!
        assertThat(OpenAiClient.normalizeChatCompletionsUrl("http://localhost:11434"))
                .isEqualTo("http://localhost:11434/v1/chat/completions");

        assertThat(OpenAiClient.normalizeChatCompletionsUrl("http://localhost:11434/"))
                .isEqualTo("http://localhost:11434/v1/chat/completions");

        assertThat(OpenAiClient.normalizeChatCompletionsUrl("https://custom.ai.gateway/v1/chat/completions?api-version=2024-02"))
                .isEqualTo("https://custom.ai.gateway/v1/chat/completions?api-version=2024-02");

        assertThat(OpenAiClient.normalizeChatCompletionsUrl(""))
                .isEqualTo("https://api.openai.com/v1/chat/completions");
    }

    @Test
    void testNormalizeModelsUrl() {
        assertThat(OpenAiClient.normalizeModelsUrl("https://api.openai.com/v1"))
                .isEqualTo("https://api.openai.com/v1/models");

        assertThat(OpenAiClient.normalizeModelsUrl("https://api.openai.com/v1/"))
                .isEqualTo("https://api.openai.com/v1/models");

        assertThat(OpenAiClient.normalizeModelsUrl("https://api.openai.com/v1/chat/completions"))
                .isEqualTo("https://api.openai.com/v1/models");

        assertThat(OpenAiClient.normalizeModelsUrl("http://localhost:11434/v1"))
                .isEqualTo("http://localhost:11434/v1/models");

        // Bare host without /v1 should automatically append /v1/models!
        assertThat(OpenAiClient.normalizeModelsUrl("http://localhost:11434"))
                .isEqualTo("http://localhost:11434/v1/models");

        assertThat(OpenAiClient.normalizeModelsUrl("http://localhost:11434/"))
                .isEqualTo("http://localhost:11434/v1/models");

        assertThat(OpenAiClient.normalizeModelsUrl("http://localhost:11434/v1/chat/completions?token=123"))
                .isEqualTo("http://localhost:11434/v1/models?token=123");

        assertThat(OpenAiClient.normalizeModelsUrl(""))
                .isEqualTo("https://api.openai.com/v1/models");
    }

    @Test
    void testBuildCandidateChatCompletionsUrls() {
        List<String> candidates = OpenAiClient.buildCandidateChatCompletionsUrls("http://localhost:11434");
        assertThat(candidates).contains(
                "http://localhost:11434/v1/chat/completions",
                "http://localhost:11434/chat/completions"
        );
    }

    @Test
    void testBuildCandidateModelUrls() {
        List<String> candidates = OpenAiClient.buildCandidateModelUrls("http://localhost:11434");
        assertThat(candidates).contains(
                "http://localhost:11434/v1/models",
                "http://localhost:11434/models",
                "http://localhost:11434/api/tags"
        );
    }

    @Test
    void testParseModelsFromJsonFormats() {
        OpenAiClient client = new OpenAiClient();

        // 1. Standard OpenAI response
        String openAiJson = """
                {
                  "object": "list",
                  "data": [
                    {"id": "gpt-4o", "object": "model"},
                    {"id": "gpt-4o-mini", "object": "model"}
                  ]
                }
                """;
        List<String> openAiModels = client.parseModelsFromJson(openAiJson);
        assertThat(openAiModels).containsExactly("gpt-4o", "gpt-4o-mini");

        // 2. Ollama /api/tags response
        String ollamaJson = """
                {
                  "models": [
                    {"name": "llama3:latest", "model": "llama3:latest"},
                    {"name": "qwen2.5:latest", "model": "qwen2.5:latest"}
                  ]
                }
                """;
        List<String> ollamaModels = client.parseModelsFromJson(ollamaJson);
        assertThat(ollamaModels).containsExactly("llama3:latest", "qwen2.5:latest");

        // 3. Azure OpenAI / REST "value" response
        String azureJson = """
                {
                  "value": [
                    {"id": "gpt-4-turbo", "model": "gpt-4-turbo"},
                    {"id": "claude-3-5-sonnet"}
                  ]
                }
                """;
        List<String> azureModels = client.parseModelsFromJson(azureJson);
        assertThat(azureModels).containsExactly("claude-3-5-sonnet", "gpt-4-turbo");

        // 4. Simple JSON array of strings
        String stringArrayJson = """
                ["deepseek-chat", "mistral-large"]
                """;
        List<String> arrayModels = client.parseModelsFromJson(stringArrayJson);
        assertThat(arrayModels).containsExactly("deepseek-chat", "mistral-large");
    }

    @Test
    void testExtractContentFromVariousResponseFormats() throws IOException {
        OpenAiClient client = new OpenAiClient();

        // Standard OpenAI message.content
        String standard = "{\"choices\": [{\"message\": {\"content\": \"{\\\"matched_ids\\\": [1, 2]}\"}}]}";
        assertThat(client.extractContentFromResponse(standard)).isEqualTo("{\"matched_ids\": [1, 2]}");

        // DeepSeek reasoning_content when content is null/empty
        String reasoning = "{\"choices\": [{\"message\": {\"content\": null, \"reasoning_content\": \"{\\\"matched_ids\\\": [3]}\"}}]}";
        assertThat(client.extractContentFromResponse(reasoning)).isEqualTo("{\"matched_ids\": [3]}");

        // Text completion format
        String textComp = "{\"choices\": [{\"text\": \"{\\\"matched_ids\\\": [4]}\"}]}";
        assertThat(client.extractContentFromResponse(textComp)).isEqualTo("{\"matched_ids\": [4]}");

        // Multi-part content array
        String multiPart = "{\"choices\": [{\"message\": {\"content\": [{\"type\": \"text\", \"text\": \"{\\\"matched_ids\\\": [5]}\"}]}}]}";
        assertThat(client.extractContentFromResponse(multiPart)).isEqualTo("{\"matched_ids\": [5]}");

        // Ollama native response
        String ollama = "{\"response\": \"{\\\"matched_ids\\\": [6]}\"}";
        assertThat(client.extractContentFromResponse(ollama)).isEqualTo("{\"matched_ids\": [6]}");

        // Direct matched_ids root
        String direct = "{\"matched_ids\": [7, 8]}";
        assertThat(client.extractContentFromResponse(direct)).isEqualTo("{\"matched_ids\": [7, 8]}");

        // Error payload in HTTP 200
        String err = "{\"error\": {\"message\": \"Model rate limit reached\"}}";
        assertThatThrownBy(() -> client.extractContentFromResponse(err))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Model rate limit reached");
    }

    @Test
    void testTestConnectionValidation() {
        OpenAiClient client = new OpenAiClient();
        OpenAiClient.ConnectionTestResult emptyUrl = client.testConnection("", "key", "model");
        assertThat(emptyUrl.success()).isFalse();
        assertThat(emptyUrl.message()).contains("Base URL");

        // Note: empty API key is allowed for local servers like Ollama
    }
}

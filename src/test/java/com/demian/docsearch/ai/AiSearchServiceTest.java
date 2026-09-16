package com.demian.docsearch.ai;

import com.demian.docsearch.db.FileIndexDatabase;
import com.demian.docsearch.db.IndexedFileRecord;
import com.demian.docsearch.model.AppConfig;
import com.demian.docsearch.model.FileItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class AiSearchServiceTest {

    @Test
    void testBuildUserPrompt() {
        AiSearchService service = new AiSearchService();
        IndexedFileRecord record1 = new IndexedFileRecord(
                1L, "C:/Docs", "C:/Docs/Finance", "Tax_2023.pdf", "C:/Docs/Finance/Tax_2023.pdf", "pdf", 1024L, 1000L, 2023, "Finance", 2000L);
        IndexedFileRecord record2 = new IndexedFileRecord(
                2L, "C:/Docs", "C:/Docs/Notes", "Ideas.txt", "C:/Docs/Notes/Ideas.txt", "txt", 500L, 1000L, 0, "", 2000L);

        String prompt = service.buildUserPrompt("find 2023 taxes", List.of(record1, record2));
        assertThat(prompt).contains("find 2023 taxes");
        assertThat(prompt).contains("Tax_2023.pdf");
        assertThat(prompt).contains("Ideas.txt");
        assertThat(prompt).contains("Finance");
    }

    @Test
    void testParseMatchedIds() {
        AiSearchService service = new AiSearchService();
        String jsonValid = "{\"matched_ids\": [1, 5, 12]}";
        List<Long> ids = service.parseMatchedIds(jsonValid);
        assertThat(ids).containsExactly(1L, 5L, 12L);

        // Markdown code block
        String jsonMarkdown = """
                Here are the matching files:
                ```json
                {
                  "matched_ids": [2, 8]
                }
                ```
                Hope this helps!
                """;
        assertThat(service.parseMatchedIds(jsonMarkdown)).containsExactly(2L, 8L);

        // Reasoning model (<think> tags)
        String reasoningJson = """
                <think>
                User wants AI books. ID 10 is AI agents. ID 15 is Deep Learning.
                </think>
                {
                  "matchedIds": [10, 15]
                }
                """;
        assertThat(service.parseMatchedIds(reasoningJson)).containsExactly(10L, 15L);

        // Direct array
        String arrayJson = "[3, 7, 9]";
        assertThat(service.parseMatchedIds(arrayJson)).containsExactly(3L, 7L, 9L);

        String jsonEmpty = "{\"matched_ids\": []}";
        assertThat(service.parseMatchedIds(jsonEmpty)).isEmpty();

        String jsonMalformed = "Invalid response from AI without numbers";
        assertThat(service.parseMatchedIds(jsonMalformed)).isEmpty();
    }

    @Test
    void testSearchWithMockClient(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path rootFolder = tempDir.resolve("Projects");
        Files.createDirectories(rootFolder);
        Path file1 = rootFolder.resolve("Doc_2023.pdf");
        Path file2 = rootFolder.resolve("Notes.txt");
        Files.writeString(file1, "doc content");
        Files.writeString(file2, "notes content");

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("test.db"));
        db.indexDirectory(rootFolder, null, null);

        List<IndexedFileRecord> indexed = db.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "");
        assertThat(indexed).hasSize(2);
        long targetId = indexed.stream().filter(r -> r.fileName().equals("Doc_2023.pdf")).findFirst().get().id();

        final String customPrompt = "Custom instructions for test prompt matching";
        OpenAiClient mockClient = new OpenAiClient() {
            @Override
            public String sendChatCompletion(
                    String baseUrl, String apiKey, String model, double temperature, int timeoutSeconds,
                    String systemPrompt, String userPrompt) {
                assertThat(systemPrompt).isEqualTo(customPrompt);
                return "{\"matched_ids\": [" + targetId + "]}";
            }
        };

        AiSearchService service = new AiSearchService(mockClient, new ObjectMapper());
        AppConfig config = new AppConfig();
        config.setAiApiKey("test-key");
        config.setAiSystemPrompt(customPrompt);

        List<FileItem> results = service.search(
                db,
                rootFolder.toAbsolutePath().toString(),
                "2023 documents",
                "pdf",
                config,
                null,
                () -> false
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Doc_2023.pdf");
    }

    @Test
    void testProgressiveStreamingDuringSearch(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path rootFolder = tempDir.resolve("StreamTest");
        Files.createDirectories(rootFolder);
        Path file1 = rootFolder.resolve("Report_2023.pdf");
        Path file2 = rootFolder.resolve("Tax_2023.pdf");
        Files.writeString(file1, "report");
        Files.writeString(file2, "tax");

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("stream.db"));
        db.indexDirectory(rootFolder, null, null);

        List<IndexedFileRecord> indexed = db.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "");
        assertThat(indexed).hasSize(2);
        long id1 = indexed.get(0).id();
        long id2 = indexed.get(1).id();

        OpenAiClient mockClient = new OpenAiClient() {
            @Override
            public String sendChatCompletion(
                    String baseUrl, String apiKey, String model, double temperature, int timeoutSeconds,
                    String systemPrompt, String userPrompt) {
                return "{\"matched_ids\": [" + id1 + ", " + id2 + "]}";
            }
        };

        AiSearchService service = new AiSearchService(mockClient, new ObjectMapper());
        AppConfig config = new AppConfig();
        config.setAiApiKey("test-key");

        List<FileItem> streamedItems = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<FileItem> finalResults = service.search(
                db,
                rootFolder.toAbsolutePath().toString(),
                "2023 docs",
                "",
                config,
                (curr, tot, item) -> {
                    if (item != null) {
                        streamedItems.add(item);
                    }
                },
                () -> false
        );

        assertThat(finalResults).hasSize(2);
        assertThat(streamedItems).hasSize(2);
        assertThat(streamedItems).extracting(FileItem::name)
                .containsExactlyInAnyOrder("Report_2023.pdf", "Tax_2023.pdf");
    }

    @Test
    void testCalculateChunkSize() {
        // Fast small chunks bounded between 15 and 30
        assertThat(AiSearchService.calculateChunkSize(0, 4)).isEqualTo(25);
        assertThat(AiSearchService.calculateChunkSize(20, 4)).isEqualTo(15);
        assertThat(AiSearchService.calculateChunkSize(80, 4)).isEqualTo(15);
        assertThat(AiSearchService.calculateChunkSize(200, 4)).isEqualTo(25);
        assertThat(AiSearchService.calculateChunkSize(320, 4)).isEqualTo(30);
        assertThat(AiSearchService.calculateChunkSize(320, 8)).isEqualTo(20);
    }
}

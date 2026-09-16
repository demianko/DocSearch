package com.demian.docsearch.ai;

import com.demian.docsearch.db.FileIndexDatabase;
import com.demian.docsearch.db.IndexedFileRecord;
import com.demian.docsearch.engine.FileSearchEngine;
import com.demian.docsearch.model.AppConfig;
import com.demian.docsearch.model.FileItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSearchService {
    private static final Logger log = LoggerFactory.getLogger(AiSearchService.class);
    private static final int MAX_CANDIDATES_FOR_LLM = 320;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "of", "in", "on", "for", "with", "about",
            "related", "to", "at", "by", "from", "is", "are", "was", "were", "be",
            "files", "file", "document", "documents", "doc", "docs", "books", "book",
            "folder", "folders", "directory", "directories", "find", "search", "get",
            "list", "show", "me", "all", "any", "please", "give", "which", "that",
            "have", "having", "contain", "containing", "named", "called", "some", "my"
    ));

    private final OpenAiClient client;
    private final ObjectMapper mapper;
    private final ExecutorService executor;

    public AiSearchService() {
        this(new OpenAiClient(), new ObjectMapper());
    }

    public AiSearchService(OpenAiClient client, ObjectMapper mapper) {
        this(client, mapper, Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "AiSearch-Worker");
            t.setDaemon(true);
            return t;
        }));
    }

    public AiSearchService(OpenAiClient client, ObjectMapper mapper, ExecutorService executor) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    public static int calculateChunkSize(int candidateCount, int parallelRequests) {
        if (candidateCount <= 0) return 25;
        int parallel = Math.max(1, parallelRequests);
        int targetChunks = parallel * 2;
        int dynamicSize = (int) Math.ceil((double) candidateCount / targetChunks);
        return Math.max(15, Math.min(30, dynamicSize));
    }

    public List<FileItem> search(
            FileIndexDatabase db,
            String targetFolder,
            String naturalLanguagePrompt,
            String extensionFilter,
            AppConfig config,
            FileSearchEngine.ProgressListener listener,
            BooleanSupplier cancelCheck) throws IOException, InterruptedException {
        Objects.requireNonNull(db, "db must not be null");
        Objects.requireNonNull(config, "config must not be null");

        if (StringUtils.isBlank(targetFolder) || StringUtils.isBlank(naturalLanguagePrompt)) {
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        List<IndexedFileRecord> allDbRecords = db.getIndexedFiles(targetFolder, extensionFilter);
        log.info("AI Search initiated: targetFolder='{}', prompt='{}', totalDbRecords={}",
                targetFolder, naturalLanguagePrompt, allDbRecords.size());

        if (allDbRecords.isEmpty()) {
            return List.of();
        }

        if (listener != null) {
            listener.onProgress(0, allDbRecords.size(), null);
        }

        if (cancelCheck != null && cancelCheck.getAsBoolean()) return List.of();

        // Step 1: Pre-filter and rank candidates using hybrid lexical/semantic scoring
        List<IndexedFileRecord> selectedCandidates = this.scoreAndFilterCandidates(allDbRecords, naturalLanguagePrompt);
        log.info("Candidate scoring completed: reduced from {} to {} prioritized files for LLM evaluation",
                allDbRecords.size(), selectedCandidates.size());

        Map<Long, IndexedFileRecord> recordMap = new HashMap<>();
        for (IndexedFileRecord rec : allDbRecords) {
            recordMap.put(rec.id(), rec);
        }

        final int parallelRequests = Math.max(1, Math.min(32, config.getAiParallelRequests()));
        final int chunkSize = calculateChunkSize(selectedCandidates.size(), parallelRequests);

        // Step 2: Partition into smaller, fast chunks for concurrent LLM processing
        List<List<IndexedFileRecord>> chunks = new ArrayList<>();
        for (int i = 0; i < selectedCandidates.size(); i += chunkSize) {
            chunks.add(selectedCandidates.subList(i, Math.min(i + chunkSize, selectedCandidates.size())));
        }

        Set<Long> matchedIds = Collections.synchronizedSet(new LinkedHashSet<>());
        AtomicInteger processedCounter = new AtomicInteger(0);
        final String systemPrompt = config.getAiSystemPrompt();
        final Semaphore semaphore = new Semaphore(parallelRequests);

        log.debug("Dispatching {} parallel LLM chunks ({} files/chunk, concurrency={}) to endpoint {}",
                chunks.size(), chunkSize, parallelRequests, config.getAiBaseUrl());

        // Step 3: Execute LLM chunk requests concurrently with bounded parallelism
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            final List<IndexedFileRecord> chunk = chunks.get(chunkIndex);
            final int index = chunkIndex + 1;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (cancelCheck != null && cancelCheck.getAsBoolean()) return;

                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    if (cancelCheck != null && cancelCheck.getAsBoolean()) return;

                    long chunkStart = System.currentTimeMillis();
                    String userPrompt = this.buildUserPrompt(naturalLanguagePrompt, chunk);
                    String aiResponse = this.client.sendChatCompletion(
                            config.getAiBaseUrl(),
                            config.getAiApiKey(),
                            config.getAiModel(),
                            config.getAiTemperature(),
                            config.getAiTimeoutSeconds(),
                            systemPrompt,
                            userPrompt
                    );

                    List<Long> chunkMatched = this.parseMatchedIds(aiResponse);
                    matchedIds.addAll(chunkMatched);
                    long chunkDuration = System.currentTimeMillis() - chunkStart;
                    log.debug("Chunk {}/{} completed in {}ms: found {} matches in {} files",
                            index, chunks.size(), chunkDuration, chunkMatched.size(), chunk.size());

                    if (listener != null) {
                        for (Long id : chunkMatched) {
                            IndexedFileRecord rec = recordMap.get(id);
                            if (rec != null) {
                                listener.onProgress(processedCounter.get(), selectedCandidates.size(), rec.toFileItem());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error executing LLM request for chunk {}/{}: {}", index, chunks.size(), e.getMessage());
                    throw new RuntimeException(e);
                } finally {
                    semaphore.release();
                    int currentProcessed = processedCounter.addAndGet(chunk.size());
                    if (listener != null) {
                        listener.onProgress(currentProcessed, selectedCandidates.size(), null);
                    }
                }
            }, this.executor);

            futures.add(future);
        }

        // Wait for all concurrent chunks to complete or handle cancellation
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IOException ioException) {
                throw ioException;
            } else if (cause instanceof InterruptedException interruptedException) {
                throw interruptedException;
            } else {
                throw new IOException("AI search chunk evaluation failed: " + cause.getMessage(), cause);
            }
        }

        if (cancelCheck != null && cancelCheck.getAsBoolean()) return List.of();

        // Step 4: Map matched IDs to FileItems
        List<FileItem> resultItems = new ArrayList<>();
        for (Long id : matchedIds) {
            IndexedFileRecord rec = recordMap.get(id);
            if (rec != null) {
                resultItems.add(rec.toFileItem());
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("AI Search completed in {}ms: found {} matches for query '{}'",
                totalDuration, resultItems.size(), naturalLanguagePrompt);

        return resultItems;
    }

    public List<IndexedFileRecord> scoreAndFilterCandidates(List<IndexedFileRecord> allCandidates, String prompt) {
        if (allCandidates.size() <= MAX_CANDIDATES_FOR_LLM) {
            return allCandidates;
        }

        Set<String> queryKeywords = extractSearchKeywords(prompt);
        if (queryKeywords.isEmpty()) {
            return allCandidates.subList(0, MAX_CANDIDATES_FOR_LLM);
        }

        // Extract potential year filters in query (e.g. 2023, 2024)
        Set<Integer> queryYears = new HashSet<>();
        Matcher yearMatcher = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(prompt);
        while (yearMatcher.find()) {
            try {
                queryYears.add(Integer.parseInt(yearMatcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        record ScoredRecord(IndexedFileRecord record, int score) implements Comparable<ScoredRecord> {
            @Override
            public int compareTo(ScoredRecord o) {
                return Integer.compare(o.score, this.score); // descending
            }
        }

        List<ScoredRecord> scored = new ArrayList<>();
        List<IndexedFileRecord> zeroScored = new ArrayList<>();

        for (IndexedFileRecord rec : allCandidates) {
            int score = 0;
            String fileNameLower = rec.fileName().toLowerCase(Locale.ROOT);
            String folderLower = rec.parentFolder().toLowerCase(Locale.ROOT);
            String publisherLower = rec.publisher().toLowerCase(Locale.ROOT);

            for (String kw : queryKeywords) {
                if (fileNameLower.contains(kw)) {
                    score += 15;
                    if (fileNameLower.startsWith(kw) || fileNameLower.contains(" " + kw) || fileNameLower.contains("_" + kw)) {
                        score += 5;
                    }
                }
                if (folderLower.contains(kw)) {
                    score += 6;
                }
                if (publisherLower.contains(kw)) {
                    score += 4;
                }
            }

            if (rec.year() > 0 && queryYears.contains(rec.year())) {
                score += 25;
            }

            if (score > 0) {
                scored.add(new ScoredRecord(rec, score));
            } else {
                zeroScored.add(rec);
            }
        }

        Collections.sort(scored);

        List<IndexedFileRecord> result = new ArrayList<>();
        for (ScoredRecord sr : scored) {
            result.add(sr.record);
            if (result.size() >= MAX_CANDIDATES_FOR_LLM) break;
        }

        // If scored items are fewer than MAX_CANDIDATES_FOR_LLM, supplement with unscored items
        if (result.size() < MAX_CANDIDATES_FOR_LLM && !zeroScored.isEmpty()) {
            int needed = Math.min(MAX_CANDIDATES_FOR_LLM - result.size(), zeroScored.size());
            result.addAll(zeroScored.subList(0, needed));
        }

        return result;
    }

    public static Set<String> extractSearchKeywords(String prompt) {
        Set<String> keywords = new LinkedHashSet<>();
        if (StringUtils.isBlank(prompt)) return keywords;

        String[] tokens = prompt.toLowerCase(Locale.ROOT).split("[\\s,;:.!?\"'()\\[\\]{}_/\\\\-]+");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && !STOP_WORDS.contains(trimmed)) {
                keywords.add(trimmed);
                addSynonyms(trimmed, keywords);
            }
        }

        return keywords;
    }

    private static void addSynonyms(String token, Set<String> keywords) {
        switch (token) {
            case "ai" -> {
                keywords.add("artificial");
                keywords.add("intelligence");
                keywords.add("neural");
                keywords.add("agent");
                keywords.add("agents");
                keywords.add("gpt");
                keywords.add("llm");
            }
            case "ml", "machinelearning" -> {
                keywords.add("machine");
                keywords.add("learning");
                keywords.add("model");
            }
            case "agent", "agents" -> {
                keywords.add("agent");
                keywords.add("agentic");
            }
            case "python" -> keywords.add("py");
            case "data" -> {
                keywords.add("dataset");
                keywords.add("analytics");
            }
            case "db", "database" -> {
                keywords.add("sql");
                keywords.add("database");
                keywords.add("db");
            }
            case "security" -> {
                keywords.add("cyber");
                keywords.add("auth");
                keywords.add("crypto");
            }
            case "finance" -> {
                keywords.add("tax");
                keywords.add("invoice");
                keywords.add("accounting");
            }
            default -> {
            }
        }
    }

    public String buildUserPrompt(String query, List<IndexedFileRecord> chunk) {
        ObjectNode rootNode = this.mapper.createObjectNode();
        rootNode.put("query", query);
        ArrayNode filesArray = rootNode.putArray("files");

        for (IndexedFileRecord rec : chunk) {
            ObjectNode fileNode = filesArray.addObject();
            fileNode.put("id", rec.id());
            fileNode.put("name", rec.fileName());
            fileNode.put("dir", rec.parentFolder());
            if (StringUtils.isNotBlank(rec.extension())) {
                fileNode.put("ext", rec.extension());
            }
            if (rec.year() > 0) {
                fileNode.put("yr", rec.year());
            }
            if (StringUtils.isNotBlank(rec.publisher())) {
                fileNode.put("pub", rec.publisher());
            }
        }

        try {
            return this.mapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{\"query\": \"" + query + "\"}";
        }
    }

    public List<Long> parseMatchedIds(String aiResponse) {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.isBlank(aiResponse)) return ids;

        String cleaned = aiResponse.trim();

        // 1. Strip reasoning tags <think>...</think> (e.g. DeepSeek-R1 / Ollama think models)
        if (cleaned.contains("</think>")) {
            cleaned = cleaned.substring(cleaned.lastIndexOf("</think>") + "</think>".length()).trim();
        }

        // 2. Strip Markdown code blocks ```json ... ``` or ``` ... ```
        if (cleaned.contains("```")) {
            int firstFence = cleaned.indexOf("```");
            int newlineAfterFence = cleaned.indexOf('\n', firstFence);
            int closingFence = cleaned.lastIndexOf("```");
            if (closingFence > firstFence) {
                if (newlineAfterFence != -1 && newlineAfterFence < closingFence) {
                    cleaned = cleaned.substring(newlineAfterFence + 1, closingFence).trim();
                } else {
                    cleaned = cleaned.substring(firstFence + 3, closingFence).trim();
                }
            }
        }

        // 3. Extract outermost JSON object {...} or array [...] if surrounding text exists
        int firstBrace = cleaned.indexOf('{');
        int firstBracket = cleaned.indexOf('[');

        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            int lastBrace = cleaned.lastIndexOf('}');
            if (lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            }
        } else if (firstBracket != -1) {
            int lastBracket = cleaned.lastIndexOf(']');
            if (lastBracket > firstBracket) {
                cleaned = cleaned.substring(firstBracket, lastBracket + 1);
            }
        }

        try {
            JsonNode root = this.mapper.readTree(cleaned);

            if (root.isArray()) {
                this.extractIdsFromArray(root, ids);
            } else if (root.isObject()) {
                String[] candidateKeys = new String[]{
                        "matched_ids", "matchedIds", "matched_files", "matches", "ids", "results", "files"
                };
                boolean found = false;
                for (String key : candidateKeys) {
                    if (root.has(key) && root.get(key).isArray()) {
                        this.extractIdsFromArray(root.get(key), ids);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    root.fields().forEachRemaining(entry -> {
                        if (entry.getValue().isArray()) {
                            this.extractIdsFromArray(entry.getValue(), ids);
                        }
                    });
                }
            }
        } catch (Exception e) {
            // Regex fallback to find IDs in response
            Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)|(?:^|\\s|,|\\[)(\\d+)(?:$|\\s|,|\\])").matcher(cleaned);
            while (matcher.find()) {
                String numStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (numStr != null) {
                    try {
                        ids.add(Long.parseLong(numStr));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return ids;
    }

    private void extractIdsFromArray(JsonNode arrayNode, List<Long> ids) {
        for (JsonNode item : arrayNode) {
            if (item.isNumber()) {
                ids.add(item.asLong());
            } else if (item.isTextual()) {
                try {
                    ids.add(Long.parseLong(item.asText().trim()));
                } catch (NumberFormatException ignored) {
                }
            } else if (item.isObject()) {
                if (item.has("id") && item.get("id").isNumber()) {
                    ids.add(item.get("id").asLong());
                } else if (item.has("id") && item.get("id").isTextual()) {
                    try {
                        ids.add(Long.parseLong(item.get("id").asText().trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}

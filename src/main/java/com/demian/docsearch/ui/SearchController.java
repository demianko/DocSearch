package com.demian.docsearch.ui;

import com.demian.docsearch.engine.FileSearchEngine;
import com.demian.docsearch.engine.QueryMatcher;
import com.demian.docsearch.engine.QueryParser;
import com.demian.docsearch.model.FileItem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.StringUtils;

public class SearchController {
    private SwingWorker<List<FileItem>, ProgressChunk> activeSearchWorker;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public record ProgressChunk(int scanned, int total, FileItem item) {
    }

    public interface SearchExecutionListener {
        void onSearchStarted();
        void onProgress(ProgressChunk lastChunk, List<FileItem> newItems);
        void onSearchCompleted(List<FileItem> results, long elapsedMillis, boolean wasCancelled);
    }

    public void startSearch(Path rootFolder,
                            String patternQuery,
                            String extensionsQuery,
                            int searchLimit,
                            SearchExecutionListener listener) {
        this.stopSearch();
        this.cancelRequested.set(false);
        final long startTime = System.currentTimeMillis();

        if (listener != null) {
            listener.onSearchStarted();
        }

        this.activeSearchWorker = new SwingWorker<>() {
            @Override
            protected List<FileItem> doInBackground() {
                return FileSearchEngine.search(
                        rootFolder,
                        patternQuery,
                        "",
                        extensionsQuery,
                        searchLimit,
                        SearchController.this.cancelRequested::get,
                        (scanned, total, item) -> this.publish(new ProgressChunk(scanned, total, item))
                );
            }

            @Override
            protected void process(List<ProgressChunk> chunks) {
                if (listener == null || chunks.isEmpty()) return;
                List<FileItem> newItems = new ArrayList<>();
                ProgressChunk lastChunk = null;
                for (ProgressChunk chunk : chunks) {
                    if (chunk.item() != null) {
                        newItems.add(chunk.item());
                    }
                    lastChunk = chunk;
                }
                listener.onProgress(lastChunk, newItems);
            }

            @Override
            protected void done() {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                boolean cancelled = SearchController.this.cancelRequested.get();
                List<FileItem> searchResults = null;
                try {
                    searchResults = this.get();
                } catch (Exception ignored) {
                }
                if (listener != null) {
                    listener.onSearchCompleted(searchResults, elapsedMillis, cancelled);
                }
            }
        };

        this.activeSearchWorker.execute();
    }

    public void stopSearch() {
        this.cancelRequested.set(true);
        if (this.activeSearchWorker != null && !this.activeSearchWorker.isDone()) {
            this.activeSearchWorker.cancel(true);
        }
    }

    public boolean isSearching() {
        return this.activeSearchWorker != null && !this.activeSearchWorker.isDone();
    }

    public boolean matchesFilter(FileItem item, QueryParser.ParsedQuery parsed) {
        if (item == null) return false;
        List<String> candidates = new ArrayList<>();
        if (StringUtils.isNotEmpty(item.name())) candidates.add(item.name());
        if (StringUtils.isNotEmpty(item.parentStr())) candidates.add(item.parentStr());
        if (item.path() != null) candidates.add(item.path().toString());
        if (StringUtils.isNotEmpty(item.publisher())) candidates.add(item.publisher());
        if (item.year() > 0) candidates.add(String.valueOf(item.year()));
        return QueryMatcher.matchesAny(candidates, parsed.rules(), parsed.globalExcludes());
    }

    public List<FileItem> filterItems(List<FileItem> allResults, String filterText) {
        if (allResults == null || allResults.isEmpty()) return List.of();
        String trimmed = StringUtils.trimToEmpty(filterText);
        if (StringUtils.isEmpty(trimmed)) {
            return allResults;
        }
        QueryParser.ParsedQuery parsed = QueryParser.parse(trimmed);
        return allResults.stream()
                .filter(item -> this.matchesFilter(item, parsed))
                .toList();
    }
}

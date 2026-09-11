package com.demian.docsearch.engine;

import com.demian.docsearch.constant.AppConstants;
import com.demian.docsearch.model.FileItem;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class FileSearchEngine {
    public static List<FileItem> search(
            Path rootFolder, String patternQuery, String publisherQuery, String extensionsQuery,
            int limit, BooleanSupplier cancelCheck, ProgressListener progressListener) {
        if (!FileSearchEngine.isValidDirectory(rootFolder)) {
            return List.of();
        }
        SearchCriteria criteria = FileSearchEngine.parseSearchCriteria(patternQuery, publisherQuery, extensionsQuery);
        List<FileEntry> fileEntries = FileSearchEngine.collectFileEntries(rootFolder, cancelCheck, progressListener);
        if (FileSearchEngine.isSearchCancelled(cancelCheck)) {
            return List.of();
        }
        fileEntries.sort(Comparator.comparingLong(FileEntry::mtime).reversed());
        return FileSearchEngine.evaluateSearchCriteriaForFileEntries(fileEntries, criteria, limit, cancelCheck, progressListener);
    }

    public static List<FileItem> search(
            Path rootFolder, String patternQuery, String publisherQuery, String extensionsQuery,
            int limit, BooleanSupplier cancelCheck) {
        return FileSearchEngine.search(rootFolder, patternQuery, publisherQuery, extensionsQuery, limit, cancelCheck, null);
    }

    private static boolean isValidDirectory(Path folder) {
        return folder != null && Files.exists(folder, new LinkOption[0]) && Files.isDirectory(folder, new LinkOption[0]);
    }

    private static boolean isSearchCancelled(BooleanSupplier cancelCheck) {
        return cancelCheck != null && cancelCheck.getAsBoolean();
    }

    private static boolean isSystemIgnored(Path path) {
        if (path == null) {
            return false;
        }
        Path filename = path.getFileName();
        if (filename == null) {
            return false;
        }
        return AppConstants.SYSTEM_IGNORED_DIRS.contains(StringUtils.upperCase(filename.toString()));
    }

    private static SearchCriteria parseSearchCriteria(String patternQuery, String publisherQuery, String extensionsQuery) {
        QueryParser.ParsedQuery patternParsed = QueryParser.parse(patternQuery);
        QueryParser.ParsedQuery pubParsed = QueryParser.parse(publisherQuery);
        ExtensionFilter extFilter = ExtensionFilter.convertFrom(extensionsQuery);
        return new SearchCriteria(patternParsed.rules(), patternParsed.globalExcludes(), pubParsed.rules(), pubParsed.globalExcludes(), extFilter);
    }

    private static List<FileEntry> collectFileEntries(Path rootFolder, final BooleanSupplier cancelCheck, final ProgressListener progressListener) {
        final ArrayList<FileEntry> fileEntries = new ArrayList<FileEntry>();
        try {
            Files.walkFileTree(rootFolder, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (FileSearchEngine.isSearchCancelled(cancelCheck)) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (FileSearchEngine.isSystemIgnored(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (FileSearchEngine.isSearchCancelled(cancelCheck)) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (attrs.isRegularFile()) {
                        fileEntries.add(new FileEntry(file, attrs.lastModifiedTime().toMillis(), attrs.size()));
                        if (progressListener != null && fileEntries.size() % 100 == 0) {
                            progressListener.onProgress(fileEntries.size(), -1, null);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            System.err.println("Error walking file tree: " + e.getMessage());
        }
        return fileEntries;
    }

    private static List<FileItem> evaluateSearchCriteriaForFileEntries(
            List<FileEntry> fileEntries, SearchCriteria criteria, int limit,
            BooleanSupplier cancelCheck, ProgressListener progressListener) {
        ArrayList<FileItem> results = new ArrayList<FileItem>();
        int totalFiles = fileEntries.size();
        if (progressListener != null) {
            progressListener.onProgress(0, totalFiles, null);
        }
        for (int index = 0; index < totalFiles && !FileSearchEngine.isSearchCancelled(cancelCheck); ++index) {
            FileEntry entry = fileEntries.get(index);
            FileItem item = FileSearchEngine.evaluateFileMatch(entry, criteria);
            if (item == null) {
                FileSearchEngine.notifyProgressIfInterval(progressListener, index + 1, totalFiles, null);
                continue;
            }
            results.add(item);
            if (progressListener != null) {
                progressListener.onProgress(index + 1, totalFiles, item);
            }
            if (limit > 0 && results.size() >= limit) break;
        }
        return results;
    }

    private static void notifyProgressIfInterval(ProgressListener listener, int current, int total, FileItem item) {
        if (listener == null) {
            return;
        }
        if (current % 25 == 0 || current == total) {
            listener.onProgress(current, total, item);
        }
    }

    private static FileItem evaluateFileMatch(FileEntry entry, SearchCriteria criteria) {
        Path file = entry.path();
        String filename = file.getFileName() != null ? file.getFileName().toString() : file.toString();
        String ext = StringUtils.substringAfterLast(filename, ".");
        if (!criteria.extFilter().matches(ext)) {
            return null;
        }
        if (!QueryMatcher.matches(filename, criteria.patternRules(), criteria.patternExcludes())) {
            return null;
        }
        String publisher = MetadataExtractorFromFileName.extractPublisher(filename);
        if (!QueryMatcher.matches(publisher, criteria.pubRules(), criteria.pubExcludes())) {
            return null;
        }
        int year = MetadataExtractorFromFileName.extractYear(filename);
        return new FileItem(file, year, publisher, entry.mtime(), false, entry.size());
    }

    public static List<FileItem> listDirectSubFolderFiles(Path folder) {
        if (!FileSearchEngine.isValidDirectory(folder)) {
            return List.of();
        }
        ArrayList<FileItem> folders = new ArrayList<FileItem>();
        ArrayList<FileItem> files = new ArrayList<FileItem>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder);){
            for (Path entry : stream) {
                FileItem item;
                if (FileSearchEngine.isSystemIgnored(entry) || (item = FileSearchEngine.toDirectFileItem(entry)) == null) continue;
                if (item.isDirectory()) {
                    folders.add(item);
                    continue;
                }
                files.add(item);
            }
        }
        catch (IOException ignored) {
            return List.of();
        }
        folders.sort(Comparator.comparing(f -> StringUtils.lowerCase(f.name())));
        files.sort(Comparator.comparingLong(FileItem::modified).reversed());
        ArrayList<FileItem> combined = new ArrayList<FileItem>(folders.size() + files.size());
        combined.addAll(folders);
        combined.addAll(files);
        return combined;
    }

    private static FileItem toDirectFileItem(Path entry) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class, new LinkOption[0]);
            long mtime = attrs.lastModifiedTime().toMillis();
            if (attrs.isDirectory()) {
                return new FileItem(entry, 0, "Folder", mtime, true, -1L);
            }
            if (!attrs.isRegularFile()) {
                return null;
            }
            Path fn = entry.getFileName();
            String name = fn != null ? fn.toString() : entry.toString();
            String pub = MetadataExtractorFromFileName.extractPublisher(name);
            int year = MetadataExtractorFromFileName.extractYear(name);
            return new FileItem(entry, year, pub, mtime, false, attrs.size());
        }
        catch (IOException ignored) {
            return null;
        }
    }

    public static List<FileItem> sortResults(List<FileItem> items, String criteria, boolean reverse) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        List<FileItem> folders = items.stream().filter(FileItem::isDirectory).toList();
        List<FileItem> files = items.stream().filter(f -> !f.isDirectory()).toList();
        String crit = StringUtils.lowerCase(StringUtils.defaultIfBlank(criteria, "modified"));
        Comparator<FileItem> folderComp = FileSearchEngine.createFolderComparator(crit, reverse);
        Comparator<FileItem> fileComp = FileSearchEngine.createFileComparator(crit, reverse);
        ArrayList<FileItem> sortedFolders = new ArrayList<FileItem>(folders);
        sortedFolders.sort(folderComp);
        ArrayList<FileItem> sortedFiles = new ArrayList<FileItem>(files);
        sortedFiles.sort(fileComp);
        ArrayList<FileItem> result = new ArrayList<FileItem>(sortedFolders.size() + sortedFiles.size());
        result.addAll(sortedFolders);
        result.addAll(sortedFiles);
        return result;
    }

    private static Comparator<FileItem> createFileComparator(String criteria, boolean reverse) {
        Comparator<FileItem> comp = switch (criteria) {
            case "published", "year" -> Comparator.comparingInt(FileItem::year).thenComparing(f -> StringUtils.lowerCase(f.name()));
            case "name" -> Comparator.comparing(f -> StringUtils.lowerCase(f.name()));
            case "path", "directory" ->
                    Comparator.comparing((FileItem f) -> StringUtils.lowerCase(f.parentStr()))
                    .thenComparing(f -> StringUtils.lowerCase(f.name()));
            case "size" -> Comparator.comparingLong(FileItem::sizeBytes);
            default -> Comparator.comparingLong(FileItem::modified).thenComparing(f -> StringUtils.lowerCase(f.name()));
        };
        return reverse ? comp.reversed() : comp;
    }

    private static Comparator<FileItem> createFolderComparator(String criteria, boolean reverse) {
        Comparator<FileItem> folderComp = Comparator.comparing(f -> StringUtils.lowerCase(f.name()));
        if ("name".equalsIgnoreCase(criteria)) {
            if (reverse) {
                folderComp = folderComp.reversed();
            }
        } else if ("path".equalsIgnoreCase(criteria) || "directory".equalsIgnoreCase(criteria)) {
            folderComp = Comparator.comparing((FileItem f) -> StringUtils.lowerCase(f.parentStr()))
                    .thenComparing(f -> StringUtils.lowerCase(f.name()));
            if (reverse) {
                folderComp = folderComp.reversed();
            }
        } else if ("modified".equalsIgnoreCase(criteria) || "date".equalsIgnoreCase(criteria)) {
            folderComp = Comparator.comparingLong(FileItem::modified);
            if (reverse) {
                folderComp = folderComp.reversed();
            }
        }
        return folderComp;
    }

    private record SearchCriteria(
            List<SearchRule> patternRules, List<Pattern> patternExcludes,
            List<SearchRule> pubRules, List<Pattern> pubExcludes, ExtensionFilter extFilter) {
    }

    @FunctionalInterface
    public static interface ProgressListener {
        public void onProgress(int var1, int var2, FileItem var3);
    }

    private record FileEntry(Path path, long mtime, long size) {
    }
}


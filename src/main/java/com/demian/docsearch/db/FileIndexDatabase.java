package com.demian.docsearch.db;

import com.demian.docsearch.config.ConfigManager;
import com.demian.docsearch.constant.AppConstants;
import com.demian.docsearch.engine.ExtensionFilter;
import com.demian.docsearch.engine.FileSearchEngine;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileIndexDatabase implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FileIndexDatabase.class);
    private final Path dbPath;
    private final String jdbcUrl;

    public FileIndexDatabase() {
        this(ConfigManager.getDefaultConfigDir().resolve("docsearch_index.db"));
    }

    public FileIndexDatabase(Path dbPath) {
        this.dbPath = Objects.requireNonNull(dbPath, "dbPath must not be null");
        this.jdbcUrl = "jdbc:sqlite:" + this.dbPath.toAbsolutePath().normalize().toString();
        this.initSchema();
    }

    public Path getDbPath() {
        return this.dbPath;
    }

    private Connection getConnection() throws SQLException {
        try {
            if (this.dbPath.getParent() != null && !Files.exists(this.dbPath.getParent(), new LinkOption[0])) {
                Files.createDirectories(this.dbPath.getParent());
            }
        } catch (IOException e) {
            throw new SQLException("Failed to create database directory: " + e.getMessage(), e);
        }
        return DriverManager.getConnection(this.jdbcUrl);
    }

    private void initSchema() {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS file_index (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    root_folder TEXT NOT NULL,
                    parent_folder TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    full_path TEXT NOT NULL UNIQUE,
                    extension TEXT,
                    size_bytes INTEGER,
                    last_modified INTEGER,
                    year INTEGER,
                    publisher TEXT,
                    indexed_at INTEGER
                );
                CREATE INDEX IF NOT EXISTS idx_root_folder ON file_index(root_folder);
                CREATE INDEX IF NOT EXISTS idx_parent_folder ON file_index(parent_folder);
                CREATE INDEX IF NOT EXISTS idx_extension ON file_index(extension);
                CREATE INDEX IF NOT EXISTS idx_file_name ON file_index(file_name);
                """;

        try (Connection conn = this.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSql);
        } catch (SQLException e) {
            log.error("Failed to initialize SQLite index database: {}", e.getMessage(), e);
        }
    }

    private static void bindFolderPrefixParams(PreparedStatement stmt, int startIndex, String normalizedRoot) throws SQLException {
        String exactSlash = normalizedRoot.replace('\\', '/');
        String exactBackslash = normalizedRoot.replace('/', '\\');
        String pSlash = exactSlash + "/%";
        String pBackslash = exactBackslash + "\\%";
        stmt.setString(startIndex, exactSlash);
        stmt.setString(startIndex + 1, exactBackslash);
        stmt.setString(startIndex + 2, pSlash);
        stmt.setString(startIndex + 3, pBackslash);
    }

    public int indexDirectory(Path rootFolder, FileSearchEngine.ProgressListener listener, BooleanSupplier cancelCheck) {
        Objects.requireNonNull(rootFolder, "rootFolder must not be null");
        if (!Files.exists(rootFolder, new LinkOption[0]) || !Files.isDirectory(rootFolder, new LinkOption[0])) return 0;

        final String rootPathStr = rootFolder.toAbsolutePath().normalize().toString();
        log.info("Indexing directory '{}': clearing old SQLite entries and recreating fresh records from disk...", rootPathStr);
        final List<IndexedFileRecord> records = new ArrayList<>();
        long scanStartTime = System.currentTimeMillis();

        try {
            Files.walkFileTree(rootFolder, (FileVisitor<? super Path>) new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (cancelCheck != null && cancelCheck.getAsBoolean()) return FileVisitResult.TERMINATE;
                    if (isSystemIgnored(dir)) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (cancelCheck != null && cancelCheck.getAsBoolean()) return FileVisitResult.TERMINATE;
                    if (attrs.isRegularFile()) {
                        IndexedFileRecord record = IndexedFileRecord.createFromPath(file, rootFolder);
                        records.add(record);
                        if (listener != null && records.size() % 100 == 0) listener.onProgress(records.size(), -1, null);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning directory '{}' for indexing: {}", rootPathStr, e.getMessage());
        }

        if (cancelCheck != null && cancelCheck.getAsBoolean()) return 0;

        String deleteSql = """
                DELETE FROM file_index 
                WHERE root_folder = ? 
                   OR root_folder = ?
                   OR root_folder LIKE ? 
                   OR root_folder LIKE ?
                   OR parent_folder = ? 
                   OR parent_folder = ?
                   OR parent_folder LIKE ? 
                   OR parent_folder LIKE ?
                   OR full_path = ? 
                   OR full_path = ?
                   OR full_path LIKE ? 
                   OR full_path LIKE ?
                """;

        String insertSql = """
                INSERT INTO file_index (
                    root_folder, parent_folder, file_name, full_path, extension, size_bytes, last_modified, year, publisher, indexed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int totalSaved = 0;
        try (Connection conn = this.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                bindFolderPrefixParams(delStmt, 1, rootPathStr);
                bindFolderPrefixParams(delStmt, 5, rootPathStr);
                bindFolderPrefixParams(delStmt, 9, rootPathStr);
                int deletedCount = delStmt.executeUpdate();
                log.debug("Deleted {} existing SQLite entries for '{}' before recreation", deletedCount, rootPathStr);
            }

            int total = records.size();
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < total; i++) {
                    if (cancelCheck != null && cancelCheck.getAsBoolean()) {
                        conn.rollback();
                        return 0;
                    }
                    IndexedFileRecord r = records.get(i);
                    insStmt.setString(1, r.rootFolder());
                    insStmt.setString(2, r.parentFolder());
                    insStmt.setString(3, r.fileName());
                    insStmt.setString(4, r.fullPath());
                    insStmt.setString(5, r.extension());
                    insStmt.setLong(6, r.sizeBytes());
                    insStmt.setLong(7, r.lastModified());
                    insStmt.setInt(8, r.year());
                    insStmt.setString(9, r.publisher());
                    insStmt.setLong(10, r.indexedAt());
                    insStmt.addBatch();

                    if (i % 500 == 0 || i == total - 1) {
                        insStmt.executeBatch();
                    }
                    if (listener != null && (i % 50 == 0 || i == total - 1)) {
                        listener.onProgress(i + 1, total, r.toFileItem());
                    }
                }
            }
            conn.commit();
            totalSaved = records.size();
            long elapsed = System.currentTimeMillis() - scanStartTime;
            log.info("Recreated SQLite index for '{}': inserted {} fresh entries (took {}ms)", rootPathStr, totalSaved, elapsed);
        } catch (SQLException e) {
            log.error("Failed to recreate indexed files in SQLite for '{}': {}", rootPathStr, e.getMessage(), e);
        }

        return totalSaved;
    }

    public List<IndexedFileRecord> getIndexedFiles(String rootFolder, String extensionsQuery) {
        if (StringUtils.isBlank(rootFolder)) return List.of();
        String normalizedRoot = Paths.get(rootFolder).toAbsolutePath().normalize().toString();
        ExtensionFilter extFilter = ExtensionFilter.convertFrom(extensionsQuery);

        String sql = """
                SELECT id, root_folder, parent_folder, file_name, full_path, extension, size_bytes, last_modified, year, publisher, indexed_at 
                FROM file_index 
                WHERE root_folder = ? 
                   OR root_folder = ?
                   OR root_folder LIKE ? 
                   OR root_folder LIKE ?
                   OR parent_folder = ?
                   OR parent_folder = ?
                   OR parent_folder LIKE ?
                   OR parent_folder LIKE ?
                   OR full_path = ?
                   OR full_path = ?
                   OR full_path LIKE ? 
                   OR full_path LIKE ?
                ORDER BY last_modified DESC
                """;

        List<IndexedFileRecord> results = new ArrayList<>();
        try (Connection conn = this.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFolderPrefixParams(stmt, 1, normalizedRoot);
            bindFolderPrefixParams(stmt, 5, normalizedRoot);
            bindFolderPrefixParams(stmt, 9, normalizedRoot);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String ext = rs.getString("extension");
                    if (!extFilter.matches(ext)) continue;

                    IndexedFileRecord record = new IndexedFileRecord(
                            rs.getLong("id"),
                            rs.getString("root_folder"),
                            rs.getString("parent_folder"),
                            rs.getString("file_name"),
                            rs.getString("full_path"),
                            ext,
                            rs.getLong("size_bytes"),
                            rs.getLong("last_modified"),
                            rs.getInt("year"),
                            rs.getString("publisher"),
                            rs.getLong("indexed_at")
                    );
                    results.add(record);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query indexed files for '{}': {}", normalizedRoot, e.getMessage());
        }
        return results;
    }

    public int getIndexedFileCount(String rootFolder) {
        if (StringUtils.isBlank(rootFolder)) return 0;
        String normalizedRoot = Paths.get(rootFolder).toAbsolutePath().normalize().toString();
        String sql = """
                SELECT COUNT(*) FROM file_index 
                WHERE root_folder = ? 
                   OR root_folder = ?
                   OR root_folder LIKE ? 
                   OR root_folder LIKE ?
                   OR parent_folder = ?
                   OR parent_folder = ?
                   OR parent_folder LIKE ?
                   OR parent_folder LIKE ?
                   OR full_path = ?
                   OR full_path = ?
                   OR full_path LIKE ? 
                   OR full_path LIKE ?
                """;
        try (Connection conn = this.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFolderPrefixParams(stmt, 1, normalizedRoot);
            bindFolderPrefixParams(stmt, 5, normalizedRoot);
            bindFolderPrefixParams(stmt, 9, normalizedRoot);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Failed to count indexed files for '{}': {}", normalizedRoot, e.getMessage());
        }
        return 0;
    }

    public long getLastIndexedTimestamp(String rootFolder) {
        if (StringUtils.isBlank(rootFolder)) return 0L;
        String normalizedRoot = Paths.get(rootFolder).toAbsolutePath().normalize().toString();
        String sql = """
                SELECT MAX(indexed_at) FROM file_index 
                WHERE root_folder = ? 
                   OR root_folder = ?
                   OR root_folder LIKE ? 
                   OR root_folder LIKE ?
                   OR parent_folder = ?
                   OR parent_folder = ?
                   OR parent_folder LIKE ?
                   OR parent_folder LIKE ?
                   OR full_path = ?
                   OR full_path = ?
                   OR full_path LIKE ? 
                   OR full_path LIKE ?
                """;
        try (Connection conn = this.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFolderPrefixParams(stmt, 1, normalizedRoot);
            bindFolderPrefixParams(stmt, 5, normalizedRoot);
            bindFolderPrefixParams(stmt, 9, normalizedRoot);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Failed to query last indexed timestamp for '{}': {}", normalizedRoot, e.getMessage());
        }
        return 0L;
    }

    public boolean hasFilesModifiedAfterIndex(Path rootFolder) {
        if (rootFolder == null || !Files.exists(rootFolder, new LinkOption[0]) || !Files.isDirectory(rootFolder, new LinkOption[0])) {
            return false;
        }
        String rootPathStr = rootFolder.toAbsolutePath().normalize().toString();
        long lastIndexed = this.getLastIndexedTimestamp(rootPathStr);
        if (lastIndexed <= 0L) {
            return false;
        }

        final boolean[] modifiedFound = new boolean[]{false};
        try {
            Files.walkFileTree(rootFolder, (FileVisitor<? super Path>) new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isSystemIgnored(dir)) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && attrs.lastModifiedTime().toMillis() > lastIndexed) {
                        modifiedFound[0] = true;
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Failed checking file modified timestamps for '{}': {}", rootPathStr, e.getMessage());
        }
        return modifiedFound[0];
    }

    public void deleteIndexForFolder(String rootFolder) {
        if (StringUtils.isBlank(rootFolder)) return;
        String normalizedRoot = Paths.get(rootFolder).toAbsolutePath().normalize().toString();
        String sql = """
                DELETE FROM file_index 
                WHERE root_folder = ? 
                   OR root_folder = ?
                   OR root_folder LIKE ? 
                   OR root_folder LIKE ?
                   OR parent_folder = ? 
                   OR parent_folder = ?
                   OR parent_folder LIKE ? 
                   OR parent_folder LIKE ?
                   OR full_path = ? 
                   OR full_path = ?
                   OR full_path LIKE ? 
                   OR full_path LIKE ?
                """;
        try (Connection conn = this.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFolderPrefixParams(stmt, 1, normalizedRoot);
            bindFolderPrefixParams(stmt, 5, normalizedRoot);
            bindFolderPrefixParams(stmt, 9, normalizedRoot);
            int deleted = stmt.executeUpdate();
            log.info("Deleted {} index entries for folder '{}'", deleted, normalizedRoot);
        } catch (SQLException e) {
            log.error("Failed to delete indexed files for folder '{}': {}", normalizedRoot, e.getMessage());
        }
    }

    public void clearAll() {
        String sql = "DELETE FROM file_index";
        try (Connection conn = this.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            log.info("Cleared all entries from SQLite file_index table");
        } catch (SQLException e) {
            log.error("Failed to clear file index table: {}", e.getMessage(), e);
        }
    }

    private static boolean isSystemIgnored(Path path) {
        if (path == null) return false;
        Path filename = path.getFileName();
        if (filename == null) return false;
        return AppConstants.SYSTEM_IGNORED_DIRS.contains(StringUtils.upperCase(filename.toString()));
    }

    @Override
    public void close() {
    }
}

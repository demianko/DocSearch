package com.demian.docsearch.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileIndexDatabaseTest {

    @TempDir
    Path tempDir;

    private FileIndexDatabase database;
    private Path dbPath;

    @BeforeEach
    void setUp() {
        this.dbPath = tempDir.resolve("test_index.db");
        this.database = new FileIndexDatabase(this.dbPath);
    }

    @AfterEach
    void tearDown() {
        if (this.database != null) {
            this.database.close();
        }
    }

    @Test
    void testIndexDirectoryAndQueryFiles() throws IOException {
        Path rootFolder = tempDir.resolve("Documents");
        Path subFolder = rootFolder.resolve("Finance");
        Files.createDirectories(subFolder);

        Path file1 = rootFolder.resolve("Readme.txt");
        Path file2 = subFolder.resolve("Financial_Report_2023.pdf");
        Path file3 = subFolder.resolve("Invoice_2024.docx");

        Files.writeString(file1, "sample text content");
        Files.writeString(file2, "pdf report binary content");
        Files.writeString(file3, "docx invoice content");

        int indexedCount = database.indexDirectory(rootFolder, null, () -> false);
        assertThat(indexedCount).isEqualTo(3);

        int countInDb = database.getIndexedFileCount(rootFolder.toAbsolutePath().toString());
        assertThat(countInDb).isEqualTo(3);

        List<IndexedFileRecord> allRecords = database.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "");
        assertThat(allRecords).hasSize(3);
        assertThat(allRecords).extracting(IndexedFileRecord::fileName)
                .containsExactlyInAnyOrder("Readme.txt", "Financial_Report_2023.pdf", "Invoice_2024.docx");

        IndexedFileRecord pdfRecord = allRecords.stream()
                .filter(r -> r.fileName().equals("Financial_Report_2023.pdf"))
                .findFirst().orElseThrow();
        assertThat(pdfRecord.extension()).isEqualTo("pdf");
        assertThat(pdfRecord.year()).isEqualTo(2023);
        assertThat(pdfRecord.sizeBytes()).isGreaterThan(0);

        List<IndexedFileRecord> pdfOnly = database.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "pdf");
        assertThat(pdfOnly).hasSize(1);
        assertThat(pdfOnly.get(0).fileName()).isEqualTo("Financial_Report_2023.pdf");

        List<IndexedFileRecord> pdfAndDocx = database.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "pdf, docx");
        assertThat(pdfAndDocx).hasSize(2);
        assertThat(pdfAndDocx).extracting(IndexedFileRecord::fileName)
                .containsExactlyInAnyOrder("Financial_Report_2023.pdf", "Invoice_2024.docx");
    }

    @Test
    void testIndexCancellation() throws IOException {
        Path rootFolder = tempDir.resolve("CancelTest");
        Files.createDirectories(rootFolder);
        Files.writeString(rootFolder.resolve("fileA.txt"), "hello");

        int indexed = database.indexDirectory(rootFolder, null, () -> true);
        assertThat(indexed).isEqualTo(0);
        assertThat(database.getIndexedFileCount(rootFolder.toAbsolutePath().toString())).isEqualTo(0);
    }

    @Test
    void testDeleteAndClearIndex() throws IOException {
        Path rootFolder = tempDir.resolve("FolderA");
        Files.createDirectories(rootFolder);
        Files.writeString(rootFolder.resolve("test.txt"), "content");

        database.indexDirectory(rootFolder, null, null);
        assertThat(database.getIndexedFileCount(rootFolder.toAbsolutePath().toString())).isEqualTo(1);

        database.deleteIndexForFolder(rootFolder.toAbsolutePath().toString());
        assertThat(database.getIndexedFileCount(rootFolder.toAbsolutePath().toString())).isEqualTo(0);

        database.indexDirectory(rootFolder, null, null);
        assertThat(database.getIndexedFileCount(rootFolder.toAbsolutePath().toString())).isEqualTo(1);

        database.clearAll();
        assertThat(database.getIndexedFileCount(rootFolder.toAbsolutePath().toString())).isEqualTo(0);
    }

    @Test
    void testReindexRecreatesDataEntriesInSqlite() throws IOException {
        Path rootFolder = tempDir.resolve("ReindexFolder");
        Files.createDirectories(rootFolder);
        Path file1 = rootFolder.resolve("OldDoc1.txt");
        Path file2 = rootFolder.resolve("OldDoc2.txt");
        Files.writeString(file1, "Initial Content 1");
        Files.writeString(file2, "Initial Content 2");

        // First index
        int firstIndexed = database.indexDirectory(rootFolder, null, null);
        assertThat(firstIndexed).isEqualTo(2);

        List<IndexedFileRecord> initialRecords = database.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "");
        assertThat(initialRecords).hasSize(2);
        long initialId1 = initialRecords.get(0).id();
        long initialId2 = initialRecords.get(1).id();

        // Now modify folder: delete file1, modify file2, add file3
        Files.delete(file1);
        Files.writeString(file2, "Updated Content 2 with more bytes");
        Path file3 = rootFolder.resolve("NewDoc3.pdf");
        Files.writeString(file3, "New PDF document");

        // Reindex folder
        int reindexedCount = database.indexDirectory(rootFolder, null, null);
        assertThat(reindexedCount).isEqualTo(2);

        List<IndexedFileRecord> recreatedRecords = database.getIndexedFiles(rootFolder.toAbsolutePath().toString(), "");
        assertThat(recreatedRecords).hasSize(2);
        assertThat(recreatedRecords).extracting(IndexedFileRecord::fileName)
                .containsExactlyInAnyOrder("OldDoc2.txt", "NewDoc3.pdf")
                .doesNotContain("OldDoc1.txt");

        // Verify IDs and entries were recreated
        IndexedFileRecord recreatedFile2 = recreatedRecords.stream()
                .filter(r -> r.fileName().equals("OldDoc2.txt"))
                .findFirst().orElseThrow();
        assertThat(recreatedFile2.sizeBytes()).isGreaterThan("Initial Content 2".length());
        assertThat(recreatedFile2.id()).isNotEqualTo(initialId1);
    }

    @Test
    void testHasFilesModifiedAfterIndex() throws Exception {
        Path rootFolder = tempDir.resolve("TimestampCheckFolder");
        Files.createDirectories(rootFolder);
        Path file1 = rootFolder.resolve("File1.txt");
        Files.writeString(file1, "Initial");

        // Before indexing:
        assertThat(database.getLastIndexedTimestamp(rootFolder.toAbsolutePath().toString())).isEqualTo(0L);
        assertThat(database.hasFilesModifiedAfterIndex(rootFolder)).isFalse();

        // Index:
        database.indexDirectory(rootFolder, null, null);
        long indexedTime = database.getLastIndexedTimestamp(rootFolder.toAbsolutePath().toString());
        assertThat(indexedTime).isGreaterThan(0L);

        // Right after indexing, no modified files:
        assertThat(database.hasFilesModifiedAfterIndex(rootFolder)).isFalse();

        // Simulate file modification with future timestamp
        Files.setLastModifiedTime(file1, java.nio.file.attribute.FileTime.fromMillis(indexedTime + 5000L));
        assertThat(database.hasFilesModifiedAfterIndex(rootFolder)).isTrue();
    }
}

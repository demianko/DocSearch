package com.demian.docsearch.ui;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileOperationsTest {

    @Test
    void testDeleteFileAndDirectoryRecursively(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "hello world");
        assertThat(Files.exists(file)).isTrue();

        Files.deleteIfExists(file);
        assertThat(Files.exists(file)).isFalse();

        Path dir = tempDir.resolve("myDir");
        Files.createDirectory(dir);
        Path subFile = dir.resolve("sub.txt");
        Files.writeString(subFile, "sub content");
        Path subSubDir = dir.resolve("nested");
        Files.createDirectory(subSubDir);
        Path deepFile = subSubDir.resolve("deep.txt");
        Files.writeString(deepFile, "deep content");

        assertThat(Files.exists(dir)).isTrue();
        assertThat(Files.exists(deepFile)).isTrue();

        FileUtils.deleteDirectory(dir.toFile());
        assertThat(Files.exists(dir)).isFalse();
    }

    @Test
    void testMoveFileAndFolder(@TempDir Path tempDir) throws IOException {
        Path srcDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("destination");
        Files.createDirectory(srcDir);
        Files.createDirectory(destDir);

        Path file1 = srcDir.resolve("doc.pdf");
        Files.writeString(file1, "pdf content");

        Path subFolder = srcDir.resolve("nestedFolder");
        Files.createDirectory(subFolder);
        Path subFile = subFolder.resolve("data.csv");
        Files.writeString(subFile, "csv content");

        // Move file
        Path destFile = destDir.resolve(file1.getFileName());
        Files.move(file1, destFile, StandardCopyOption.REPLACE_EXISTING);
        assertThat(Files.exists(file1)).isFalse();
        assertThat(Files.exists(destFile)).isTrue();

        // Move folder
        Path destSubFolder = destDir.resolve(subFolder.getFileName());
        Files.move(subFolder, destSubFolder, StandardCopyOption.REPLACE_EXISTING);
        assertThat(Files.exists(subFolder)).isFalse();
        assertThat(Files.exists(destSubFolder)).isTrue();
        assertThat(Files.exists(destSubFolder.resolve("data.csv"))).isTrue();
    }

    @Test
    void testSelfNestingCheck() {
        Path parent = Path.of("C:/Projects/MyFolder").toAbsolutePath().normalize();
        Path child = Path.of("C:/Projects/MyFolder/Sub/Nested").toAbsolutePath().normalize();
        Path sibling = Path.of("C:/Projects/OtherFolder").toAbsolutePath().normalize();

        // Moving parent into child is self-nesting
        assertThat(child.startsWith(parent)).isTrue();

        // Moving parent into sibling is valid
        assertThat(sibling.startsWith(parent)).isFalse();
    }

    @Test
    void testCopyFileAndFolderRecursively(@TempDir Path tempDir) throws IOException {
        Path srcDir = tempDir.resolve("srcFolder");
        Path destDir = tempDir.resolve("destFolder");
        Files.createDirectory(srcDir);
        Files.createDirectory(destDir);

        Path file1 = srcDir.resolve("sample.txt");
        Files.writeString(file1, "sample text content");
        Path subDir = srcDir.resolve("subDir");
        Files.createDirectory(subDir);
        Path subFile = subDir.resolve("sub.txt");
        Files.writeString(subFile, "sub text content");

        // Copy file
        Path destFile = destDir.resolve(file1.getFileName());
        Files.copy(file1, destFile, StandardCopyOption.COPY_ATTRIBUTES);
        assertThat(Files.exists(file1)).isTrue();
        assertThat(Files.exists(destFile)).isTrue();
        assertThat(Files.readString(destFile)).isEqualTo("sample text content");

        // Copy directory recursively
        Path destSubDir = destDir.resolve(subDir.getFileName());
        FileUtils.copyDirectory(subDir.toFile(), destSubDir.toFile());
        assertThat(Files.exists(subDir)).isTrue();
        assertThat(Files.exists(destSubDir)).isTrue();
        assertThat(Files.exists(destSubDir.resolve("sub.txt"))).isTrue();
        assertThat(Files.readString(destSubDir.resolve("sub.txt"))).isEqualTo("sub text content");
    }

    @Test
    void testClipboardBufferOperations(@TempDir Path tempDir) {
        ClipboardBuffer buffer = new ClipboardBuffer();
        assertThat(buffer.hasLocalContent()).isFalse();

        Path p1 = tempDir.resolve("doc1.pdf");
        Path p2 = tempDir.resolve("doc2.pdf");

        // Test copy
        buffer.copy(List.of(p1, p2));
        assertThat(buffer.hasContent()).isTrue();
        assertThat(buffer.isCut()).isFalse();
        assertThat(buffer.getMode()).isEqualTo(ClipboardBuffer.Mode.COPY);
        assertThat(buffer.getPaths()).containsExactly(p1, p2);

        // Test cut
        buffer.cut(List.of(p1));
        assertThat(buffer.hasContent()).isTrue();
        assertThat(buffer.isCut()).isTrue();
        assertThat(buffer.getMode()).isEqualTo(ClipboardBuffer.Mode.CUT);
        assertThat(buffer.getPaths()).containsExactly(p1);

        // Test clear
        buffer.clear();
        assertThat(buffer.getLocalPaths()).isEmpty();
        assertThat(buffer.hasLocalContent()).isFalse();
        assertThat(buffer.isCut()).isFalse();
    }

    @Test
    void testPasteConflictDetection(@TempDir Path tempDir) throws IOException {
        Path srcDir = tempDir.resolve("source");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectory(srcDir);
        Files.createDirectory(targetDir);

        Path file1 = srcDir.resolve("existing.txt");
        Files.writeString(file1, "source version");

        // Create conflict in target
        Path targetConflict = targetDir.resolve("existing.txt");
        Files.writeString(targetConflict, "target version");

        FileOperationsService service = new FileOperationsService();
        ClipboardBuffer buffer = new ClipboardBuffer();
        buffer.copy(List.of(file1));

        // Attempt paste into targetDir where existing.txt already exists.
        // It should detect conflict and skip.
        int pasted = service.pasteItems(null, buffer, targetDir, null, null);
        assertThat(pasted).isEqualTo(0);
        // Original target file remains untouched
        assertThat(Files.readString(targetConflict)).isEqualTo("target version");
        // Source file is intact
        assertThat(Files.readString(file1)).isEqualTo("source version");
    }
}

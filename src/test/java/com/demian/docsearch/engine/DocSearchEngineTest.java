package com.demian.docsearch.engine;

import com.demian.docsearch.model.FileItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocSearchEngineTest {

    @Test
    void testDirectFolderListingExcludesNestedFiles(@TempDir Path tempDir) throws IOException {
        // Direct file
        Path file1 = tempDir.resolve("direct_doc.pdf");
        Files.writeString(file1, "content1");

        // Subdirectory with nested file
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectory(subDir);
        Path nestedFile = subDir.resolve("nested_doc.pdf");
        Files.writeString(nestedFile, "nested");

        // System folder to be ignored
        Path recycleDir = tempDir.resolve("$RECYCLE.BIN");
        Files.createDirectory(recycleDir);

        List<FileItem> directItems = FileSearchEngine.listDirectSubFolderFiles(tempDir);
        assertThat(directItems).hasSize(2);

        // Folders first
        assertThat(directItems.get(0).isDirectory()).isTrue();
        assertThat(directItems.get(0).name()).isEqualTo("subfolder");

        // Files second
        assertThat(directItems.get(1).isDirectory()).isFalse();
        assertThat(directItems.get(1).name()).isEqualTo("direct_doc.pdf");
    }

    @Test
    void testRecursiveSearchWithFilter(@TempDir Path tempDir) throws IOException {
        Path f1 = tempDir.resolve("Java 21 Design Patterns.pdf");
        Path f2 = tempDir.resolve("Python Guide.epub");
        Files.writeString(f1, "dummy");
        Files.writeString(f2, "dummy");

        java.util.concurrent.atomic.AtomicInteger progressCount = new java.util.concurrent.atomic.AtomicInteger(0);

        List<FileItem> results = FileSearchEngine.search(
                tempDir,
                "java * pattern",
                "",
                "pdf",
                0,
                () -> false,
                (scanned, total, item) -> {
                    if (item != null) {
                        progressCount.incrementAndGet();
                    }
                }
        );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Java 21 Design Patterns.pdf");
        assertThat(progressCount.get()).isEqualTo(1);
    }

    @Test
    void testSortResults() {
        Path p1 = Path.of("C:/docs/alpha.pdf");
        Path p2 = Path.of("C:/docs/beta.pdf");
        Path p3 = Path.of("C:/archive/gamma.pdf");
        Path folder1 = Path.of("C:/docs/z_folder");

        FileItem folderItem = new FileItem(folder1, 0, "", 1000L, true, -1L);
        FileItem item1 = new FileItem(p1, 2020, "", 2000L, false, 100L);
        FileItem item2 = new FileItem(p2, 2024, "", 1500L, false, 200L);
        FileItem item3 = new FileItem(p3, 2018, "", 3000L, false, 50L);

        List<FileItem> items = List.of(item1, item2, folderItem, item3);

        // Sort by Name (A-Z, folders always anchored at top)
        List<FileItem> sortedByName = FileSearchEngine.sortResults(items, "name", false);
        assertThat(sortedByName).hasSize(4);
        assertThat(sortedByName.get(0).name()).isEqualTo("z_folder");
        assertThat(sortedByName.get(1).name()).isEqualTo("alpha.pdf");
        assertThat(sortedByName.get(2).name()).isEqualTo("beta.pdf");
        assertThat(sortedByName.get(3).name()).isEqualTo("gamma.pdf");

        // Sort by Date Modified (Newest first, reverse = true)
        List<FileItem> sortedByDateDesc = FileSearchEngine.sortResults(items, "modified", true);
        assertThat(sortedByDateDesc.get(0).name()).isEqualTo("z_folder");
        assertThat(sortedByDateDesc.get(1).name()).isEqualTo("gamma.pdf"); // 3000L
        assertThat(sortedByDateDesc.get(2).name()).isEqualTo("alpha.pdf"); // 2000L
        assertThat(sortedByDateDesc.get(3).name()).isEqualTo("beta.pdf");  // 1500L

        // Sort by Year (Newest first, reverse = true)
        List<FileItem> sortedByYear = FileSearchEngine.sortResults(items, "year", true);
        assertThat(sortedByYear.get(0).name()).isEqualTo("z_folder");
        assertThat(sortedByYear.get(1).name()).isEqualTo("beta.pdf");  // 2024
        assertThat(sortedByYear.get(2).name()).isEqualTo("alpha.pdf"); // 2020
        assertThat(sortedByYear.get(3).name()).isEqualTo("gamma.pdf"); // 2018

        // Sort by Path (folders anchored at top, files ordered by path)
        List<FileItem> sortedByPath = FileSearchEngine.sortResults(items, "path", false);
        assertThat(sortedByPath.get(0).name()).isEqualTo("z_folder");
        assertThat(sortedByPath.get(1).name()).isEqualTo("gamma.pdf"); // C:/archive
        assertThat(sortedByPath.get(2).name()).isEqualTo("alpha.pdf"); // C:/docs
        assertThat(sortedByPath.get(3).name()).isEqualTo("beta.pdf");  // C:/docs
    }
}

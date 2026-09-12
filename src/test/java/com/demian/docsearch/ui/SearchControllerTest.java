package com.demian.docsearch.ui;

import com.demian.docsearch.model.FileItem;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchControllerTest {

    @Test
    void testFilterItemsWithPatterns() {
        SearchController controller = new SearchController();
        FileItem item1 = new FileItem(Path.of("C:/docs/java_concurrency.pdf"), 2021, "Manning", 1000L, false, 100L);
        FileItem item2 = new FileItem(Path.of("C:/docs/python_cookbook.pdf"), 2022, "O'Reilly", 2000L, false, 200L);
        FileItem item3 = new FileItem(Path.of("C:/projects/rust_programming.pdf"), 2023, "NoStarch", 3000L, false, 300L);
        List<FileItem> all = List.of(item1, item2, item3);

        // Empty filter returns all
        assertThat(controller.filterItems(all, "")).hasSize(3);
        assertThat(controller.filterItems(all, "   ")).hasSize(3);

        // Substring match on filename
        List<FileItem> javaMatches = controller.filterItems(all, "concurrency");
        assertThat(javaMatches).containsExactly(item1);

        // Match on year
        List<FileItem> yearMatches = controller.filterItems(all, "2022");
        assertThat(yearMatches).containsExactly(item2);

        // Match on publisher
        List<FileItem> pubMatches = controller.filterItems(all, "NoStarch");
        assertThat(pubMatches).containsExactly(item3);

        // OR filter
        List<FileItem> orMatches = controller.filterItems(all, "concurrency | python");
        assertThat(orMatches).containsExactly(item1, item2);

        // NOT filter
        List<FileItem> notMatches = controller.filterItems(all, "pdf NOT python NOT rust");
        assertThat(notMatches).containsExactly(item1);
    }
}

package com.demian.docsearch.ui;

import com.demian.docsearch.model.AppConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UIComponentsTest {

    @Test
    void testSearchControlsPanelGettersAndSetters() {
        AppConfig config = new AppConfig();
        config.setDirectory("D:/TestDir");
        config.setPattern("test*");
        config.setExtension("pdf,txt");
        config.setLimit(50);

        SearchControlsPanel panel = new SearchControlsPanel(config);
        assertThat(panel.getDirectory()).isEqualTo("D:/TestDir");
        assertThat(panel.getPattern()).isEqualTo("test*");
        assertThat(panel.getExtension()).isEqualTo("pdf,txt");
        assertThat(panel.getLimit()).isEqualTo(50);

        panel.setDirectory("C:/NewDir");
        panel.setPattern("*.java");
        panel.setExtension("java");
        panel.setLimit(100);

        assertThat(panel.getDirectory()).isEqualTo("C:/NewDir");
        assertThat(panel.getPattern()).isEqualTo("*.java");
        assertThat(panel.getExtension()).isEqualTo("java");
        assertThat(panel.getLimit()).isEqualTo(100);
    }

    @Test
    void testSearchFilterBar() {
        SearchFilterBar filterBar = new SearchFilterBar();
        assertThat(filterBar.getFilterText()).isEmpty();

        filterBar.setFilterText("sample query");
        assertThat(filterBar.getFilterText()).isEqualTo("sample query");

        filterBar.setProgress(25, 100);
        assertThat(filterBar.getProgressMaximum()).isEqualTo(100);

        filterBar.resetProgress();
    }

    @Test
    void testStatusBarPanel() {
        StatusBarPanel statusBar = new StatusBarPanel();
        statusBar.setStatus("Custom Status");
        statusBar.setCount("42 files found");
    }
}

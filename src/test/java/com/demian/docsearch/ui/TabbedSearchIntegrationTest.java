package com.demian.docsearch.ui;

import com.demian.docsearch.config.ConfigManager;
import com.demian.docsearch.db.FileIndexDatabase;
import com.demian.docsearch.model.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JTabbedPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TabbedSearchIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testThreeTabsPresentAndTab2ViewableWithDisabledControlsWhenAiNotConfigured() {
        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setAiApiKey(""); // Not configured
        configManager.save(config);

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("index.db"));
        DocSearchApp app = new DocSearchApp(configManager, db);

        try {
            JTabbedPane tabs = app.getTabbedPane();
            assertThat(tabs).isNotNull();
            assertThat(tabs.getTabCount()).isEqualTo(3);
            assertThat(tabs.getTitleAt(0)).contains("Standard Search");
            assertThat(tabs.getTitleAt(1)).contains("AI Search");
            assertThat(tabs.getTitleAt(2)).contains("AI Settings");

            // Tab 1 (AI Search) itself is viewable and enabled in JTabbedPane
            assertThat(tabs.isEnabledAt(0)).isTrue();
            assertThat(tabs.isEnabledAt(1)).isTrue();
            assertThat(tabs.isEnabledAt(2)).isTrue();

            // But inputs and buttons inside Tab 2 should be grayed out/disabled!
            AiSearchControlsPanel aiPanel = app.getAiControlsPanel();
            assertThat(aiPanel.isAiConfigured()).isFalse();
            assertThat(aiPanel.getTxtFolder().isEnabled()).isFalse();
            assertThat(aiPanel.getTxtPatterns().isEnabled()).isFalse();
            assertThat(aiPanel.getTxtExtensions().isEnabled()).isFalse();
            assertThat(aiPanel.getBtnSearch().isEnabled()).isFalse();
            assertThat(aiPanel.getBtnIndex().isEnabled()).isFalse();
            assertThat(aiPanel.getLblStatusWarning().isVisible()).isTrue();

            // When unconfigured, model dropdown has no options
            AiConfigPanel configPanel = app.getAiConfigPanel();
            assertThat(configPanel.getComboModel().getItemCount()).isEqualTo(0);
        } finally {
            app.dispose();
            db.close();
        }
    }

    @Test
    void testTab2ControlsEnabledWhenAiConfigured() {
        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setAiBaseUrl("https://api.openai.com/v1");
        config.setAiApiKey("sk-valid-test-key-12345");
        config.setAiModel("gpt-4o-mini");
        config.setAiTemperature(0.2);
        configManager.save(config);

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("index.db"));
        DocSearchApp app = new DocSearchApp(configManager, db);

        try {
            AiSearchControlsPanel aiPanel = app.getAiControlsPanel();
            assertThat(aiPanel.isAiConfigured()).isTrue();
            assertThat(aiPanel.getTxtFolder().isEnabled()).isTrue();
            assertThat(aiPanel.getTxtPatterns().isEnabled()).isTrue();
            assertThat(aiPanel.getTxtExtensions().isEnabled()).isTrue();
            assertThat(aiPanel.getBtnSearch().isEnabled()).isTrue();
            assertThat(aiPanel.getBtnIndex().isEnabled()).isTrue();
            assertThat(aiPanel.getLblStatusWarning().isVisible()).isFalse();
        } finally {
            app.dispose();
            db.close();
        }
    }

    @Test
    void testDirectorySelectionSyncsAcrossTabsAndSetsReindexButton() throws IOException {
        Path folder = tempDir.resolve("TargetFolder");
        Files.createDirectories(folder);
        Path sampleFile = folder.resolve("Document.pdf");
        Files.writeString(sampleFile, "content");

        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setAiApiKey("sk-valid-key");
        configManager.save(config);

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("index.db"));
        DocSearchApp app = new DocSearchApp(configManager, db);

        try {
            app.onNavFolderSelected(folder);

            assertThat(app.getControlsPanel().getDirectory())
                    .isEqualTo(folder.toAbsolutePath().toString());
            assertThat(app.getAiControlsPanel().getDirectory())
                    .isEqualTo(folder.toAbsolutePath().toString());

            // Initially not indexed in DB -> button text is "⚡ Index Folder"
            assertThat(app.getAiControlsPanel().getBtnIndex().getText()).contains("Index");
            assertThat(app.getAiControlsPanel().isIndexed()).isFalse();

            // Index directory into DB
            db.indexDirectory(folder, null, null);
            app.updateFolderIndexState();

            // Once indexed data exists in SQLite -> button text changes to "⚡ Reindex Folder"
            assertThat(app.getAiControlsPanel().getBtnIndex().getText()).contains("Reindex");
            assertThat(app.getAiControlsPanel().isIndexed()).isTrue();
        } finally {
            app.dispose();
            db.close();
        }
    }

    @Test
    void testAiConfigPanelSaveEnablesAiControlsAndPersistsTemperature() {
        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig initialConfig = new AppConfig();
        initialConfig.setAiApiKey(""); // empty
        configManager.save(initialConfig);

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("index.db"));
        DocSearchApp app = new DocSearchApp(configManager, db);

        try {
            assertThat(app.getAiControlsPanel().isAiConfigured()).isFalse();

            AiConfigPanel configPanel = app.getAiConfigPanel();
            configPanel.setBaseUrl("https://api.openai.com/v1");
            configPanel.setApiKey("sk-new-api-key");
            configPanel.setModel("gpt-4o");
            configPanel.setTemperature(0.3);
            configPanel.saveSettings();

            // After saving valid config, Tab 2 controls should be immediately enabled!
            assertThat(app.getAiControlsPanel().isAiConfigured()).isTrue();
            assertThat(app.getAiControlsPanel().getBtnSearch().isEnabled()).isTrue();

            AppConfig reloaded = configManager.load();
            assertThat(reloaded.getAiApiKey()).isEqualTo("sk-new-api-key");
            assertThat(reloaded.getAiModel()).isEqualTo("gpt-4o");
            assertThat(reloaded.getAiTemperature()).isEqualTo(0.3);
        } finally {
            app.dispose();
            db.close();
        }
    }

    @Test
    void testSystemPromptEditingAndReset() {
        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig initialConfig = new AppConfig();
        configManager.save(initialConfig);

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("index.db"));
        DocSearchApp app = new DocSearchApp(configManager, db);

        try {
            AiConfigPanel configPanel = app.getAiConfigPanel();
            assertThat(configPanel.getSystemPrompt()).isEqualTo(AppConfig.DEFAULT_AI_SYSTEM_PROMPT);
            assertThat(configPanel.getBtnEditPrompt()).isNotNull();
            assertThat(configPanel.getLblPromptSummary().getText()).contains("Standard default prompt");

            configPanel.setSystemPrompt("You are a specialized legal document finder.");
            assertThat(configPanel.getLblPromptSummary().getText()).contains("Custom prompt configured");
            configPanel.saveSettings();

            AppConfig reloaded = configManager.load();
            assertThat(reloaded.getAiSystemPrompt()).isEqualTo("You are a specialized legal document finder.");

            configPanel.getBtnResetPrompt().doClick();
            assertThat(configPanel.getSystemPrompt()).isEqualTo(AppConfig.DEFAULT_AI_SYSTEM_PROMPT);
            assertThat(configPanel.getLblPromptSummary().getText()).contains("Standard default prompt");
        } finally {
            app.dispose();
            db.close();
        }
    }

    @Test
    void testAiSearchControlsPanelGettersAndSetters() {
        AiSearchControlsPanel panel = new AiSearchControlsPanel("D:/InitialDir");
        assertThat(panel.getDirectory()).isEqualTo("D:/InitialDir");
        assertThat(panel.getPattern()).isEmpty();
        assertThat(panel.getExtension()).isEmpty();

        panel.setDirectory("C:/NewDir");
        panel.setPattern("quarterly tax reports");
        panel.setExtension("pdf, docx");

        assertThat(panel.getDirectory()).isEqualTo("C:/NewDir");
        assertThat(panel.getPattern()).isEqualTo("quarterly tax reports");
        assertThat(panel.getExtension()).isEqualTo("pdf, docx");

        panel.setIndexed(true);
        assertThat(panel.getBtnIndex().getText()).contains("Reindex");
        panel.setIndexed(false);
        assertThat(panel.getBtnIndex().getText()).contains("Index");
    }

    @Test
    void testAiSearchFilterResultsOnlyFiltersAiResults() throws IOException {
        Path folder = tempDir.resolve("FilterTestFolder");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("StandardDoc1.pdf"), "content1");
        Files.writeString(folder.resolve("StandardDoc2.txt"), "content2");

        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setAiApiKey("sk-key");
        configManager.save(config);

        FileIndexDatabase db = new FileIndexDatabase(tempDir.resolve("index.db"));
        DocSearchApp app = new DocSearchApp(configManager, db);

        try {
            // Select folder -> loads folder files into allResults (standard search)
            app.onNavFolderSelected(folder);
            assertThat(app.getAllResults()).hasSize(2);
            assertThat(app.getAiResults()).isEmpty();

            // When on Standard Search tab (Tab 0)
            app.getTabbedPane().setSelectedIndex(0);
            assertThat(app.getTablePanel().getTableModel().getRowCount()).isEqualTo(2);

            // Switch to AI Search tab (Tab 1) -> table shows 0 items (aiResults is empty)
            app.getTabbedPane().setSelectedIndex(1);
            assertThat(app.getTablePanel().getTableModel().getRowCount()).isEqualTo(0);

            // Simulate AI Search returning specific matched items
            com.demian.docsearch.model.FileItem aiItem1 = new com.demian.docsearch.model.FileItem(
                    folder.resolve("AI_Match_2023_Tax.pdf"), 2023, "Finance", 1000L, false);
            com.demian.docsearch.model.FileItem aiItem2 = new com.demian.docsearch.model.FileItem(
                    folder.resolve("AI_Match_2024_Budget.pdf"), 2024, "Finance", 2000L, false);

            app.getAiResults().add(aiItem1);
            app.getAiResults().add(aiItem2);
            app.applyLiveFilter();

            assertThat(app.getTablePanel().getTableModel().getRowCount()).isEqualTo(2);

            // Live filter on "Filter Results" while on AI search tab
            SearchFilterBar filterBar = app.getFilterBar();

            filterBar.setFilterText("2023");
            // Filtering on AI Search tab should ONLY filter from the 2 AI matches!
            assertThat(app.getTablePanel().getTableModel().getRowCount()).isEqualTo(1);
            assertThat(app.getTablePanel().getTableModel().getItems().get(0).name()).isEqualTo("AI_Match_2023_Tax.pdf");

            // Filter with NOT exclusion on AI results
            filterBar.setFilterText("NOT 2023");
            assertThat(app.getTablePanel().getTableModel().getRowCount()).isEqualTo(1);
            assertThat(app.getTablePanel().getTableModel().getItems().get(0).name()).isEqualTo("AI_Match_2024_Budget.pdf");

            // Switch back to Standard Search tab (Tab 0) -> table filters from Standard results
            filterBar.setFilterText("Doc1");
            app.getTabbedPane().setSelectedIndex(0);
            assertThat(app.getTablePanel().getTableModel().getRowCount()).isEqualTo(1);
            assertThat(app.getTablePanel().getTableModel().getItems().get(0).name()).isEqualTo("StandardDoc1.pdf");
        } finally {
            app.dispose();
            db.close();
        }
    }

    @Test
    void testAiConfigParallelRequestsSaveAndLoad() {
        Path configPath = tempDir.resolve("config_parallel.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setAiParallelRequests(6);

        AiConfigPanel panel = new AiConfigPanel(config, configManager);
        assertThat(panel.getParallelRequests()).isEqualTo(6);

        panel.setParallelRequests(12);
        assertThat(panel.getParallelRequests()).isEqualTo(12);
        panel.saveSettings();

        AppConfig loaded = configManager.load();
        assertThat(loaded.getAiParallelRequests()).isEqualTo(12);
    }
}

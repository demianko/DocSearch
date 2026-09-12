package com.demian.docsearch.ui;

import com.demian.docsearch.config.ConfigManager;
import com.demian.docsearch.model.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocSearchAppStartupTest {

    @Test
    void testAppStartupExpandsAndLoadsLastUsedDirectory(@TempDir Path tempDir) throws IOException {
        Path lastUsedDir = tempDir.resolve("Workspace").resolve("ProjectA");
        Files.createDirectories(lastUsedDir.resolve("SubDir"));
        Path sampleFile1 = lastUsedDir.resolve("notes.txt");
        Path sampleFile2 = lastUsedDir.resolve("document.pdf");
        Files.writeString(sampleFile1, "hello");
        Files.writeString(sampleFile2, "world");

        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setDirectory(lastUsedDir.toAbsolutePath().toString());
        configManager.save(config);

        DocSearchApp app = new DocSearchApp(configManager);
        try {
            assertThat(app.getControlsPanel().getDirectory())
                    .isEqualTo(lastUsedDir.toAbsolutePath().toString());

            assertThat(app.getExplorerNav().getSelectedPath())
                    .isEqualTo(lastUsedDir.toAbsolutePath().normalize());

            assertThat(app.getExplorerNav().getExpandedPaths())
                    .contains(lastUsedDir.toAbsolutePath().normalize());

            assertThat(app.getAllResults())
                    .hasSize(3)
                    .extracting(item -> item.path().getFileName().toString())
                    .containsExactlyInAnyOrder("SubDir", "notes.txt", "document.pdf");
        } finally {
            app.dispose();
        }
    }

    @Test
    void testAppStartupWithInvalidDirectoryFallsBackToCollapsedDrives(@TempDir Path tempDir) throws IOException {
        Path nonExistent = tempDir.resolve("DoesNotExistFolder");
        Path configPath = tempDir.resolve("config.json");
        ConfigManager configManager = new ConfigManager(configPath);
        AppConfig config = new AppConfig();
        config.setDirectory(nonExistent.toAbsolutePath().toString());
        configManager.save(config);

        DocSearchApp app = new DocSearchApp(configManager);
        try {
            assertThat(app.getExplorerNav().getSelectedPath()).isNull();
            assertThat(app.getAllResults()).isEmpty();
        } finally {
            app.dispose();
        }
    }
}

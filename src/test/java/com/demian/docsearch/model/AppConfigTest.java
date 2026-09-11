package com.demian.docsearch.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.demian.docsearch.config.ConfigManager;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void testConfigSaveAndLoad(@TempDir Path tempDir) throws Exception {
        Path cfgFile = tempDir.resolve("config.json");
        ConfigManager manager = new ConfigManager(cfgFile);

        AppConfig config = new AppConfig("D:\\downloads", "*.mp4", "mp4, mkv", "modified", "O'Reilly", 500);
        manager.save(config);

        AppConfig loaded = manager.load();
        assertThat(loaded.getDirectory()).isEqualTo("D:\\downloads");
        assertThat(loaded.getPattern()).isEqualTo("*.mp4");
        assertThat(loaded.getExtension()).isEqualTo("mp4, mkv");
        assertThat(loaded.getSortOrder()).isEqualTo("modified");
        assertThat(loaded.getPublisher()).isEqualTo("O'Reilly");
        assertThat(loaded.getLimit()).isEqualTo(500);

        String jsonContent = java.nio.file.Files.readString(cfgFile);
        assertThat(jsonContent).doesNotContain("filterResult");
    }

    @Test
    void testLegacyConfigWithFilterResultIgnored(@TempDir Path tempDir) throws Exception {
        Path cfgFile = tempDir.resolve("legacy_config.json");
        String legacyJson = """
            {
              "directory": "C:\\\\docs",
              "pattern": "*.pdf",
              "extension": "pdf",
              "sortOrder": "name",
              "publisher": "Manning",
              "limit": 50,
              "filterResult": "ignoredFilterText"
            }
            """;
        java.nio.file.Files.writeString(cfgFile, legacyJson);

        ConfigManager manager = new ConfigManager(cfgFile);
        AppConfig loaded = manager.load();
        assertThat(loaded.getDirectory()).isEqualTo("C:\\docs");
        assertThat(loaded.getLimit()).isEqualTo(50);
    }
}

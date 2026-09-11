package com.demian.docsearch.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.demian.docsearch.config.ConfigManager;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void testConfigSaveAndLoad(@TempDir Path tempDir) {
        Path cfgFile = tempDir.resolve("config.json");
        ConfigManager manager = new ConfigManager(cfgFile);

        AppConfig config = new AppConfig("D:\\downloads", "*.mp4", "mp4, mkv", "modified", "O'Reilly", 500, "sample");
        manager.save(config);

        AppConfig loaded = manager.load();
        assertThat(loaded.getDirectory()).isEqualTo("D:\\downloads");
        assertThat(loaded.getPattern()).isEqualTo("*.mp4");
        assertThat(loaded.getExtension()).isEqualTo("mp4, mkv");
        assertThat(loaded.getSortOrder()).isEqualTo("modified");
        assertThat(loaded.getPublisher()).isEqualTo("O'Reilly");
        assertThat(loaded.getLimit()).isEqualTo(500);
        assertThat(loaded.getFilterResult()).isEqualTo("sample");
    }
}

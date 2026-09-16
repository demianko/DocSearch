package com.demian.docsearch.config;

import com.demian.docsearch.model.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private final Path configPath;
    private final ObjectMapper mapper;

    public ConfigManager() {
        this(ConfigManager.getDefaultConfigPath());
    }

    public ConfigManager(Path configPath) {
        this.configPath = ObjectUtils.defaultIfNull(configPath, ConfigManager.getDefaultConfigPath());
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.ensureDirectoriesExist();
    }

    private void ensureDirectoriesExist() {
        try {
            Path configDir = this.getConfigDir();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            Path logsDir = this.getLogsDir();
            if (logsDir != null && !Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }
        } catch (Exception e) {
            log.warn("Could not pre-create config/logs directory: {}", e.getMessage());
        }
    }

    public static Path getDefaultConfigPath() {
        String userHome = StringUtils.defaultIfBlank(System.getProperty("user.home"), ".");
        return Paths.get(userHome, ".filesearch", "config.json");
    }

    public static Path getDefaultConfigDir() {
        String userHome = StringUtils.defaultIfBlank(System.getProperty("user.home"), ".");
        return Paths.get(userHome, ".filesearch");
    }

    public Path getConfigPath() {
        return this.configPath;
    }

    public Path getConfigDir() {
        return this.configPath.getParent() != null ? this.configPath.getParent() : getDefaultConfigDir();
    }

    public Path getLogsDir() {
        return this.getConfigDir().resolve("logs");
    }

    public AppConfig load() {
        if (!Files.exists(this.configPath, new LinkOption[0])) {
            Path legacyPath;
            if (this.configPath.getParent() != null && Files.exists(legacyPath = this.configPath.getParent().resolve("config"), new LinkOption[0])) {
                try {
                    AppConfig cfg = this.mapper.readValue(legacyPath.toFile(), AppConfig.class);
                    log.info("Loaded legacy config from {}", legacyPath);
                    return cfg;
                } catch (Exception e) {
                    log.warn("Failed to load legacy config from {}: {}", legacyPath, e.getMessage());
                }
            }
            log.debug("Config file does not exist at {}, returning default AppConfig", this.configPath);
            return new AppConfig();
        }
        try {
            AppConfig cfg = this.mapper.readValue(this.configPath.toFile(), AppConfig.class);
            log.debug("Successfully loaded config from {}", this.configPath);
            return cfg;
        } catch (IOException e) {
            log.error("Failed to read config from {}: {}", this.configPath, e.getMessage());
            return new AppConfig();
        }
    }

    public void save(AppConfig config) {
        if (config == null) return;
        try {
            if (this.configPath.getParent() != null) {
                Files.createDirectories(this.configPath.getParent(), new FileAttribute[0]);
            }
            this.mapper.writeValue(this.configPath.toFile(), config);
            log.debug("Saved config to {}", this.configPath);
        } catch (IOException e) {
            log.error("Failed to save config to {}: {}", this.configPath, e.getMessage());
        }
    }
}

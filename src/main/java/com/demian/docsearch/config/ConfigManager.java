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

public class ConfigManager {
    private final Path configPath;
    private final ObjectMapper mapper;

    public ConfigManager() {
        this(ConfigManager.getDefaultConfigPath());
    }

    public ConfigManager(Path configPath) {
        this.configPath = ObjectUtils.defaultIfNull(configPath, ConfigManager.getDefaultConfigPath());
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static Path getDefaultConfigPath() {
        String userHome = StringUtils.defaultIfBlank(System.getProperty("user.home"), ".");
        return Paths.get(userHome, ".filesearch", "config.json");
    }

    public AppConfig load() {
        if (!Files.exists(this.configPath, new LinkOption[0])) {
            Path legacyPath;
            if (this.configPath.getParent() != null && Files.exists(legacyPath = this.configPath.getParent().resolve("config"), new LinkOption[0])) {
                try {
                    return this.mapper.readValue(legacyPath.toFile(), AppConfig.class);
                }
                catch (Exception e) {
                    System.err.println("Failed to load legacy config: " + e.getMessage());
                }
            }
            return new AppConfig();
        }
        try {
            return this.mapper.readValue(this.configPath.toFile(), AppConfig.class);
        }
        catch (IOException e) {
            return new AppConfig();
        }
    }

    public void save(AppConfig config) {
        if (config == null) return;
        try {
            if (this.configPath.getParent() != null) {
                Files.createDirectories(this.configPath.getParent(), new FileAttribute[0]);
            }
            this.mapper.writeValue(this.configPath.toFile(), (Object)config);
        }
        catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}


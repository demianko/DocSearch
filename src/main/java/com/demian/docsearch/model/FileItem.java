package com.demian.docsearch.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public record FileItem(Path path, int year, String publisher, long modified, boolean isDirectory, long sizeBytes) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public FileItem(Path path, int year, String publisher, long modified, boolean isDirectory) {
        this(path, year, publisher, modified, isDirectory, FileItem.calculateSize(path, isDirectory));
    }

    private static long calculateSize(Path path, boolean isDirectory) {
        if (isDirectory || path == null) {
            return -1L;
        }
        try {
            return Files.size(path);
        }
        catch (IOException e) {
            System.err.println("Failed to calculate size for " + String.valueOf(path) + ": " + e.getMessage());
            return -1L;
        }
    }

    public String name() {
        if (this.path == null) {
            return "";
        }
        return this.path.getFileName() != null ? this.path.getFileName().toString() : this.path.toString();
    }

    public String nameDisplay() {
        return this.isDirectory ? "\ud83d\udcc1  " + this.name() : this.name();
    }

    public String parentStr() {
        if (this.path == null) {
            return "";
        }
        return this.path.getParent() != null ? this.path.getParent().toString() : "";
    }

    public String publisherDisplay() {
        if (this.isDirectory) {
            return "Folder";
        }
        if (StringUtils.isBlank(this.publisher)) {
            return "Unknown";
        }
        return StringUtils.capitalize(this.publisher.toLowerCase(Locale.ROOT));
    }

    public String yearDisplay() {
        return this.isDirectory || this.year <= 0 ? "-" : String.valueOf(this.year);
    }

    public String dateModifiedStr() {
        if (this.modified <= 0L) {
            return "-";
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.modified), ZoneId.systemDefault());
        return DATE_FORMATTER.format(ldt);
    }

    public String sizeDisplay() {
        if (this.isDirectory) {
            return "Folder";
        }
        if (this.sizeBytes < 0L) {
            return "-";
        }
        if (this.sizeBytes < 1024L) {
            return this.sizeBytes + " B";
        }
        int exp = (int)(Math.log(this.sizeBytes) / Math.log(1024.0));
        String pre = "" + "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.2f %sB", (double)this.sizeBytes / Math.pow(1024.0, exp), pre);
    }

    public FileItem withPath(Path newPath) {
        return new FileItem(newPath, this.year, this.publisher, this.modified, this.isDirectory, this.sizeBytes);
    }

    public FileItem withMetadata(String newPublisher, int newYear) {
        return new FileItem(this.path, newYear, newPublisher, this.modified, this.isDirectory, this.sizeBytes);
    }
}


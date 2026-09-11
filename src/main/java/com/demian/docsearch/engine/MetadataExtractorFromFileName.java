package com.demian.docsearch.engine;

import com.demian.docsearch.model.FileItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class MetadataExtractorFromFileName {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
    private static final Pattern PUBLISHER_PATTERN = Pattern.compile("([^\\-]+)-");

    public static int extractYear(String filename) {
        if (StringUtils.isBlank(filename)) {
            return 0;
        }
        Matcher matcher = YEAR_PATTERN.matcher(filename);
        if (matcher.find()) {
            return NumberUtils.toInt(matcher.group(), 0);
        }
        return 0;
    }

    public static String extractPublisher(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "unknown";
        }
        Matcher matcher = PUBLISHER_PATTERN.matcher(filename);
        if (matcher.find()) {
            String pub = StringUtils.lowerCase(StringUtils.trimToEmpty(matcher.group(1)));
            return StringUtils.defaultIfBlank(pub, "unknown");
        }
        return "unknown";
    }

    public static FileItem createFileItem(Path filePath, Long mtime) {
        long modifiedTime;
        if (mtime == null) {
            try {
                modifiedTime = Files.getLastModifiedTime(filePath, new LinkOption[0]).toMillis();
            }
            catch (IOException e) {
                System.err.println("Failed to get last modified time for " + String.valueOf(filePath) + ": " + e.getMessage());
                modifiedTime = 0L;
            }
        } else {
            modifiedTime = mtime;
        }
        String filename = filePath.getFileName() != null ? filePath.getFileName().toString() : filePath.toString();
        int year = MetadataExtractorFromFileName.extractYear(filename);
        String publisher = MetadataExtractorFromFileName.extractPublisher(filename);
        boolean isDir = Files.isDirectory(filePath, new LinkOption[0]);
        return new FileItem(filePath, year, publisher, modifiedTime, isDir);
    }
}


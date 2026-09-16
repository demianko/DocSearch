package com.demian.docsearch.db;

import com.demian.docsearch.engine.MetadataExtractorFromFileName;
import com.demian.docsearch.model.FileItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public record IndexedFileRecord(
        long id,
        String rootFolder,
        String parentFolder,
        String fileName,
        String fullPath,
        String extension,
        long sizeBytes,
        long lastModified,
        int year,
        String publisher,
        long indexedAt
) {
    public IndexedFileRecord {
        Objects.requireNonNull(rootFolder, "rootFolder must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(fullPath, "fullPath must not be null");
    }

    public FileItem toFileItem() {
        Path path = Paths.get(this.fullPath);
        return new FileItem(path, this.year, this.publisher, this.lastModified, false, this.sizeBytes);
    }

    public static IndexedFileRecord createFromPath(Path file, Path rootFolder) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(rootFolder, "rootFolder must not be null");

        String name = file.getFileName() != null ? file.getFileName().toString() : file.toString();
        String full = file.toAbsolutePath().normalize().toString();
        String root = rootFolder.toAbsolutePath().normalize().toString();
        String parent = file.getParent() != null ? file.getParent().toAbsolutePath().normalize().toString() : root;
        String ext = StringUtils.lowerCase(StringUtils.substringAfterLast(name, "."));

        long size = -1L;
        long mtime = System.currentTimeMillis();
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class, new LinkOption[0]);
            size = attrs.size();
            mtime = attrs.lastModifiedTime().toMillis();
        } catch (IOException ignored) {
        }

        int year = MetadataExtractorFromFileName.extractYear(name);
        String publisher = MetadataExtractorFromFileName.extractPublisher(name);

        return new IndexedFileRecord(0L, root, parent, name, full, ext, size, mtime, year, publisher, System.currentTimeMillis());
    }
}

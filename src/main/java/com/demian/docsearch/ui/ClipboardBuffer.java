package com.demian.docsearch.ui;

import com.demian.docsearch.config.WindowsClipboardHelper;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

public class ClipboardBuffer {
    public enum Mode {
        COPY,
        CUT
    }

    private final List<Path> paths = new ArrayList<>();
    private Mode mode = Mode.COPY;

    public void copy(List<Path> pathsToCopy) {
        this.paths.clear();
        if (CollectionUtils.isNotEmpty(pathsToCopy)) {
            this.paths.addAll(pathsToCopy);
            this.mode = Mode.COPY;
            WindowsClipboardHelper.copyFilesToClipboard(pathsToCopy);
        }
    }

    public void cut(List<Path> pathsToCut) {
        this.paths.clear();
        if (CollectionUtils.isNotEmpty(pathsToCut)) {
            this.paths.addAll(pathsToCut);
            this.mode = Mode.CUT;
            WindowsClipboardHelper.copyFilesToClipboard(pathsToCut);
        }
    }

    public void clear() {
        this.paths.clear();
        this.mode = Mode.COPY;
    }

    public List<Path> getLocalPaths() {
        return new ArrayList<>(this.paths);
    }

    public List<Path> getPaths() {
        if (!this.paths.isEmpty()) {
            return new ArrayList<>(this.paths);
        }
        // Fallback: system clipboard files
        List<File> files = WindowsClipboardHelper.getFilesFromClipboard();
        if (CollectionUtils.isNotEmpty(files)) {
            return files.stream().map(File::toPath).toList();
        }
        return List.of();
    }

    public Mode getMode() {
        return this.mode;
    }

    public boolean hasLocalContent() {
        return !this.paths.isEmpty();
    }

    public boolean hasContent() {
        if (!this.paths.isEmpty()) {
            return true;
        }
        return CollectionUtils.isNotEmpty(WindowsClipboardHelper.getFilesFromClipboard());
    }

    public boolean isCut() {
        return this.mode == Mode.CUT;
    }
}

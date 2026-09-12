package com.demian.docsearch.ui;

import com.demian.docsearch.config.WindowsClipboardHelper;
import com.demian.docsearch.model.FileItem;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class FileOperationsService {

    public void openItem(FileItem item, Component parent, Consumer<Path> onFolderSelected) {
        if (item == null) return;
        if (item.isDirectory()) {
            if (onFolderSelected != null) {
                onFolderSelected.accept(item.path());
            }
        } else if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(item.path().toFile());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Could not open file:\n" + e.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void openInExplorer(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths) || !Desktop.isDesktopSupported()) {
            return;
        }
        Path target = paths.getFirst();
        try {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", target.toAbsolutePath().toString()});
        } catch (IOException e) {
            try {
                Desktop.getDesktop().open(target.getParent().toFile());
            } catch (IOException ee) {
                System.err.println("Failed to open folder in explorer: " + ee.getMessage());
            }
        }
    }

    public boolean copyToClipboard(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) return false;
        return WindowsClipboardHelper.copyFilesToClipboard(paths);
    }

    public boolean copyPathsText(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) return false;
        String text = StringUtils.join(paths, System.lineSeparator());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        return true;
    }

    public int deleteItems(Component parent,
                           List<Path> paths,
                           Path currentFolder,
                           Consumer<Path> onFolderDeleted,
                           Consumer<Boolean> onCurrentFolderDeletedStatus,
                           Runnable onRefreshCurrent) {
        if (CollectionUtils.isEmpty(paths)) return 0;

        String message;
        if (paths.size() == 1) {
            Path p = paths.getFirst();
            boolean isDir = Files.isDirectory(p);
            String name = p.getFileName() != null ? p.getFileName().toString() : p.toString();
            if (isDir) {
                message = "Are you sure you want to permanently delete folder '" + name + "' and all of its contents?";
            } else {
                message = "Are you sure you want to permanently delete '" + name + "'?";
            }
        } else {
            message = "Are you sure you want to permanently delete these " + paths.size() + " selected items?";
        }

        if (parent != null) {
            int choice = JOptionPane.showConfirmDialog(
                    parent,
                    message,
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return 0;
            }
        }

        int deletedCount = 0;
        List<String> errors = new ArrayList<>();
        boolean curFolderDeleted = false;

        for (Path p : paths) {
            if (!Files.exists(p, new LinkOption[0])) continue;
            if (currentFolder != null && (p.equals(currentFolder) || currentFolder.startsWith(p))) {
                curFolderDeleted = true;
            }
            try {
                if (Files.isDirectory(p)) {
                    FileUtils.deleteDirectory(p.toFile());
                    if (onFolderDeleted != null) {
                        onFolderDeleted.accept(p);
                    }
                } else {
                    Files.deleteIfExists(p);
                }
                deletedCount++;
            } catch (Exception e) {
                errors.add(p.getFileName() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty() && parent != null) {
            JOptionPane.showMessageDialog(parent,
                    "Some items could not be deleted:\n" + String.join("\n", errors),
                    "Delete Error", JOptionPane.ERROR_MESSAGE);
        }

        if (onCurrentFolderDeletedStatus != null) {
            onCurrentFolderDeletedStatus.accept(curFolderDeleted);
        } else if (onRefreshCurrent != null) {
            onRefreshCurrent.run();
        }

        return deletedCount;
    }

    public int moveItems(Component parent,
                         List<File> files,
                         Path targetDir,
                         BiConsumer<Path, Path> onFolderRenamed,
                         Runnable onCompleteRefresh) {
        if (CollectionUtils.isEmpty(files) || targetDir == null || !Files.isDirectory(targetDir)) {
            return 0;
        }
        Path normTarget = targetDir.toAbsolutePath().normalize();
        int movedCount = 0;
        List<String> errors = new ArrayList<>();

        for (File file : files) {
            Path src = file.toPath().toAbsolutePath().normalize();
            if (!Files.exists(src, new LinkOption[0])) continue;
            Path dest = normTarget.resolve(src.getFileName()).toAbsolutePath().normalize();

            if (src.equals(dest)) continue;
            if (src.getParent() != null && src.getParent().equals(normTarget)) continue;
            if (Files.isDirectory(src) && normTarget.startsWith(src)) {
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent,
                            "Cannot move folder '" + src.getFileName() + "' into a subdirectory of itself.",
                            "Invalid Move", JOptionPane.WARNING_MESSAGE);
                }
                continue;
            }

            if (Files.exists(dest, new LinkOption[0])) {
                String itemType = Files.isDirectory(dest) ? "folder" : "file";
                String msg = "A " + itemType + " named '" + dest.getFileName()
                        + "' already exists in '" + targetDir.getFileName() + "'.\n"
                        + "Do you want to overwrite it?";
                int confirm = parent != null ? JOptionPane.showConfirmDialog(
                        parent,
                        msg,
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                ) : JOptionPane.YES_OPTION;
                if (confirm != JOptionPane.YES_OPTION) {
                    continue;
                }
                try {
                    if (Files.isDirectory(dest)) {
                        FileUtils.deleteDirectory(dest.toFile());
                    } else {
                        Files.deleteIfExists(dest);
                    }
                } catch (Exception e) {
                    errors.add("Could not replace existing " + itemType + " " + dest.getFileName() + ": " + e.getMessage());
                    continue;
                }
            }

            try {
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
                movedCount++;
                if (Files.isDirectory(dest) && onFolderRenamed != null) {
                    onFolderRenamed.accept(src, dest);
                }
            } catch (Exception e) {
                errors.add(src.getFileName() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty() && parent != null) {
            JOptionPane.showMessageDialog(parent,
                    "Some items could not be moved:\n" + String.join("\n", errors),
                    "Move Error", JOptionPane.ERROR_MESSAGE);
        }

        if (movedCount > 0 && onCompleteRefresh != null) {
            onCompleteRefresh.run();
        }
        return movedCount;
    }

    public int pasteItems(Component parent,
                          ClipboardBuffer buffer,
                          Path targetFolder,
                          BiConsumer<Path, Path> onFolderMoved,
                          Runnable onCompleteRefresh) {
        if (buffer == null || targetFolder == null || !Files.isDirectory(targetFolder)) {
            return 0;
        }
        List<Path> items = buffer.getPaths();
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }

        Path normTarget = targetFolder.toAbsolutePath().normalize();
        int processedCount = 0;
        List<String> errors = new ArrayList<>();
        boolean isCut = buffer.isCut();

        for (Path src : items) {
            Path normSrc = src.toAbsolutePath().normalize();
            if (!Files.exists(normSrc, new LinkOption[0])) continue;

            Path dest = normTarget.resolve(normSrc.getFileName()).toAbsolutePath().normalize();
            if (normSrc.equals(dest)) continue;

            if (isCut && Files.isDirectory(normSrc) && normTarget.startsWith(normSrc)) {
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent,
                            "Cannot move folder '" + normSrc.getFileName() + "' into a subdirectory of itself.",
                            "Invalid Move", JOptionPane.WARNING_MESSAGE);
                }
                continue;
            }

            if (Files.exists(dest, new LinkOption[0])) {
                String itemType = Files.isDirectory(dest) ? "folder" : "file";
                String actionStr = isCut ? "Move" : "Copy";
                if (parent != null) {
                    JOptionPane.showMessageDialog(
                            parent,
                            "A " + itemType + " with the name '" + dest.getFileName() + "' already exists in '"
                                    + normTarget.getFileName() + "'.\n" + actionStr + " cannot be completed.",
                            "Naming Conflict",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                continue;
            }

            try {
                if (isCut) {
                    Files.move(normSrc, dest);
                    processedCount++;
                    if (Files.isDirectory(dest) && onFolderMoved != null) {
                        onFolderMoved.accept(normSrc, dest);
                    }
                } else {
                    if (Files.isDirectory(normSrc)) {
                        FileUtils.copyDirectory(normSrc.toFile(), dest.toFile());
                    } else {
                        Files.copy(normSrc, dest, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                    processedCount++;
                }
            } catch (Exception e) {
                errors.add((isCut ? "Move " : "Copy ") + normSrc.getFileName() + ": " + e.getMessage());
            }
        }

        if (isCut && processedCount > 0) {
            buffer.clear();
        }

        if (!errors.isEmpty() && parent != null) {
            JOptionPane.showMessageDialog(parent,
                    "Some items could not be processed:\n" + String.join("\n", errors),
                    "Operation Error", JOptionPane.ERROR_MESSAGE);
        }

        if (processedCount > 0 && onCompleteRefresh != null) {
            onCompleteRefresh.run();
        }

        return processedCount;
    }
}

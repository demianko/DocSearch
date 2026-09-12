package com.demian.docsearch.config;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class WindowsClipboardHelper {
    private static final DataFlavor[] SUPPORTED_FLAVORS = new DataFlavor[]{DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor};

    public static boolean copyFilesToClipboard(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return false;
        }
        ArrayList<File> fileList = new ArrayList<File>();
        ArrayList<String> pathStrings = new ArrayList<String>();
        for (Path p : paths) {
            if (p == null) continue;
            fileList.add(p.toFile());
            pathStrings.add(p.toAbsolutePath().toString());
        }
        if (CollectionUtils.isEmpty(fileList)) {
            return false;
        }
        String plainText = StringUtils.join(pathStrings, System.lineSeparator());
        FileListAndTextTransferable transferable = new FileListAndTextTransferable(fileList, plainText);
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(transferable, null);
            return true;
        }
        catch (Exception e) {
            System.err.println("Failed to copy to clipboard: " + e.getMessage());
            return false;
        }
    }

    public static List<File> getFilesFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                if (files != null) {
                    return files;
                }
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private static class FileListAndTextTransferable
    implements Transferable {
        private final List<File> files;
        private final String text;

        public FileListAndTextTransferable(List<File> files, String text) {
            this.files = files;
            this.text = text;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return (DataFlavor[])SUPPORTED_FLAVORS.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                return this.files;
            }
            if (DataFlavor.stringFlavor.equals(flavor)) {
                return this.text;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}


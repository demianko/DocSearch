package com.demian.docsearch.ui;

import com.demian.docsearch.ui.DocExplorerNav.FolderNodeUserObject;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class DocExplorerTransferHandler extends TransferHandler {
    private final BiConsumer<List<File>, Path> onMoveCallback;

    public DocExplorerTransferHandler(BiConsumer<List<File>, Path> onMoveCallback) {
        this.onMoveCallback = onMoveCallback;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || !support.isDrop()) {
            return false;
        }
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath path = dl.getPath();
        if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof FolderNodeUserObject) {
            support.setShowDropLocation(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();
            FolderNodeUserObject uo = (FolderNodeUserObject) node.getUserObject();
            Path targetDir = uo.getPath();
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (this.onMoveCallback != null && files != null && !files.isEmpty()) {
                this.onMoveCallback.accept(files, targetDir);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

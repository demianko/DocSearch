package com.demian.docsearch.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

public class DocExplorerNav extends JPanel {
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final DocExplorerTreeManager treeManager;

    public DocExplorerNav(Consumer<Path> onSelectCallback, Consumer<Path> onRenameCallback) {
        this(onSelectCallback, onRenameCallback, null, null, null, null);
    }

    public DocExplorerNav(Consumer<Path> onSelectCallback, Consumer<Path> onRenameCallback,
                          Consumer<Path> onNewFolderCallback, Consumer<Path> onNewFileCallback) {
        this(onSelectCallback, onRenameCallback, onNewFolderCallback, onNewFileCallback, null, null);
    }

    public DocExplorerNav(Consumer<Path> onSelectCallback, Consumer<Path> onRenameCallback,
                          Consumer<Path> onNewFolderCallback, Consumer<Path> onNewFileCallback,
                          Consumer<Path> onDeleteCallback, BiConsumer<List<File>, Path> onMoveCallback) {
        this(onSelectCallback, onRenameCallback, onNewFolderCallback, onNewFileCallback,
                onDeleteCallback, onMoveCallback, null, null, null, null);
    }

    public DocExplorerNav(Consumer<Path> onSelectCallback, Consumer<Path> onRenameCallback,
                          Consumer<Path> onNewFolderCallback, Consumer<Path> onNewFileCallback,
                          Consumer<Path> onDeleteCallback, BiConsumer<List<File>, Path> onMoveCallback,
                          Consumer<Path> onCopyCallback, Consumer<Path> onCutCallback,
                          Consumer<Path> onPasteCallback, java.util.function.Supplier<Boolean> canPasteSupplier) {
        super(new BorderLayout());
        this.setPreferredSize(new Dimension(280, 600));
        this.setMinimumSize(new Dimension(180, 300));
        this.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
        JLabel lblTitle = new JLabel("\ud83d\udcc1 Explorer");
        lblTitle.setFont(lblTitle.getFont().deriveFont(1, 12.0f));
        JButton btnRefresh = new JButton("\u21bb");
        btnRefresh.setPreferredSize(new Dimension(28, 24));
        btnRefresh.setToolTipText("Refresh drives and directories");
        btnRefresh.addActionListener(e -> this.refresh());
        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(btnRefresh, BorderLayout.EAST);
        this.add(headerPanel, BorderLayout.NORTH);

        this.rootNode = new DefaultMutableTreeNode("Root");
        this.treeModel = new DefaultTreeModel(this.rootNode);
        this.tree = new JTree(this.treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.tree.getSelectionModel().setSelectionMode(1);
        this.tree.setRowHeight(24);
        this.tree.setDragEnabled(false);
        this.tree.setDropMode(DropMode.ON);

        this.treeManager = new DocExplorerTreeManager(this.tree, this.treeModel, this.rootNode);

        this.tree.setTransferHandler(new DocExplorerTransferHandler(onMoveCallback));

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        this.tree.setCellRenderer(renderer);

        this.tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                DocExplorerNav.this.treeManager.expandNodeLazily(node);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });

        this.tree.addTreeSelectionListener(e -> {
            if (this.treeManager.isSuppressSelectEvent()) return;
            TreePath path = this.tree.getSelectionPath();
            if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode treeNode
                    && treeNode.getUserObject() instanceof FolderNodeUserObject uo) {
                if (onSelectCallback != null) {
                    onSelectCallback.accept(uo.getPath());
                }
            }
        });

        new DocExplorerContextMenu(
                this.tree, this.treeManager, this,
                onNewFolderCallback, onNewFileCallback,
                onRenameCallback, onDeleteCallback, onSelectCallback,
                onCopyCallback, onCutCallback, onPasteCallback, canPasteSupplier
        );

        this.treeManager.populateDrives();

        JScrollPane scrollPane = new JScrollPane(this.tree);
        scrollPane.setBorder(BorderFactory.createLineBorder(this.tree.getBackground().darker()));
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public Path getSelectedPath() {
        return this.treeManager.getSelectedPath();
    }

    public void selectPath(Path targetPath, boolean expandTarget) {
        this.treeManager.selectPath(targetPath, expandTarget);
    }

    public void onFolderRenamed(Path oldPath, Path newPath) {
        this.treeManager.onFolderRenamed(oldPath, newPath);
    }

    public void onFolderAdded(Path parentFolder, Path newFolder) {
        this.treeManager.onFolderAdded(parentFolder, newFolder);
    }

    public void onFolderDeleted(Path folderPath) {
        this.treeManager.onFolderDeleted(folderPath);
    }

    public void expandPath(Path targetPath) {
        this.treeManager.expandPath(targetPath);
    }

    public List<TreePath> getExpandedTreePaths() {
        return this.treeManager.getExpandedTreePaths();
    }

    public void restoreExpandedTreePaths(List<TreePath> paths) {
        this.treeManager.restoreExpandedTreePaths(paths);
    }

    public List<Path> getExpandedPaths() {
        return this.treeManager.getExpandedPaths();
    }

    public void populateDrives() {
        this.treeManager.populateDrives();
    }

    public void refresh() {
        this.treeManager.refresh();
    }

    public static class FolderNodeUserObject {
        private final Path path;
        private final String displayName;

        public FolderNodeUserObject(Path path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }

        public Path getPath() {
            return this.path;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}

package com.demian.docsearch.ui;

import com.demian.docsearch.constant.AppConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

public class DocExplorerNav
extends JPanel {
    private final Consumer<Path> onSelectCallback;
    private final Consumer<Path> onRenameCallback;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final Map<Path, DefaultMutableTreeNode> pathToNodeMap = new HashMap<Path, DefaultMutableTreeNode>();
    private boolean suppressSelectEvent = false;

    public DocExplorerNav(Consumer<Path> onSelectCallback, Consumer<Path> onRenameCallback) {
        super(new BorderLayout());
        this.onSelectCallback = onSelectCallback;
        this.onRenameCallback = onRenameCallback;
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
        headerPanel.add((Component)lblTitle, "West");
        headerPanel.add((Component)btnRefresh, "East");
        this.add((Component)headerPanel, "North");
        this.rootNode = new DefaultMutableTreeNode("Root");
        this.treeModel = new DefaultTreeModel(this.rootNode);
        this.tree = new JTree(this.treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.tree.getSelectionModel().setSelectionMode(1);
        this.tree.setRowHeight(24);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        this.tree.setCellRenderer(renderer);
        this.tree.addTreeWillExpandListener(new TreeWillExpandListener(){

            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
                DocExplorerNav.this.expandNodeLazily(node);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });
        this.tree.addTreeSelectionListener(e -> {
            if (this.suppressSelectEvent) return;
            TreePath path = this.tree.getSelectionPath();
            if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode treeNode
                    && treeNode.getUserObject() instanceof FolderNodeUserObject uo) {
                if (onSelectCallback != null) {
                    onSelectCallback.accept(uo.getPath());
                }
            }
        });
        this.setupContextMenu();
        this.populateDrives();
        JScrollPane scrollPane = new JScrollPane(this.tree);
        scrollPane.setBorder(BorderFactory.createLineBorder(this.tree.getBackground().darker()));
        this.add((Component)scrollPane, "Center");
    }

    private void setupContextMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem miRename = new JMenuItem("\u270f\ufe0f Rename");
        miRename.setAccelerator(KeyStroke.getKeyStroke(113, 0));
        miRename.addActionListener(e -> this.triggerRename());
        JMenuItem miCopyPath = new JMenuItem("\ud83d\udccb Copy Full Path");
        miCopyPath.addActionListener(e -> {
            Path p = this.getSelectedPath();
            if (p != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(p.toAbsolutePath().toString()), null);
            }
        });
        JMenuItem miOpenExplorer = new JMenuItem("\ud83d\udcc1 Open in Explorer");
        miOpenExplorer.addActionListener(event -> {
            Path p = this.getSelectedPath();
            if (p != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(p.toFile());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        menu.add(miRename);
        menu.addSeparator();
        menu.add(miCopyPath);
        menu.add(miOpenExplorer);
        this.tree.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                int row;
                if (e.isPopupTrigger() && (row = DocExplorerNav.this.tree.getRowForLocation(e.getX(), e.getY())) >= 0) {
                    DocExplorerNav.this.tree.setSelectionRow(row);
                    DocExplorerNav.this.tree.requestFocusInWindow();
                    Path p = DocExplorerNav.this.getSelectedPath();
                    boolean isDriveRoot = p != null && p.getParent() == null;
                    miRename.setEnabled(!isDriveRoot);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        this.tree.registerKeyboardAction(event -> this.triggerRename(), KeyStroke.getKeyStroke(113, 0), 0);
    }

    public Path getSelectedPath() {
        DefaultMutableTreeNode node;
        Object object;
        TreePath path = this.tree.getSelectionPath();
        if (path != null && (object = (node = (DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject()) instanceof FolderNodeUserObject) {
            FolderNodeUserObject uo = (FolderNodeUserObject)object;
            return uo.getPath();
        }
        return null;
    }

    private void triggerRename() {
        Path p = this.getSelectedPath();
        if (p != null && this.onRenameCallback != null) {
            if (p.getParent() == null) {
                return;
            }
            this.onRenameCallback.accept(p);
        }
    }

    public void populateDrives() {
        this.rootNode.removeAllChildren();
        this.pathToNodeMap.clear();
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                Path rootPath = root.toPath();
                String displayName = fsv.getSystemDisplayName(root);
                if (displayName == null || displayName.isBlank()) {
                    displayName = root.getAbsolutePath();
                }
                DefaultMutableTreeNode driveNode = new DefaultMutableTreeNode(new FolderNodeUserObject(rootPath, "\ud83d\uddb4  " + displayName));
                driveNode.add(new DefaultMutableTreeNode("__dummy__"));
                this.rootNode.add(driveNode);
                this.pathToNodeMap.put(rootPath.toAbsolutePath().normalize(), driveNode);
            }
        }
        this.treeModel.reload();
    }

    private void expandNodeLazily(DefaultMutableTreeNode node) {
        if (node.getChildCount() != 1) {
            return;
        }
        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)node.getFirstChild();
        if (!"__dummy__".equals(firstChild.getUserObject())) {
            return;
        }
        node.removeAllChildren();
        Object object = node.getUserObject();
        if (object instanceof FolderNodeUserObject) {
            FolderNodeUserObject uo = (FolderNodeUserObject)object;
            Path dir = uo.getPath();
            ArrayList<Path> subdirs = new ArrayList<Path>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir);){
                for (Path child : stream) {
                    Path fn = child.getFileName();
                    if (fn == null || AppConstants.SYSTEM_IGNORED_DIRS.contains(fn.toString().toUpperCase())
                            || !Files.isDirectory(child)) {
                        continue;
                    }
                    subdirs.add(child);
                }
            }
            catch (IOException e) {
                System.err.println("Error reading directory " + String.valueOf(dir) + ": " + e.getMessage());
            }
            subdirs.sort(Comparator.comparing(p -> p.getFileName() != null ? p.getFileName().toString().toLowerCase() : ""));
            for (Path sub : subdirs) {
                String name = sub.getFileName() != null ? sub.getFileName().toString() : sub.toString();
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FolderNodeUserObject(sub, "\ud83d\udcc1  " + name));
                if (this.hasSubdirectories(sub)) {
                    childNode.add(new DefaultMutableTreeNode("__dummy__"));
                }
                node.add(childNode);
                this.pathToNodeMap.put(sub.toAbsolutePath().normalize(), childNode);
            }
        }
        this.treeModel.nodeStructureChanged(node);
    }

    private boolean hasSubdirectories(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                Path fn = child.getFileName();
                if (fn != null && !AppConstants.SYSTEM_IGNORED_DIRS.contains(fn.toString().toUpperCase())
                        && Files.isDirectory(child)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public void selectPath(Path targetPath, boolean expandTarget) {
        if (targetPath == null || !Files.exists(targetPath, new LinkOption[0])) {
            return;
        }
        Path normTarget = targetPath.toAbsolutePath().normalize();
        Path root = normTarget.getRoot();
        if (root == null) {
            return;
        }
        this.suppressSelectEvent = true;
        try {
            DefaultMutableTreeNode currentNode = this.pathToNodeMap.get(root);
            if (currentNode == null) {
                return;
            }
            this.tree.expandPath(new TreePath(currentNode.getPath()));
            this.expandNodeLazily(currentNode);
            int nameCount = normTarget.getNameCount();
            Path currentPath = root;
            for (int i = 0; i < nameCount; ++i) {
                DefaultMutableTreeNode nextNode = this.pathToNodeMap.get(currentPath = currentPath.resolve(normTarget.getName(i)));
                if (nextNode == null) {
                    this.expandNodeLazily(currentNode);
                    nextNode = this.pathToNodeMap.get(currentPath);
                }
                if (nextNode == null) break;
                currentNode = nextNode;
                this.tree.expandPath(new TreePath(currentNode.getPath()));
            }
            if (currentNode != null) {
                if (expandTarget) {
                    this.expandNodeLazily(currentNode);
                    this.tree.expandPath(new TreePath(currentNode.getPath()));
                }
                TreePath treePath = new TreePath(currentNode.getPath());
                this.tree.setSelectionPath(treePath);
                this.tree.scrollPathToVisible(treePath);
            }
        }
        finally {
            this.suppressSelectEvent = false;
        }
    }

    public void refresh() {
        Path selected = this.getSelectedPath();
        this.populateDrives();
        if (selected != null && Files.exists(selected, new LinkOption[0])) {
            this.selectPath(selected, false);
        }
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

        public String toString() {
            return this.displayName;
        }
    }
}


package com.demian.docsearch.ui;

import com.demian.docsearch.constant.AppConstants;
import com.demian.docsearch.ui.DocExplorerNav.FolderNodeUserObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class DocExplorerTreeManager {
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final Map<Path, DefaultMutableTreeNode> pathToNodeMap = new HashMap<>();
    private boolean suppressSelectEvent = false;

    public DocExplorerTreeManager(JTree tree, DefaultTreeModel treeModel, DefaultMutableTreeNode rootNode) {
        this.tree = tree;
        this.treeModel = treeModel;
        this.rootNode = rootNode;
    }

    public boolean isSuppressSelectEvent() {
        return this.suppressSelectEvent;
    }

    public void setSuppressSelectEvent(boolean suppress) {
        this.suppressSelectEvent = suppress;
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
                DefaultMutableTreeNode driveNode = new DefaultMutableTreeNode(
                        new FolderNodeUserObject(rootPath, "\ud83d\uddb4  " + displayName));
                driveNode.add(new DefaultMutableTreeNode("__dummy__"));
                this.rootNode.add(driveNode);
                this.pathToNodeMap.put(rootPath.toAbsolutePath().normalize(), driveNode);
            }
        }
        this.treeModel.reload();
    }

    public void expandNodeLazily(DefaultMutableTreeNode node) {
        if (node.getChildCount() != 1) {
            return;
        }
        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getFirstChild();
        if (!"__dummy__".equals(firstChild.getUserObject())) {
            return;
        }
        node.removeAllChildren();
        Object object = node.getUserObject();
        if (object instanceof FolderNodeUserObject uo) {
            Path dir = uo.getPath();
            ArrayList<Path> subdirs = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path child : stream) {
                    Path fn = child.getFileName();
                    if (fn == null || AppConstants.SYSTEM_IGNORED_DIRS.contains(fn.toString().toUpperCase())
                            || !Files.isDirectory(child)) {
                        continue;
                    }
                    subdirs.add(child);
                }
            } catch (IOException e) {
                System.err.println("Error reading directory " + dir + ": " + e.getMessage());
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

    public boolean hasSubdirectories(Path dir) {
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

    public Path getSelectedPath() {
        TreePath path = this.tree.getSelectionPath();
        if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof FolderNodeUserObject uo) {
            return uo.getPath();
        }
        return null;
    }

    public DefaultMutableTreeNode getNodeForPath(Path path) {
        if (path == null) return null;
        Path norm = path.toAbsolutePath().normalize();
        DefaultMutableTreeNode node = this.pathToNodeMap.get(norm);
        if (node != null) return node;
        for (Map.Entry<Path, DefaultMutableTreeNode> entry : this.pathToNodeMap.entrySet()) {
            if (entry.getKey().toString().equalsIgnoreCase(norm.toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void selectPath(Path targetPath, boolean expandTarget) {
        if (targetPath == null || !Files.exists(targetPath, new LinkOption[0])) return;

        Path normTarget = targetPath.toAbsolutePath().normalize();
        Path root = normTarget.getRoot();
        if (root == null) return;

        this.suppressSelectEvent = true;
        try {
            DefaultMutableTreeNode currentNode = this.getNodeForPath(root);
            if (currentNode == null) return;
            this.tree.expandPath(new TreePath(currentNode.getPath()));
            this.expandNodeLazily(currentNode);
            int nameCount = normTarget.getNameCount();
            Path currentPath = root;
            if (currentNode.getUserObject() instanceof FolderNodeUserObject uo) {
                currentPath = uo.getPath();
            }
            for (int i = 0; i < nameCount; ++i) {
                currentPath = currentPath.resolve(normTarget.getName(i));
                DefaultMutableTreeNode nextNode = this.getNodeForPath(currentPath);
                if (nextNode == null) {
                    this.expandNodeLazily(currentNode);
                    nextNode = this.getNodeForPath(currentPath);
                }
                if (nextNode == null) break;
                currentNode = nextNode;
                if (currentNode.getUserObject() instanceof FolderNodeUserObject uo) {
                    currentPath = uo.getPath();
                }
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
        } finally {
            this.suppressSelectEvent = false;
        }
    }

    public void onFolderRenamed(Path oldPath, Path newPath) {
        if (oldPath == null || newPath == null) return;
        Path oldNorm = oldPath.toAbsolutePath().normalize();
        Path newNorm = newPath.toAbsolutePath().normalize();

        DefaultMutableTreeNode node = this.pathToNodeMap.get(oldNorm);
        if (node == null) return;

        String displayName = "\ud83d\udcc1  " + (newNorm.getFileName() != null ? newNorm.getFileName().toString() : newNorm.toString());
        node.setUserObject(new FolderNodeUserObject(newNorm, displayName));

        this.pathToNodeMap.remove(oldNorm);
        this.pathToNodeMap.put(newNorm, node);

        this.updateDescendantsPath(node, oldNorm, newNorm);

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        if (parentNode != null) {
            String newName = newNorm.getFileName() != null ? newNorm.getFileName().toString() : "";
            int currentIndex = parentNode.getIndex(node);
            int targetIndex = 0;
            int childCount = parentNode.getChildCount();
            for (int i = 0; i < childCount; i++) {
                DefaultMutableTreeNode sibling = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                if (sibling == node) continue;
                Object obj = sibling.getUserObject();
                if (obj instanceof FolderNodeUserObject siblingUo) {
                    Path sPath = siblingUo.getPath();
                    String sName = sPath.getFileName() != null ? sPath.getFileName().toString() : "";
                    if (newName.compareToIgnoreCase(sName) > 0) {
                        targetIndex++;
                    }
                }
            }

            if (currentIndex == targetIndex) {
                this.treeModel.nodeChanged(node);
            } else {
                List<TreePath> expandedPaths = this.getExpandedTreePaths();
                parentNode.remove(node);
                parentNode.insert(node, targetIndex);
                this.treeModel.nodeStructureChanged(parentNode);
                this.restoreExpandedTreePaths(expandedPaths);
            }
        } else {
            this.treeModel.nodeChanged(node);
        }

        TreePath treePath = new TreePath(node.getPath());
        this.suppressSelectEvent = true;
        try {
            this.tree.setSelectionPath(treePath);
            this.tree.scrollPathToVisible(treePath);
        } finally {
            this.suppressSelectEvent = false;
        }
    }

    private void updateDescendantsPath(DefaultMutableTreeNode parentNode, Path oldPrefix, Path newPrefix) {
        int count = parentNode.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            Object obj = child.getUserObject();
            if (obj instanceof FolderNodeUserObject childUo) {
                Path childPath = childUo.getPath();
                Path childNorm = childPath.toAbsolutePath().normalize();
                if (childNorm.startsWith(oldPrefix)) {
                    Path rel = oldPrefix.relativize(childNorm);
                    Path newChildPath = newPrefix.resolve(rel);
                    String displayName = "\ud83d\udcc1  " + (newChildPath.getFileName() != null ?
                            newChildPath.getFileName().toString() : newChildPath.toString());
                    child.setUserObject(new FolderNodeUserObject(newChildPath, displayName));
                    this.pathToNodeMap.remove(childNorm);
                    this.pathToNodeMap.put(newChildPath.toAbsolutePath().normalize(), child);
                }
                this.updateDescendantsPath(child, oldPrefix, newPrefix);
            }
        }
    }

    public void onFolderAdded(Path parentFolder, Path newFolder) {
        if (parentFolder == null || newFolder == null) return;
        Path parentNorm = parentFolder.toAbsolutePath().normalize();
        Path newNorm = newFolder.toAbsolutePath().normalize();

        DefaultMutableTreeNode parentNode = this.pathToNodeMap.get(parentNorm);
        if (parentNode == null) return;

        if (parentNode.getChildCount() == 1) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) parentNode.getFirstChild();
            if ("__dummy__".equals(firstChild.getUserObject())) {
                return;
            }
        }

        String newName = newNorm.getFileName() != null ? newNorm.getFileName().toString() : newNorm.toString();
        int insertIndex = 0;
        int count = parentNode.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode sibling = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            Object obj = sibling.getUserObject();
            if (obj instanceof FolderNodeUserObject siblingUo) {
                Path sPath = siblingUo.getPath();
                String sName = sPath.getFileName() != null ? sPath.getFileName().toString() : "";
                if (newName.compareToIgnoreCase(sName) > 0) {
                    insertIndex++;
                }
            }
        }

        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FolderNodeUserObject(newNorm, "\ud83d\udcc1  " + newName));
        if (this.hasSubdirectories(newNorm)) {
            childNode.add(new DefaultMutableTreeNode("__dummy__"));
        }
        parentNode.insert(childNode, insertIndex);
        this.pathToNodeMap.put(newNorm, childNode);
        this.treeModel.nodesWereInserted(parentNode, new int[]{insertIndex});

        TreePath childTreePath = new TreePath(childNode.getPath());
        this.suppressSelectEvent = true;
        try {
            this.tree.setSelectionPath(childTreePath);
            this.tree.scrollPathToVisible(childTreePath);
        } finally {
            this.suppressSelectEvent = false;
        }
    }

    public void onFolderDeleted(Path folderPath) {
        if (folderPath == null) return;
        Path norm = folderPath.toAbsolutePath().normalize();
        DefaultMutableTreeNode node = this.pathToNodeMap.get(norm);
        if (node == null) return;

        this.removeNodeFromMap(node);

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent != null) {
            this.treeModel.removeNodeFromParent(node);
            TreePath parentPath = new TreePath(parent.getPath());
            this.suppressSelectEvent = true;
            try {
                this.tree.setSelectionPath(parentPath);
                this.tree.scrollPathToVisible(parentPath);
            } finally {
                this.suppressSelectEvent = false;
            }
        }
    }

    private void removeNodeFromMap(DefaultMutableTreeNode node) {
        Object obj = node.getUserObject();
        if (obj instanceof FolderNodeUserObject uo) {
            this.pathToNodeMap.remove(uo.getPath().toAbsolutePath().normalize());
        }
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            this.removeNodeFromMap((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }

    public List<TreePath> getExpandedTreePaths() {
        List<TreePath> list = new ArrayList<>();
        Enumeration<TreePath> en = this.tree.getExpandedDescendants(new TreePath(this.rootNode.getPath()));
        if (en != null) {
            while (en.hasMoreElements()) {
                list.add(en.nextElement());
            }
        }
        return list;
    }

    public void restoreExpandedTreePaths(List<TreePath> paths) {
        if (paths == null) return;
        for (TreePath tp : paths) {
            this.tree.expandPath(tp);
        }
    }

    public List<Path> getExpandedPaths() {
        List<Path> list = new ArrayList<>();
        Enumeration<TreePath> en = this.tree.getExpandedDescendants(new TreePath(this.rootNode.getPath()));
        if (en != null) {
            while (en.hasMoreElements()) {
                TreePath tp = en.nextElement();
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) tp.getLastPathComponent();
                if (n.getUserObject() instanceof FolderNodeUserObject uo) {
                    list.add(uo.getPath());
                }
            }
        }
        return list;
    }

    public void expandPath(Path targetPath) {
        if (targetPath == null || !Files.exists(targetPath, new LinkOption[0])) return;
        Path normTarget = targetPath.toAbsolutePath().normalize();
        Path root = normTarget.getRoot();
        if (root == null) return;
        DefaultMutableTreeNode currentNode = this.getNodeForPath(root);
        if (currentNode == null) return;
        this.tree.expandPath(new TreePath(currentNode.getPath()));
        this.expandNodeLazily(currentNode);
        int nameCount = normTarget.getNameCount();
        Path currentPath = root;
        if (currentNode.getUserObject() instanceof FolderNodeUserObject uo) {
            currentPath = uo.getPath();
        }
        for (int i = 0; i < nameCount; ++i) {
            currentPath = currentPath.resolve(normTarget.getName(i));
            DefaultMutableTreeNode nextNode = this.getNodeForPath(currentPath);
            if (nextNode == null) {
                this.expandNodeLazily(currentNode);
                nextNode = this.getNodeForPath(currentPath);
            }
            if (nextNode == null) break;
            currentNode = nextNode;
            if (currentNode.getUserObject() instanceof FolderNodeUserObject uo) {
                currentPath = uo.getPath();
            }
            this.tree.expandPath(new TreePath(currentNode.getPath()));
        }
        if (currentNode != null) {
            this.expandNodeLazily(currentNode);
            this.tree.expandPath(new TreePath(currentNode.getPath()));
        }
    }

    public void refresh() {
        List<Path> expanded = this.getExpandedPaths();
        Path selected = this.getSelectedPath();
        this.populateDrives();
        for (Path p : expanded) {
            if (Files.exists(p, new LinkOption[0])) {
                this.expandPath(p);
            }
        }
        if (selected != null && Files.exists(selected, new LinkOption[0])) {
            this.selectPath(selected, false);
        }
    }
}

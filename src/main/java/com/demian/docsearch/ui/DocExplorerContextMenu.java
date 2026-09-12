package com.demian.docsearch.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class DocExplorerContextMenu {
    private final JTree tree;
    private final DocExplorerTreeManager treeManager;
    private final Component parentComponent;
    private final Consumer<Path> onNewFolderCallback;
    private final Consumer<Path> onNewFileCallback;
    private final Consumer<Path> onRenameCallback;
    private final Consumer<Path> onDeleteCallback;
    private final Consumer<Path> onSelectCallback;
    private final Consumer<Path> onCopyCallback;
    private final Consumer<Path> onCutCallback;
    private final Consumer<Path> onPasteCallback;
    private final Supplier<Boolean> canPasteSupplier;

    public DocExplorerContextMenu(JTree tree,
                                  DocExplorerTreeManager treeManager,
                                  Component parentComponent,
                                  Consumer<Path> onNewFolderCallback,
                                  Consumer<Path> onNewFileCallback,
                                  Consumer<Path> onRenameCallback,
                                  Consumer<Path> onDeleteCallback,
                                  Consumer<Path> onSelectCallback,
                                  Consumer<Path> onCopyCallback,
                                  Consumer<Path> onCutCallback,
                                  Consumer<Path> onPasteCallback,
                                  Supplier<Boolean> canPasteSupplier) {
        this.tree = tree;
        this.treeManager = treeManager;
        this.parentComponent = parentComponent;
        this.onNewFolderCallback = onNewFolderCallback;
        this.onNewFileCallback = onNewFileCallback;
        this.onRenameCallback = onRenameCallback;
        this.onDeleteCallback = onDeleteCallback;
        this.onSelectCallback = onSelectCallback;
        this.onCopyCallback = onCopyCallback;
        this.onCutCallback = onCutCallback;
        this.onPasteCallback = onPasteCallback;
        this.canPasteSupplier = canPasteSupplier;
        this.init();
    }

    private void init() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem miNewFolder = new JMenuItem("\ud83d\udcc1 New Folder");
        miNewFolder.addActionListener(e -> this.triggerNewFolder());
        final JMenuItem miNewFile = new JMenuItem("\ud83d\udcc4 New File");
        miNewFile.addActionListener(e -> this.triggerNewFile());

        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        final JMenuItem miCopy = new JMenuItem("\ud83d\udcc4 Copy");
        miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey));
        miCopy.addActionListener(e -> this.triggerCopy());

        final JMenuItem miCut = new JMenuItem("\u2702\ufe0f Cut");
        miCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutKey));
        miCut.addActionListener(e -> this.triggerCut());

        final JMenuItem miPaste = new JMenuItem("\ud83d\udccb Paste");
        miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKey));
        miPaste.addActionListener(e -> this.triggerPaste());

        final JMenuItem miRename = new JMenuItem("\u270f\ufe0f Rename");
        miRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        miRename.addActionListener(e -> this.triggerRename());
        final JMenuItem miDelete = new JMenuItem("\ud83d\uddd1\ufe0f Delete");
        miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        miDelete.addActionListener(e -> this.triggerDelete());

        JMenuItem miCopyPath = new JMenuItem("\ud83d\udccb Copy Full Path");
        miCopyPath.addActionListener(e -> {
            Path p = this.treeManager.getSelectedPath();
            if (p != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(p.toAbsolutePath().toString()), null);
            }
        });

        JMenuItem miOpenExplorer = new JMenuItem("\ud83d\udcc1 Open in Explorer");
        miOpenExplorer.addActionListener(event -> {
            Path p = this.treeManager.getSelectedPath();
            if (p != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(p.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        menu.add(miNewFolder);
        menu.add(miNewFile);
        menu.addSeparator();
        menu.add(miCopy);
        menu.add(miCut);
        menu.add(miPaste);
        menu.addSeparator();
        menu.add(miRename);
        menu.add(miDelete);
        menu.addSeparator();
        menu.add(miCopyPath);
        menu.add(miOpenExplorer);

        this.tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = DocExplorerContextMenu.this.tree.getRowForLocation(e.getX(), e.getY());
                if (row < 0) return;

                DocExplorerContextMenu.this.tree.setSelectionRow(row);
                DocExplorerContextMenu.this.tree.requestFocusInWindow();
                Path p = DocExplorerContextMenu.this.treeManager.getSelectedPath();
                boolean isDriveRoot = p != null && p.getParent() == null;
                boolean hasFolder = p != null && Files.isDirectory(p);
                boolean canPaste = hasFolder && DocExplorerContextMenu.this.canPasteSupplier != null
                        && Boolean.TRUE.equals(DocExplorerContextMenu.this.canPasteSupplier.get());

                miCopy.setEnabled(!isDriveRoot && p != null);
                miCut.setEnabled(!isDriveRoot && p != null);
                miPaste.setEnabled(canPaste);
                miRename.setEnabled(!isDriveRoot);
                miDelete.setEnabled(!isDriveRoot && p != null);
                miNewFolder.setEnabled(hasFolder);
                miNewFile.setEnabled(hasFolder);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        this.tree.registerKeyboardAction(event -> this.triggerCopy(), KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey), 0);
        this.tree.registerKeyboardAction(event -> this.triggerCut(), KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutKey), 0);
        this.tree.registerKeyboardAction(event -> this.triggerPaste(), KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKey), 0);
        this.tree.registerKeyboardAction(event -> this.triggerRename(), KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), 0);
        this.tree.registerKeyboardAction(event -> this.triggerDelete(), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), 0);
    }

    public void triggerCopy() {
        Path p = this.treeManager.getSelectedPath();
        if (p == null || p.getParent() == null) return;
        if (this.onCopyCallback != null) {
            this.onCopyCallback.accept(p);
        }
    }

    public void triggerCut() {
        Path p = this.treeManager.getSelectedPath();
        if (p == null || p.getParent() == null) return;
        if (this.onCutCallback != null) {
            this.onCutCallback.accept(p);
        }
    }

    public void triggerPaste() {
        Path p = this.treeManager.getSelectedPath();
        if (p == null || !Files.isDirectory(p)) return;
        if (this.onPasteCallback != null) {
            this.onPasteCallback.accept(p);
        }
    }

    public void triggerDelete() {
        Path p = this.treeManager.getSelectedPath();
        if (p == null || p.getParent() == null) return;
        if (this.onDeleteCallback != null) {
            this.onDeleteCallback.accept(p);
        }
    }

    public void triggerNewFolder() {
        Path p = this.treeManager.getSelectedPath();
        if (p == null) return;
        if (this.onNewFolderCallback != null) {
            this.onNewFolderCallback.accept(p);
        } else {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this.parentComponent);
            new NewItemDialog(owner, p, true, newFolder -> this.treeManager.onFolderAdded(p, newFolder)).setVisible(true);
        }
    }

    public void triggerNewFile() {
        Path p = this.treeManager.getSelectedPath();
        if (p == null) return;
        if (this.onNewFileCallback != null) {
            this.onNewFileCallback.accept(p);
        } else {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this.parentComponent);
            new NewItemDialog(owner, p, false, newFile -> {
                if (this.onSelectCallback != null) {
                    this.onSelectCallback.accept(p);
                }
            }).setVisible(true);
        }
    }

    public void triggerRename() {
        Path p = this.treeManager.getSelectedPath();
        if (p != null && this.onRenameCallback != null) {
            if (p.getParent() == null) return;
            this.onRenameCallback.accept(p);
        }
    }
}

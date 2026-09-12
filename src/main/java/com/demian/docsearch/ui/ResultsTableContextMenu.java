package com.demian.docsearch.ui;

import com.demian.docsearch.model.FileItem;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;

public class ResultsTableContextMenu {
    private final ResultsTablePanel tablePanel;
    private final Runnable onOpen;
    private final Runnable onOpenInExplorer;
    private final Runnable onNewFolder;
    private final Runnable onNewFile;
    private final Runnable onCopy;
    private final Runnable onCut;
    private final Runnable onPaste;
    private final Runnable onRename;
    private final Runnable onDelete;
    private final Runnable onCopyPath;
    private final Supplier<Boolean> canPasteSupplier;

    public ResultsTableContextMenu(ResultsTablePanel tablePanel,
                                   Runnable onOpen,
                                   Runnable onOpenInExplorer,
                                   Runnable onNewFolder,
                                   Runnable onNewFile,
                                   Runnable onCopy,
                                   Runnable onCut,
                                   Runnable onPaste,
                                   Runnable onRename,
                                   Runnable onDelete,
                                   Runnable onCopyPath,
                                   Supplier<Boolean> canPasteSupplier) {
        this.tablePanel = tablePanel;
        this.onOpen = onOpen;
        this.onOpenInExplorer = onOpenInExplorer;
        this.onNewFolder = onNewFolder;
        this.onNewFile = onNewFile;
        this.onCopy = onCopy;
        this.onCut = onCut;
        this.onPaste = onPaste;
        this.onRename = onRename;
        this.onDelete = onDelete;
        this.onCopyPath = onCopyPath;
        this.canPasteSupplier = canPasteSupplier;
        this.init();
    }

    private void init() {
        JTable table = this.tablePanel.getTable();
        final JPopupMenu menu = new JPopupMenu();

        final JMenuItem miOpen = new JMenuItem("\u25b6 Open");
        miOpen.addActionListener(e -> {
            if (this.onOpen != null) this.onOpen.run();
        });

        final JMenuItem miOpenExplorer = new JMenuItem("\ud83d\udcc1 Open in Explorer");
        miOpenExplorer.addActionListener(e -> {
            if (this.onOpenInExplorer != null) this.onOpenInExplorer.run();
        });

        final JMenuItem miNewFolder = new JMenuItem("\ud83d\udcc1 New Folder");
        miNewFolder.addActionListener(e -> {
            if (this.onNewFolder != null) this.onNewFolder.run();
        });

        final JMenuItem miNewFile = new JMenuItem("\ud83d\udcc4 New File");
        miNewFile.addActionListener(e -> {
            if (this.onNewFile != null) this.onNewFile.run();
        });

        final JPopupMenu.Separator sepNew = new JPopupMenu.Separator();

        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        final JMenuItem miCopy = new JMenuItem("\ud83d\udcc4 Copy");
        miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey));
        miCopy.addActionListener(e -> {
            if (this.onCopy != null) this.onCopy.run();
        });

        final JMenuItem miCut = new JMenuItem("\u2702\ufe0f Cut");
        miCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutKey));
        miCut.addActionListener(e -> {
            if (this.onCut != null) this.onCut.run();
        });

        final JMenuItem miPaste = new JMenuItem("\ud83d\udccb Paste");
        miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKey));
        miPaste.addActionListener(e -> {
            if (this.onPaste != null) this.onPaste.run();
        });

        final JMenuItem miRename = new JMenuItem("\u270f\ufe0f Rename");
        miRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        miRename.addActionListener(e -> {
            if (this.onRename != null) this.onRename.run();
        });

        final JMenuItem miDelete = new JMenuItem("\ud83d\uddd1\ufe0f Delete");
        miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        miDelete.addActionListener(e -> {
            if (this.onDelete != null) this.onDelete.run();
        });

        final JMenuItem miCopyPath = new JMenuItem("\ud83d\udccb Copy Full Path");
        miCopyPath.addActionListener(e -> {
            if (this.onCopyPath != null) this.onCopyPath.run();
        });

        final JMenuItem miSelectAll = new JMenuItem("Select All");
        miSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutKey));
        miSelectAll.addActionListener(e -> this.tablePanel.selectAll());

        menu.add(miOpen);
        menu.add(miOpenExplorer);
        menu.add(sepNew);
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
        menu.add(miSelectAll);

        table.addMouseListener(new MouseAdapter() {
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
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) {
                    table.setRowSelectionInterval(row, row);
                }
                int count = table.getSelectedRowCount();
                boolean canPaste = ResultsTableContextMenu.this.canPasteSupplier != null
                        && Boolean.TRUE.equals(ResultsTableContextMenu.this.canPasteSupplier.get());

                if (count <= 0) {
                    if (!canPaste) return;
                    miOpen.setVisible(false);
                    miOpenExplorer.setVisible(false);
                    sepNew.setVisible(false);
                    miNewFolder.setVisible(false);
                    miNewFile.setVisible(false);
                    miCopy.setEnabled(false);
                    miCut.setEnabled(false);
                    miPaste.setEnabled(true);
                    miRename.setEnabled(false);
                    miDelete.setEnabled(false);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                    return;
                }

                miOpen.setVisible(true);
                miOpenExplorer.setVisible(true);
                miCopy.setEnabled(true);
                miCut.setEnabled(true);
                miPaste.setEnabled(canPaste);

                if (count == 1) {
                    FileItem item = ResultsTableContextMenu.this.tablePanel.getSelectedItem();
                    boolean isFolder = item != null && item.isDirectory();
                    miOpen.setText(isFolder ? "\ud83d\udcc2 Open Folder" : "\u25b6 Open File");
                    miRename.setEnabled(true);
                    miDelete.setEnabled(true);
                    miNewFolder.setVisible(isFolder);
                    miNewFolder.setEnabled(isFolder);
                    miNewFile.setVisible(isFolder);
                    miNewFile.setEnabled(isFolder);
                    sepNew.setVisible(isFolder);
                } else {
                    miOpen.setText("\u25b6 Open " + count + " Files");
                    miRename.setEnabled(false);
                    miDelete.setEnabled(true);
                    miNewFolder.setVisible(false);
                    miNewFolder.setEnabled(false);
                    miNewFile.setVisible(false);
                    miNewFile.setEnabled(false);
                    sepNew.setVisible(false);
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        table.registerKeyboardAction(e -> {
            if (this.onCopy != null) this.onCopy.run();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey), 0);

        table.registerKeyboardAction(e -> {
            if (this.onCut != null) this.onCut.run();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutKey), 0);

        table.registerKeyboardAction(e -> {
            if (this.onPaste != null) this.onPaste.run();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKey), 0);

        table.registerKeyboardAction(e -> this.tablePanel.selectAll(),
                KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutKey), 0);

        table.registerKeyboardAction(e -> {
            if (this.onDelete != null) this.onDelete.run();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_FOCUSED);
    }
}

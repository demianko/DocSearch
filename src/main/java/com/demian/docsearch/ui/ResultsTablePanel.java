package com.demian.docsearch.ui;

import com.demian.docsearch.constant.ResultColumn;
import com.demian.docsearch.model.FileItem;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

public class ResultsTablePanel extends JPanel {
    private final ResultsTableModel tableModel;
    private final JTable resultsTable;
    private Consumer<FileItem> onOpenRequested;
    private Consumer<Integer> onSortRequested;
    private BiConsumer<List<File>, Path> onMoveDroppedFiles;

    public ResultsTablePanel() {
        super(new BorderLayout());
        this.tableModel = new ResultsTableModel();
        this.resultsTable = new JTable(this.tableModel);
        this.resultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.resultsTable.setRowHeight(24);
        this.resultsTable.setShowGrid(false);
        this.resultsTable.setIntercellSpacing(new Dimension(0, 0));
        this.resultsTable.setFillsViewportHeight(true);

        for (ResultColumn col : ResultColumn.values()) {
            this.resultsTable.getColumnModel().getColumn(col.modelIndex()).setPreferredWidth(col.defaultWidth());
        }

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.resultsTable.getColumnModel().getColumn(ResultColumn.INDEX.modelIndex()).setCellRenderer(centerRenderer);
        this.resultsTable.getColumnModel().getColumn(ResultColumn.INDEX.modelIndex()).setMaxWidth(75);

        this.resultsTable.getTableHeader().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.resultsTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int viewCol = ResultsTablePanel.this.resultsTable.getColumnModel().getColumnIndexAtX(e.getX());
                if (viewCol < 0) return;
                int modelCol = ResultsTablePanel.this.resultsTable.convertColumnIndexToModel(viewCol);
                if (ResultsTablePanel.this.onSortRequested != null) {
                    ResultsTablePanel.this.onSortRequested.accept(modelCol);
                }
            }
        });

        this.resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    ResultsTablePanel.this.triggerOpen();
                }
            }
        });

        this.resultsTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ResultsTablePanel.this.triggerOpen();
                    e.consume();
                }
            }
        });

        this.resultsTable.setDragEnabled(true);
        this.resultsTable.setDropMode(DropMode.ON);
        this.resultsTable.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || !support.isDrop()) {
                    return false;
                }
                JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                int row = dl.getRow();
                if (row >= 0 && row < ResultsTablePanel.this.tableModel.getRowCount()) {
                    FileItem item = ResultsTablePanel.this.tableModel.getItem(row);
                    if (item != null && item.isDirectory()) {
                        support.setShowDropLocation(true);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                    int row = dl.getRow();
                    FileItem item = ResultsTablePanel.this.tableModel.getItem(row);
                    if (item == null || !item.isDirectory()) return false;
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (ResultsTablePanel.this.onMoveDroppedFiles != null && files != null && !files.isEmpty()) {
                        ResultsTablePanel.this.onMoveDroppedFiles.accept(files, item.path());
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                List<Path> selectedPaths = ResultsTablePanel.this.getSelectedPaths();
                if (selectedPaths.isEmpty()) return null;
                final List<File> files = selectedPaths.stream().map(Path::toFile).toList();
                final String text = String.join(System.lineSeparator(), selectedPaths.stream().map(Path::toString).toList());
                return new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.javaFileListFlavor.equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) {
                        if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                            return files;
                        }
                        return text;
                    }
                };
            }
        });

        JScrollPane scrollPane = new JScrollPane(this.resultsTable);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private void triggerOpen() {
        int row = this.resultsTable.getSelectedRow();
        if (row < 0) return;
        FileItem item = this.tableModel.getItem(row);
        if (item != null && this.onOpenRequested != null) {
            this.onOpenRequested.accept(item);
        }
    }

    public void setOnOpenRequested(Consumer<FileItem> onOpenRequested) {
        this.onOpenRequested = onOpenRequested;
    }

    public void setOnSortRequested(Consumer<Integer> onSortRequested) {
        this.onSortRequested = onSortRequested;
    }

    public void setOnMoveDroppedFiles(BiConsumer<List<File>, Path> onMoveDroppedFiles) {
        this.onMoveDroppedFiles = onMoveDroppedFiles;
    }

    public JTable getTable() {
        return this.resultsTable;
    }

    public ResultsTableModel getModel() {
        return this.tableModel;
    }

    public List<Path> getSelectedPaths() {
        int[] rows = this.resultsTable.getSelectedRows();
        List<Path> paths = new ArrayList<>();
        for (int r : rows) {
            FileItem item = this.tableModel.getItem(r);
            if (item == null) continue;
            paths.add(item.path());
        }
        return paths;
    }

    public int getSelectedRow() {
        return this.resultsTable.getSelectedRow();
    }

    public int getSelectedRowCount() {
        return this.resultsTable.getSelectedRowCount();
    }

    public FileItem getSelectedItem() {
        int row = this.getSelectedRow();
        return row >= 0 ? this.tableModel.getItem(row) : null;
    }

    public void setItems(List<FileItem> items) {
        this.tableModel.setItems(items);
    }

    public void addItem(FileItem item) {
        this.tableModel.addItem(item);
    }

    public void updateItem(int row, FileItem item) {
        this.tableModel.updateItem(row, item);
    }

    public void selectAll() {
        this.resultsTable.selectAll();
    }

    public void setRowSelectionInterval(int index0, int index1) {
        this.resultsTable.setRowSelectionInterval(index0, index1);
    }

    public boolean isRowSelected(int row) {
        return this.resultsTable.isRowSelected(row);
    }

    public int rowAtPoint(Point point) {
        return this.resultsTable.rowAtPoint(point);
    }

    public void selectItemByPath(Path path) {
        if (path == null) return;
        for (int r = 0; r < this.tableModel.getRowCount(); ++r) {
            FileItem it = this.tableModel.getItem(r);
            if (it == null || !it.path().equals(path)) continue;
            this.resultsTable.setRowSelectionInterval(r, r);
            this.resultsTable.scrollRectToVisible(this.resultsTable.getCellRect(r, 0, true));
            break;
        }
    }

    public void updateHeaderSortIndicators(int activeCol, boolean ascending) {
        for (int i = 0; i < this.resultsTable.getColumnCount(); ++i) {
            TableColumn col = this.resultsTable.getColumnModel().getColumn(i);
            int modelIdx = col.getModelIndex();
            String base = ResultColumn.fromIndex(modelIdx).header();
            if (modelIdx == activeCol && modelIdx != ResultColumn.INDEX.modelIndex()) {
                col.setHeaderValue(base + (ascending ? "  \u25b2" : "  \u25bc"));
            } else {
                col.setHeaderValue(base);
            }
        }
        this.resultsTable.getTableHeader().repaint();
    }
}

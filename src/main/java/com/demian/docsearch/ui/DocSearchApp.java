package com.demian.docsearch.ui;

import com.demian.docsearch.config.ConfigManager;
import com.demian.docsearch.config.WindowsClipboardHelper;
import com.demian.docsearch.constant.ResultColumn;
import com.demian.docsearch.engine.FileSearchEngine;
import com.demian.docsearch.engine.QueryMatcher;
import com.demian.docsearch.engine.QueryParser;
import com.demian.docsearch.model.AppConfig;
import com.demian.docsearch.model.FileItem;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DocSearchApp
extends JFrame {
    private final ConfigManager configManager;
    private final AppConfig config;
    private JTextField txtFolder;
    private JTextField txtPatterns;
    private JTextField txtExtensions;
    private JTextField txtLimit;
    private JButton btnSearch;
    private JButton btnStop;
    private int currentSortColumn = 2;
    private boolean sortAscending = false;
    private JTextField txtFilter;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblCount;
    private DocExplorerNav explorerNav;
    private JTable resultsTable;
    private ResultsTableModel tableModel;
    private SwingWorker<List<FileItem>, ProgressChunk> activeSearchWorker;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final List<FileItem> allResults = new ArrayList<FileItem>();

    public DocSearchApp() {
        this.configManager = new ConfigManager();
        this.config = this.configManager.load();
        this.setTitle("DocSearch Pro \u2014 Fast Search & Sort");
        this.setSize(1350, 850);
        this.setMinimumSize(new Dimension(1050, 650));
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(0);
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                DocSearchApp.this.saveCurrentConfig();
                DocSearchApp.this.dispose();
                System.exit(0);
            }
        });
        this.initUI();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        this.explorerNav = new DocExplorerNav(this::onNavFolderSelected, this::onNavFolderRename);
        JPanel rightWorkspace = new JPanel(new BorderLayout(0, 10));
        rightWorkspace.add((Component)this.createControlsPanel(), "North");
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add((Component)this.createProgressAndFilterPanel(), "North");
        centerPanel.add((Component)this.createResultsTablePanel(), "Center");
        centerPanel.add((Component)this.createFooterStatusPanel(), "South");
        rightWorkspace.add((Component)centerPanel, "Center");
        JSplitPane splitPane = new JSplitPane(1, this.explorerNav, rightWorkspace);
        splitPane.setDividerLocation(280);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        rootPanel.add((Component)splitPane, "Center");
        this.setContentPane(rootPanel);
        this.getRootPane().registerKeyboardAction(e -> this.renameSelectedItem(), KeyStroke.getKeyStroke(113, 0), 2);
        this.getRootPane().registerKeyboardAction(e -> this.stopSearch(), KeyStroke.getKeyStroke(27, 0), 2);
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panel.getBackground().darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblFolder = new JLabel("\ud83d\udcc1 Directory:");
        lblFolder.setFont(lblFolder.getFont().deriveFont(1, 13.0f));
        panel.add((Component)lblFolder, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtFolder = new JTextField(this.config.getDirectory());
        this.txtFolder.setPreferredSize(new Dimension(500, 32));
        this.txtFolder.addActionListener(e -> this.searchFiles());
        panel.add((Component)this.txtFolder, gbc);
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        JButton btnBrowse = new JButton("Browse...");
        btnBrowse.setPreferredSize(new Dimension(95, 32));
        btnBrowse.addActionListener(e -> this.browseDirectory());
        panel.add((Component)btnBrowse, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel lblPattern = new JLabel("\ud83d\udd0d Patterns:");
        lblPattern.setFont(lblPattern.getFont().deriveFont(1, 13.0f));
        panel.add((Component)lblPattern, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtPatterns = new JTextField(this.config.getPattern());
        this.txtPatterns.setPreferredSize(new Dimension(500, 32));
        this.txtPatterns.addActionListener(e -> this.searchFiles());
        panel.add((Component)this.txtPatterns, gbc);
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnSearch = new JButton("\u26a1 Search (Enter)");
        this.btnSearch.setPreferredSize(new Dimension(140, 32));
        this.btnSearch.setFont(this.btnSearch.getFont().deriveFont(1));
        this.btnSearch.addActionListener(e -> this.searchFiles());
        panel.add((Component)this.btnSearch, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel lblExt = new JLabel("\ud83d\udcc4 Extensions:");
        lblExt.setFont(lblExt.getFont().deriveFont(1, 13.0f));
        panel.add((Component)lblExt, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtExtensions = new JTextField(this.config.getExtension());
        this.txtExtensions.setPreferredSize(new Dimension(500, 32));
        this.txtExtensions.addActionListener(e -> this.searchFiles());
        panel.add((Component)this.txtExtensions, gbc);
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnStop = new JButton("\ud83d\uded1 Stop (Esc)");
        this.btnStop.setPreferredSize(new Dimension(140, 32));
        this.btnStop.setEnabled(false);
        this.btnStop.addActionListener(e -> this.stopSearch());
        panel.add((Component)this.btnStop, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        JPanel optionsPanel = new JPanel(new FlowLayout(0, 12, 4));
        optionsPanel.add(new JLabel("Limit:"));
        this.txtLimit = new JTextField(this.config.getLimit() > 0 ? String.valueOf(this.config.getLimit()) : "", 5);
        optionsPanel.add(this.txtLimit);
        panel.add((Component)optionsPanel, gbc);
        return panel;
    }

    private JPanel createProgressAndFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 8, 2);
        gbc.fill = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        this.progressBar = new JProgressBar(0, 100);
        this.progressBar.setValue(0);
        this.progressBar.setStringPainted(false);
        this.progressBar.setPreferredSize(new Dimension(200, 10));
        this.progressBar.setForeground(new Color(31, 106, 165));
        panel.add((Component)this.progressBar, gbc);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 2, 0, 8);
        JLabel lblFilter = new JLabel("\ud83c\udfaf Filter Results:");
        lblFilter.setFont(lblFilter.getFont().deriveFont(1, 12.0f));
        panel.add((Component)lblFilter, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 2);
        this.txtFilter = new JTextField(this.config.getFilterResult());
        this.txtFilter.setPreferredSize(new Dimension(200, 30));
        this.txtFilter.putClientProperty("JTextField.placeholderText", "Type to filter displayed items instantly (supports '|', 'NOT', '*', '?')...");
        this.txtFilter.putClientProperty("JTextField.showClearButton", true);
        this.txtFilter.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                DocSearchApp.this.applyLiveFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                DocSearchApp.this.applyLiveFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                DocSearchApp.this.applyLiveFilter();
            }
        });
        panel.add((Component)this.txtFilter, gbc);
        return panel;
    }

    private JPanel createResultsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        this.tableModel = new ResultsTableModel();
        this.resultsTable = new JTable(this.tableModel);
        this.resultsTable.setSelectionMode(2);
        this.resultsTable.setRowHeight(24);
        this.resultsTable.setShowGrid(false);
        this.resultsTable.setIntercellSpacing(new Dimension(0, 0));
        this.resultsTable.setFillsViewportHeight(true);
        this.resultsTable.setDragEnabled(true);
        for (ResultColumn col : ResultColumn.values()) {
            this.resultsTable.getColumnModel().getColumn(col.modelIndex()).setPreferredWidth(col.defaultWidth());
        }
        this.resultsTable.getTableHeader().setCursor(Cursor.getPredefinedCursor(12));
        this.resultsTable.getTableHeader().addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int viewCol = DocSearchApp.this.resultsTable.getColumnModel().getColumnIndexAtX(e.getX());
                if (viewCol < 0) {
                    return;
                }
                int modelCol = DocSearchApp.this.resultsTable.convertColumnIndexToModel(viewCol);
                DocSearchApp.this.sortByColumn(modelCol);
            }
        });
        this.updateHeaderSortIndicators(this.currentSortColumn, this.sortAscending);
        this.resultsTable.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    DocSearchApp.this.openSelectedItem();
                }
            }
        });
        this.resultsTable.addKeyListener(new KeyAdapter(){

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    DocSearchApp.this.openSelectedItem();
                    e.consume();
                }
            }
        });
        this.resultsTable.setTransferHandler(new TransferHandler(){

            @Override
            public int getSourceActions(JComponent c) {
                return 1;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                List<Path> selectedPaths = DocSearchApp.this.getSelectedPaths();
                if (selectedPaths.isEmpty()) {
                    return null;
                }
                final List<File> files = selectedPaths.stream().map(Path::toFile).toList();
                final String text = String.join((CharSequence)System.lineSeparator(), selectedPaths.stream().map(Path::toString).toList());
                return new Transferable(){

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
        this.setupTableContextMenu();
        JScrollPane scrollPane = new JScrollPane(this.resultsTable);
        panel.add((Component)scrollPane, "Center");
        return panel;
    }

    private void setupTableContextMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem miOpen = new JMenuItem("\u25b6 Open");
        miOpen.addActionListener(e -> this.openSelectedItem());
        JMenuItem miOpenExplorer = new JMenuItem("\ud83d\udcc1 Open in Explorer");
        miOpenExplorer.addActionListener(e -> this.openInExplorer());
        JMenuItem miCopy = new JMenuItem("\ud83d\udcc4 Copy");
        miCopy.setAccelerator(KeyStroke.getKeyStroke(67, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miCopy.addActionListener(e -> this.copySelectedItems());
        final JMenuItem miRename = new JMenuItem("\u270f\ufe0f Rename");
        miRename.setAccelerator(KeyStroke.getKeyStroke(113, 0));
        miRename.addActionListener(e -> this.renameSelectedItem());
        JMenuItem miCopyPath = new JMenuItem("\ud83d\udccb Copy Full Path");
        miCopyPath.addActionListener(e -> this.copySelectedPathsText());
        JMenuItem miSelectAll = new JMenuItem("Select All");
        miSelectAll.setAccelerator(KeyStroke.getKeyStroke(65, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miSelectAll.addActionListener(e -> this.resultsTable.selectAll());
        menu.add(miOpen);
        menu.add(miOpenExplorer);
        menu.addSeparator();
        menu.add(miCopy);
        menu.add(miRename);
        menu.addSeparator();
        menu.add(miCopyPath);
        menu.add(miSelectAll);
        this.resultsTable.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = DocSearchApp.this.resultsTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && !DocSearchApp.this.resultsTable.isRowSelected(row)) {
                        DocSearchApp.this.resultsTable.setRowSelectionInterval(row, row);
                    }
                    if (DocSearchApp.this.resultsTable.getSelectedRowCount() > 0) {
                        int count = DocSearchApp.this.resultsTable.getSelectedRowCount();
                        if (count == 1) {
                            FileItem item = DocSearchApp.this.tableModel.getItem(DocSearchApp.this.resultsTable.getSelectedRow());
                            miOpen.setText(item != null && item.isDirectory() ? "\ud83d\udcc2 Open Folder" : "\u25b6 Open File");
                            miRename.setEnabled(true);
                        } else {
                            miOpen.setText("\u25b6 Open " + count + " Files");
                            miRename.setEnabled(false);
                        }
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        this.resultsTable.registerKeyboardAction(
                e -> this.copySelectedItems(), KeyStroke.getKeyStroke(67, shortcutKey), 0);
        this.resultsTable.registerKeyboardAction(
                e -> this.resultsTable.selectAll(), KeyStroke.getKeyStroke(65, shortcutKey), 0);
    }

    private JPanel createFooterStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        this.lblStatus = new JLabel("Ready");
        this.lblCount = new JLabel("0 files found");
        this.lblCount.setFont(this.lblCount.getFont().deriveFont(1));
        panel.add((Component)this.lblStatus, "West");
        panel.add((Component)this.lblCount, "East");
        return panel;
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(1);
        String current = this.txtFolder.getText().trim();
        if (!current.isEmpty() && Files.exists(Paths.get(current, new String[0]), new LinkOption[0])) {
            chooser.setCurrentDirectory(new File(current));
        }
        if (chooser.showOpenDialog(this) == 0) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            this.txtFolder.setText(path);
            this.onNavFolderSelected(Paths.get(path, new String[0]));
        }
    }

    public void onNavFolderSelected(Path folder) {
        if (folder == null || !Files.exists(folder, new LinkOption[0])) {
            return;
        }
        this.txtFolder.setText(folder.toAbsolutePath().toString());
        this.saveCurrentConfig();
        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(0);
        List<FileItem> items = FileSearchEngine.listDirectSubFolderFiles(folder);
        String criteria = this.getSortCriteria(this.currentSortColumn);
        List<FileItem> sorted = FileSearchEngine.sortResults(items, criteria, !this.sortAscending);
        this.allResults.clear();
        this.allResults.addAll(sorted);
        this.tableModel.setItems(sorted);
        this.lblCount.setText(sorted.size() + " items");
        this.lblStatus.setText("Viewing folder: " + String.valueOf(folder.getFileName()));
    }

    private void onNavFolderRename(Path folderPath) {
        new RenameDialog((Frame)this, folderPath, true, newPath -> {
            if (this.txtFolder.getText().trim().equalsIgnoreCase(folderPath.toString())) {
                this.txtFolder.setText(newPath.toAbsolutePath().toString());
                this.onNavFolderSelected((Path)newPath);
            }
            this.explorerNav.refresh();
            this.lblStatus.setText("Renamed folder to '" + String.valueOf(newPath.getFileName()) + "'");
        }).setVisible(true);
    }

    private void searchFiles() {
        String dirStr = StringUtils.trimToEmpty(this.txtFolder.getText());
        if (StringUtils.isEmpty(dirStr) || !Files.exists(Paths.get(dirStr, new String[0]), new LinkOption[0])) {
            JOptionPane.showMessageDialog(this, "Please enter a valid target directory.", "Directory Not Found", 2);
            return;
        }
        this.saveCurrentConfig();
        this.stopSearch();
        final Path rootFolder = Paths.get(dirStr, new String[0]);
        final String patternQuery = StringUtils.trimToEmpty(this.txtPatterns.getText());
        final String publisherQuery = "";
        final String extensionsQuery = StringUtils.trimToEmpty(this.txtExtensions.getText());
        int limit = 0;
        try {
            String limStr = StringUtils.trimToEmpty(this.txtLimit.getText());
            if (StringUtils.isNotEmpty(limStr)) {
                limit = Integer.parseInt(limStr);
            }
        }
        catch (NumberFormatException e) {
            System.err.println("Invalid limit format: " + e.getMessage());
        }
        final int searchLimit = limit;
        this.btnSearch.setEnabled(false);
        this.btnStop.setEnabled(true);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setValue(0);
        this.lblStatus.setText("Scanning directory: " + String.valueOf(rootFolder.getFileName()) + "...");
        this.lblCount.setText("0 files found");
        this.allResults.clear();
        this.tableModel.setItems(List.of());
        this.cancelRequested.set(false);
        final long startTime = System.currentTimeMillis();
        this.activeSearchWorker = new SwingWorker<List<FileItem>, ProgressChunk>(){

            @Override
            protected List<FileItem> doInBackground() {
                return FileSearchEngine.search(
                        rootFolder, patternQuery, publisherQuery, extensionsQuery, searchLimit,
                        DocSearchApp.this.cancelRequested::get,
                        (scanned, total, item) -> this.publish(new ProgressChunk(scanned, total, item)));
            }

            @Override
            protected void process(List<ProgressChunk> chunks) {
                ProgressChunk lastChunk = null;
                for (ProgressChunk chunk : chunks) {
                    if (chunk.item() != null) {
                        DocSearchApp.this.allResults.add(chunk.item());
                        DocSearchApp.this.tableModel.addItem(chunk.item());
                    }
                    lastChunk = chunk;
                }
                if (lastChunk != null) {
                    if (lastChunk.total() > 0) {
                        DocSearchApp.this.progressBar.setIndeterminate(false);
                        DocSearchApp.this.progressBar.setMaximum(lastChunk.total());
                        DocSearchApp.this.progressBar.setValue(lastChunk.scanned());
                        DocSearchApp.this.lblStatus.setText(String.format(
                                "Scanned %,d of %,d files... (Esc to stop)", lastChunk.scanned(), lastChunk.total()));
                    } else if (lastChunk.total() == -1) {
                        DocSearchApp.this.progressBar.setIndeterminate(true);
                        DocSearchApp.this.lblStatus.setText(String.format(
                                "Scanning directory: %s (%,d files found)...", rootFolder.getFileName(), lastChunk.scanned()));
                    }
                }
                DocSearchApp.this.lblCount.setText(DocSearchApp.this.allResults.size() + " files found");
            }

            @Override
            protected void done() {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                double elapsedSec = (double)elapsedMillis / 1000.0;
                DocSearchApp.this.btnSearch.setEnabled(true);
                DocSearchApp.this.btnStop.setEnabled(false);
                DocSearchApp.this.progressBar.setIndeterminate(false);
                if (DocSearchApp.this.cancelRequested.get()) {
                    DocSearchApp.this.lblStatus.setText(String.format(
                            "Search stopped (Esc) in %.2fs. Found %,d matching files.",
                            elapsedSec, DocSearchApp.this.allResults.size()));
                } else {
                    DocSearchApp.this.progressBar.setValue(DocSearchApp.this.progressBar.getMaximum());
                    DocSearchApp.this.lblStatus.setText(String.format(
                            "Completed in %.2fs (Found %,d files in '%s')",
                            elapsedSec, DocSearchApp.this.allResults.size(), rootFolder.getFileName()));
                }
                if (!DocSearchApp.this.allResults.isEmpty() && (DocSearchApp.this.currentSortColumn != 2 || DocSearchApp.this.sortAscending)) {
                    String criteria = DocSearchApp.this.getSortCriteria(DocSearchApp.this.currentSortColumn);
                    List<FileItem> sorted = FileSearchEngine.sortResults(DocSearchApp.this.allResults, criteria, !DocSearchApp.this.sortAscending);
                    DocSearchApp.this.allResults.clear();
                    DocSearchApp.this.allResults.addAll(sorted);
                    DocSearchApp.this.applyLiveFilter();
                }
                DocSearchApp.this.lblCount.setText(DocSearchApp.this.allResults.size() + " files found");
            }
        };
        this.activeSearchWorker.execute();
    }

    private void stopSearch() {
        this.cancelRequested.set(true);
        if (this.activeSearchWorker != null && !this.activeSearchWorker.isDone()) {
            this.activeSearchWorker.cancel(true);
        }
        this.btnSearch.setEnabled(true);
        this.btnStop.setEnabled(false);
        this.progressBar.setIndeterminate(false);
    }

    private void applyLiveFilter() {
        String filterText = StringUtils.trimToEmpty(this.txtFilter.getText());
        if (StringUtils.isEmpty(filterText)) {
            this.tableModel.setItems(this.allResults);
            this.lblCount.setText(this.allResults.size() + " items");
            return;
        }
        QueryParser.ParsedQuery parsed = QueryParser.parse(filterText);
        List<FileItem> filtered = this.allResults.stream()
                .filter(item -> QueryMatcher.matches(item.name(), parsed.rules(), parsed.globalExcludes()))
                .toList();
        this.tableModel.setItems(filtered);
        this.lblCount.setText(filtered.size() + " / " + this.allResults.size() + " items");
    }

    private void openSelectedItem() {
        int row = this.resultsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        FileItem item = this.tableModel.getItem(row);
        if (item == null) {
            return;
        }
        if (item.isDirectory()) {
            this.onNavFolderSelected(item.path());
            this.explorerNav.selectPath(item.path(), true);
        } else if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(item.path().toFile());
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Could not open file:\n" + e.getMessage(), "Open Error", 0);
            }
        }
    }

    private void openInExplorer() {
        List<Path> paths = this.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths) || !Desktop.isDesktopSupported()) {
            return;
        }
        Path target = paths.getFirst();
        try {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", target.toAbsolutePath().toString()});
        }
        catch (IOException e) {
            try {
                Desktop.getDesktop().open(target.getParent().toFile());
            }
            catch (IOException ee) {
                System.err.println("Failed to open folder in explorer: " + ee.getMessage());
            }
        }
    }

    private void copySelectedItems() {
        List<Path> paths = this.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        boolean success = WindowsClipboardHelper.copyFilesToClipboard(paths);
        if (success) {
            this.lblStatus.setText("Copied " + paths.size() + " item(s) to clipboard (Ctrl+V to paste in Explorer)");
        }
    }

    private void copySelectedPathsText() {
        List<Path> paths = this.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        String text = StringUtils.join(paths, System.lineSeparator());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        this.lblStatus.setText("Copied " + paths.size() + " path(s) to clipboard");
    }

    private void renameSelectedItem() {
        FileItem item;
        int row = this.resultsTable.getSelectedRow();
        if (row >= 0 && (item = this.tableModel.getItem(row)) != null) {
            new RenameDialog((Frame)this, item.path(), item.isDirectory(), newPath -> {
                FileItem updated = item.withPath((Path)newPath);
                this.tableModel.updateItem(row, updated);
                if (item.isDirectory()) {
                    this.explorerNav.refresh();
                }
                this.lblStatus.setText("Renamed to '" + String.valueOf(newPath.getFileName()) + "'");
            }).setVisible(true);
            return;
        }
        Path navPath = this.explorerNav.getSelectedPath();
        if (navPath != null && navPath.getParent() != null) {
            this.onNavFolderRename(navPath);
        }
    }

    private List<Path> getSelectedPaths() {
        int[] rows = this.resultsTable.getSelectedRows();
        ArrayList<Path> paths = new ArrayList<Path>();
        for (int r : rows) {
            FileItem item = this.tableModel.getItem(r);
            if (item == null) continue;
            paths.add(item.path());
        }
        return paths;
    }

    private String getSortCriteria(int col) {
        return ResultColumn.fromIndex(col).sortCriteria();
    }

    private void sortByColumn(int col) {
        if (this.allResults.isEmpty()) {
            return;
        }
        int selectedRow = this.resultsTable.getSelectedRow();
        FileItem selectedItem = selectedRow >= 0 ? this.tableModel.getItem(selectedRow) : null;
        ResultColumn resultCol = ResultColumn.fromIndex(col);
        if (this.currentSortColumn == col) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.currentSortColumn = col;
            this.sortAscending = resultCol.defaultAscending();
        }
        String criteria = resultCol.sortCriteria();
        List<FileItem> sorted = FileSearchEngine.sortResults(this.allResults, criteria, !this.sortAscending);
        this.allResults.clear();
        this.allResults.addAll(sorted);
        this.applyLiveFilter();
        this.updateHeaderSortIndicators(this.currentSortColumn, this.sortAscending);
        if (selectedItem != null) {
            for (int r = 0; r < this.tableModel.getRowCount(); ++r) {
                FileItem it = this.tableModel.getItem(r);
                if (it == null || !it.path().equals(selectedItem.path())) continue;
                this.resultsTable.setRowSelectionInterval(r, r);
                this.resultsTable.scrollRectToVisible(this.resultsTable.getCellRect(r, 0, true));
                break;
            }
        }
    }

    private void updateHeaderSortIndicators(int activeCol, boolean ascending) {
        for (int i = 0; i < this.resultsTable.getColumnCount(); ++i) {
            TableColumn col = this.resultsTable.getColumnModel().getColumn(i);
            int modelIdx = col.getModelIndex();
            String base = ResultColumn.fromIndex(modelIdx).header();
            if (modelIdx == activeCol) {
                col.setHeaderValue(base + (ascending ? "  \u25b2" : "  \u25bc"));
                continue;
            }
            col.setHeaderValue(base);
        }
        this.resultsTable.getTableHeader().repaint();
    }

    public void saveCurrentConfig() {
        this.config.setDirectory(this.txtFolder.getText().trim());
        this.config.setPattern(this.txtPatterns.getText().trim());
        this.config.setExtension(this.txtExtensions.getText().trim());
        this.config.setFilterResult(this.txtFilter.getText().trim());
        try {
            String lim = this.txtLimit.getText().trim();
            this.config.setLimit(lim.isEmpty() ? 0 : Integer.parseInt(lim));
        }
        catch (NumberFormatException e) {
            System.err.println("Invalid limit format: " + e.getMessage());
        }
        this.configManager.save(this.config);
    }

    public record ProgressChunk(int scanned, int total, FileItem item) {
    }
}


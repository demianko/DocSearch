package com.demian.docsearch.ui;

import com.demian.docsearch.config.ConfigManager;
import com.demian.docsearch.constant.ResultColumn;
import com.demian.docsearch.engine.FileSearchEngine;
import com.demian.docsearch.model.AppConfig;
import com.demian.docsearch.model.FileItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DocSearchApp extends JFrame {
    private final ConfigManager configManager;
    private final AppConfig config;
    private final FileOperationsService fileOperationsService;
    private final SearchController searchController;
    private final List<FileItem> allResults = new ArrayList<>();

    private final ClipboardBuffer clipboardBuffer = new ClipboardBuffer();
    private DocExplorerNav explorerNav;
    private SearchControlsPanel controlsPanel;
    private SearchFilterBar filterBar;
    private ResultsTablePanel tablePanel;
    private StatusBarPanel statusBarPanel;

    private int currentSortColumn = ResultColumn.DATE_MODIFIED.modelIndex();
    private boolean sortAscending = false;

    public DocSearchApp() {
        this(new ConfigManager());
    }

    public DocSearchApp(ConfigManager configManager) {
        this.configManager = configManager != null ? configManager : new ConfigManager();
        this.config = this.configManager.load();
        this.fileOperationsService = new FileOperationsService();
        this.searchController = new SearchController();

        this.setTitle("DocSearch Pro \u2014 Fast Search & Sort");
        this.setSize(1350, 850);
        this.setMinimumSize(new Dimension(1050, 650));
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
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

        this.explorerNav = new DocExplorerNav(
                this::onNavFolderSelected,
                this::onNavFolderRename,
                this::onNavNewFolder,
                this::onNavNewFile,
                this::deleteNavFolder,
                this::moveItemsToDirectory,
                p -> this.copyPaths(List.of(p)),
                p -> this.cutPaths(List.of(p)),
                this::pasteIntoFolder,
                this.clipboardBuffer::hasContent
        );

        this.controlsPanel = new SearchControlsPanel(this.config);
        this.controlsPanel.setOnSearchAction(this::searchFiles);
        this.controlsPanel.setOnStopAction(this::stopSearch);
        this.controlsPanel.setOnBrowseAction(this::browseDirectory);

        this.filterBar = new SearchFilterBar();
        this.filterBar.setOnFilterChanged(this::applyLiveFilter);

        this.tablePanel = new ResultsTablePanel();
        this.tablePanel.setOnOpenRequested(this::openSelectedItem);
        this.tablePanel.setOnSortRequested(this::sortByColumn);
        this.tablePanel.setOnMoveDroppedFiles(this::moveItemsToDirectory);

        new ResultsTableContextMenu(
                this.tablePanel,
                this::openSelectedItem,
                this::openInExplorer,
                this::newFolderUnderSelectedItem,
                this::newFileUnderSelectedItem,
                this::copySelectedItems,
                this::cutSelectedItems,
                this::pasteIntoSelectedOrCurrent,
                this::renameSelectedItem,
                this::deleteSelectedItems,
                this::copySelectedPathsText,
                this.clipboardBuffer::hasContent
        );

        this.statusBarPanel = new StatusBarPanel();

        JPanel rightWorkspace = new JPanel(new BorderLayout(0, 10));
        rightWorkspace.add(this.controlsPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(this.filterBar, BorderLayout.NORTH);
        centerPanel.add(this.tablePanel, BorderLayout.CENTER);
        centerPanel.add(this.statusBarPanel, BorderLayout.SOUTH);
        rightWorkspace.add(centerPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.explorerNav, rightWorkspace);
        splitPane.setDividerLocation(280);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        rootPanel.add(splitPane, BorderLayout.CENTER);

        this.setContentPane(rootPanel);

        int shortcutKey = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        this.getRootPane().registerKeyboardAction(e -> this.renameSelectedItem(), KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), 2);
        this.getRootPane().registerKeyboardAction(
                e -> this.deleteSelectedItems(), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), 2);
        this.getRootPane().registerKeyboardAction(e -> this.stopSearch(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 2);
        this.getRootPane().registerKeyboardAction(
                e -> this.copySelectedItems(), KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey), 2);
        this.getRootPane().registerKeyboardAction(
                e -> this.cutSelectedItems(), KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutKey), 2);
        this.getRootPane().registerKeyboardAction(
                e -> this.pasteIntoSelectedOrCurrent(), KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKey), 2);

        this.restoreInitialDirectory();
    }

    private void restoreInitialDirectory() {
        String dir = this.config.getDirectory();
        if (StringUtils.isNotBlank(dir)) {
            try {
                Path path = Paths.get(dir);
                if (Files.isDirectory(path)) {
                    this.selectDirectory(path);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void selectDirectory(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        this.onNavFolderSelected(dir);
        this.explorerNav.selectPath(dir, true);
        SwingUtilities.invokeLater(() -> this.explorerNav.selectPath(dir, true));
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = this.controlsPanel.getDirectory();
        if (!current.isEmpty() && Files.exists(Paths.get(current), new LinkOption[0])) {
            chooser.setCurrentDirectory(new File(current));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            Path selectedPath = Paths.get(path);
            this.selectDirectory(selectedPath);
        }
    }

    public void onNavFolderSelected(Path folder) {
        if (folder == null || !Files.exists(folder, new LinkOption[0])) {
            return;
        }
        this.controlsPanel.setDirectory(folder.toAbsolutePath().toString());
        this.saveCurrentConfig();
        this.filterBar.resetProgress();
        List<FileItem> items = FileSearchEngine.listDirectSubFolderFiles(folder);
        String criteria = this.getSortCriteria(this.currentSortColumn);
        List<FileItem> sorted = FileSearchEngine.sortResults(items, criteria, !this.sortAscending);
        this.allResults.clear();
        this.allResults.addAll(sorted);
        this.applyLiveFilter();
        this.statusBarPanel.setStatus("Viewing folder: " + folder.getFileName());
    }

    private void onNavFolderRename(Path folderPath) {
        new RenameDialog((Frame) this, folderPath, true, newPath -> {
            if (this.controlsPanel.getDirectory().equalsIgnoreCase(folderPath.toString())) {
                this.controlsPanel.setDirectory(newPath.toAbsolutePath().toString());
                this.onNavFolderSelected(newPath);
            }
            this.explorerNav.onFolderRenamed(folderPath, newPath);
            this.statusBarPanel.setStatus("Renamed folder to '" + newPath.getFileName() + "'");
        }).setVisible(true);
    }

    private void onNavNewFolder(Path parentFolder) {
        new NewItemDialog((Frame) this, parentFolder, true, newFolder -> {
            this.explorerNav.onFolderAdded(parentFolder, newFolder);
            Path curFolder = this.getCurrentFolder();
            if (curFolder != null && (curFolder.equals(parentFolder) || curFolder.equals(newFolder.getParent()))) {
                this.onNavFolderSelected(curFolder);
            }
            this.statusBarPanel.setStatus("Created folder '" + newFolder.getFileName() + "' in '" + parentFolder.getFileName() + "'");
        }).setVisible(true);
    }

    private void onNavNewFile(Path parentFolder) {
        new NewItemDialog((Frame) this, parentFolder, false, newFile -> {
            Path curFolder = this.getCurrentFolder();
            if (curFolder != null && curFolder.equals(parentFolder)) {
                this.onNavFolderSelected(curFolder);
            }
            this.statusBarPanel.setStatus("Created file '" + newFile.getFileName() + "' in '" + parentFolder.getFileName() + "'");
        }).setVisible(true);
    }

    private Path getCurrentFolder() {
        String current = this.controlsPanel.getDirectory();
        if (!current.isEmpty()) {
            try {
                Path p = Paths.get(current);
                if (Files.exists(p)) {
                    return p;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void searchFiles() {
        String dirStr = this.controlsPanel.getDirectory();
        if (StringUtils.isEmpty(dirStr) || !Files.exists(Paths.get(dirStr), new LinkOption[0])) {
            JOptionPane.showMessageDialog(this, "Please enter a valid target directory.", "Directory Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.saveCurrentConfig();
        this.stopSearch();

        final Path rootFolder = Paths.get(dirStr);
        final String patternQuery = this.controlsPanel.getPattern();
        final String extensionsQuery = this.controlsPanel.getExtension();
        final int searchLimit = this.controlsPanel.getLimit();

        this.controlsPanel.setSearchEnabled(false);
        this.controlsPanel.setStopEnabled(true);
        this.filterBar.setIndeterminate(true);
        this.statusBarPanel.setStatus("Scanning directory: " + rootFolder.getFileName() + "...");
        this.statusBarPanel.setCount("0 files found");
        this.allResults.clear();
        this.tablePanel.setItems(List.of());

        this.searchController.startSearch(rootFolder, patternQuery, extensionsQuery, searchLimit,
                new SearchController.SearchExecutionListener() {
                    @Override
                    public void onSearchStarted() {
                    }

                    @Override
                    public void onProgress(SearchController.ProgressChunk lastChunk, List<FileItem> newItems) {
                        for (FileItem item : newItems) {
                            DocSearchApp.this.allResults.add(item);
                            DocSearchApp.this.tablePanel.addItem(item);
                        }
                        if (lastChunk != null) {
                            if (lastChunk.total() > 0) {
                                DocSearchApp.this.filterBar.setProgress(lastChunk.scanned(), lastChunk.total());
                                DocSearchApp.this.statusBarPanel.setStatus(String.format(
                                        "Scanned %,d of %,d files... (Esc to stop)", lastChunk.scanned(), lastChunk.total()));
                            } else if (lastChunk.total() == -1) {
                                DocSearchApp.this.filterBar.setIndeterminate(true);
                                DocSearchApp.this.statusBarPanel.setStatus(String.format(
                                        "Scanning directory: %s (%,d files found)...", rootFolder.getFileName(), lastChunk.scanned()));
                            }
                        }
                        DocSearchApp.this.statusBarPanel.setCount(DocSearchApp.this.allResults.size() + " files found");
                    }

                    @Override
                    public void onSearchCompleted(List<FileItem> results, long elapsedMillis, boolean wasCancelled) {
                        double elapsedSec = (double) elapsedMillis / 1000.0;
                        DocSearchApp.this.controlsPanel.setSearchEnabled(true);
                        DocSearchApp.this.controlsPanel.setStopEnabled(false);
                        DocSearchApp.this.filterBar.setIndeterminate(false);

                        if (wasCancelled) {
                            DocSearchApp.this.statusBarPanel.setStatus(String.format(
                                    "Search stopped (Esc) in %.2fs. Found %,d matching files.",
                                    elapsedSec, DocSearchApp.this.allResults.size()));
                        } else {
                            DocSearchApp.this.filterBar.setProgressValue(DocSearchApp.this.filterBar.getProgressMaximum());
                            DocSearchApp.this.statusBarPanel.setStatus(String.format(
                                    "Completed in %.2fs (Found %,d files in '%s')",
                                    elapsedSec, DocSearchApp.this.allResults.size(), rootFolder.getFileName()));
                        }

                        if (results != null) {
                            DocSearchApp.this.allResults.clear();
                            DocSearchApp.this.allResults.addAll(results);
                        }

                        if (!DocSearchApp.this.allResults.isEmpty()
                                && (DocSearchApp.this.currentSortColumn != ResultColumn.DATE_MODIFIED.modelIndex()
                                || DocSearchApp.this.sortAscending)) {
                            String criteria = DocSearchApp.this.getSortCriteria(DocSearchApp.this.currentSortColumn);
                            List<FileItem> sorted = FileSearchEngine.sortResults(
                                    DocSearchApp.this.allResults, criteria, !DocSearchApp.this.sortAscending);
                            DocSearchApp.this.allResults.clear();
                            DocSearchApp.this.allResults.addAll(sorted);
                        }
                        DocSearchApp.this.applyLiveFilter();
                    }
                }
        );
    }

    private void stopSearch() {
        this.searchController.stopSearch();
        this.controlsPanel.setSearchEnabled(true);
        this.controlsPanel.setStopEnabled(false);
        this.filterBar.setIndeterminate(false);
    }

    private void applyLiveFilter() {
        String filterText = this.filterBar.getFilterText();
        if (StringUtils.isEmpty(filterText)) {
            this.tablePanel.setItems(this.allResults);
            this.statusBarPanel.setCount(this.allResults.size() + " items");
            return;
        }
        List<FileItem> filtered = this.searchController.filterItems(this.allResults, filterText);
        this.tablePanel.setItems(filtered);
        this.statusBarPanel.setCount(filtered.size() + " / " + this.allResults.size() + " items");
    }

    private void openSelectedItem() {
        FileItem item = this.tablePanel.getSelectedItem();
        this.openSelectedItem(item);
    }

    private void openSelectedItem(FileItem item) {
        if (item == null) return;
        if (item.isDirectory()) {
            this.onNavFolderSelected(item.path());
            this.explorerNav.selectPath(item.path(), true);
        } else {
            this.fileOperationsService.openItem(item, this, this::onNavFolderSelected);
        }
    }

    private void openInExplorer() {
        this.fileOperationsService.openInExplorer(this.tablePanel.getSelectedPaths());
    }

    public void copyPaths(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) return;
        this.clipboardBuffer.copy(paths);
        this.statusBarPanel.setStatus("Copied " + paths.size() + " item(s) to clipboard (Ctrl+V to paste)");
    }

    public void cutPaths(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) return;
        this.clipboardBuffer.cut(paths);
        this.statusBarPanel.setStatus("Cut " + paths.size() + " item(s) to clipboard (Ctrl+V to paste)");
    }

    public void copySelectedItems() {
        List<Path> paths = this.tablePanel.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths)) {
            Path navPath = this.explorerNav.getSelectedPath();
            if (navPath != null && navPath.getParent() != null) {
                paths = List.of(navPath);
            }
        }
        this.copyPaths(paths);
    }

    public void cutSelectedItems() {
        List<Path> paths = this.tablePanel.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths)) {
            Path navPath = this.explorerNav.getSelectedPath();
            if (navPath != null && navPath.getParent() != null) {
                paths = List.of(navPath);
            }
        }
        this.cutPaths(paths);
    }

    public void pasteIntoFolder(Path targetFolder) {
        if (targetFolder == null || !Files.isDirectory(targetFolder)) return;
        int count = this.fileOperationsService.pasteItems(
                this,
                this.clipboardBuffer,
                targetFolder,
                this.explorerNav::onFolderRenamed,
                () -> {
                    Path cur = this.getCurrentFolder();
                    if (cur != null && (cur.equals(targetFolder) || cur.startsWith(targetFolder) || targetFolder.startsWith(cur))) {
                        this.onNavFolderSelected(cur);
                    }
                }
        );
        if (count > 0) {
            this.statusBarPanel.setStatus("Pasted " + count + " item(s) into '" + targetFolder.getFileName() + "'");
        }
    }

    public void pasteIntoSelectedOrCurrent() {
        FileItem item = this.tablePanel.getSelectedItem();
        if (item != null && item.isDirectory()) {
            this.pasteIntoFolder(item.path());
            return;
        }
        Path navPath = this.explorerNav.getSelectedPath();
        if (navPath != null && Files.isDirectory(navPath) && this.explorerNav.isFocusOwner()) {
            this.pasteIntoFolder(navPath);
            return;
        }
        Path cur = this.getCurrentFolder();
        if (cur != null && Files.isDirectory(cur)) {
            this.pasteIntoFolder(cur);
            return;
        }
        if (navPath != null && Files.isDirectory(navPath)) {
            this.pasteIntoFolder(navPath);
        }
    }

    private void copySelectedPathsText() {
        List<Path> paths = this.tablePanel.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths)) return;
        boolean success = this.fileOperationsService.copyPathsText(paths);
        if (success) {
            this.statusBarPanel.setStatus("Copied " + paths.size() + " path(s) to clipboard");
        }
    }

    private void renameSelectedItem() {
        FileItem item = this.tablePanel.getSelectedItem();
        int row = this.tablePanel.getSelectedRow();
        if (row >= 0 && item != null) {
            new RenameDialog((Frame) this, item.path(), item.isDirectory(), newPath -> {
                FileItem updated = item.withPath(newPath);
                this.tablePanel.updateItem(row, updated);
                if (item.isDirectory()) {
                    this.explorerNav.onFolderRenamed(item.path(), newPath);
                }
                this.statusBarPanel.setStatus("Renamed to '" + newPath.getFileName() + "'");
            }).setVisible(true);
            return;
        }
        Path navPath = this.explorerNav.getSelectedPath();
        if (navPath != null && navPath.getParent() != null) {
            this.onNavFolderRename(navPath);
        }
    }

    private void newFolderUnderSelectedItem() {
        FileItem item = this.tablePanel.getSelectedItem();
        if (item != null && item.isDirectory()) {
            this.onNavNewFolder(item.path());
            return;
        }
        Path navPath = this.explorerNav.getSelectedPath();
        if (navPath != null && Files.isDirectory(navPath)) {
            this.onNavNewFolder(navPath);
        }
    }

    private void newFileUnderSelectedItem() {
        FileItem item = this.tablePanel.getSelectedItem();
        if (item != null && item.isDirectory()) {
            this.onNavNewFile(item.path());
            return;
        }
        Path navPath = this.explorerNav.getSelectedPath();
        if (navPath != null && Files.isDirectory(navPath)) {
            this.onNavNewFile(navPath);
        }
    }

    public void deleteSelectedItems() {
        List<Path> paths = this.tablePanel.getSelectedPaths();
        if (CollectionUtils.isEmpty(paths)) {
            Path navPath = this.explorerNav.getSelectedPath();
            if (navPath != null && navPath.getParent() != null) {
                paths = List.of(navPath);
            }
        }
        if (CollectionUtils.isEmpty(paths)) return;
        this.confirmAndDelete(paths);
    }

    public void deleteNavFolder(Path folderPath) {
        if (folderPath == null || folderPath.getParent() == null) return;
        this.confirmAndDelete(List.of(folderPath));
    }

    public void confirmAndDelete(List<Path> paths) {
        Path curFolder = this.getCurrentFolder();
        int deleted = this.fileOperationsService.deleteItems(
                this,
                paths,
                curFolder,
                this.explorerNav::onFolderDeleted,
                curFolderDeleted -> {
                    if (curFolderDeleted) {
                        Path parent = curFolder != null ? curFolder.getParent() : null;
                        if (parent != null && Files.exists(parent, new LinkOption[0])) {
                            this.onNavFolderSelected(parent);
                        } else {
                            this.allResults.clear();
                            this.applyLiveFilter();
                            this.controlsPanel.setDirectory("");
                        }
                    } else if (curFolder != null && Files.exists(curFolder, new LinkOption[0])) {
                        this.onNavFolderSelected(curFolder);
                    }
                },
                () -> {
                    if (curFolder != null && Files.exists(curFolder, new LinkOption[0])) {
                        this.onNavFolderSelected(curFolder);
                    }
                }
        );
        if (deleted > 0) {
            this.statusBarPanel.setStatus("Deleted " + deleted + " item(s)");
        }
    }

    public void moveItemsToDirectory(List<File> files, Path targetDir) {
        int moved = this.fileOperationsService.moveItems(
                this,
                files,
                targetDir,
                this.explorerNav::onFolderRenamed,
                () -> {
                    Path current = this.getCurrentFolder();
                    if (current != null && Files.exists(current, new LinkOption[0])) {
                        this.onNavFolderSelected(current);
                    }
                }
        );
        if (moved > 0) {
            this.statusBarPanel.setStatus("Moved " + moved + " item(s) to '" + targetDir.getFileName() + "'");
        }
    }

    private String getSortCriteria(int col) {
        return ResultColumn.fromIndex(col).sortCriteria();
    }

    private void sortByColumn(int col) {
        if (this.allResults.isEmpty() || col == ResultColumn.INDEX.modelIndex()) {
            return;
        }
        FileItem selectedItem = this.tablePanel.getSelectedItem();
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
        this.tablePanel.updateHeaderSortIndicators(this.currentSortColumn, this.sortAscending);
        if (selectedItem != null) {
            this.tablePanel.selectItemByPath(selectedItem.path());
        }
    }

    public void saveCurrentConfig() {
        this.config.setDirectory(this.controlsPanel.getDirectory());
        this.config.setPattern(this.controlsPanel.getPattern());
        this.config.setExtension(this.controlsPanel.getExtension());
        this.config.setLimit(this.controlsPanel.getLimit());
        this.configManager.save(this.config);
    }

    public DocExplorerNav getExplorerNav() {
        return this.explorerNav;
    }

    public SearchControlsPanel getControlsPanel() {
        return this.controlsPanel;
    }

    public ResultsTablePanel getTablePanel() {
        return this.tablePanel;
    }

    public List<FileItem> getAllResults() {
        return this.allResults;
    }

    public record ProgressChunk(int scanned, int total, FileItem item) {
    }
}

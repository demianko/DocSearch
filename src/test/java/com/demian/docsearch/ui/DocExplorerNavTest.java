package com.demian.docsearch.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocExplorerNavTest {

    @Test
    void testFolderRenamedPreservesTreeStructure(@TempDir Path tempDir) throws IOException {
        Path rootFolder = tempDir.resolve("RootTestFolder");
        Files.createDirectory(rootFolder);
        Path childA = rootFolder.resolve("Alpha");
        Files.createDirectory(childA);
        Path childB = rootFolder.resolve("Beta");
        Files.createDirectory(childB);

        DocExplorerNav nav = new DocExplorerNav(null, null);
        nav.selectPath(childA, true);

        // Alpha is selected
        Path selectedBefore = nav.getSelectedPath();
        assertThat(selectedBefore).isEqualTo(childA.toAbsolutePath().normalize());

        // Rename Alpha to AlphaRenamed
        Path childRenamed = rootFolder.resolve("AlphaRenamed");
        Files.move(childA, childRenamed);

        nav.onFolderRenamed(childA, childRenamed);

        // Path should now be AlphaRenamed and remain selected
        Path selectedAfter = nav.getSelectedPath();
        assertThat(selectedAfter).isEqualTo(childRenamed.toAbsolutePath().normalize());
    }

    @Test
    void testFolderAddedInsertsChildNode(@TempDir Path tempDir) throws IOException {
        Path rootFolder = tempDir.resolve("ParentTestFolder");
        Files.createDirectory(rootFolder);
        Path child1 = rootFolder.resolve("ChildOne");
        Files.createDirectory(child1);

        DocExplorerNav nav = new DocExplorerNav(null, null);
        nav.selectPath(rootFolder, true);

        Path newChild = rootFolder.resolve("ChildTwo");
        Files.createDirectory(newChild);

        nav.onFolderAdded(rootFolder, newChild);

        // Selected path should be the new child
        assertThat(nav.getSelectedPath()).isEqualTo(newChild.toAbsolutePath().normalize());
    }

    @Test
    void testSelectPathDeeplyNestedExpandsAndSelects(@TempDir Path tempDir) throws IOException {
        Path level1 = tempDir.resolve("L1");
        Path level2 = level1.resolve("L2");
        Path level3 = level2.resolve("L3");
        Path level4 = level3.resolve("L4");
        Files.createDirectories(level4);

        DocExplorerNav nav = new DocExplorerNav(null, null);
        nav.selectPath(level3, true);

        assertThat(nav.getSelectedPath()).isEqualTo(level3.toAbsolutePath().normalize());
        assertThat(nav.getExpandedPaths()).contains(
                level1.toAbsolutePath().normalize(),
                level2.toAbsolutePath().normalize(),
                level3.toAbsolutePath().normalize()
        );
    }
}

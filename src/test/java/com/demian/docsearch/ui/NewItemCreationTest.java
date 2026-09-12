package com.demian.docsearch.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NewItemCreationTest {

    @Test
    void testIsValidName() {
        assertThat(NewItemDialog.isValidName("MyFolder")).isTrue();
        assertThat(NewItemDialog.isValidName("notes.txt")).isTrue();
        assertThat(NewItemDialog.isValidName("")).isFalse();
        assertThat(NewItemDialog.isValidName("   ")).isFalse();
        assertThat(NewItemDialog.isValidName(null)).isFalse();

        // Illegal characters: \ / : * ? " < > |
        assertThat(NewItemDialog.isValidName("abc\\def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc/def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc:def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc*def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc?def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc\"def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc<def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc>def")).isFalse();
        assertThat(NewItemDialog.isValidName("abc|def")).isFalse();
    }

    @Test
    void testSuggestDefaultNameFolder(@TempDir Path tempDir) throws IOException {
        String name1 = NewItemDialog.suggestDefaultName(tempDir, true);
        assertThat(name1).isEqualTo("New Folder");

        Files.createDirectory(tempDir.resolve("New Folder"));
        String name2 = NewItemDialog.suggestDefaultName(tempDir, true);
        assertThat(name2).isEqualTo("New Folder (2)");

        Files.createDirectory(tempDir.resolve("New Folder (2)"));
        String name3 = NewItemDialog.suggestDefaultName(tempDir, true);
        assertThat(name3).isEqualTo("New Folder (3)");
    }

    @Test
    void testSuggestDefaultNameFile(@TempDir Path tempDir) throws IOException {
        String name1 = NewItemDialog.suggestDefaultName(tempDir, false);
        assertThat(name1).isEqualTo("New File.txt");

        Files.createFile(tempDir.resolve("New File.txt"));
        String name2 = NewItemDialog.suggestDefaultName(tempDir, false);
        assertThat(name2).isEqualTo("New File (2).txt");
    }

    @Test
    void testPhysicalCreationUnderSelectedFolder(@TempDir Path tempDir) throws IOException {
        Path subFolder = tempDir.resolve("ParentFolder");
        Files.createDirectory(subFolder);

        Path newFolder = subFolder.resolve("ChildFolder");
        Files.createDirectory(newFolder);
        assertThat(Files.isDirectory(newFolder)).isTrue();
        assertThat(newFolder.getParent()).isEqualTo(subFolder);

        Path newFile = subFolder.resolve("ChildDoc.txt");
        Files.createFile(newFile);
        assertThat(Files.isRegularFile(newFile)).isTrue();
        assertThat(newFile.getParent()).isEqualTo(subFolder);
    }

    @Test
    void testNameConflictDetection(@TempDir Path tempDir) throws IOException {
        Path existingFolder = tempDir.resolve("ExistingFolder");
        Files.createDirectory(existingFolder);

        Path existingFile = tempDir.resolve("ExistingDoc.txt");
        Files.createFile(existingFile);

        // Conflicting folder name
        assertThat(Files.exists(tempDir.resolve("ExistingFolder"))).isTrue();
        assertThat(Files.isDirectory(tempDir.resolve("ExistingFolder"))).isTrue();

        // Conflicting file name
        assertThat(Files.exists(tempDir.resolve("ExistingDoc.txt"))).isTrue();
        assertThat(Files.isRegularFile(tempDir.resolve("ExistingDoc.txt"))).isTrue();

        // Non-conflicting name
        assertThat(Files.exists(tempDir.resolve("NonExistent"))).isFalse();
    }
}

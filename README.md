# DocSearch Pro - Java 21 Swing Edition

A desktop file and document search utility built with **Java 21 LTS** and **Java Swing** with the modern **FlatLaf Dark Theme**.

## Features & Capabilities

1. **Dual-Pane Navigation & Split Layout**:
   - **Left Explorer Pane (`DocExplorerNav`)**: Windows Explorer style tree navigation with lazy directory expansion, system drive discovery,
     folder creation, file creation, folder renaming (<kbd>F2</kbd>), deletion (<kbd>Delete</kbd>), and context menu actions.
   - **Right Results Pane (`ResultsTableModel` & `JTable`)**: Streamed search results displaying sequential integer **# (Index)**, **Name**,
     **Year**, **Date Modified**, and **Directory Path**.
   - **Folder Navigation**: Double-click (or press <kbd>Enter</kbd>) on any folder to navigate directly into it; single-click selects the item.

2. **Folder Context Menus ("New Folder" & "New File")**:
   - Right-click any folder in the left Explorer tree or search results table to access:
     - **📁 New Folder**: Opens a compact modal dialog to enter a folder name. Automatically suggests next available names (`"New Folder"`,
       `"New Folder (2)"`), validates against invalid characters (`\ / : * ? " < > |`), checks collisions, and creates the folder.
     - **📄 New File**: Opens a compact modal dialog to enter a file name. Automatically suggests next available names (`"New File.txt"`,
       `"New File (2).txt"`), pre-selects the basename for quick typing, validates input, and creates the file.
   - In-place tree updates preserve all expanded directory nodes without collapsing the tree.

3. **Delete Action with Confirmation (<kbd>Delete</kbd> or Context Menu)**:
   - Available via right-click context menu (**🗑️ Delete**) or the <kbd>Delete</kbd> key for single files, folders, or multi-item selections.
   - Displays a confirmation dialog before permanent deletion:
     - Prompts specifically for single files, warns about recursive deletion for folders, and displays count for multiple items.
   - Deletes files and folders recursively using Apache Commons IO.
   - Automatically removes deleted items from the results table and updates the Explorer tree.
   - System drive roots (`C:\`, `D:\`) are protected against accidental deletion.
   - If the folder currently being viewed is deleted, the view automatically navigates to its parent directory.

4. **Drag-and-Drop Moving**:
   - Drag a file or group of files directly from the results table and drop them onto:
     - Any **folder row** in the search results table.
     - Any **folder node** in the left Explorer tree navigation panel.
   - Automatically moves the selected files into the target folder and refreshes the directory view.
   - Prevents invalid moves such as moving a folder into a subdirectory of itself or moving into the same folder.

5. **Interactive Column Header Click Sorting**:
   - Click column headers to toggle sort direction with visual arrow indicators (`▲` / `▼`):
     - **#** (Col 0): 1-based sequential index numbering.
     - **Name** (Col 1): Alphabetical A–Z (▲) / Z–A (▼).
     - **Year** (Col 2): Newest year first (▼) / Oldest year first (▲).
     - **Date Modified** (Col 3): Newest timestamp first (▼) / Oldest first (▲).
     - **Directory Path** (Col 4): Folder path A–Z (▲) / Z–A (▼).
   - Folders remain anchored at the top regardless of sort direction.
   - Preserves row selection and automatically scrolls selected items into view across sorts.

6. **Advanced Query Syntax**:
   - **Wildcards**: `*` (zero or more characters), `?` (single character).
   - **Word-Boundary Matching**: Spaces match common file delimiters (`.`, `_`, `-`, `+`, spaces).
   - **Logical OR Alternatives**: Pipe syntax (e.g. `tutorial | guide | intro`).
   - **Multi-Query Patterns**: Comma-separated query terms (e.g. `python*, java*`).
   - **Exclusion Filters**:
     - Inline exclusions: `learning python NOT draft`
     - Global exclusions: `-draft, NOT old, NOT temp`

7. **Extension Filtering**:
   - Comma-delimited extension filtering with inclusion and exclusion support:
     - Inclusions: `pdf, epub, mobi`
     - Exclusions: `-tmp, -log, NOT txt`

8. **Smart Metadata Extraction**:
   - Fast filename parsing extracting release and publication year (`1990`–`2029`).
   - Publisher detection (`O'Reilly`, `Packt`, `Manning`, `Apress`, `Wiley`, `Addison-Wesley`, `No Starch`, `Microsoft`, etc.).

9. **Instant Direct Folder Browsing**:
   - Selecting any folder in the left navigation tree immediately loads its direct child files and subdirectories without recursive overhead.
   - System directories (`$RECYCLE.BIN`, `System Volume Information`) are automatically filtered out.

10. **Native Windows Shell Integration**:
    - **Dual-Flavor Clipboard Copy (<kbd>Ctrl+C</kbd>)**: Sets `CF_HDROP` (`DataFlavor.javaFileListFlavor`) allowing users to copy results and
      paste actual file objects directly into Windows Explorer or Desktop, as well as text paths.
    - **In-Place Rename Dialog (<kbd>F2</kbd> or Context Menu)**: Centered modal dialog with base name pre-selected (excluding extension),
      collision checks, invalid character validation (`\ / : * ? " < > |`), and non-collapsing tree updates.
    - **Context Menu & Shortcuts**: "Open File" / "Open Folder" (<kbd>Enter</kbd> or Double-Click), "Open in File Explorer",
      "Copy Full Path" (<kbd>Ctrl+C</kbd>), "Delete" (<kbd>Delete</kbd>), and "Select All" (<kbd>Ctrl+A</kbd>).

11. **Live Search Streaming & Background Execution**:
    - Asynchronous search execution powered by `SwingWorker` keeps the GUI completely responsive during long operations.
    - Real-time progress bar with live file scan counts and progress metrics.
    - Clean search cancellation via <kbd>Esc</kbd> or the Stop button.

12. **Live In-Memory Filtering & History Persistence**:
    - Instant live filter bar filters loaded results in real-time as you type without re-scanning disks.
    - Live filter query is kept in-memory only and is excluded from configuration persistence.
    - Automatic persistence of last directory, patterns, extensions, search limit, window dimensions, and splitter position
      in `~/.docsearch/config.json`.

13. **Multi-Instance Support**:
    - Launch multiple independent search windows concurrently.

---

## Prerequisites

- **Java JDK 21+** (e.g. `C:\ADev\lang\java\jdk21`)
- **Apache Maven 3.9+** (e.g. `C:\ADev\tools\apache-maven-3.9.6`)

---

## Build & Run

### Build with Maven
```cmd
build.bat
```
`build.bat` verifies that `%JAVA_HOME%` is configured and points to a valid JDK before compiling and packaging the shaded fat JAR.

Or manually:
```cmd
mvn clean package
```

### Launch Application
```cmd
run.bat
```
Or directly using the shaded JAR:
```cmd
java -jar target\doc-search-pro-1.0.0.jar
```
Or passing an initial directory:
```cmd
java -jar target\doc-search-pro-1.0.0.jar "C:\MyFolder"
```

### Run Tests
```cmd
mvn test
```
All unit tests cover query parsing, wildcards, exclusions, metadata extraction, NIO.2 search, delete/move operations, and config persistence.

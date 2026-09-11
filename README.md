# FileSearch Pro - Java 21 Swing Edition

A desktop file search utility ported from the Python/PyQt version to **Java 21 LTS** and **Java Swing** with the modern **FlatLaf Dark Theme**.

## Features & Capabilities

1. **Dual-Pane Navigation & Split Layout**:
   - **Left Explorer Pane (`FileExplorerNav`)**: Windows Explorer style tree navigation with lazy directory expansion, system drive discovery,
     folder renaming (<kbd>F2</kbd>), and context menu actions ("Open in File Explorer", "Copy Path", "Rename").
   - **Right Results Pane (`ResultsTableModel` & `JTable`)**: Streamed search results displaying **Name**, **Year**, **Date Modified**, and
     **Directory Path**.

2. **Interactive Column Header Click Sorting**:
   - Click column headers to toggle sort direction with visual arrow indicators (`▲` / `▼`):
     - **Name** (Col 0): Alphabetical A–Z (▲) / Z–A (▼).
     - **Year** (Col 1): Newest year first (▼) / Oldest year first (▲).
     - **Date Modified** (Col 2): Newest timestamp first (▼) / Oldest first (▲).
     - **Directory Path** (Col 3): Folder path A–Z (▲) / Z–A (▼).
   - Folders remain anchored at the top regardless of sort direction.
   - Preserves row selection and automatically scrolls selected items into view across sorts.

3. **Advanced Query Syntax**:
   - **Wildcards**: `*` (zero or more characters), `?` (single character).
   - **Word-Boundary Matching**: Spaces match common file delimiters (`.`, `_`, `-`, `+`, spaces).
   - **Logical OR Alternatives**: Pipe syntax (e.g. `tutorial | guide | intro`).
   - **Multi-Query Patterns**: Comma-separated query terms (e.g. `python*, java*`).
   - **Exclusion Filters**:
     - Inline exclusions: `learning python NOT draft`
     - Global exclusions: `-draft, NOT old, NOT temp`

4. **Extension Filtering**:
   - Comma-delimited extension filtering with inclusion and exclusion support:
     - Inclusions: `pdf, epub, mobi`
     - Exclusions: `-tmp, -log, NOT txt`

5. **Smart Metadata Extraction**:
   - Fast filename parsing extracting release and publication year (`1990`–`2029`).
   - Publisher detection (`O'Reilly`, `Packt`, `Manning`, `Apress`, `Wiley`, `Addison-Wesley`, `No Starch`, `Microsoft`, etc.).

6. **Instant Direct Folder Browsing**:
   - Selecting any folder in the left navigation tree immediately loads its direct child files and subdirectories without recursive overhead.
   - System directories (`$RECYCLE.BIN`, `System Volume Information`) are automatically filtered out.

7. **Native Windows Shell Integration**:
   - **Dual-Flavor Clipboard Copy (<kbd>Ctrl+C</kbd>)**: Sets `CF_HDROP` (`DataFlavor.javaFileListFlavor`) allowing users to copy results and
     paste actual file objects directly into Windows Explorer or Desktop, as well as text paths.
   - **In-Place Rename Dialog (<kbd>F2</kbd> or Context Menu)**: Centered modal dialog with base name pre-selected (excluding extension),
     collision checks, and invalid character validation (`\ / : * ? " < > |`).
   - **Context Menu & Shortcuts**: "Open File" (<kbd>Enter</kbd> or Double-Click), "Open in File Explorer", "Copy Full Path" (<kbd>Ctrl+C</kbd>),
     and "Select All" (<kbd>Ctrl+A</kbd>).

8. **Live Search Streaming & Background Execution**:
   - Asynchronous search execution powered by `SwingWorker` keeps the GUI completely responsive during long operations.
   - Real-time progress bar with live file scan counts and progress metrics.
   - Clean search cancellation via <kbd>Esc</kbd> or the Stop button.

9. **Live In-Memory Filtering & History Persistence**:
   - Instant live filter bar filters loaded results in real-time as you type without re-scanning disks.
   - Automatic persistence of last directory, patterns, extensions, search limit, window dimensions, and splitter position
     in `~/.filesearch/config.json`.

10. **Multi-Instance Support**:
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
java -jar target/file-search-pro-1.0.0.jar
```
Or passing an initial directory:
```cmd
java -jar target/file-search-pro-1.0.0.jar "C:\MyFolder"
```

### Run Tests
```cmd
mvn test
```
All unit tests cover query parsing, wildcards, exclusions, metadata extraction, NIO.2 file search engine, and configuration persistence.

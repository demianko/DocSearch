# DocSearch Pro - Java 21 Swing Edition

A high-performance, modern desktop document and file search utility built with **Java 21 LTS**, **Java Swing**, and the **FlatLaf Dark Theme**.

<img width="1345" height="1038" alt="DocSearch file name based search" src="https://github.com/user-attachments/assets/0378919a-7afa-4b06-8e4b-c83f3361f30a" />


<img width="1347" height="1132" alt="DocSearch Pro AI Search UI" src="https://github.com/user-attachments/assets/75d1ec95-c368-4c06-bdc0-d59d4c7bbcb7" />

<img width="1155" height="252" alt="LLM configuration" src="https://github.com/user-attachments/assets/d880c696-e1f4-458a-95b8-910cdbe48245" />


---

## 🚀 Key Features & Capabilities

### 1. 🗂️ Tabbed Multi-Mode Architecture
- **Tab 1 — 🔍 Standard Search**: Instant wildcard, multi-query, extension, and publisher search with zero-overhead live folder browsing.
- **Tab 2 — 🤖 AI Search**: Natural language semantic intent search powered by OpenAI-compatible LLMs and local SQLite indexing.
- **Tab 3 — ⚙️ AI Settings**: Centralized configuration for OpenAI-compliant endpoints, API keys, model discovery, temperature, parallel concurrency, and customizable system prompts.

---

### 2. ⚡ High-Performance AI Natural Language Search
- **Natural Language Intent Queries**: Query documents using human-like semantic prompts (e.g., *"2023 invoices"*, *"tax returns"*, *"machine learning architecture designs"*).
- **Hybrid Candidate Pre-Scoring**: High-speed keyword tokenization and metadata ranking filters large directories down to high-relevance candidate pools before invoking the LLM.
- **Configurable Parallel LLM Requests (Default: 4)**: Dispatches chunked evaluations concurrently over a bounded `Semaphore` worker pool (configurable from 1 to 32 parallel requests in AI Settings).
- **Dynamic Small-Chunk Processing (15–30 files/chunk)**: Ultra-compact JSON payloads (~600–800 tokens) allow LLMs to return chunk evaluations in under **500–800ms**.
- **Real-Time Progressive Result Streaming**: Discovered matches appear in the results table live as each LLM chunk finishes—no waiting for the entire batch to complete.
- **Outdated Index Detection & 3-Second Auto-Dismiss Confirmation Dialog**:
  - Automatically compares disk `last_modified` timestamps against SQLite `indexed_at` timestamps.
  - If modified or new files are detected, a modal prompt offers **⚡ Reindex & Search**, **Search As-Is**, or **Cancel**.
  - Includes a live 1-second countdown timer that automatically dismisses after **3 seconds** and defaults to continuing search unattended.
- **Tab-Isolated Result Sets**: AI search results remain isolated from standard search; typing in **🎯 Filter Results** filters strictly within AI search results in-memory.

---

### 3. 🗄️ High-Speed Local SQLite Index Database
- Embedded `sqlite-jdbc` index database stored at `~/.filesearch/docsearch_index.db` with indexed lookups on `root_folder`, `parent_folder`, `extension`, and `file_name`.
- **Atomic Recreation on Reindexing**: Reindexing a directory performs a boundary-safe deletion of previous records matching the directory tree before batch-inserting fresh records from disk.
- Automatically handles cross-platform forward-slash (`/`) and backslash (`\`) path normalization.

---

### 4. 🌐 AI Settings & OpenAI-Compliant Endpoint Support
- **Universal Provider Compatibility**: Works seamlessly with OpenAI, Ollama (`http://localhost:11434/v1`), LM Studio, vLLM, LocalAI, Azure OpenAI, OpenRouter, and custom endpoints.
- **Dynamic Model Discovery ("🔄 Fetch Models")**: Queries endpoint `/v1/models` and automatically populates available models into an editable dropdown.
- **Connection Diagnostics ("🔌 Test Connection")**: Sends a lightweight validation request and displays live visual feedback.
- **Concurrency & Tuning Controls**: Configure **⚡ Parallel Requests** (1–32), **🌡️ Temperature** (0.0–2.0), and **Timeout** (seconds).
- **Customizable System Prompt Dialog**: Dedicated modal dialog with one-click **↺ Reset** to restore default system prompt.

---

### 5. 📝 Enterprise Logging with SLF4J & Logback
- Full integration with **SLF4J 2.0.16** and **Logback 1.5.12**.
- **Rolling File Logging**: Automatically writes rotating daily log files to `~/.filesearch/logs/docsearch.log` (10MB max per file, 7-day retention, 50MB total cap).
- **Console Output**: Formatted with timestamps, thread names, log levels, and logger categories.

---

### 6. 📁 Explorer Navigation & File Management
- **Left Explorer Pane (`DocExplorerNav`)**: Windows Explorer tree navigation with lazy directory expansion, system drive discovery, folder renaming (<kbd>F2</kbd>), deletion (<kbd>Delete</kbd>), and context menus.
- **Folder Context Menus ("New Folder" & "New File")**: Modal dialogs with automatic naming suggestion (`"New Folder (2)"`), collision checks, and character validation (`\ / : * ? " < > |`).
- **Safe Recursive Deletion**: Prompts confirmation before permanent deletion with recursive deletion protection for system drive roots (`C:\`, `D:\`).
- **Drag-and-Drop Moving**: Drag files from the results table directly onto folder rows or Explorer tree nodes to move files safely with circular dependency prevention.
- **Interactive Column Header Click Sorting**: Click column headers with visual arrow indicators (`▲` / `▼`) for Index `#`, Name, Year, Date Modified, and Directory Path.
- **Dual-Flavor Clipboard Copy (<kbd>Ctrl+C</kbd>)**: Copies native `CF_HDROP` file objects directly pasteable into Windows Explorer and Desktop.
- **Live In-Memory Filtering**: Instant live filter bar filters loaded results in real-time as you type without disk rescans.
- **Configuration Persistence**: Automatic saving of last directory, patterns, extensions, search limit, AI settings, window geometry, and split positions in `~/.filesearch/config.json`.

---

## 🛠️ Technology Stack

| Component | Technology / Library | Version |
|---|---|---|
| **Runtime** | Java LTS | 21+ |
| **GUI Framework** | Java Swing / AWT | Core |
| **Look and Feel** | FlatLaf (Dark) | 3.5.4 |
| **Local Database** | SQLite JDBC | 3.47.2.0 |
| **JSON Serialization** | Jackson Databind | 2.18.2 |
| **HTTP Client** | Java 11+ HttpClient (`java.net.http`) | Native |
| **Logging Facade** | SLF4J API | 2.0.16 |
| **Logging Implementation** | Logback Classic & Core | 1.5.12 |
| **Testing** | JUnit 5 & AssertJ | 5.11.4 / 3.27.3 |

---

## 📂 File Locations & Structure

All user data, index caches, and logs are organized in the user's home directory under `~/.filesearch/`:

```
~/.filesearch/
├── config.json              # UI state, search history, AI endpoint & parallel configuration
├── docsearch_index.db       # Embedded SQLite index database for lightning-fast lookups
└── logs/
    ├── docsearch.log        # Active application log
    └── docsearch.YYYY-MM-DD.N.log  # Archived rolling logs (max 10MB each, 7-day retention)
```

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Context | Action |
|---|---|---|
| <kbd>Enter</kbd> | Search / Filter Inputs | Execute search or trigger immediate filter |
| <kbd>Ctrl</kbd> + <kbd>C</kbd> | Results Table | Copy selected rows as text and native files to OS clipboard |
| <kbd>F2</kbd> | Explorer Tree | Rename selected folder or file |
| <kbd>Delete</kbd> | Explorer Tree / Results | Prompt confirmation and safely delete selected file/folder |
| <kbd>Ctrl</kbd> + <kbd>L</kbd> | Path Bar | Focus and highlight directory path input |
| <kbd>Esc</kbd> | Modals / Dialogs | Dismiss active dialog or cancel operation |

---

## ⚙️ Development Prerequisites

- **Java JDK 21+** (e.g. `C:\ADev\lang\java\jdk21`)
- **Apache Maven 3.9+** (e.g. `C:\ADev\tools\apache-maven-3.9.6`)

---

## 📦 Build & Run

### Build with Maven
```cmd
build.bat
```
`build.bat` verifies that `%JAVA_HOME%` is configured and points to a valid JDK before compiling and packaging the shaded fat JAR.

Or manually via Maven:
```cmd
mvn clean package
```

### Launch Application
```cmd
run.bat
```
Or run the shaded fat JAR directly:
```cmd
java -jar target\doc-search-pro-1.0.0.jar
```
Or specify an initial target directory:
```cmd
java -jar target\doc-search-pro-1.0.0.jar "C:\MyFolder"
```

### Run Test Suite
```cmd
mvn test
```
All **79 automated tests** validate query parsing, wildcards, metadata extraction, NIO.2 search, SQLite index recreation, AI parallel chunking, progressive streaming, auto-dismiss confirmation dialogs, and config persistence.

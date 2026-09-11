package com.demian.docsearch.constant;

public enum ResultColumn {
    NAME(0, "Name", "name", 600, true),
    YEAR(1, "Year", "year", 70, false),
    DATE_MODIFIED(2, "Date Modified", "modified", 140, false),
    DIRECTORY_PATH(3, "Directory Path", "path", 280, true);

    private final int modelIndex;
    private final String header;
    private final String sortCriteria;
    private final int defaultWidth;
    private final boolean defaultAscending;

    private ResultColumn(int modelIndex, String header, String sortCriteria, int defaultWidth, boolean defaultAscending) {
        this.modelIndex = modelIndex;
        this.header = header;
        this.sortCriteria = sortCriteria;
        this.defaultWidth = defaultWidth;
        this.defaultAscending = defaultAscending;
    }

    public int modelIndex() {
        return this.modelIndex;
    }

    public String header() {
        return this.header;
    }

    public String sortCriteria() {
        return this.sortCriteria;
    }

    public int defaultWidth() {
        return this.defaultWidth;
    }

    public boolean defaultAscending() {
        return this.defaultAscending;
    }

    public static ResultColumn fromIndex(int index) {
        for (ResultColumn col : ResultColumn.values()) {
            if (col.modelIndex != index) continue;
            return col;
        }
        return DATE_MODIFIED;
    }
}


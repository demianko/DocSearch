package com.demian.docsearch.constant;

public enum ResultColumn {
    INDEX(0, AppConstants.COL_INDEX, "index", 55, true),
    NAME(1, AppConstants.COL_NAME, AppConstants.SORT_NAME, 560, true),
    YEAR(2, AppConstants.COL_YEAR, AppConstants.SORT_YEAR, 70, false),
    DATE_MODIFIED(3, AppConstants.COL_DATE_MODIFIED, AppConstants.SORT_MODIFIED, 140, false),
    DIRECTORY_PATH(4, AppConstants.COL_DIRECTORY_PATH, AppConstants.SORT_PATH, 280, true);

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


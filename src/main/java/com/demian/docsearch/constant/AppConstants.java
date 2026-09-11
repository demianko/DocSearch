package com.demian.docsearch.constant;

import java.util.Set;

public final class AppConstants {
    public static final String COL_NAME = "Name";
    public static final String COL_YEAR = "Year";
    public static final String COL_DATE_MODIFIED = "Date Modified";
    public static final String COL_DIRECTORY_PATH = "Directory Path";
    public static final String[] TABLE_COLUMN_NAMES = new String[]{"Name", "Year", "Date Modified", "Directory Path"};
    public static final String SORT_NAME = "name";
    public static final String SORT_YEAR = "year";
    public static final String SORT_PUBLISHED = "published";
    public static final String SORT_MODIFIED = "modified";
    public static final String SORT_DATE = "date";
    public static final String SORT_PATH = "path";
    public static final String SORT_DIRECTORY = "directory";
    public static final String SORT_SIZE = "size";
    public static final Set<String> SYSTEM_IGNORED_DIRS = Set.of("$RECYCLE.BIN", "SYSTEM VOLUME INFORMATION");
    public static final String CONFIG_DIR_NAME = ".filesearch";
    public static final String CONFIG_FILE_NAME = "config.json";
    public static final String LEGACY_CONFIG_FILE_NAME = "config";
    public static final String APP_TITLE = "DocSearch Pro \u2014 Fast Search & Sort";
    public static final String TYPE_FOLDER = "Folder";
    public static final String DUMMY_TREE_NODE = "__dummy__";

    private AppConstants() {
    }
}


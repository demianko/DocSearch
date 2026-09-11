package com.demian.docsearch.ui;

import com.demian.docsearch.constant.AppConstants;
import com.demian.docsearch.model.FileItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class ResultsTableModel
extends AbstractTableModel {
    private final List<FileItem> items = new ArrayList<FileItem>();

    public void setItems(List<FileItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        this.fireTableDataChanged();
    }

    public void addItem(FileItem item) {
        if (item != null) {
            int index = this.items.size();
            this.items.add(item);
            this.fireTableRowsInserted(index, index);
        }
    }

    public FileItem getItem(int row) {
        if (row >= 0 && row < this.items.size()) {
            return this.items.get(row);
        }
        return null;
    }

    public void updateItem(int row, FileItem newItem) {
        if (row >= 0 && row < this.items.size()) {
            this.items.set(row, newItem);
            this.fireTableRowsUpdated(row, row);
        }
    }

    public List<FileItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    @Override
    public int getRowCount() {
        return this.items.size();
    }

    @Override
    public int getColumnCount() {
        return AppConstants.TABLE_COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return AppConstants.TABLE_COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FileItem item = this.getItem(rowIndex);
        if (item == null) {
            return "";
        }
        return switch (columnIndex) {
            case 0 -> item.nameDisplay();
            case 1 -> item.yearDisplay();
            case 2 -> item.dateModifiedStr();
            case 3 -> item.parentStr();
            default -> "";
        };
    }
}


package com.demian.docsearch.ui;

import com.demian.docsearch.constant.AppConstants;
import com.demian.docsearch.constant.ResultColumn;
import com.demian.docsearch.model.FileItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsTableModelTest {

    @Test
    void testColumnsAndHeaders() {
        ResultsTableModel model = new ResultsTableModel();
        assertThat(model.getColumnCount()).isEqualTo(5);
        assertThat(model.getColumnName(0)).isEqualTo(AppConstants.COL_INDEX);
        assertThat(model.getColumnName(1)).isEqualTo(AppConstants.COL_NAME);
        assertThat(model.getColumnName(2)).isEqualTo(AppConstants.COL_YEAR);
        assertThat(model.getColumnName(3)).isEqualTo(AppConstants.COL_DATE_MODIFIED);
        assertThat(model.getColumnName(4)).isEqualTo(AppConstants.COL_DIRECTORY_PATH);

        assertThat(model.getColumnClass(0)).isEqualTo(Integer.class);
        assertThat(model.getColumnClass(1)).isEqualTo(String.class);
    }

    @Test
    void testIndexNumbering() {
        ResultsTableModel model = new ResultsTableModel();
        FileItem item1 = new FileItem(Path.of("C:/docs/alpha.pdf"), 2021, "Packt", 1000L, false, 100L);
        FileItem item2 = new FileItem(Path.of("C:/docs/beta.pdf"), 2022, "O'Reilly", 2000L, false, 200L);
        FileItem item3 = new FileItem(Path.of("C:/docs/gamma.pdf"), 2023, "Manning", 3000L, false, 300L);

        model.setItems(List.of(item1, item2, item3));
        assertThat(model.getRowCount()).isEqualTo(3);

        // Index column returns 1-based sequential integers
        assertThat(model.getValueAt(0, ResultColumn.INDEX.modelIndex())).isEqualTo(1);
        assertThat(model.getValueAt(1, ResultColumn.INDEX.modelIndex())).isEqualTo(2);
        assertThat(model.getValueAt(2, ResultColumn.INDEX.modelIndex())).isEqualTo(3);

        // Other columns
        assertThat(model.getValueAt(0, ResultColumn.NAME.modelIndex())).isEqualTo("alpha.pdf");
        assertThat(model.getValueAt(1, ResultColumn.NAME.modelIndex())).isEqualTo("beta.pdf");
    }
}

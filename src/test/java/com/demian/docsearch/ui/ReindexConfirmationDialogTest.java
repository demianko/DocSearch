package com.demian.docsearch.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;

import static org.assertj.core.api.Assertions.assertThat;

class ReindexConfirmationDialogTest {

    @Test
    void testDialogCreationAndTimerRunning() {
        ReindexConfirmationDialog dialog = new ReindexConfirmationDialog(null, "TestFolder", 3);
        try {
            assertThat(dialog.getTitle()).contains("Reindex Confirmation");
            assertThat(dialog.getAutoCloseTimer()).isNotNull();
            assertThat(dialog.getAutoCloseTimer().isRunning()).isTrue();
            assertThat(dialog.getUserChoice()).isEqualTo(ReindexConfirmationDialog.UserChoice.CONTINUE_SEARCH);
        } finally {
            dialog.dispose();
            assertThat(dialog.getAutoCloseTimer().isRunning()).isFalse();
        }
    }

    @Test
    void testUserClickReindexButton() {
        ReindexConfirmationDialog dialog = new ReindexConfirmationDialog(null, "Documents", 3);
        try {
            JPanel content = (JPanel) dialog.getContentPane();
            JButton reindexBtn = findButton(content, "Reindex");
            assertThat(reindexBtn).isNotNull();
            reindexBtn.doClick();

            assertThat(dialog.getUserChoice()).isEqualTo(ReindexConfirmationDialog.UserChoice.REINDEX);
        } finally {
            dialog.dispose();
        }
    }

    @Test
    void testUserClickCancelButton() {
        ReindexConfirmationDialog dialog = new ReindexConfirmationDialog(null, "Documents", 3);
        try {
            JPanel content = (JPanel) dialog.getContentPane();
            JButton cancelBtn = findButton(content, "Cancel");
            assertThat(cancelBtn).isNotNull();
            cancelBtn.doClick();

            assertThat(dialog.getUserChoice()).isEqualTo(ReindexConfirmationDialog.UserChoice.CANCEL);
        } finally {
            dialog.dispose();
        }
    }

    @Test
    void testUserClickContinueButton() {
        ReindexConfirmationDialog dialog = new ReindexConfirmationDialog(null, "Documents", 3);
        try {
            JPanel content = (JPanel) dialog.getContentPane();
            JButton continueBtn = findButton(content, "Search As-Is");
            assertThat(continueBtn).isNotNull();
            continueBtn.doClick();

            assertThat(dialog.getUserChoice()).isEqualTo(ReindexConfirmationDialog.UserChoice.CONTINUE_SEARCH);
        } finally {
            dialog.dispose();
        }
    }

    private JButton findButton(Component comp, String text) {
        if (comp instanceof JButton btn && btn.getText().contains(text)) {
            return btn;
        }
        if (comp instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}

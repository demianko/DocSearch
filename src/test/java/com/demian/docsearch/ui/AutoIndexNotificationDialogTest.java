package com.demian.docsearch.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;

import static org.assertj.core.api.Assertions.assertThat;

class AutoIndexNotificationDialogTest {

    @Test
    void testDialogCreationAndAutoCloseTimer() {
        AutoIndexNotificationDialog dialog = new AutoIndexNotificationDialog(null, "TestFolder", 3000);
        try {
            assertThat(dialog.getTitle()).isEqualTo("Auto-Indexing Folder");
            assertThat(dialog.getAutoCloseTimer()).isNotNull();
            assertThat(dialog.getAutoCloseTimer().getDelay()).isEqualTo(3000);
            assertThat(dialog.getAutoCloseTimer().isRunning()).isTrue();
        } finally {
            dialog.dispose();
            assertThat(dialog.getAutoCloseTimer().isRunning()).isFalse();
        }
    }

    @Test
    void testDialogManualOkClose() {
        AutoIndexNotificationDialog dialog = new AutoIndexNotificationDialog(null, "Projects", 3000);
        try {
            // Find OK button
            JPanel content = (JPanel) dialog.getContentPane();
            JButton okButton = findButton(content, "OK");
            assertThat(okButton).isNotNull();
            okButton.doClick();

            assertThat(dialog.isDisplayable()).isFalse();
        } finally {
            dialog.dispose();
        }
    }

    private JButton findButton(Component comp, String text) {
        if (comp instanceof JButton btn && text.equalsIgnoreCase(btn.getText())) {
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

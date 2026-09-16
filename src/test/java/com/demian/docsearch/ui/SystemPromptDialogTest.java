package com.demian.docsearch.ui;

import com.demian.docsearch.model.AppConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptDialogTest {

    @Test
    void testDialogInitializationAndReset() {
        SystemPromptDialog dialog = new SystemPromptDialog(null, "Custom prompt text");
        try {
            assertThat(dialog.getTxtPrompt().getRows()).isGreaterThanOrEqualTo(10);
            assertThat(dialog.getTxtPrompt().getText()).isEqualTo("Custom prompt text");
            assertThat(dialog.isSaved()).isFalse();

            // Test Reset
            dialog.getBtnReset().doClick();
            assertThat(dialog.getTxtPrompt().getText()).isEqualTo(AppConfig.DEFAULT_AI_SYSTEM_PROMPT);
        } finally {
            dialog.dispose();
        }
    }

    @Test
    void testDialogSaveAndCancel() {
        SystemPromptDialog dialog = new SystemPromptDialog(null, "Initial prompt");
        try {
            dialog.getTxtPrompt().setText("Updated prompt by user");
            dialog.getBtnSave().doClick();

            assertThat(dialog.isSaved()).isTrue();
            assertThat(dialog.getPrompt()).isEqualTo("Updated prompt by user");
        } finally {
            dialog.dispose();
        }

        SystemPromptDialog dialog2 = new SystemPromptDialog(null, "Initial prompt");
        try {
            dialog2.getTxtPrompt().setText("Ignored changes");
            dialog2.getBtnCancel().doClick();

            assertThat(dialog2.isSaved()).isFalse();
            assertThat(dialog2.getPrompt()).isEqualTo("Initial prompt");
        } finally {
            dialog2.dispose();
        }
    }
}

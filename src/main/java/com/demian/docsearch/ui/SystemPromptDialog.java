package com.demian.docsearch.ui;

import com.demian.docsearch.model.AppConfig;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;

public class SystemPromptDialog extends JDialog {

    private final JTextArea txtPrompt;
    private final JButton btnReset;
    private final JButton btnSave;
    private final JButton btnCancel;

    private boolean saved = false;
    private String promptResult;

    public SystemPromptDialog(Window owner, String initialPrompt) {
        super(owner, "AI System Prompt Configuration", ModalityType.APPLICATION_MODAL);
        this.promptResult = StringUtils.defaultIfBlank(initialPrompt, AppConfig.DEFAULT_AI_SYSTEM_PROMPT);

        this.setLayout(new BorderLayout(10, 10));
        this.setSize(680, 440);
        this.setMinimumSize(new Dimension(520, 340));
        this.setLocationRelativeTo(owner);

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 6, 16));

        JLabel lblTitle = new JLabel("📝 AI System Prompt Configuration");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 15.0f));
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));

        JLabel lblSubtitle = new JLabel(
                "Custom instructions passed to the AI model at runtime to guide file name matching & ranking.");
        lblSubtitle.setFont(lblSubtitle.getFont().deriveFont(Font.PLAIN, 12.0f));
        lblSubtitle.setForeground(Color.GRAY);
        headerPanel.add(lblSubtitle);

        this.add(headerPanel, BorderLayout.NORTH);

        // Center: 12-line height text area inside scroll pane
        this.txtPrompt = new JTextArea(this.promptResult, 12, 50);
        this.txtPrompt.setLineWrap(true);
        this.txtPrompt.setWrapStyleWord(true);
        this.txtPrompt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.txtPrompt.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(this.txtPrompt);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 16, 0, 16),
                BorderFactory.createLineBorder(this.getBackground().darker(), 1, true)));
        this.add(scrollPane, BorderLayout.CENTER);

        // Bottom Button Bar
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 14, 16));

        this.btnReset = new JButton("↺ Reset to Default");
        this.btnReset.setToolTipText("Restore default system prompt instructions");
        this.btnReset.addActionListener(e -> this.txtPrompt.setText(AppConfig.DEFAULT_AI_SYSTEM_PROMPT));
        bottomPanel.add(this.btnReset, BorderLayout.WEST);

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        this.btnCancel = new JButton("Cancel");
        this.btnCancel.setPreferredSize(new Dimension(90, 32));
        this.btnCancel.addActionListener(e -> {
            this.saved = false;
            this.dispose();
        });
        actionButtonsPanel.add(this.btnCancel);

        this.btnSave = new JButton("💾 Save Prompt");
        this.btnSave.setFont(this.btnSave.getFont().deriveFont(Font.BOLD));
        this.btnSave.setPreferredSize(new Dimension(130, 32));
        this.btnSave.addActionListener(e -> {
            this.promptResult = StringUtils.trimToEmpty(this.txtPrompt.getText());
            this.saved = true;
            this.dispose();
        });
        actionButtonsPanel.add(this.btnSave);

        bottomPanel.add(actionButtonsPanel, BorderLayout.EAST);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    public static String showDialog(Component parent, String currentPrompt) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        SystemPromptDialog dialog = new SystemPromptDialog(owner, currentPrompt);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            return dialog.getPrompt();
        }
        return currentPrompt;
    }

    public boolean isSaved() {
        return this.saved;
    }

    public String getPrompt() {
        return this.promptResult;
    }

    public JTextArea getTxtPrompt() {
        return this.txtPrompt;
    }

    public JButton getBtnReset() {
        return this.btnReset;
    }

    public JButton getBtnSave() {
        return this.btnSave;
    }

    public JButton getBtnCancel() {
        return this.btnCancel;
    }
}

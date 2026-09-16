package com.demian.docsearch.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class ReindexConfirmationDialog extends JDialog {

    public enum UserChoice {
        REINDEX,
        CONTINUE_SEARCH,
        CANCEL
    }

    private UserChoice userChoice = UserChoice.CONTINUE_SEARCH;
    private final Timer autoCloseTimer;
    private int secondsRemaining;
    private final JLabel lblCountdown;

    public ReindexConfirmationDialog(Frame owner, String folderName, int autoCloseSeconds) {
        super(owner, "Folder Modified - Reindex Confirmation", true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.secondsRemaining = Math.max(1, autoCloseSeconds);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 14));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Header and description
        JPanel textPanel = new JPanel(new BorderLayout(0, 8));
        JLabel lblHeader = new JLabel("⚠️ Folder Files Modified Since Last Index");
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 14.5f));
        lblHeader.setForeground(new Color(217, 119, 6)); // amber-600
        textPanel.add(lblHeader, BorderLayout.NORTH);

        String safeFolderName = folderName != null ? folderName : "selected directory";
        JLabel lblMessage = new JLabel(String.format(
                "<html>Files in folder <b>'%s'</b> have been modified since they were last indexed.<br>" +
                "Would you like to reindex this folder now before continuing AI search?</html>",
                safeFolderName));
        lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN, 12.5f));
        textPanel.add(lblMessage, BorderLayout.CENTER);

        this.lblCountdown = new JLabel(this.formatCountdownText(this.secondsRemaining));
        this.lblCountdown.setFont(this.lblCountdown.getFont().deriveFont(Font.ITALIC, 11.5f));
        this.lblCountdown.setForeground(new Color(100, 116, 139));
        textPanel.add(this.lblCountdown, BorderLayout.SOUTH);

        mainPanel.add(textPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(btnCancel.getFont().deriveFont(Font.PLAIN, 12.0f));
        btnCancel.setPreferredSize(new Dimension(85, 30));
        btnCancel.addActionListener(e -> {
            this.userChoice = UserChoice.CANCEL;
            this.dispose();
        });
        btnPanel.add(btnCancel);

        JButton btnContinue = new JButton("Search As-Is");
        btnContinue.setFont(btnContinue.getFont().deriveFont(Font.PLAIN, 12.0f));
        btnContinue.setPreferredSize(new Dimension(115, 30));
        btnContinue.addActionListener(e -> {
            this.userChoice = UserChoice.CONTINUE_SEARCH;
            this.dispose();
        });
        btnPanel.add(btnContinue);

        JButton btnReindex = new JButton("⚡ Reindex & Search");
        btnReindex.setFont(btnReindex.getFont().deriveFont(Font.BOLD, 12.0f));
        btnReindex.setPreferredSize(new Dimension(155, 30));
        btnReindex.addActionListener(e -> {
            this.userChoice = UserChoice.REINDEX;
            this.dispose();
        });
        btnPanel.add(btnReindex);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
        this.pack();
        this.setMinimumSize(new Dimension(460, 185));
        this.setLocationRelativeTo(owner);

        // Timer for auto-dismiss
        this.autoCloseTimer = new Timer(1000, e -> {
            this.secondsRemaining--;
            if (this.secondsRemaining <= 0) {
                this.userChoice = UserChoice.CONTINUE_SEARCH;
                this.dispose();
            } else {
                this.lblCountdown.setText(this.formatCountdownText(this.secondsRemaining));
            }
        });
        this.autoCloseTimer.setRepeats(true);
        this.autoCloseTimer.start();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (autoCloseTimer.isRunning()) {
                    autoCloseTimer.stop();
                }
            }
        });
    }

    private String formatCountdownText(int seconds) {
        return String.format("⏱️ Auto-dismissing in %ds... (search will continue without reindexing)", seconds);
    }

    @Override
    public void dispose() {
        if (this.autoCloseTimer != null && this.autoCloseTimer.isRunning()) {
            this.autoCloseTimer.stop();
        }
        super.dispose();
    }

    public UserChoice getUserChoice() {
        return this.userChoice;
    }

    public Timer getAutoCloseTimer() {
        return this.autoCloseTimer;
    }

    public static UserChoice showDialog(Frame owner, String folderName) {
        return showDialog(owner, folderName, 3);
    }

    public static UserChoice showDialog(Frame owner, String folderName, int autoCloseSeconds) {
        ReindexConfirmationDialog dialog = new ReindexConfirmationDialog(owner, folderName, autoCloseSeconds);
        dialog.setVisible(true);
        return dialog.getUserChoice();
    }
}

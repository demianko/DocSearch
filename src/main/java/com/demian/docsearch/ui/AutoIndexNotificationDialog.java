package com.demian.docsearch.ui;

import java.awt.BorderLayout;
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

public class AutoIndexNotificationDialog extends JDialog {
    private final Timer autoCloseTimer;

    public AutoIndexNotificationDialog(Frame owner, String folderName, int autoCloseMillis) {
        super(owner, "Auto-Indexing Folder", false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        JLabel lblHeader = new JLabel("\u26a1 Indexing Folder for AI Search");
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 14.0f));
        textPanel.add(lblHeader, BorderLayout.NORTH);

        String safeFolderName = folderName != null ? folderName : "selected directory";
        JLabel lblMessage = new JLabel(String.format(
                "<html>No existing index data found for <b>'%s'</b>.<br>" +
                "The folder is being indexed in the background before searching.<br>" +
                "<font color='#888888' size='2'>(Auto-closing in %d seconds)</font></html>",
                safeFolderName, Math.max(1, autoCloseMillis / 1000)));
        lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN, 12.0f));
        textPanel.add(lblMessage, BorderLayout.CENTER);
        mainPanel.add(textPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton btnOk = new JButton("OK");
        btnOk.setPreferredSize(new Dimension(80, 28));
        btnOk.setFont(btnOk.getFont().deriveFont(Font.BOLD));
        btnOk.addActionListener(e -> this.dispose());
        btnPanel.add(btnOk);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
        this.pack();
        this.setMinimumSize(new Dimension(420, 175));
        this.setLocationRelativeTo(owner);

        int delay = autoCloseMillis > 0 ? autoCloseMillis : 3000;
        this.autoCloseTimer = new Timer(delay, e -> {
            if (AutoIndexNotificationDialog.this.isDisplayable()) {
                AutoIndexNotificationDialog.this.dispose();
            }
        });
        this.autoCloseTimer.setRepeats(false);
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

    @Override
    public void dispose() {
        if (this.autoCloseTimer != null && this.autoCloseTimer.isRunning()) {
            this.autoCloseTimer.stop();
        }
        super.dispose();
    }

    public static AutoIndexNotificationDialog showDialog(Frame owner, String folderName) {
        return showDialog(owner, folderName, 3000);
    }

    public static AutoIndexNotificationDialog showDialog(Frame owner, String folderName, int autoCloseMillis) {
        AutoIndexNotificationDialog dialog = new AutoIndexNotificationDialog(owner, folderName, autoCloseMillis);
        dialog.setVisible(true);
        return dialog;
    }

    public Timer getAutoCloseTimer() {
        return this.autoCloseTimer;
    }
}

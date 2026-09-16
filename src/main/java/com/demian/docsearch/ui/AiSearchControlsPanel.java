package com.demian.docsearch.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.commons.lang3.StringUtils;

public class AiSearchControlsPanel extends JPanel {
    private final JTextField txtFolder;
    private final JButton btnIndex;
    private final JTextField txtPatterns;
    private final JTextField txtExtensions;
    private final JButton btnSearch;
    private final JButton btnStop;
    private final JLabel lblStatusWarning;

    private boolean isAiConfigured = true;
    private boolean isIndexed = false;

    public AiSearchControlsPanel() {
        this("");
    }

    public AiSearchControlsPanel(String initialDirectory) {
        super(new GridBagLayout());
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(this.getBackground().darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Directory and Index button (replaces Browse)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblFolder = new JLabel("\ud83d\udcc1 Directory:");
        lblFolder.setFont(lblFolder.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblFolder, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtFolder = new JTextField(initialDirectory != null ? initialDirectory : "");
        this.txtFolder.setPreferredSize(new Dimension(500, 32));
        this.txtFolder.putClientProperty("JTextField.placeholderText", "Selected folder from left navigation panel...");
        this.add(this.txtFolder, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnIndex = new JButton("\u26a1 Index Folder");
        this.btnIndex.setPreferredSize(new Dimension(140, 32));
        this.btnIndex.setFont(this.btnIndex.getFont().deriveFont(Font.BOLD));
        this.btnIndex.setToolTipText("Scan and index files in this directory and subdirectories into SQLite");
        this.add(this.btnIndex, gbc);

        // Row 1: Natural Language Patterns prompt
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel lblPattern = new JLabel("\ud83e\udd16 AI Prompt:");
        lblPattern.setFont(lblPattern.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblPattern, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtPatterns = new JTextField();
        this.txtPatterns.setPreferredSize(new Dimension(500, 32));
        this.txtPatterns.putClientProperty("JTextField.placeholderText",
                "Natural language search: e.g. '2023 invoices', 'financial reports', 'machine learning design'...");
        this.add(this.txtPatterns, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnSearch = new JButton("\u2728 AI Search (Enter)");
        this.btnSearch.setPreferredSize(new Dimension(140, 32));
        this.btnSearch.setFont(this.btnSearch.getFont().deriveFont(Font.BOLD));
        this.add(this.btnSearch, gbc);

        // Row 2: Extensions & Stop button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel lblExt = new JLabel("\ud83d\udcc4 Extensions:");
        lblExt.setFont(lblExt.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblExt, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtExtensions = new JTextField();
        this.txtExtensions.setPreferredSize(new Dimension(500, 32));
        this.txtExtensions.putClientProperty("JTextField.placeholderText", "Optional extension filter (e.g. pdf, docx, txt)");
        this.add(this.txtExtensions, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnStop = new JButton("\ud83d\uded1 Stop (Esc)");
        this.btnStop.setPreferredSize(new Dimension(140, 32));
        this.btnStop.setEnabled(false);
        this.add(this.btnStop, gbc);

        // Row 3: Status Warning Label (Visible when AI is not configured)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        this.lblStatusWarning = new JLabel("\u26a0\ufe0f AI Search controls are grayed out: Please configure your OpenAI compliant URL & key in the 'AI Settings' tab.");
        this.lblStatusWarning.setFont(this.lblStatusWarning.getFont().deriveFont(Font.PLAIN, 12.0f));
        this.lblStatusWarning.setForeground(new Color(230, 81, 0));
        this.lblStatusWarning.setVisible(false);
        this.add(this.lblStatusWarning, gbc);
    }

    public void setAiConfigured(boolean configured) {
        this.isAiConfigured = configured;
        this.txtFolder.setEnabled(configured);
        this.btnIndex.setEnabled(configured);
        this.txtPatterns.setEnabled(configured);
        this.txtExtensions.setEnabled(configured);
        this.btnSearch.setEnabled(configured);
        this.btnStop.setEnabled(false);
        this.lblStatusWarning.setVisible(!configured);
    }

    public boolean isAiConfigured() {
        return this.isAiConfigured;
    }

    public void setIndexed(boolean indexed) {
        this.isIndexed = indexed;
        if (indexed) {
            this.btnIndex.setText("\u26a1 Reindex Folder");
            this.btnIndex.setToolTipText("Re-scan and refresh SQLite index for this folder");
        } else {
            this.btnIndex.setText("\u26a1 Index Folder");
            this.btnIndex.setToolTipText("Scan and index files in this directory and subdirectories into SQLite");
        }
    }

    public boolean isIndexed() {
        return this.isIndexed;
    }

    public String getDirectory() {
        return StringUtils.trimToEmpty(this.txtFolder.getText());
    }

    public void setDirectory(String directory) {
        this.txtFolder.setText(directory != null ? directory : "");
    }

    public String getPattern() {
        return StringUtils.trimToEmpty(this.txtPatterns.getText());
    }

    public void setPattern(String pattern) {
        this.txtPatterns.setText(pattern != null ? pattern : "");
    }

    public String getExtension() {
        return StringUtils.trimToEmpty(this.txtExtensions.getText());
    }

    public void setExtension(String extension) {
        this.txtExtensions.setText(extension != null ? extension : "");
    }

    public void setSearchEnabled(boolean enabled) {
        if (!this.isAiConfigured) {
            this.btnSearch.setEnabled(false);
            return;
        }
        this.btnSearch.setEnabled(enabled);
    }

    public void setStopEnabled(boolean enabled) {
        this.btnStop.setEnabled(enabled);
    }

    public void setIndexEnabled(boolean enabled) {
        if (!this.isAiConfigured) {
            this.btnIndex.setEnabled(false);
            return;
        }
        this.btnIndex.setEnabled(enabled);
    }

    public void setOnSearchAction(Runnable action) {
        this.btnSearch.addActionListener(e -> {
            if (this.isAiConfigured) action.run();
        });
        this.txtFolder.addActionListener(e -> {
            if (this.isAiConfigured) action.run();
        });
        this.txtPatterns.addActionListener(e -> {
            if (this.isAiConfigured) action.run();
        });
        this.txtExtensions.addActionListener(e -> {
            if (this.isAiConfigured) action.run();
        });
    }

    public void setOnStopAction(Runnable action) {
        this.btnStop.addActionListener(e -> action.run());
    }

    public void setOnIndexAction(Runnable action) {
        this.btnIndex.addActionListener(e -> {
            if (this.isAiConfigured) action.run();
        });
    }

    public JTextField getTxtFolder() {
        return this.txtFolder;
    }

    public JTextField getTxtPatterns() {
        return this.txtPatterns;
    }

    public JTextField getTxtExtensions() {
        return this.txtExtensions;
    }

    public JButton getBtnIndex() {
        return this.btnIndex;
    }

    public JButton getBtnSearch() {
        return this.btnSearch;
    }

    public JButton getBtnStop() {
        return this.btnStop;
    }

    public JLabel getLblStatusWarning() {
        return this.lblStatusWarning;
    }
}

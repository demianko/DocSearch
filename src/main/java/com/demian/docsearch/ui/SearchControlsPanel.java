package com.demian.docsearch.ui;

import com.demian.docsearch.model.AppConfig;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.commons.lang3.StringUtils;

public class SearchControlsPanel extends JPanel {
    private final JTextField txtFolder;
    private final JTextField txtPatterns;
    private final JTextField txtExtensions;
    private final JTextField txtLimit;
    private final JButton btnSearch;
    private final JButton btnStop;
    private final JButton btnBrowse;

    public SearchControlsPanel(AppConfig config) {
        super(new GridBagLayout());
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(this.getBackground().darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblFolder = new JLabel("\ud83d\udcc1 Directory:");
        lblFolder.setFont(lblFolder.getFont().deriveFont(1, 13.0f));
        this.add(lblFolder, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtFolder = new JTextField(config != null ? config.getDirectory() : "");
        this.txtFolder.setPreferredSize(new Dimension(500, 32));
        this.add(this.txtFolder, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnBrowse = new JButton("Browse...");
        this.btnBrowse.setPreferredSize(new Dimension(95, 32));
        this.add(this.btnBrowse, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel lblPattern = new JLabel("\ud83d\udd0d Patterns:");
        lblPattern.setFont(lblPattern.getFont().deriveFont(1, 13.0f));
        this.add(lblPattern, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtPatterns = new JTextField(config != null ? config.getPattern() : "");
        this.txtPatterns.setPreferredSize(new Dimension(500, 32));
        this.add(this.txtPatterns, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnSearch = new JButton("\u26a1 Search (Enter)");
        this.btnSearch.setPreferredSize(new Dimension(140, 32));
        this.btnSearch.setFont(this.btnSearch.getFont().deriveFont(1));
        this.add(this.btnSearch, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel lblExt = new JLabel("\ud83d\udcc4 Extensions:");
        lblExt.setFont(lblExt.getFont().deriveFont(1, 13.0f));
        this.add(lblExt, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtExtensions = new JTextField(config != null ? config.getExtension() : "");
        this.txtExtensions.setPreferredSize(new Dimension(500, 32));
        this.add(this.txtExtensions, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnStop = new JButton("\ud83d\uded1 Stop (Esc)");
        this.btnStop.setPreferredSize(new Dimension(140, 32));
        this.btnStop.setEnabled(false);
        this.add(this.btnStop, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        optionsPanel.add(new JLabel("Limit:"));
        this.txtLimit = new JTextField(config != null && config.getLimit() > 0 ? String.valueOf(config.getLimit()) : "", 5);
        optionsPanel.add(this.txtLimit);
        this.add(optionsPanel, gbc);
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

    public int getLimit() {
        try {
            String limStr = StringUtils.trimToEmpty(this.txtLimit.getText());
            if (StringUtils.isNotEmpty(limStr)) {
                return Integer.parseInt(limStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid limit format: " + e.getMessage());
        }
        return 0;
    }

    public void setLimit(int limit) {
        this.txtLimit.setText(limit > 0 ? String.valueOf(limit) : "");
    }

    public void setSearchEnabled(boolean enabled) {
        this.btnSearch.setEnabled(enabled);
    }

    public void setStopEnabled(boolean enabled) {
        this.btnStop.setEnabled(enabled);
    }

    public void setOnSearchAction(Runnable action) {
        this.btnSearch.addActionListener(e -> action.run());
        this.txtFolder.addActionListener(e -> action.run());
        this.txtPatterns.addActionListener(e -> action.run());
        this.txtExtensions.addActionListener(e -> action.run());
    }

    public void setOnStopAction(Runnable action) {
        this.btnStop.addActionListener(e -> action.run());
    }

    public void setOnBrowseAction(Runnable action) {
        this.btnBrowse.addActionListener(e -> action.run());
    }
}

package com.demian.docsearch.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.StringUtils;

public class SearchFilterBar extends JPanel {
    private final JProgressBar progressBar;
    private final JTextField txtFilter;

    public SearchFilterBar() {
        super(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 8, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;

        this.progressBar = new JProgressBar(0, 100);
        this.progressBar.setValue(0);
        this.progressBar.setStringPainted(false);
        this.progressBar.setPreferredSize(new Dimension(200, 10));
        this.progressBar.setForeground(new Color(31, 106, 165));
        this.add(this.progressBar, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 2, 0, 8);
        JLabel lblFilter = new JLabel("🎯 Filter Results:");
        lblFilter.setFont(lblFilter.getFont().deriveFont(1, 12.0f));
        this.add(lblFilter, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 2);
        this.txtFilter = new JTextField();
        this.txtFilter.setPreferredSize(new Dimension(200, 30));
        this.txtFilter.putClientProperty("JTextField.placeholderText",
                "Type to filter displayed items instantly (supports '|', 'NOT', '*', '?')...");
        this.txtFilter.putClientProperty("JTextField.showClearButton", true);
        this.add(this.txtFilter, gbc);
    }

    public String getFilterText() {
        return StringUtils.trimToEmpty(this.txtFilter.getText());
    }

    public void setFilterText(String text) {
        this.txtFilter.setText(text != null ? text : "");
    }

    public void setOnFilterChanged(Runnable onFilterChanged) {
        if (onFilterChanged == null) return;
        this.txtFilter.addActionListener(e -> onFilterChanged.run());
        this.txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onFilterChanged.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onFilterChanged.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onFilterChanged.run();
            }
        });
    }

    public void setIndeterminate(boolean indeterminate) {
        if (SwingUtilities.isEventDispatchThread()) {
            this.progressBar.setIndeterminate(indeterminate);
        } else {
            SwingUtilities.invokeLater(() -> this.progressBar.setIndeterminate(indeterminate));
        }
    }

    public void resetProgress() {
        this.progressBar.setValue(0);
        if (this.progressBar.isIndeterminate()) {
            if (SwingUtilities.isEventDispatchThread()) {
                this.progressBar.setIndeterminate(false);
            } else {
                SwingUtilities.invokeLater(() -> this.progressBar.setIndeterminate(false));
            }
        }
    }

    public void setProgress(int current, int total) {
        this.progressBar.setMaximum(total);
        this.progressBar.setValue(current);
        if (this.progressBar.isIndeterminate()) {
            if (SwingUtilities.isEventDispatchThread()) {
                this.progressBar.setIndeterminate(false);
            } else {
                SwingUtilities.invokeLater(() -> this.progressBar.setIndeterminate(false));
            }
        }
    }

    public void setProgressValue(int val) {
        this.progressBar.setValue(val);
    }

    public int getProgressMaximum() {
        return this.progressBar.getMaximum();
    }
}

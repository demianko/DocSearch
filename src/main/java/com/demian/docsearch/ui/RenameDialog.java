package com.demian.docsearch.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;

public class RenameDialog
extends JDialog {
    private static final String INVALID_CHARS = "\\/:*?\"<>|";
    private final Path oldPath;
    private final boolean isDirectory;
    private final Consumer<Path> onSuccess;
    private final JTextField nameField;

    public RenameDialog(Frame owner, Path oldPath, boolean isDirectory, Consumer<Path> onSuccess) {
        super(owner, "Rename " + (isDirectory ? "Folder" : "File"), true);
        this.oldPath = oldPath;
        this.isDirectory = isDirectory;
        this.onSuccess = onSuccess;
        String oldName = oldPath.getFileName() != null ? oldPath.getFileName().toString() : oldPath.toString();
        JPanel contentPane = new JPanel(new BorderLayout(0, 16));
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel lblPrompt = new JLabel("Enter new name for " + (isDirectory ? "folder" : "file") + ":");
        lblPrompt.setFont(lblPrompt.getFont().deriveFont(1, 13.0f));
        this.nameField = new JTextField(oldName);
        this.nameField.setPreferredSize(new Dimension(420, 34));
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, 1));
        centerPanel.add(lblPrompt);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(this.nameField);
        contentPane.add((Component)centerPanel, "Center");
        JPanel buttonPanel = new JPanel(new FlowLayout(2, 10, 0));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(90, 32));
        btnCancel.addActionListener(e -> this.dispose());
        JButton btnRename = new JButton("Rename");
        btnRename.setPreferredSize(new Dimension(100, 32));
        btnRename.setFont(btnRename.getFont().deriveFont(1));
        btnRename.putClientProperty("JButton.buttonType", "roundRect");
        btnRename.addActionListener(e -> this.doRename());
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnRename);
        contentPane.add((Component)buttonPanel, "South");
        this.setContentPane(contentPane);
        this.getRootPane().setDefaultButton(btnRename);
        this.getRootPane().registerKeyboardAction(e -> this.dispose(), KeyStroke.getKeyStroke(27, 0), 2);
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> {
            this.nameField.requestFocusInWindow();
            if (!isDirectory && oldName.contains(".")) {
                int extIdx = oldName.lastIndexOf(46);
                if (extIdx > 0) {
                    this.nameField.select(0, extIdx);
                    this.nameField.setCaretPosition(extIdx);
                } else {
                    this.nameField.selectAll();
                }
            } else {
                this.nameField.selectAll();
            }
        });
    }

    private void doRename() {
        String oldName;
        String newName = StringUtils.trimToEmpty(this.nameField.getText());
        String string = oldName = this.oldPath.getFileName() != null ? this.oldPath.getFileName().toString() : this.oldPath.toString();
        if (StringUtils.isEmpty(newName) || newName.equals(oldName)) {
            this.dispose();
            return;
        }
        if (!StringUtils.containsNone((CharSequence)newName, INVALID_CHARS)) {
            JOptionPane.showMessageDialog(this, "A name cannot contain any of the following characters:\n\\/:*?\"<>|", "Invalid Name", 0);
            this.nameField.requestFocusInWindow();
            return;
        }
        Path newPath = this.oldPath.resolveSibling(newName);
        if (Files.exists(newPath, new LinkOption[0])) {
            JOptionPane.showMessageDialog(this, "An item with the name '" + newName + "' already exists in this folder.", "Already Exists", 0);
            this.nameField.requestFocusInWindow();
            return;
        }
        try {
            File oldFile = this.oldPath.toFile();
            File newFile = newPath.toFile();
            boolean renamed = oldFile.renameTo(newFile);
            if (!renamed) {
                Files.move(this.oldPath, newPath, new CopyOption[0]);
            }
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not rename:\n" + e.getMessage(), "Rename Error", 0);
            this.nameField.requestFocusInWindow();
            return;
        }
        this.dispose();
        if (this.onSuccess != null) {
            this.onSuccess.accept(newPath);
        }
    }
}


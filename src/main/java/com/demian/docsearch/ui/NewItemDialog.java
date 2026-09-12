package com.demian.docsearch.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
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

public class NewItemDialog extends JDialog {
    public static final String INVALID_CHARS = "\\/:*?\"<>|";
    private final Path parentFolder;
    private final boolean isDirectory;
    private final Consumer<Path> onSuccess;
    private final JTextField nameField;

    public NewItemDialog(Frame owner, Path parentFolder, boolean isDirectory, Consumer<Path> onSuccess) {
        super(owner, "New " + (isDirectory ? "Folder" : "File"), true);
        this.parentFolder = parentFolder;
        this.isDirectory = isDirectory;
        this.onSuccess = onSuccess;

        String defaultName = suggestDefaultName(parentFolder, isDirectory);

        JPanel contentPane = new JPanel(new BorderLayout(0, 16));
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel lblPrompt = new JLabel("Enter new " + (isDirectory ? "folder" : "file") + " name:");
        lblPrompt.setFont(lblPrompt.getFont().deriveFont(1, 13.0f));

        this.nameField = new JTextField(defaultName);
        this.nameField.setPreferredSize(new Dimension(420, 34));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(lblPrompt);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(this.nameField);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(90, 32));
        btnCancel.addActionListener(e -> this.dispose());

        JButton btnCreate = new JButton("Create");
        btnCreate.setPreferredSize(new Dimension(100, 32));
        btnCreate.setFont(btnCreate.getFont().deriveFont(1));
        btnCreate.putClientProperty("JButton.buttonType", "roundRect");
        btnCreate.addActionListener(e -> this.doCreate());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnCreate);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        this.setContentPane(contentPane);
        this.getRootPane().setDefaultButton(btnCreate);
        this.getRootPane().registerKeyboardAction(e -> this.dispose(), KeyStroke.getKeyStroke(27, 0), 2);
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(owner);

        SwingUtilities.invokeLater(() -> {
            this.nameField.requestFocusInWindow();
            if (!isDirectory && defaultName.contains(".")) {
                int extIdx = defaultName.lastIndexOf('.');
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

    public static boolean isValidName(String name) {
        if (StringUtils.isBlank(name)) return false;
        return StringUtils.containsNone(name, INVALID_CHARS);
    }

    public static String suggestDefaultName(Path parentFolder, boolean isDirectory) {
        String baseName = isDirectory ? "New Folder" : "New File";
        String ext = isDirectory ? "" : ".txt";
        String candidate = baseName + ext;
        if (parentFolder == null || !Files.exists(parentFolder, new LinkOption[0])) {
            return candidate;
        }
        if (!Files.exists(parentFolder.resolve(candidate), new LinkOption[0])) {
            return candidate;
        }
        int counter = 2;
        while (true) {
            String next = baseName + " (" + counter + ")" + ext;
            if (!Files.exists(parentFolder.resolve(next), new LinkOption[0])) {
                return next;
            }
            counter++;
        }
    }

    private void doCreate() {
        String name = StringUtils.trimToEmpty(this.nameField.getText());
        if (StringUtils.isEmpty(name)) {
            JOptionPane.showMessageDialog(this, "Please enter a " + (this.isDirectory ? "folder" : "file") + " name.",
                    "Missing Name", JOptionPane.WARNING_MESSAGE);
            this.nameField.requestFocusInWindow();
            return;
        }

        if (!isValidName(name)) {
            JOptionPane.showMessageDialog(this,
                    "A name cannot contain any of the following characters:\n\\/:*?\"<>|",
                    "Invalid Name", JOptionPane.ERROR_MESSAGE);
            this.nameField.requestFocusInWindow();
            return;
        }

        Path newPath = this.parentFolder.resolve(name);
        if (Files.exists(newPath, new LinkOption[0])) {
            String itemType = this.isDirectory ? "folder" : "file";
            JOptionPane.showMessageDialog(
                    this,
                    "A " + itemType + " with the name '" + name + "' already exists in this folder.\n"
                            + "Please select a new " + itemType + " name.",
                    "Name Conflict",
                    JOptionPane.WARNING_MESSAGE
            );
            this.nameField.selectAll();
            this.nameField.requestFocusInWindow();
            return;
        }

        try {
            if (this.isDirectory) Files.createDirectory(newPath);
            else Files.createFile(newPath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not create " + (this.isDirectory ? "folder" : "file") + ":\n" + e.getMessage(),
                    "Creation Error", JOptionPane.ERROR_MESSAGE);
            this.nameField.requestFocusInWindow();
            return;
        }

        this.dispose();
        if (this.onSuccess != null) {
            this.onSuccess.accept(newPath);
        }
    }
}

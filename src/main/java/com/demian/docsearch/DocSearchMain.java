package com.demian.docsearch;

import com.demian.docsearch.ui.DocSearchApp;
import com.formdev.flatlaf.FlatDarkLaf;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DocSearchMain {
    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        }
        catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf look and feel: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
            DocSearchApp app = new DocSearchApp();
            if (args.length > 0) {
                Path targetDir = Paths.get(args[0]).toAbsolutePath().normalize();
                if (Files.exists(targetDir) && Files.isDirectory(targetDir)) {
                    app.onNavFolderSelected(targetDir);
                }
            }
            app.setVisible(true);
        });
    }
}


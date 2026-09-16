package com.demian.docsearch.ui;

import com.demian.docsearch.ai.OpenAiClient;
import com.demian.docsearch.config.ConfigManager;
import com.demian.docsearch.model.AppConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.StringUtils;

public class AiConfigPanel extends JPanel {
    private final AppConfig config;
    private final ConfigManager configManager;
    private final OpenAiClient openAiClient;

    private final JTextField txtBaseUrl;
    private final JPasswordField txtApiKey;
    private final JToggleButton btnToggleKeyVisibility;
    private final JComboBox<String> comboModel;
    private final JButton btnFetchModels;
    private final JTextField txtTemperature;
    private final JTextField txtTimeout;
    private final JTextField txtParallel;
    private final JButton btnEditPrompt;
    private final JButton btnResetPrompt;
    private final JLabel lblPromptSummary;
    private final JButton btnTestConnection;
    private final JButton btnSave;
    private final JLabel lblStatus;

    private String currentSystemPrompt;
    private Consumer<AppConfig> onConfigSaved;

    public AiConfigPanel(AppConfig config, ConfigManager configManager) {
        this(config, configManager, new OpenAiClient());
    }

    public AiConfigPanel(AppConfig config, ConfigManager configManager, OpenAiClient openAiClient) {
        super(new GridBagLayout());
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager must not be null");
        this.openAiClient = Objects.requireNonNull(openAiClient, "openAiClient must not be null");
        this.currentSystemPrompt = StringUtils.defaultIfBlank(config.getAiSystemPrompt(), AppConfig.DEFAULT_AI_SYSTEM_PROMPT);

        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(this.getBackground().darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: OpenAI Compliant URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblUrl = new JLabel("🌐 OpenAI Compliant URL:");
        lblUrl.setFont(lblUrl.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblUrl, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        this.txtBaseUrl = new JTextField(config.getAiBaseUrl());
        this.txtBaseUrl.setPreferredSize(new Dimension(500, 32));
        this.txtBaseUrl.putClientProperty("JTextField.placeholderText", "e.g. https://api.openai.com/v1 or http://localhost:11434/v1");
        this.add(this.txtBaseUrl, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnTestConnection = new JButton("🔌 Test Connection");
        this.btnTestConnection.setPreferredSize(new Dimension(145, 32));
        this.btnTestConnection.setFont(this.btnTestConnection.getFont().deriveFont(Font.BOLD));
        this.add(this.btnTestConnection, gbc);

        // Row 1: API Key
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel lblKey = new JLabel("🔑 API Key:");
        lblKey.setFont(lblKey.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblKey, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        this.txtApiKey = new JPasswordField(config.getAiApiKey());
        this.txtApiKey.setPreferredSize(new Dimension(420, 32));
        this.txtApiKey.putClientProperty("JTextField.placeholderText", "sk-... or custom secret token");
        this.add(this.txtApiKey, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnToggleKeyVisibility = new JToggleButton("👁 Show");
        this.btnToggleKeyVisibility.setPreferredSize(new Dimension(75, 32));
        this.btnToggleKeyVisibility.addActionListener(e -> {
            if (this.btnToggleKeyVisibility.isSelected()) {
                this.txtApiKey.setEchoChar((char) 0);
                this.btnToggleKeyVisibility.setText("🙈 Hide");
            } else {
                this.txtApiKey.setEchoChar('•');
                this.btnToggleKeyVisibility.setText("👁 Show");
            }
        });
        this.add(this.btnToggleKeyVisibility, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnSave = new JButton("💾 Save Config");
        this.btnSave.setPreferredSize(new Dimension(145, 32));
        this.btnSave.setFont(this.btnSave.getFont().deriveFont(Font.BOLD));
        this.add(this.btnSave, gbc);

        // Row 2: Model Name, Fetch Models button, Temperature & Timeout
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel lblModel = new JLabel("🧠 Model Name:");
        lblModel.setFont(lblModel.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblModel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        this.comboModel = new JComboBox<>();
        this.comboModel.setEditable(true);
        this.comboModel.setPreferredSize(new Dimension(280, 32));
        if (config.isAiConfigured() && StringUtils.isNotBlank(config.getAiModel())) {
            this.comboModel.addItem(config.getAiModel());
            this.comboModel.setSelectedItem(config.getAiModel());
        }
        this.add(this.comboModel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        this.btnFetchModels = new JButton("🔄 Fetch Models");
        this.btnFetchModels.setPreferredSize(new Dimension(125, 32));
        this.btnFetchModels.setToolTipText("Query endpoint to populate available models list");
        this.add(this.btnFetchModels, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        JPanel tempTimeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        tempTimeoutPanel.add(new JLabel("🌡️ Temp:"));
        this.txtTemperature = new JTextField(String.valueOf(config.getAiTemperature()), 3);
        this.txtTemperature.setPreferredSize(new Dimension(38, 30));
        this.txtTemperature.setToolTipText("Sampling temperature (0.0 = deterministic, 2.0 = creative, default 0.1)");
        tempTimeoutPanel.add(this.txtTemperature);
        tempTimeoutPanel.add(new JLabel("Timeout:"));
        this.txtTimeout = new JTextField(String.valueOf(config.getAiTimeoutSeconds()), 3);
        this.txtTimeout.setPreferredSize(new Dimension(38, 30));
        tempTimeoutPanel.add(this.txtTimeout);
        tempTimeoutPanel.add(new JLabel("s"));
        this.add(tempTimeoutPanel, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        JPanel parallelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        parallelPanel.add(new JLabel("⚡ Parallel:"));
        this.txtParallel = new JTextField(String.valueOf(config.getAiParallelRequests()), 2);
        this.txtParallel.setPreferredSize(new Dimension(38, 30));
        this.txtParallel.setToolTipText("Number of concurrent parallel chunk requests (default 4, range 1-32)");
        parallelPanel.add(this.txtParallel);
        this.add(parallelPanel, gbc);

        // Row 3: System Prompt Configuration Button + Reset & Summary
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        JLabel lblPrompt = new JLabel("📝 System Prompt:");
        lblPrompt.setFont(lblPrompt.getFont().deriveFont(Font.BOLD, 13.0f));
        this.add(lblPrompt, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        JPanel promptActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        this.btnEditPrompt = new JButton("⚙️ Configure System Prompt...");
        this.btnEditPrompt.setPreferredSize(new Dimension(220, 30));
        this.btnEditPrompt.setToolTipText("Open dialog to view and edit custom AI system prompt");
        promptActionPanel.add(this.btnEditPrompt);

        this.btnResetPrompt = new JButton("↺ Reset");
        this.btnResetPrompt.setFont(this.btnResetPrompt.getFont().deriveFont(Font.PLAIN, 11.0f));
        this.btnResetPrompt.setPreferredSize(new Dimension(80, 30));
        this.btnResetPrompt.setToolTipText("Restore default AI search system prompt");
        promptActionPanel.add(this.btnResetPrompt);

        this.add(promptActionPanel, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        this.lblPromptSummary = new JLabel(this.formatPromptSummary(this.currentSystemPrompt));
        this.lblPromptSummary.setFont(this.lblPromptSummary.getFont().deriveFont(Font.ITALIC, 11.5f));
        this.lblPromptSummary.setForeground(new Color(100, 116, 139));
        this.add(this.lblPromptSummary, gbc);

        // Row 4: Status / Feedback bar
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        this.lblStatus = new JLabel(this.getStatusMessage(config.isAiConfigured()));
        this.lblStatus.setFont(this.lblStatus.getFont().deriveFont(Font.PLAIN, 12.0f));
        this.add(this.lblStatus, gbc);

        this.initListeners();
    }

    private String formatPromptSummary(String prompt) {
        if (StringUtils.isBlank(prompt) || prompt.trim().equals(AppConfig.DEFAULT_AI_SYSTEM_PROMPT.trim())) {
            return "✓ Standard default prompt active";
        }
        return "⚡ Custom prompt configured (" + prompt.trim().length() + " chars)";
    }

    private String getStatusMessage(boolean isConfigured) {
        if (isConfigured) {
            return "✅ AI Configuration active: endpoint is configured.";
        }
        return "⚠️ AI not configured yet. Please enter OpenAI-compliant URL & API key, then click 'Save Config'.";
    }

    private void initListeners() {
        this.btnSave.addActionListener(e -> this.saveSettings());
        this.btnTestConnection.addActionListener(e -> this.testConnectionAsync());
        this.btnFetchModels.addActionListener(e -> this.fetchModelsAsync());
        this.btnEditPrompt.addActionListener(e -> this.openSystemPromptDialog());
        this.btnResetPrompt.addActionListener(e -> {
            this.setSystemPrompt(AppConfig.DEFAULT_AI_SYSTEM_PROMPT);
            this.lblStatus.setText("↺ System prompt reset to default (click 'Save Config' to persist).");
            this.lblStatus.setForeground(new Color(30, 64, 175));
        });
    }

    public void openSystemPromptDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        SystemPromptDialog dialog = new SystemPromptDialog(owner, this.currentSystemPrompt);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            this.currentSystemPrompt = dialog.getPrompt();
            this.lblPromptSummary.setText(this.formatPromptSummary(this.currentSystemPrompt));
            this.lblStatus.setText("📝 System prompt updated in dialog (click 'Save Config' to persist).");
            this.lblStatus.setForeground(new Color(30, 64, 175));
        }
    }

    public void saveSettings() {
        String url = StringUtils.trimToEmpty(this.txtBaseUrl.getText());
        String key = new String(this.txtApiKey.getPassword()).trim();
        String model = this.comboModel.getSelectedItem() != null ? this.comboModel.getSelectedItem().toString().trim() : "";
        double temperature = 0.1;
        try {
            temperature = Double.parseDouble(StringUtils.trimToEmpty(this.txtTemperature.getText()));
        } catch (NumberFormatException ignored) {
        }
        int timeout = 30;
        try {
            timeout = Integer.parseInt(StringUtils.trimToEmpty(this.txtTimeout.getText()));
        } catch (NumberFormatException ignored) {
        }
        int parallel = 4;
        try {
            parallel = Integer.parseInt(StringUtils.trimToEmpty(this.txtParallel.getText()));
        } catch (NumberFormatException ignored) {
        }

        this.config.setAiBaseUrl(url);
        this.config.setAiApiKey(key);
        this.config.setAiModel(model);
        this.config.setAiTemperature(temperature);
        this.config.setAiTimeoutSeconds(timeout);
        this.config.setAiParallelRequests(parallel);
        this.config.setAiSystemPrompt(this.currentSystemPrompt);

        this.configManager.save(this.config);

        boolean ready = this.config.isAiConfigured();
        if (ready) {
            this.lblStatus.setText("✅ Settings saved successfully. AI search is ready!");
            this.lblStatus.setForeground(new Color(46, 125, 50));
        } else {
            this.lblStatus.setText("⚠️ Settings saved. AI URL and API key are required to unlock AI search.");
            this.lblStatus.setForeground(new Color(230, 81, 0));
        }

        if (this.onConfigSaved != null) {
            this.onConfigSaved.accept(this.config);
        }
    }

    public void fetchModelsAsync() {
        String url = StringUtils.trimToEmpty(this.txtBaseUrl.getText());
        String key = new String(this.txtApiKey.getPassword()).trim();

        if (StringUtils.isBlank(url)) {
            this.lblStatus.setText("⚠️ Please provide an OpenAI Compliant URL first.");
            this.lblStatus.setForeground(new Color(230, 81, 0));
            return;
        }

        this.btnFetchModels.setEnabled(false);
        this.lblStatus.setText("⏳ Fetching models from " + url + "...");
        this.lblStatus.setForeground(Color.GRAY);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return openAiClient.fetchAvailableModels(url, key);
            }

            @Override
            protected void done() {
                try {
                    List<String> models = get();
                    if (models != null && !models.isEmpty()) {
                        String currentSelected = comboModel.getSelectedItem() != null
                                ? comboModel.getSelectedItem().toString() : "";
                        comboModel.removeAllItems();
                        for (String m : models) {
                            comboModel.addItem(m);
                        }
                        if (StringUtils.isNotBlank(currentSelected) && models.contains(currentSelected)) {
                            comboModel.setSelectedItem(currentSelected);
                        } else if (!models.isEmpty()) {
                            comboModel.setSelectedItem(models.get(0));
                        }
                        lblStatus.setText("✅ Successfully populated " + models.size() + " models from endpoint!");
                        lblStatus.setForeground(new Color(46, 125, 50));
                    } else {
                        comboModel.removeAllItems();
                        lblStatus.setText("⚠️ No model list returned by endpoint.");
                        lblStatus.setForeground(new Color(230, 81, 0));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("❌ Failed to fetch models: " + ex.getMessage());
                    lblStatus.setForeground(new Color(198, 40, 40));
                } finally {
                    btnFetchModels.setEnabled(true);
                }
            }
        }.execute();
    }

    private void testConnectionAsync() {
        String url = StringUtils.trimToEmpty(this.txtBaseUrl.getText());
        String key = new String(this.txtApiKey.getPassword()).trim();
        String model = this.comboModel.getSelectedItem() != null ? this.comboModel.getSelectedItem().toString().trim() : "";

        this.btnTestConnection.setEnabled(false);
        this.lblStatus.setText("⏳ Testing connection to " + url + "...");
        this.lblStatus.setForeground(Color.GRAY);

        new SwingWorker<OpenAiClient.ConnectionTestResult, Void>() {
            @Override
            protected OpenAiClient.ConnectionTestResult doInBackground() {
                return openAiClient.testConnection(url, key, model);
            }

            @Override
            protected void done() {
                try {
                    OpenAiClient.ConnectionTestResult res = get();
                    if (res.success()) {
                        lblStatus.setText("✅ " + res.message());
                        lblStatus.setForeground(new Color(46, 125, 50));
                    } else {
                        lblStatus.setText("❌ Connection Failed: " + res.message());
                        lblStatus.setForeground(new Color(198, 40, 40));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("❌ Error: " + ex.getMessage());
                    lblStatus.setForeground(new Color(198, 40, 40));
                } finally {
                    btnTestConnection.setEnabled(true);
                }
            }
        }.execute();
    }

    public void setOnConfigSaved(Consumer<AppConfig> onConfigSaved) {
        this.onConfigSaved = onConfigSaved;
    }

    public String getBaseUrl() {
        return StringUtils.trimToEmpty(this.txtBaseUrl.getText());
    }

    public void setBaseUrl(String url) {
        this.txtBaseUrl.setText(url != null ? url : "");
    }

    public String getApiKey() {
        return new String(this.txtApiKey.getPassword()).trim();
    }

    public void setApiKey(String key) {
        this.txtApiKey.setText(key != null ? key : "");
    }

    public String getModel() {
        return this.comboModel.getSelectedItem() != null ? this.comboModel.getSelectedItem().toString().trim() : "";
    }

    public void setModel(String model) {
        if (StringUtils.isNotBlank(model)) {
            boolean exists = false;
            for (int i = 0; i < this.comboModel.getItemCount(); i++) {
                if (model.trim().equals(this.comboModel.getItemAt(i))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                this.comboModel.addItem(model.trim());
            }
            this.comboModel.setSelectedItem(model.trim());
        } else {
            this.comboModel.setSelectedItem(null);
        }
    }

    public double getTemperature() {
        try {
            return Double.parseDouble(StringUtils.trimToEmpty(this.txtTemperature.getText()));
        } catch (NumberFormatException e) {
            return 0.1;
        }
    }

    public void setTemperature(double temp) {
        this.txtTemperature.setText(String.valueOf(Math.max(0.0, Math.min(2.0, temp))));
    }

    public int getParallelRequests() {
        try {
            return Integer.parseInt(StringUtils.trimToEmpty(this.txtParallel.getText()));
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    public void setParallelRequests(int parallel) {
        this.txtParallel.setText(String.valueOf(Math.max(1, Math.min(32, parallel))));
    }

    public JTextField getTxtParallel() {
        return this.txtParallel;
    }

    public String getSystemPrompt() {
        return this.currentSystemPrompt;
    }

    public void setSystemPrompt(String prompt) {
        this.currentSystemPrompt = prompt != null ? prompt : AppConfig.DEFAULT_AI_SYSTEM_PROMPT;
        if (this.lblPromptSummary != null) {
            this.lblPromptSummary.setText(this.formatPromptSummary(this.currentSystemPrompt));
        }
    }

    public JComboBox<String> getComboModel() {
        return this.comboModel;
    }

    public JButton getBtnEditPrompt() {
        return this.btnEditPrompt;
    }

    public JButton getBtnFetchModels() {
        return this.btnFetchModels;
    }

    public JButton getBtnResetPrompt() {
        return this.btnResetPrompt;
    }

    public JButton getBtnSave() {
        return this.btnSave;
    }

    public JButton getBtnTestConnection() {
        return this.btnTestConnection;
    }

    public JLabel getLblPromptSummary() {
        return this.lblPromptSummary;
    }
}

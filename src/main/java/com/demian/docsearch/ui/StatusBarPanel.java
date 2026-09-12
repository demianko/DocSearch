package com.demian.docsearch.ui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusBarPanel extends JPanel {
    private final JLabel lblStatus;
    private final JLabel lblCount;

    public StatusBarPanel() {
        super(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        this.lblStatus = new JLabel("Ready");
        this.lblCount = new JLabel("0 files found");
        this.lblCount.setFont(this.lblCount.getFont().deriveFont(1));
        this.add(this.lblStatus, BorderLayout.WEST);
        this.add(this.lblCount, BorderLayout.EAST);
    }

    public void setStatus(String status) {
        this.lblStatus.setText(status != null ? status : "");
    }

    public void setCount(String count) {
        this.lblCount.setText(count != null ? count : "");
    }
}

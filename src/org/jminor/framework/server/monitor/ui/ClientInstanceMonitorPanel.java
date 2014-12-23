/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.common.server.ClientLog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private final JTextField txtCreationDate = new JTextField();
  private final JTextArea txtLog = new JTextArea();

  private ClientInstanceMonitor model;
  private JCheckBox chkLoggingEnabled;

  /**
   * Instantiates a new ClientInstanceMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitorPanel() throws RemoteException {
    initializeUI();
    updateView();
  }

  public void setModel(final ClientInstanceMonitor model) throws RemoteException {
    this.model = model;
    if (model != null) {
      chkLoggingEnabled.setModel(model.getLoggingEnabledButtonModel());
    }
    updateView();
  }

  public void disconnectSelected() {
    throw new UnsupportedOperationException("ClientInstanceMonitorPanel.disconnectSelected() has not been implemented");
  }

  public void updateView() throws RemoteException {
    final StringBuilder log = new StringBuilder();
    if (model != null) {
      final ClientLog serverLog = model.getLog();
      if (serverLog != null) {
        MethodLogger.appendLogEntries(log, serverLog.getEntries(), 0);
      }
      else {
        log.append("Disconnected!");
      }
      chkLoggingEnabled.setSelected(model.isLoggingEnabled());
      txtCreationDate.setText(DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(new Date(model.getCreationDate())));
    }
    else {
      txtCreationDate.setText("");
    }
    txtLog.setText(log.toString());
  }

  public boolean isLoggingEnabled() throws RemoteException {
    return model != null && model.isLoggingEnabled();
  }

  public void setLoggingEnabled(final boolean status) throws RemoteException {
    if (model != null) {
      model.setLoggingEnabled(status);
    }
  }

  private void initializeUI() {
    setLayout(UiUtil.createBorderLayout());
    txtCreationDate.setEditable(false);
    final JPanel infoPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    infoPanel.add(new JLabel("Creation date"));
    infoPanel.add(txtCreationDate);
    final JPanel infoBase = new JPanel(UiUtil.createBorderLayout());
    infoBase.setBorder(BorderFactory.createTitledBorder("Connection info"));
    infoBase.add(infoPanel, BorderLayout.CENTER);
    chkLoggingEnabled = new JCheckBox("Logging enabled");
    final JPanel pnlSettings = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    pnlSettings.add(chkLoggingEnabled);
    pnlSettings.add(ControlProvider.createButton(
            Controls.methodControl(this, "updateView", "Refresh log")));
    infoBase.add(pnlSettings, BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    txtLog.setLineWrap(false);
    txtLog.setEditable(false);
    final JScrollPane scrollPane = new JScrollPane(txtLog);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Connection log"));

    add(scrollPane, BorderLayout.CENTER);
  }
}

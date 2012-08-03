/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.LogEntry;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.server.ServerLog;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.framework.Configuration;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private static final int GAP = Configuration.getIntValue(Configuration.DEFAULT_HORIZONTAL_AND_VERTICAL_COMPONENT_GAP);

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
      final ServerLog serverLog = model.getLog();
      if (serverLog != null) {
        final List<LogEntry> logEntries = serverLog.getLog();
        appendLogEntries(log, logEntries, 0);
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
    setLayout(new BorderLayout(GAP, GAP));
    txtCreationDate.setEditable(false);
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
    infoPanel.add(new JLabel("Creation date"));
    infoPanel.add(txtCreationDate);
    final JPanel infoBase = new JPanel(new BorderLayout(GAP, GAP));
    infoBase.setBorder(BorderFactory.createTitledBorder("Connection info"));
    infoBase.add(infoPanel, BorderLayout.CENTER);
    chkLoggingEnabled = new JCheckBox("Logging enabled");
    final JPanel pnlSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
    pnlSettings.add(chkLoggingEnabled);
    pnlSettings.add(ControlProvider.createButton(
            Controls.methodControl(this, "updateView", "Refresh log")));
    infoBase.add(pnlSettings, BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    txtLog.setLineWrap(false);
    final JScrollPane scrollPane = new JScrollPane(txtLog);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Connection log"));

    add(scrollPane, BorderLayout.CENTER);
  }

  private static void appendLogEntries(final StringBuilder log, final List<LogEntry> logEntries, final int indentation) {
    Collections.sort(logEntries);
    for (final LogEntry logEntry : logEntries) {
      log.append(logEntry.toString(indentation)).append("\n");
      final List<LogEntry> subLog = logEntry.getSubLog();
      if (subLog != null) {
        appendLogEntries(log, subLog, indentation + 1);
      }
    }
  }
}

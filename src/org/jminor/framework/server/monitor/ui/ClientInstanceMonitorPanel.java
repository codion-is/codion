/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.model.LogEntry;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.server.ServerLog;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
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
 * User: Bjorn Darri<br>
 * Date: 4.12.2007<br>
 * Time: 22:49:42<br>
 */
public class ClientInstanceMonitorPanel extends JPanel {

  private final JTextField txtCreationDate = new JTextField();
  private final JTextArea txtLog = new JTextArea();

  private ClientInstanceMonitor model;
  private JCheckBox chkLoggingEnabled;

  public ClientInstanceMonitorPanel() throws RemoteException {
    initUI();
    updateView();
  }

  public void setModel(final ClientInstanceMonitor model) throws RemoteException {
    this.model = model;
    if (model != null) {
      chkLoggingEnabled.setModel(model.getLoggingEnabledButtonModel());
    }
    updateView();
  }

  public void disconnectSelected() throws RemoteException {
    throw new RuntimeException("ClientInstanceMonitorPanel.disconnectSelected() has not been implemented");
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

      txtCreationDate.setText(DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(new Date(model.getCreationDate())));
    }
    else {
      txtCreationDate.setText("");
    }

    txtLog.setText(log.toString());
  }

  public boolean isLoggingOn() throws RemoteException {
    return model != null && model.isLoggingOn();
  }

  public void setLoggingOn(final boolean status) throws RemoteException {
    if (model != null) {
      model.setLoggingOn(status);
    }
  }

  private void appendLogEntries(StringBuilder log, List<LogEntry> logEntries, final int indentation) {
    Collections.sort(logEntries);
    for (final LogEntry logEntry : logEntries) {
      log.append(logEntry.toString(indentation)).append("\n");
      final List<LogEntry> subLog = logEntry.getSubLog();
      if (subLog != null) {
        appendLogEntries(log, subLog, indentation + 1);
      }
    }
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout(5,5));
    txtCreationDate.setEditable(false);
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Creation date"));
    infoPanel.add(txtCreationDate);
    final JPanel infoBase = new JPanel(new BorderLayout(5,5));
    infoBase.setBorder(BorderFactory.createTitledBorder("Connection info"));
    infoBase.add(infoPanel, BorderLayout.CENTER);
    chkLoggingEnabled = new JCheckBox("Logging enabled");
    final JPanel pnlSettings = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    pnlSettings.add(chkLoggingEnabled);
    pnlSettings.add(ControlProvider.createButton(
            ControlFactory.methodControl(this, "updateView", "Refresh log")));
    infoBase.add(pnlSettings, BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    txtLog.setLineWrap(false);
    final JScrollPane scrollPane = new JScrollPane(txtLog);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Connection log"));

    add(scrollPane, BorderLayout.CENTER);
  }

//  private ControlSet getPopupCommands() {
//    final ControlSet controlSet = new ControlSet();
//    controlSet.add(ControlFactory.methodControl(this, "disconnectSelected", "Disconnect selected"));
//
//    return controlSet;
//  }
}

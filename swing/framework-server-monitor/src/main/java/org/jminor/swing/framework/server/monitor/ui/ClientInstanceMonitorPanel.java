/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.MethodLogger;
import org.jminor.common.remote.ClientLog;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private final JTextField creationDateField = new JTextField();
  private final JTextArea logArea = new JTextArea();
  private final JTree treeLog = new JTree();

  private ClientInstanceMonitor model;
  private JCheckBox loggingEnabledCheckBox;

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
      loggingEnabledCheckBox.setModel(model.getLoggingEnabledButtonModel());
      treeLog.setModel(model.getLogTreeModel());
    }
    else {
      treeLog.setModel(null);
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
      loggingEnabledCheckBox.setSelected(model.isLoggingEnabled());
      final LocalDateTime creationDate = model.getCreationDate();
      creationDateField.setText(creationDate == null ? "unknown" : DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP).format(creationDate));
      model.refreshLogTreeModel();
    }
    else {
      creationDateField.setText("");
    }
    logArea.setText(log.toString());
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
    creationDateField.setEditable(false);
    final JPanel infoPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    infoPanel.add(new JLabel("Creation date"));
    infoPanel.add(creationDateField);
    final JPanel infoBase = new JPanel(UiUtil.createBorderLayout());
    infoBase.setBorder(BorderFactory.createTitledBorder("Connection info"));
    infoBase.add(infoPanel, BorderLayout.CENTER);
    loggingEnabledCheckBox = new JCheckBox("Logging enabled");
    final JPanel settingsPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    settingsPanel.add(loggingEnabledCheckBox);
    settingsPanel.add(new JButton(Controls.control(this::updateView, "Refresh log")));
    infoBase.add(settingsPanel, BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    logArea.setLineWrap(false);
    logArea.setEditable(false);
    final JScrollPane scrollPane = new JScrollPane(logArea);

    treeLog.setRootVisible(false);
    final JScrollPane treeScrollPane = new JScrollPane(treeLog);

    final JTabbedPane logPane = new JTabbedPane(SwingConstants.TOP);
    logPane.add(scrollPane, "Text");
    logPane.add(treeScrollPane, "Tree");

    add(logPane, BorderLayout.CENTER);
  }
}

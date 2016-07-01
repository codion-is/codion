/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.MethodLogger;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.server.ClientLog;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
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
import java.util.Date;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private final JTextField txtCreationDate = new JTextField();
  private final JTextArea txtLog = new JTextArea();
  private final JTree treeLog = new JTree();

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
      chkLoggingEnabled.setSelected(model.isLoggingEnabled());
      txtCreationDate.setText(DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(new Date(model.getCreationDate())));
      model.refreshLogTreeModel();
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

    treeLog.setRootVisible(false);
    final JScrollPane treeScrollPane = new JScrollPane(treeLog);

    final JTabbedPane logPane = new JTabbedPane(SwingConstants.TOP);
    logPane.add(scrollPane, "Text");
    logPane.add(treeScrollPane, "Tree");

    add(logPane, BorderLayout.CENTER);
  }
}

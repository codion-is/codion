/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.ServerLog;
import org.jminor.common.db.ServerLogEntry;
import org.jminor.common.model.formats.FullTimestampFormat;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
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
import java.util.ListIterator;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 22:49:42
 */
public class ClientInstanceMonitorPanel extends JPanel {

  private final JTextField txtCreationDate = new JTextField();
  private final JTextArea txtLog = new JTextArea();

  private ClientInstanceMonitor model;

  public ClientInstanceMonitorPanel() throws RemoteException {
    initUI();
    updateView();
  }

  public void setModel(final ClientInstanceMonitor model) throws RemoteException {
    this.model = model;
    updateView();
  }

  public void disconnectSelected() throws RemoteException {
    throw new RuntimeException("ClientInstanceMonitorPanel.disconnectSelected() has not been implemented");
  }

  public void updateView() throws RemoteException {
    final StringBuilder log = new StringBuilder();
    if (model != null) {
      final ServerLog serverLog = model.getLog();
      if (serverLog != null)
        for (final ServerLogEntry logEntry : sortAndRemoveNullEntries(serverLog.log))
          log.append(logEntry.toString());
      else
        log.append("Disconnected!");

      txtCreationDate.setText(FullTimestampFormat.get().format(new Date(model.getCreationDate())));
    }
    else {
      txtCreationDate.setText("");
    }

    txtLog.setText(log.toString());
  }

  private List<ServerLogEntry> sortAndRemoveNullEntries(final List<ServerLogEntry> log) {
    Collections.sort(log);
    final ListIterator<ServerLogEntry> iterator = log.listIterator();
    while (iterator.hasNext())
      if (iterator.next().entryTime == 0)
        iterator.remove();

    return log;
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
    infoBase.add(ControlProvider.createButton(
            ControlFactory.methodControl(this, "updateView", "Refresh log")), BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    txtLog.setLineWrap(false);
    final JScrollPane scrollPane = new JScrollPane(txtLog);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Connection log"));

    add(scrollPane, BorderLayout.CENTER);
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(this, "disconnectSelected", "Disconnect selected"));

    return ret;
  }
}

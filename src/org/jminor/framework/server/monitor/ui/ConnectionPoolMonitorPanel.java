/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.pool.monitor.ConnectionPoolInstanceMonitor;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.ConnectionPoolInstanceMonitorPanel;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri
 * Date: 10.12.2007
 * Time: 15:54:36
 */
public class ConnectionPoolMonitorPanel extends JPanel {

  private final ConnectionPoolMonitor model;

  public ConnectionPoolMonitorPanel(final ConnectionPoolMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  public void addConnectionPool() throws RemoteException {
    final String usernames = JOptionPane.showInputDialog("User name(s) (comma separated)");
    if (usernames != null && usernames.length() > 0)
      model.addConnectionPools(usernames.split(","));
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane connectionPoolPane = new JTabbedPane();
    connectionPoolPane.setUI(new BorderlessTabbedPaneUI());
    for (final ConnectionPoolInstanceMonitor monitor : model.getConnectionPoolInstanceMonitors())
      connectionPoolPane.addTab(monitor.getUser().getUsername(), new ConnectionPoolInstanceMonitorPanel(monitor));
    add(connectionPoolPane, BorderLayout.CENTER);
  }

//  private ControlSet getPopupCommands() {
//    final ControlSet controlSet = new ControlSet();
//    controlSet.add(ControlFactory.methodControl(this, "addConnectionPool", "Add connection pool(s)"));
//    controlSet.addSeparator();
//    controlSet.add(ControlFactory.methodControl(model, "refresh", "Refresh"));
//
//    return controlSet;
//  }
}

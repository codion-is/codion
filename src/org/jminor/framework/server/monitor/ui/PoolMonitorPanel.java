/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.framework.server.monitor.PoolMonitor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 * Date: 10.12.2007<br>
 * Time: 15:54:36<br>
 */
public final class PoolMonitorPanel extends JPanel {

  private final PoolMonitor model;

  public PoolMonitorPanel(final PoolMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  public void addConnectionPool() throws RemoteException {
    final String usernames = JOptionPane.showInputDialog("User name(s) (comma separated)");
    if (usernames != null && usernames.length() > 0) {
      model.addConnectionPools(usernames.split(","));
    }
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane connectionPoolPane = new JTabbedPane();
    connectionPoolPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final ConnectionPoolMonitor monitor : model.getConnectionPoolInstanceMonitors()) {
      connectionPoolPane.addTab(monitor.getUser().getUsername(), new ConnectionPoolMonitorPanel(monitor));
    }
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

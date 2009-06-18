/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.framework.server.monitor.ClientMonitor;
import org.jminor.framework.server.monitor.ClientTypeMonitor;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 10.12.2007
 * Time: 17:32:53
 */
public class ClientMonitorPanel extends JPanel {

  private final ClientMonitor model;

  public ClientMonitorPanel(final ClientMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout());
    final JTabbedPane clientTypesPane = new JTabbedPane();
    for (final ClientTypeMonitor monitor : model.getClientTypeMonitors())
      clientTypesPane.addTab(monitor.getClientTypeID(), new ClientTypeMonitorPanel(monitor));
  }
}

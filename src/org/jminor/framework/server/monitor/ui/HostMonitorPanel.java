/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.ServerMonitor;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:12:28
 */
public class HostMonitorPanel extends JPanel {

  private final HostMonitor model;

  public HostMonitorPanel(final HostMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout(5,5));
    final JTabbedPane serverPane = new JTabbedPane();
    for (final String serverName : model.getServerNames())
      serverPane.add(serverName, new ServerMonitorPanel(new ServerMonitor(model.getHostName(), serverName)));
    add(serverPane, BorderLayout.CENTER);
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}

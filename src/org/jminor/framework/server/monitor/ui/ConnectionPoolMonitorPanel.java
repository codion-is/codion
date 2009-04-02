/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.rmi.RemoteException;

/**
 * User: Bj�rn Darri
 * Date: 10.12.2007
 * Time: 15:54:36
 */
public class ConnectionPoolMonitorPanel extends JPanel implements IPopupProvider {

  private final ConnectionPoolMonitor model;
  private JPopupMenu popupMenu;

  public ConnectionPoolMonitorPanel(final ConnectionPoolMonitor model) {
    this.model = model;
  }

  public void addConnectionPool() throws RemoteException {
    final String usernames = JOptionPane.showInputDialog("User name(s) (comma seperated)");
    if (usernames != null && usernames.length() > 0)
      model.addConnectionPools(usernames.split(","));
  }

  public JPopupMenu getPopupMenu() {
    if (popupMenu == null)
      popupMenu = ControlProvider.createPopupMenu(getPopupCommands());

    return popupMenu;
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(this, "addConnectionPool", "Add connection pool(s)"));
    ret.addSeparator();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}

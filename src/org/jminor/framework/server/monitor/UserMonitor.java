/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.User;
import org.jminor.framework.server.EntityDbServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;
import java.util.Enumeration;

/**
 * User: Bjorn Darri<br>
 * Date: 11.12.2007<br>
 * Time: 11:26:21<br>
 */
public class UserMonitor {

  private final EntityDbServerAdmin server;

  private final DefaultListModel userInstanceMonitorsListModel = new DefaultListModel();

  public UserMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    userInstanceMonitorsListModel.clear();
    for (final User user : server.getUsers())
      userInstanceMonitorsListModel.addElement(new UserInstanceMonitor(server, user));
  }

  public DefaultListModel getUserInstanceMonitorsListModel() {
    return userInstanceMonitorsListModel;
  }

  public EntityDbServerAdmin getServer() {
    return server;
  }

  public void shutdown() {
    System.out.println("UserMonitor shutdown");
    final Enumeration enumeration = userInstanceMonitorsListModel.elements();
    while (enumeration.hasMoreElements())
      ((UserInstanceMonitor) enumeration.nextElement()).shutdown();
  }
}
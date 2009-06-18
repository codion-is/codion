/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.User;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import javax.swing.DefaultListModel;
import java.rmi.RemoteException;
import java.util.Enumeration;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:26:21
 */
public class UserMonitor {

  private final IEntityDbRemoteServerAdmin server;

  private final DefaultListModel userInstanceMonitorsListModel = new DefaultListModel();

  public UserMonitor(final IEntityDbRemoteServerAdmin server) throws RemoteException {
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

  public IEntityDbRemoteServerAdmin getServer() {
    return server;
  }

  public void shutdown() throws RemoteException {
    System.out.println("UserMonitor shutdown");
    final Enumeration enumeration = userInstanceMonitorsListModel.elements();
    while (enumeration.hasMoreElements())
      ((UserInstanceMonitor) enumeration.nextElement()).shutdown();
  }
}
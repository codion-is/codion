/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.User;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines the methods available to remote clients.
 * User: darri
 * Date: 4.3.2005
 * Time: 10:59:16
 */
public interface EntityDbServer extends Remote {

  public final String SERVER_ADMIN_SUFFIX = "-admin";

  /**
   * Establishes a connection to this EntityDbServer
   * @param user the user
   * @param connectionKey a String identifying the connection
   * @param clientTypeID a String identifying the client
   * @return a EntityDbRemote instance
   * @throws RemoteException in case of a RemoteException
   */
  EntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID) throws RemoteException;

  /**
   * @return the server name
   * @throws RemoteException in case of a communication error
   */
  String getServerName() throws RemoteException;

  /**
   * @return the server port
   * @throws RemoteException in case of a RemoteException
   */
  int getServerPort() throws RemoteException;

  /**
   * @return the server load as number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getServerLoad() throws RemoteException;
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.User;
import org.jminor.framework.domain.EntityDefinition;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Defines the methods available to clients
 * User: darri
 * Date: 4.3.2005
 * Time: 10:59:16
 */
public interface EntityDbServer extends Remote {

  public final String SERVER_ADMIN_SUFFIX = "-admin";

  EntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID,
                                final Map<String, EntityDefinition> repository) throws RemoteException;

  /**
   * @return the server name
   * @throws RemoteException in case of a communication error
   */
  String getServerName() throws RemoteException;

  /**
   * @return the server port
   * @throws java.rmi.RemoteException in case of a RemoteException
   */
  int getServerPort() throws RemoteException;

  /**
   * @return the server load as number of service requests per second
   * @throws RemoteException in case of an exception
   */
  Integer getServerLoad() throws RemoteException;
}

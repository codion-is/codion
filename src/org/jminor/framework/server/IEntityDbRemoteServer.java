/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.User;
import org.jminor.framework.model.EntityRepository;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines the methods available to clients
 * User: darri
 * Date: 4.3.2005
 * Time: 10:59:16
 */
public interface IEntityDbRemoteServer extends Remote {

  public IEntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID,
                                 final EntityRepository repository) throws RemoteException;

  /**
   * @return the server name
   * @throws RemoteException in case of a communication error
   */
  public String getServerName() throws RemoteException;

  /**
   * @return the server port
   * @throws java.rmi.RemoteException in case of a RemoteException
   */
  public int getServerPort() throws RemoteException;

  /**
   * @return the server load as number of service requests per second
   * @throws RemoteException in case of an exception
   */
  public Integer getServerLoad() throws RemoteException;
}

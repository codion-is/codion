/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.credentials.server;

import org.jminor.common.user.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A service for supplying user credentials for one-time authentication tokens.
 */
public interface CredentialsService extends Remote {

  /**
   * @param authenticationToken the token
   * @return the user credentials associated with the given token, null if expired or invalid
   * @throws RemoteException in case of a communication error
   */
  User getUser(UUID authenticationToken) throws RemoteException;
}

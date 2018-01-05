/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A service for supplying user credentials for one-time authentication tokens.
 */
public interface CredentialService extends Remote {

  /**
   * @param authenticationToken the token
   * @return the user credentials associated with the given token, null if expired or invalid
   * @throws RemoteException in case of a communication error
   */
  User getUser(final UUID authenticationToken) throws RemoteException;
}

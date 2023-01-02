/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.common.user.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;

/**
 * A service for supplying user credentials for one-time authentication tokens.
 * @see #REGISTRY_PORT
 */
public interface CredentialsService extends Remote {

  /**
   * Specifies the RMI registry port to use when looking up the credentials service.
   * Default value: {@link Registry#REGISTRY_PORT}
   */
  PropertyValue<Integer> REGISTRY_PORT = Configuration.integerValue("codion.credentials.registryPort", Registry.REGISTRY_PORT);

  /**
   * @param authenticationToken the token
   * @return the user credentials associated with the given token, null if expired or invalid
   * @throws RemoteException in case of a communication error
   */
  User user(UUID authenticationToken) throws RemoteException;
}

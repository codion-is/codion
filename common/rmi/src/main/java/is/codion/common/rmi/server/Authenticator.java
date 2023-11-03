/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.server.exception.LoginException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * An authenticator.
 */
public interface Authenticator {

  /**
   * Returns the the id of the client type for which to use this authenticator.
   * If none is specified, this authenticator is shared between all clients.
   * @return the String identifying the client type for which to use this authenticator or an empty optional in case this authenticator should be shared
   */
  default Optional<String> clientTypeId() {
    return Optional.empty();
  }

  /**
   * Performs login validation for the user specified by the remote client
   * and returns a remote client with the same clientId and user but possibly
   * a different databaseUser to propagate to further login procedures
   * @param remoteClient the client
   * @return a new client with the same clientId but not necessarily the same user or databaseUser
   * @throws LoginException in case the login fails
   * @see RemoteClient#databaseUser()
   */
  RemoteClient login(RemoteClient remoteClient) throws LoginException;

  /**
   * Called after the given client has been disconnected
   * @param remoteClient the remote client
   */
  void logout(RemoteClient remoteClient);

  /**
   * Disposes of all resources used by this authenticator, after a call to this
   * method the authenticator should be regarded as unusable.
   * This method should be called by a server using this authenticator on shutdown,
   * giving the authenticator a chance to release resources in an orderly manner.
   * Any exception thrown by this method is ignored.
   */
  void close();

  /**
   * @return a list containing all the authenticators registered with {@link ServiceLoader}.
   */
  static List<Authenticator> authenticators() {
    List<Authenticator> authenticators = new ArrayList<>();
    ServiceLoader<Authenticator> loader = ServiceLoader.load(Authenticator.class);
    for (Authenticator authenticator : loader) {
      authenticators.add(authenticator);
    }
    System.out.println(authenticators.size());

    return authenticators;
  }
}
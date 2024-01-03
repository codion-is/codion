/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
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
  default void logout(RemoteClient remoteClient) {}

  /**
   * Disposes of all resources used by this authenticator, after a call to this
   * method the authenticator should be regarded as unusable.
   * This method should be called by a server using this authenticator on shutdown,
   * giving the authenticator a chance to release resources in an orderly manner.
   * Any exception thrown by this method is ignored.
   */
  default void close() {}

  /**
   * @return a list containing all the authenticators registered with {@link ServiceLoader}.
   */
  static List<Authenticator> authenticators() {
    List<Authenticator> authenticators = new ArrayList<>();
    for (Authenticator authenticator : ServiceLoader.load(Authenticator.class)) {
      authenticators.add(authenticator);
    }

    return authenticators;
  }
}
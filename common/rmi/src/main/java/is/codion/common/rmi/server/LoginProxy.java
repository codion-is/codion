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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.server.exception.LoginException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * A login proxy.
 */
public interface LoginProxy {

  /**
   * Returns the the id of the client type for which to use this login proxy.
   * If none is specified, this login proxy is shared between all clients.
   * @return the String identifying the client type for which to use this login proxy or an empty optional in case this login proxy should be shared
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
   * Disposes of all resources used by this LoginProxy, after a call to this
   * method the proxy should be regarded as unusable.
   * This method should be called by a server using this LoginProxy on shutdown,
   * giving the LoginProxy a chance to release resources in an orderly manner.
   * Any exception thrown by this method is ignored.
   */
  void close();

  /**
   * @return a list containing all the LoginProxies registered with {@link ServiceLoader}.
   */
  static List<LoginProxy> loginProxies() {
    List<LoginProxy> loginProxies = new ArrayList<>();
    ServiceLoader<LoginProxy> loader = ServiceLoader.load(LoginProxy.class);
    for (LoginProxy loginProxy : loader) {
      loginProxies.add(loginProxy);
    }

    return loginProxies;
  }
}
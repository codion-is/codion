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
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.framework.db.EntityConnectionProvider;

/**
 * A class responsible for managing a remote entity connection.
 * @see RemoteEntityConnectionProvider#builder()
 */
public interface RemoteEntityConnectionProvider extends EntityConnectionProvider {

  /**
   * A key for specifying the domain type required by a remote client
   */
  String REMOTE_CLIENT_DOMAIN_TYPE = "codion.client.domainType";

  /**
   * @return the name of the host of the server providing the connection
   */
  String hostName();

  /**
   * Instantiates a new builder instance.
   * @return a new builder
   */
  static Builder builder() {
    return new DefaultRemoteEntityConnectionProviderBuilder();
  }

  /**
   * Builds a {@link RemoteEntityConnectionProvider}.
   */
  interface Builder extends EntityConnectionProvider.Builder<RemoteEntityConnectionProvider, Builder> {

    /**
     * @param hostName the server host name
     * @return this builder instance
     */
    Builder hostName(String hostName);

    /**
     * @param port the server port
     * @return this builder instance
     */
    Builder port(int port);

    /**
     * @param registryPort the rmi registry port
     * @return this builder instance
     */
    Builder registryPort(int registryPort);

    /**
     * @param serverNamePrefix the name prefix to use when looking up the server
     * @return this builder instance
     */
    Builder namePrefix(String serverNamePrefix);
  }
}

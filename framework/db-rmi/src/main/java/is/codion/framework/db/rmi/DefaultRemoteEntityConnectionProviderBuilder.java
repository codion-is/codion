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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.db.EntityConnectionProvider;

import static java.util.Objects.requireNonNull;

/**
 * Builds a {@link RemoteEntityConnectionProvider} instance.
 * @see RemoteEntityConnectionProvider#builder()
 */
public final class DefaultRemoteEntityConnectionProviderBuilder
        extends AbstractBuilder<RemoteEntityConnectionProvider, RemoteEntityConnectionProvider.Builder>
        implements RemoteEntityConnectionProvider.Builder {

  String hostName = Clients.SERVER_HOSTNAME.get();
  int port = ServerConfiguration.SERVER_PORT.get();
  int registryPort = ServerConfiguration.REGISTRY_PORT.get();
  String namePrefix = ServerConfiguration.SERVER_NAME_PREFIX.get();

  public DefaultRemoteEntityConnectionProviderBuilder() {
    super(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
  }

  @Override
  public RemoteEntityConnectionProvider.Builder hostName(String hostName) {
    this.hostName = requireNonNull(hostName);
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider.Builder port(int port) {
    this.port = port;
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider.Builder registryPort(int registryPort) {
    this.registryPort = registryPort;
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider.Builder namePrefix(String namePrefix) {
    this.namePrefix = requireNonNull(namePrefix);
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider build() {
    return new DefaultRemoteEntityConnectionProvider(this);
  }
}

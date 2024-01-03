/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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

/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

  String serverHostName = Clients.SERVER_HOST_NAME.get();
  int serverPort = ServerConfiguration.SERVER_PORT.get();
  int registryPort = ServerConfiguration.REGISTRY_PORT.get();

  public DefaultRemoteEntityConnectionProviderBuilder() {
    super(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
  }

  @Override
  public RemoteEntityConnectionProvider.Builder serverHostName(String serverHostName) {
    this.serverHostName = requireNonNull(serverHostName);
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider.Builder serverPort(int serverPort) {
    this.serverPort = serverPort;
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider.Builder registryPort(int registryPort) {
    this.registryPort = registryPort;
    return this;
  }

  @Override
  public RemoteEntityConnectionProvider build() {
    return new DefaultRemoteEntityConnectionProvider(this);
  }
}

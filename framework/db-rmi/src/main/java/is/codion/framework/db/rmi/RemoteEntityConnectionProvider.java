/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.common.rmi.server.ServerInformation;
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
  String getServerHostName();

  /**
   * @return the info on the server last connected to
   */
  ServerInformation getServerInformation();

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
  interface Builder extends EntityConnectionProvider.Builder<Builder, RemoteEntityConnectionProvider> {

    /**
     * @param serverHostName the server host name
     * @return this builder instance
     */
    Builder serverHostName(String serverHostName);

    /**
     * @param serverPort the server port
     * @return this builder instance
     */
    Builder serverPort(int serverPort);

    /**
     * @param registryPort the rmi registry port
     * @return this builder instance
     */
    Builder registryPort(int registryPort);
  }
}

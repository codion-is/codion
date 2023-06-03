/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

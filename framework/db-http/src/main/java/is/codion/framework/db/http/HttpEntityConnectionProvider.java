/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.EntityConnectionProvider;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#builder()
 * @see HttpEntityConnection#HOSTNAME
 * @see HttpEntityConnection#PORT
 * @see HttpEntityConnection#SECURE
 * @see HttpEntityConnection#JSON
 * @see HttpEntityConnection#SOCKET_TIMEOUT
 * @see HttpEntityConnection#CONNECT_TIMEOUT
 */
public interface HttpEntityConnectionProvider extends EntityConnectionProvider {

  /**
   * Instantiates a new builder instance.
   * @return a new builder
   */
  static Builder builder() {
    return new DefaultHttpEntityConnectionProviderBuilder();
  }

  /**
   * Builds a {@link HttpEntityConnectionProvider} instance.
   */
  interface Builder extends EntityConnectionProvider.Builder<HttpEntityConnectionProvider, Builder> {

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
     * @param https true if https should be enabled
     * @return this builder instance
     */
    Builder https(boolean https);

    /**
     * @param json true if json serialization should be used
     * @return this builder instance
     */
    Builder json(boolean json);

    /**
     * @param socketTimeout the socket timeout
     * @return this builder instance
     */
    Builder socketTimeout(int socketTimeout);

    /**
     * @param connectTimeout the connect timeout
     * @return this builder instance
     */
    Builder connectTimeout(int connectTimeout);
  }
}

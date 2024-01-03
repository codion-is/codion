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
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.EntityConnectionProvider;

import java.util.concurrent.Executor;

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
     * @param securePort the server https port
     * @return this builder instance
     */
    Builder securePort(int securePort);

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

    /**
     * By default the http client uses a shared thread pool executor.
     * @param executor the http client executor to use
     * @return this builder instance
     */
    Builder executor(Executor executor);
  }
}

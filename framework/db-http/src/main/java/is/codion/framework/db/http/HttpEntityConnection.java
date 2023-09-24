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
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;

import java.util.UUID;

/**
 * A factory class for http based EntityConnection builder.
 * @see #HOSTNAME
 * @see #PORT
 * @see #SECURE
 * @see #JSON
 * @see #SOCKET_TIMEOUT
 * @see #CONNECT_TIMEOUT
 */
public interface HttpEntityConnection extends EntityConnection {

  /**
   * The host on which to locate the http server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> HOSTNAME = Configuration.stringValue("codion.client.http.hostname", "localhost");

  /**
   * The port which the http client should use.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  PropertyValue<Integer> PORT = Configuration.integerValue("codion.client.http.port", 8080);

  /**
   * The port which the https client should use.<br>
   * Value type: Integer<br>
   * Default value: 4443
   */
  PropertyValue<Integer> SECURE_PORT = Configuration.integerValue("codion.client.http.securePort", 4443);

  /**
   * Specifies whether https should be used.<br>
   * Value type: boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SECURE = Configuration.booleanValue("codion.client.http.secure", true);

  /**
   * Specifies whether json serialization should be used.<br>
   * Value types: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> JSON = Configuration.booleanValue("codion.client.http.json", true);

  /**
   * The socket timeout in milliseconds.<br>
   * Value type: Integer<br>
   * Default value: 2000 ms
   */
  PropertyValue<Integer> SOCKET_TIMEOUT = Configuration.integerValue("codion.client.http.socketTimeout", 2000);

  /**
   * The connect timeout in milliseconds.<br>
   * Value type: Integer<br>
   * Default value: 2000 ms
   */
  PropertyValue<Integer> CONNECT_TIMEOUT = Configuration.integerValue("codion.client.http.connectTimeout", 2000);

  /**
   * @return a new builder instance
   */
  static Builder builder() {
    return new DefaultHttpEntityConnection.DefaultBuilder();
  }

  /**
   * Builds a http based EntityConnection
   */
  interface Builder {

    /**
     * @param domainType the domain model type
     * @return this builder instance
     */
    Builder domainType(DomainType domainType);

    /**
     * @param hostName the http server host name
     * @return this builder instance
     */
    Builder hostName(String hostName);

    /**
     * @param port the http server port
     * @return this builder instance
     */
    Builder port(int port);

    /**
     * @param securePort the https server port
     * @return this builder instance
     */
    Builder securePort(int securePort);

    /**
     * @param https true if https should be used
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
     * @param user the user
     * @return this builder instance
     */
    Builder user(User user);

    /**
     * @param clientTypeId the client type id
     * @return this builder instance
     */
    Builder clientTypeId(String clientTypeId);

    /**
     * @param clientId the client id
     * @return this builder instance
     */
    Builder clientId(UUID clientId);

    /**
     * @return a http based EntityConnection
     */
    EntityConnection build();
  }
}

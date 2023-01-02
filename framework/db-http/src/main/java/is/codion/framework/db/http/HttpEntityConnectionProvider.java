/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#builder()
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_HOST_NAME
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_PORT
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_SECURE
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_JSON
 */
public interface HttpEntityConnectionProvider extends EntityConnectionProvider {

  /**
   * The host on which to locate the http server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  PropertyValue<String> HTTP_CLIENT_HOST_NAME = Configuration.stringValue("codion.client.http.hostname", "localhost");

  /**
   * The port which the http client should use.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  PropertyValue<Integer> HTTP_CLIENT_PORT = Configuration.integerValue("codion.client.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value type: boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> HTTP_CLIENT_SECURE = Configuration.booleanValue("codion.client.http.secure", true);

  /**
   * Specifies whether json serialization should be used.<br>
   * Value types: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> HTTP_CLIENT_JSON = Configuration.booleanValue("codion.client.http.json", true);

  /**
   * @return the name of the host of the server providing the connection
   */
  String serverHostName();

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
     * @param https true if https should be enabled
     * @return this builder instance
     */
    Builder https(boolean https);

    /**
     * @param json true if json serialization should be used
     * @return this builder instance
     */
    Builder json(boolean json);
  }
}

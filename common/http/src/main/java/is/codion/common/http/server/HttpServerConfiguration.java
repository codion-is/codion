/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.http.server;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;

/**
 * Configuration values for a {@link HttpServer}.
 */
public interface HttpServerConfiguration {

  /**
   * The port on which the http server is made available to clients.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  PropertyValue<Integer> HTTP_SERVER_PORT = Configuration.integerValue("codion.server.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> HTTP_SERVER_SECURE = Configuration.booleanValue("codion.server.http.secure", true);

  /**
   * The https keystore to use on the classpath, this will be resolved to a temporary file and set
   * as the codion.server.http.keyStore system property on server start<br>
   * Value type: String
   * Default value: null
   */
  PropertyValue<String> HTTP_SERVER_CLASSPATH_KEYSTORE = Configuration.stringValue("codion.server.http.classpathKeyStore");

  /**
   * Specifies the keystore to use for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   * @see #HTTP_SERVER_CLASSPATH_KEYSTORE
   */
  PropertyValue<String> HTTP_SERVER_KEYSTORE_PATH = Configuration.stringValue("codion.server.http.keyStore");

  /**
   * Specifies the password for the keystore used for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD = Configuration.stringValue("codion.server.http.keyStorePassword");

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> DOCUMENT_ROOT = Configuration.stringValue("codion.server.http.documentRoot");

  /**
   * @param port the server port
   * @return a new builder instance
   */
  static HttpServerConfiguration.Builder builder(int port) {
    return new DefaultHttpServerConfiguration.DefaultBuilder(port);
  }

  /**
   * Parses configuration from system properties.
   * @return a server configuration builder initialized according to system properties
   */
  static HttpServerConfiguration.Builder builderFromSystemProperties() {
    return builder(HttpServerConfiguration.HTTP_SERVER_PORT.getOrThrow())
            .secure(HttpServerConfiguration.HTTP_SERVER_SECURE.get())
            .documentRoot(HttpServerConfiguration.DOCUMENT_ROOT.get())
            .keystorePath(HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.get())
            .keystorePassword(HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.get());
  }

  /**
   * @return the server port
   */
  int serverPort();

  /**
   * @return true if https is used
   */
  boolean isSecure();

  /**
   * @return the document root
   */
  String documentRoot();

  /**
   * @return the keystore path
   */
  String keystorePath();

  /**
   * @return the keystore password
   */
  String keystorePassword();

  /**
   * Builds a {@link HttpServerConfiguration} instance
   */
  interface Builder {

    /**
     * @param secure true if https should be used
     * @return this builder instance
     */
    Builder secure(boolean secure);

    /**
     * @param documentRoot the document root to serve files from
     * @return this builder instance
     */
    Builder documentRoot(String documentRoot);

    /**
     * @param keystorePath the keystore path
     * @return this builder instance
     */
    Builder keystorePath(String keystorePath);

    /**
     * @param keystorePassword the keystore password
     * @return this builder instance
     */
    Builder keystorePassword(String keystorePassword);

    /**
     * @return a new {@link HttpServerConfiguration} instance
     */
    HttpServerConfiguration build();
  }
}

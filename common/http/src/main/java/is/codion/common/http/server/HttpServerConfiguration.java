/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
   * Value types: Https<br>
   * Default value: true
   */
  PropertyValue<ServerHttps> HTTP_SERVER_SECURE = Configuration.enumValue("codion.server.http.secure", ServerHttps.class, ServerHttps.TRUE);

  /**
   * Specifies the keystore to use for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
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
   * Instantiates a new HttpServerConfiguration.
   * @param port the port on which to serve
   * @param https yes if https should be used
   * @return a default configuration
   */
  static HttpServerConfiguration configuration(int port, ServerHttps https) {
    return new DefaultHttpServerConfiguration(port, https);
  }

  /**
   * Parses configuration from system properties.
   * @return a server configuration according to system properties
   */
  static HttpServerConfiguration fromSystemProperties() {
    DefaultHttpServerConfiguration configuration = new DefaultHttpServerConfiguration(
            HttpServerConfiguration.HTTP_SERVER_PORT.get(),
            HttpServerConfiguration.HTTP_SERVER_SECURE.get());
    configuration.setDocumentRoot(HttpServerConfiguration.DOCUMENT_ROOT.get());
    configuration.setKeystore(HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.get(), HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.get());

    return configuration;
  }

  /**
   * @return the server port
   */
  int getServerPort();

  /**
   * @return true if https is used
   */
  boolean isSecure();

  /**
   * @return the document root
   */
  String getDocumentRoot();

  /**
   * @return the keystore path
   */
  String getKeystorePath();

  /**
   * @return the keystore password
   */
  String getKeystorePassword();

  /**
   * @param documentRoot the document root
   */
  void setDocumentRoot(String documentRoot);

  /**
   * @param keystorePath the keystore path
   * @param keystorePassword the keystore password
   */
  void setKeystore(String keystorePath, String keystorePassword);
}

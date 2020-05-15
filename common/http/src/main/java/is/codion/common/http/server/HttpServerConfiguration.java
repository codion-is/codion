/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.http.server;

import dev.codion.common.Configuration;
import dev.codion.common.value.PropertyValue;

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
   * Specifies the keystore to use for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> HTTP_SERVER_KEYSTORE_PATH = Configuration.stringValue("codion.server.http.keyStore", null);

  /**
   * Specifies the password for the keystore used for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD = Configuration.stringValue("codion.server.http.keyStorePassword", null);

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> DOCUMENT_ROOT = Configuration.stringValue("codion.server.http.documentRoot", null);

  /**
   * Specifies whether a http server should use https.
   */
  enum Secure {
    /**
     * Https should be enabled.
     */
    YES,
    /**
     * Https should not be enabled.
     */
    NO
  }

  /**
   * Instantiates a new HttpServerConfiguration.
   * @param port the port on which to serve
   * @param secure yes if https should be used
   * @return a default configuration
   */
  static HttpServerConfiguration configuration(final int port, final Secure secure) {
    return new DefaultHttpServerConfiguration(port, secure);
  }

  /**
   * Parses configuration from system properties.
   * @return a server configuration according to system properties
   */
  static HttpServerConfiguration fromSystemProperties() {
    final DefaultHttpServerConfiguration configuration = new DefaultHttpServerConfiguration(
            HttpServerConfiguration.HTTP_SERVER_PORT.get(),
            HttpServerConfiguration.HTTP_SERVER_SECURE.get() ? Secure.YES : Secure.NO);
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

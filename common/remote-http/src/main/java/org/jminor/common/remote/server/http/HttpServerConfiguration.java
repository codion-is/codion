/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server.http;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;

/**
 * Configuration values for a {@link HttpServer}.
 */
public interface HttpServerConfiguration {

  /**
   * The port on which the http server is made available to clients.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  PropertyValue<Integer> HTTP_SERVER_PORT = Configuration.integerValue("jminor.server.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> HTTP_SERVER_SECURE = Configuration.booleanValue("jminor.server.http.secure", true);

  /**
   * Specifies the keystore to use for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> HTTP_SERVER_KEYSTORE_PATH = Configuration.stringValue("jminor.server.http.keyStore", null);

  /**
   * Specifies the password for the keystore used for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD = Configuration.stringValue("jminor.server.http.keyStorePassword", null);

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> DOCUMENT_ROOT = Configuration.stringValue("jminor.server.http.documentRoot", null);

  /**
   * Instantiates a new HttpServerConfiguration.
   * @param port the port on which to serve
   * @param secure true if https should be used
   * @return a default configuration
   */
  static HttpServerConfiguration configuration(final int port, final boolean secure) {
    return new DefaultHttpServerConfiguration(port, secure);
  }

  /**
   * Parses configuration from system properties.
   * @return a server configuration according to system properties
   */
  static HttpServerConfiguration fromSystemProperties() {
    return new DefaultHttpServerConfiguration(HttpServerConfiguration.HTTP_SERVER_PORT.get(),
            HttpServerConfiguration.HTTP_SERVER_SECURE.get())
            .setDocumentRoot(HttpServerConfiguration.DOCUMENT_ROOT.get())
            .setKeystore(HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.get(), HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.get());
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
   * @return this configuration instance
   */
  HttpServerConfiguration setDocumentRoot(String documentRoot);

  /**
   * @param keystorePath the keystore path
   * @param keystorePassword the keystore password
   * @return this configuration instance
   */
  HttpServerConfiguration setKeystore(String keystorePath, String keystorePassword);
}

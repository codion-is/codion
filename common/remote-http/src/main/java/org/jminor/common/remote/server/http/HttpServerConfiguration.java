/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server.http;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;

/**
 * Configuration values for a {@link HttpServer}.
 */
public final class HttpServerConfiguration {

  /**
   * The port on which the http server is made available to clients.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  public static final PropertyValue<Integer> HTTP_SERVER_PORT = Configuration.integerValue("jminor.server.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> HTTP_SERVER_SECURE = Configuration.booleanValue("jminor.server.http.secure", true);

  /**
   * Specifies the keystore to use for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PATH = Configuration.stringValue("jminor.server.http.keyStore", null);


  /**
   * Specifies the password for the keystore used for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD = Configuration.stringValue("jminor.server.http.keyStorePassword", null);

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  public static final PropertyValue<String> DOCUMENT_ROOT = Configuration.stringValue("jminor.server.http.documentRoot", null);

  private final int port;
  private final boolean secure;

  private String documentRoot;
  private String keystorePath;
  private String keystorePassword;

  /**
   * Instantiates a new HttpServerConfiguration.
   * @param port the port on which to serve
   * @param secure true if https should be used
   */
  public HttpServerConfiguration(final int port, final boolean secure) {
    this.port = port;
    this.secure = secure;
  }

  /**
   * @param documentRoot the document root, null to disable file serving
   * @return this configuration instance
   */
  public HttpServerConfiguration documentRoot(final String documentRoot) {
    this.documentRoot = documentRoot;
    return this;
  }

  /**
   * @param keystorePath the keystore path
   * @param keystorePassword the keystore password
   * @return this configuration instance
   */
  public HttpServerConfiguration keystore(final String keystorePath, final String keystorePassword) {
    this.keystorePath = keystorePath;
    this.keystorePassword = keystorePassword;
    return this;
  }

  /**
   * Parses configuration from system properties.
   * @return a server configuration according to system properties
   */
  public static HttpServerConfiguration fromSystemProperties() {
    return new HttpServerConfiguration(HTTP_SERVER_PORT.get(), HTTP_SERVER_SECURE.get())
            .documentRoot(DOCUMENT_ROOT.get()).keystore(HTTP_SERVER_KEYSTORE_PATH.get(), HTTP_SERVER_KEYSTORE_PASSWORD.get());
  }

  int getPort() {
    return port;
  }

  boolean isSecure() {
    return secure;
  }

  String getDocumentRoot() {
    return documentRoot;
  }

  String getKeystorePath() {
    return keystorePath;
  }

  String getKeystorePassword() {
    return keystorePassword;
  }
}

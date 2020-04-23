/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server.http;

final class DefaultHttpServerConfiguration implements HttpServerConfiguration {

  private final int serverPort;
  private final boolean secure;

  private String documentRoot;
  private String keystorePath;
  private String keystorePassword;

  DefaultHttpServerConfiguration(final int serverPort, final boolean secure) {
    this.serverPort = serverPort;
    this.secure = secure;
  }

  /**
   * @param documentRoot the document root, null to disable file serving
   * @return this configuration instance
   */
  @Override
  public DefaultHttpServerConfiguration setDocumentRoot(final String documentRoot) {
    this.documentRoot = documentRoot;
    return this;
  }

  /**
   * @param keystorePath the keystore path
   * @param keystorePassword the keystore password
   * @return this configuration instance
   */
  @Override
  public DefaultHttpServerConfiguration setKeystore(final String keystorePath, final String keystorePassword) {
    this.keystorePath = keystorePath;
    this.keystorePassword = keystorePassword;
    return this;
  }

  @Override
  public int getServerPort() {
    return serverPort;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public String getDocumentRoot() {
    return documentRoot;
  }

  @Override
  public String getKeystorePath() {
    return keystorePath;
  }

  @Override
  public String getKeystorePassword() {
    return keystorePassword;
  }
}

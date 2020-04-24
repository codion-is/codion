/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.http.server;

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

  @Override
  public void setDocumentRoot(final String documentRoot) {
    this.documentRoot = documentRoot;
  }

  @Override
  public void setKeystore(final String keystorePath, final String keystorePassword) {
    this.keystorePath = keystorePath;
    this.keystorePassword = keystorePassword;
  }
}

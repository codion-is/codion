/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.http.server;

final class DefaultHttpServerConfiguration implements HttpServerConfiguration {

  private final int serverPort;
  private final boolean secure;

  private String documentRoot;
  private String keystorePath;
  private String keystorePassword;

  DefaultHttpServerConfiguration(int serverPort, ServerHttps secure) {
    this.serverPort = serverPort;
    this.secure = secure == ServerHttps.TRUE;
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
  public void setDocumentRoot(String documentRoot) {
    this.documentRoot = documentRoot;
  }

  @Override
  public void setKeystore(String keystorePath, String keystorePassword) {
    this.keystorePath = keystorePath;
    this.keystorePassword = keystorePassword;
  }
}

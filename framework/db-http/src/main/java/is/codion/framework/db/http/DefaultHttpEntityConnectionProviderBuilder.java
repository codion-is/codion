/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.db.EntityConnectionProvider;

import static java.util.Objects.requireNonNull;

/**
 * Builds a {@link HttpEntityConnectionProvider} instance.
 * @see HttpEntityConnectionProvider#builder()
 */
public final class DefaultHttpEntityConnectionProviderBuilder
        extends AbstractBuilder<HttpEntityConnectionProvider, HttpEntityConnectionProvider.Builder>
        implements HttpEntityConnectionProvider.Builder {

  String hostName = HttpEntityConnection.HOSTNAME.get();
  int port = HttpEntityConnection.PORT.get();
  int securePort = HttpEntityConnection.SECURE_PORT.get();
  boolean https = HttpEntityConnection.SECURE.get();
  boolean json = HttpEntityConnection.JSON.get();
  int socketTimeout = HttpEntityConnection.SOCKET_TIMEOUT.get();
  int connectTimeout = HttpEntityConnection.CONNECT_TIMEOUT.get();

  public DefaultHttpEntityConnectionProviderBuilder() {
    super(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
  }

  @Override
  public HttpEntityConnectionProvider.Builder hostName(String hostName) {
    this.hostName = requireNonNull(hostName);
    return this;
  }

  @Override
  public HttpEntityConnectionProvider.Builder port(int port) {
    this.port = port;
    return this;
  }

  @Override
  public HttpEntityConnectionProvider.Builder securePort(int securePort) {
    this.securePort = securePort;
    return this;
  }

  @Override
  public HttpEntityConnectionProvider.Builder https(boolean https) {
    this.https = https;
    return this;
  }

  @Override
  public HttpEntityConnectionProvider.Builder json(boolean json) {
    this.json = json;
    return this;
  }

  @Override
  public HttpEntityConnectionProvider.Builder socketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
    return this;
  }

  @Override
  public HttpEntityConnectionProvider.Builder connectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  @Override
  public HttpEntityConnectionProvider build() {
    return new DefaultHttpEntityConnectionProvider(this);
  }
}

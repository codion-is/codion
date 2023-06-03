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

  String hostName = HttpEntityConnectionProvider.HTTP_CLIENT_HOSTNAME.get();
  int port = HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get();
  boolean https = HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get();
  boolean json = HttpEntityConnectionProvider.HTTP_CLIENT_JSON.get();

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
  public HttpEntityConnectionProvider build() {
    return new DefaultHttpEntityConnectionProvider(this);
  }
}

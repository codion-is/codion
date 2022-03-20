/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#builder()
 */
public final class DefaultHttpEntityConnectionProvider extends AbstractEntityConnectionProvider
        implements HttpEntityConnectionProvider{

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  private final String serverHostName;
  private final int serverPort;
  private final boolean https;
  private final boolean json;

  private DefaultHttpEntityConnectionProvider(DefaultBuilder builder) {
    super(builder);
    this.serverHostName = requireNonNull(builder.serverHostName, "serverHostName");
    this.serverPort = builder.serverPort;
    this.https = builder.https;
    this.json = builder.json;
  }

  @Override
  public String getConnectionType() {
    return CONNECTION_TYPE_HTTP;
  }

  @Override
  public String getDescription() {
    return getServerHostName();
  }

  public String getServerHostName() {
    return serverHostName;
  }

  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      if (https) {
        HttpEntityConnections.createSecureConnection(getDomainTypeName(getDomainClassName()), getServerHostName(),
                serverPort, getUser(), getClientTypeId(), getClientId(), json);
      }

      return HttpEntityConnections.createConnection(getDomainTypeName(getDomainClassName()), getServerHostName(),
              serverPort, getUser(), getClientTypeId(), getClientId(), json);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    connection.close();
  }

  public static final class DefaultBuilder extends AbstractBuilder<HttpEntityConnectionProvider.Builder, HttpEntityConnectionProvider>
          implements HttpEntityConnectionProvider.Builder {

    private String serverHostName = HTTP_CLIENT_HOST_NAME.get();
    private int serverPort = HTTP_CLIENT_PORT.get();
    private boolean https = ClientHttps.TRUE.equals(HTTP_CLIENT_SECURE.get());
    private boolean json = HTTP_CLIENT_JSON.get();

    public DefaultBuilder() {
      super(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    }

    @Override
    public HttpEntityConnectionProvider.Builder serverHostName(String serverHostName) {
      this.serverHostName = requireNonNull(serverHostName);
      return this;
    }

    @Override
    public HttpEntityConnectionProvider.Builder serverPort(int serverPort) {
      this.serverPort = serverPort;
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
}

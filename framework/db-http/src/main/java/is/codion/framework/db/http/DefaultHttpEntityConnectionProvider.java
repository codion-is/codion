/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#builder()
 */
final class DefaultHttpEntityConnectionProvider extends AbstractEntityConnectionProvider
        implements HttpEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  private final String hostName;
  private final int port;
  private final int securePort;
  private final boolean https;
  private final boolean json;
  private final int socketTimeout;
  private final int connectTimeout;

  DefaultHttpEntityConnectionProvider(DefaultHttpEntityConnectionProviderBuilder builder) {
    super(builder);
    this.hostName = requireNonNull(builder.hostName, "hostName");
    this.port = builder.port;
    this.securePort = builder.securePort;
    this.https = builder.https;
    this.json = builder.json;
    this.socketTimeout = builder.socketTimeout;
    this.connectTimeout = builder.connectTimeout;
  }

  @Override
  public String connectionType() {
    return CONNECTION_TYPE_HTTP;
  }

  @Override
  public String description() {
    return hostName;
  }

  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", user());
      return HttpEntityConnection.builder()
              .domainType(domainType())
              .hostName(hostName)
              .port(port)
              .securePort(securePort)
              .user(user())
              .clientTypeId(clientTypeId())
              .clientId(clientId())
              .json(json)
              .https(https)
              .socketTimeout(socketTimeout)
              .connectTimeout(connectTimeout)
              .build();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    connection.close();
  }
}

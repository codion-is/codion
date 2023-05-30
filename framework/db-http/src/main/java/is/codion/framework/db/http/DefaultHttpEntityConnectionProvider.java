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

  private final String serverHostName;
  private final int serverPort;
  private final boolean https;
  private final boolean json;

  DefaultHttpEntityConnectionProvider(DefaultHttpEntityConnectionProviderBuilder builder) {
    super(builder);
    this.serverHostName = requireNonNull(builder.serverHostName, "serverHostName");
    this.serverPort = builder.serverPort;
    this.https = builder.https;
    this.json = builder.json;
  }

  @Override
  public String connectionType() {
    return CONNECTION_TYPE_HTTP;
  }

  @Override
  public String description() {
    return serverHostName();
  }

  @Override
  public String serverHostName() {
    return serverHostName;
  }

  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", user());
      return HttpEntityConnection.builder()
              .domainTypeName(domainTypeName(domainClassName()))
              .serverHostName(serverHostName())
              .serverPort(serverPort)
              .user(user())
              .clientTypeId(clientTypeId())
              .clientId(clientId())
              .json(json)
              .https(https)
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

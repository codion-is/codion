/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.Configuration;
import org.jminor.common.User;
import org.jminor.common.Value;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * A class responsible for managing a httpConnection entity connection.
 */
public final class HttpEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  /**
   * The host on which to locate the http server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final Value<String> HTTP_SERVER_HOST_NAME = Configuration.stringValue("jminor.client.http.hostname", "localhost");

  /**
   * The port which the http client should use.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  public static final Value<Integer> HTTP_SERVER_PORT = Configuration.integerValue("jminor.client.http.port", 8080);

  private final String serverHostName;
  private final String domainId;
  private final Integer serverPort;
  private final String clientTypeId;
  private final UUID clientId;

  /**
   * Instantiates a new HttpEntityConnectionProvider.
   * @param domainId the id of the domain model
   * @param user the user to use when initializing connections
   * @param clientTypeId the client type id
   * @param clientId a UUID identifying the client
   * @see HttpEntityConnectionProvider#HTTP_SERVER_HOST_NAME
   * @see HttpEntityConnectionProvider#HTTP_SERVER_PORT
   */
  public HttpEntityConnectionProvider(final String domainId, final User user, final String clientTypeId,
                                      final UUID clientId) {
    this(domainId, HTTP_SERVER_HOST_NAME.get(), HTTP_SERVER_PORT.get(), user, clientTypeId, clientId);
  }

  /**
   * Instantiates a new HttpEntityConnectionProvider.
   * @param domainId the id of the domain model
   * @param serverHostName the server host name
   * @param serverPort the server port
   * @param user the user to use when initializing connections
   * @param clientTypeId the client type id
   * @param clientId a UUID identifying the client
   */
  public HttpEntityConnectionProvider(final String domainId, final String serverHostName, final Integer serverPort,
                                      final User user, final String clientTypeId, final UUID clientId) {
    super(user, false);
    this.domainId = domainId;
    this.serverHostName = Objects.requireNonNull(serverHostName, "serverHostName");
    this.serverPort = Objects.requireNonNull(serverPort, "serverPort");
    this.clientTypeId = Objects.requireNonNull(clientTypeId, "clientTypeId");
    this.clientId = Objects.requireNonNull(clientId, "clientId");
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnection.Type getConnectionType() {
    return EntityConnection.Type.HTTP;
  }

  /**
   * @return a string describing the server connection
   */
  @Override
  public String getDescription() {
    if (!isConnectionValid()) {
      return serverHostName + " - " + Messages.get(Messages.NOT_CONNECTED);
    }

    return serverHostName;
  }

  /** {@inheritDoc} */
  @Override
  public String getServerHostName() {
    return serverHostName;
  }

  /**
   * @return the client ID
   */
  public UUID getClientId() {
    return clientId;
  }

  /** {@inheritDoc} */
  @Override
  protected Entities initializeEntities() {
    return connect().getEntities().registerDomain();
  }

  /** {@inheritDoc} */
  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return HttpEntityConnections.createConnection(domainId, serverHostName, serverPort, getUser(),
              clientTypeId, clientId);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void disconnect(final EntityConnection connection) {
    connection.disconnect();
  }
}

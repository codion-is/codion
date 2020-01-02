/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.Configuration;
import org.jminor.common.i18n.Messages;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_HOST_NAME
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_PORT
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_SECURE
 */
public final class HttpEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  /**
   * The host on which to locate the http server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final PropertyValue<String> HTTP_CLIENT_HOST_NAME = Configuration.stringValue("jminor.client.http.hostname", "localhost");

  /**
   * The port which the http client should use.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  public static final PropertyValue<Integer> HTTP_CLIENT_PORT = Configuration.integerValue("jminor.client.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value types: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> HTTP_CLIENT_SECURE = Configuration.booleanValue("jminor.client.http.secure", true);

  private String serverHostName;
  private Integer serverPort;
  private Boolean https;

  /**
   * Instantiates a new HttpEntityConnectionProvider.
   */
  public HttpEntityConnectionProvider() {}

  /**
   * Instantiates a new HttpEntityConnectionProvider.
   * @param serverHostName the server host name
   * @param serverPort the server port
   * @param https true if https should be used
   */
  public HttpEntityConnectionProvider(final String serverHostName, final Integer serverPort, final Boolean https) {
    this.serverHostName = requireNonNull(serverHostName, "serverHostName");
    this.serverPort = requireNonNull(serverPort, "serverPort");
    this.https = requireNonNull(https, "https");
  }

  /** {@inheritDoc} */
  @Override
  public String getConnectionType() {
    return CONNECTION_TYPE_HTTP;
  }

  /**
   * @return a string describing the server connection
   */
  @Override
  public String getDescription() {
    if (!isConnectionValid()) {
      return getServerHostName() + " - " + Messages.get(Messages.NOT_CONNECTED);
    }

    return getServerHostName();
  }

  /**
   * @return the name of the host of the server providing the connection
   */
  public String getServerHostName() {
    if (serverHostName == null) {
      serverHostName = HTTP_CLIENT_HOST_NAME.get();
    }

    return serverHostName;
  }

  /** {@inheritDoc} */
  @Override
  protected HttpEntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return HttpEntityConnections.createConnection(getDomainId(getDomainClassName()), getServerHostName(),
              getServerPort(), getHttps(), getUser(), getClientTypeId(), getClientId());
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

  private Integer getServerPort() {
    if (serverPort == null) {
      serverPort = HTTP_CLIENT_PORT.get();
    }

    return serverPort;
  }

  private Boolean getHttps() {
    if (https == null) {
      https = HTTP_CLIENT_SECURE.get();
    }

    return https;
  }
}

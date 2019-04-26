/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.Configuration;
import org.jminor.common.Value;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.db.AbstractEntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A class responsible for managing a httpConnection entity connection.
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_HOST_NAME
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_PORT
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_SECURE
 */
public final class HttpEntityConnectionProvider extends AbstractEntityConnectionProvider<HttpEntityConnection> {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  /**
   * The host on which to locate the http server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final Value<String> HTTP_CLIENT_HOST_NAME = Configuration.stringValue("jminor.client.http.hostname", "localhost");

  /**
   * The port which the http client should use.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  public static final Value<Integer> HTTP_CLIENT_PORT = Configuration.integerValue("jminor.client.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value types: Boolean<br>
   * Default value: true
   */
  public static final Value<Boolean> HTTP_CLIENT_SECURE = Configuration.booleanValue("jminor.client.http.secure", true);

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
    this.serverHostName = Objects.requireNonNull(serverHostName, "serverHostName");
    this.serverPort = Objects.requireNonNull(serverPort, "serverPort");
    this.https = Objects.requireNonNull(https, "https");
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

  /** {@inheritDoc} */
  @Override
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
  protected void disconnect(final HttpEntityConnection connection) {
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

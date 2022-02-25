/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_HOST_NAME
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_PORT
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_SECURE
 * @see HttpEntityConnectionProvider#HTTP_CLIENT_JSON
 */
public final class HttpEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionProvider.class);

  /**
   * The host on which to locate the http server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final PropertyValue<String> HTTP_CLIENT_HOST_NAME = Configuration.stringValue("codion.client.http.hostname", "localhost");

  /**
   * The port which the http client should use.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  public static final PropertyValue<Integer> HTTP_CLIENT_PORT = Configuration.integerValue("codion.client.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value types: Https<br>
   * Default value: true
   */
  public static final PropertyValue<ClientHttps> HTTP_CLIENT_SECURE = Configuration.enumValue("codion.client.http.secure", ClientHttps.class, ClientHttps.TRUE);

  /**
   * Specifies whether json serialization should be used.<br>
   * Value types: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> HTTP_CLIENT_JSON = Configuration.booleanValue("codion.client.http.json", true);

  private String serverHostName;
  private Integer serverPort;
  private Boolean https;
  private Boolean json;

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
  public HttpEntityConnectionProvider(final String serverHostName, final Integer serverPort, final ClientHttps https) {
    this.serverHostName = requireNonNull(serverHostName, "serverHostName");
    this.serverPort = requireNonNull(serverPort, "serverPort");
    this.https = requireNonNull(https, "https").equals(ClientHttps.TRUE);
  }

  @Override
  public String getConnectionType() {
    return CONNECTION_TYPE_HTTP;
  }

  /**
   * @return a string describing the server connection
   */
  @Override
  public String getDescription() {
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

  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      if (getHttps()) {
        HttpEntityConnections.createSecureConnection(getDomainTypeName(getDomainClassName()), getServerHostName(),
              getServerPort(), getUser(), getClientTypeId(), getClientId(), getJson());
      }

      return HttpEntityConnections.createConnection(getDomainTypeName(getDomainClassName()), getServerHostName(),
              getServerPort(), getUser(), getClientTypeId(), getClientId(), getJson());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(final EntityConnection connection) {
    connection.close();
  }

  private Integer getServerPort() {
    if (serverPort == null) {
      serverPort = HTTP_CLIENT_PORT.get();
    }

    return serverPort;
  }

  private Boolean getHttps() {
    if (https == null) {
      https = ClientHttps.TRUE.equals(HTTP_CLIENT_SECURE.get());
    }

    return https;
  }

  private Boolean getJson() {
    if (json == null) {
      json = HTTP_CLIENT_JSON.get();
    }

    return json;
  }
}

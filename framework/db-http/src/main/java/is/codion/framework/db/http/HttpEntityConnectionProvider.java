/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#builder()
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

  private final String serverHostName;
  private final int serverPort;
  private final boolean https;
  private final boolean json;

  private HttpEntityConnectionProvider(DefaultBuilder builder) {
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
    return serverHostName;
  }

  /**
   * Instantiates a new builder instance.
   * @return a new builder
   */
  public static Builder builder() {
    return new DefaultBuilder();
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

  /**
   * Builds a {@link HttpEntityConnectionProvider} instance.
   */
  public interface Builder extends EntityConnectionProvider.Builder<Builder, HttpEntityConnectionProvider> {

    /**
     * @param serverHostName the server host name
     * @return this builder instance
     */
    Builder serverHostName(String serverHostName);

    /**
     * @param serverPort the server port
     * @return this builder instance
     */
    Builder serverPort(int serverPort);

    /**
     * @param https true if https should be enabled
     * @return this builder instance
     */
    Builder https(boolean https);

    /**
     * @param json true if json serialization should be used
     * @return this builder instance
     */
    Builder json(boolean json);
  }

  public static final class DefaultBuilder extends AbstractBuilder<HttpEntityConnectionProvider.Builder, HttpEntityConnectionProvider> implements Builder {

    private String serverHostName = HTTP_CLIENT_HOST_NAME.get();
    private int serverPort = HTTP_CLIENT_PORT.get();
    private boolean https = ClientHttps.TRUE.equals(HTTP_CLIENT_SECURE.get());
    private boolean json = HTTP_CLIENT_JSON.get();

    public DefaultBuilder() {
      super(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    }

    @Override
    public Builder serverHostName(String serverHostName) {
      this.serverHostName = requireNonNull(serverHostName);
      return this;
    }

    @Override
    public Builder serverPort(int serverPort) {
      this.serverPort = serverPort;
      return this;
    }

    @Override
    public Builder https(boolean https) {
      this.https = https;
      return this;
    }

    @Override
    public Builder json(boolean json) {
      this.json = json;
      return this;
    }

    @Override
    public HttpEntityConnectionProvider build() {
      return new HttpEntityConnectionProvider(this);
    }
  }
}

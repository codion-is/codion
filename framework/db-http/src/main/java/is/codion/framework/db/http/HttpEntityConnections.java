/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;

import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * A factory class for http based EntityConnection builder.
 */
public final class HttpEntityConnections {

  private HttpEntityConnections() {}

  /**
   * @return a new builder instance
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  /**
   * Builds a http based EntityConnection
   */
  public interface Builder {

    /**
     * @param domainTypeName the name of the domain model type
     * @return this builder instance
     */
    Builder domainTypeName(String domainTypeName);

    /**
     * @param serverHostName the http server host name
     * @return this builder instance
     */
    Builder serverHostName(String serverHostName);

    /**
     * @param serverPort the http server port
     * @return this builder instance
     */
    Builder serverPort(int serverPort);

    /**
     * @param https true if https should be used
     * @return this builder instance
     */
    Builder https(boolean https);

    /**
     * @param json true if json serialization should be used
     * @return this builder instance
     */
    Builder json(boolean json);

    /**
     * @param user the user
     * @return this builder instance
     */
    Builder user(User user);

    /**
     * @param clientTypeId the client type id
     * @return this builder instance
     */
    Builder clientTypeId(String clientTypeId);

    /**
     * @param clientId the client id
     * @return this builder instance
     */
    Builder clientId(UUID clientId);

    /**
     * @return a http based EntityConnection
     */
    EntityConnection build();
  }

  private static final class DefaultBuilder implements Builder {

    private String domainTypeName;
    private String serverHostName = HttpEntityConnectionProvider.HTTP_CLIENT_HOSTNAME.get();
    private int serverPort = HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get();
    private boolean https = HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get();
    private boolean json = HttpEntityConnectionProvider.HTTP_CLIENT_JSON.get();
    private User user;
    private String clientTypeId;
    private UUID clientId;

    @Override
    public Builder domainTypeName(String domainTypeName) {
      this.domainTypeName = requireNonNull(domainTypeName);
      return this;
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
    public Builder user(User user) {
      this.user = requireNonNull(user);
      return this;
    }

    @Override
    public Builder clientTypeId(String clientTypeId) {
      this.clientTypeId = requireNonNull(clientTypeId);
      return this;
    }

    @Override
    public Builder clientId(UUID clientId) {
      this.clientId = requireNonNull(clientId);
      return this;
    }

    @Override
    public EntityConnection build() {
      if (json) {
        return new HttpJsonEntityConnection(domainTypeName, serverHostName, serverPort, https, user, clientTypeId, clientId, new BasicHttpClientConnectionManager());
      }

      return new HttpEntityConnection(domainTypeName, serverHostName, serverPort, https, user, clientTypeId, clientId, new BasicHttpClientConnectionManager());
    }
  }
}

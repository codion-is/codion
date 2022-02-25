/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates information about a client required by a server for establishing a connection
 */
public interface ConnectionRequest {

  /**
   * @return the user
   */
  User getUser();

  /**
   * @return the client id
   */
  UUID getClientId();

  /**
   * @return the client type id
   */
  String getClientTypeId();

  /**
   * @return the client version
   */
  Version getClientVersion();

  /**
   * @return the version of Codion the client is using
   */
  Version getFrameworkVersion();

  /**
   * @return misc. parameters, an empty map if none are specified
   */
  Map<String, Object> getParameters();

  /**
   * @return a ConnectionRequest.Builder
   */
  static ConnectionRequest.Builder builder() {
    return new ConnectionRequestBuilder();
  }

  /**
   * A builder for ConnectionRequest
   */
  interface Builder {

    /**
     * @param user the user
     * @return this Builder instance
     */
    Builder user(User user);

    /**
     * @param clientId the client id
     * @return this Builder instance
     */
    Builder clientId(UUID clientId);

    /**
     * @param clientTypeId the client type id
     * @return this Builder instance
     */
    Builder clientTypeId(String clientTypeId);

    /**
     * @param clientVersion the client version
     * @return this Builder instance
     */
    Builder clientVersion(Version clientVersion);

    /**
     * @param key the key
     * @param value the value
     * @return this Builder instance
     */
    Builder parameter(String key, Object value);

    /**
     * @return a new ConnectionRequest instance
     */
    ConnectionRequest build();
  }

  final class ConnectionRequestBuilder implements Builder {

    private User user;
    private UUID clientId = UUID.randomUUID();
    private String clientTypeId;
    private Version clientVersion;
    private Map<String, Object> parameters;

    @Override
    public Builder user(User user) {
      this.user = requireNonNull(user);
      return this;
    }

    @Override
    public Builder clientId(UUID clientId) {
      this.clientId = requireNonNull(clientId);
      return this;
    }

    @Override
    public Builder clientTypeId(String clientTypeId) {
      this.clientTypeId = requireNonNull(clientTypeId);
      return this;
    }

    @Override
    public Builder clientVersion(Version clientVersion) {
      this.clientVersion = clientVersion;
      return this;
    }

    @Override
    public Builder parameter(String key, Object value) {
      if (parameters == null) {
        parameters = new HashMap<>();
      }
      parameters.put(key, value);
      return this;
    }

    @Override
    public ConnectionRequest build() {
      return new DefaultConnectionRequest(user, clientId, clientTypeId, clientVersion, Version.getVersion(), parameters);
    }
  }
}

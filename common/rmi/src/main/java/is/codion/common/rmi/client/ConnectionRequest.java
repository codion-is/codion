/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.util.Map;
import java.util.UUID;

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
    return new DefaultConnectionRequest.DefaultBuilder();
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
}

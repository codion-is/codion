/*
 * Copyright (c) 2015 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  User user();

  /**
   * @return the client id
   */
  UUID clientId();

  /**
   * @return the client type id
   */
  String clientTypeId();

  /**
   * @return the client version
   */
  Version clientVersion();

  /**
   * @return the version of Codion the client is using
   */
  Version frameworkVersion();

  /**
   * @return misc. parameters, an empty map if none are specified
   */
  Map<String, Object> parameters();

  /**
   * @return a copy of this connection request with a copy of the user instance
   */
  ConnectionRequest copy();

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

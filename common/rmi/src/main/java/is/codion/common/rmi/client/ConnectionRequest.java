/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @return a ConnectionRequest
   */
  static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId) {
    return connectionRequest(user, clientId, clientTypeId, null);
  }

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @param parameters misc. parameters, values must implement {@link java.io.Serializable}
   * @return a ConnectionRequest
   */
  static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                             final Map<String, Object> parameters) {
    return connectionRequest(user, clientId, clientTypeId, null, parameters);
  }

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @param clientVersion the client application version
   * @param parameters misc. parameters, values must implement {@link java.io.Serializable}
   * @return a ConnectionRequest
   */
  static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                             final Version clientVersion, final Map<String, Object> parameters) {
    return new DefaultConnectionRequest(user, clientId, clientTypeId, clientVersion, Version.getVersion(), parameters);
  }
}

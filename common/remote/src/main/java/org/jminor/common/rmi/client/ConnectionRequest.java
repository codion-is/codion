/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.rmi.client;

import org.jminor.common.user.User;
import org.jminor.common.version.Version;
import org.jminor.common.version.Versions;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Encapsulates information about a client required by a server for establishing a connection
 */
public interface ConnectionRequest extends Serializable {

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
   * @return the version of JMinor the client is using
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
   * @param parameters misc. parameters, values must implement {@link Serializable}
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
   * @param parameters misc. parameters, values must implement {@link Serializable}
   * @return a ConnectionRequest
   */
  static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                             final Version clientVersion, final Map<String, Object> parameters) {
    return new DefaultConnectionRequest(user, clientId, clientTypeId, clientVersion, Versions.getVersion(), parameters);
  }
}

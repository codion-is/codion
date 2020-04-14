/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.client;

import org.jminor.common.user.User;
import org.jminor.common.version.Version;
import org.jminor.common.version.Versions;

import java.util.Map;
import java.util.UUID;

/**
 * Utility methods for remote clients
 */
public final class Clients {

  private Clients() {}

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientId the client id
   * @param clientTypeId the client type id
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId) {
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
  public static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
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
  public static ConnectionRequest connectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                                    final Version clientVersion, final Map<String, Object> parameters) {
    return new DefaultConnectionRequest(user, clientId, clientTypeId, clientVersion, Versions.getVersion(), parameters);
  }
}

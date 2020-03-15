/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.user.User;
import org.jminor.common.version.Version;
import org.jminor.common.version.Versions;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

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

  private static final class DefaultConnectionRequest implements ConnectionRequest {

    private static final long serialVersionUID = 1;

    private final User user;
    private final UUID clientId;
    private final String clientTypeId;
    private final Version clientVersion;
    private final Version frameworkVersion;
    private final Map<String, Object> parameters;

    private DefaultConnectionRequest(final User user, final UUID clientId, final String clientTypeId,
                                     final Version clientVersion, final Version frameworkVersion,
                                     final Map<String, Object> parameters) {
      this.user = requireNonNull(user, "user");
      this.clientId = requireNonNull(clientId, "clientId");
      this.clientTypeId = requireNonNull(clientTypeId, "clientTypeId");
      this.clientVersion = clientVersion;
      this.frameworkVersion = frameworkVersion;
      this.parameters = parameters;
    }

    @Override
    public User getUser() {
      return user;
    }

    @Override
    public UUID getClientId() {
      return clientId;
    }

    @Override
    public String getClientTypeId() {
      return clientTypeId;
    }

    @Override
    public Version getClientVersion() {
      return clientVersion;
    }

    @Override
    public Version getFrameworkVersion() {
      return frameworkVersion;
    }

    @Override
    public Map<String, Object> getParameters() {
      return parameters == null ? Collections.emptyMap() : parameters;
    }

    @Override
    public boolean equals(final Object obj) {
      return this == obj || obj instanceof ConnectionRequest && clientId.equals(((ConnectionRequest) obj).getClientId());
    }

    @Override
    public int hashCode() {
      return clientId.hashCode();
    }

    @Override
    public String toString() {
      return user.toString() + " [" + clientTypeId + "] - " + clientId.toString();
    }
  }
}

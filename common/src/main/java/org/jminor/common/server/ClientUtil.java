/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.jminor.common.model.Version;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility methods for remote clients
 */
public final class ClientUtil {

  private ClientUtil() {}

  /**
   * Instantiates a ConnectionInfo
   * @param user the user
   * @param clientID the client id
   * @param clientTypeID the client type id
   * @return a ConnectionInfo
   */
  public static ConnectionInfo connectionInfo(final User user, final UUID clientID, final String clientTypeID) {
    return connectionInfo(user, clientID, clientTypeID, null);
  }

  /**
   * Instantiates a ConnectionInfo
   * @param user the user
   * @param clientID the client id
   * @param clientTypeID the client type id
   * @param clientVersion the client application version
   * @return a ConnectionInfo
   */
  public static ConnectionInfo connectionInfo(final User user, final UUID clientID, final String clientTypeID,
                                              final Version clientVersion) {
    return new DefaultConnectionInfo(user, clientID, clientTypeID, clientVersion, Version.getVersion());
  }

  private static final class DefaultConnectionInfo implements ConnectionInfo, Serializable {

    private static final long serialVersionUID = 1;

    private final User user;
    private final UUID clientID;
    private final String clientTypeID;
    private final Version clientVersion;
    private final Version frameworkVersion;

    private DefaultConnectionInfo(final User user, final UUID clientID, final String clientTypeID,
                                  final Version clientVersion, final Version frameworkVersion) {
      this.user = Objects.requireNonNull(user, "user");
      this.clientID = Objects.requireNonNull(clientID, "clientID");
      this.clientTypeID = Objects.requireNonNull(clientTypeID, "clientTypeID");
      this.clientVersion = clientVersion;
      this.frameworkVersion = frameworkVersion;
    }

    @Override
    public User getUser() {
      return user;
    }

    @Override
    public UUID getClientID() {
      return clientID;
    }

    @Override
    public String getClientTypeID() {
      return clientTypeID;
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
    public boolean equals(final Object obj) {
      return this == obj || obj instanceof ConnectionInfo && clientID.equals(((ConnectionInfo) obj).getClientID());
    }

    @Override
    public int hashCode() {
      return clientID.hashCode();
    }

    @Override
    public String toString() {
      if (user == null) {
        return clientID.toString();
      }

      return user.toString() + " [" + clientTypeID + "] - " + clientID.toString();
    }
  }
}

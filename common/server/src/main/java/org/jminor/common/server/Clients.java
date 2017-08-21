/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;
import org.jminor.common.Version;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility methods for remote clients
 */
public final class Clients {

  private Clients() {}

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientID the client id
   * @param clientTypeID the client type id
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientID, final String clientTypeID) {
    return connectionRequest(user, clientID, clientTypeID, null);
  }

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientID the client id
   * @param clientTypeID the client type id
   * @param parameters misc. parameters, values must implement {@link java.io.Serializable}
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientID, final String clientTypeID,
                                                    final Map<String, Object> parameters) {
    return connectionRequest(user, clientID, clientTypeID, null, parameters);
  }

  /**
   * Instantiates a ConnectionRequest
   * @param user the user
   * @param clientID the client id
   * @param clientTypeID the client type id
   * @param clientVersion the client application version
   * @param parameters misc. parameters, values must implement {@link java.io.Serializable}
   * @return a ConnectionRequest
   */
  public static ConnectionRequest connectionRequest(final User user, final UUID clientID, final String clientTypeID,
                                                    final Version clientVersion, final Map<String, Object> parameters) {
    return new DefaultConnectionRequest(user, clientID, clientTypeID, clientVersion, Version.getVersion(), parameters);
  }

  /**
   * Performs a authentication lookup on localhost via a {@link CredentialServer}.
   * @param authenticationToken the authentication token
   * @return the User credentials associated with the {@code authenticationToken}, null if the user credentials
   * have expired or if no authentication server is running
   * @see CredentialServer
   */
  public static User getUserCredentials(final UUID authenticationToken) {
    try {
      final Remote credentialService = Servers.getRegistry(Registry.REGISTRY_PORT).lookup(CredentialService.class.getSimpleName());

      return ((CredentialService) credentialService).getUser(authenticationToken);
    }
    catch (NotBoundException | RemoteException e) {
      //no credential server available or not reachable
      return null;
    }
  }

  private static final class DefaultConnectionRequest implements ConnectionRequest {

    private static final long serialVersionUID = 1;

    private final User user;
    private final UUID clientID;
    private final String clientTypeID;
    private final Version clientVersion;
    private final Version frameworkVersion;
    private final Map<String, Object> parameters;

    private DefaultConnectionRequest(final User user, final UUID clientID, final String clientTypeID,
                                     final Version clientVersion, final Version frameworkVersion,
                                     final Map<String, Object> parameters) {
      this.user = Objects.requireNonNull(user, "user");
      this.clientID = Objects.requireNonNull(clientID, "clientID");
      this.clientTypeID = Objects.requireNonNull(clientTypeID, "clientTypeID");
      this.clientVersion = clientVersion;
      this.frameworkVersion = frameworkVersion;
      this.parameters = parameters;
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
    public Map<String, Object> getParameters() {
      return parameters;
    }

    @Override
    public boolean equals(final Object obj) {
      return this == obj || obj instanceof ConnectionRequest && clientID.equals(((ConnectionRequest) obj).getClientID());
    }

    @Override
    public int hashCode() {
      return clientID.hashCode();
    }

    @Override
    public String toString() {
      return user.toString() + " [" + clientTypeID + "] - " + clientID.toString();
    }
  }
}

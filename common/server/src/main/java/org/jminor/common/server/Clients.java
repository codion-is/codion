/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;
import org.jminor.common.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility methods for remote clients
 */
public final class Clients {

  private static final Logger LOG = LoggerFactory.getLogger(Clients.class);

  public static final String AUTHENTICATION_TOKEN_PREFIX = "authenticationToken";
  public static final String AUTHENTICATION_TOKEN_DELIMITER = ":";

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
    return new DefaultConnectionRequest(user, clientId, clientTypeId, clientVersion, Version.getVersion(), parameters);
  }

  /**
   * Performs a authentication lookup on localhost via a {@link CredentialServer}.
   * @param programArguments the arguments list in which to search for the autentication token [authenticationToke:123-123-123]
   * @return the User credentials associated with the {@code authenticationToken}, null if no token was found in the
   * arguments list, if the user credentials have expired or if no authentication server is running
   * @see CredentialServer
   */
  public static User getUserCredentials(final String[] programArguments) {
    final UUID token = getAuthenticationToken(programArguments);

    return token == null ? null : getUserCredentials(token);
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
      LOG.debug("CredentialService found: " + credentialService);

      return ((CredentialService) credentialService).getUser(Objects.requireNonNull(authenticationToken, "authenticationToken"));
    }
    catch (final NotBoundException | RemoteException e) {
      LOG.debug("No CredentialService found", e);
      //no credential server available or not reachable
      return null;
    }
  }

  private static UUID getAuthenticationToken(final String[] args) {
    LOG.debug("getAuthenticationToken() args: " + Arrays.toString(args));
    if (args == null) {
      return null;
    }

    final UUID token = Arrays.stream(args).filter(Clients::isAuthenticationToken).findFirst()
            .map(Clients::getAuthenticationToken).orElse(null);
    //keep old WebStart method for backwards compatibility
    if (token == null && args.length > 1 && "-open".equals(args[0])) {
      return UUID.fromString(args[1]);
    }

    return token;
  }

  private static boolean isAuthenticationToken(final String argument) {
    return argument.startsWith(AUTHENTICATION_TOKEN_PREFIX + AUTHENTICATION_TOKEN_DELIMITER);
  }

  private static UUID getAuthenticationToken(final String argument) {
    return UUID.fromString(argument.split(AUTHENTICATION_TOKEN_DELIMITER)[1]);
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
      this.user = Objects.requireNonNull(user, "user");
      this.clientId = Objects.requireNonNull(clientId, "clientId");
      this.clientTypeId = Objects.requireNonNull(clientTypeId, "clientTypeId");
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

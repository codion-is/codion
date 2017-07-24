/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.TaskScheduler;
import org.jminor.common.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A simple credential server for one-time authentication tokens for applications running on localhost.
 * Setting the following before the server is constructed is recommended:
 * {@code System.setProperty("java.rmi.server.hostname", CredentialServer.LOCALHOST);}
 * <pre>
 * {@code
 * CredentialServer credentialServer = new CredentialServer(12345, 30000, 60000);
 * String jnlpUrl = getApplicationJNLPUrl();
 * UUID token = UUID.randomUUID();
 * credentialServer.addAuthenticationToken(token, user);
 * new ProcessBuilder().command("javaws", "-open", token.toString(), jnlpUrl).start();
 * }
 * </pre>
 * @see ClientUtil#getUserCredentials(UUID)
 */
public final class CredentialServer extends UnicastRemoteObject implements CredentialService {

  private static final Logger LOG = LoggerFactory.getLogger(CredentialServer.class);

  public static final String LOCALHOST = "127.0.0.1";

  private final Map<UUID, UserExpiration> authenticationTokens = Collections.synchronizedMap(new HashMap<>());

  private final TaskScheduler expiredCleaner;

  private final int tokenValidity;

  /**
   * Starts a server on the given port
   * @param port the port
   * @param tokenValidity the number of milliseconds a token is valid
   * @param cleanupInterval the expired token cleanup interval in milliseconds
   * @throws RemoteException in case of a communication error
   * @throws AlreadyBoundException if a credential server is already running
   */
  public CredentialServer(final int port, final int tokenValidity, final int cleanupInterval) throws AlreadyBoundException, RemoteException {
    super(port);
    this.tokenValidity = tokenValidity;
    this.expiredCleaner = new TaskScheduler(this::removeExpired, cleanupInterval, TimeUnit.MILLISECONDS).start();
    ServerUtil.initializeRegistry(Registry.REGISTRY_PORT).bind(CredentialService.class.getSimpleName(), this);
  }

  /**
   * @param authenticationToken the one-time token
   * @param user the user credentials associated with the token
   */
  public void addAuthenticationToken(final UUID authenticationToken, final User user) {
    synchronized (authenticationTokens) {
      authenticationTokens.put(authenticationToken, new UserExpiration(user, System.currentTimeMillis() + tokenValidity));
    }
  }

  /** {@inheritDoc} */
  @Override
  public User getUser(final UUID authenticationToken) throws RemoteException {
    try {
      final String clientHost = getClientHost();
      if (!Objects.equals(clientHost, LOCALHOST)) {
        LOG.debug("Request denied, clientHost should be " + LOCALHOST + " but was: " + clientHost);
        return null;
      }

      synchronized (authenticationTokens) {
        final UserExpiration userExpiration = authenticationTokens.remove(authenticationToken);
        if (userExpiration == null || userExpiration.isExpired()) {
          LOG.debug("Request failed, authentication token: " + authenticationToken + " invalid or expired");

          return null;
        }

        return userExpiration.user;
      }
    }
    catch (final ServerNotActiveException e) {
      //not able to get request host
      return null;
    }
  }

  /**
   * Removes this server from the registry
   */
  public void exit() {
    try {
      expiredCleaner.stop();
      synchronized (authenticationTokens) {
        ServerUtil.getRegistry(Registry.REGISTRY_PORT).unbind(CredentialService.class.getSimpleName());
        authenticationTokens.clear();
      }
    }
    catch (final Exception e) {
      LOG.error("Error on exit", e);
    }
  }

  private void removeExpired() {
    synchronized (authenticationTokens) {
      for (final Map.Entry<UUID, UserExpiration> entry : new ArrayList<>(authenticationTokens.entrySet())) {
        if (entry.getValue().isExpired()) {
          authenticationTokens.remove(entry.getKey());
          LOG.debug("Expired token removed for user: " + entry.getValue().user);
        }
      }
    }
  }

  private static final class UserExpiration {

    private final User user;
    private final long expires;

    private UserExpiration(final User user, final long expires) {
      this.user = user;
      this.expires = expires;
    }

    private boolean isExpired() {
      return System.currentTimeMillis() > expires;
    }
  }
}

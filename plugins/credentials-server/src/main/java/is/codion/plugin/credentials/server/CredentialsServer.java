/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.credentials.CredentialsProvider;
import is.codion.common.rmi.server.Server;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A credentials server for one-time authentication tokens for applications running on localhost.
 * Setting the following before the server is constructed is recommended:
 * {@code System.setProperty("java.rmi.server.hostname", CredentialServer.LOCALHOST);}
 * <pre>
 * {@code
 * CredentialsServer credentialsServer = CredentialsServer.credentialsServer(12345, 30000, 60000);
 * String jnlpUrl = getApplicationJNLPUrl();
 * UUID token = UUID.randomUUID();
 * credentialServer.addAuthenticationToken(token, user);
 * new ProcessBuilder().command("javaws", "-open", token.toString(), jnlpUrl).start();
 * }
 * </pre>
 * For instances use the {@link #credentialsServer(int, int, int)} or {@link #credentialsServer(int, int, int, int)} factory methods.
 * @see CredentialsProvider#credentials(UUID)
 * @see #credentialsServer(int, int, int)
 * @see #credentialsServer(int, int, int, int)
 */
public final class CredentialsServer extends UnicastRemoteObject implements CredentialsService {

  private static final Logger LOG = LoggerFactory.getLogger(CredentialsServer.class);

  private static final long serialVersionUID = 1;

  public static final String LOCALHOST = "127.0.0.1";

  private final Registry registry;
  private final Map<UUID, UserExpiration> authenticationTokens = new ConcurrentHashMap<>();
  private final TaskScheduler expiredCleaner;
  private final int tokenValidity;

  private CredentialsServer(int port, int registryPort, int tokenValidity,
                            int cleanupInterval) throws AlreadyBoundException, RemoteException {
    super(port);
    this.tokenValidity = tokenValidity;
    this.expiredCleaner = TaskScheduler.builder(this::removeExpired)
            .interval(cleanupInterval, TimeUnit.MILLISECONDS)
            .start();
    this.registry = Server.Locator.registry(registryPort);
    this.registry.bind(CredentialsService.class.getSimpleName(), this);
  }

  /**
   * Adds an authenticationToken associated with the given user, with the default token validity period
   * @param authenticationToken the one-time token
   * @param user the user credentials associated with the token
   */
  public void addAuthenticationToken(UUID authenticationToken, User user) {
    authenticationTokens.put(authenticationToken, new UserExpiration(user, System.currentTimeMillis() + tokenValidity));
  }

  @Override
  public User user(UUID authenticationToken) throws RemoteException {
    try {
      String clientHost = getClientHost();
      if (!Objects.equals(clientHost, LOCALHOST)) {
        LOG.debug("Request denied, clientHost should be " + LOCALHOST + " but was: " + clientHost);
        return null;
      }

      UserExpiration userExpiration = authenticationTokens.remove(authenticationToken);
      if (userExpiration == null || userExpiration.isExpired()) {
        LOG.debug("Request failed, authentication token: " + authenticationToken + " invalid or expired");

        return null;
      }

      return userExpiration.user;
    }
    catch (ServerNotActiveException e) {
      LOG.debug("Request denied, unable to get request host", e);
      return null;
    }
  }

  /**
   * Removes this server from the registry
   */
  public void exit() {
    try {
      expiredCleaner.stop();
      authenticationTokens.clear();
      registry.unbind(CredentialsService.class.getSimpleName());
      UnicastRemoteObject.unexportObject(registry, true);
    }
    catch (Exception e) {
      LOG.error("Error on exit", e);
    }
  }

  /**
   * Creates and starts a server on the given port
   * @param port the port
   * @param tokenValidity the number of milliseconds a token is valid
   * @param cleanupInterval the expired token cleanup interval in milliseconds
   * @return a new {@link CredentialsServer} instance
   * @throws RemoteException in case of a communication error
   * @throws AlreadyBoundException if a credential server is already running
   */
  public static CredentialsServer credentialsServer(int port, int tokenValidity,
                                                    int cleanupInterval) throws AlreadyBoundException, RemoteException {
    return credentialsServer(port, CredentialsService.REGISTRY_PORT.getOrThrow(), tokenValidity, cleanupInterval);
  }

  /**
   * Creates and starts a server on the given port
   * @param port the port
   * @param registryPort the registry port
   * @param tokenValidity the number of milliseconds a token is valid
   * @param cleanupInterval the expired token cleanup interval in milliseconds
   * @return a new {@link CredentialsServer} instance
   * @throws RemoteException in case of a communication error
   * @throws AlreadyBoundException if a credential server is already running
   */
  public static CredentialsServer credentialsServer(int port, int registryPort, int tokenValidity,
                                                    int cleanupInterval) throws AlreadyBoundException, RemoteException {
    return new CredentialsServer(port, registryPort, tokenValidity, cleanupInterval);
  }

  private void removeExpired() {
    for (Map.Entry<UUID, UserExpiration> entry : authenticationTokens.entrySet()) {
      if (entry.getValue().isExpired()) {
        authenticationTokens.remove(entry.getKey());
        LOG.debug("Expired token removed for user: " + entry.getValue().user);
      }
    }
  }

  private static final class UserExpiration {

    private final User user;
    private final long expires;

    private UserExpiration(User user, long expires) {
      this.user = user;
      this.expires = expires;
    }

    private boolean isExpired() {
      return System.currentTimeMillis() > expires;
    }
  }
}

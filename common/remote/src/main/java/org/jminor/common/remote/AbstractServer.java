/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.User;
import org.jminor.common.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

/**
 * A default Server implementation.
 * @param <T> the type of remote interface served by this server
 * @param <A> the type of the admin interface this server provides
 */
public abstract class AbstractServer<T extends Remote, A extends Remote>
        extends UnicastRemoteObject implements Server<T, A> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractServer.class);

  private final Map<UUID, RemoteClientConnection<T>> connections = Collections.synchronizedMap(new HashMap<>());
  private final Map<String, LoginProxy> loginProxies = Collections.synchronizedMap(new HashMap<>());
  private final List<LoginProxy> sharedLoginProxies = Collections.synchronizedList(new LinkedList<>());
  private final Map<String, ConnectionValidator> connectionValidators = Collections.synchronizedMap(new HashMap<>());
  private final ConnectionValidator defaultConnectionValidator = new DefaultConnectionValidator();

  private final ServerInfo serverInfo;
  private volatile int connectionLimit = -1;
  private volatile boolean shuttingDown = false;

  /**
   * Instantiates a new AbstractServer
   * @param serverPort the port on which the server should be exported
   * @param serverName the name used when exporting this server
   * @throws RemoteException in case of an exception
   */
  public AbstractServer(final int serverPort, final String serverName) throws RemoteException {
    this(serverPort, serverName, null, null);
  }

  /**
   * Instantiates a new AbstractServer
   * @param serverPort the port on which the server should be exported
   * @param serverName the name used when exporting this server
   * @param clientSocketFactory the client socket factory to use, null for default
   * @param serverSocketFactory the server socket factory to use, null for default
   * @throws RemoteException in case of an exception
   */
  public AbstractServer(final int serverPort, final String serverName, final RMIClientSocketFactory clientSocketFactory,
                        final RMIServerSocketFactory serverSocketFactory) throws RemoteException {
    super(serverPort, clientSocketFactory, serverSocketFactory);
    this.serverInfo = new DefaultServerInfo(UUID.randomUUID(), serverName, serverPort, System.currentTimeMillis());
  }

  /**
   * @return a map containing the current connections
   */
  public final Map<RemoteClient, T> getConnections() {
    synchronized (connections) {
      final Map<RemoteClient, T> clients = new HashMap<>(connections.size());
      for (final RemoteClientConnection<T> remoteClientConnection : connections.values()) {
        clients.put(remoteClientConnection.getClient(), remoteClientConnection.getConnection());
      }

      return clients;
    }
  }

  /**
   * @param clientId the client id
   * @return the connection associated with the given client, null if none exists
   */
  public final T getConnection(final UUID clientId) {
    synchronized (connections) {
      final RemoteClientConnection<T> clientConnection = connections.get(clientId);
      if (clientConnection != null) {
        return clientConnection.getConnection();
      }

      return null;
    }
  }

  /**
   * @return the current number of connections
   */
  public final int getConnectionCount() {
    synchronized (connections) {
      return connections.size();
    }
  }

  /**
   * @return the maximum number of concurrent connections accepted by this server,
   * a negative number means no limit while 0 means the server is closed.
   */
  public final int getConnectionLimit() {
    return connectionLimit;
  }

  /**
   * @param connectionLimit the maximum number of concurrent connections accepted by this server,
   * a negative number means no limit while 0 means the server is closed.
   */
  public final void setConnectionLimit(final int connectionLimit) {
    this.connectionLimit = connectionLimit;
  }

  /** {@inheritDoc} */
  @Override
  public final ServerInfo getServerInfo() {
    return serverInfo;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean connectionsAvailable() {
    return !maximumNumberOfConnectionsReached();
  }

  /** {@inheritDoc} */
  @Override
  public final T connect(final ConnectionRequest connectionRequest) throws RemoteException, ServerException.ServerFullException,
          ServerException.LoginException, ServerException.ConnectionValidationException {
    if (shuttingDown) {
      throw ServerException.loginException("Server is shutting down");
    }
    Objects.requireNonNull(connectionRequest, "connectionRequest");
    Objects.requireNonNull(connectionRequest.getUser(), "user");
    Objects.requireNonNull(connectionRequest.getClientId(), "clientId");
    Objects.requireNonNull(connectionRequest.getClientTypeId(), "clientTypeId");

    getConnectionValidator(connectionRequest.getClientTypeId()).validate(connectionRequest);
    final LoginProxy clientLoginProxy = getLoginProxy(connectionRequest.getClientTypeId());
    LOG.debug("Connecting client {}, loginProxy {}", connectionRequest, clientLoginProxy);
    synchronized (connections) {
      RemoteClientConnection<T> remoteClientConnection = connections.get(connectionRequest.getClientId());
      if (remoteClientConnection != null) {
        validateUserCredentials(connectionRequest.getUser(), remoteClientConnection.getClient().getUser());
        LOG.debug("Active connection exists {}", connectionRequest);
        return remoteClientConnection.getConnection();
      }

      if (maximumNumberOfConnectionsReached()) {
        throw ServerException.serverFullException();
      }

      LOG.debug("No active connection found for client {}, establishing a new connection", connectionRequest);
      RemoteClient remoteClient = Servers.remoteClient(connectionRequest);
      setClientHost(remoteClient, (String) connectionRequest.getParameters().get(CLIENT_HOST_KEY));
      for (final LoginProxy loginProxy : sharedLoginProxies) {
        remoteClient = loginProxy.doLogin(remoteClient);
      }
      if (clientLoginProxy != null) {
        remoteClient = clientLoginProxy.doLogin(remoteClient);
      }
      remoteClientConnection = new RemoteClientConnection<>(remoteClient, doConnect(remoteClient));
      connections.put(remoteClient.getClientId(), remoteClientConnection);

      return remoteClientConnection.getConnection();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect(final UUID clientId) throws RemoteException {
    if (clientId == null) {
      return;
    }

    final RemoteClientConnection<T> remoteClientConnection;
    synchronized (connections) {
      remoteClientConnection = connections.remove(clientId);
    }
    if (remoteClientConnection != null) {
      doDisconnect(remoteClientConnection.getConnection());
      final RemoteClient remoteClient = remoteClientConnection.getClient();
      for (final LoginProxy loginProxy : sharedLoginProxies) {
        loginProxy.doLogout(remoteClient);
      }
      final LoginProxy loginProxy = getLoginProxy(remoteClient.getClientTypeId());
      if (loginProxy != null) {
        loginProxy.doLogout(remoteClient);
      }
      LOG.debug("Client disconnected {}", remoteClient);
    }
  }

  /**
   * Adds a shared LoginProxy, used for all connection requests.
   * @param loginProxy the login proxy
   * @throws IllegalStateException in case the given login proxy has already been added
   */
  public final void addSharedLoginProxy(final LoginProxy loginProxy) {
    synchronized (sharedLoginProxies) {
      if (sharedLoginProxies.contains(loginProxy)) {
        throw new IllegalStateException("Login proxy " + loginProxy + " has already been added");
      }
      sharedLoginProxies.add(loginProxy);
    }
  }

  /**
   * Sets the LoginProxy for the given client type id, if {@code loginProxy} is null
   * the current login proxy (if any) is closed and removed.
   * @param clientTypeId the client type ID with which to associate the given login proxy
   * @param loginProxy the login proxy
   * @throws IllegalStateException in case the login proxy has already been set for the given client type
   */
  public final void setLoginProxy(final String clientTypeId, final LoginProxy loginProxy) {
    synchronized (loginProxies) {
      if (loginProxy == null) {
        final LoginProxy currentProxy = loginProxies.remove(clientTypeId);
        if (currentProxy != null) {
          currentProxy.close();
        }
      }
      else {
        if (loginProxies.containsKey(clientTypeId)) {
          throw new IllegalStateException("Login proxy has already been set for: " + clientTypeId);
        }
        loginProxies.put(clientTypeId, loginProxy);
      }
    }
  }

  /**
   * Sets the {@link ConnectionValidator} for the given client type id, if {@code connectionValidator} is null
   * the current connection validator (if any) is removed.
   * @param clientTypeId the client type ID with which to associate the given connection validator
   * @param connectionValidator the connection validator
   * @throws IllegalStateException in case the connection validator has already been set for the given client type
   */
  public final void setConnectionValidator(final String clientTypeId, final ConnectionValidator connectionValidator) {
    synchronized (connectionValidators) {
      if (connectionValidator == null) {
        connectionValidators.remove(clientTypeId);
      }
      else {
        if (connectionValidators.containsKey(clientTypeId)) {
          throw new IllegalStateException("Connection validator has already been set for: " + clientTypeId);
        }
        connectionValidators.put(clientTypeId, connectionValidator);
      }
    }
  }

  /**
   * @return true if this server is in the process of shutting down
   */
  public final boolean isShuttingDown() {
    return shuttingDown;
  }

  /**
   * Shuts down this server.
   * @throws RemoteException in case of an exception
   */
  public final void shutdown() throws RemoteException {
    if (shuttingDown) {
      return;
    }
    shuttingDown = true;
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (final NoSuchObjectException e) {
      LOG.error("Exception while unexporting server on shutdown", e);
    }
    for (final UUID clientId : new ArrayList<>(connections.keySet())) {
      try {
        disconnect(clientId);
      }
      catch (final RemoteException e) {
        LOG.debug("Error while disconnecting a client on shutdown: " + clientId, e);
      }
    }
    sharedLoginProxies.forEach(AbstractServer::closeLoginProxy);
    loginProxies.values().forEach(AbstractServer::closeLoginProxy);

    handleShutdown();
  }

  /**
   * Called after shutdown has finished, for subclasses
   * @throws RemoteException in case of an exception
   */
  protected void handleShutdown() throws RemoteException {/*Provided for subclasses*/}

  /**
   * Establishes the actual client connection.
   * @param remoteClient the client connection info
   * @return a connection servicing the given client
   * @throws RemoteException in case of an exception
   * @throws ServerException.LoginException in case of an error during the login
   * @throws ServerException.ServerFullException in case the server is not accepting new connections
   */
  protected abstract T doConnect(final RemoteClient remoteClient)
          throws RemoteException, ServerException.LoginException, ServerException.ServerFullException;

  /**
   * Disconnects the given connection.
   * @param connection the connection to disconnect
   * @throws RemoteException in case of an exception
   */
  protected abstract void doDisconnect(final T connection) throws RemoteException;

  /**
   * Validates the given user credentials
   * @param userToCheck the credentials to check
   * @param requiredUser the required credentials
   * @throws ServerException.AuthenticationException in case either User instance is null or if the username or password does not match
   */
  protected static final void validateUserCredentials(final User userToCheck, final User requiredUser) throws ServerException.AuthenticationException {
    if (userToCheck == null || requiredUser == null
            || !Objects.equals(userToCheck.getUsername(), requiredUser.getUsername())
            || !Arrays.equals(userToCheck.getPassword(), requiredUser.getPassword())) {
      throw ServerException.authenticationException("Wrong username or password");
    }
  }

  private boolean maximumNumberOfConnectionsReached() {
    return connectionLimit > -1 && getConnectionCount() >= connectionLimit;
  }

  private LoginProxy getLoginProxy(final String clientTypeId) {
    synchronized (loginProxies) {
      return loginProxies.get(clientTypeId);
    }
  }

  private ConnectionValidator getConnectionValidator(final String clientTypeId) {
    synchronized (connectionValidators) {
      final ConnectionValidator connectionValidator = connectionValidators.get(clientTypeId);
      if (connectionValidator == null) {
        return defaultConnectionValidator;
      }

      return connectionValidator;
    }
  }

  private static void closeLoginProxy(final LoginProxy loginProxy) {
    try {
      loginProxy.close();
    }
    catch (final Exception e) {
      LOG.error("Exception while closing loginProxy for client type: " + loginProxy.getClientTypeId(), e);
    }
  }

  private void setClientHost(final RemoteClient remoteClient, final String requestParameterHost) {
    if (requestParameterHost == null) {
      try {
        remoteClient.setClientHost(getClientHost());
      }
      catch (final ServerNotActiveException ignored) {/*ignored*/}
    }
    else {
      remoteClient.setClientHost(requestParameterHost);
    }
  }

  private static final class RemoteClientConnection<T> {
    private final RemoteClient client;
    private final T connection;

    private RemoteClientConnection(final RemoteClient client, final T connection) {
      this.client = client;
      this.connection = connection;
    }

    private RemoteClient getClient() {
      return client;
    }

    private T getConnection() {
      return connection;
    }
  }

  private static final class DefaultServerInfo implements ServerInfo, Serializable {
    private static final long serialVersionUID = 1;

    private final UUID serverId;
    private final String serverName;
    private final int serverPort;
    private final long serverStartupTime;
    private final Locale locale = Locale.getDefault();
    private final TimeZone timeZone = TimeZone.getDefault();
    private final Version serverVersion = Version.getVersion();

    private DefaultServerInfo(final UUID serverId, final String serverName, final int serverPort, final long serverStartupTime) {
      this.serverId = serverId;
      this.serverName = serverName;
      this.serverPort = serverPort;
      this.serverStartupTime = serverStartupTime;
    }

    @Override
    public String getServerName() {
      return serverName;
    }

    @Override
    public UUID getServerId() {
      return serverId;
    }

    @Override
    public int getServerPort() {
      return serverPort;
    }

    @Override
    public Version getServerVersion() {
      return serverVersion;
    }

    @Override
    public long getStartTime() {
      return serverStartupTime;
    }

    @Override
    public Locale getLocale() {
      return locale;
    }

    @Override
    public TimeZone getTimeZone() {
      return timeZone;
    }
  }

  private static final class DefaultConnectionValidator implements ConnectionValidator {
    @Override
    public String getClientTypeId() {
      return "defaultClient";
    }

    @Override
    public void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException {/*No validation*/}
  }
}

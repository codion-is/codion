/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
  private final LoginProxy defaultLoginProxy = new DefaultLoginProxy();
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
   * @return true if such a client is connected
   */
  public final boolean containsConnection(final UUID clientId) {
    synchronized (connections) {
      return connections.containsKey(clientId);
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
    final LoginProxy loginProxy = getLoginProxy(connectionRequest.getClientTypeId());
    LOG.debug("Connecting client {}, loginProxy {}", connectionRequest, loginProxy);
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
      final RemoteClient remoteClient = Servers.remoteClient(connectionRequest);
      setClientHost(remoteClient, (String) connectionRequest.getParameters().get(CLIENT_HOST_KEY));
      remoteClientConnection = new RemoteClientConnection<>(remoteClient, doConnect(loginProxy.doLogin(remoteClient)));
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
      getLoginProxy(remoteClient.getClientTypeId()).doLogout(remoteClient);
      LOG.debug("Client disconnected {}", remoteClient);
    }
  }

  /**
   * Sets the LoginProxy for the given client type id, if {@code loginProxy} is null
   * the login proxy is removed.
   * @param clientTypeId the client type ID with which to associate the given login proxy
   * @param loginProxy the login proxy
   * @throws IllegalStateException in case the login proxy has already been set for the given client type
   */
  public final void setLoginProxy(final String clientTypeId, final LoginProxy loginProxy) {
    synchronized (loginProxies) {
      if (loginProxy == null) {
        loginProxies.remove(clientTypeId);
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
   * the connection validator is removed.
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
    loginProxies.values().forEach(AbstractServer::closeLoginProxy);

    handleShutdown();
  }

  /**
   * Called after shutdown has finished
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

  protected final void validateUserCredentials(final User userToCheck, final User requiredUser) throws ServerException.AuthenticationException {
    if (userToCheck == null || requiredUser == null
            || !Objects.equals(userToCheck.getUsername(), requiredUser.getUsername())
            || !Objects.equals(userToCheck.getPassword(), requiredUser.getPassword())) {
      throw ServerException.authenticationException("Authentication failed");
    }
  }

  private boolean maximumNumberOfConnectionsReached() {
    return connectionLimit > -1 && getConnectionCount() >= connectionLimit;
  }

  private LoginProxy getLoginProxy(final String clientTypeId) {
    synchronized (loginProxies) {
      final LoginProxy loginProxy = loginProxies.get(clientTypeId);
      if (loginProxy == null) {
        return defaultLoginProxy;
      }

      return loginProxy;
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
  }

  private static final class DefaultLoginProxy implements LoginProxy {
    @Override
    public String getClientTypeId() {
      return "defaultClient";
    }

    @Override
    public RemoteClient doLogin(final RemoteClient remoteClient) {
      return remoteClient;
    }

    @Override
    public void doLogout(final RemoteClient remoteClient) {/*No logout action required*/}

    @Override
    public void close() {/*Not required*/}
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

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
import java.rmi.server.RMISocketFactory;
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
 * @param <A> the type of the admin interface this server supplies
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
    this(serverPort, serverName, RMISocketFactory.getSocketFactory(), RMISocketFactory.getSocketFactory());
  }

  /**
   * Instantiates a new AbstractServer
   * @param serverPort the port on which the server should be exported
   * @param serverName the name used when exporting this server
   * @param clientSocketFactory the client socket factory to use
   * @param serverSocketFactory the server socket factory to use
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
        clients.put(remoteClientConnection.getRemoteClient(), remoteClientConnection.getConnection());
      }

      return clients;
    }
  }

  /**
   * @param clientID the client id
   * @return true if such a client is connected
   */
  public final boolean containsConnection(final UUID clientID) {
    synchronized (connections) {
      return connections.containsKey(clientID);
    }
  }

  /**
   * @param clientID the client id
   * @return the connection associated with the given client, null if none exists
   */
  public final T getConnection(final UUID clientID) {
    synchronized (connections) {
      final RemoteClientConnection<T> clientConnection = connections.get(clientID);
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
    return !maximumNumberOfConnectionReached();
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
    Objects.requireNonNull(connectionRequest.getClientID(), "clientID");
    Objects.requireNonNull(connectionRequest.getClientTypeID(), "clientTypeID");

    getConnectionValidator(connectionRequest.getClientTypeID()).validate(connectionRequest);
    final LoginProxy loginProxy = getLoginProxy(connectionRequest.getClientTypeID());
    LOG.debug("Connecting client {}, loginProxy {}", connectionRequest, loginProxy);
    synchronized (connections) {
      RemoteClientConnection<T> remoteClientConnection = connections.get(connectionRequest.getClientID());
      if (remoteClientConnection != null) {
        validateUserCredentials(connectionRequest.getUser(), remoteClientConnection.getRemoteClient().getUser());
        LOG.debug("Active connection exists {}", connectionRequest);
        return remoteClientConnection.getConnection();
      }

      if (maximumNumberOfConnectionReached()) {
        throw ServerException.serverFullException();
      }

      LOG.debug("No active connection found for client {}, establishing a new connection", connectionRequest);
      final RemoteClient remoteClient = Servers.remoteClient(connectionRequest);
      try {
        remoteClient.setClientHost(getClientHost());
      }
      catch (final ServerNotActiveException ignored) {/*ignored*/}
      remoteClientConnection = new RemoteClientConnection<>(remoteClient, doConnect(loginProxy.doLogin(remoteClient)));
      connections.put(remoteClient.getClientID(), remoteClientConnection);

      return remoteClientConnection.getConnection();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect(final UUID clientID) throws RemoteException {
    if (clientID == null) {
      return;
    }

    final RemoteClientConnection<T> remoteClientConnection;
    synchronized (connections) {
      remoteClientConnection = connections.remove(clientID);
    }
    if (remoteClientConnection != null) {
      doDisconnect(remoteClientConnection.getConnection());
      final RemoteClient remoteClient = remoteClientConnection.getRemoteClient();
      getLoginProxy(remoteClient.getClientTypeID()).doLogout(remoteClient);
      LOG.debug("Client disconnected {}", remoteClient);
    }
  }

  /**
   * Sets the LoginProxy for the given client type id, if {@code loginProxy} is null
   * the login proxy is removed.
   * @param clientTypeID the client type ID with which to associate the given login proxy
   * @param loginProxy the login proxy
   * @throws IllegalStateException in case the login proxy has already been set for the given client type
   */
  public final void setLoginProxy(final String clientTypeID, final LoginProxy loginProxy) {
    synchronized (loginProxies) {
      if (loginProxy == null) {
        loginProxies.remove(clientTypeID);
      }
      else {
        if (loginProxies.containsKey(clientTypeID)) {
          throw new IllegalStateException("Login proxy has already been set for: " + clientTypeID);
        }
        loginProxies.put(clientTypeID, loginProxy);
      }
    }
  }

  /**
   * Sets the ConnectionValidator for the given client type id, if {@code connectionValidator} is null
   * the connection validator is removed.
   * @param clientTypeID the client type ID with which to associate the given connection validator
   * @param connectionValidator the connection validator
   * @throws IllegalStateException in case the connection validator has already been set for the given client type
   */
  public final void setConnectionValidator(final String clientTypeID, final ConnectionValidator connectionValidator) {
    synchronized (connectionValidators) {
      if (connectionValidator == null) {
        connectionValidators.remove(clientTypeID);
      }
      else {
        if (connectionValidators.containsKey(clientTypeID)) {
          throw new IllegalStateException("Connection validator has already been set for: " + clientTypeID);
        }
        connectionValidators.put(clientTypeID, connectionValidator);
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
    catch (final NoSuchObjectException ignored) {/*ignored*/}
    for (final LoginProxy proxy : loginProxies.values()) {
      try {
        proxy.close();
      }
      catch (final Exception ignored) {/*ignored*/}
    }

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

  private boolean maximumNumberOfConnectionReached() {
    return connectionLimit > -1 && getConnectionCount() >= connectionLimit;
  }

  private LoginProxy getLoginProxy(final String clientTypeID) {
    synchronized (loginProxies) {
      final LoginProxy loginProxy = loginProxies.get(clientTypeID);
      if (loginProxy == null) {
        return defaultLoginProxy;
      }

      return loginProxy;
    }
  }

  private ConnectionValidator getConnectionValidator(final String clientTypeID) {
    synchronized (connectionValidators) {
      final ConnectionValidator connectionValidator = connectionValidators.get(clientTypeID);
      if (connectionValidator == null) {
        return defaultConnectionValidator;
      }

      return connectionValidator;
    }
  }

  private static final class RemoteClientConnection<T> {
    private final T connection;
    private final RemoteClient remoteClient;

    private RemoteClientConnection(final RemoteClient remoteClient, final T connection) {
      this.remoteClient = remoteClient;
      this.connection = connection;
    }

    private RemoteClient getRemoteClient() {
      return remoteClient;
    }

    private T getConnection() {
      return connection;
    }
  }

  private static final class DefaultServerInfo implements ServerInfo, Serializable {
    private static final long serialVersionUID = 1;

    private final UUID serverID;
    private final String serverName;
    private final int serverPort;
    private final long serverStartupTime;
    private final Version serverVersion = Version.getVersion();

    private DefaultServerInfo(final UUID serverID, final String serverName, final int serverPort, final long serverStartupTime) {
      this.serverID = serverID;
      this.serverName = serverName;
      this.serverPort = serverPort;
      this.serverStartupTime = serverStartupTime;
    }

    @Override
    public String getServerName() {
      return serverName;
    }

    @Override
    public UUID getServerID() {
      return serverID;
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
    public String getClientTypeID() {
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
    public String getClientTypeID() {
      return "defaultClient";
    }

    @Override
    public void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException {/*No validation*/}
  }
}

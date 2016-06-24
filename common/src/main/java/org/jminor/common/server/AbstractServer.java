/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.jminor.common.model.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
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

  private final Map<UUID, ClientConnectionInfo<T>> connections = Collections.synchronizedMap(new HashMap<>());
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
  public final Map<ClientInfo, T> getConnections() {
    synchronized (connections) {
      final Map<ClientInfo, T> clients = new HashMap<>(connections.size());
      for (final ClientConnectionInfo<T> clientConnectionInfo : connections.values()) {
        clients.put(clientConnectionInfo.getClientInfo(), clientConnectionInfo.getConnection());
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
      final ClientConnectionInfo<T> connectionInfo = connections.get(clientID);
      if (connectionInfo != null) {
        return connectionInfo.getConnection();
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
  public final T connect(final ConnectionInfo connectionInfo) throws RemoteException, ServerException.ServerFullException,
          ServerException.LoginException, ServerException.ConnectionValidationException {
    if (shuttingDown) {
      throw ServerException.loginException("Server is shutting down");
    }
    Objects.requireNonNull(connectionInfo, "connectionInfo");
    Objects.requireNonNull(connectionInfo.getUser(), "user");
    Objects.requireNonNull(connectionInfo.getClientID(), "clientID");
    Objects.requireNonNull(connectionInfo.getClientTypeID(), "clientTypeID");

    getConnectionValidator(connectionInfo.getClientTypeID()).validate(connectionInfo);
    final LoginProxy loginProxy = getLoginProxy(connectionInfo.getClientTypeID());
    LOG.debug("Connecting client {}, loginProxy {}", connectionInfo, loginProxy);
    synchronized (connections) {
      ClientConnectionInfo<T> clientConnectionInfo = connections.get(connectionInfo.getClientID());
      if (clientConnectionInfo != null) {
        validateUserCredentials(connectionInfo.getUser(), clientConnectionInfo.getClientInfo().getUser());
        LOG.debug("Active connection exists {}", connectionInfo);
        return clientConnectionInfo.getConnection();
      }

      if (maximumNumberOfConnectionReached()) {
        throw ServerException.serverFullException();
      }

      LOG.debug("No active connection found for client {}, establishing a new connection", connectionInfo);
      final ClientInfo clientInfo = ServerUtil.clientInfo(connectionInfo);
      clientConnectionInfo = new ClientConnectionInfo<>(clientInfo, doConnect(loginProxy.doLogin(clientInfo)));
      connections.put(clientInfo.getClientID(), clientConnectionInfo);

      return clientConnectionInfo.getConnection();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect(final UUID clientID) throws RemoteException {
    if (clientID == null) {
      return;
    }

    final ClientConnectionInfo<T> clientConnectionInfo;
    synchronized (connections) {
      clientConnectionInfo = connections.remove(clientID);
    }
    if (clientConnectionInfo != null) {
      doDisconnect(clientConnectionInfo.getConnection());
      final ClientInfo clientInfo = clientConnectionInfo.getClientInfo();
      getLoginProxy(clientInfo.getClientTypeID()).doLogout(clientInfo);
      LOG.debug("Client disconnected {}", clientInfo);
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
   * @param connectionInfo the client connection info
   * @return a connection servicing the given client
   * @throws RemoteException in case of an exception
   * @throws ServerException.LoginException in case of an error during the login
   * @throws ServerException.ServerFullException in case the server is not accepting new connections
   */
  protected abstract T doConnect(final ClientInfo connectionInfo)
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

  private static final class ClientConnectionInfo<T> {
    private final T connection;
    private final ClientInfo clientInfo;

    private ClientConnectionInfo(final ClientInfo clientInfo, final T connection) {
      this.clientInfo = clientInfo;
      this.connection = connection;
    }

    private ClientInfo getClientInfo() {
      return clientInfo;
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
    public ClientInfo doLogin(final ClientInfo clientInfo) {
      return clientInfo;
    }

    @Override
    public void doLogout(final ClientInfo clientInfo) {/*No logout action required*/}

    @Override
    public void close() {/*Not required*/}
  }

  private static final class DefaultConnectionValidator implements ConnectionValidator {
    @Override
    public String getClientTypeID() {
      return "defaultClient";
    }

    @Override
    public void validate(final ConnectionInfo connectionInfo) throws ServerException.ConnectionValidationException {/*No validation*/}
  }
}

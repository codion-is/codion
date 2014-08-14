/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.UUID;

/**
 * A default RemoteServer implementation.
 * @param <T> the type of remote interface served by this server
 */
public abstract class AbstractRemoteServer<T extends Remote> extends UnicastRemoteObject implements RemoteServer<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteServer.class);

  private final Map<UUID, ConnectionInfo<T>> connections = Collections.synchronizedMap(new HashMap<UUID, ConnectionInfo<T>>());
  private final Map<String, LoginProxy> loginProxies = Collections.synchronizedMap(new HashMap<String, LoginProxy>());
  private final LoginProxy defaultLoginProxy = new LoginProxy() {
    @Override
    public String getClientTypeID() {
      return "defaultClient";
    }
    @Override
    public ClientInfo doLogin(final ClientInfo clientInfo) {
      return clientInfo;
    }
    @Override
    public void doLogout(final ClientInfo clientInfo) {}
    @Override
    public void close() {}
  };

  private final String serverName;
  private final int serverPort;
  private volatile int connectionLimit = -1;
  private volatile boolean shuttingDown = false;

  /**
   * Instantiates a new AbstractRemoteServer
   * @param serverPort the port on which the server should be exported
   * @param serverName the name used when exporting this server
   * @throws RemoteException in case of an exception
   */
  public AbstractRemoteServer(final int serverPort, final String serverName) throws RemoteException {
    this(serverPort, serverName, RMISocketFactory.getSocketFactory(), RMISocketFactory.getSocketFactory());
  }

  /**
   * Instantiates a new AbstractRemoteServer
   * @param serverPort the port on which the server should be exported
   * @param serverName the name used when exporting this server
   * @param clientSocketFactory the client socket factory to use
   * @param serverSocketFactory the server socket factory to use
   * @throws RemoteException in case of an exception
   */
  public AbstractRemoteServer(final int serverPort, final String serverName, final RMIClientSocketFactory clientSocketFactory,
                              final RMIServerSocketFactory serverSocketFactory) throws RemoteException {
    super(serverPort, clientSocketFactory, serverSocketFactory);
    this.serverName = serverName;
    this.serverPort = serverPort;
  }

  /**
   * @return a map containing the current connections
   */
  public final Map<ClientInfo, T> getConnections() {
    synchronized (connections) {
      final Map<ClientInfo, T> clients = new HashMap<>(connections.size());
      for (final ConnectionInfo<T> connectionInfo : connections.values()) {
        clients.put(connectionInfo.getClientInfo(), connectionInfo.getConnection());
      }

      return clients;
    }
  }

  /**
   * @param client the client info
   * @return true if such a client is connected
   */
  public final boolean containsConnection(final ClientInfo client) {
    synchronized (connections) {
      return connections.containsKey(client.getClientID());
    }
  }

  /**
   * @param client the client info
   * @return the connection associated with the given client, null if none exists
   */
  public final T getConnection(final ClientInfo client) {
    synchronized (connections) {
      return connections.get(client.getClientID()).getConnection();
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
  public final boolean connectionsAvailable() {
    return !maximumNumberOfConnectionReached();
  }

  /** {@inheritDoc} */
  @Override
  public final T connect(final User user, final UUID clientID, final String clientTypeID) throws RemoteException,
          ServerException.ServerFullException, ServerException.LoginException {
    if (clientID == null) {
      return null;
    }

    return connect(new ClientInfo(clientID, clientTypeID, user));
  }

  //todo review synchronization
  /** {@inheritDoc} */
  @Override
  public final T connect(final ClientInfo clientInfo) throws RemoteException,
          ServerException.ServerFullException, ServerException.LoginException {
    if (shuttingDown) {
      throw ServerException.loginException("Server is shutting down");
    }
    final LoginProxy loginProxy = getLoginProxy(clientInfo);
    LOG.debug("Connecting client {}, loginProxy {}", clientInfo, loginProxy);
    synchronized (connections) {
      ConnectionInfo<T> connectionInfo = connections.get(clientInfo.getClientID());
      if (connectionInfo != null) {
        LOG.debug("Active connection exists {}", clientInfo);
        return connectionInfo.getConnection();
      }

      if (maximumNumberOfConnectionReached()) {
        throw ServerException.serverFullException();
      }

      LOG.debug("No active connection found for client {}, establishing a new connection", clientInfo);
      connectionInfo = new ConnectionInfo<>(clientInfo, doConnect(loginProxy.doLogin(clientInfo)));
      LOG.debug("Created a new connection {}", clientInfo);
      connections.put(clientInfo.getClientID(), connectionInfo);

      return connectionInfo.getConnection();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect(final UUID clientID) throws RemoteException {
    if (clientID == null) {
      return;
    }

    final ConnectionInfo<T> connectionInfo;
    synchronized (connections) {
      connectionInfo = connections.remove(clientID);
    }
    if (connectionInfo != null) {
      LOG.debug("About to disconnect {}", connectionInfo.getClientInfo());
      doDisconnect(connectionInfo.getConnection());
      final ClientInfo clientInfo = connectionInfo.getClientInfo();
      getLoginProxy(clientInfo).doLogout(clientInfo);
      LOG.debug("Finished disconnecting {}", clientInfo);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getServerName() {
    return serverName;
  }

  /** {@inheritDoc} */
  @Override
  public final String getServerVersion() {
    return Util.getVersionAndBuildNumber();
  }

  /** {@inheritDoc} */
  @Override
  public final int getServerPort() {
    return serverPort;
  }

  /**
   * Sets the LoginProxy for the given client type id, if <code>loginProxy</code> is null
   * the login proxy is removed.
   * @param clientTypeID the client type ID with which to associate the given login proxy
   * @param loginProxy the login proxy
   */
  public final void setLoginProxy(final String clientTypeID, final LoginProxy loginProxy) {
    synchronized (loginProxies) {
      if (loginProxy == null) {
        loginProxies.remove(clientTypeID);
      }
      else {
        if (loginProxies.containsKey(clientTypeID)) {
          throw new IllegalArgumentException("Login proxy has already been set for: " + clientTypeID);
        }
        loginProxies.put(clientTypeID, loginProxy);
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
    catch (NoSuchObjectException ignored) {}
    for (final LoginProxy proxy : loginProxies.values()) {
      try {
        proxy.close();
      }
      catch (Exception ignored) {}
    }

    handleShutdown();
  }

  /**
   * Called after shutdown has finished
   * @throws RemoteException in case of an exception
   */
  protected void handleShutdown() throws RemoteException {}

  /**
   * Establishes the actual client connection.
   * @param clientInfo the client info
   * @return a connection servicing the given client
   * @throws RemoteException in case of an exception
   */
  protected abstract T doConnect(final ClientInfo clientInfo)
          throws RemoteException, ServerException.LoginException, ServerException.ServerFullException;

  /**
   * Disconnects the given connection.
   * @param connection the connection to disconnect
   * @throws RemoteException in case of an exception
   */
  protected abstract void doDisconnect(final T connection) throws RemoteException;

  private boolean maximumNumberOfConnectionReached() {
    return connectionLimit > -1 && getConnectionCount() >= connectionLimit;
  }

  private LoginProxy getLoginProxy(final ClientInfo clientInfo) {
    synchronized (loginProxies) {
      final LoginProxy loginProxy = loginProxies.get(clientInfo.getClientTypeID());
      if (loginProxy == null) {
        return defaultLoginProxy;
      }

      return loginProxy;
    }
  }

  private static final class ConnectionInfo<T> {
    private final T connection;
    private final ClientInfo clientInfo;

    private ConnectionInfo(final ClientInfo clientInfo, final T connection) {
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
}

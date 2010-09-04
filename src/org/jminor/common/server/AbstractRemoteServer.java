/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A default RemoteServer implementation.
 */
public abstract class AbstractRemoteServer<T> extends UnicastRemoteObject implements RemoteServer<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteServer.class);

  private final Map<ClientInfo, T> connections = Collections.synchronizedMap(new HashMap<ClientInfo, T>());

  private final String serverName;
  private final int serverPort;
  private int connectionLimit = -1;
  private volatile boolean shuttingDown = false;

  private LoginProxy loginProxy = new LoginProxy() {
    public ClientInfo doLogin(final ClientInfo clientInfo) {
      return clientInfo;
    }
  };

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
      return new HashMap<ClientInfo, T>(connections);
    }
  }

  /**
   * @param client the client info
   * @return true if such a client is connected
   */
  public final boolean containsConnection(final ClientInfo client) {
    return connections.containsKey(client);
  }

  /**
   * @param client the client info
   * @return the connection associated with the given client, null if none exists
   */
  public final T getConnection(final ClientInfo client) {
    return connections.get(client);
  }

  /**
   * @return the current number of connections
   */
  public final int getConnectionCount() {
    return connections.size();
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
  public final boolean connectionsAvailable() throws RemoteException {
    return !maximumNummberOfConnectionReached();
  }

  /** {@inheritDoc} */
  public final T connect(final User user, final UUID clientID, final String clientTypeID) throws RemoteException,
          ServerException.ServerFullException, ServerException.LoginException {
    if (clientID == null) {
      return null;
    }

    final ClientInfo client = new ClientInfo(clientID, clientTypeID, user);
    return connect(client);
  }

  /** {@inheritDoc} */
  public final T connect(final ClientInfo clientInfo) throws RemoteException,
          ServerException.ServerFullException, ServerException.LoginException {
    if (connections.containsKey(clientInfo)) {
      return connections.get(clientInfo);
    }

    if (maximumNummberOfConnectionReached()) {
      throw ServerException.serverFullException();
    }

    final T connection = doConnect(loginProxy.doLogin(clientInfo));
    synchronized (connections) {
      connections.put(clientInfo, connection);
    }

    return connection;
  }

  /** {@inheritDoc} */
  public final void disconnect(final UUID clientID) throws RemoteException {
    if (clientID == null) {
      return;
    }

    final ClientInfo client = new ClientInfo(clientID);
    synchronized (connections) {
      if (connections.containsKey(client)) {
        doDisconnect(connections.remove(client));
      }
    }
  }

  /** {@inheritDoc} */
  public final String getServerName() {
    return serverName;
  }

  /** {@inheritDoc} */
  public final int getServerPort() {
    return serverPort;
  }

  /**
   * @param loginProxy the login proxy
   */
  public final void setLoginProxy(final LoginProxy loginProxy) {
    this.loginProxy = loginProxy;
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
      getRegistry().unbind(serverName);
    }
    catch (NotBoundException e) {
      LOG.error(e.getMessage(), e);
    }
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {
      LOG.warn(e.getMessage(), e);
    }
    handleShutdown();
  }

  /**
   * @return the local registry
   * @throws RemoteException in case of an exception
   */
  public static Registry getRegistry() throws RemoteException {
    return LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
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
  protected abstract T doConnect(final ClientInfo clientInfo) throws RemoteException;

  /**
   * Disconnects the given connection.
   * @param connection the connection to disconnect
   * @throws RemoteException in case of an exception
   */
  protected abstract void doDisconnect(final T connection) throws RemoteException;

  private boolean maximumNummberOfConnectionReached() {
    return connectionLimit > -1 && getConnectionCount() >= connectionLimit;
  }
}

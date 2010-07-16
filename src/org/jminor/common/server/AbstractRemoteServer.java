/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

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
 * A default RemoteServer implementation.<br>
 * User: Björn Darri<br>
 * Date: 17.4.2010<br>
 * Time: 22:15:55<br>
 */
public abstract class AbstractRemoteServer<T> extends UnicastRemoteObject implements RemoteServer<T> {

  private static final Logger LOG = Util.getLogger(AbstractRemoteServer.class);

  private final Map<ClientInfo, T> connections = Collections.synchronizedMap(new HashMap<ClientInfo, T>());

  private final String serverName;
  private final int serverPort;
  private volatile boolean shuttingDown = false;

  public AbstractRemoteServer(final int serverPort, final String serverName, final RMIClientSocketFactory clientSocketFactory,
                              final RMIServerSocketFactory serverSocketFactory) throws RemoteException {
    super(serverPort, clientSocketFactory, serverSocketFactory);
    this.serverName = serverName;
    this.serverPort = serverPort;
  }

  public Map<ClientInfo, T> getConnections() {
    return new HashMap<ClientInfo, T>(connections);
  }

  public boolean containsConnection(final ClientInfo client) {
    return connections.containsKey(client);
  }

  public T getConnection(final ClientInfo client) {
    return connections.get(client);
  }

  public int getConnectionCount() {
    return connections.size();
  }

  public T connect(final User user, final UUID clientID, final String clientTypeID) throws RemoteException {
    if (clientID == null) {
      return null;
    }

    final ClientInfo client = new ClientInfo(clientID, clientTypeID, user);
    if (connections.containsKey(client)) {
      return connections.get(client);
    }

    final T connection = doConnect(client);
    connections.put(client, connection);

    return connection;
  }

  public void disconnect(final UUID clientID) throws RemoteException {
    if (clientID == null) {
      return;
    }

    final ClientInfo client = new ClientInfo(clientID);
    if (connections.containsKey(client)) {
      doDisconnect(connections.remove(client));
    }
  }

  public String getServerName() {
    return serverName;
  }

  public int getServerPort() {
    return serverPort;
  }

  public Registry getRegistry() throws RemoteException {
    return LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
  }

  public boolean isShuttingDown() {
    return shuttingDown;
  }

  public void shutdown() throws RemoteException {
    if (shuttingDown) {
      return;
    }
    shuttingDown = true;
    try {
      getRegistry().unbind(serverName);
    }
    catch (NotBoundException e) {
      LOG.error(this, e);
    }
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {
      LOG.warn(e);
    }
  }

  protected abstract T doConnect(final ClientInfo info) throws RemoteException;

  protected abstract void doDisconnect(final T connection) throws RemoteException;
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.provider;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.RemoteServer;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.AbstractEntityDbProvider;
import org.jminor.framework.server.EntityDbRemote;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A class responsible for managing a remote db connection.
 */
public final class EntityDbRemoteProvider extends AbstractEntityDbProvider {

  private static final Logger LOG = Util.getLogger(EntityDbRemoteProvider.class);

  private final String serverHostName;
  private final UUID clientID;
  private final String clientTypeID;
  private RemoteServer server;
  private String serverName;

  /**
   * Instantiates a new EntityDbRemoteProvider.
   * @param user the user to base the db provider on
   * @param clientID the client ID
   * @param clientTypeID a string identifying the client type
   */
  public EntityDbRemoteProvider(final User user, final UUID clientID, final String clientTypeID) {
    super(user);
    Util.rejectNullValue(user, "user");
    serverHostName = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    Configuration.resolveFileProperty(clientTypeID, "javax.net.ssl.trustStore");
  }

  /** {@inheritDoc} */
  public String getDescription() {
    try {
      return server != null ? server.getServerName() : "";
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  public void disconnect() {
    try {
      if (getEntityDbInternal() != null && isConnectionValid()) {
        server.disconnect(clientID);
      }
      setEntityDb(null);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the client ID
   */
  public UUID getClientID() {
    return clientID;
  }

  /** {@inheritDoc} */
  @Override
  protected EntityDb connect() {
    try {
      LOG.debug("Initializing connection for " + getUser());
      final EntityDbRemote remote = (EntityDbRemote) getRemoteEntityDbServer().connect(getUser(), clientID, clientTypeID);
      return initializeProxy(remote);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
    catch (NotBoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isConnectionValid() {
    try {
      //could be a call to any method, simply checking if remote connection is valid
      getEntityDbInternal().isConnected();

      return true;
    }
    catch (Exception e) {
      LOG.debug("Remote connection invalid: " + e.getMessage());
      return false;
    }
  }

  /**
   * @return the EntityDbRemoveServer instance, with an established connection
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private RemoteServer getRemoteEntityDbServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null) {
        this.server.getServerPort();
      }//just to check the connection
    }
    catch (RemoteException e) {
      LOG.info(serverName + " was unreachable, " + getUser() + " - " + clientID + " reconnecting...");
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      LOG.debug("ClientID: " + clientID + ", user: " + getUser() + " connected to server: " + serverName);
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    final List<RemoteServer> servers = getEntityServers(serverHostName);
    if (!servers.isEmpty()) {
      Collections.sort(servers, new ServerComparator());
      this.server = servers.get(0);
      this.serverName = this.server.getServerName();
    }
    else {
      throw new NotBoundException("No reachable or suitable entity server found!");
    }
  }

  private static List<RemoteServer> getEntityServers(final String hostNames) throws RemoteException {
    final List<RemoteServer> servers = new ArrayList<RemoteServer>();
    for (final String serverHostName : hostNames.split(",")) {
      final Registry registry = LocateRegistry.getRegistry(serverHostName);
      final String version = Util.getVersion();
      final String[] boundNames = registry.list();
      for (final String name : boundNames) {
        if (name.startsWith((String) Configuration.getValue(Configuration.SERVER_NAME_PREFIX))
                && name.contains(version) && !name.contains(RemoteServer.SERVER_ADMIN_SUFFIX)) {
          try {
            final RemoteServer server = checkServer((RemoteServer) registry.lookup(name));
            if (server != null) {
              servers.add(server);
            }
          }
          catch (Exception e) {
            LOG.error("Server \"" + name + "\" is unreachable", e);
          }
        }
      }
    }

    return servers;
  }

  private static RemoteServer checkServer(final RemoteServer server) throws RemoteException {
    final int port = server.getServerPort();
    final String requestedPort = Configuration.getStringValue(Configuration.SERVER_PORT);
    if (requestedPort == null || (!requestedPort.isEmpty() && port == Integer.parseInt(requestedPort))) {
      return server;
    }

    return null;
  }

  private EntityDb initializeProxy(final EntityDbRemote remote) {
    return (EntityDb) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class[] {EntityDb.class}, new RemoteInvocationHandler(remote));
  }

  private static final class RemoteInvocationHandler implements InvocationHandler {
    private final EntityDbRemote remote;

    private RemoteInvocationHandler(final EntityDbRemote remote) {
      this.remote = remote;
    }

    /** {@inheritDoc} */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final Method remoteMethod = EntityDbRemote.class.getMethod(method.getName(), method.getParameterTypes());
      try {
        return remoteMethod.invoke(remote, args);
      }
      catch (InvocationTargetException ie) {
        LOG.error(this, ie);
        throw (Exception) ie.getTargetException();
      }
      catch (Exception ie) {
        LOG.error(this, ie);
        throw ie;
      }
    }
  }

  private static final class ServerComparator implements Comparator<RemoteServer>, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    public int compare(final RemoteServer o1, final RemoteServer o2) {
      try {
        return Integer.valueOf(o1.getServerLoad()).compareTo(o2.getServerLoad());
      }
      catch (RemoteException e) {
        return 1;
      }
    }
  }
}

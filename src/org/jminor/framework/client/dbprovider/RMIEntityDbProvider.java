/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.dbprovider;

import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.model.Entity;
import org.jminor.framework.server.IEntityDbRemote;
import org.jminor.framework.server.IEntityDbRemoteServer;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class responisble for managing remote db connections
 */
public class RMIEntityDbProvider implements IEntityDbProvider {

  /**
   * Fired when a successful connection has been made
   */
  public final Event evtConnected = new Event("RMIEntityDbProvider.evtConnected");

  private static final Logger log = Util.getLogger(RMIEntityDbProvider.class);

  static {
    System.setSecurityManager(new RMISecurityManager());
  }

  private final String serverHostName = System.getProperty(FrameworkConstants.SERVER_HOST_NAME_PROPERTY);
  private final User user;
  private final String clientID;
  private final String clientTypeID;
  private IEntityDbRemoteServer server;
  private IEntityDbRemote entityDb;
  private IEntityDb entityDbProxy;
  private String serverName;

  public RMIEntityDbProvider(final User user, final String clientID, final String clientTypeID) {
    this.user = user;
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
  }

  public IEntityDb getEntityDb() throws UserException {
    initializeEntityDb();
    if (entityDbProxy == null)
      entityDbProxy = initializeDbProxy();

    return entityDbProxy;
  }

  public Event getConnectEvent() {
    return evtConnected;
  }

  protected void initializeEntityDb() throws UserException {
    try {
      if (entityDb == null || !connectionValid()) {
        entityDb = getRemoteEntityDbServer().connect(user, clientID, clientTypeID, Entity.repository, FrameworkSettings.get());
        evtConnected.fire();
      }
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  protected void connectServer() throws Exception {
    this.server = getRemoteEntityDbServer();
  }

  protected String getClientID() {
    return clientID;
  }

  private boolean connectionValid() {
    try {
      //could be a call to any method, simply checking if remote connection is valid
      entityDb.isConnected();

      return true;
    }
    catch (Exception e) {
      log.debug("$$$$ connection invalid: " + e.getMessage());
      return false;
    }
  }

  /**
   * @return Value for property 'remoteEntityDbServer'.
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private IEntityDbRemoteServer getRemoteEntityDbServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null)
        this.server.getServerPort();//just to check the connection
    }
    catch (Exception e) {
      e.printStackTrace();
      log.info(serverName + " was unreachable, " + user + " - " + clientID + " reconnecting...");
      unreachable = true;
    }
    if (server == null || unreachable) {
      //if server is not reachable, try to reconnect once and return
      connectToServer();
      log.info(serverName + ", " + user + " - " + clientID + " was able to connect");
    }

    return this.server;
  }

  private void connectToServer() throws RemoteException, NotBoundException {
    final List<IEntityDbRemoteServer> servers = getEntityServers(serverHostName);
    if (servers.size() > 0) {
      Collections.sort(servers, new Comparator<IEntityDbRemoteServer>() {
        public int compare(IEntityDbRemoteServer s1, IEntityDbRemoteServer s2) {
          try {
            return s1.getServerLoad().compareTo(s2.getServerLoad());
          }
          catch (RemoteException e) {
            return 1;
          }
        }
      });
      this.server = servers.get(0);
      this.serverName = this.server.getServerName();
    }
    else
      throw new NotBoundException("No reachable or suitable entity server found!");
  }

  private static IEntityDbRemoteServer checkServer(final IEntityDbRemoteServer server) throws RemoteException {
    final int port = server.getServerPort();
    final String requestedPort = System.getProperty(FrameworkConstants.SERVER_PORT_PROPERTY);
    if (requestedPort == null || (requestedPort.length() > 0 && port == Integer.parseInt(requestedPort)))
      return server;

    return null;
  }

  private static List<IEntityDbRemoteServer> getEntityServers(final String hostNames) throws RemoteException {
    final List<IEntityDbRemoteServer> ret = new ArrayList<IEntityDbRemoteServer>();
    for (final String serverHostName : hostNames.split(",")) {
      final Registry registry = LocateRegistry.getRegistry(serverHostName);
      final String version = Util.getVersion();
      final String[] boundNames = registry.list();
      for (final String name : boundNames) {
        if (name.startsWith(FrameworkConstants.JMINOR_SERVER_NAME_PREFIX) && name.contains(version)) {
          try {
            final IEntityDbRemoteServer server = checkServer((IEntityDbRemoteServer) registry.lookup(name));
            if (server != null)
              ret.add(server);
          }
          catch (Exception e) {
            e.printStackTrace();
            log.error("Server \"" + name + "\" is unreachable");
          }
        }
      }
    }

    return ret;
  }

  private IEntityDb initializeDbProxy() {
    return (IEntityDb) Proxy.newProxyInstance(EntityDbProxy.class.getClassLoader(),
            new Class[] {IEntityDb.class}, new EntityDbProxy());
  }

  private class EntityDbProxy implements InvocationHandler {
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      try {
        return method.invoke(entityDb, args);
      }
      catch (InvocationTargetException ie) {
        throw ie.getTargetException();
      }
    }
  }
}

/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.provider;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.server.EntityDbServer;

import org.apache.log4j.Logger;

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
 * A class responsible for managing a remote db connection
 */
public class EntityDbRemoteProvider implements EntityDbProvider {

  private static final Logger log = Util.getLogger(EntityDbRemoteProvider.class);

  static {
    System.setSecurityManager(new RMISecurityManager());
  }

  private final String serverHostName = System.getProperty(Configuration.SERVER_HOST_NAME);
  private final User user;
  private final String clientID;
  private final String clientTypeID;
  private EntityDbServer server;
  private EntityDb entityDb;
  private String serverName;

  public EntityDbRemoteProvider(final User user, final String clientID, final String clientTypeID) {
    this.user = user;
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
  }

  public EntityDb getEntityDb() throws UserException {
    initializeEntityDb();

    return entityDb;
  }

  public void disconnect() throws UserException {
    try {
      getEntityDb().disconnect();
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private void initializeEntityDb() throws UserException {
    try {
      if (entityDb == null || !connectionValid())
        entityDb = getRemoteEntityDbServer().connect(user, clientID, clientTypeID, EntityRepository.getRepository());
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private boolean connectionValid() {
    try {
      //could be a call to any method, simply checking if remote connection is valid
      entityDb.isConnected();

      return true;
    }
    catch (Exception e) {
      log.debug("Remote connection invalid: " + e.getMessage());
      return false;
    }
  }

  /**
   * @return the EntityDbRemoveServer instance, with an established connection
   * @throws java.rmi.NotBoundException if no server is reachable or if the servers found are not using the specified port
   * @throws java.rmi.RemoteException in case of remote exceptions
   */
  private EntityDbServer getRemoteEntityDbServer() throws RemoteException, NotBoundException {
    boolean unreachable = false;
    try {
      if (this.server != null)
        this.server.getServerPort();//just to check the connection
    }
    catch (RemoteException e) {
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
    final List<EntityDbServer> servers = getEntityServers(serverHostName);
    if (servers.size() > 0) {
      Collections.sort(servers, new Comparator<EntityDbServer>() {
        public int compare(final EntityDbServer serverOne, final EntityDbServer serverTwo) {
          try {
            return serverOne.getServerLoad().compareTo(serverTwo.getServerLoad());
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

  private static List<EntityDbServer> getEntityServers(final String hostNames) throws RemoteException {
    final List<EntityDbServer> ret = new ArrayList<EntityDbServer>();
    for (final String serverHostName : hostNames.split(",")) {
      final Registry registry = LocateRegistry.getRegistry(serverHostName);
      final String version = Util.getVersion();
      final String[] boundNames = registry.list();
      for (final String name : boundNames) {
        if (name.startsWith((String) Configuration.getValue(Configuration.SERVER_NAME_PREFIX))
                && name.contains(version) && !name.contains(EntityDbServer.SERVER_ADMIN_SUFFIX)) {
          try {
            final EntityDbServer server = checkServer((EntityDbServer) registry.lookup(name));
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

  private static EntityDbServer checkServer(final EntityDbServer server) throws RemoteException {
    final int port = server.getServerPort();
    final String requestedPort = System.getProperty(Configuration.SERVER_PORT);
    if (requestedPort == null || (requestedPort.length() > 0 && port == Integer.parseInt(requestedPort)))
      return server;

    return null;
  }
}

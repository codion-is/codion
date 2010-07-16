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

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class EntityDbRemoteProvider extends AbstractEntityDbProvider {

  private static final Logger LOG = Util.getLogger(EntityDbRemoteProvider.class);
  private static final int INPUT_BUFFER_SIZE = 8192;

  private final String serverHostName;
  private final UUID clientID;
  private final String clientTypeID;
  private RemoteServer server;
  private String serverName;

  public EntityDbRemoteProvider(final User user, final UUID clientID, final String clientTypeID) {
    super(user);
    Util.rejectNullValue(user, "user");
    serverHostName = System.getProperty(Configuration.SERVER_HOST_NAME, "localhost");
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    setTruststore(clientTypeID);
  }

  public String getDescription() {
    try {
      return server != null ? server.getServerName() : "";
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  public void disconnect() {
    try {
      if (entityDb != null && isConnectionValid()) {
        server.disconnect(clientID);
      }
      entityDb = null;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public UUID getClientID() {
    return clientID;
  }

  @Override
  protected EntityDb connect() {
    try {
      LOG.debug("Initializing connection for " + getUser());
      return (EntityDb) getRemoteEntityDbServer().connect(getUser(), clientID, clientTypeID);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected boolean isConnectionValid() {
    try {
      //could be a call to any method, simply checking if remote connection is valid
      entityDb.isConnected();

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
    if (servers.size() > 0) {
      Collections.sort(servers, new Comparator<RemoteServer>() {
        public int compare(final RemoteServer o1, final RemoteServer o2) {
          try {
            return Integer.valueOf(o1.getServerLoad()).compareTo(o2.getServerLoad());
          }
          catch (RemoteException e) {
            return 1;
          }
        }
      });
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
            LOG.debug("Server \"" + name + "\" is unreachable", e);
          }
        }
      }
    }

    return servers;
  }

  private static RemoteServer checkServer(final RemoteServer server) throws RemoteException {
    final int port = server.getServerPort();
    final String requestedPort = System.getProperty(Configuration.SERVER_PORT);
    if (requestedPort == null || (requestedPort.length() > 0 && port == Integer.parseInt(requestedPort))) {
      return server;
    }

    return null;
  }

  private static void setTruststore(final String clientTypeID) {
    final String truststore = System.getProperty("javax.net.ssl.trustStore");
    if (truststore != null) {
      FileOutputStream out = null;
      InputStream in = null;
      try {
        ClassLoader loader = EntityDbRemoteProvider.class.getClassLoader();
        in = loader.getResourceAsStream(truststore);
        if (in == null) {
          LOG.debug("Truststore resource '" + truststore + "' was not found in classpath");
          return;
        }
        final File trustFile = File.createTempFile(clientTypeID, "ts");
        trustFile.deleteOnExit();
        out = new FileOutputStream(trustFile);
        byte buf[] = new byte[INPUT_BUFFER_SIZE];
        int br = in.read(buf);
        while (br > 0) {
          out.write(buf, 0, br);
          br = in.read(buf);
        }
        System.setProperty("javax.net.ssl.trustStore", trustFile.toString());
        LOG.debug("Truststore set to : " + trustFile.toString() + ", original " + truststore);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      finally {
        Util.closeSilently(out, in);
      }
    }
  }
}

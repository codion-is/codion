/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.DbLog;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.IDatabase;
import org.jminor.common.model.ClientInfo;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.model.EntityRepository;

import org.apache.log4j.Logger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The remote server class, responsible for handling remote db connection requests
 */
public class EntityDbRemoteServer extends UnicastRemoteObject implements IEntityDbRemoteServer {

  private static final Logger log = Util.getLogger(EntityDbRemoteServer.class);

  private static final boolean loggingEnabled =
          System.getProperty(FrameworkConstants.SERVER_LOGGING_ON, "1").equalsIgnoreCase("1");

  private static final int SERVER_PORT;
  private static final int SERVER_ADMIN_PORT;
  private static final int SERVER_DB_PORT;

  static {
    final String serverPortProperty = System.getProperty(FrameworkConstants.SERVER_PORT_PROPERTY);
    final String serverAdminPortProperty = System.getProperty(FrameworkConstants.SERVER_ADMIN_PORT_PROPERTY);
    final String serverDbPortProperty = System.getProperty(FrameworkConstants.SERVER_DB_PORT_PROPERTY);

    if (serverPortProperty == null)
      throw new RuntimeException("Required server property missing: " + FrameworkConstants.SERVER_PORT_PROPERTY);
    if (serverAdminPortProperty == null)
      throw new RuntimeException("Required server property missing: " + FrameworkConstants.SERVER_ADMIN_PORT_PROPERTY);
    if (serverDbPortProperty == null)
      throw new RuntimeException("Required server property missing: " + FrameworkConstants.SERVER_DB_PORT_PROPERTY);

    SERVER_PORT = Integer.parseInt(serverPortProperty);
    SERVER_ADMIN_PORT = Integer.parseInt(serverAdminPortProperty);
    SERVER_DB_PORT = Integer.parseInt(serverDbPortProperty);
  }

  private final Date startDate = new Date();
  private Timer connectionMaintenanceTimer;
  private transient int checkMaintenanceInterval = 30; //seconds

  private final EntityDbRemoteServerAdmin serverAdmin;

  private final Map<ClientInfo, EntityDbRemoteAdapter> connections =
          Collections.synchronizedMap(new HashMap<ClientInfo, EntityDbRemoteAdapter>());
  private final String serverName;
  private int connectionTimeout = 120000;
  private static boolean useSecureConnection =
          Integer.parseInt(System.getProperty(FrameworkConstants.SERVER_SECURE_CONNECTION, "1")) == 1;

  /**
   * Constructs a new EntityDbRemoteServer.
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  private EntityDbRemoteServer() throws RemoteException {
    super(SERVER_PORT, useSecureConnection ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            useSecureConnection ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    final String host = System.getProperty(IDatabase.DATABASE_HOST_PROPERTY);
    final String port = System.getProperty(IDatabase.DATABASE_PORT_PROPERTY);
    final String sid = System.getProperty(IDatabase.DATABASE_SID_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException("Database host must be specified (" + IDatabase.DATABASE_HOST_PROPERTY +")");
    if (!Database.get().isEmbedded() && (sid == null || sid.length() == 0))
      throw new RuntimeException("Database sid must be specified (" + IDatabase.DATABASE_SID_PROPERTY +")");
    if (!Database.get().isEmbedded() && (port == null || port.length() == 0))
      throw new RuntimeException("Database port must be specified (" + IDatabase.DATABASE_PORT_PROPERTY +")");

    serverName = FrameworkSettings.get().getProperty(FrameworkSettings.SERVER_NAME_PREFIX)
            + " " + Util.getVersion() + " @ " + (sid != null ? sid.toUpperCase() : host.toUpperCase())
            + " [id:" + Long.toHexString(System.currentTimeMillis()) + "]";
    serverAdmin = new EntityDbRemoteServerAdmin(this, SERVER_ADMIN_PORT, useSecureConnection);
    startConnectionCheckTimer();
  }

  /** {@inheritDoc} */
  public String getServerName() {
    return serverName;
  }

  /** {@inheritDoc} */
  public int getServerPort() throws RemoteException {
    return SERVER_PORT;
  }

  /** {@inheritDoc} */
  public Integer getServerLoad() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  public IEntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID,
                                 final EntityRepository repository) throws RemoteException {
    if (connectionKey == null)
      return null;

    if (!EntityRepository.get().contains(repository.getEntityIDs()))
      EntityRepository.get().add(repository.initializeAll());

    final ClientInfo client = new ClientInfo(connectionKey, clientTypeID, user);
    if (connections.containsKey(client))
      return connections.get(client);

    return doConnect(client);
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public Collection<User> getUsers() throws RemoteException {
    final Set<User> ret = new HashSet<User>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        ret.add(adapter.getUser());
    }

    return ret;
  }

  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> ret = new ArrayList<ClientInfo>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (user == null || adapter.getUser().equals(user))
          ret.add(adapter.getClient());
    }

    return ret;
  }

  public void disconnect(final String connectionKey) throws RemoteException {
    if (connectionKey == null)
      return;

    removeConnection(new ClientInfo(connectionKey), true);
  }

  public int getCheckMaintenanceInterval() {
    return checkMaintenanceInterval;
  }

  public void setCheckMaintenanceInterval(final int checkTimerInterval) {
    if (checkMaintenanceInterval != checkTimerInterval)
      checkMaintenanceInterval = checkTimerInterval <= 0 ? 1 : checkTimerInterval;
  }

  public DbLog getConnectionLog(final String connectionKey) {
    synchronized (connections) {
      final ClientInfo client = new ClientInfo(connectionKey);
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (adapter.getClient().equals(client))
          return adapter.getEntityDbLog();
    }

    return null;
  }

  public int getConnectionCount() {
    return connections.size();
  }

  public boolean isLoggingOn(final String connectionKey) {
    synchronized (connections) {
      final ClientInfo client = new ClientInfo(connectionKey);
      for (final EntityDbRemoteAdapter connection : connections.values()) {
        if (connection.getClient().equals(client)) {
          return connection.isLoggingEnabled();
        }
      }
    }

    return false;
  }

  public void setLoggingOn(final String connectionKey, final boolean status) {
    synchronized (connections) {
      final ClientInfo client = new ClientInfo(connectionKey);
      for (final EntityDbRemoteAdapter connection : connections.values()) {
        if (connection.getClient().equals(client)) {
          connection.setLoggingEnabled(status);
          return;
        }
      }
    }
  }

  public int getServerDbPort() {
    return SERVER_DB_PORT;
  }

  public Date getStartDate() {
    return startDate;
  }

  public synchronized void shutdown() throws RemoteException {
    unexport(this);
    unexport(serverAdmin);
    removeConnections(false);
    final Registry localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT );
    if (localRegistry != null) {
      try {
        log.info("Shutting down server: " + serverName);
        localRegistry.unbind(serverName);
        localRegistry.unbind(serverName + "-admin");
      }
      catch (NotBoundException ex) {
        log.error(this, ex);
      }
    }
    Database.get().shutdownEmbedded(null);//todo does not work when shutdown requires user authentication
  }

  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    synchronized (connections) {
      final List<ClientInfo> clients = new ArrayList<ClientInfo>(connections.keySet());
      for (final ClientInfo client : clients) {
        final EntityDbRemoteAdapter adapter = connections.get(client);
        if (inactiveOnly) {
          if (!adapter.isWorking() && adapter.hasBeenInactive(getConnectionTimeout() * 1000))
            adapter.logout();
        }
        else
          adapter.logout();
      }
    }
  }

  private void startConnectionCheckTimer() {
    if (connectionMaintenanceTimer != null)
      connectionMaintenanceTimer.cancel();

    connectionMaintenanceTimer = new Timer(true);
    connectionMaintenanceTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        maintainConnections();
      }
    }, new Date(), checkMaintenanceInterval * 1000L);
  }

  private void maintainConnections() {
    try {
      removeConnections(true);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private void removeConnection(final ClientInfo client, final boolean logout) throws RemoteException {
    if (connections.containsKey(client)) {
      log.debug("Removing connection: " + client);
      final EntityDbRemoteAdapter adapter = connections.remove(client);
      if (logout)
        adapter.logout();
    }
  }

  private EntityDbRemoteAdapter doConnect(final ClientInfo client) throws RemoteException {
    final EntityDbRemoteAdapter ret = new EntityDbRemoteAdapter(client, SERVER_DB_PORT, loggingEnabled);
    ret.evtLoggingOut.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          removeConnection(client, false);
        }
        catch (RemoteException ex) {
          ex.printStackTrace();
        }
      }
    });
    connections.put(client, ret);
    log.info("JMinor server added connection (" + client + ")");

    return ret;
  }

  private static void unexport(final UnicastRemoteObject connection) {
    try {
      UnicastRemoteObject.unexportObject(connection, true);
    }
    catch (NoSuchObjectException e) {
      log.error(e);
    }
  }

  public static void main(String[] args) {
    try {
      System.setSecurityManager(new RMISecurityManager());
      Registry localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
      try {
        localRegistry.list();
      }
      catch (Exception e) {
        log.info("Server creating registry on port: " + Registry.REGISTRY_PORT);
        localRegistry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
      }

      final EntityDbRemoteServer server = new EntityDbRemoteServer();

      localRegistry.rebind(server.getServerName(), server);
      localRegistry.rebind(server.getServerName() + IEntityDbRemoteServer.SERVER_ADMIN_SUFFIX, server.serverAdmin);
      final String connectInfo = server.getServerName() + " bound to registry";
      log.info(connectInfo);
      System.out.println(connectInfo);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}

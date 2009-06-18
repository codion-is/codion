/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.Database;
import org.jminor.common.db.DbLog;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.IDatabase;
import org.jminor.common.model.ClientInfo;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.model.EntityRepository;

import org.apache.log4j.Level;
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
public class EntityDbRemoteServer extends UnicastRemoteObject implements IEntityDbRemoteServer, IEntityDbRemoteServerAdmin {

  private static final Logger log = Util.getLogger(EntityDbRemoteServer.class);

  private static final boolean loggingEnabled =
          System.getProperty(FrameworkConstants.SERVER_LOGGING_ON, "1").equalsIgnoreCase("1");
  private static final int SERVER_PORT = Integer.parseInt(System.getProperty(FrameworkConstants.SERVER_PORT_PROPERTY));

  private static final int SERVER_DB_PORT = Integer.parseInt(System.getProperty(FrameworkConstants.SERVER_DB_PORT_PROPERTY));

  private final Date startDate = new Date();
  private Timer connectionMaintenanceTimer;
  private transient int checkMaintenanceInterval = 30; //seconds

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
      throw new IllegalArgumentException("Db host must be specified!");
    if (!Database.get().isEmbedded() && (sid == null || sid.length() == 0))
      throw new IllegalArgumentException("Db sid must be specified");
    if (!Database.get().isEmbedded() && (port == null || port.length() == 0))
      throw new IllegalArgumentException("Db port must be specified");

    serverName = FrameworkSettings.get().getProperty(FrameworkSettings.SERVER_NAME_PREFIX)
            + " " + Util.getVersion() + " @ " + (sid != null ? sid.toUpperCase() : host.toUpperCase())
            + " [id:" + Long.toHexString(System.currentTimeMillis()) + "]";
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
  public int getServerDbPort() throws RemoteException {
    return SERVER_DB_PORT;
  }

  /** {@inheritDoc} */
  public Date getStartDate() {
    return startDate;
  }

  /** {@inheritDoc} */
  public String getDatabaseURL() {
    return Database.get().getURL(null);
  }

  /** {@inheritDoc} */
  public Level getLoggingLevel() throws RemoteException {
    return Util.getLoggingLevel();
  }

  /** {@inheritDoc} */
  public void setLoggingLevel(final Level level) throws RemoteException {
    Util.setLoggingLevel(level);
  }

  /** {@inheritDoc} */
  public Collection<User> getUsers() throws RemoteException {
    final Set<User> ret = new HashSet<User>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        ret.add(adapter.getUser());
    }

    return ret;
  }

  /** {@inheritDoc} */
  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> ret = new ArrayList<ClientInfo>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (user == null || adapter.getUser().equals(user))
          ret.add(adapter.getClient());
    }

    return ret;
  }

  public Collection<String> getClientTypes() throws RemoteException {
    final Set<String> ret = new HashSet<String>();
    for (final ClientInfo client : getClients(null))
      ret.add(client.getClientTypeID());

    return ret;
  }

  /** {@inheritDoc} */
  public IEntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID,
                                 final EntityRepository repository)
          throws RemoteException {
    if (connectionKey == null)
      return null;

    if (!EntityRepository.get().contains(repository.getEntityIDs()))
      EntityRepository.get().add(repository.initializeAll());

    final ClientInfo client = new ClientInfo(connectionKey, clientTypeID, user);
    if (connections.containsKey(client))
      return connections.get(client);

    return doConnect(client);
  }

  /** {@inheritDoc} */
  public void disconnect(final String connectionKey) throws RemoteException {
    if (connectionKey == null)
      return;

    removeConnection(new ClientInfo(connectionKey));
  }

  /** {@inheritDoc} */
  public synchronized void shutdown() throws RemoteException {
    unexport(this);
    removeConnections(false);
    final Registry localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT );
    if (localRegistry != null) {
      try {
        log.info("Shutting down server: " + serverName);
        localRegistry.unbind(serverName);
      }
      catch (NotBoundException ex) {
        log.error(this, ex);
      }
    }
    System.exit(0);
  }

  /** {@inheritDoc} */
  public int getActiveConnectionCount() throws RemoteException {
    return EntityDbRemoteAdapter.getActiveCount();
  }

  /** {@inheritDoc} */
  public int getCheckMaintenanceInterval() {
    return checkMaintenanceInterval;
  }

  /** {@inheritDoc} */
  public void setCheckMaintenanceInterval(final int checkTimerInterval) {
    if (this.checkMaintenanceInterval != checkTimerInterval) {
      this.checkMaintenanceInterval = checkTimerInterval <= 0 ? 1 : checkTimerInterval;
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public void resetConnectionPoolStatistics(final User user) throws RemoteException {
    EntityDbRemoteAdapter.resetConnectionPoolStats(user);
  }

  /** {@inheritDoc} */
  public int getRequestsPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  public int getWarningTimeThreshold() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningThreshold();
  }

  /** {@inheritDoc} */
  public void setWarningTimeThreshold(int threshold) throws RemoteException {
    EntityDbRemoteAdapter.setWarningThreshold(threshold);
  }

  /** {@inheritDoc} */
  public int getWarningTimeExceededPerSecond() throws RemoteException {
    return EntityDbRemoteAdapter.getWarningTimeExceededPerSecond();
  }

  /** {@inheritDoc} */
  public ConnectionPoolSettings getConnectionPoolSettings(final User user, final long since) throws RemoteException {
    return EntityDbRemoteAdapter.getConnectionPoolSettings(user, since);
  }

  /** {@inheritDoc} */
  public List<ConnectionPoolSettings> getActiveConnectionPools() throws RemoteException {
    return EntityDbRemoteAdapter.getActiveConnectionPoolSettings();
  }

  /** {@inheritDoc} */
  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException {
    EntityDbRemoteAdapter.setConnectionPoolSettings(settings.getUser(), settings);
  }

  /** {@inheritDoc} */
  public String getMemoryUsage() throws RemoteException {
    return Util.getMemoryUsageString();
  }

  /** {@inheritDoc} */
  public void performGC() throws RemoteException {
    Runtime.getRuntime().gc();
  }

  /** {@inheritDoc} */
  public int getConnectionCount() {
    return connections.size();
  }

  /** {@inheritDoc} */
  public DbLog getConnectionLog(final String connectionKey) {
    synchronized (connections) {
      final ClientInfo client = new ClientInfo(connectionKey);
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (adapter.getClient().equals(client))
          return adapter.getEntityDbLog();
    }

    return null;
  }

  /** {@inheritDoc} */
  public boolean isLoggingOn(final String connectionKey) throws RemoteException {
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

  /** {@inheritDoc} */
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

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public String getSystemProperties() {
    return Util.getSystemProperties();
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
    }, new Date(), getCheckMaintenanceInterval() * 1000L);
  }

  private void maintainConnections() {
    try {
      removeConnections(true);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private void removeConnection(final ClientInfo client) throws RemoteException {
    if (connections.containsKey(client)) {
      log.debug(client + " is being closed");
      connections.remove(client).logout();
    }
  }

  private EntityDbRemoteAdapter doConnect(final ClientInfo client) throws RemoteException {
    final EntityDbRemoteAdapter ret = new EntityDbRemoteAdapter(client, SERVER_DB_PORT, loggingEnabled);
    ret.evtLoggingOut.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          removeConnection(client);
        }
        catch (RemoteException ex) {
          ex.printStackTrace();
        }
      }
    });
    connections.put(client, ret);
    log.info("EntityDbServer added connection (" + client + ")");

    return ret;
  }

  private static void unexport(final UnicastRemoteObject connection) {
    try {
      UnicastRemoteObject.unexportObject(connection, false);
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
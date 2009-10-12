/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Dbms;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.EntityRepository;

import org.apache.log4j.Logger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
public class EntityDbRemoteServer extends UnicastRemoteObject implements EntityDbServer {

  private static final Logger log = Util.getLogger(EntityDbRemoteServer.class);

  private static final boolean LOGGING_ENABLED =
          System.getProperty(Configuration.SERVER_LOGGING_ON, "1").equalsIgnoreCase("1");
  static final boolean SECURE_CONNECTION =
          Integer.parseInt(System.getProperty(Configuration.SERVER_SECURE_CONNECTION, "1")) == 1;

  private static final int SERVER_PORT;
  private static final int SERVER_DB_PORT;

  static {
    final String serverPortProperty = System.getProperty(Configuration.SERVER_PORT);
    final String serverDbPortProperty = System.getProperty(Configuration.SERVER_DB_PORT);

    if (serverPortProperty == null)
      throw new RuntimeException("Required server property missing: " + Configuration.SERVER_PORT);
    if (serverDbPortProperty == null)
      throw new RuntimeException("Required server property missing: " + Configuration.SERVER_DB_PORT);

    SERVER_PORT = Integer.parseInt(serverPortProperty);
    SERVER_DB_PORT = Integer.parseInt(serverDbPortProperty);
  }

  private final Registry registry;
  private final String serverName;
  private final Date startDate = new Date();private final Map<ClientInfo, EntityDbRemoteAdapter> connections =
          Collections.synchronizedMap(new HashMap<ClientInfo, EntityDbRemoteAdapter>());

  private Timer connectionMaintenanceTimer;
  private int checkMaintenanceInterval = 30; //seconds
  private int connectionTimeout = 120; //seconds

  /**
   * Constructs a new EntityDbRemoteServer and binds it to the given registry
   * @param registry the Registry to bind to
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  EntityDbRemoteServer(final Registry registry) throws RemoteException {
    super(SERVER_PORT, SECURE_CONNECTION ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            SECURE_CONNECTION ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    final String host = System.getProperty(Dbms.DATABASE_HOST);
    final String port = System.getProperty(Dbms.DATABASE_PORT);
    final String sid = System.getProperty(Dbms.DATABASE_SID);
    if (host == null || host.length() == 0)
      throw new RuntimeException("Database host must be specified (" + Dbms.DATABASE_HOST +")");
    if (!Database.get().isEmbedded() && (sid == null || sid.length() == 0))
      throw new RuntimeException("Database sid must be specified (" + Dbms.DATABASE_SID +")");
    if (!Database.get().isEmbedded() && (port == null || port.length() == 0))
      throw new RuntimeException("Database port must be specified (" + Dbms.DATABASE_PORT +")");

    serverName = Configuration.getValue(Configuration.SERVER_NAME_PREFIX)
            + " " + Util.getVersion() + " @ " + (sid != null ? sid.toUpperCase() : host.toUpperCase())
            + " [id:" + Long.toHexString(System.currentTimeMillis()) + "]";
    startConnectionCheckTimer();
    this.registry = registry;
    this.registry.rebind(getServerName(), this);
    final String connectInfo = getServerName() + " bound to registry";
    log.info(connectInfo);
    System.out.println(connectInfo);
  }

  public Registry getRegistry() {
    return registry;
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
  public EntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID,
                                final Map<String, EntityDefinition> repository) throws RemoteException {
    if (connectionKey == null)
      return null;

    if (!EntityRepository.contains(repository))
      EntityRepository.putAll(repository);

    final ClientInfo client = new ClientInfo(connectionKey, clientTypeID, user);
    if (connections.containsKey(client))
      return connections.get(client);

    return doConnect(client);
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(final int timeout) {
    this.connectionTimeout = timeout;
  }

  public Collection<User> getUsers() throws RemoteException {
    final Set<User> users = new HashSet<User>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        users.add(adapter.getUser());
    }

    return users;
  }

  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (user == null || adapter.getUser().equals(user))
          clients.add(adapter.getClientInfo());
    }

    return clients;
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

  public ServerLog getServerLog(final String connectionKey) {
    synchronized (connections) {
      final ClientInfo client = new ClientInfo(connectionKey);
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (adapter.getClientInfo().equals(client))
          return adapter.getServerLog();
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
        if (connection.getClientInfo().equals(client)) {
          return connection.getMethodLogger().isLoggingEnabled();
        }
      }
    }

    return false;
  }

  public void setLoggingOn(final String connectionKey, final boolean status) {
    synchronized (connections) {
      final ClientInfo client = new ClientInfo(connectionKey);
      for (final EntityDbRemoteAdapter connection : connections.values()) {
        if (connection.getClientInfo().equals(client)) {
          connection.getMethodLogger().setLoggingEnabled(status);
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

  public void shutdown() throws RemoteException {
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {
      log.warn(e);
    }
    removeConnections(false);
    try {
      getRegistry().unbind(serverName);
    }
    catch (NotBoundException e) {
      log.error(this, e);
    }
    Database.get().shutdownEmbedded(null);//todo does not work when shutdown requires user authentication
  }

  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    synchronized (connections) {
      final List<ClientInfo> clients = new ArrayList<ClientInfo>(connections.keySet());
      for (final ClientInfo client : clients) {
        final EntityDbRemoteAdapter adapter = connections.get(client);
        if (inactiveOnly) {
          if (!adapter.isActive() && adapter.hasBeenInactive(getConnectionTimeout()*1000))
            adapter.disconnect();
        }
        else
          adapter.disconnect();
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
      log.info("Connection removed: " + client);
      final EntityDbRemoteAdapter adapter = connections.remove(client);
      if (logout)
        adapter.disconnect();
    }
  }

  private EntityDbRemoteAdapter doConnect(final ClientInfo client) throws RemoteException {
    final EntityDbRemoteAdapter remoteAdapter = new EntityDbRemoteAdapter(client, SERVER_DB_PORT, LOGGING_ENABLED);
    remoteAdapter.evtLoggingOut.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          removeConnection(client, false);
        }
        catch (RemoteException ex) {
          ex.printStackTrace();
        }
      }
    });
    connections.put(client, remoteAdapter);
    log.info("Connection added: " + client);

    return remoteAdapter;
  }
}

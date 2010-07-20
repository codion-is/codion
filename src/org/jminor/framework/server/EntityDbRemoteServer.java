/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.AbstractRemoteServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.EntityRepository;

import org.apache.log4j.Logger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * The remote server class, responsible for handling remote db connection requests.
 */
public final class EntityDbRemoteServer extends AbstractRemoteServer<EntityDbRemote> {

  static final Logger LOG = Util.getLogger(EntityDbRemoteServer.class);

  private static final boolean CLIENT_LOGGING_ENABLED =
          System.getProperty(Configuration.SERVER_CLIENT_LOGGING_ENABLED, "true").equalsIgnoreCase("true");
  static final boolean SSL_CONNECTION_ENABLED =
          System.getProperty(Configuration.SERVER_CONNECTION_SSL_ENABLED, "true").equalsIgnoreCase("true");

  private static final int SERVER_PORT;
  private static final int SERVER_DB_PORT;

  private final Database database;

  static {
    final String serverPortProperty = System.getProperty(Configuration.SERVER_PORT);
    final String serverDbPortProperty = System.getProperty(Configuration.SERVER_DB_PORT);

    Util.require(Configuration.SERVER_PORT, serverPortProperty);
    Util.require(Configuration.SERVER_DB_PORT, serverDbPortProperty);

    SERVER_PORT = Integer.parseInt(serverPortProperty);
    SERVER_DB_PORT = Integer.parseInt(serverDbPortProperty);

    try {
      initializeRegistry();
    }
    catch (RemoteException re) {
      throw new RuntimeException(re);
    }
  }

  private final long startDate = System.currentTimeMillis();

  private Timer connectionMaintenanceTimer;
  private int checkMaintenanceInterval = 30; //seconds
  private int connectionTimeout = 120; //seconds

  /**
   * Constructs a new EntityDbRemoteServer and binds it to the given registry
   * @param database the Database implementation
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  EntityDbRemoteServer(final Database database) throws RemoteException {
    super(SERVER_PORT, initializeServerName(database.getHost(), database.getSid()),
            SSL_CONNECTION_ENABLED ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            SSL_CONNECTION_ENABLED ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    loadDefaultDomainModels();
    this.database = database;
    EntityDbRemoteAdapter.initConnectionPools(database);
    final String host = database.getHost();
    final String port = database.getPort();
    if (host == null || host.length() == 0) {
      throw new RuntimeException("Database host must be specified (" + Database.DATABASE_HOST + ")");
    }
    if (!database.isEmbedded() && (port == null || port.length() == 0)) {
      throw new RuntimeException("Database port must be specified (" + Database.DATABASE_PORT + ")");
    }

    startConnectionCheckTimer();
    getRegistry().rebind(getServerName(), this);
    final String connectInfo = getServerName() + " bound to registry";
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  public Database getDatabase() {
    return database;
  }

  public int getServerLoad() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(final int timeout) {
    this.connectionTimeout = timeout;
  }

  public Collection<User> getUsers() throws RemoteException {
    final Set<User> users = new HashSet<User>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      users.add(((EntityDbRemoteAdapter) adapter).getUser());
    }

    return users;
  }

  public Collection<ClientInfo> getClients() throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      clients.add(((EntityDbRemoteAdapter) adapter).getClientInfo());
    }

    return clients;
  }

  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      if (user == null || ((EntityDbRemoteAdapter) adapter).getUser().equals(user)) {
        clients.add(((EntityDbRemoteAdapter) adapter).getClientInfo());
      }
    }

    return clients;
  }

  public Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      if (((EntityDbRemoteAdapter) adapter).getClientInfo().getClientTypeID().equals(clientTypeID)) {
        clients.add(((EntityDbRemoteAdapter) adapter).getClientInfo());
      }
    }

    return clients;
  }

  public int getCheckMaintenanceInterval() {
    return checkMaintenanceInterval;
  }

  public void setCheckMaintenanceInterval(final int checkTimerInterval) {
    if (checkMaintenanceInterval != checkTimerInterval) {
      checkMaintenanceInterval = checkTimerInterval <= 0 ? 1 : checkTimerInterval;
    }
  }

  /**
   * Returns the server log for the connection identified by the given key.
   * @param clientID the UUID identifying the client
   * @return the server log for the given connection
   */
  public ServerLog getServerLog(final UUID clientID) {
    final ClientInfo client = new ClientInfo(clientID);
    if (containsConnection(client)) {
      return ((EntityDbRemoteAdapter) getConnection(client)).getServerLog();
    }

    return null;
  }

  public boolean isLoggingOn(final UUID clientID) {
    final ClientInfo client = new ClientInfo(clientID);
    for (final EntityDbRemote connection : getConnections().values()) {
      if (((EntityDbRemoteAdapter) connection).getClientInfo().equals(client)) {
        return ((EntityDbRemoteAdapter) connection).getMethodLogger().isEnabled();
      }
    }

    return false;
  }

  public void setLoggingOn(final UUID clientID, final boolean status) {
    final ClientInfo client = new ClientInfo(clientID);
    for (final EntityDbRemote adapter : getConnections().values()) {
      if (((EntityDbRemoteAdapter) adapter).getClientInfo().equals(client)) {
        ((EntityDbRemoteAdapter) adapter).getMethodLogger().setEnabled(status);
        return;
      }
    }
  }

  public int getServerDbPort() {
    return SERVER_DB_PORT;
  }

  public long getStartDate() {
    return startDate;
  }

  @Override
  public void shutdown() throws RemoteException {
    if (isShuttingDown()) {
      return;
    }
    super.shutdown();
    removeConnections(false);
    final String connectInfo = getServerName() + " removed from registry";
    LOG.info(connectInfo);
    System.out.println(connectInfo);
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication
  }

  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    final List<ClientInfo> clients = new ArrayList<ClientInfo>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final EntityDbRemoteAdapter adapter = (EntityDbRemoteAdapter) getConnection(client);
      if (inactiveOnly) {
        if (!adapter.isActive() && adapter.hasBeenInactive(connectionTimeout * 1000)) {
          disconnect(client.getClientID());
        }
      }
      else {
        disconnect(client.getClientID());
      }
    }
  }

  public static Collection<EntityDefinition> getEntityDefinitions() {
    return new ArrayList<EntityDefinition>(EntityRepository.getEntityDefinitions().values());
  }

  public static void loadDomainModel(final String domainClassName) throws ClassNotFoundException,
          InstantiationException, IllegalAccessException {
    loadDomainModel((URI) null, domainClassName);
  }

  public static void loadDomainModel(final URI location, final String domainClassName) throws ClassNotFoundException,
          IllegalAccessException, InstantiationException {
    loadDomainModel(location == null ? null : new URI[] {location}, domainClassName);
  }

  public static void loadDomainModel(final URI[] locations, final String domainClassName) throws ClassNotFoundException,
          IllegalAccessException, InstantiationException {
    final String message = "Server loading domain model class '" + domainClassName + "' from"
            + (locations == null || locations.length == 0 ? " classpath" : " jars: ") + Util.getArrayContentsAsString(locations, false);
    LOG.info(message);
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run() {
        try {
          if (locations == null || locations.length == 0) {
            Class.forName(domainClassName);
          }
          else {
            final URL[] locationsURL = new URL[locations.length];
            int i = 0;
            for (final URI uri : locations) {
              locationsURL[i++] = uri.toURL();
            }
            Class.forName(domainClassName.trim(), true, new URLClassLoader(locationsURL, ClassLoader.getSystemClassLoader()));
            LOG.info("Domain class loaded: " + domainClassName);
          }

          return null;
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Override
  protected final void doDisconnect(final EntityDbRemote connection) throws RemoteException {
    try {
      ((EntityDbRemoteAdapter) connection).disconnect();
      LOG.debug(((EntityDbRemoteAdapter) connection).getClientInfo() + " disconnected");
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  @Override
  protected final EntityDbRemoteAdapter doConnect(final ClientInfo info) throws RemoteException {
    final EntityDbRemoteAdapter remoteAdapter = new EntityDbRemoteAdapter(this, database, info, SERVER_DB_PORT, CLIENT_LOGGING_ENABLED);
    LOG.debug(info + " connected");

    return remoteAdapter;
  }

  private void loadDefaultDomainModels() throws RemoteException {
    final String domainModelClasses = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    if (domainModelClasses == null || domainModelClasses.length() == 0) {
      return;
    }

    final String[] classes = domainModelClasses.split(",");
    final String domainModelJars = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_JARS);
    final String[] jars = domainModelJars == null || domainModelJars.length() == 0 ? null : domainModelJars.split(",");
    try {
      final URI[] jarURIs = jars == null ? null : Util.getURIs(jars);
      for (final String classname : classes) {
        loadDomainModel(jarURIs, classname);
      }
    }
    catch (Exception e) {
      throw new RemoteException("Exception while loading default domain models", e);
    }
  }

  private void startConnectionCheckTimer() {
    if (connectionMaintenanceTimer != null) {
      connectionMaintenanceTimer.cancel();
    }

    connectionMaintenanceTimer = new Timer(true);
    connectionMaintenanceTimer.schedule(new TimerTask() {
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

  private static String initializeServerName(String host, String sid) {
    return Configuration.getValue(Configuration.SERVER_NAME_PREFIX)
            + " " + Util.getVersion() + " @ " + (sid != null ? sid.toUpperCase() : host.toUpperCase())
            + " [id:" + Long.toHexString(System.currentTimeMillis()) + "]";
  }

  private static void initializeRegistry() throws RemoteException {
    Registry localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
    try {
      localRegistry.list();
    }
    catch (Exception e) {
      LOG.info("Server creating registry on port: " + Registry.REGISTRY_PORT);
      LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    }
  }
}

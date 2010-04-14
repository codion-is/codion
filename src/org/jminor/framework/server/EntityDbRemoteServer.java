/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.User;
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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * The remote server class, responsible for handling remote db connection requests.
 */
public class EntityDbRemoteServer extends UnicastRemoteObject implements EntityDbServer {

  private static final Logger log = Util.getLogger(EntityDbRemoteServer.class);

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

    if (serverPortProperty == null)
      throw new RuntimeException("Required server property missing: " + Configuration.SERVER_PORT);
    if (serverDbPortProperty == null)
      throw new RuntimeException("Required server property missing: " + Configuration.SERVER_DB_PORT);

    SERVER_PORT = Integer.parseInt(serverPortProperty);
    SERVER_DB_PORT = Integer.parseInt(serverDbPortProperty);

    try {
      initializeRegistry();
    }
    catch (RemoteException re) {
      throw new RuntimeException(re);
    }
  }

  private final String serverName;
  private final Date startDate = new Date();
  private final Map<ClientInfo, EntityDbRemoteAdapter> connections =
          Collections.synchronizedMap(new HashMap<ClientInfo, EntityDbRemoteAdapter>());

  private Timer connectionMaintenanceTimer;
  private int checkMaintenanceInterval = 30; //seconds
  private int connectionTimeout = 120; //seconds

  /**
   * Constructs a new EntityDbRemoteServer and binds it to the given registry
   * @param database the Database implementation
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  EntityDbRemoteServer(final Database database) throws RemoteException {
    super(SERVER_PORT, SSL_CONNECTION_ENABLED ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            SSL_CONNECTION_ENABLED ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    loadDefaultDomainModels();
    this.database = database;
    EntityDbRemoteAdapter.initConnectionPools(database);
    final String host = database.getHost();
    final String port = database.getPort();
    final String sid = database.getSid();
    if (host == null || host.length() == 0)
      throw new RuntimeException("Database host must be specified (" + Database.DATABASE_HOST + ")");
    if (!database.isEmbedded() && (port == null || port.length() == 0))
      throw new RuntimeException("Database port must be specified (" + Database.DATABASE_PORT + ")");

    serverName = Configuration.getValue(Configuration.SERVER_NAME_PREFIX)
            + " " + Util.getVersion() + " @ " + (sid != null ? sid.toUpperCase() : host.toUpperCase())
            + " [id:" + Long.toHexString(System.currentTimeMillis()) + "]";
    startConnectionCheckTimer();
    getRegistry().rebind(getServerName(), this);
    final String connectInfo = getServerName() + " bound to registry";
    log.info(connectInfo);
    System.out.println(connectInfo);
  }

  public Registry getRegistry() throws RemoteException {
    return LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
  }

  public Database getDatabase() {
    return database;
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
  public int getServerLoad() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  public EntityDbRemote connect(final User user, final String connectionKey, final String clientTypeID) throws RemoteException {
    if (connectionKey == null)
      return null;

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

  public Collection<ClientInfo> getClients() throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        clients.add(adapter.getClientInfo());
    }

    return clients;
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

  public Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    synchronized (connections) {
      for (final EntityDbRemoteAdapter adapter : connections.values())
        if (adapter.getClientInfo().getClientTypeID().equals(clientTypeID))
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
      if (connections.containsKey(client))
        return connections.get(client).getServerLog();
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
      getRegistry().unbind(serverName);
    }
    catch (NotBoundException e) {
      log.error(this, e);
    }
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {
      log.warn(e);
    }
    removeConnections(false);
    final String connectInfo = getServerName() + " removed from registry";
    log.info(connectInfo);
    System.out.println(connectInfo);
    if (database.isEmbedded())
      database.shutdownEmbedded(null);//todo does not work when shutdown requires user authentication
  }

  public void removeConnections(final boolean inactiveOnly) throws RemoteException {
    synchronized (connections) {
      final List<ClientInfo> clients = new ArrayList<ClientInfo>(connections.keySet());
      for (final ClientInfo client : clients) {
        final EntityDbRemoteAdapter adapter = connections.get(client);
        if (inactiveOnly) {
          if (!adapter.isActive() && adapter.hasBeenInactive(getConnectionTimeout() * 1000))
            adapter.disconnect();
        }
        else
          adapter.disconnect();
      }
    }
  }

  public static Map<String, EntityDefinition> getEntityDefinitions() {
    return EntityRepository.getEntityDefinitions();
  }

  public static void loadDomainModel(final String domainClassName) throws ClassNotFoundException,
          InstantiationException, IllegalAccessException {
    loadDomainModel((URL) null, domainClassName);
  }

  public static void loadDomainModel(final URL location, final String domainClassName) throws ClassNotFoundException,
          IllegalAccessException, InstantiationException {
    loadDomainModel(location == null ? null : new URL[] {location}, domainClassName);
  }

  public static void loadDomainModel(final URL[] locations, final String domainClassName) throws ClassNotFoundException,
          IllegalAccessException, InstantiationException {
    final String message = "Server loading domain model class '" + domainClassName + "' from"
            + (locations == null || locations.length == 0 ? " classpath" : " jars: ") + Util.getArrayContentsAsString(locations, false);
    log.info(message);
    System.out.println(message);
    if (locations == null || locations.length == 0)
      Class.forName(domainClassName);
    else
      Class.forName(domainClassName, true, new URLClassLoader(locations, ClassLoader.getSystemClassLoader()));
  }

  private void loadDefaultDomainModels() throws RemoteException {
    final String domainModelClasses = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    if (domainModelClasses == null || domainModelClasses.length() == 0)
      return;

    final String[] classes = domainModelClasses.split(",");
    final String domainModelJars = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_JARS);
    final String[] jars = domainModelJars == null || domainModelJars.length() == 0 ? null : domainModelJars.split(",");
    try {
      final URL[] jarURLs = jars == null ? null : getJarURLs(jars);
      for (final String classname : classes)
        loadDomainModel(jarURLs, classname);
    }
    catch (Exception e) {
      throw new RemoteException("Exception while loading default domain models", e);
    }
  }

  private URL[] getJarURLs(final String[] jars) throws MalformedURLException {
    final URL[] urls = new URL[jars.length];
    for (int i = 0; i < jars.length; i++) {
      if (jars[i].toLowerCase().startsWith("http"))
        urls[i] = new URL(jars[i]);
      else
        urls[i] = new File(jars[i]).toURI().toURL();
    }

    return urls;
  }

  private void startConnectionCheckTimer() {
    if (connectionMaintenanceTimer != null)
      connectionMaintenanceTimer.cancel();

    connectionMaintenanceTimer = new Timer(true);
    connectionMaintenanceTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        maintainConnections();
      }
    }, new Date(), checkMaintenanceInterval * 1000);
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
      if (log.isDebugEnabled())
        log.debug("Connection removed: " + client);
      final EntityDbRemoteAdapter adapter = connections.remove(client);
      if (logout)
        adapter.disconnect();
    }
  }

  private EntityDbRemoteAdapter doConnect(final ClientInfo client) throws RemoteException {
    final EntityDbRemoteAdapter remoteAdapter = new EntityDbRemoteAdapter(database, client, SERVER_DB_PORT, CLIENT_LOGGING_ENABLED);
    remoteAdapter.eventLogout().addListener(new ActionListener() {
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
    if (log.isDebugEnabled())
      log.debug("Connection added: " + client);

    return remoteAdapter;
  }

  private static void initializeRegistry() throws RemoteException {
    Registry localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
    try {
      localRegistry.list();
    }
    catch (Exception e) {
      log.info("Server creating registry on port: " + Registry.REGISTRY_PORT);
      LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    }
  }
}

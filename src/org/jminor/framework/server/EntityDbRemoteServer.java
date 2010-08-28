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
import org.jminor.framework.domain.Entities;

import org.apache.log4j.Logger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * The remote server class, responsible for handling remote EntityDb connection requests.
 */
final class EntityDbRemoteServer extends AbstractRemoteServer<EntityDbRemote> {

  private static final long serialVersionUID = 1;

  static final Logger LOG = Util.getLogger(EntityDbRemoteServer.class);

  private static final boolean CLIENT_LOGGING_ENABLED =
          Configuration.getBooleanValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED);
  static final boolean SSL_CONNECTION_ENABLED =
          Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_SSL_ENABLED);

  private static final int DEFAULT_CHECK_INTERVAL_MS = 30000;
  private static final int DEFAULT_TIMEOUT_MS = 120000;

  private static final int SERVER_PORT;
  private static final int SERVER_DB_PORT;

  private final Database database;

  static {
    final String serverPortProperty = Configuration.getStringValue(Configuration.SERVER_PORT);
    final String serverDbPortProperty = Configuration.getStringValue(Configuration.SERVER_DB_PORT);

    Util.require(Configuration.SERVER_PORT, serverPortProperty);
    Util.require(Configuration.SERVER_DB_PORT, serverDbPortProperty);

    SERVER_PORT = Integer.parseInt(serverPortProperty);
    SERVER_DB_PORT = Integer.parseInt(serverDbPortProperty);

    try {
      Util.initializeRegistry();
    }
    catch (RemoteException re) {
      throw new RuntimeException(re);
    }
  }

  private final long startDate = System.currentTimeMillis();

  private Timer connectionMaintenanceTimer;
  private int maintenanceInterval = DEFAULT_CHECK_INTERVAL_MS;
  private int connectionTimeout = DEFAULT_TIMEOUT_MS;

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
    if (Util.nullOrEmpty(host)) {
      throw new IllegalArgumentException("Database host must be specified (" + Database.DATABASE_HOST + ")");
    }
    if (!database.isEmbedded() && Util.nullOrEmpty(port)) {
      throw new IllegalArgumentException("Database port must be specified (" + Database.DATABASE_PORT + ")");
    }

    startConnectionCheckTimer();
    getRegistry().rebind(getServerName(), this);
    final String connectInfo = getServerName() + " bound to registry";
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  /** {@inheritDoc} */
  public int getServerLoad() throws RemoteException {
    return EntityDbRemoteAdapter.getRequestsPerSecond();
  }

  /**
   * @return the underlying Database implementation class
   */
  Database getDatabase() {
    return database;
  }

  /**
   * @return the connection timeout
   */
  int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * @param timeout the new timeout value
   */
  void setConnectionTimeout(final int timeout) {
    this.connectionTimeout = timeout;
  }

  /**
   * @return info on all connected users
   * @throws RemoteException in case of an exception
   */
  Collection<User> getUsers() throws RemoteException {
    final Set<User> users = new HashSet<User>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      users.add(adapter.getUser());
    }

    return users;
  }

  /**
   * @return info on all connected clients
   * @throws RemoteException in case of an exception
   */
  Collection<ClientInfo> getClients() throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      clients.add(((EntityDbRemoteAdapter) adapter).getClientInfo());
    }

    return clients;
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   * @throws RemoteException in case of an exception
   */
  Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      if (user == null || adapter.getUser().equals(user)) {
        clients.add(((EntityDbRemoteAdapter) adapter).getClientInfo());
      }
    }

    return clients;
  }

  /**
   * @param clientTypeID the client type ID
   * @return all clients of the given type
   * @throws RemoteException in case of an exception
   */
  Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final EntityDbRemote adapter : getConnections().values()) {
      if (((EntityDbRemoteAdapter) adapter).getClientInfo().getClientTypeID().equals(clientTypeID)) {
        clients.add(((EntityDbRemoteAdapter) adapter).getClientInfo());
      }
    }

    return clients;
  }

  /**
   * @return the maintenance check interval in ms
   */
  int getMaintenanceInterval() {
    return maintenanceInterval;
  }

  /**
   * @param maintenanceInterval the new maintenance interval in ms
   */
  void setMaintenanceInterval(final int maintenanceInterval) {
    if (this.maintenanceInterval != maintenanceInterval) {
      this.maintenanceInterval = maintenanceInterval <= 0 ? 1 : maintenanceInterval;
    }
  }

  /**
   * Returns the server log for the connection identified by the given key.
   * @param clientID the UUID identifying the client
   * @return the server log for the given connection
   */
  ServerLog getServerLog(final UUID clientID) {
    final ClientInfo client = new ClientInfo(clientID);
    if (containsConnection(client)) {
      return ((EntityDbRemoteAdapter) getConnection(client)).getServerLog();
    }

    return null;
  }

  /**
   * @param clientID the client ID
   * @return true if logging is enabled for the given client
   */
  boolean isLoggingOn(final UUID clientID) {
    final ClientInfo client = new ClientInfo(clientID);
    for (final EntityDbRemote connection : getConnections().values()) {
      if (((EntityDbRemoteAdapter) connection).getClientInfo().equals(client)) {
        return ((EntityDbRemoteAdapter) connection).getMethodLogger().isEnabled();
      }
    }

    return false;
  }

  /**
   * @param clientID the client ID
   * @param status the new logging status
   */
  void setLoggingOn(final UUID clientID, final boolean status) {
    final ClientInfo client = new ClientInfo(clientID);
    for (final EntityDbRemote adapter : getConnections().values()) {
      if (((EntityDbRemoteAdapter) adapter).getClientInfo().equals(client)) {
        ((EntityDbRemoteAdapter) adapter).getMethodLogger().setEnabled(status);
        return;
      }
    }
  }

  /**
   * @return the port this server exports client db connections on
   */
  int getServerDbPort() {
    return SERVER_DB_PORT;
  }

  /**
   * @return the start date of the server
   */
  long getStartDate() {
    return startDate;
  }

  /**
   * @param inactiveOnly if true only inactive connections are culled
   * @throws RemoteException in case of an exception
   */
  void removeConnections(final boolean inactiveOnly) throws RemoteException {
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

  /**
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  static Map<String,String> getEntityDefinitions() {
    return Entities.getEntityDefinitions();
  }

  static void loadDomainModel(final String domainClassName) throws ClassNotFoundException,
          InstantiationException, IllegalAccessException {
    loadDomainModel((URI) null, domainClassName);
  }

  static void loadDomainModel(final URI location, final String domainClassName) throws ClassNotFoundException,
          IllegalAccessException {
    loadDomainModel(location == null ? null : Arrays.asList(location), domainClassName);
  }

  static void loadDomainModel(final Collection<URI> locations, final String domainClassName) throws ClassNotFoundException,
          IllegalAccessException {
    final String message = "Server loading domain model class '" + domainClassName + "' from"
            + (locations == null || locations.isEmpty() ? " classpath" : " jars: ")
            + Util.getCollectionContentsAsString(locations, false);
    LOG.info(message);
    AccessController.doPrivileged(new DomainModelAction(locations, domainClassName));
  }

  /** {@inheritDoc} */
  @Override
  protected void handleShutdown() throws RemoteException {
    removeConnections(false);
    final String connectInfo = getServerName() + " removed from registry";
    LOG.info(connectInfo);
    System.out.println(connectInfo);
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication, jminor.db.shutdownUser hmmm
  }

  /** {@inheritDoc} */
  @Override
  protected void doDisconnect(final EntityDbRemote connection) throws RemoteException {
    connection.disconnect();
    LOG.debug(((EntityDbRemoteAdapter) connection).getClientInfo() + " disconnected");
  }

  /** {@inheritDoc} */
  @Override
  protected EntityDbRemoteAdapter doConnect(final ClientInfo clientInfo) throws RemoteException {
    final EntityDbRemoteAdapter remoteAdapter = new EntityDbRemoteAdapter(database, clientInfo, SERVER_DB_PORT,
            CLIENT_LOGGING_ENABLED, SSL_CONNECTION_ENABLED);
    remoteAdapter.addDisconnectListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          disconnect(remoteAdapter.getClientInfo().getClientID());
        }
        catch (RemoteException ex) {
          LOG.error(ex);
        }
      }
    });
    LOG.debug(clientInfo + " connected");

    return remoteAdapter;
  }

  private void loadDefaultDomainModels() throws RemoteException {
    final String domainModelClasses = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    if (Util.nullOrEmpty(domainModelClasses)) {
      return;
    }

    final String[] classes = domainModelClasses.split(",");
    final String domainModelJars = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_JARS);
    final String[] jars = Util.nullOrEmpty(domainModelJars) ? null : domainModelJars.split(",");
    try {
      final Collection<URI> jarURIs = jars == null ? null : Util.getURIs(Arrays.asList(jars));
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
        try {
          maintainConnections();
        }
        catch (RemoteException e) {
          throw new RuntimeException(e);
        }
      }
    }, new Date(), maintenanceInterval);
  }

  private void maintainConnections() throws RemoteException {
    removeConnections(true);
  }

  private static String initializeServerName(final String host, final String sid) {
    return Configuration.getValue(Configuration.SERVER_NAME_PREFIX)
            + " " + Util.getVersion() + " @ " + (sid != null ? sid.toUpperCase() : host.toUpperCase())
            + " [id:" + Long.toHexString(System.currentTimeMillis()) + "]";
  }

  private static final class DomainModelAction implements PrivilegedAction<Object> {

    private final Collection<URI> locations;
    private final String domainClassName;

    private DomainModelAction(final Collection<URI> locations, final String domainClassName) {
      this.locations = locations;
      this.domainClassName = domainClassName;
    }

    /** {@inheritDoc} */
    public Object run() {
      try {
        if (locations == null || locations.isEmpty()) {
          Class.forName(domainClassName);
        }
        else {
          final URL[] locationsURL = new URL[locations.size()];
          int i = 0;
          for (final URI uri : locations) {
            locationsURL[i++] = uri.toURL();
          }
          Class.forName(domainClassName.trim(), true, new URLClassLoader(locationsURL, ClassLoader.getSystemClassLoader()));
          LOG.info("Domain class loaded: " + domainClassName);
        }

        return null;
      }
      catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.AbstractRemoteServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.ServerLog;
import org.jminor.common.server.ServerUtil;
import org.jminor.common.server.web.WebStartServer;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The remote server class, responsible for handling requests for RemoteEntityConnections.
 */
final class EntityConnectionServer extends AbstractRemoteServer<RemoteEntityConnection> {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(EntityConnectionServer.class);

  private static final int DEFAULT_CHECK_INTERVAL_MS = 30000;

  private final int registryPort;
  private final Database database;
  private final boolean sslEnabled;
  private final boolean clientLoggingEnabled = Configuration.getBooleanValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED);

  private final long startDate = System.currentTimeMillis();

  private WebStartServer webServer;
  private Timer connectionTimeoutTimer;
  private int maintenanceInterval = DEFAULT_CHECK_INTERVAL_MS;
  private int connectionTimeout = Configuration.getIntValue(Configuration.SERVER_CONNECTION_TIMEOUT);

  /**
   * Constructs a new EntityConnectionServer and binds it to a registry on the given port
   * @param serverName the serverName
   * @param serverPort the port on which to make the server accessible
   * @param registryPort the registry port to use
   * @param database the Database implementation
   * @param sslEnabled if true then ssl is enabled
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @throws java.rmi.RemoteException in case of a remote exception
   * @throws ClassNotFoundException in case the domain model classes are not found on the classpath or
   * if the jdbc driver class is not found
   * @throws DatabaseException in case of an exception while constructing the initial pooled connections
   * @see Configuration#SERVER_DOMAIN_MODEL_CLASSES
   */
  EntityConnectionServer(final String serverName, final int serverPort, final int registryPort, final Database database,
                         final boolean sslEnabled, final int connectionLimit) throws RemoteException, ClassNotFoundException, DatabaseException {
    super(serverPort, serverName,
            sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.database = database;
    this.registryPort = registryPort;
    this.sslEnabled = sslEnabled;
    loadDefaultDomainModels();
    loadLoginProxies();
    RemoteEntityConnectionImpl.initializeConnectionPools(database);
    setConnectionLimit(connectionLimit);
    startConnectionTimeoutTimer();
    startWebServer();
    ServerUtil.initializeRegistry(registryPort);
    ServerUtil.getRegistry(registryPort).rebind(getServerName(), this);
    final String connectInfo = getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  /** {@inheritDoc} */
  @Override
  public int getServerLoad() throws RemoteException {
    return RemoteEntityConnectionImpl.getRequestsPerSecond();
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
    for (final RemoteEntityConnection connection : getConnections().values()) {
      users.add(connection.getUser());
    }

    return users;
  }

  /**
   * @return info on all connected clients
   * @throws RemoteException in case of an exception
   */
  Collection<ClientInfo> getClients() throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<ClientInfo>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      clients.add(((RemoteEntityConnectionImpl) connection).getClientInfo());
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
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (user == null || connection.getUser().equals(user)) {
        clients.add(((RemoteEntityConnectionImpl) connection).getClientInfo());
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
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (((RemoteEntityConnectionImpl) connection).getClientInfo().getClientTypeID().equals(clientTypeID)) {
        clients.add(((RemoteEntityConnectionImpl) connection).getClientInfo());
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
      return ((RemoteEntityConnectionImpl) getConnection(client)).getServerLog();
    }

    return null;
  }

  /**
   * @param clientID the client ID
   * @return true if logging is enabled for the given client
   */
  boolean isLoggingEnabled(final UUID clientID) {
    final ClientInfo client = new ClientInfo(clientID);
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (((RemoteEntityConnectionImpl) connection).getClientInfo().equals(client)) {
        return ((RemoteEntityConnectionImpl) connection).getMethodLogger().isEnabled();
      }
    }

    return false;
  }

  /**
   * @param clientID the client ID
   * @param status the new logging status
   */
  void setLoggingEnabled(final UUID clientID, final boolean status) {
    final ClientInfo client = new ClientInfo(clientID);
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (((RemoteEntityConnectionImpl) connection).getClientInfo().equals(client)) {
        ((RemoteEntityConnectionImpl) connection).getMethodLogger().setEnabled(status);
        return;
      }
    }
  }

  /**
   * @return the start date of the server
   */
  long getStartDate() {
    return startDate;
  }

  /**
   * @return the port of the registry this server is using
   */
  int getRegistryPort() {
    return registryPort;
  }

  /**
   * @return true if connections to this server are ssl enabled
   */
  boolean isSslEnabled() {
    return sslEnabled;
  }

  /**
   * @param inactiveOnly if true only inactive connections are culled
   * @throws RemoteException in case of an exception
   */
  void removeConnections(final boolean inactiveOnly) throws RemoteException {
    final List<ClientInfo> clients = new ArrayList<ClientInfo>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final RemoteEntityConnectionImpl connection = (RemoteEntityConnectionImpl) getConnection(client);
      if (inactiveOnly) {
        if (!connection.isActive() && connection.hasBeenInactive(connectionTimeout)) {
          disconnect(client.getClientID());
        }
      }
      else {
        disconnect(client.getClientID());
      }
    }
  }

  /**
   * Starts the web server in case a web document root is specified
   * @see Configuration#WEB_SERVER_DOCUMENT_ROOT
   */
  private void startWebServer() {
    final String webDocumentRoot = Configuration.getStringValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    if (webDocumentRoot != null) {
      final int port = Configuration.getIntValue(Configuration.WEB_SERVER_PORT);
      LOG.info("Starting web server on port: {}, document root: {}", port, webDocumentRoot);
      webServer = new WebStartServer(webDocumentRoot, port);
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute(new Runnable() {
        @Override
        public void run() {
          webServer.serve();
        }
      });
    }
  }

  private void shutdownWebServer() {
    if (webServer != null) {
      LOG.info("Shutting down web server");
      webServer.stop();
    }
  }

  /**
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  static Map<String, String> getEntityDefinitions() {
    return Entities.getDefinitions();
  }

  /** {@inheritDoc} */
  @Override
  protected void handleShutdown() throws RemoteException {
    removeConnections(false);
    shutdownWebServer();
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication, jminor.db.shutdownUser hmmm
  }

  /** {@inheritDoc} */
  @Override
  protected void doDisconnect(final RemoteEntityConnection connection) throws RemoteException {
    connection.disconnect();
    LOG.debug("{} disconnected", ((RemoteEntityConnectionImpl) connection).getClientInfo());
  }

  /** {@inheritDoc} */
  @Override
  protected RemoteEntityConnectionImpl doConnect(final ClientInfo clientInfo) throws RemoteException {
    try {
      final RemoteEntityConnectionImpl connection = new RemoteEntityConnectionImpl(database, clientInfo, getServerPort(),
              clientLoggingEnabled, sslEnabled);
      connection.addDisconnectListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
        public void eventOccurred() {
          try {
            disconnect(connection.getClientInfo().getClientID());
          }
          catch (RemoteException ex) {
            LOG.error(ex.getMessage(), ex);
          }
        }
      });
      LOG.debug("{} connected", clientInfo);

      return connection;
    }
    catch (DatabaseException e) {
      throw new RemoteException(e.getMessage(), e);
    }
    catch (ClassNotFoundException e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  private void loadLoginProxies() throws ClassNotFoundException {
    final String loginProxyClasses = Configuration.getStringValue(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    if (Util.nullOrEmpty(loginProxyClasses)) {
      return;
    }

    final String[] classes = loginProxyClasses.split(",");
    for (final String className : classes) {
      loadLoginProxy(className.trim());
    }
  }

  private void loadLoginProxy(final String loginProxyClassName) throws ClassNotFoundException {
    final String message = "Server loading login proxy class '" + loginProxyClassName + "' from classpath";
    LOG.info(message);
    final Class loginProxyClass = Class.forName(loginProxyClassName);
    try {
      final LoginProxy proxy = (LoginProxy) loginProxyClass.getConstructor().newInstance();
      setLoginProxy(proxy.getClientTypeID(), proxy);
    }
    catch (Exception ex) {
      LOG.error("Exception while instantiating LoginProxy: " + loginProxyClassName, ex);
      throw new RuntimeException(ex);
    }
  }

  private static void loadDefaultDomainModels() throws ClassNotFoundException {
    final String domainModelClasses = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    if (Util.nullOrEmpty(domainModelClasses)) {
      return;
    }

    final String[] classes = domainModelClasses.split(",");
    for (final String className : classes) {
      loadDomainModel(className.trim());
    }
  }

  private static void loadDomainModel(final String domainClassName) throws ClassNotFoundException {
    final String message = "Server loading domain model class '" + domainClassName + "' from classpath";
    LOG.info(message);
    Class.forName(domainClassName);
  }

  private void startConnectionTimeoutTimer() {
    if (connectionTimeoutTimer != null) {
      connectionTimeoutTimer.cancel();
    }

    connectionTimeoutTimer = new Timer(true);
    connectionTimeoutTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          removeConnections(true);
        }
        catch (RemoteException e) {
          throw new RuntimeException(e);
        }
      }
    }, new Date(), maintenanceInterval);
  }
}

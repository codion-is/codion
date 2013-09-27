/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.AbstractRemoteServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The remote server class, responsible for handling requests for RemoteEntityConnections.
 */
public final class EntityConnectionServer extends AbstractRemoteServer<RemoteEntityConnection> {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(EntityConnectionServer.class);

  private static final int DEFAULT_MAINTENANCE_INTERVAL_MS = 30000;

  private final AuxiliaryServer webServer;
  private final long startDate = System.currentTimeMillis();
  private final int registryPort;
  private final Database database;
  private final boolean sslEnabled;
  private final boolean clientLoggingEnabled;
  private final TaskScheduler connectionTimeoutScheduler = new TaskScheduler(new Runnable() {
    /** {@inheritDoc} */
    @Override
    public void run() {
      try {
        removeConnections(true);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }
  }, DEFAULT_MAINTENANCE_INTERVAL_MS, DEFAULT_MAINTENANCE_INTERVAL_MS, TimeUnit.MILLISECONDS).start();

  private int connectionTimeout;

  /**
   * Constructs a new EntityConnectionServer and binds it to a registry on the given port
   * @param serverName the serverName
   * @param serverPort the port on which to make the server accessible
   * @param registryPort the registry port to use
   * @param database the Database implementation
   * @param sslEnabled if true then ssl is enabled
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @throws RemoteException in case of a remote exception
   * @throws ClassNotFoundException in case the domain model classes are not found on the classpath or
   * if the jdbc driver class is not found
   * @throws DatabaseException in case of an exception while constructing the initial pooled connections
   */
  public EntityConnectionServer(final String serverName, final int serverPort, final int registryPort, final Database database,
                                final boolean sslEnabled, final int connectionLimit, final Collection<String> domainModelClassNames,
                                final Collection<String> loginProxyClassNames, final Collection<User> initialPoolUsers,
                                final String webDocumentRoot, final int webServerPort, final boolean clientLoggingEnabled,
                                final int connectionTimeout)
          throws RemoteException, ClassNotFoundException, DatabaseException {
    super(serverPort, serverName,
            sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    try {
      this.database = Util.rejectNullValue(database, "database");
      this.registryPort = registryPort;
      this.sslEnabled = sslEnabled;
      this.clientLoggingEnabled = clientLoggingEnabled;
      setConnectionTimeout(connectionTimeout);
      loadDomainModels(domainModelClassNames);
      if (initialPoolUsers != null) {
        ConnectionPools.initializeConnectionPools(database, initialPoolUsers);
      }
      loadLoginProxies(loginProxyClassNames);
      setConnectionLimit(connectionLimit);
      webServer = initializeWebServer(webDocumentRoot, webServerPort);
      if (webServer != null) {
        startWebServer(webDocumentRoot, webServerPort);
      }
    }
    catch (Error e) {
      throw logShutdownAndReturn(e, this);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getServerLoad() throws RemoteException {
    return DefaultRemoteEntityConnection.getRequestsPerSecond();
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
   * @param timeout the new timeout value in milliseconds
   * @throws IllegalArgumentException in case timeout is less than zero
   */
  void setConnectionTimeout(final int timeout) {
    if (timeout < 0) {
      throw new IllegalArgumentException("Connection timeout must be a positive integer");
    }
    this.connectionTimeout = timeout;
  }

  /**
   * @return info on all connected users
   * @throws RemoteException in case of an exception
   */
  Collection<User> getUsers() throws RemoteException {
    final Set<User> users = new HashSet<>();
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
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      clients.add(((DefaultRemoteEntityConnection) connection).getClientInfo());
    }

    return clients;
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   * @throws RemoteException in case of an exception
   */
  Collection<ClientInfo> getClients(final User user) throws RemoteException {
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (user == null || connection.getUser().equals(user)) {
        clients.add(((DefaultRemoteEntityConnection) connection).getClientInfo());
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
    final Collection<ClientInfo> clients = new ArrayList<>();
    for (final RemoteEntityConnection connection : getConnections().values()) {
      if (((DefaultRemoteEntityConnection) connection).getClientInfo().getClientTypeID().equals(clientTypeID)) {
        clients.add(((DefaultRemoteEntityConnection) connection).getClientInfo());
      }
    }

    return clients;
  }

  /**
   * @return the maintenance check interval in ms
   */
  int getMaintenanceInterval() {
    return connectionTimeoutScheduler.getInterval();
  }

  /**
   * @param maintenanceInterval the new maintenance interval in ms
   */
  void setMaintenanceInterval(final int maintenanceInterval) {
    connectionTimeoutScheduler.setInterval(maintenanceInterval);
  }

  /**
   * Returns the server log for the connection identified by the given key.
   * @param clientID the UUID identifying the client
   * @return the server log for the given connection
   */
  ClientLog getClientLog(final UUID clientID) {
    final ClientInfo client = new ClientInfo(clientID);
    if (containsConnection(client)) {
      return ((DefaultRemoteEntityConnection) getConnection(client)).getClientLog();
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
      if (((DefaultRemoteEntityConnection) connection).getClientInfo().equals(client)) {
        return ((DefaultRemoteEntityConnection) connection).isLoggingEnabled();
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
      if (((DefaultRemoteEntityConnection) connection).getClientInfo().equals(client)) {
        ((DefaultRemoteEntityConnection) connection).setLoggingEnabled(status);
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
    final List<ClientInfo> clients = new ArrayList<>(getConnections().keySet());
    for (final ClientInfo client : clients) {
      final DefaultRemoteEntityConnection connection = (DefaultRemoteEntityConnection) getConnection(client);
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
   * Binds this server instance to the registry
   * @throws RemoteException in case of an exception
   */
  void bindToRegistry() throws RemoteException {
    ServerUtil.initializeRegistry(registryPort);
    ServerUtil.getRegistry(registryPort).rebind(getServerName(), this);
    final String connectInfo = getServerName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  /**
   * Starts the web server in case the web document root is specified
   */
  private void startWebServer(final String webDocumentRoot, final Integer webServerPort) {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        LOG.info("Starting web server on port: {}, document root: {}", webServerPort, webDocumentRoot);
        try {
          webServer.start();
        }
        catch (Exception e) {
          LOG.error("Trying to start web server on port: {}, document root: {}", webServerPort, webDocumentRoot);
        }
      }
    });
  }

  /**
   * Stops the web server in case it's running
   */
  private void shutdownWebServer() throws Exception {
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
    connectionTimeoutScheduler.stop();
    removeConnections(false);
    ConnectionPools.closeConnectionPools();
    try {
      shutdownWebServer();
    }
    catch (Exception ignored) {}
    if (database.isEmbedded()) {
      database.shutdownEmbedded(null);
    }//todo does not work when shutdown requires user authentication, jminor.db.shutdownUser hmmm
  }

  /** {@inheritDoc} */
  @Override
  protected void doDisconnect(final RemoteEntityConnection connection) throws RemoteException {
    connection.disconnect();
    LOG.debug("{} disconnected", ((DefaultRemoteEntityConnection) connection).getClientInfo());
  }

  /** {@inheritDoc} */
  @Override
  protected DefaultRemoteEntityConnection doConnect(final ClientInfo clientInfo) throws RemoteException, ServerException.LoginException {
    try {
      final DefaultRemoteEntityConnection connection;
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
      if (connectionPool == null) {
        connection = new DefaultRemoteEntityConnection(database, clientInfo, getServerPort(),
                clientLoggingEnabled, sslEnabled);
      }
      else {
        checkConnectionPoolCredentials(connectionPool.getUser(), clientInfo.getDatabaseUser());
        connection = new DefaultRemoteEntityConnection(connectionPool, database, clientInfo, getServerPort(),
                clientLoggingEnabled, sslEnabled);
      }
      connection.addDisconnectListener(new EventListener() {
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
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.debug(clientInfo + " unable to connect", e);
      throw ServerException.loginException(e.getMessage());
    }
  }

  private void loadLoginProxies(final Collection<String> loginProxyClassNames) throws ClassNotFoundException {
    if (loginProxyClassNames != null) {
      for (final String loginProxyClassName : loginProxyClassNames) {
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
    }
  }

  private AuxiliaryServer initializeWebServer(final String webDocumentRoot, final Integer webServerPort) {
    if (Util.nullOrEmpty(webDocumentRoot)) {
      return null;
    }

    final String webServerClassName = Configuration.getStringValue(Configuration.WEB_SERVER_IMPLEMENTATION_CLASS);
    try {
      return (AuxiliaryServer) Class.forName(webServerClassName).getConstructor(
              EntityConnectionServer.class, String.class, Integer.class).newInstance(this, webDocumentRoot, webServerPort);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks the credentials provided by <code>clientInfo</code> against the credentials
   * found in the connection pool user, assuming the user names match
   * @param connectionPoolUser the connection pool user credentials
   * @param user the user credentials to check
   * @throws DatabaseException in case the password does not match the one in the connection pool user
   */
  private static void checkConnectionPoolCredentials(final User connectionPoolUser, final User user) throws DatabaseException {
    if (!connectionPoolUser.getPassword().equals(user.getPassword())) {
      throw new DatabaseException("Wrong username or password for connection pool");
    }
  }

  private static void loadDomainModels(final Collection<String> domainModelClassNames) throws ClassNotFoundException {
    if (domainModelClassNames != null) {
      for (final String className : domainModelClassNames) {
        final String message = "Server loading domain model class '" + className + "' from classpath";
        LOG.info(message);
        Class.forName(className);
      }
    }
  }

  private static <T> T logShutdownAndReturn(final T exception, final EntityConnectionServer server) {
    LOG.error("Exception on server startup", exception);
    try {
      server.shutdown();
    }
    catch (RemoteException ignored) {}

    return exception;
  }

  /**
   * Auxiliary servers to be run in conjunction with a EntityConnectionServer must implement this interface,
   * as well as provide a constructor with the following signature: (EntityConnectionServer, String, Integer)
   * for the server, file document root and port respectively
   */
  public interface AuxiliaryServer {

    /**
     * Starts the web server
     * @throws Exception in case of an exception
     */
    void start() throws Exception;

    /**
     * Stops the web server
     * @throws Exception in case of an exception
     */
    void stop() throws Exception;
  }
}

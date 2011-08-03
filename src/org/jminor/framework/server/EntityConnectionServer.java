/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.AbstractRemoteServer;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.common.server.web.WebStartServer;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

  private static final boolean CLIENT_LOGGING_ENABLED =
          Configuration.getBooleanValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED);
  static final boolean SSL_CONNECTION_ENABLED =
          Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_SSL_ENABLED);

  private static final int DEFAULT_CHECK_INTERVAL_MS = 30000;
  private static final int DEFAULT_TIMEOUT_MS = 120000;

  static final int REGISTRY_PORT;
  private static final int SERVER_PORT;
  private static final int SERVER_DB_PORT;

  private final Database database;

  static {
    final String serverPortProperty = Configuration.getStringValue(Configuration.SERVER_PORT);
    final String serverDbPortProperty = Configuration.getStringValue(Configuration.SERVER_DB_PORT);

    Util.require(Configuration.SERVER_PORT, serverPortProperty);
    Util.require(Configuration.SERVER_DB_PORT, serverDbPortProperty);

    REGISTRY_PORT = Configuration.getIntValue(Configuration.REGISTRY_PORT_NUMBER);
    SERVER_PORT = Integer.parseInt(serverPortProperty);
    SERVER_DB_PORT = Integer.parseInt(serverDbPortProperty);

    try {
      loadDefaultDomainModels();
      Util.initializeRegistry(REGISTRY_PORT);
    }
    catch (Throwable re) {
      LOG.error("Exception while initializing server", re);
      throw new RuntimeException(re);
    }
  }

  private final long startDate = System.currentTimeMillis();

  private WebStartServer webServer;
  private Timer connectionTimeoutTimer;
  private int maintenanceInterval = DEFAULT_CHECK_INTERVAL_MS;
  private int connectionTimeout = DEFAULT_TIMEOUT_MS;

  /**
   * Constructs a new RemoteEntityServer and binds it to the given registry
   * @param database the Database implementation
   * @throws java.rmi.RemoteException in case of a remote exception
   * @throws ClassNotFoundException in case the domain model classes are not found on the classpath
   * @see Configuration#SERVER_DOMAIN_MODEL_CLASSES
   */
  EntityConnectionServer(final Database database) throws RemoteException, ClassNotFoundException {
    super(SERVER_PORT, initializeServerName(database.getHost(), database.getSid()),
            SSL_CONNECTION_ENABLED ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            SSL_CONNECTION_ENABLED ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.database = database;
    RemoteEntityConnectionImpl.initializeConnectionPools(database);
    setConnectionLimit(Configuration.getIntValue(Configuration.SERVER_CONNECTION_LIMIT));
    startConnectionTimeoutTimer();
    Util.getRegistry(REGISTRY_PORT).rebind(getServerName(), this);
    startWebServer();
    final String connectInfo = getServerName() + " bound to registry on port: " + REGISTRY_PORT;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  /** {@inheritDoc} */
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
  boolean isLoggingOn(final UUID clientID) {
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
  void setLoggingOn(final UUID clientID, final boolean status) {
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
      LOG.info("Starting web server on port: " + port + ", document root: " + webDocumentRoot);
      webServer = new WebStartServer(webDocumentRoot, port);
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute(new Runnable() {
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

  /**
   * @return the port this server exports client db connections on
   */
  static int getServerDbPort() {
    return SERVER_DB_PORT;
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
    LOG.debug(((RemoteEntityConnectionImpl) connection).getClientInfo() + " disconnected");
  }

  /** {@inheritDoc} */
  @Override
  protected RemoteEntityConnectionImpl doConnect(final ClientInfo clientInfo) throws RemoteException {
    final RemoteEntityConnectionImpl connection = new RemoteEntityConnectionImpl(database, clientInfo, SERVER_DB_PORT,
            CLIENT_LOGGING_ENABLED, SSL_CONNECTION_ENABLED);
    connection.addDisconnectListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          disconnect(connection.getClientInfo().getClientID());
        }
        catch (RemoteException ex) {
          LOG.error(ex.getMessage(), ex);
        }
      }
    });
    LOG.debug(clientInfo + " connected");

    return connection;
  }

  private static void loadDefaultDomainModels() throws ClassNotFoundException {
    final String domainModelClasses = Configuration.getStringValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    if (Util.nullOrEmpty(domainModelClasses)) {
      return;
    }

    final String[] classes = domainModelClasses.split(",");
    for (final String className : classes) {
      loadDomainModel(className);
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
            + " " + Util.getVersion() + "@" + (sid != null ? sid.toUpperCase() : host.toUpperCase());
  }
}

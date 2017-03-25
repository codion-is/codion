/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.Map;

/**
 * A remote server class, responsible for handling requests for RemoteEntityConnections.
 */
public final class DefaultEntityConnectionServer extends AbstractEntityConnectionServer<DefaultRemoteEntityConnection> {

  /**
   * Constructs a new DefaultEntityConnectionServer and binds it to a registry on the given port
   * @param serverName the serverName
   * @param serverPort the port on which to make the server accessible
   * @param serverAdminPort the port on which to make the server admin interface accessible
   * @param registryPort the registry port to use
   * @param database the Database implementation
   * @param sslEnabled if true then ssl is enabled
   * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
   * @param domainModelClassNames the domain model classes to load on startup
   * @param loginProxyClassNames the login proxy classes to initialize on startup
   * @param connectionValidatorClassNames the connection validation classes to initialize on startup
   * @param initialPoolUsers the users for which to initialize connection pools on startup
   * @param webDocumentRoot the web root from which to server files, if any
   * @param webServerPort the web server port, if any
   * @param clientLoggingEnabled if true then client logging is enabled on startup
   * @param connectionTimeout the idle connection timeout
   * @param clientSpecificConnectionTimeouts client specific connection timeouts, mapped to clientTypeID
   * @param adminUser the admin user
   * @throws RemoteException in case of a remote exception
   * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
   * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
   */
  public DefaultEntityConnectionServer(final String serverName, final int serverPort, final int serverAdminPort,
                                       final int registryPort, final Database database, final boolean sslEnabled,
                                       final int connectionLimit, final Collection<String> domainModelClassNames,
                                       final Collection<String> loginProxyClassNames, final Collection<String> connectionValidatorClassNames,
                                       final Collection<User> initialPoolUsers, final String webDocumentRoot,
                                       final Integer webServerPort, final boolean clientLoggingEnabled,
                                       final int connectionTimeout, final Map<String, Integer> clientSpecificConnectionTimeouts,
                                       final User adminUser)
          throws RemoteException {
    super(serverName, serverPort, serverAdminPort, registryPort, database, sslEnabled, connectionLimit, domainModelClassNames,
            loginProxyClassNames, connectionValidatorClassNames, initialPoolUsers, webDocumentRoot, webServerPort, clientLoggingEnabled,
            connectionTimeout, clientSpecificConnectionTimeouts, adminUser);
  }

  /** {@inheritDoc} */
  @Override
  protected DefaultRemoteEntityConnection createRemoteConnection(final ConnectionPool connectionPool, final Database database,
                                                                 final ClientInfo clientInfo, final int port,
                                                                 final boolean clientLoggingEnabled, final boolean sslEnabled)
          throws RemoteException, DatabaseException {
    return new DefaultRemoteEntityConnection(connectionPool, database, clientInfo, port, clientLoggingEnabled, sslEnabled);
  }

  /**
   * Starts the server
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized DefaultEntityConnectionServer startServer() throws RemoteException {
    final Integer serverPort = (Integer) Configuration.getValue(Configuration.SERVER_PORT);
    if (serverPort == null) {
      throw new IllegalArgumentException("Configuration property '" + Configuration.SERVER_PORT + "' is required");
    }
    final Integer registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    final Integer serverAdminPort = Configuration.getIntValue(Configuration.SERVER_ADMIN_PORT);
    final boolean sslEnabled = Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_SSL_ENABLED);
    final Integer connectionLimit = Configuration.getIntValue(Configuration.SERVER_CONNECTION_LIMIT);
    final Database database = Databases.createInstance();
    final String serverName = initializeServerName(database.getHost(), database.getSid());

    final Collection<String> domainModelClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    final Collection<String> loginProxyClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    final Collection<String> connectionValidationClassNames = Configuration.parseCommaSeparatedValues(Configuration.SERVER_CONNECTION_VALIDATOR_CLASSES);
    final Collection<String> initialPoolUsers = Configuration.parseCommaSeparatedValues(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    final String webDocumentRoot = Configuration.getStringValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    final Integer webServerPort = Configuration.getIntValue(Configuration.WEB_SERVER_PORT);
    final boolean clientLoggingEnabled = Configuration.getBooleanValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED);
    final Integer connectionTimeout = Configuration.getIntValue(Configuration.SERVER_CONNECTION_TIMEOUT);
    final Map<String, Integer> clientTimeouts = getClientTimeoutValues();
    final String adminUserString = Configuration.getStringValue(Configuration.SERVER_ADMIN_USER);
    final User adminUser = Util.nullOrEmpty(adminUserString) ? null : User.parseUser(adminUserString);
    if (adminUser == null) {
      LOG.info("No admin user specified");
    }
    else {
      LOG.info("Admin user: " + adminUser);
    }
    DefaultEntityConnectionServer server = null;
    try {
      server = new DefaultEntityConnectionServer(serverName, serverPort, serverAdminPort, registryPort, database,
              sslEnabled, connectionLimit, domainModelClassNames, loginProxyClassNames, connectionValidationClassNames,
              getPoolUsers(initialPoolUsers), webDocumentRoot, webServerPort, clientLoggingEnabled, connectionTimeout,
              clientTimeouts, adminUser);
      server.bindToRegistry();

      return server;
    }
    catch (final Exception e) {
      LOG.error("Exception when starting server", e);
      if (server != null) {
        server.shutdown();
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Connects to the server and shuts it down
   */
  static synchronized void shutdownServer() throws ServerException.AuthenticationException {
    final int registryPort = Configuration.getIntValue(Configuration.REGISTRY_PORT);
    final String sid = System.getProperty(Database.DATABASE_SID);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String serverName = initializeServerName(host, sid);
    final String adminUserString = Configuration.getStringValue(Configuration.SERVER_ADMIN_USER);
    if (Util.nullOrEmpty(adminUserString)) {
      throw ServerException.authenticationException("No admin user specified");
    }
    final User adminUser = User.parseUser(adminUserString);
    ServerUtil.resolveTrustStoreFromClasspath(DefaultEntityConnectionServerAdmin.class.getSimpleName());
    try {
      final Registry registry = ServerUtil.getRegistry(registryPort);
      final Server server = (Server) registry.lookup(serverName);
      final EntityConnectionServerAdmin serverAdmin = (EntityConnectionServerAdmin) server.getServerAdmin(adminUser);
      final String shutDownInfo = serverName + " found in registry on port: " + registryPort + ", shutting down";
      LOG.info(shutDownInfo);
      System.out.println(shutDownInfo);
      serverAdmin.shutdown();
    }
    catch (final RemoteException e) {
      System.out.println("Unable to shutdown server: " + e.getMessage());
      LOG.error("Error on shutdown", e);
    }
    catch (final NotBoundException e) {
      System.out.println(serverName + " not bound to registry on port: " + registryPort);
    }
    catch (final ServerException.AuthenticationException e) {
      LOG.error("Admin user info not provided or incorrect", e);
      throw e;
    }
  }

  /**
   * If no arguments are supplied a new DefaultEntityConnectionServer is started.
   * @param arguments 'start' (or no argument) starts the server, 'stop' or 'shutdown' causes a running server to be shut down and 'restart' restarts the server
   * @throws RemoteException in case of a remote exception during service export
   * @throws ClassNotFoundException in case the domain model classes required for the server is not found or
   * if the jdbc driver class is not found
   * @throws DatabaseException in case of an exception while constructing the initial pooled connections
   * @throws ServerException.AuthenticationException in case of missing or incorrect admin user information
   */
  public static void main(final String[] arguments) throws RemoteException, ClassNotFoundException, DatabaseException,
          ServerException.AuthenticationException {
    final String argument = arguments.length == 0 ? START : arguments[0];
    switch (argument) {
      case START:
        startServer();
        break;
      case STOP:
      case SHUTDOWN:
        shutdownServer();
        break;
      case RESTART:
        shutdownServer();
        startServer();
        break;
      default:
        throw new IllegalArgumentException("Unknown argument '" + argument + "'");
    }
  }
}
/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.report.Report;
import is.codion.common.event.EventListener;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.AbstractServer;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.server.EntityServerAdmin.DomainEntityDefinition;
import is.codion.framework.server.EntityServerAdmin.DomainOperation;
import is.codion.framework.server.EntityServerAdmin.DomainReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A remote server class, responsible for handling requests for AbstractRemoteEntityConnections.
 */
public class EntityServer extends AbstractServer<AbstractRemoteEntityConnection, EntityServerAdmin> {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(EntityServer.class);

  protected static final String START = "start";
  protected static final String STOP = "stop";
  protected static final String SHUTDOWN = "shutdown";
  protected static final String RESTART = "restart";

  private final EntityServerConfiguration configuration;
  private final Map<DomainType, Domain> domainModels;
  private final Database database;
  private final boolean clientLoggingEnabled;
  private final Map<String, Integer> clientTypeIdleConnectionTimeouts = new HashMap<>();

  private int idleConnectionTimeout;

  /**
   * Constructs a new EntityServer and binds it to a registry on the port found in the configuration.
   * @param configuration the server configuration
   * @throws RemoteException in case of a remote exception
   * @throws RuntimeException in case the domain model classes are not found on the classpath or if the
   * jdbc driver class is not found or in case of an exception while constructing the initial pooled connections
   */
  public EntityServer(EntityServerConfiguration configuration) throws RemoteException {
    super(configuration);
    addShutdownListener(new ShutdownListener());
    this.configuration = configuration;
    try {
      this.database = requireNonNull(configuration.database(), "database");
      this.clientLoggingEnabled = configuration.isClientLoggingEnabled();
      this.domainModels = loadDomainModels(configuration.domainClassNames());
      setAdmin(createServerAdmin(configuration));
      setIdleConnectionTimeout(configuration.idleConnectionTimeout());
      setClientTypeIdleConnectionTimeouts(configuration.clientTypeIdleConnectionTimeouts());
      createConnectionPools(configuration.database(), configuration.connectionPoolProvider(), configuration.connectionPoolUsers());
      setConnectionLimit(configuration.connectionLimit());
      bindToRegistry(configuration.registryPort());
    }
    catch (Throwable t) {
      throw logShutdownAndReturn(new RuntimeException(t));
    }
  }

  /**
   * @param user the server admin user
   * @return the administration interface for this server
   * @throws ServerAuthenticationException in case authentication fails
   * @throws IllegalStateException in case no server admin instance is available
   */
  @Override
  public final EntityServerAdmin serverAdmin(User user) throws ServerAuthenticationException {
    validateUserCredentials(user, configuration.adminUser());

    return getAdmin();
  }

  @Override
  public final int serverLoad() {
    return AbstractRemoteEntityConnection.requestsPerSecond();
  }

  @Override
  protected final AbstractRemoteEntityConnection connect(RemoteClient remoteClient) throws RemoteException, LoginException {
    requireNonNull(remoteClient, "remoteClient");
    try {
      AbstractRemoteEntityConnection connection = createRemoteConnection(database(), remoteClient,
              configuration.serverPort(), configuration.rmiClientSocketFactory(), configuration.rmiServerSocketFactory());
      connection.setLoggingEnabled(clientLoggingEnabled);

      connection.addDisconnectListener(this::disconnectQuietly);
      LOG.debug("{} connected", remoteClient);

      return connection;
    }
    catch (AuthenticationException e) {
      throw new ServerAuthenticationException(e.getMessage());
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.debug(remoteClient + " unable to connect", e);
      throw new LoginException(e.getMessage());
    }
  }

  @Override
  protected final void disconnect(AbstractRemoteEntityConnection connection) throws RemoteException {
    connection.close();
  }

  /**
   * Creates the remote connection provided by this server
   * @param database the underlying database
   * @param remoteClient the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param clientSocketFactory the client socket factory, null for default
   * @param serverSocketFactory the server socket factory, null for default
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   * @return a remote connection
   */
  protected AbstractRemoteEntityConnection createRemoteConnection(Database database,
                                                                  RemoteClient remoteClient, int port,
                                                                  RMIClientSocketFactory clientSocketFactory,
                                                                  RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultRemoteEntityConnection(clientDomainModel(remoteClient), database, remoteClient, port,
            clientSocketFactory, serverSocketFactory);
  }

  /**
   * @return the underlying Database implementation class
   */
  final Database database() {
    return database;
  }

  /**
   * @return the idle connection timeout in milliseconds
   */
  final int getIdleConnectionTimeout() {
    return idleConnectionTimeout;
  }

  /**
   * @param idleConnectionTimeout the new idle connection timeout value in milliseconds
   * @throws IllegalArgumentException in case timeout is less than zero
   */
  final void setIdleConnectionTimeout(int idleConnectionTimeout) {
    if (idleConnectionTimeout < 0) {
      throw new IllegalArgumentException("Idle connection timeout must be a positive integer");
    }
    this.idleConnectionTimeout = idleConnectionTimeout;
  }

  /**
   * @param clientTypeIdleConnectionTimeouts the idle connection timeout values mapped to each clientTypeId
   */
  final void setClientTypeIdleConnectionTimeouts(Map<String, Integer> clientTypeIdleConnectionTimeouts) {
    this.clientTypeIdleConnectionTimeouts.putAll(clientTypeIdleConnectionTimeouts);
  }

  /**
   * Returns the statistics gathered via {@link Database#countQuery(String)}.
   * @return a {@link Database.Statistics} object containing query statistics collected since
   * the last time this function was called.
   */
  final Database.Statistics databaseStatistics() {
    return database.statistics();
  }

  @Override
  protected final void maintainConnections(Collection<ClientConnection<AbstractRemoteEntityConnection>> connections) throws RemoteException {
    for (ClientConnection<AbstractRemoteEntityConnection> client : connections) {
      AbstractRemoteEntityConnection connection = client.connection();
      if (!connection.isActive()) {
        boolean connected = connection.isConnected();
        boolean timedOut = hasConnectionTimedOut(connection);
        if (!connected || timedOut) {
          LOG.debug("Removing connection {}, connected: {}, timeout: {}", client, connected, timedOut);
          disconnect(client.remoteClient().clientId());
        }
      }
    }
  }

  @Override
  protected final Collection<RemoteClient> clients(String clientTypeId) {
    //using the remoteClient from the connection since it contains the correct database user
    return connections().values().stream()
            .map(AbstractRemoteEntityConnection::remoteClient)
            .filter(remoteClient -> remoteClient.clientTypeId().equals(clientTypeId))
            .collect(toList());
  }

  final Map<DomainType, Collection<DomainEntityDefinition>> domainEntityDefinitions() {
    Map<DomainType, Collection<DomainEntityDefinition>> domainEntities = new HashMap<>();
    for (Domain domain : domainModels.values()) {
      domainEntities.put(domain.type(), domain.entities().definitions().stream()
              .map(definition -> new DefaultDomainEntityDefinition(definition.type().name(), definition.tableName()))
              .collect(Collectors.toList()));
    }

    return domainEntities;
  }

  final Map<DomainType, Collection<DomainReport>> domainReports() {
    Map<DomainType, Collection<DomainReport>> domainReports = new HashMap<>();
    for (Domain domain : domainModels.values()) {
      domainReports.put(domain.type(), domain.reports().entrySet().stream()
              .map(entry -> new DefaultDomainReport(entry.getKey().name(), entry.getValue().toString(), entry.getValue().isCached()))
              .collect(toList()));
    }

    return domainReports;
  }

  final Map<DomainType, Collection<DomainOperation>> domainOperations() {
    Map<DomainType, Collection<DomainOperation>> domainOperations = new HashMap<>();
    for (Domain domain : domainModels.values()) {
      Collection<DomainOperation> operations = new ArrayList<>();
      operations.addAll(domain.procedures().entrySet().stream()
              .map(entry -> new DefaultDomainOperation("Procedure", entry.getKey().name(), entry.getValue().getClass().getName()))
              .collect(toList()));
      operations.addAll(domain.functions().entrySet().stream()
              .map(entry -> new DefaultDomainOperation("Function", entry.getKey().name(), entry.getValue().getClass().getName()))
              .collect(toList()));
      domainOperations.put(domain.type(), operations);
    }

    return domainOperations;
  }

  /**
   * Clears all cached reports, triggering a reload on next usage.
   */
  final void clearReportCache() {
    for (Domain domain : domainModels.values()) {
      domain.reports().values().forEach(Report::clearCache);
    }
  }

  /**
   * Returns the client log for the connection identified by the given key.
   * @param clientId the UUID identifying the client
   * @return the client log for the given connection
   */
  final ClientLog clientLog(UUID clientId) {
    return connection(clientId).clientLog();
  }

  /**
   * @param clientId the client id
   * @return true if logging is enabled for the given client
   */
  final boolean isLoggingEnabled(UUID clientId) {
    return connection(clientId).isLoggingEnabled();
  }

  /**
   * @param clientId the client id
   * @param loggingEnabled the new logging status
   */
  final void setLoggingEnabled(UUID clientId, boolean loggingEnabled) {
    connection(clientId).setLoggingEnabled(loggingEnabled);
  }

  /**
   * @param timedOutOnly if true only connections that have timed out are culled
   * @throws RemoteException in case of an exception
   * @see #hasConnectionTimedOut(AbstractRemoteEntityConnection)
   */
  final void disconnectClients(boolean timedOutOnly) throws RemoteException {
    List<RemoteClient> clients = new ArrayList<>(connections().keySet());
    for (RemoteClient client : clients) {
      AbstractRemoteEntityConnection connection = connection(client.clientId());
      if (timedOutOnly) {
        boolean active = connection.isActive();
        if (!active && hasConnectionTimedOut(connection)) {
          disconnect(client.clientId());
        }
      }
      else {
        disconnect(client.clientId());
      }
    }
  }

  private void disconnectQuietly(AbstractRemoteEntityConnection connection) {
    try {
      disconnect(connection.remoteClient().clientId());
    }
    catch (RemoteException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  /**
   * Creates a {@link EntityServerAdmin} instance if the server admin port is specified.
   * @param configuration the server configuration
   * @return a admin instance
   * @throws RemoteException in case of an exception
   */
  private EntityServerAdmin createServerAdmin(EntityServerConfiguration configuration) throws RemoteException {
    if (configuration.serverAdminPort() != 0) {
      return new DefaultEntityServerAdmin(this, configuration);
    }

    return null;
  }

  /**
   * Binds this server instance to the registry
   * @throws RemoteException in case of an exception
   * @param registryPort the registry port
   */
  private void bindToRegistry(int registryPort) throws RemoteException {
    registry().rebind(serverInformation().serverName(), this);
    String connectInfo = serverInformation().serverName() + " bound to registry on port: " + registryPort;
    LOG.info(connectInfo);
    System.out.println(connectInfo);
  }

  private boolean hasConnectionTimedOut(AbstractRemoteEntityConnection connection) {
    Integer timeout = clientTypeIdleConnectionTimeouts.get(connection.remoteClient().clientTypeId());
    if (timeout == null) {
      timeout = idleConnectionTimeout;
    }

    return connection.hasBeenInactive(timeout);
  }

  private Domain clientDomainModel(RemoteClient remoteClient) {
    String domainTypeName = (String) remoteClient.parameters().get(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE);
    if (domainTypeName == null) {
      throw new IllegalArgumentException("'" + RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE + "' parameter not specified");
    }

    return domainModels.get(DomainType.getDomainType(domainTypeName));
  }

  private static Map<DomainType, Domain> loadDomainModels(Collection<String> domainModelClassNames) throws Throwable {
    Map<DomainType, Domain> domains = new HashMap<>();
    List<Domain> serviceDomains = Domain.domains();
    try {
      serviceDomains.forEach(domain -> {
        LOG.info("Server loading and registering domain model '" + domain.type() + "' as a service");
        domains.put(domain.type(), domain);
      });
      for (String className : domainModelClassNames) {
        LOG.info("Server loading and registering domain model class '" + className + "' from classpath");
        Domain domain = (Domain) Class.forName(className).getConstructor().newInstance();
        domains.put(domain.type(), domain);
      }

      return unmodifiableMap(domains);
    }
    catch (InvocationTargetException ite) {
      LOG.error("Exception while loading and registering domain model", ite);
      throw ite.getCause();
    }
    catch (Exception e) {
      LOG.error("Exception while loading and registering domain model", e);
      throw e;
    }
  }

  private static void createConnectionPools(Database database, String connectionPoolFactoryClassName,
                                            Collection<User> connectionPoolUsers) throws DatabaseException {
    if (!connectionPoolUsers.isEmpty()) {
      ConnectionPoolFactory poolFactory;
      if (nullOrEmpty(connectionPoolFactoryClassName)) {
        poolFactory = ConnectionPoolFactory.instance();
      }
      else {
        poolFactory = ConnectionPoolFactory.instance(connectionPoolFactoryClassName);
      }
      for (User user : connectionPoolUsers) {
        database.createConnectionPool(poolFactory, user);
      }
    }
  }

  /**
   * Starts the server, using the configuration from system properties.
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static EntityServer startServer() throws RemoteException {
    return startServer(EntityServerConfiguration.builderFromSystemProperties().build());
  }

  /**
   * Starts the server.
   * @param configuration the configuration
   * @return the server instance
   * @throws RemoteException in case of an exception
   */
  public static synchronized EntityServer startServer(EntityServerConfiguration configuration) throws RemoteException {
    requireNonNull(configuration, "configuration");
    try {
      return new EntityServer(configuration);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error("Exception when starting server", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Connects to the server and shuts it down
   */
  static synchronized void shutdownServer() throws ServerAuthenticationException {
    EntityServerConfiguration configuration = EntityServerConfiguration.builderFromSystemProperties().build();
    String serverName = configuration.serverName();
    int registryPort = configuration.registryPort();
    User adminUser = configuration.adminUser();
    if (adminUser == null) {
      throw new ServerAuthenticationException("No admin user specified");
    }
    Clients.resolveTrustStore();
    try {
      Registry registry = LocateRegistry.getRegistry(registryPort);
      Server<?, EntityServerAdmin> server = (Server<?, EntityServerAdmin>) registry.lookup(serverName);
      EntityServerAdmin serverAdmin = server.serverAdmin(adminUser);
      String shutDownInfo = serverName + " found in registry on port: " + registryPort + ", shutting down";
      LOG.info(shutDownInfo);
      System.out.println(shutDownInfo);
      serverAdmin.shutdown();
    }
    catch (RemoteException e) {
      System.out.println("Unable to shutdown server: " + e.getMessage());
      LOG.error("Error on shutdown", e);
    }
    catch (NotBoundException e) {
      System.out.println(serverName + " not bound to registry on port: " + registryPort);
    }
    catch (ServerAuthenticationException e) {
      LOG.error("Admin user info not provided or incorrect", e);
      throw e;
    }
  }

  /**
   * If no arguments are supplied a new EntityServer is started.
   * @param arguments 'start' (or no argument) starts the server, 'stop' or 'shutdown' causes a running server to be shut down and 'restart' restarts the server
   * @throws RemoteException in case of a remote exception during service export
   * @throws ServerAuthenticationException in case of missing or incorrect admin user information
   */
  public static void main(String[] arguments) throws RemoteException, ServerAuthenticationException {
    String argument = arguments.length == 0 ? START : arguments[0];
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
        startServer();
    }
  }

  private final class ShutdownListener implements EventListener {

    @Override
    public void onEvent() {
      database.closeConnectionPools();
      database.shutdownEmbedded();
    }
  }

  private static final class DefaultDomainEntityDefinition implements DomainEntityDefinition, Serializable {

    private static final long serialVersionUID = 1;

    private final String name;
    private final String tableName;

    private DefaultDomainEntityDefinition(String name, String tableName) {
      this.name = name;
      this.tableName = tableName;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String tableName() {
      return tableName;
    }
  }

  private static final class DefaultDomainReport implements DomainReport, Serializable {

    private static final long serialVersionUID = 1;

    private final String name;
    private final String description;
    private final boolean cached;

    private DefaultDomainReport(String name, String description, boolean cached) {
      this.name = name;
      this.description = description;
      this.cached = cached;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String description() {
      return description;
    }

    @Override
    public boolean isCached() {
      return cached;
    }
  }

  private static final class DefaultDomainOperation implements DomainOperation, Serializable {

    private static final long serialVersionUID = 1;

    private final String type;
    private final String name;
    private final String className;

    private DefaultDomainOperation(String type, String name, String className) {
      this.type = type;
      this.name = name;
      this.className = className;
    }

    @Override
    public String type() {
      return type;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String className() {
      return className;
    }
  }
}
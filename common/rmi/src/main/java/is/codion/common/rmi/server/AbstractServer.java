/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.ConnectionValidationException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static is.codion.common.rmi.server.SerializationWhitelist.isSerializationDryRunActive;
import static is.codion.common.rmi.server.SerializationWhitelist.writeDryRunWhitelist;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A default Server implementation.
 * @param <T> the type of remote interface served by this server
 * @param <A> the type of the admin interface this server provides
 */
public abstract class AbstractServer<T extends Remote, A extends ServerAdmin> extends UnicastRemoteObject implements Server<T, A> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractServer.class);

  private static final boolean OBJECT_INPUT_FILTER_ON_CLASSPATH = onClasspath("sun.misc.ObjectInputFilter");
  private static final String CLIENT_ID = "clientId";

  private final Map<UUID, ClientConnection<T>> connections = new ConcurrentHashMap<>();
  private final Map<String, LoginProxy> loginProxies = new HashMap<>();
  private final Collection<LoginProxy> sharedLoginProxies = new ArrayList<>();
  private final Collection<AuxiliaryServer> auxiliaryServers = new ArrayList<>();
  private final TaskScheduler connectionMaintenanceScheduler;

  private final ServerConfiguration configuration;
  private final ServerInformation serverInformation;
  private final Event<?> shutdownEvent = Event.event();
  private volatile int connectionLimit = -1;
  private volatile boolean shuttingDown = false;

  private Registry registry;
  private A admin;

  /**
   * Instantiates a new AbstractServer
   * @param configuration the configuration
   * @throws RemoteException in case of an exception
   */
  protected AbstractServer(ServerConfiguration configuration) throws RemoteException {
    super(requireNonNull(configuration, "configuration").serverPort(),
            configuration.rmiClientSocketFactory(), configuration.rmiServerSocketFactory());
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    try {
      this.configuration = configuration;
      this.serverInformation = new DefaultServerInformation(UUID.randomUUID(), configuration.serverName(),
              configuration.serverPort(), ZonedDateTime.now());
      this.connectionMaintenanceScheduler = TaskScheduler.builder(new MaintenanceTask())
              .interval(configuration.connectionMaintenanceInterval())
              .initialDelay(configuration.connectionMaintenanceInterval())
              .timeUnit(TimeUnit.MILLISECONDS)
              .start();
      configureSerializationWhitelist(configuration);
      startAuxiliaryServers(configuration.auxiliaryServerFactoryClassNames());
      loadLoginProxies();
    }
    catch (Throwable exception) {
      throw logShutdownAndReturn(new RuntimeException(exception));
    }
  }

  /**
   * @return a map containing the current connections
   */
  public final Map<RemoteClient, T> connections() {
    Map<RemoteClient, T> clients = new HashMap<>();
    for (ClientConnection<T> clientConnection : connections.values()) {
      clients.put(clientConnection.remoteClient(), clientConnection.connection());
    }

    return clients;
  }

  /**
   * @param clientId the client id
   * @return the connection associated with the given client
   * @throws IllegalArgumentException in case no such client is connected
   */
  public final T connection(UUID clientId) {
    ClientConnection<T> clientConnection = connections.get(requireNonNull(clientId, CLIENT_ID));
    if (clientConnection != null) {
      return clientConnection.connection();
    }

    throw new IllegalArgumentException("Client not connected: " + clientId);
  }

  /**
   * @return the current number of connections
   */
  public final int connectionCount() {
    return connections.size();
  }

  /**
   * @return the maximum number of concurrent connections accepted by this server,
   * a negative number means no limit while 0 means the server is closed.
   */
  public final int getConnectionLimit() {
    return connectionLimit;
  }

  /**
   * @param connectionLimit the maximum number of concurrent connections accepted by this server,
   * a negative number means no limit while 0 means the server is closed.
   */
  public final void setConnectionLimit(int connectionLimit) {
    this.connectionLimit = connectionLimit;
  }

  /**
   * @return the maintenance check interval in ms
   */
  public final int getMaintenanceInterval() {
    return connectionMaintenanceScheduler.getInterval();
  }

  /**
   * @param maintenanceInterval the new maintenance interval in ms
   */
  public final void setMaintenanceInterval(int maintenanceInterval) {
    connectionMaintenanceScheduler.setInterval(maintenanceInterval);
  }

  @Override
  public final ServerInformation serverInformation() {
    return serverInformation;
  }

  @Override
  public final boolean connectionsAvailable() {
    return !maximumNumberOfConnectionsReached();
  }

  @Override
  public final T connect(ConnectionRequest connectionRequest) throws RemoteException, ConnectionNotAvailableException,
          LoginException, ConnectionValidationException {
    if (shuttingDown) {
      throw new LoginException("Server is shutting down");
    }
    requireNonNull(connectionRequest, "connectionRequest");
    requireNonNull(connectionRequest.user(), "user");
    requireNonNull(connectionRequest.clientId(), CLIENT_ID);
    requireNonNull(connectionRequest.clientTypeId(), "clientTypeId");
    synchronized (connections) {
      ClientConnection<T> clientConnection = connections.get(connectionRequest.clientId());
      if (clientConnection != null) {
        validateUserCredentials(connectionRequest.user(), clientConnection.remoteClient().user());
        LOG.trace("Active connection exists {}", connectionRequest);

        return clientConnection.connection();
      }

      if (maximumNumberOfConnectionsReached()) {
        LOG.debug("Maximum number of connections reached {}", connectionLimit);
        throw new ConnectionNotAvailableException();
      }
      LOG.trace("No active connection found for client {}, establishing a new connection", connectionRequest);

      return createConnection(connectionRequest).connection();
    }
  }

  @Override
  public final void disconnect(UUID clientId) throws RemoteException {
    ClientConnection<T> clientConnection = connections.remove(requireNonNull(clientId, CLIENT_ID));
    if (clientConnection != null) {
      disconnect(clientConnection.connection());
      RemoteClient remoteClient = clientConnection.remoteClient();
      for (LoginProxy loginProxy : sharedLoginProxies) {
        loginProxy.logout(remoteClient);
      }
      LoginProxy loginProxy = loginProxies.get(remoteClient.clientTypeId());
      if (loginProxy != null) {
        loginProxy.logout(remoteClient);
      }
      LOG.debug("Client disconnected {}", remoteClient);
    }
  }

  /**
   * Shuts down this server.
   */
  public final void shutdown() {
    if (shuttingDown) {
      return;
    }
    shuttingDown = true;
    connectionMaintenanceScheduler.stop();
    if (registry != null) {
      unexportSilently(registry);
    }
    unexportSilently(this);
    if (admin != null) {
      unexportSilently(admin);
    }
    for (UUID clientId : new ArrayList<>(connections.keySet())) {
      try {
        disconnect(clientId);
      }
      catch (RemoteException e) {
        LOG.debug("Error while disconnecting a client on shutdown: " + clientId, e);
      }
    }
    sharedLoginProxies.forEach(AbstractServer::closeLoginProxy);
    loginProxies.values().forEach(AbstractServer::closeLoginProxy);
    auxiliaryServers.forEach(AbstractServer::stopAuxiliaryServer);
    if (OBJECT_INPUT_FILTER_ON_CLASSPATH && isSerializationDryRunActive()) {
      writeDryRunWhitelist();
    }
    shutdownEvent.onEvent();
  }

  public final void addLoginProxy(LoginProxy loginProxy) {
    requireNonNull(loginProxy, "loginProxy");
    if (loginProxy.clientTypeId() == null) {
      sharedLoginProxies.add(loginProxy);
    }
    else {
      loginProxies.put(loginProxy.clientTypeId(), loginProxy);
    }
  }

  /**
   * @return info on all connected users
   */
  final Collection<User> users() {
    return connections().keySet().stream()
            .map(ConnectionRequest::user)
            .collect(toSet());
  }

  /**
   * @return info on all connected clients
   */
  final Collection<RemoteClient> clients() {
    return new ArrayList<>(connections().keySet());
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   */
  final Collection<RemoteClient> clients(User user) {
    requireNonNull(user);

    return connections().keySet().stream()
            .filter(remoteClient -> user == null || remoteClient.user().equals(user))
            .collect(toList());
  }

  protected final void setAdmin(A admin) {
    if (this.admin != null) {
      throw new IllegalStateException("Admin has already been set for this server");
    }
    this.admin = admin;
  }

  protected final A getAdmin() {
    if (admin == null) {
      throw new IllegalStateException("No admin instance available");
    }

    return admin;
  }

  /**
   * @param listener a listener notified when this server is shutting down.
   */
  protected final void addShutdownListener(EventListener listener) {
    shutdownEvent.addListener(requireNonNull(listener, "listener"));
  }

  /**
   * Establishes the actual client connection.
   * @param remoteClient the client connection info
   * @return a connection servicing the given client
   * @throws RemoteException in case of an exception
   * @throws LoginException in case of an error during the login
   */
  protected abstract T connect(RemoteClient remoteClient) throws RemoteException, LoginException;

  /**
   * Disconnects the given connection.
   * @param connection the connection to disconnect
   * @throws RemoteException in case of an exception
   */
  protected abstract void disconnect(T connection) throws RemoteException;

  /**
   * Maintains the given connections, that is, disconnects inactive or invalid connections, if required.
   * @throws RemoteException in case of an exception
   * @param connections all current connections
   */
  protected abstract void maintainConnections(Collection<ClientConnection<T>> connections) throws RemoteException;

  /**
   * @param clientTypeId the client type id
   * @return all clients of the given type
   */
  protected Collection<RemoteClient> clients(String clientTypeId) {
    return connections().keySet().stream()
            .filter(client -> Objects.equals(client.clientTypeId(), clientTypeId))
            .collect(toList());
  }

  protected final Registry registry() throws RemoteException {
    if (registry == null) {
      this.registry = LocateRegistry.createRegistry(configuration.registryPort());
    }

    return this.registry;
  }

  protected final <T extends Throwable> T logShutdownAndReturn(T exception) {
    LOG.error("Exception on server startup", exception);
    shutdown();

    return exception;
  }

  /**
   * Validates the given user credentials
   * @param userToCheck the credentials to check
   * @param requiredUser the required credentials
   * @throws ServerAuthenticationException in case either User instance is null or if the username or password does not match
   */
  protected static final void validateUserCredentials(User userToCheck, User requiredUser) throws ServerAuthenticationException {
    if (userToCheck == null || requiredUser == null
            || !Objects.equals(userToCheck.username(), requiredUser.username())
            || !Arrays.equals(userToCheck.getPassword(), requiredUser.getPassword())) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }

  private boolean maximumNumberOfConnectionsReached() {
    return connectionLimit > -1 && connectionCount() >= connectionLimit;
  }

  private ClientConnection<T> createConnection(ConnectionRequest connectionRequest) throws LoginException, RemoteException {
    RemoteClient remoteClient = RemoteClient.remoteClient(connectionRequest);
    setClientHost(remoteClient, (String) connectionRequest.parameters().get(CLIENT_HOST_KEY));
    for (LoginProxy loginProxy : sharedLoginProxies) {
      remoteClient = loginProxy.login(remoteClient);
    }
    LoginProxy clientLoginProxy = loginProxies.get(connectionRequest.clientTypeId());
    LOG.debug("Connecting client {}, loginProxy {}", connectionRequest, clientLoginProxy);
    if (clientLoginProxy != null) {
      remoteClient = clientLoginProxy.login(remoteClient);
    }
    ClientConnection<T> clientConnection = new ClientConnection<>(remoteClient, connect(remoteClient));
    connections.put(remoteClient.clientId(), clientConnection);

    return clientConnection;
  }

  private void startAuxiliaryServers(Collection<String> auxiliaryServerFactoryClassNames) {
    try {
      for (String auxiliaryServerFactoryClassName : auxiliaryServerFactoryClassNames) {
        AuxiliaryServerFactory<T, A, ?> auxiliaryServerFactory = AuxiliaryServerFactory.instance(auxiliaryServerFactoryClassName);
        AuxiliaryServer auxiliaryServer = auxiliaryServerFactory.createServer(this);
        auxiliaryServers.add(auxiliaryServer);
        Callable<?> starter = () -> startAuxiliaryServer(auxiliaryServer);
        newSingleThreadScheduledExecutor(new DaemonThreadFactory()).submit(starter).get();
      }
    }
    catch (InterruptedException e) {
      LOG.error("Interrupted during auxiliary server startup", e);
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOG.error("Starting auxiliary server", e);
      throw new RuntimeException(e);
    }
  }

  private static void configureSerializationWhitelist(ServerConfiguration configuration) {
    if (OBJECT_INPUT_FILTER_ON_CLASSPATH) {
      if (configuration.isSerializationFilterDryRun()) {
        SerializationWhitelist.configureDryRun(configuration.serializationFilterWhitelist());
      }
      else {
        SerializationWhitelist.configure(configuration.serializationFilterWhitelist());
      }
    }
  }

  private static void closeLoginProxy(LoginProxy loginProxy) {
    try {
      loginProxy.close();
    }
    catch (Exception e) {
      LOG.error("Exception while closing loginProxy for client type: " + loginProxy.clientTypeId(), e);
    }
  }

  private static Object startAuxiliaryServer(AuxiliaryServer server) throws Exception {
    try {
      server.startServer();
      LOG.info("Auxiliary server started: " + server);

      return null;
    }
    catch (Exception e) {
      LOG.error("Starting auxiliary server", e);
      throw e;
    }
  }

  private static void stopAuxiliaryServer(AuxiliaryServer server) {
    try {
      server.stopServer();
      LOG.info("Auxiliary server stopped: " + server);
    }
    catch (Exception e) {
      LOG.error("Stopping auxiliary server", e);
    }
  }

  private static void unexportSilently(Remote object) {
    try {
      unexportObject(object, true);
    }
    catch (NoSuchObjectException e) {
      LOG.error("Exception while unexporting " + object + " on shutdown", e);
    }
  }

  private static void setClientHost(RemoteClient remoteClient, String requestParameterHost) {
    if (requestParameterHost == null) {
      try {
        remoteClient.setClientHost(getClientHost());
      }
      catch (ServerNotActiveException ignored) {/*ignored*/}
    }
    else {
      remoteClient.setClientHost(requestParameterHost);
    }
  }

  private static boolean onClasspath(String className) {
    try {
      Class.forName(className);
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  private void loadLoginProxies() {
    LoginProxy.loginProxies().forEach(loginProxy -> {
      String clientTypeId = loginProxy.clientTypeId();
      LOG.info("Server loading " + (clientTypeId == null ? "shared" : "") + "login proxy '" + loginProxy.getClass().getName() + "' as service");
      addLoginProxy(loginProxy);
    });
  }

  protected static final class ClientConnection<T> {

    private final RemoteClient client;
    private final T connection;

    private ClientConnection(RemoteClient client, T connection) {
      this.client = client;
      this.connection = connection;
    }

    public RemoteClient remoteClient() {
      return client;
    }

    public T connection() {
      return connection;
    }
  }

  private final class MaintenanceTask implements Runnable {

    @Override
    public void run() {
      try {
        if (connectionCount() > 0) {
          maintainConnections(unmodifiableCollection(connections.values()));
        }
      }
      catch (Exception e) {
        LOG.error("Exception while maintaining connections", e);
      }
    }
  }

  private static final class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }
}

/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.event.Event;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
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
import java.util.function.Predicate;

import static is.codion.common.rmi.server.RemoteClient.remoteClient;
import static is.codion.common.rmi.server.SerializationWhitelist.isSerializationDryRunActive;
import static is.codion.common.rmi.server.SerializationWhitelist.writeDryRunWhitelist;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.*;

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
    super(requireNonNull(configuration, "configuration").port(),
            configuration.rmiClientSocketFactory(), configuration.rmiServerSocketFactory());
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    try {
      this.configuration = configuration;
      this.serverInformation = new DefaultServerInformation(UUID.randomUUID(),
              configuration.serverName(), configuration.port(), ZonedDateTime.now());
      this.connectionMaintenanceScheduler = TaskScheduler.builder(new MaintenanceTask())
              .interval(configuration.connectionMaintenanceInterval(), TimeUnit.MILLISECONDS)
              .initialDelay(configuration.connectionMaintenanceInterval())
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
    return connections.values().stream().collect(toMap(ClientConnection::remoteClient, ClientConnection::connection));
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
  public final T connect(ConnectionRequest connectionRequest) throws RemoteException, ConnectionNotAvailableException, LoginException {
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
    unexportSilently(registry, this, admin);
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
    shutdownEvent.run();
  }

  /**
   * Adds a {@link LoginProxy} instance to this server.
   * If {@link LoginProxy#clientTypeId()} is empty, the login proxy
   * is shared between all clients, otherwise it is only used for clients with the given id.
   * @param loginProxy the login proxy to add
   * @throws IllegalStateException in case a login proxy with the same clientTypeId has been added
   */
  public final void addLoginProxy(LoginProxy loginProxy) {
    requireNonNull(loginProxy, "loginProxy");
    if (loginProxy.clientTypeId().isPresent()) {
      String clientTypeId = loginProxy.clientTypeId().get();
      if (loginProxies.containsKey(clientTypeId)) {
        throw new IllegalStateException("LoginProxy for clientTypeId '" + clientTypeId + "' has alread been added");
      }
      loginProxies.put(clientTypeId, loginProxy);
    }
    else {
      sharedLoginProxies.add(loginProxy);
    }
  }

  /**
   * @return info on all connected users
   */
  final Collection<User> users() {
    return connections().keySet().stream()
            .map(ConnectionRequest::user)
            .map(User::copy)
            .map(User::clearPassword)
            .collect(toSet());
  }

  /**
   * @return info on all connected clients
   */
  final Collection<RemoteClient> clients() {
    return clients(remoteClient -> true);
  }

  /**
   * @param user the user
   * @return all clients connected with the given user
   */
  final Collection<RemoteClient> clients(User user) {
    return clients(remoteClient -> remoteClient.user().equals(requireNonNull(user)));
  }

  /**
   * Sets the admin instance for this server
   * @param admin the admin instance
   * @throws IllegalStateException in case an admin instance has already been set
   */
  protected final void setAdmin(A admin) {
    if (this.admin != null) {
      throw new IllegalStateException("Admin has already been set for this server");
    }
    this.admin = admin;
  }

  /**
   * @return the admin instance associated with this server
   * @throws IllegalStateException in case no admin instance has been set
   * @see #setAdmin(ServerAdmin)
   */
  protected final A getAdmin() {
    if (admin == null) {
      throw new IllegalStateException("No admin instance available");
    }

    return admin;
  }

  /**
   * @param listener a listener notified when this server is shutting down.
   */
  protected final void addShutdownListener(Runnable listener) {
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
   * @param connections all current connections
   * @throws RemoteException in case of an exception
   */
  protected abstract void maintainConnections(Collection<ClientConnection<T>> connections) throws RemoteException;

  /**
   * @param clientTypeId the client type id
   * @return all clients of the given type
   */
  protected final Collection<RemoteClient> clients(String clientTypeId) {
    return clients(remoteClient -> Objects.equals(remoteClient.clientTypeId(), requireNonNull(clientTypeId)));
  }

  /**
   * @return the Registry instance
   * @throws RemoteException in case of an exception
   */
  protected final Registry registry() throws RemoteException {
    if (registry == null) {
      registry = LocateRegistry.createRegistry(configuration.registryPort());
    }

    return registry;
  }

  /**
   * Logs the given exception and shuts down this server
   * @param exception the exception
   * @return the exception
   * @param <T> the exception type
   */
  protected final <T extends Throwable> T logShutdownAndReturn(T exception) {
    LOG.error("Exception on server startup", exception);
    shutdown();

    return exception;
  }

  /**
   * @return an unmodifiable view of the auxialiary servers running along side this server
   */
  protected final Collection<AuxiliaryServer> auxiliaryServers() {
    return unmodifiableCollection(auxiliaryServers);
  }

  /**
   * Validates the given user credentials
   * @param userToCheck the credentials to check
   * @param requiredUser the required credentials
   * @throws ServerAuthenticationException in case either User instance is null or if the username or password does not match
   */
  protected static void validateUserCredentials(User userToCheck, User requiredUser) throws ServerAuthenticationException {
    if (userToCheck == null || requiredUser == null
            || !Objects.equals(userToCheck.username(), requiredUser.username())
            || !Arrays.equals(userToCheck.password(), requiredUser.password())) {
      throw new ServerAuthenticationException("Wrong username or password");
    }
  }

  private boolean maximumNumberOfConnectionsReached() {
    return connectionLimit > -1 && connectionCount() >= connectionLimit;
  }

  private ClientConnection<T> createConnection(ConnectionRequest connectionRequest) throws LoginException, RemoteException {
    RemoteClient remoteClient = remoteClient(connectionRequest,
            clientHost((String) connectionRequest.parameters().get(CLIENT_HOST)));
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
        newSingleThreadExecutor(new DaemonThreadFactory()).submit(starter).get();
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

  private Collection<RemoteClient> clients(Predicate<RemoteClient> predicate) {
    return connections().keySet().stream()
            .filter(predicate)
            .map(RemoteClient::copy)
            .map(AbstractServer::clearPasswords)
            .collect(toList());
  }

  private static RemoteClient clearPasswords(RemoteClient remoteClient) {
    remoteClient.user().clearPassword();
    remoteClient.databaseUser().clearPassword();

    return remoteClient;
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

  private static void unexportSilently(Remote... remotes) {
    for (Remote remote : remotes) {
      if (remote != null) {
        try {
          unexportObject(remote, true);
        }
        catch (NoSuchObjectException e) {
          LOG.error("Exception while unexporting " + remote + " on shutdown", e);
        }
      }
    }
  }

  private static String clientHost(String requestParameterHost) {
    if (requestParameterHost == null) {
      try {
        return getClientHost();
      }
      catch (ServerNotActiveException ignored) {/*ignored*/}
    }

    return requestParameterHost;
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
      String clientTypeId = loginProxy.clientTypeId().orElse(null);
      LOG.info("Server loading login proxy '" + loginProxy.getClass().getName() + "' as service, " +
              (clientTypeId == null ? "shared" : "(clientTypeId: '" + clientTypeId + "'"));
      addLoginProxy(loginProxy);
    });
  }

  /**
   * Represents a remote client connection.
   * @param <T> the connection type
   */
  protected static final class ClientConnection<T> {

    private final RemoteClient client;
    private final T connection;

    private ClientConnection(RemoteClient client, T connection) {
      this.client = client;
      this.connection = connection;
    }

    /**
     * @return the remote client
     */
    public RemoteClient remoteClient() {
      return client;
    }

    /**
     * @return the connection
     */
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

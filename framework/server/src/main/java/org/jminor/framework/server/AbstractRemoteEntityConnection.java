/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.MethodLogger;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.remote.ClientLog;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Servers;
import org.jminor.common.user.User;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A base class for remote connections served by a {@link DefaultEntityConnectionServer}.
 * Handles logging of service calls and database connection pooling.
 */
public abstract class AbstractRemoteEntityConnection extends UnicastRemoteObject {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteEntityConnection.class);

  /**
   * A Proxy for logging method calls
   */
  protected final transient EntityConnection connectionProxy;

  /**
   * The proxy connection handler
   */
  private final transient RemoteEntityConnectionHandler connectionHandler;

  /**
   * An event notified when this connection is disconnected
   */
  private final transient Event<AbstractRemoteEntityConnection> disconnectedEvent = Events.event();

  /**
   * Instantiates a new AbstractRemoteEntityConnection and exports it on the given port number
   * @param domain the domain model entities
   * @param connectionPool the connection pool to use, if none is provided a local connection is established
   * @param database defines the underlying database
   * @param remoteClient information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param clientSocketFactory the client socket factory to use, null for default
   * @param serverSocketFactory the server socket factory to use, null for default
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   */
  protected AbstractRemoteEntityConnection(final Domain domain, final ConnectionPool connectionPool, final Database database,
                                           final RemoteClient remoteClient, final int port,
                                           final RMIClientSocketFactory clientSocketFactory,
                                           final RMIServerSocketFactory serverSocketFactory)
          throws DatabaseException, RemoteException {
    super(port, clientSocketFactory, serverSocketFactory);
    this.connectionHandler = new RemoteEntityConnectionHandler(domain, remoteClient, connectionPool, database);
    this.connectionProxy = (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
            new Class[] {EntityConnection.class}, connectionHandler);
  }

  /**
   * @return the user this connection is using
   */
  public final User getUser() {
    return connectionHandler.remoteClient.getUser();
  }

  /**
   * @return true if this connection is connected
   */
  public final boolean isConnected() {
    synchronized (connectionProxy) {
      return connectionHandler.isConnected();
    }
  }

  /**
   * Disconnects this connection
   */
  public final void disconnect() {
    synchronized (connectionProxy) {
      if (connectionHandler.disconnected) {
        return;
      }
      try {
        UnicastRemoteObject.unexportObject(this, true);
      }
      catch (final NoSuchObjectException e) {
        LOG.error(e.getMessage(), e);
      }
      connectionHandler.disconnect();
    }
    disconnectedEvent.onEvent(this);
  }

  /**
   * @return the remote client using this remote connection
   */
  final RemoteClient getRemoteClient() {
    return connectionHandler.remoteClient;
  }

  /**
   * @return a ClientLog instance containing information about this connections recent activity
   */
  final ClientLog getClientLog() {
    return connectionHandler.getClientLog();
  }

  /**
   * @param timeout the number of milliseconds
   * @return true if this connection has been inactive for {@code timeout} milliseconds or longer
   */
  final boolean hasBeenInactive(final int timeout) {
    return System.currentTimeMillis() - connectionHandler.lastAccessTime > timeout;
  }

  final void setLoggingEnabled(final boolean status) {
    connectionHandler.methodLogger.setEnabled(status);
  }

  final boolean isLoggingEnabled() {
    return connectionHandler.methodLogger.isEnabled();
  }

  /**
   * @return true during a remote method call
   */
  final boolean isActive() {
    return connectionHandler.active.get();
  }

  final void addDisconnectListener(final EventDataListener<AbstractRemoteEntityConnection> listener) {
    disconnectedEvent.addDataListener(listener);
  }

  static int getRequestsPerSecond() {
    return RemoteEntityConnectionHandler.REQUEST_COUNTER.getRequestsPerSecond();
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {

    private static final String LOG_IDENTIFIER_PROPERTY = "logIdentifier";
    private static final String GET_CONNECTION = "getConnection";
    private static final String RETURN_CONNECTION = "returnConnection";

    private static final RequestCounter REQUEST_COUNTER = new RequestCounter();

    /**
     * The domain model
     */
    private final Domain domain;

    /**
     * Contains information about the client using this connection
     */
    private final RemoteClient remoteClient;

    /**
     * Contains information about the underlying database
     */
    private final Database database;

    /**
     * The connection pool to use, if any
     */
    private final ConnectionPool connectionPool;

    /**
     * The method call log
     */
    private final MethodLogger methodLogger;

    /**
     * Identifies the log file being used for this connection
     */
    private final String logIdentifier;

    /**
     * The date and time when this remote connection was established
     */
    private final long creationDate = System.currentTimeMillis();

    /**
     * True while working
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * A local connection used in case no connection pool is provided, managed by getConnection()/returnConnection()
     */
    private LocalEntityConnection localEntityConnection;

    /**
     * A local connection used in case of a connection pool, managed by getConnection()/returnConnection()
     */
    private LocalEntityConnection poolEntityConnection;

    /**
     * The time this connection was last used
     */
    private long lastAccessTime = creationDate;

    /**
     * Indicates whether or not this remote connection has been disconnected
     */
    private boolean disconnected = false;

    private RemoteEntityConnectionHandler(final Domain domain, final RemoteClient remoteClient,
                                          final ConnectionPool connectionPool, final Database database)
            throws DatabaseException {
      this.domain = domain;
      this.remoteClient = remoteClient;
      this.connectionPool = connectionPool;
      this.database = database;
      this.methodLogger = new MethodLogger(LocalEntityConnection.CONNECTION_LOG_SIZE.get(), new EntityArgumentToString(domain));
      this.logIdentifier = remoteClient.getUser().getUsername().toLowerCase() + "@" + remoteClient.getClientTypeId();
      try {
        if (connectionPool == null) {
          localEntityConnection = LocalEntityConnections.createConnection(domain, this.database, this.remoteClient.getDatabaseUser());
          localEntityConnection.setMethodLogger(methodLogger);
        }
        else {
          poolEntityConnection = LocalEntityConnections.createConnection(domain, this.database, connectionPool.getConnection());
          returnConnectionToPool();
        }
      }
      catch (final DatabaseException e) {
        disconnect();
        throw e;
      }
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      active.set(true);
      lastAccessTime = System.currentTimeMillis();
      final String methodName = method.getName();
      Exception exception = null;
      try {
        MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
        REQUEST_COUNTER.incrementRequestsPerSecondCounter();
        if (methodLogger.isEnabled()) {
          methodLogger.logAccess(methodName, args);
        }

        return method.invoke(getConnection(), args);
      }
      catch (final InvocationTargetException e) {
        LOG.error(e.getMessage(), e);
        exception = e.getCause() instanceof Exception ? (Exception) e.getCause() : e;

        throw exception;
      }
      catch (final ConnectionPoolException.NoConnectionAvailable e) {
        exception = e;
        throw exception;
      }
      catch (final Exception e) {
        LOG.error(e.getMessage(), e);
        exception = e;
        throw exception;
      }
      finally {
        returnConnection();
        if (methodLogger.isEnabled()) {
          final MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
          final StringBuilder messageBuilder = new StringBuilder(remoteClient.toString()).append("\n");
          entry.append(messageBuilder);
          LOG.info(messageBuilder.toString());
        }
        MDC.remove(LOG_IDENTIFIER_PROPERTY);
        active.set(false);
      }
    }

    private EntityConnection getConnection() throws DatabaseException {
      DatabaseException exception = null;
      try {
        if (methodLogger.isEnabled()) {
          methodLogger.logAccess(GET_CONNECTION, new Object[] {remoteClient.getDatabaseUser(), remoteClient.getUser()});
        }
        if (connectionPool != null) {
          return getPooledEntityConnection();
        }

        return getLocalEntityConnection();
      }
      catch (final DatabaseException ex) {
        exception = ex;
        throw ex;
      }
      finally {
        if (methodLogger.isEnabled()) {
          methodLogger.logExit(GET_CONNECTION, exception);
        }
      }
    }

    private EntityConnection getPooledEntityConnection() throws DatabaseException {
      if (poolEntityConnection.isTransactionOpen()) {
        return poolEntityConnection;
      }
      poolEntityConnection.getDatabaseConnection().setConnection(connectionPool.getConnection());
      poolEntityConnection.setMethodLogger(methodLogger);

      return poolEntityConnection;
    }

    private EntityConnection getLocalEntityConnection() throws DatabaseException {
      if (!localEntityConnection.isConnected()) {
        localEntityConnection.disconnect();//just in case
        localEntityConnection = LocalEntityConnections.createConnection(domain, database, remoteClient.getDatabaseUser());
        localEntityConnection.setMethodLogger(methodLogger);
      }

      return localEntityConnection;
    }

    /**
     * Returns the pooled connection to a connection pool if the connection is not within an open transaction
     */
    private void returnConnection() {
      if (poolEntityConnection == null || poolEntityConnection.isTransactionOpen()) {
        return;
      }
      try {
        if (methodLogger.isEnabled()) {
          methodLogger.logAccess(RETURN_CONNECTION, new Object[] {remoteClient.getDatabaseUser(), remoteClient.getUser()});
        }
        poolEntityConnection.setMethodLogger(null);
        returnConnectionToPool();
      }
      catch (final Exception e) {
        LOG.info("Exception while returning connection to pool", e);
      }
      finally {
        if (methodLogger.isEnabled()) {
          methodLogger.logExit(RETURN_CONNECTION, null, null);
        }
      }
    }

    private void returnConnectionToPool() {
      final Connection connection = poolEntityConnection.getDatabaseConnection().getConnection();
      if (connection != null) {
        Databases.closeSilently(connection);
        poolEntityConnection.getDatabaseConnection().setConnection(null);
      }
    }

    private boolean isConnected() {
      if (connectionPool != null) {
        return !disconnected;
      }

      return !disconnected && localEntityConnection != null && localEntityConnection.isConnected();
    }

    private void disconnect() {
      if (disconnected) {
        return;
      }
      disconnected = true;
      cleanupLocalConnections();
    }

    private void cleanupLocalConnections() {
      if (poolEntityConnection != null) {
        rollbackIfRequired(poolEntityConnection);
        returnConnectionToPool();
        poolEntityConnection = null;
      }
      if (localEntityConnection != null) {
        rollbackIfRequired(localEntityConnection);
        localEntityConnection.disconnect();
        localEntityConnection = null;
      }
    }

    private void rollbackIfRequired(final LocalEntityConnection entityConnection) {
      if (entityConnection.isTransactionOpen()) {
        LOG.info("Rollback open transaction on disconnect: {}", remoteClient);
        entityConnection.rollbackTransaction();
      }
    }

    private ClientLog getClientLog() {
      synchronized (methodLogger) {
        return Servers.clientLog(remoteClient.getClientId(),
                Instant.ofEpochMilli(creationDate).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                methodLogger.getEntries());
      }
    }
  }

  private static final class RequestCounter {

    private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

    private static final double THOUSAND = 1000d;

    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private final AtomicLong requestsPerSecondTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger requestsPerSecond = new AtomicInteger(0);
    private final AtomicInteger requestsPerSecondCounter = new AtomicInteger(0);

    private RequestCounter() {
      executorService.scheduleWithFixedDelay(this::updateRequestsPerSecond, 0,
              DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - requestsPerSecondTime.get()) / THOUSAND;
      if (seconds > 0) {
        requestsPerSecond.set((int) ((double) requestsPerSecondCounter.get() / seconds));
        requestsPerSecondCounter.set(0);
        requestsPerSecondTime.set(current);
      }
    }

    private void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter.incrementAndGet();
    }

    private int getRequestsPerSecond() {
      return requestsPerSecond.get();
    }
  }

  private static final class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(final Runnable runnable) {
      final Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }

  /**
   * An implementation tailored for EntityConnections.
   */
  private static final class EntityArgumentToString extends MethodLogger.ArgumentToString {

    private final EntityDefinition.Provider definitionProvider;

    private EntityArgumentToString(final EntityDefinition.Provider definitionProvider) {
      this.definitionProvider = definitionProvider;
    }

    @Override
    protected String toString(final Object object) {
      if (object == null) {
        return "null";
      }
      if (object instanceof String) {
        return "'" + object + "'";
      }
      if (object instanceof Entity) {
        return entityToString((Entity) object);
      }
      else if (object instanceof Entity.Key) {
        return entityKeyToString((Entity.Key) object);
      }

      return super.toString(object);
    }

    private String entityToString(final Entity entity) {
      final StringBuilder builder = new StringBuilder(entity.getEntityId()).append(" {");
      final List<ColumnProperty> columnProperties = definitionProvider.getDefinition(entity.getEntityId()).getColumnProperties();
      for (int i = 0; i < columnProperties.size(); i++) {
        final ColumnProperty property = columnProperties.get(i);
        final boolean modified = entity.isModified(property);
        if (property.isPrimaryKeyProperty() || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginal(property)).append("->");
          }
          valueString.append(entity.get(property.getPropertyId()));
          builder.append(property.getPropertyId()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }

    private static String entityKeyToString(final Entity.Key key) {
      return key.getEntityId() + " {" + key.toString() + "}";
    }
  }
}

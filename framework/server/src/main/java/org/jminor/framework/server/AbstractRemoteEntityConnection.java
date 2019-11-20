/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.DaemonThreadFactory;
import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.remote.ClientLog;
import org.jminor.common.remote.RemoteClient;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A base class for remote connections served by a {@link DefaultEntityConnectionServer}.
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
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @param clientSocketFactory the client socket factory to use, null for default
   * @param serverSocketFactory the server socket factory to use, null for default
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   */
  protected AbstractRemoteEntityConnection(final Domain domain, final ConnectionPool connectionPool, final Database database,
                                           final RemoteClient remoteClient, final int port, final boolean loggingEnabled,
                                           final RMIClientSocketFactory clientSocketFactory,
                                           final RMIServerSocketFactory serverSocketFactory)
          throws DatabaseException, RemoteException {
    super(port, clientSocketFactory, serverSocketFactory);
    this.connectionHandler = new RemoteEntityConnectionHandler(domain, remoteClient,
            connectionPool, database, loggingEnabled);
    this.connectionProxy = Util.initializeProxy(EntityConnection.class, connectionHandler);
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
    disconnectedEvent.fire(this);
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
    return RemoteEntityConnectionHandler.ACTIVE_CONNECTIONS.contains(connectionHandler.remoteClient.getClientId());
  }

  final void addDisconnectListener(final EventDataListener<AbstractRemoteEntityConnection> listener) {
    disconnectedEvent.addDataListener(listener);
  }

  final void removeDisconnectListener(final EventDataListener listener) {
    disconnectedEvent.removeDataListener(listener);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  static int getActiveCount() {
    return RemoteEntityConnectionHandler.ACTIVE_CONNECTIONS.size();
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
     * Contains the clientIds of active remote connections, that is, those connections that are in the process of serving a request
     */
    private static final Set<UUID> ACTIVE_CONNECTIONS = Collections.synchronizedSet(new HashSet<>());

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
                                          final ConnectionPool connectionPool, final Database database,
                                          final boolean loggingEnabled) throws DatabaseException {
      this.domain = domain;
      this.remoteClient = remoteClient;
      this.connectionPool = connectionPool;
      this.database = database;
      this.methodLogger = LocalEntityConnections.createLogger(domain);
      this.methodLogger.setEnabled(loggingEnabled);
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
      lastAccessTime = System.currentTimeMillis();
      final String methodName = method.getName();
      Exception exception = null;
      try {
        MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
        ACTIVE_CONNECTIONS.add(remoteClient.getClientId());
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
        ACTIVE_CONNECTIONS.remove(remoteClient.getClientId());
        returnConnection();
        if (methodLogger.isEnabled()) {
          final MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
          final StringBuilder messageBuilder = new StringBuilder(remoteClient.toString()).append("\n");
          MethodLogger.appendLogEntry(messageBuilder, entry, 0);
          LOG.info(messageBuilder.toString());
        }
        MDC.remove(LOG_IDENTIFIER_PROPERTY);
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
          String message = null;
          final int retryCount = poolEntityConnection == null ? 0 : poolEntityConnection.getDatabaseConnection().getRetryCount();
          if (retryCount > 0) {
            message = "retries: " + retryCount;
          }
          methodLogger.logExit(GET_CONNECTION, exception, message);
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
        connectionPool.returnConnection(connection);
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
        if (poolEntityConnection.isTransactionOpen()) {
          LOG.info("Rollback open transaction on disconnect: {}", remoteClient);
          poolEntityConnection.rollbackTransaction();
        }
        returnConnectionToPool();
        poolEntityConnection = null;
      }
      if (localEntityConnection != null) {
        if (localEntityConnection.isTransactionOpen()) {
          LOG.info("Rollback open transaction on disconnect: {}", remoteClient);
          localEntityConnection.rollbackTransaction();
        }
        localEntityConnection.disconnect();
        localEntityConnection = null;
      }
    }

    private ClientLog getClientLog() {
      synchronized (methodLogger) {
        return new ClientLog(remoteClient.getClientId(),
                Instant.ofEpochMilli(creationDate).atZone(ZoneId.systemDefault()).toLocalDateTime(), methodLogger.getEntries());
      }
    }
  }

  private static final class RequestCounter {

    private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

    private static final double THOUSAND = 1000d;

    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private long requestsPerSecondTime = System.currentTimeMillis();
    private int requestsPerSecond = 0;
    private int requestsPerSecondCounter = 0;

    private RequestCounter() {
      executorService.scheduleWithFixedDelay(this::updateRequestsPerSecond, 0,
              DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - requestsPerSecondTime) / THOUSAND;
      if (seconds > 0) {
        requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
        requestsPerSecondCounter = 0;
        requestsPerSecondTime = current;
      }
    }

    private void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter++;
    }

    private int getRequestsPerSecond() {
      return requestsPerSecond;
    }
  }
}

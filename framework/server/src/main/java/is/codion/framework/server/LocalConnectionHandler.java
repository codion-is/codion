/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.logging.MethodLogger;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ColumnProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
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

import static is.codion.common.logging.MethodLogger.methodLogger;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;

final class LocalConnectionHandler implements InvocationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LocalConnectionHandler.class);

  private static final String LOG_IDENTIFIER_PROPERTY = "logIdentifier";
  private static final String GET_CONNECTION = "getConnection";
  private static final String RETURN_CONNECTION = "returnConnection";

  static final RequestCounter REQUEST_COUNTER = new RequestCounter();

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
  private final ConnectionPoolWrapper connectionPool;

  /**
   * The method call log
   */
  private final MethodLogger methodLogger;

  /**
   * Identifies the log file being used for this connection
   */
  private final String logIdentifier;

  /**
   * Describes the logged-in user, for logging purposes
   */
  private final String userDescription;

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
   * Indicates whether this remote connection has been disconnected
   */
  private boolean closed = false;

  LocalConnectionHandler(Domain domain, RemoteClient remoteClient, Database database) throws DatabaseException {
    this.domain = domain;
    this.remoteClient = remoteClient;
    this.connectionPool = database.getConnectionPool(remoteClient.getDatabaseUser().getUsername());
    this.database = database;
    this.methodLogger = methodLogger(LocalEntityConnection.CONNECTION_LOG_SIZE.get(), new EntityArgumentToString());
    this.logIdentifier = remoteClient.getUser().getUsername().toLowerCase() + "@" + remoteClient.getClientTypeId();
    this.userDescription = "Remote user: " + remoteClient.getUser().getUsername() + ", database user: " + remoteClient.getDatabaseUser().getUsername();
    try {
      if (connectionPool == null) {
        localEntityConnection = localEntityConnection(domain, database, remoteClient.getDatabaseUser());
        localEntityConnection.setMethodLogger(methodLogger);
      }
      else {
        poolEntityConnection = localEntityConnection(domain, database, connectionPool.getConnection(remoteClient.getDatabaseUser()));
        rollbackSilently(poolEntityConnection.getDatabaseConnection());
        returnConnectionToPool();
      }
    }
    catch (DatabaseException e) {
      close();
      throw e;
    }
  }

  @Override
  public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
    active.set(true);
    lastAccessTime = System.currentTimeMillis();
    String methodName = method.getName();
    Exception exception = null;
    try {
      MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
      REQUEST_COUNTER.incrementRequestsPerSecondCounter();
      if (methodLogger.isEnabled()) {
        methodLogger.logAccess(methodName, args);
      }

      return method.invoke(getConnection(), args);
    }
    catch (InvocationTargetException e) {
      throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      exception = e;
      throw exception;
    }
    finally {
      returnConnection();
      if (methodLogger.isEnabled()) {
        MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
        StringBuilder messageBuilder = new StringBuilder(remoteClient.toString()).append("\n");
        entry.append(messageBuilder);
        LOG.info(messageBuilder.toString());
      }
      MDC.remove(LOG_IDENTIFIER_PROPERTY);
      active.set(false);
    }
  }

  boolean isConnected() {
    if (connectionPool != null) {
      return !closed;
    }

    return !closed && localEntityConnection != null && localEntityConnection.isConnected();
  }

  void close() {
    if (closed) {
      return;
    }
    closed = true;
    cleanupLocalConnections();
  }

  ClientLog getClientLog() {
    synchronized (methodLogger) {
      return ClientLog.clientLog(remoteClient.getClientId(),
              Instant.ofEpochMilli(creationDate).atZone(ZoneId.systemDefault()).toLocalDateTime(),
              methodLogger.getEntries());
    }
  }

  RemoteClient getRemoteClient() {
    return remoteClient;
  }

  long getLastAccessTime() {
    return lastAccessTime;
  }

  MethodLogger getMethodLogger() {
    return methodLogger;
  }

  boolean isActive() {
    return active.get();
  }

  boolean isClosed() {
    return closed;
  }

  private EntityConnection getConnection() throws DatabaseException {
    DatabaseException exception = null;
    try {
      if (methodLogger.isEnabled()) {
        methodLogger.logAccess(GET_CONNECTION, userDescription);
      }
      if (connectionPool != null) {
        return getPooledEntityConnection();
      }

      return getLocalEntityConnection();
    }
    catch (DatabaseException ex) {
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
    poolEntityConnection.getDatabaseConnection().setConnection(connectionPool.getConnection(remoteClient.getDatabaseUser()));
    poolEntityConnection.setMethodLogger(methodLogger);

    return poolEntityConnection;
  }

  private EntityConnection getLocalEntityConnection() throws DatabaseException {
    if (!localEntityConnection.isConnected()) {
      localEntityConnection.close();//just in case
      localEntityConnection = localEntityConnection(domain, database, remoteClient.getDatabaseUser());
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
        methodLogger.logAccess(RETURN_CONNECTION, userDescription);
      }
      poolEntityConnection.setMethodLogger(null);
      returnConnectionToPool();
    }
    catch (Exception e) {
      LOG.info("Exception while returning connection to pool", e);
    }
    finally {
      if (methodLogger.isEnabled()) {
        methodLogger.logExit(RETURN_CONNECTION, null, null);
      }
    }
  }

  private void returnConnectionToPool() {
    Connection connection = poolEntityConnection.getDatabaseConnection().getConnection();
    if (connection != null) {
      Database.closeSilently(connection);
      poolEntityConnection.getDatabaseConnection().setConnection(null);
    }
  }

  private void cleanupLocalConnections() {
    if (poolEntityConnection != null) {
      rollbackIfRequired(poolEntityConnection);
      returnConnectionToPool();
      poolEntityConnection = null;
    }
    if (localEntityConnection != null) {
      rollbackIfRequired(localEntityConnection);
      localEntityConnection.close();
      localEntityConnection = null;
    }
  }

  private void rollbackIfRequired(LocalEntityConnection entityConnection) {
    if (entityConnection.isTransactionOpen()) {
      LOG.info("Rollback open transaction on disconnect: {}", remoteClient);
      entityConnection.rollbackTransaction();
    }
  }

  private static void rollbackSilently(DatabaseConnection databaseConnection) {
    try {
      //otherwise the connection's commit state is dirty, so it gets discarded by the connection pool when we try to return it
      databaseConnection.rollback();
    }
    catch (SQLException e) {/*Silently*/}
  }

  static final class RequestCounter {

    private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

    private static final double THOUSAND = 1000d;

    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private final AtomicLong requestsPerSecondTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger requestsPerSecond = new AtomicInteger();
    private final AtomicInteger requestsPerSecondCounter = new AtomicInteger();

    private RequestCounter() {
      executorService.scheduleWithFixedDelay(this::updateRequestsPerSecond, DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL,
              DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    int getRequestsPerSecond() {
      return requestsPerSecond.get();
    }

    private void updateRequestsPerSecond() {
      long current = System.currentTimeMillis();
      double seconds = (current - requestsPerSecondTime.get()) / THOUSAND;
      if (seconds > 0) {
        requestsPerSecond.set((int) (requestsPerSecondCounter.get() / seconds));
        requestsPerSecondCounter.set(0);
        requestsPerSecondTime.set(current);
      }
    }

    private void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter.incrementAndGet();
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

  /**
   * An implementation tailored for EntityConnections.
   */
  private static final class EntityArgumentToString extends MethodLogger.DefaultArgumentToString {

    private static final String PREPARE_STATEMENT = "prepareStatement";

    @Override
    protected String toString(String methodName, Object object) {
      if (PREPARE_STATEMENT.equals(methodName)) {
        return (String) object;
      }

      return toString(object);
    }

    @Override
    protected String toString(Object object) {
      if (object == null) {
        return "null";
      }
      if (object instanceof String) {
        return "'" + object + "'";
      }
      if (object instanceof Entity) {
        return entityToString((Entity) object);
      }
      else if (object instanceof Key) {
        return entityKeyToString((Key) object);
      }

      return super.toString(object);
    }

    private static String entityToString(Entity entity) {
      StringBuilder builder = new StringBuilder(entity.getEntityType().getName()).append(" {");
      List<ColumnProperty<?>> columnProperties = entity.getDefinition().getColumnProperties();
      for (int i = 0; i < columnProperties.size(); i++) {
        ColumnProperty<?> property = columnProperties.get(i);
        boolean modified = entity.isModified(property.getAttribute());
        if (property.isPrimaryKeyColumn() || modified) {
          StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginal(property.getAttribute())).append("->");
          }
          valueString.append(entity.toString(property.getAttribute()));
          builder.append(property.getAttribute()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }

    private static String entityKeyToString(Key key) {
      return key.getEntityType() + " {" + key + "}";
    }
  }
}

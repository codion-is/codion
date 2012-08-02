/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionLogger;
import org.jminor.framework.db.EntityConnections;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An implementation of the RemoteEntityConnection interface, provides the logging of service calls
 * and database connection pooling.
 */
final class RemoteEntityConnectionImpl extends UnicastRemoteObject implements RemoteEntityConnection {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(RemoteEntityConnectionImpl.class);
  private static final String LOG_IDENTIFIER_PROPERTY = "logIdentifier";

  /**
   * Identifies the log file being used for this connection
   */
  private final String logIdentifier;

  /**
   * Contains information about the client using this connection
   */
  private final ClientInfo clientInfo;

  /**
   * Contains information about the underlying database
   */
  private final Database database;

  /**
   * A Proxy for logging method calls
   */
  private final EntityConnection connectionProxy;

  /**
   * A local connection, managed by getConnection()/returnConnection()
   */
  private transient EntityConnection localEntityConnection;

  /**
   * Indicates whether or not this remote connection has been disconnected
   */
  private boolean connected = true;

  /**
   * The date and time when this remote connection was established
   */
  private final long creationDate = System.currentTimeMillis();

  /**
   * The method call log
   */
  private final transient EntityConnectionLogger methodLogger = new EntityConnectionLogger();

  /**
   * Contains the active remote connections, that is, those connections that are in the process of serving a request
   */
  private static final List<RemoteEntityConnectionImpl> ACTIVE_CONNECTIONS = Collections.synchronizedList(new ArrayList<RemoteEntityConnectionImpl>());

  private static final String GET_CONNECTION = "getConnection";
  private static final String RETURN_CONNECTION = "returnConnection";

  private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

  private final Event evtDisconnected = Events.event();

  static {
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        RequestCounter.updateRequestsPerSecond();
      }
    }, new Date(), DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL);
  }

  /**
   * Instantiates a new RemoteEntityConnectionImpl and exports it on the given port number
   * @param database defines the underlying database
   * @param clientInfo information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @param sslEnabled specifies whether or not ssl should be enabled
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   * @throws ClassNotFoundException in case the database driver class is not found
   */
  RemoteEntityConnectionImpl(final Database database, final ClientInfo clientInfo, final int port,
                             final boolean loggingEnabled, final boolean sslEnabled)
          throws DatabaseException, ClassNotFoundException, RemoteException {
    super(port, sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    try {
      this.database = database;
      this.clientInfo = clientInfo;
      this.methodLogger.setEnabled(loggingEnabled);
      this.logIdentifier = getUser().getUsername().toLowerCase() +"@" + clientInfo.getClientTypeID();
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
      if (connectionPool != null && connectionPool.isEnabled()) {
        checkConnectionPoolCredentials(connectionPool.getUser(), clientInfo.getDatabaseUser());
      }
      else {
        localEntityConnection = EntityConnections.createConnection(database, clientInfo.getDatabaseUser());
      }
      addDisconnectListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          cleanupLocalConnection();
        }
      });
      this.connectionProxy = initializeProxy();
      try {
        clientInfo.setClientHost(getClientHost());
      }
      catch (ServerNotActiveException ignored) {}
    }
    catch (DatabaseException e) {
      disconnect();
      throw e;
    }
    catch (ClassNotFoundException e) {
      disconnect();
      throw e;
    }
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() throws RemoteException {
    return clientInfo.getUser();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized boolean isConnected() throws RemoteException {
    return connected;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void disconnect() throws RemoteException {
    if (!isConnected()) {
      return;
    }

    connected = false;
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {
      LOG.error(e.getMessage(), e);
    }
    if (localEntityConnection != null) {
      if (localEntityConnection.isTransactionOpen()) {
        localEntityConnection.rollbackTransaction();
      }
      returnConnection(false);
    }
    evtDisconnected.fire();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized int selectRowCount(final EntityCriteria criteria) throws DatabaseException, RemoteException {
    return connectionProxy.selectRowCount(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException, RemoteException, DatabaseException {
    return connectionProxy.fillReport(reportWrapper);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException {
    connectionProxy.executeProcedure(procedureID, arguments);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<?> executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
    return connectionProxy.executeFunction(functionID, arguments);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized boolean isValid() throws RemoteException {
    return connectionProxy.isValid();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void beginTransaction() throws RemoteException {
    connectionProxy.beginTransaction();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void commitTransaction() throws RemoteException {
    connectionProxy.commitTransaction();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void rollbackTransaction() throws RemoteException {
    connectionProxy.rollbackTransaction();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized boolean isTransactionOpen() throws RemoteException {
    return connectionProxy.isTransactionOpen();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException, RemoteException {
    return connectionProxy.insert(entities);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> update(final List<Entity> entities) throws DatabaseException, RemoteException {
    return connectionProxy.update(entities);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void delete(final List<Entity.Key> entityKeys) throws DatabaseException, RemoteException {
    connectionProxy.delete(entityKeys);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void delete(final EntityCriteria criteria) throws DatabaseException, RemoteException {
    connectionProxy.delete(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Object> selectPropertyValues(final String entityID, final String propertyID,
                                                        final boolean order) throws DatabaseException, RemoteException {
    return connectionProxy.selectPropertyValues(entityID, propertyID, order);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException, RemoteException {
    return connectionProxy.selectSingle(entityID, propertyID, value);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Entity selectSingle(final Entity.Key key) throws DatabaseException, RemoteException {
    return connectionProxy.selectSingle(key);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Entity selectSingle(final EntitySelectCriteria criteria) throws DatabaseException, RemoteException {
    return connectionProxy.selectSingle(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException, RemoteException {
    return connectionProxy.selectMany(keys);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectMany(final EntitySelectCriteria criteria) throws DatabaseException, RemoteException {
    return connectionProxy.selectMany(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectMany(final String entityID, final String propertyID,
                                              final Object... values) throws DatabaseException, RemoteException {
    return connectionProxy.selectMany(entityID, propertyID, values);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized List<Entity> selectAll(final String entityID) throws DatabaseException, RemoteException {
    return connectionProxy.selectAll(entityID);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException, RemoteException {
    return connectionProxy.selectDependentEntities(entities);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final byte[] blobData) throws DatabaseException, RemoteException{
    connectionProxy.writeBlob(primaryKey, blobPropertyID, blobData);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException, RemoteException {
    return connectionProxy.readBlob(primaryKey, blobPropertyID);
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() {
    throw new UnsupportedOperationException("getDatabaseConnection is not supported on remote connections");
  }

  /**
   * @return information on the client using this remote connection
   */
  ClientInfo getClientInfo() {
    return clientInfo;
  }

  /**
   * @return a ServerLog instance containing information about this connections recent activity
   * @see org.jminor.framework.Configuration#SERVER_CONNECTION_LOG_SIZE
   */
  ServerLog getServerLog() {
    return new ServerLog(clientInfo.getClientID(), creationDate, methodLogger.getLogEntries(),
            methodLogger.getLastAccessDate(), methodLogger.getLastExitDate(), methodLogger.getLastAccessedMethod(),
            methodLogger.getLastAccessMessage(), methodLogger.getLastExitedMethod());
  }

  /**
   * @return the object containing the method call log
   */
  MethodLogger getMethodLogger() {
    return methodLogger;
  }

  /**
   * @param timeout the number of milliseconds
   * @return true if this connection has been inactive for <code>timeout</code> milliseconds or longer
   */
  boolean hasBeenInactive(final int timeout) {
    return System.currentTimeMillis() - methodLogger.getLastAccessDate() > timeout;
  }

  /**
   * @return true during a remote method call
   */
  boolean isActive() {
    return ACTIVE_CONNECTIONS.contains(this);
  }

  void addDisconnectListener(final EventListener listener) {
    evtDisconnected.addListener(listener);
  }

  void removeDisconnectListener(final EventListener listener) {
    evtDisconnected.removeListener(listener);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  static int getActiveCount() {
    return ACTIVE_CONNECTIONS.size();
  }

  /**
   * @return a List containing the the enabled connection pools
   */
  static List<User> getEnabledConnectionPools() {
    final List<User> enabledPoolUsers = new ArrayList<User>();
    for (final ConnectionPool pool : ConnectionPools.getConnectionPools()) {
      if (pool.isEnabled()) {
        enabledPoolUsers.add(pool.getUser());
      }
    }

    return enabledPoolUsers;
  }

  static boolean isPoolEnabled(final User user) throws RemoteException {
    return ConnectionPools.getConnectionPool(user).isEnabled();
  }

  static void setPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    ConnectionPools.getConnectionPool(user).setEnabled(enabled);
  }

  static int getPoolCleanupInterval(final User user) {
    return ConnectionPools.getConnectionPool(user).getCleanupInterval();
  }

  static void setPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    ConnectionPools.getConnectionPool(user).setCleanupInterval(poolCleanupInterval);
  }

  static int getMaximumPoolSize(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumPoolSize();
  }

  static void setMaximumPoolSize(final User user, final int value) {
    ConnectionPools.getConnectionPool(user).setMaximumPoolSize(value);
  }

  static int getMaximumPoolCheckOutTime(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumCheckOutTime();
  }

  static void setMaximumPoolCheckOutTime(final User user, final int value) {
    ConnectionPools.getConnectionPool(user).setMaximumCheckOutTime(value);
  }

  static int getMinimumPoolSize(final User user) {
    return ConnectionPools.getConnectionPool(user).getMinimumPoolSize();
  }

  static void setMinimumPoolSize(final User user, final int value) {
    ConnectionPools.getConnectionPool(user).setMinimumPoolSize(value);
  }

  static int getPoolConnectionThreshold(final User user) {
    return ConnectionPools.getConnectionPool(user).getNewConnectionThreshold();
  }

  static void setPoolConnectionThreshold(final User user, final int value) {
    ConnectionPools.getConnectionPool(user).setNewConnectionThreshold(value);
  }

  static int getPoolConnectionTimeout(final User user) {
    return ConnectionPools.getConnectionPool(user).getConnectionTimeout();
  }

  static void setPoolConnectionTimeout(final User user, final int timeout) {
    ConnectionPools.getConnectionPool(user).setConnectionTimeout(timeout);
  }

  static int getMaximumPoolRetryWaitPeriod(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumRetryWaitPeriod();
  }

  static void setMaximumPoolRetryWaitPeriod(final User user, final int value) {
    ConnectionPools.getConnectionPool(user).setMaximumRetryWaitPeriod(value);
  }

  static ConnectionPoolStatistics getPoolStatistics(final User user, final long since) {
    return ConnectionPools.getConnectionPool(user).getStatistics(since);
  }

  static void resetPoolStatistics(final User user) {
    ConnectionPools.getConnectionPool(user).resetStatistics();
  }

  static boolean isCollectFineGrainedPoolStatistics(final User user) {
    return ConnectionPools.getConnectionPool(user).isCollectFineGrainedStatistics();
  }

  static void setCollectFineGrainedPoolStatistics(final User user, final boolean value) {
    ConnectionPools.getConnectionPool(user).setCollectFineGrainedStatistics(value);
  }

  static int getRequestsPerSecond() {
    return RequestCounter.getRequestsPerSecond();
  }

  static int getWarningTimeExceededPerSecond() {
    return RequestCounter.getWarningTimeExceededPerSecond();
  }

  static int getWarningThreshold() {
    return RequestCounter.getWarningThreshold();
  }

  static void setWarningThreshold(final int threshold) {
    RequestCounter.setWarningThreshold(threshold);
  }

  private void setActive() {
    ACTIVE_CONNECTIONS.add(this);
  }

  private void setInactive() {
    ACTIVE_CONNECTIONS.remove(this);
  }

  private EntityConnection initializeProxy() throws ClassNotFoundException, DatabaseException {
    return Util.initializeProxy(EntityConnection.class, new RemoteConnectionHandler(this));
  }

  private void cleanupLocalConnection() {
    if (localEntityConnection != null) {
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
      if (connectionPool != null && connectionPool.isEnabled()) {
        if (localEntityConnection.isTransactionOpen()) {
          localEntityConnection.rollbackTransaction();
        }
        returnConnection(false);
      }
      else {
        localEntityConnection.disconnect();
      }
      localEntityConnection = null;
    }
  }

  private EntityConnection getConnection(final boolean logMethod) throws ClassNotFoundException, DatabaseException {
    Exception exception = null;
    try {
      if (logMethod) {
        methodLogger.logAccess(GET_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
      }
      final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
      final boolean poolEnabled = connectionPool != null && connectionPool.isEnabled();
      if (localEntityConnection != null) {//pool not enabled or a transaction is open
        if (poolEnabled) {
          if (!localEntityConnection.isTransactionOpen()) {
            throw new IllegalStateException("Local connection, not within a transaction, is available when pool is enabled");
          }
        }
        else if (!localEntityConnection.isValid()) {//dead connection, no pool
          localEntityConnection.disconnect();
          localEntityConnection = EntityConnections.createConnection(database, clientInfo.getDatabaseUser());
        }

        return localEntityConnection;
      }
      if (!poolEnabled) {
        throw new DatabaseException("No connection pool available or enabled for user: " + clientInfo.getDatabaseUser());
      }
      localEntityConnection = (EntityConnection) connectionPool.getConnection();
      if (methodLogger.isEnabled()) {
        localEntityConnection.getDatabaseConnection().setLoggingEnabled(methodLogger.isEnabled());
      }

      return localEntityConnection;
    }
    catch (ClassNotFoundException ex) {
      exception = ex;
      throw ex;
    }
    catch (DatabaseException ex) {
      exception = ex;
      throw ex;
    }
    finally {
      if (logMethod) {
        String message = null;
        if (localEntityConnection != null && localEntityConnection.getDatabaseConnection().getRetryCount() > 0) {
          message = "retries: " + localEntityConnection.getDatabaseConnection().getRetryCount();
        }
        methodLogger.logExit(GET_CONNECTION, exception, null, message);
      }
    }
  }

  /**
   * Returns the local connection to a connection pool if one is available and the connection
   * is not within an open transaction
   * @param logMethod if true then this method call is logged with the method logger
   */
  private void returnConnection(final boolean logMethod) {
    if (localEntityConnection == null || localEntityConnection.isTransactionOpen()) {
      return;
    }
    final ConnectionPool connectionPool = ConnectionPools.getConnectionPool(clientInfo.getDatabaseUser());
    final boolean poolEnabled = connectionPool != null && connectionPool.isEnabled();
    if (poolEnabled) {
      try {
        if (logMethod) {
          methodLogger.logAccess(RETURN_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
        }
        if (methodLogger.isEnabled()) {
          //we turned logging on when we fetched the connection, turn it off again
          localEntityConnection.getDatabaseConnection().setLoggingEnabled(false);
        }
        connectionPool.returnConnection(localEntityConnection.getDatabaseConnection());
        localEntityConnection = null;
      }
      finally {
        if (logMethod) {
          methodLogger.logExit(RETURN_CONNECTION, null, null);
        }
      }
    }
  }

  /**
   * Checks the credentials provided by <code>clientInfo</code> against the credentials
   * found in the connection pool user
   * @param connectionPoolUser the connection pool user credentials
   * @param user the user credentials to check
   * @throws DatabaseException in case the password does not match the one in the connection pool user
   */
  private static void checkConnectionPoolCredentials(final User connectionPoolUser, final User user) throws DatabaseException {
    if (!connectionPoolUser.getPassword().equals(user.getPassword())) {
      throw new DatabaseException("Wrong username or password for connection pool");
    }
  }

  private static final class RemoteConnectionHandler implements InvocationHandler {

    private final RemoteEntityConnectionImpl remoteEntityConnection;
    private final EntityConnectionLogger methodLogger;

    private RemoteConnectionHandler(final RemoteEntityConnectionImpl remoteEntityConnection) throws DatabaseException, ClassNotFoundException {
      this.remoteEntityConnection = remoteEntityConnection;
      this.methodLogger = remoteEntityConnection.methodLogger;
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      Exception exception = null;
      final boolean logMethod = methodLogger.isEnabled() && methodLogger.shouldMethodBeLogged(methodName);
      final long startTime = System.currentTimeMillis();
      try {
        MDC.put(LOG_IDENTIFIER_PROPERTY, remoteEntityConnection.logIdentifier);
        remoteEntityConnection.setActive();
        RequestCounter.incrementRequestsPerSecondCounter();
        remoteEntityConnection.getConnection(logMethod);
        if (logMethod) {
          methodLogger.logAccess(methodName, args);
        }

        return method.invoke(remoteEntityConnection.localEntityConnection, args);
      }
      catch (Exception e) {
        exception = Util.unwrapAndLog(e, InvocationTargetException.class, LOG,
                ConnectionPoolException.NoConnectionAvailable.class);
        throw exception;
      }
      finally {
        remoteEntityConnection.setInactive();
        final long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > RequestCounter.warningThreshold) {
          RequestCounter.incrementWarningTimeExceededCounter();
        }
        if (logMethod) {
          final LogEntry logEntry = methodLogger.logExit(methodName, exception, remoteEntityConnection.localEntityConnection != null
                  ? remoteEntityConnection.localEntityConnection.getDatabaseConnection().getLogEntries() : null);
          if (methodLogger.isEnabled()) {
            final StringBuilder messageBuilder = new StringBuilder(remoteEntityConnection.getClientInfo().toString()).append("\n");
            EntityConnectionLogger.appendLogEntries(messageBuilder, Arrays.asList(logEntry), 1);
            LOG.info(messageBuilder.toString());
          }
        }
        remoteEntityConnection.returnConnection(logMethod);
        MDC.remove(LOG_IDENTIFIER_PROPERTY);
      }
    }
  }

  private static final class RequestCounter {//todo should I bother to synchronize this?

    private static long requestsPerSecondTime = System.currentTimeMillis();
    private static int requestsPerSecond = 0;
    private static int requestsPerSecondCounter = 0;
    private static int warningThreshold = 60;
    private static int warningTimeExceededPerSecond = 0;
    private static int warningTimeExceededCounter = 0;

    private RequestCounter() {}

    static void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - requestsPerSecondTime) / 1000d;
      if (seconds > 0) {
        requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
        warningTimeExceededPerSecond = (int) ((double) warningTimeExceededCounter / seconds);
        warningTimeExceededCounter = 0;
        requestsPerSecondCounter = 0;
        requestsPerSecondTime = current;
      }
    }

    private static int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    private static int getWarningTimeExceededPerSecond() {
      return warningTimeExceededPerSecond;
    }

    private static int getWarningThreshold() {
      return warningThreshold;
    }

    private static void setWarningThreshold(final int warningThreshold) {
      RequestCounter.warningThreshold = warningThreshold;
    }

    private static void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter++;
    }

    private static void incrementWarningTimeExceededCounter() {
      warningTimeExceededCounter++;
    }
  }
}

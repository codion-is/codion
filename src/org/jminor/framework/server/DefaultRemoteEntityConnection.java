/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.framework.db.EntityConnection;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the RemoteEntityConnection interface, provides the logging of service calls
 * and database connection pooling.
 */
final class DefaultRemoteEntityConnection extends UnicastRemoteObject implements RemoteEntityConnection {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(DefaultRemoteEntityConnection.class);
  private static final RequestCounter REQUEST_COUNTER = new RequestCounter();
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
   * The connection pool to use, if any
   */
  private final ConnectionPool connectionPool;

  /**
   * A local connection used in case no connection pool is provided, managed by getConnection()/returnConnection()
   */
  private EntityConnection localEntityConnection;

  /**
   * A local connection used by the connection pool, managed by getConnection()/returnConnection()
   */
  private EntityConnection poolEntityConnection;

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
  private final transient MethodLogger methodLogger = EntityConnections.createLogger();

  /**
   * Contains the active remote connections, that is, those connections that are in the process of serving a request
   */
  private static final List<DefaultRemoteEntityConnection> ACTIVE_CONNECTIONS = Collections.synchronizedList(new ArrayList<DefaultRemoteEntityConnection>());

  private static final String GET_CONNECTION = "getConnection";
  private static final String RETURN_CONNECTION = "returnConnection";

  private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

  private final Event disconnectedEvent = Events.event();

  /**
   * Instantiates a new DefaultRemoteEntityConnection and exports it on the given port number
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
  DefaultRemoteEntityConnection(final Database database, final ClientInfo clientInfo, final int port,
                                final boolean loggingEnabled, final boolean sslEnabled)
          throws DatabaseException, ClassNotFoundException, RemoteException {
    this(null, database, clientInfo, port, loggingEnabled, sslEnabled);
  }

  /**
   * Instantiates a new DefaultRemoteEntityConnection and exports it on the given port number
   * @param connectionPool the connection pool to use, if none is provided a local connection is established
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
  DefaultRemoteEntityConnection(final ConnectionPool connectionPool, final Database database, final ClientInfo clientInfo,
                                final int port, final boolean loggingEnabled, final boolean sslEnabled)
          throws DatabaseException, ClassNotFoundException, RemoteException {
    super(port, sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    try {
      this.database = database;
      this.connectionPool = connectionPool;
      this.clientInfo = clientInfo;
      this.methodLogger.setEnabled(loggingEnabled);
      this.logIdentifier = getUser().getUsername().toLowerCase() +"@" + clientInfo.getClientTypeID();
      if (connectionPool == null) {
        localEntityConnection = EntityConnections.createConnection(database, clientInfo.getDatabaseUser());
        localEntityConnection.setMethodLogger(methodLogger);
      }
      else {
        poolEntityConnection = EntityConnections.createConnection(database, connectionPool.getConnection());
        returnConnectionToPool();
      }
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
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() throws RemoteException {
    return clientInfo.getUser();
  }

  /** {@inheritDoc} */
  @Override
  public void setMethodLogger(final MethodLogger methodLogger) throws RemoteException{
    throw new UnsupportedOperationException("setMethodLogger is not supported on remote connections");
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
    cleanupLocalConnections();
    disconnectedEvent.fire();
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
  public synchronized List executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
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
   * @return a ClientLog instance containing information about this connections recent activity
   * @see org.jminor.framework.Configuration#SERVER_CONNECTION_LOG_SIZE
   */
  ClientLog getClientLog() {
    synchronized (methodLogger) {
      return new ClientLog(clientInfo.getClientID(), creationDate, methodLogger.getEntries());
    }
  }

  /**
   * @param timeout the number of milliseconds
   * @return true if this connection has been inactive for <code>timeout</code> milliseconds or longer
   */
  boolean hasBeenInactive(final int timeout) {
    return System.currentTimeMillis() - methodLogger.getLastAccessTime() > timeout;
  }

  void setLoggingEnabled(final boolean status) {
    methodLogger.setEnabled(status);
  }

  boolean isLoggingEnabled() {
    return methodLogger.isEnabled();
  }

  /**
   * @return true during a remote method call
   */
  boolean isActive() {
    return ACTIVE_CONNECTIONS.contains(this);
  }

  void addDisconnectListener(final EventListener listener) {
    disconnectedEvent.addListener(listener);
  }

  void removeDisconnectListener(final EventListener listener) {
    disconnectedEvent.removeListener(listener);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  static int getActiveCount() {
    return ACTIVE_CONNECTIONS.size();
  }

  static int getRequestsPerSecond() {
    return REQUEST_COUNTER.getRequestsPerSecond();
  }

  static int getWarningTimeExceededPerSecond() {
    return REQUEST_COUNTER.getWarningTimeExceededPerSecond();
  }

  static int getWarningThreshold() {
    return REQUEST_COUNTER.getWarningThreshold();
  }

  static void setWarningThreshold(final int threshold) {
    REQUEST_COUNTER.setWarningThreshold(threshold);
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

  private void cleanupLocalConnections() {
    if (poolEntityConnection != null) {
      if (poolEntityConnection.isTransactionOpen()) {
        LOG.info("Rollback open transaction on disconnect: {}", clientInfo);
        poolEntityConnection.rollbackTransaction();
      }
      returnConnectionToPool();
      poolEntityConnection = null;
    }
    if (localEntityConnection != null) {
      if (localEntityConnection.isTransactionOpen()) {
        LOG.info("Rollback open transaction on disconnect: {}", clientInfo);
        localEntityConnection.rollbackTransaction();
      }
      localEntityConnection.disconnect();
      localEntityConnection = null;
    }
  }

  private EntityConnection getConnection() throws DatabaseException {
    Exception exception = null;
    try {
      methodLogger.logAccess(GET_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
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
      String message = null;
      if (poolEntityConnection != null && poolEntityConnection.getDatabaseConnection().getRetryCount() > 0) {
        message = "retries: " + poolEntityConnection.getDatabaseConnection().getRetryCount();
      }
      methodLogger.logExit(GET_CONNECTION, exception, message);
    }
  }

  /**
   * Returns the pooled connection to a connection pool if the connection is not within an open transaction
   */
  private void returnConnection() {
    if (poolEntityConnection == null || poolEntityConnection.isTransactionOpen()) {
      return;
    }
    try {
      methodLogger.logAccess(RETURN_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
      poolEntityConnection.setMethodLogger(null);
      returnConnectionToPool();
    }
    catch (Exception e) {
      LOG.info("Exception while returning connection to pool", e);
    }
    finally {
      methodLogger.logExit(RETURN_CONNECTION, null, null);
    }
  }

  private EntityConnection getPooledEntityConnection() throws DatabaseException {
    if (poolEntityConnection.isTransactionOpen()) {
      return poolEntityConnection;
    }
    EntityConnections.setConnection(poolEntityConnection, connectionPool.getConnection());
    poolEntityConnection.setMethodLogger(methodLogger);

    return poolEntityConnection;
  }

  private void returnConnectionToPool() {
    if (poolEntityConnection.isConnected()) {
      connectionPool.returnConnection(poolEntityConnection.getDatabaseConnection().getConnection());
      EntityConnections.setConnection(poolEntityConnection, null);
    }
  }

  private EntityConnection getLocalEntityConnection() throws DatabaseException {
    if (!localEntityConnection.isValid()) {
      localEntityConnection.disconnect();
      localEntityConnection = EntityConnections.createConnection(database, clientInfo.getDatabaseUser());
      localEntityConnection.setMethodLogger(methodLogger);
    }

    return localEntityConnection;
  }

  private static final class RemoteConnectionHandler implements InvocationHandler {

    private final DefaultRemoteEntityConnection remoteEntityConnection;
    private final MethodLogger methodLogger;

    private RemoteConnectionHandler(final DefaultRemoteEntityConnection remoteEntityConnection) throws DatabaseException, ClassNotFoundException {
      this.remoteEntityConnection = remoteEntityConnection;
      this.methodLogger = remoteEntityConnection.methodLogger;
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      Exception exception = null;
      final long startTime = System.currentTimeMillis();
      try {
        MDC.put(LOG_IDENTIFIER_PROPERTY, remoteEntityConnection.logIdentifier);
        remoteEntityConnection.setActive();
        REQUEST_COUNTER.incrementRequestsPerSecondCounter();
        methodLogger.logAccess(methodName, args);

        final EntityConnection connection = remoteEntityConnection.getConnection();

        return method.invoke(connection, args);
      }
      catch (Exception e) {
        exception = Util.unwrapAndLog(e, InvocationTargetException.class, LOG,
                ConnectionPoolException.NoConnectionAvailable.class);
        throw exception;
      }
      finally {
        remoteEntityConnection.setInactive();
        final long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > REQUEST_COUNTER.warningThreshold) {
          REQUEST_COUNTER.incrementWarningTimeExceededCounter();
        }
        remoteEntityConnection.returnConnection();
        final MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
        if (methodLogger.isEnabled()) {
          final StringBuilder messageBuilder = new StringBuilder(remoteEntityConnection.getClientInfo().toString()).append("\n");
          MethodLogger.appendLogEntry(messageBuilder, entry, 0);
          LOG.info(messageBuilder.toString());
        }
        MDC.remove(LOG_IDENTIFIER_PROPERTY);
      }
    }
  }

  private static final class RequestCounter {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new Util.DaemonThreadFactory());
    private long requestsPerSecondTime = System.currentTimeMillis();
    private int requestsPerSecond = 0;
    private int requestsPerSecondCounter = 0;
    private int warningThreshold = 60;
    private int warningTimeExceededPerSecond = 0;
    private int warningTimeExceededCounter = 0;

    private RequestCounter() {
      executorService.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          updateRequestsPerSecond();
        }
      }, 0, DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void updateRequestsPerSecond() {
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

    private void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter++;
    }

    private void incrementWarningTimeExceededCounter() {
      warningTimeExceededCounter++;
    }

    private int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    private int getWarningTimeExceededPerSecond() {
      return warningTimeExceededPerSecond;
    }

    private int getWarningThreshold() {
      return warningThreshold;
    }

    private void setWarningThreshold(final int warningThreshold) {
      this.warningThreshold = warningThreshold;
    }
  }
}

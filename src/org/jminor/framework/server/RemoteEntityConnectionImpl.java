/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.db.pool.ConnectionPoolImpl;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.db.pool.PoolableConnectionProvider;
import org.jminor.common.model.Event;
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
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionImpl;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
  private final EntityConnection loggingEntityConnectionProxy;
  /**
   * The db connection used if connection pooling is not enabled
   */
  private EntityConnectionImpl entityConnection;
  /**
   * Indicates whether or not this remote connection is enabled
   */
  private boolean connected = true;
  /**
   * The date and time when this remote connection was established
   */
  private final long creationDate = System.currentTimeMillis();
  /**
   * The object containing the method call log
   */
  private final transient MethodLogger methodLogger;
  /**
   * Contains the active remote connections, that is, those connections that are in the middle of serving a request
   */
  private static final List<RemoteEntityConnectionImpl> ACTIVE_CONNECTIONS = Collections.synchronizedList(new ArrayList<RemoteEntityConnectionImpl>());
  /**
   * The available connection pools
   */
  private static final Map<User, ConnectionPool> CONNECTION_POOLS = Collections.synchronizedMap(new HashMap<User, ConnectionPool>());

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
   */
  RemoteEntityConnectionImpl(final Database database, final ClientInfo clientInfo, final int port,
                             final boolean loggingEnabled, final boolean sslEnabled) throws RemoteException {
    super(port, sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    if (CONNECTION_POOLS.containsKey(clientInfo.getUser())) {
      CONNECTION_POOLS.get(clientInfo.getUser()).getUser().setPassword(clientInfo.getUser().getPassword());
    }
    this.database = database;
    this.clientInfo = clientInfo;
    this.loggingEntityConnectionProxy = initializeProxy();
    this.methodLogger = new RemoteLogger();
    this.methodLogger.setEnabled(loggingEnabled);
    try {
      clientInfo.setClientHost(getClientHost());
    }
    catch (ServerNotActiveException e) {/**/}
  }

  /** {@inheritDoc} */
  public User getUser() throws RemoteException {
    return clientInfo.getUser();
  }

  /** {@inheritDoc} */
  public boolean isConnected() throws RemoteException {
    return entityConnection == null ? connected : entityConnection.isConnected();
  }

  /** {@inheritDoc} */
  public void disconnect() throws RemoteException {
    if (!isConnected()) {
      return;
    }

    if (entityConnection != null) {
      entityConnection.disconnect();
    }
    entityConnection = null;
    connected = false;
    try {
      UnicastRemoteObject.unexportObject(this, true);
    }
    catch (NoSuchObjectException e) {
      LOG.error(e.getMessage(), e);
    }
    evtDisconnected.fire();
  }

  /** {@inheritDoc} */
  public int selectRowCount(final EntityCriteria criteria) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectRowCount(criteria);
  }

  /** {@inheritDoc} */
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException, RemoteException, DatabaseException {
    return loggingEntityConnectionProxy.fillReport(reportWrapper);
  }

  /** {@inheritDoc} */
  public List<List> selectRows(final String statement, final int fetchCount) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectRows(statement, fetchCount);
  }

  /** {@inheritDoc} */
  public void executeProcedure(final String procedureID, final List<Object> arguments) throws DatabaseException {
    loggingEntityConnectionProxy.executeProcedure(procedureID, arguments);
  }

  /** {@inheritDoc} */
  public List<Object> executeFunction(final String functionID, final List<Object> arguments) throws DatabaseException {
    return loggingEntityConnectionProxy.executeFunction(functionID, arguments);
  }

  /** {@inheritDoc} */
  public boolean isValid() throws RemoteException {
    return loggingEntityConnectionProxy.isValid();
  }

  /** {@inheritDoc} */
  public void beginTransaction() throws RemoteException {
    loggingEntityConnectionProxy.beginTransaction();
  }

  /** {@inheritDoc} */
  public void commitTransaction() throws RemoteException {
    loggingEntityConnectionProxy.commitTransaction();
  }

  /** {@inheritDoc} */
  public void rollbackTransaction() throws RemoteException {
    loggingEntityConnectionProxy.rollbackTransaction();
  }

  /** {@inheritDoc} */
  public boolean isTransactionOpen() throws RemoteException {
    return loggingEntityConnectionProxy.isTransactionOpen();
  }

  /** {@inheritDoc} */
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.insert(entities);
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.update(entities);
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException, RemoteException {
    loggingEntityConnectionProxy.delete(entityKeys);
  }

  /** {@inheritDoc} */
  public void delete(final EntityCriteria criteria) throws DatabaseException, RemoteException {
    loggingEntityConnectionProxy.delete(criteria);
  }

  /** {@inheritDoc} */
  public List<Object> selectPropertyValues(final String entityID, final String propertyID,
                                           final boolean order) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectPropertyValues(entityID, propertyID, order);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectSingle(entityID, propertyID, value);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final Entity.Key key) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectSingle(key);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntitySelectCriteria criteria) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectSingle(criteria);
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectMany(keys);
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectMany(criteria);
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectMany(entityID, propertyID, values);
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectAll(entityID);
  }

  /** {@inheritDoc} */
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.selectDependentEntities(entities);
  }

  /** {@inheritDoc} */
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final String dataDescription,
                        final byte[] blobData) throws DatabaseException, RemoteException{
    loggingEntityConnectionProxy.writeBlob(primaryKey, blobPropertyID, dataDescription, blobData);
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException, RemoteException {
    return loggingEntityConnectionProxy.readBlob(primaryKey, blobPropertyID);
  }

  /**
   * @return information on the client using this remote connection
   */
  ClientInfo getClientInfo() {
    return clientInfo;
  }

  /**
   * @return a ServerLog instance containing information about this connections recent activity
   * @see Configuration#SERVER_CONNECTION_LOG_SIZE
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

  void addDisconnectListener(final ActionListener listener) {
    evtDisconnected.addListener(listener);
  }

  void removeDisconnectListener(final ActionListener listener) {
    evtDisconnected.removeListener(listener);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  static int getActiveCount() {
    return ACTIVE_CONNECTIONS.size();
  }

  /**
   * @return a List containing the settings of the enabled connection pools
   */
  static List<User> getEnabledConnectionPoolSettings() {
    final List<User> enabledPoolUsers = new ArrayList<User>();
    for (final ConnectionPool pool : CONNECTION_POOLS.values()) {
      if (pool.isEnabled()) {
        enabledPoolUsers.add(pool.getUser());
      }
    }

    return enabledPoolUsers;
  }

  static boolean isPoolEnabled(final User user) throws RemoteException {
    return CONNECTION_POOLS.get(user).isEnabled();
  }

  static void setPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    CONNECTION_POOLS.get(user).setEnabled(enabled);
  }

  static int getPoolCleanupInterval(final User user) {
    return CONNECTION_POOLS.get(user).getCleanupInterval();
  }

  static void setPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    CONNECTION_POOLS.get(user).setCleanupInterval(poolCleanupInterval);
  }

  static int getMaximumPoolSize(final User user) {
    return CONNECTION_POOLS.get(user).getMaximumPoolSize();
  }

  static void setMaximumPoolSize(final User user, final int value) {
    CONNECTION_POOLS.get(user).setMaximumPoolSize(value);
  }

  static int getMaximumPoolCheckOutTime(final User user) {
    return CONNECTION_POOLS.get(user).getMaximumCheckOutTime();
  }

  static void setMaximumPoolCheckOutTime(final User user, final int value) {
    CONNECTION_POOLS.get(user).setMaximumCheckOutTime(value);
  }

  static int getMinimumPoolSize(final User user) {
    return CONNECTION_POOLS.get(user).getMinimumPoolSize();
  }

  static void setMinimumPoolSize(final User user, final int value) {
    CONNECTION_POOLS.get(user).setMinimumPoolSize(value);
  }

  static int getPoolConnectionThreshold(final User user) {
    return CONNECTION_POOLS.get(user).getNewConnectionThreshold();
  }

  static void setPoolConnectionThreshold(final User user, final int value) {
    CONNECTION_POOLS.get(user).setNewConnectionThreshold(value);
  }

  static int getPoolConnectionTimeout(final User user) {
    return CONNECTION_POOLS.get(user).getConnectionTimeout();
  }

  static void setPoolConnectionTimeout(final User user, final int timeout) {
    CONNECTION_POOLS.get(user).setConnectionTimeout(timeout);
  }

  static int getMaximumPoolRetryWaitPeriod(final User user) {
    return CONNECTION_POOLS.get(user).getMaximumRetryWaitPeriod();
  }

  static void setMaximumPoolRetryWaitPeriod(final User user, final int value) {
    CONNECTION_POOLS.get(user).setMaximumRetryWaitPeriod(value);
  }

  static ConnectionPoolStatistics getPoolStatistics(final User user, final long since) {
    return CONNECTION_POOLS.get(user).getStatistics(since);
  }

  static void resetPoolStatistics(final User user) {
    CONNECTION_POOLS.get(user).resetStatistics();
  }

  static boolean isCollectFineGrainedPoolStatistics(final User user) {
    return CONNECTION_POOLS.get(user).isCollectFineGrainedStatistics();
  }

  static void setCollectFineGrainedPoolStatistics(final User user, final boolean value) {
    CONNECTION_POOLS.get(user).setCollectFineGrainedStatistics(value);
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

  static void initializeConnectionPools(final Database database) {
    final String initialPoolUsers = Configuration.getStringValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    if (!Util.nullOrEmpty(initialPoolUsers)) {
      for (final String username : initialPoolUsers.split(",")) {
        final User poolUser = new User(username.trim(), null);
        CONNECTION_POOLS.put(poolUser, new ConnectionPoolImpl(new ConnectionProvider(database), poolUser));
      }
    }
  }

  private void setActive() {
    ACTIVE_CONNECTIONS.add(this);
  }

  private void setInactive() {
    ACTIVE_CONNECTIONS.remove(this);
  }

  private EntityConnection initializeProxy() {
    return (EntityConnection) Proxy.newProxyInstance(EntityConnectionImpl.class.getClassLoader(),
            EntityConnectionImpl.class.getInterfaces(), new LoggingInvocationHandler(clientInfo));
  }

  private EntityConnectionImpl getConnection() throws ClassNotFoundException, SQLException {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(clientInfo.getUser());
    if (connectionPool != null && connectionPool.isEnabled()) {
      if (entityConnection != null) {//pool has been turned on since this one was created
        entityConnection.disconnect();//discard
        entityConnection = null;
      }
      final EntityConnectionImpl pooledDbConnection = (EntityConnectionImpl) connectionPool.getConnection();
      if (methodLogger.isEnabled()) {
        pooledDbConnection.setLoggingEnabled(methodLogger.isEnabled());
      }

      return pooledDbConnection;
    }

    if (entityConnection == null) {
      entityConnection = createDbConnection(database, clientInfo.getUser());
    }
    else {
      if (!entityConnection.isValid()) {//dead connection
        LOG.debug("Removing an invalid database connection: " + entityConnection);
        entityConnection.disconnect();//just in case
        entityConnection = createDbConnection(database, clientInfo.getUser());
      }
    }
    entityConnection.setLoggingEnabled(methodLogger.isEnabled());

    return entityConnection;
  }

  private void returnConnection(final EntityConnectionImpl connection, final boolean logMethod) {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(clientInfo.getUser());
    if (methodLogger.isEnabled()) {
      connection.setLoggingEnabled(false);
    }
    if (connectionPool != null && connectionPool.isEnabled()) {
      try {
        if (logMethod) {
          methodLogger.logAccess(RETURN_CONNECTION, new Object[]{clientInfo.getUser()});
        }
        connectionPool.returnConnection(connection);
      }
      finally {
        if (logMethod) {
          methodLogger.logExit(RETURN_CONNECTION, null, null);
        }
      }
    }
  }

  private static EntityConnectionImpl createDbConnection(final Database database, final User user) throws ClassNotFoundException, SQLException {
    return new EntityConnectionImpl(database, user);
  }

  private static final String IS_CONNECTED = "isConnected";
  private static final String CONNECTION_VALID = "isValid";
  private static final String GET_ACTIVE_USER = "getActiveUser";

  private static boolean shouldMethodBeLogged(final String methodName) {
    return !(methodName.equals(IS_CONNECTED) || methodName.equals(CONNECTION_VALID) || methodName.equals(GET_ACTIVE_USER));
  }

  private final class LoggingInvocationHandler implements InvocationHandler {

    private static final String GET_CONNECTION = "getConnection";
    private final ClientInfo client;

    private LoggingInvocationHandler(final ClientInfo client) {
      this.client = client;
    }

    /**
     * Holds the connection while a transaction is open, since it isn't proper to return one to the pool in such a state
     */
    private volatile EntityConnectionImpl transactionDbConnection;

    /** {@inheritDoc} */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      Exception exception = null;
      EntityConnectionImpl connection = transactionDbConnection;
      final boolean logMethod = methodLogger.isEnabled() && shouldMethodBeLogged(methodName);
      final long startTime = System.currentTimeMillis();
      try {
        setActive();
        RequestCounter.incrementRequestsPerSecondCounter();
        if (connection == null) {
          Exception getException = null;
          methodLogger.logAccess(GET_CONNECTION, new Object[]{clientInfo.getUser()});
          try {
            connection = getConnection();
          }
          catch (Exception e) {
            getException = e;
            throw e;
          }
          finally {
            String message = null;
            if (connection != null && connection.getRetryCount() > 0) {
              message = "retries: " + connection.getRetryCount();
            }
            methodLogger.logExit(GET_CONNECTION, getException, null, message);
          }
        }
        else {
          //we must be within a transaction, basic sanity check, cheap operation
          if (!connection.isTransactionOpen()) {
            throw new IllegalStateException("Transaction closed");
          }
        }
        if (logMethod) {
          methodLogger.logAccess(methodName, args);
        }

        return method.invoke(connection, args);
      }
      catch (Exception e) {
        exception = Util.unwrapAndLog(e, InvocationTargetException.class, LOG,
                ConnectionPoolException.NoConnectionAvailable.class);
        throw exception;
      }
      finally {
        setInactive();
        final long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > RequestCounter.warningThreshold) {
          RequestCounter.incrementWarningTimeExceededCounter();
        }
        if (logMethod) {
          final LogEntry logEntry = methodLogger.logExit(methodName, exception, connection != null ? connection.getLogEntries() : null);
          if (methodLogger.isEnabled()) {
            final StringBuilder messageBuilder = new StringBuilder(client.toString()).append("\n");
            appendLogEntries(messageBuilder, logEntry.getSubLog(), 1);
            LOG.debug(messageBuilder.toString());
          }
        }
        if (connection != null) {
          if (connection.isTransactionOpen()) {
            //keep this connection around until the transaction is closed
            connection.setLoggingEnabled(methodLogger.isEnabled());
            transactionDbConnection = connection;
          }
          else {
            transactionDbConnection = null;
            returnConnection(connection, logMethod);
          }
        }
      }
    }
  }

  private static void appendLogEntries(final StringBuilder log, final List<LogEntry> logEntries, final int indentation) {
    Collections.sort(logEntries);
    for (final LogEntry logEntry : logEntries) {
      log.append(logEntry.toString(indentation)).append("\n");
      final List<LogEntry> subLog = logEntry.getSubLog();
      if (subLog != null) {
        appendLogEntries(log, subLog, indentation + 1);
      }
    }
  }

  private static final class RemoteLogger extends MethodLogger {

    private RemoteLogger() {
      super(Configuration.getIntValue(Configuration.SERVER_CONNECTION_LOG_SIZE));
    }

    /** {@inheritDoc} */
    @Override
    protected String getMethodArgumentAsString(final Object argument) {
      if (argument == null) {
        return "";
      }

      final StringBuilder builder = new StringBuilder();
      if (argument instanceof EntityCriteria) {
        builder.append(appendEntityCriteria((EntityCriteria) argument));
      }
      else if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
        builder.append("[").append(argumentArrayToString((Object[]) argument)).append("]");
      }
      else if (argument instanceof Collection && !((Collection) argument).isEmpty()) {
        builder.append("[").append(argumentArrayToString(((Collection) argument).toArray())).append("]");
      }
      else if (argument instanceof Entity) {
        builder.append(getEntityParameterString((Entity) argument));
      }
      else {
        builder.append(argument.toString());
      }

      return builder.toString();
    }

    private String appendEntityCriteria(final EntityCriteria criteria) {
      final StringBuilder builder = new StringBuilder();
      builder.append(criteria.getEntityID());
      final String whereClause = criteria.getWhereClause(true);
      if (!Util.nullOrEmpty(whereClause)) {
        builder.append(", ").append(whereClause);
      }
      final List<?> values = criteria.getValues();
      if (values != null) {
        builder.append(", ").append(getMethodArgumentAsString(values));
      }

      return builder.toString();
    }

    private static String getEntityParameterString(final Entity entity) {
      final StringBuilder builder = new StringBuilder();
      builder.append(entity.getEntityID()).append(" {");
      for (final Property property : Entities.getColumnProperties(entity.getEntityID(), true, true, true)) {
        final boolean modified = entity.isModified(property.getPropertyID());
        if (property instanceof Property.PrimaryKeyProperty || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginalValue(property.getPropertyID())).append("->");
          }
          valueString.append(entity.getValue(property.getPropertyID()));
          builder.append(property.getPropertyID()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }
  }

  private static final class ConnectionProvider implements PoolableConnectionProvider {

    private final Database database;

    private ConnectionProvider(final Database database) {
      this.database = database;
    }

    /** {@inheritDoc} */
    public PoolableConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
      return createDbConnection(database, user);
    }

    /** {@inheritDoc} */
    public void destroyConnection(final PoolableConnection connection) {
      connection.disconnect();
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

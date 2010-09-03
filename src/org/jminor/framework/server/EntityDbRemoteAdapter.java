/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.db.pool.ConnectionPoolImpl;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.db.pool.PoolableConnectionProvider;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnection;
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
 * An implementation of the EntityDbRemote interface, provides the logging of service calls
 * and database connection pooling.
 */
final class EntityDbRemoteAdapter extends UnicastRemoteObject implements EntityDbRemote {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(EntityDbRemoteAdapter.class);
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
  private final EntityDb loggingEntityDbProxy;
  /**
   * The db connection used if connection pooling is not enabled
   */
  private EntityDbConnection entityDbConnection;
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
  private static final List<EntityDbRemoteAdapter> ACTIVE_CONNECTIONS = Collections.synchronizedList(new ArrayList<EntityDbRemoteAdapter>());
  /**
   * The available connection pools
   */
  private static final Map<User, ConnectionPool> CONNECTION_POOLS = Collections.synchronizedMap(new HashMap<User, ConnectionPool>());

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
   * Instantiates a new EntityDbRemoteAdapter and exports it on the given port number
   * @param database defines the underlying database
   * @param clientInfo information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @param sslEnabled specifies whether or not ssl should be enabled
   * @throws RemoteException in case of an exception
   */
  EntityDbRemoteAdapter(final Database database, final ClientInfo clientInfo, final int port,
                        final boolean loggingEnabled, final boolean sslEnabled) throws RemoteException {
    super(port, sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    if (CONNECTION_POOLS.containsKey(clientInfo.getUser())) {
      CONNECTION_POOLS.get(clientInfo.getUser()).getUser().setPassword(clientInfo.getUser().getPassword());
    }
    this.database = database;
    this.clientInfo = clientInfo;
    this.loggingEntityDbProxy = initializeProxy();
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
    return entityDbConnection == null ? connected : entityDbConnection.isConnected();
  }

  /** {@inheritDoc} */
  public void disconnect() throws RemoteException {
    if (!isConnected()) {
      return;
    }

    if (entityDbConnection != null) {
      entityDbConnection.disconnect();
    }
    entityDbConnection = null;
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
  public int selectRowCount(final EntityCriteria criteria) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectRowCount(criteria);
  }

  /** {@inheritDoc} */
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException, RemoteException, DbException {
    return loggingEntityDbProxy.fillReport(reportWrapper);
  }

  /** {@inheritDoc} */
  public void executeStatement(final String statement) throws DbException, RemoteException {
    loggingEntityDbProxy.executeStatement(statement);
  }

  /** {@inheritDoc} */
  public List<List> selectRows(final String statement, final int fetchCount) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectRows(statement, fetchCount);
  }

  /** {@inheritDoc} */
  public Object executeStatement(final String statement, final int outParameterType) throws DbException, RemoteException {
    return loggingEntityDbProxy.executeStatement(statement, outParameterType);
  }

  /** {@inheritDoc} */
  public boolean isValid() throws RemoteException {
    return loggingEntityDbProxy.isValid();
  }

  /** {@inheritDoc} */
  public void beginTransaction() throws RemoteException {
    loggingEntityDbProxy.beginTransaction();
  }

  /** {@inheritDoc} */
  public void commitTransaction() throws RemoteException {
    loggingEntityDbProxy.commitTransaction();
  }

  /** {@inheritDoc} */
  public void rollbackTransaction() throws RemoteException {
    loggingEntityDbProxy.rollbackTransaction();
  }

  /** {@inheritDoc} */
  public boolean isTransactionOpen() throws RemoteException {
    return loggingEntityDbProxy.isTransactionOpen();
  }

  /** {@inheritDoc} */
  public List<Entity.Key> insert(final List<Entity> entities) throws DbException, RemoteException {
    return loggingEntityDbProxy.insert(entities);
  }

  /** {@inheritDoc} */
  public List<Entity> update(final List<Entity> entities) throws DbException, RemoteException {
    return loggingEntityDbProxy.update(entities);
  }

  /** {@inheritDoc} */
  public void delete(final List<Entity.Key> entityKeys) throws DbException, RemoteException {
    loggingEntityDbProxy.delete(entityKeys);
  }

  /** {@inheritDoc} */
  public void delete(final EntityCriteria criteria) throws DbException, RemoteException {
    loggingEntityDbProxy.delete(criteria);
  }

  /** {@inheritDoc} */
  public List<Object> selectPropertyValues(final String entityID, final String propertyID,
                                           final boolean order) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectPropertyValues(entityID, propertyID, order);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectSingle(entityID, propertyID, value);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final Entity.Key key) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectSingle(key);
  }

  /** {@inheritDoc} */
  public Entity selectSingle(final EntitySelectCriteria criteria) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectSingle(criteria);
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectMany(keys);
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectMany(criteria);
  }

  /** {@inheritDoc} */
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectMany(entityID, propertyID, values);
  }

  /** {@inheritDoc} */
  public List<Entity> selectAll(final String entityID) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectAll(entityID);
  }

  /** {@inheritDoc} */
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DbException, RemoteException {
    return loggingEntityDbProxy.selectDependentEntities(entities);
  }

  /** {@inheritDoc} */
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final String dataDescription,
                        final byte[] blobData) throws DbException, RemoteException{
    loggingEntityDbProxy.writeBlob(primaryKey, blobPropertyID, dataDescription, blobData);
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DbException, RemoteException {
    return loggingEntityDbProxy.readBlob(primaryKey, blobPropertyID);
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

  private EntityDb initializeProxy() {
    return (EntityDb) Proxy.newProxyInstance(EntityDbConnection.class.getClassLoader(),
            EntityDbConnection.class.getInterfaces(), new LoggingInvocationHandler());
  }

  private EntityDbConnection getConnection() throws ClassNotFoundException, SQLException {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(clientInfo.getUser());
    if (connectionPool != null && connectionPool.isEnabled()) {
      if (entityDbConnection != null) {//pool has been turned on since this one was created
        entityDbConnection.disconnect();//discard
        entityDbConnection = null;
      }
      final EntityDbConnection pooledDbConnection = (EntityDbConnection) connectionPool.getConnection();
      if (methodLogger.isEnabled()) {
        pooledDbConnection.setLoggingEnabled(methodLogger.isEnabled());
      }

      return pooledDbConnection;
    }

    if (entityDbConnection == null) {
      entityDbConnection = createDbConnection(database, clientInfo.getUser());
    }
    else {
      if (!entityDbConnection.isValid()) {//dead connection
        LOG.debug("Removing an invalid database connection: " + entityDbConnection);
        entityDbConnection.disconnect();//just in case
        entityDbConnection = createDbConnection(database, clientInfo.getUser());
      }
    }
    entityDbConnection.setLoggingEnabled(methodLogger.isEnabled());

    return entityDbConnection;
  }

  private void returnConnection(final EntityDbConnection connection) {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(clientInfo.getUser());
    if (methodLogger.isEnabled()) {
      connection.setLoggingEnabled(false);
    }
    if (connectionPool != null && connectionPool.isEnabled()) {
      connectionPool.returnConnection(connection);
    }
  }

  private static EntityDbConnection createDbConnection(final Database database, final User user) throws ClassNotFoundException, SQLException {
    return new EntityDbConnection(database, user);
  }

  private static final String IS_CONNECTED = "isConnected";
  private static final String CONNECTION_VALID = "isValid";
  private static final String GET_ACTIVE_USER = "getActiveUser";

  private static boolean shouldMethodBeLogged(final String methodName) {
    return !(methodName.equals(IS_CONNECTED) || methodName.equals(CONNECTION_VALID) || methodName.equals(GET_ACTIVE_USER));
  }

  private final class LoggingInvocationHandler implements InvocationHandler {

    private static final String GET_CONNECTION = "getConnection";

    /** {@inheritDoc} */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      Exception exception = null;
      EntityDbConnection connection = null;
      final boolean logMethod = methodLogger.isEnabled() && shouldMethodBeLogged(methodName);
      final long startTime = System.currentTimeMillis();
      try {
        setActive();
        RequestCounter.incrementRequestsPerSecondCounter();
        connection = getConnection();
        if (logMethod) {
          logAccess(args, methodName, connection, startTime);
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
          logExit(methodName, exception, connection, currentTime);
        }
        if (connection != null) {
          returnConnection(connection);
        }
      }
    }

    private void logExit(final String methodName, final Exception exception, final EntityDbConnection connection,
                         final long timestamp) {
      methodLogger.logExit(methodName, exception, timestamp, connection != null ? connection.getLogEntries() : null);
    }

    private void logAccess(final Object[] args, final String methodName, final EntityDbConnection connection,
                           final long startTimestamp) {
      methodLogger.logAccess(GET_CONNECTION, new Object[]{clientInfo.getUser()}, startTimestamp);
      final int retries = connection.getRetryCount();
      final String message = retries > 0 ? "retries: " + retries : null;
      methodLogger.logExit(GET_CONNECTION, null, System.currentTimeMillis(), null, message);
      methodLogger.logAccess(methodName, args);
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

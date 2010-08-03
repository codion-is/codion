/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolImpl;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.db.pool.PoolableConnectionProvider;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnection;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
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
 * An adapter for handling logging and database connection pooling.
 */
public final class EntityDbRemoteAdapter extends UnicastRemoteObject implements EntityDbRemote {

  private static final Logger LOG = Util.getLogger(EntityDbRemoteAdapter.class);
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
  private final MethodLogger methodLogger;
  /**
   * Indicates whether or not a SSL client socket factory should be used when establishing the connection
   */
  private static final boolean SSL_CONNECTION_ENABLED = System.getProperty(Configuration.SERVER_CONNECTION_SSL_ENABLED, "true").equalsIgnoreCase("true");
  /**
   * Contains the active remote connections, that is, those connections that are in the middle of serving a request
   */
  private static final List<EntityDbRemoteAdapter> ACTIVE_CONNECTIONS = Collections.synchronizedList(new ArrayList<EntityDbRemoteAdapter>());
  /**
   * The available connection pools
   */
  private static final Map<User, ConnectionPool> CONNECTION_POOLS = Collections.synchronizedMap(new HashMap<User, ConnectionPool>());
  /**
   * The remote server responsible for instantiating this remote adapter
   */
  private final RemoteServer server;

  private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

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
   * @param server the RemoteServer instance responsible for instantiating this remote adapter
   * @param database defines the underlying database
   * @param clientInfo information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @throws RemoteException in case of an exception
   */
  public EntityDbRemoteAdapter(final RemoteServer server, final Database database, final ClientInfo clientInfo, final int port,
                               final boolean loggingEnabled) throws RemoteException {
    super(port, SSL_CONNECTION_ENABLED ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            SSL_CONNECTION_ENABLED ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    if (CONNECTION_POOLS.containsKey(clientInfo.getUser())) {
      CONNECTION_POOLS.get(clientInfo.getUser()).getUser().setPassword(clientInfo.getUser().getPassword());
    }
    this.server = server;
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

  public User getUser() throws RemoteException {
    return clientInfo.getUser();
  }

  public boolean isConnected() throws RemoteException {
    try {
      return entityDbConnection == null ? connected : entityDbConnection.isConnected();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void disconnect() throws RemoteException {
    try {
      if (!isConnected()) {
        return;
      }

      if (entityDbConnection != null) {
        entityDbConnection.disconnect();
      }
      entityDbConnection = null;
      connected = false;
      server.disconnect(clientInfo.getClientID());
      try {
        UnicastRemoteObject.unexportObject(this, true);
      }
      catch (NoSuchObjectException e) {
        LOG.error(e);
      }
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public int selectRowCount(final EntityCriteria criteria) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectRowCount(criteria);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException, RemoteException {
    try {
      return loggingEntityDbProxy.fillReport(reportWrapper);
    }
    catch (ReportException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void executeStatement(final String statement) throws DbException, RemoteException {
    try {
      loggingEntityDbProxy.executeStatement(statement);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<List> selectRows(final String statement, final int fetchCount) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectRows(statement, fetchCount);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public Object executeStatement(final String statement, final int outParameterType) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.executeStatement(statement, outParameterType);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public boolean isConnectionValid() throws RemoteException {
    try {
      return loggingEntityDbProxy.isConnectionValid();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void beginTransaction() throws RemoteException {
    try {
      loggingEntityDbProxy.beginTransaction();
    }
    catch (IllegalStateException is) {
      throw is;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void commitTransaction() throws SQLException, RemoteException {
    try {
      loggingEntityDbProxy.commitTransaction();
    }
    catch (IllegalStateException is) {
      throw is;
    }
    catch (SQLException exception) {
      throw exception;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void rollbackTransaction() throws SQLException, RemoteException {
    try {
      loggingEntityDbProxy.rollbackTransaction();
    }
    catch (IllegalStateException is) {
      throw is;
    }
    catch (SQLException exception) {
      throw exception;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public boolean isTransactionOpen() throws RemoteException {
    try {
      return loggingEntityDbProxy.isTransactionOpen();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Entity.Key> insert(final List<Entity> entities) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.insert(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Entity> update(final List<Entity> entities) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.update(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void delete(final List<Entity.Key> entityKeys) throws DbException, RemoteException {
    try {
      loggingEntityDbProxy.delete(entityKeys);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void delete(final EntityCriteria criteria) throws DbException, RemoteException {
    try {
      loggingEntityDbProxy.delete(criteria);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Object> selectPropertyValues(final String entityID, final String propertyID,
                                           final boolean order) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectPropertyValues(entityID, propertyID, order);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectSingle(entityID, propertyID, value);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public Entity selectSingle(final Entity.Key key) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectSingle(key);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public Entity selectSingle(final EntitySelectCriteria criteria) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectSingle(criteria);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Entity> selectMany(final List<Entity.Key> keys) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectMany(keys);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectMany(criteria);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectMany(entityID, propertyID, values);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public List<Entity> selectAll(final String entityID) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectAll(entityID);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectDependentEntities(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final String dataDescription,
                        final byte[] blobData) throws DbException, RemoteException{
    try {
      loggingEntityDbProxy.writeBlob(primaryKey, blobPropertyID, dataDescription, blobData);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.readBlob(primaryKey, blobPropertyID);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /**
   * @return information on the client using this remote connection
   */
  public ClientInfo getClientInfo() {
    return clientInfo;
  }

  /**
   * @return a ServerLog instance containing information about this connections recent activity
   * @see Configuration#SERVER_CONNECTION_LOG_SIZE
   */
  public ServerLog getServerLog() {
    return new ServerLog(clientInfo.getClientID(), creationDate, methodLogger.getLogEntries(),
            methodLogger.getLastAccessDate(), methodLogger.getLastExitDate(), methodLogger.getLastAccessedMethod(),
            methodLogger.getLastAccessMessage(), methodLogger.getLastExitedMethod());
  }

  /**
   * @return the object containing the method call log
   */
  public MethodLogger getMethodLogger() {
    return methodLogger;
  }

  /**
   * @param timeout the number of milliseconds
   * @return true if this connection has been inactive for <code>timeout</code> milliseconds or longer
   */
  public boolean hasBeenInactive(final int timeout) {
    return System.currentTimeMillis() - methodLogger.getLastAccessDate() > timeout;
  }

  /**
   * @return true during a remote method call
   */
  public boolean isActive() {
    return ACTIVE_CONNECTIONS.contains(this);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  public static int getActiveCount() {
    return ACTIVE_CONNECTIONS.size();
  }

  /**
   * @return a List containing the settings of the enabled connection pools
   */
  public static List<User> getEnabledConnectionPoolSettings() {
    final List<User> enabledPoolUsers = new ArrayList<User>();
    for (final ConnectionPool pool : CONNECTION_POOLS.values()) {
      if (pool.isEnabled()) {
        enabledPoolUsers.add(pool.getUser());
      }
    }

    return enabledPoolUsers;
  }

  public static boolean isConnectionPoolEnabled(final User user) throws RemoteException {
    return CONNECTION_POOLS.get(user).isEnabled();
  }

  public static void setConnectionPoolEnabled(final User user, final boolean enabled) throws RemoteException {
    CONNECTION_POOLS.get(user).setEnabled(enabled);
  }

  public static void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException {
    CONNECTION_POOLS.get(user).setPoolCleanupInterval(poolCleanupInterval);
  }

  public static int getConnectionPoolCleanupInterval(final User user) {
    return CONNECTION_POOLS.get(user).getPoolCleanupInterval();
  }

  public static int getMaximumConnectionPoolSize(final User user) {
    return CONNECTION_POOLS.get(user).getMaximumPoolSize();
  }

  public static int getMinimumConnectionPoolSize(final User user) {
    return CONNECTION_POOLS.get(user).getMinimumPoolSize();
  }

  public static int getPooledConnectionTimeout(final User user) {
    return CONNECTION_POOLS.get(user).getPooledConnectionTimeout();
  }

  public static void setMaximumConnectionPoolSize(final User user, final int value) {
    CONNECTION_POOLS.get(user).setMaximumPoolSize(value);
  }

  public static void setMinimumConnectionPoolSize(final User user, final int value) {
    CONNECTION_POOLS.get(user).setMinimumPoolSize(value);
  }

  public static void setPooledConnectionTimeout(final User user, final int timeout) {
    CONNECTION_POOLS.get(user).setPooledConnectionTimeout(timeout);
  }

  public static ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) {
    return CONNECTION_POOLS.get(user).getConnectionPoolStatistics(since);
  }

  public static void resetConnectionPoolStatistics(final User user) {
    CONNECTION_POOLS.get(user).resetPoolStatistics();
  }

  public static boolean isCollectFineGrainedPoolStatistics(final User user) {
    return CONNECTION_POOLS.get(user).isCollectFineGrainedStatistics();
  }

  public static void setCollectFineGrainedPoolStatistics(final User user, final boolean value) {
    CONNECTION_POOLS.get(user).setCollectFineGrainedStatistics(value);
  }

  public static int getRequestsPerSecond() {
    return RequestCounter.getRequestsPerSecond();
  }

  public static int getWarningTimeExceededPerSecond() {
    return RequestCounter.getWarningTimeExceededPerSecond();
  }

  public static int getWarningThreshold() {
    return RequestCounter.getWarningThreshold();
  }

  public static void setWarningThreshold(final int threshold) {
    RequestCounter.setWarningThreshold(threshold);
  }

  static void initConnectionPools(final Database database) {
    final String initialPoolUsers = System.getProperty(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    if (!Util.nullOrEmpty(initialPoolUsers)) {
      for (final String username : initialPoolUsers.split(",")) {
        final User poolUser = new User(username.trim(), null);
        CONNECTION_POOLS.put(poolUser, new ConnectionPoolImpl(new PoolableConnectionProvider() {
          public PoolableConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
            return EntityDbRemoteAdapter.createDbConnection(database, user);
          }
          public void destroyConnection(final PoolableConnection connection) {
            connection.disconnect();
          }
        }, poolUser));
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
            EntityDbConnection.class.getInterfaces(), new EntityDbRemoteProxy(this));
  }

  private void returnConnection(final User user, final EntityDbConnection connection) {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(user);
    connection.setLoggingEnabled(false);
    if (connectionPool != null && connectionPool.isEnabled()) {
      connectionPool.checkInConnection(connection);
    }
  }

  private EntityDbConnection getConnection(final User user) throws ClassNotFoundException, SQLException {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(user);
    if (connectionPool != null && connectionPool.isEnabled()) {
      final EntityDbConnection pooledDbConnection = (EntityDbConnection) connectionPool.checkOutConnection();
      if (entityDbConnection != null) {//pool has been turned on since this one was created
        entityDbConnection.disconnect();//discard
        entityDbConnection = null;
      }
      pooledDbConnection.setLoggingEnabled(methodLogger.isEnabled());

      return pooledDbConnection;
    }

    if (entityDbConnection == null) {
      entityDbConnection = createDbConnection(database, clientInfo.getUser());
    }
    entityDbConnection.setLoggingEnabled(methodLogger.isEnabled());

    return entityDbConnection;
  }

  private static EntityDbConnection createDbConnection(final Database database, final User user) throws ClassNotFoundException, SQLException {
    return new EntityDbConnection(database, user);
  }

  private static final String IS_CONNECTED = "isConnected";
  private static final String CONNECTION_VALID = "isConnectionValid";
  private static final String GET_ACTIVE_USER = "getActiveUser";

  private static boolean shouldMethodBeLogged(final String hashCode) {
    return !(hashCode.equals(IS_CONNECTED) || hashCode.equals(CONNECTION_VALID) || hashCode.equals(GET_ACTIVE_USER));
  }

  static class EntityDbRemoteProxy implements InvocationHandler {
    private final EntityDbRemoteAdapter remoteAdapter;

    EntityDbRemoteProxy(final EntityDbRemoteAdapter remoteAdapter) {
      this.remoteAdapter = remoteAdapter;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      RequestCounter.incrementRequestsPerSecondCounter();
      final String methodName = method.getName();
      Throwable ex = null;
      EntityDbConnection connection = null;
      final boolean logMethod = shouldMethodBeLogged(methodName);
      try {
        remoteAdapter.setActive();
        if (logMethod) {
          remoteAdapter.methodLogger.logAccess("getConnection", new Object[]{remoteAdapter.clientInfo.getUser()});
        }
        connection = remoteAdapter.getConnection(remoteAdapter.clientInfo.getUser());
        if (logMethod) {
          final int retries = connection.getPoolRetryCount();
          final String message = retries > 0 ? "retries: " + retries : null;
          remoteAdapter.methodLogger.logExit("getConnection", null, null, message);
          remoteAdapter.methodLogger.logAccess(methodName, args);
        }

        return method.invoke(connection, args);
      }
      catch (InvocationTargetException ie) {
        LOG.error(this, ie);
        ex = ie.getCause();
        throw (Exception) ie.getTargetException();
      }
      catch (Exception ie) {
        LOG.error(this, ie);
        ex = ie;
        throw ie;
      }
      finally {
        try {
          remoteAdapter.setInactive();
          if (logMethod) {
            final LogEntry entry = remoteAdapter.methodLogger.logExit(methodName, ex,
                    connection != null ? connection.getLogEntries() : null);
            if (entry != null && entry.getDelta() > RequestCounter.warningThreshold) {
              RequestCounter.incrementWarningTimeExceededCounter();
            }
          }
          if (connection != null && !connection.isTransactionOpen()) {
            remoteAdapter.returnConnection(remoteAdapter.clientInfo.getUser(), connection);
          }
        }
        catch (Exception e) {
          LOG.error(this, e);
        }
      }
    }
  }

  static class RemoteLogger extends MethodLogger {

    RemoteLogger() {
      super(Integer.parseInt(System.getProperty(Configuration.SERVER_CONNECTION_LOG_SIZE, "40")));
    }

    @Override
    protected void appendArgumentAsString(final Object argument, final StringBuilder destination) {
      if (argument == null) {
        return;
      }

      if (argument instanceof EntityCriteria) {
        appendEntityCriteria((EntityCriteria) argument, destination);
      }
      else if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
        destination.append("[").append(argumentArrayToString((Object[]) argument)).append("]");
      }
      else if (argument instanceof Collection && !((Collection) argument).isEmpty()) {
        destination.append("[").append(argumentArrayToString(((Collection) argument).toArray())).append("]");
      }
      else if (argument instanceof Entity) {
        destination.append(getEntityParameterString((Entity) argument));
      }
      else {
        destination.append(argument.toString());
      }
    }

    private void appendEntityCriteria(final EntityCriteria criteria, final StringBuilder destination) {
      destination.append(criteria.getEntityID());
      final String whereClause = criteria.getWhereClause(true);
      if (!Util.nullOrEmpty(whereClause)) {
        destination.append(", ").append(whereClause);
      }
      final List<?> values = criteria.getValues();
      if (values != null) {
        destination.append(", ");
        appendArgumentAsString(values, destination);
      }
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

    public static int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    public static int getWarningTimeExceededPerSecond() {
      return warningTimeExceededPerSecond;
    }

    public static int getWarningThreshold() {
      return warningThreshold;
    }

    public static void setWarningThreshold(final int warningThreshold) {
      RequestCounter.warningThreshold = warningThreshold;
    }

    public static void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter++;
    }

    public static void incrementWarningTimeExceededCounter() {
      warningTimeExceededCounter++;
    }
  }
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.ConnectionPools;
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
import org.jminor.framework.db.EntityConnections;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.awt.event.ActionListener;
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
   * The connection used if connection pooling is not enabled
   */
  private EntityConnection entityConnection;

  /**
   * Indicates whether or not this remote connection is connected when it is based on a connection pool,
   * otherwise checking the entityConnection is sufficient
   */
  private boolean connected = true;

  /**
   * The date and time when this remote connection was established
   */
  private final long creationDate = System.currentTimeMillis();

  /**
   * The method call log
   */
  private final transient MethodLogger methodLogger;

  /**
   * Contains the active remote connections, that is, those connections that are in the process of serving a request
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
      if (CONNECTION_POOLS.containsKey(clientInfo.getDatabaseUser())) {
        checkConnectionPoolCredentials(clientInfo.getDatabaseUser());
      }
      else {
        entityConnection = createDatabaseConnection(database, clientInfo.getDatabaseUser());
      }
      this.database = database;
      this.clientInfo = clientInfo;
      this.connectionProxy = initializeProxy();
      this.methodLogger = new RemoteLogger();
      this.methodLogger.setEnabled(loggingEnabled);
      this.logIdentifier = getUser().getUsername() +"@" + clientInfo.getClientTypeID();
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
  public boolean isConnected() throws RemoteException {
    return entityConnection == null ? connected : entityConnection.isConnected();
  }

  /** {@inheritDoc} */
  @Override
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
  @Override
  public int selectRowCount(final EntityCriteria criteria) throws DatabaseException, RemoteException {
    return connectionProxy.selectRowCount(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException, RemoteException, DatabaseException {
    return connectionProxy.fillReport(reportWrapper);
  }

  /** {@inheritDoc} */
  @Override
  public void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException {
    connectionProxy.executeProcedure(procedureID, arguments);
  }

  /** {@inheritDoc} */
  @Override
  public List<?> executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
    return connectionProxy.executeFunction(functionID, arguments);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValid() throws RemoteException {
    return connectionProxy.isValid();
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() throws RemoteException {
    connectionProxy.beginTransaction();
  }

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() throws RemoteException {
    connectionProxy.commitTransaction();
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() throws RemoteException {
    connectionProxy.rollbackTransaction();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() throws RemoteException {
    return connectionProxy.isTransactionOpen();
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException, RemoteException {
    return connectionProxy.insert(entities);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException, RemoteException {
    return connectionProxy.update(entities);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException, RemoteException {
    connectionProxy.delete(entityKeys);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCriteria criteria) throws DatabaseException, RemoteException {
    connectionProxy.delete(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectPropertyValues(final String entityID, final String propertyID,
                                           final boolean order) throws DatabaseException, RemoteException {
    return connectionProxy.selectPropertyValues(entityID, propertyID, order);
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException, RemoteException {
    return connectionProxy.selectSingle(entityID, propertyID, value);
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException, RemoteException {
    return connectionProxy.selectSingle(key);
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCriteria criteria) throws DatabaseException, RemoteException {
    return connectionProxy.selectSingle(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException, RemoteException {
    return connectionProxy.selectMany(keys);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCriteria criteria) throws DatabaseException, RemoteException {
    return connectionProxy.selectMany(criteria);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws DatabaseException, RemoteException {
    return connectionProxy.selectMany(entityID, propertyID, values);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectAll(final String entityID) throws DatabaseException, RemoteException {
    return connectionProxy.selectAll(entityID);
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException, RemoteException {
    return connectionProxy.selectDependentEntities(entities);
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final byte[] blobData) throws DatabaseException, RemoteException{
    connectionProxy.writeBlob(primaryKey, blobPropertyID, blobData);
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException, RemoteException {
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

  static void initializeConnectionPools(final Database database) throws ClassNotFoundException, DatabaseException {
    final String initialPoolUsers = Configuration.getStringValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    if (!Util.nullOrEmpty(initialPoolUsers)) {
      for (final String commaSplit : initialPoolUsers.split(",")) {
        final String usernamePassword = commaSplit.trim();
        final int splitIndex = usernamePassword.indexOf(':');
        if (splitIndex == -1) {
          throw new IllegalArgumentException("Username and password for pooled connection should be separated by ':', " + usernamePassword);
        }
        final String username = usernamePassword.substring(0, splitIndex);
        final String password = usernamePassword.substring(splitIndex + 1, usernamePassword.length());
        final User poolUser = new User(username, password);
        CONNECTION_POOLS.put(poolUser, ConnectionPools.createPool(new ConnectionProvider(database, poolUser)));
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
    return Util.initializeProxy(EntityConnection.class, new RemoteConnectionHandler(clientInfo));
  }

  private EntityConnection getConnection() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(clientInfo.getDatabaseUser());
    if (connectionPool != null && connectionPool.isEnabled()) {
      if (entityConnection != null) {//pool has been turned on since this one was created
        entityConnection.disconnect();//discard
        entityConnection = null;
      }
      final EntityConnection pooledConnection = (EntityConnection) connectionPool.getConnection();
      if (methodLogger.isEnabled()) {
        pooledConnection.getDatabaseConnection().setLoggingEnabled(methodLogger.isEnabled());
      }

      return pooledConnection;
    }

    if (entityConnection == null) {
      entityConnection = createDatabaseConnection(database, clientInfo.getDatabaseUser());
    }
    else {
      if (!entityConnection.isValid()) {//dead connection
        LOG.debug("Removing an invalid database connection: {}", entityConnection);
        entityConnection.disconnect();//just in case
        entityConnection = createDatabaseConnection(database, clientInfo.getDatabaseUser());
      }
    }
    entityConnection.getDatabaseConnection().setLoggingEnabled(methodLogger.isEnabled());

    return entityConnection;
  }

  private void returnConnection(final EntityConnection connection, final boolean logMethod) {
    final ConnectionPool connectionPool = CONNECTION_POOLS.get(clientInfo.getDatabaseUser());
    if (methodLogger.isEnabled()) {
      //we turned logging on when we fetched the connection, turn it off again
      connection.getDatabaseConnection().setLoggingEnabled(false);
    }
    if (connectionPool != null && connectionPool.isEnabled()) {
      try {
        if (logMethod) {
          methodLogger.logAccess(RETURN_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
        }
        connectionPool.returnConnection(connection.getDatabaseConnection());
      }
      finally {
        if (logMethod) {
          methodLogger.logExit(RETURN_CONNECTION, null, null);
        }
      }
    }
  }

  private static EntityConnection createDatabaseConnection(final Database database, final User user) throws ClassNotFoundException, DatabaseException {
    return EntityConnections.createConnection(database, user);
  }

  /**
   * Checks the credentials provided by <code>clientInfo</code> against the credentials
   * found in the connection pool
   * @param user the user credentials to check
   * @throws DatabaseException in case the password does not match the one in the pool
   */
  private static void checkConnectionPoolCredentials(final User user) throws DatabaseException {
    final User poolUser = CONNECTION_POOLS.get(user).getUser();
    if (!poolUser.getPassword().equals(user.getPassword())) {
      throw new DatabaseException("Wrong username or password for connection pool");
    }
  }

  private static final String IS_CONNECTED = "isConnected";
  private static final String CONNECTION_VALID = "isValid";
  private static final String GET_ACTIVE_USER = "getActiveUser";

  private static boolean shouldMethodBeLogged(final String methodName) {
    return !(methodName.equals(IS_CONNECTED) || methodName.equals(CONNECTION_VALID) || methodName.equals(GET_ACTIVE_USER));
  }

  private final class RemoteConnectionHandler implements InvocationHandler {

    private static final String GET_CONNECTION = "getConnection";
    private final ClientInfo client;

    private RemoteConnectionHandler(final ClientInfo client) {
      this.client = client;
    }

    /**
     * Holds the connection while a transaction is open, since it isn't proper to return one to the pool in such a state
     */
    private volatile EntityConnection transactionConnection;

    /** {@inheritDoc} */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      Exception exception = null;
      EntityConnection connection = transactionConnection;
      final boolean logMethod = methodLogger.isEnabled() && shouldMethodBeLogged(methodName);
      final long startTime = System.currentTimeMillis();
      try {
        MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
        setActive();
        RequestCounter.incrementRequestsPerSecondCounter();
        if (connection == null) {
          methodLogger.logAccess(GET_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
          Exception getConnectionException = null;
          try {
            connection = getConnection();
          }
          catch (Exception e) {
            getConnectionException = e;
            throw e;
          }
          finally {
            String message = null;
            if (connection != null && connection.getDatabaseConnection().getRetryCount() > 0) {
              message = "retries: " + connection.getDatabaseConnection().getRetryCount();
            }
            methodLogger.logExit(GET_CONNECTION, getConnectionException, null, message);
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
          final LogEntry logEntry = methodLogger.logExit(methodName, exception, connection != null ? connection.getDatabaseConnection().getLogEntries() : null);
          if (methodLogger.isEnabled()) {
            final StringBuilder messageBuilder = new StringBuilder(client.toString()).append("\n");
            appendLogEntries(messageBuilder, Arrays.asList(logEntry), 1);
            LOG.error(messageBuilder.toString());
          }
        }
        if (connection != null) {
          if (connection.isTransactionOpen()) {
            //keep this connection around until the transaction is closed, todo, implement a timeout perhaps?
            connection.getDatabaseConnection().setLoggingEnabled(methodLogger.isEnabled());
            transactionConnection = connection;
          }
          else {
            transactionConnection = null;
            returnConnection(connection, logMethod);
          }
        }
        MDC.remove(LOG_IDENTIFIER_PROPERTY);
      }
    }
  }

  private static void appendLogEntries(final StringBuilder log, final List<LogEntry> logEntries, final int indentation) {
    if (logEntries != null && !logEntries.isEmpty()) {
      Collections.sort(logEntries);
      for (final LogEntry logEntry : logEntries) {
        log.append(logEntry.toString(indentation)).append("\n");
        final List<LogEntry> subLog = logEntry.getSubLog();
        if (subLog != null) {
          appendLogEntries(log, subLog, indentation + 1);
        }
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

  private static final class ConnectionProvider implements DatabaseConnectionProvider {

    private final Database database;
    private final User user;

    private ConnectionProvider(final Database database, final User user) {
      this.database = database;
      this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public DatabaseConnection createConnection() throws ClassNotFoundException, DatabaseException {
      return createDatabaseConnection(database, user).getDatabaseConnection();
    }

    /** {@inheritDoc} */
    @Override
    public void destroyConnection(final DatabaseConnection connection) {
      connection.disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public User getUser() {
      return user;
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

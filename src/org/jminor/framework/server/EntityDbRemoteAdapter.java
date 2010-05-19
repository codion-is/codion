/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbConnectionProvider;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolSettings;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.DbConnectionPool;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnection;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
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
public class EntityDbRemoteAdapter extends UnicastRemoteObject implements EntityDbRemote {

  private static final Logger log = Util.getLogger(EntityDbRemoteAdapter.class);
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
  private static final List<EntityDbRemoteAdapter> active = Collections.synchronizedList(new ArrayList<EntityDbRemoteAdapter>());
  /**
   * The available connection pools
   */
  private static final Map<User, ConnectionPool> connectionPools = Collections.synchronizedMap(new HashMap<User, ConnectionPool>());
  /**
   * The remote server responsible for instantiating this remote adapter
   */
  private final RemoteServer server;

  static {
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        RequestCounter.updateRequestsPerSecond();
      }
    }, new Date(), 2500);
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
    if (connectionPools.containsKey(clientInfo.getUser()))
      connectionPools.get(clientInfo.getUser()).getConnectionPoolSettings().getUser().setPassword(clientInfo.getUser().getPassword());
    this.server = server;
    this.database = database;
    this.clientInfo = clientInfo;
    this.loggingEntityDbProxy = initializeProxy();
    this.methodLogger = new RemoteLogger();
    this.methodLogger.setEnabled(loggingEnabled);
    try {
      clientInfo.setClientHost(getClientHost());
    }
    catch (ServerNotActiveException e) {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} */
  public User getUser() throws RemoteException {
    return clientInfo.getUser();
  }

  /** {@inheritDoc} */
  public boolean isConnected() throws RemoteException {
    try {
      return entityDbConnection == null ? connected : entityDbConnection.isConnected();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public void disconnect() throws RemoteException {
    try {
      if (!isConnected())
        return;

      if (entityDbConnection != null)
        entityDbConnection.disconnect();
      entityDbConnection = null;
      connected = false;
      server.disconnect(clientInfo.getClientID());
      try {
        UnicastRemoteObject.unexportObject(this, true);
      }
      catch (NoSuchObjectException e) {
        log.error(e);
      }
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public JasperPrint fillReport(final JasperReport report, final Map reportParameters) throws JRException, RemoteException {
    try {
      return loggingEntityDbProxy.fillReport(report, reportParameters);
    }
    catch (JRException jre) {
      throw jre;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public boolean isConnectionValid() throws RemoteException {
    try {
      return loggingEntityDbProxy.isConnectionValid();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public void beginTransaction() throws IllegalStateException, RemoteException {
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

  /** {@inheritDoc} */
  public void commitTransaction() throws IllegalStateException, SQLException, RemoteException {
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

  /** {@inheritDoc} */
  public void rollbackTransaction() throws IllegalStateException, SQLException, RemoteException {
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

  /** {@inheritDoc} */
  public boolean isTransactionOpen() throws RemoteException {
    try {
      return loggingEntityDbProxy.isTransactionOpen();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  public Map<String, List<Entity>> selectDependentEntities(final List<Entity> entities) throws DbException, RemoteException {
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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
    return new ServerLog(getClientInfo().getClientID(), creationDate, methodLogger.getLogEntries(),
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
  public boolean hasBeenInactive(final long timeout) {
    return System.currentTimeMillis() - methodLogger.getLastAccessDate() > timeout;
  }

  /**
   * @return true during a remote method call
   */
  public boolean isActive() {
    return active.contains(this);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  public static int getActiveCount() {
    return active.size();
  }

  /**
   * @return a List containing the settings of the enabled connection pools
   */
  public static List<ConnectionPoolSettings> getEnabledConnectionPoolSettings() {
    final List<ConnectionPoolSettings> poolSettings = new ArrayList<ConnectionPoolSettings>();
    for (final ConnectionPool pool : connectionPools.values()) {
      if (pool.getConnectionPoolSettings().isEnabled())
        poolSettings.add(pool.getConnectionPoolSettings());
    }

    return poolSettings;
  }

  public static ConnectionPoolSettings getConnectionPoolSettings(final User user) {
    return connectionPools.get(user).getConnectionPoolSettings();
  }

  public static void setConnectionPoolSettings(final Database database, final ConnectionPoolSettings settings) {
    ConnectionPool pool = connectionPools.get(settings.getUser());
    if (pool == null) {
      connectionPools.put(settings.getUser(), new DbConnectionPool(new DbConnectionProvider() {
        public DbConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
          return EntityDbRemoteAdapter.createDbConnection(database, user);
        }
        public void destroyConnection(final DbConnection connection) {
          connection.disconnect();
        }
      }, settings));
    }
    else
      pool.setConnectionPoolSettings(settings);
  }

  public static ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) {
    return connectionPools.get(user).getConnectionPoolStatistics(since);
  }

  public static void resetConnectionPoolStatistics(final User user) {
    connectionPools.get(user).resetPoolStatistics();
  }

  public static boolean isCollectFineGrainedPoolStatistics(final User user) {
    return connectionPools.get(user).isCollectFineGrainedStatistics();
  }

  public static void setCollectFineGrainedPoolStatistics(final User user, final boolean value) {
    connectionPools.get(user).setCollectFineGrainedStatistics(value);
  }

  public static int getRequestsPerSecond() {
    return RequestCounter.requestsPerSecond;
  }

  public static int getWarningTimeExceededPerSecond() {
    return RequestCounter.warningTimeExceededPerSecond;
  }

  public static int getWarningThreshold() {
    return RequestCounter.warningThreshold;
  }

  public static void setWarningThreshold(final int threshold) {
    RequestCounter.warningThreshold = threshold;
  }

  static void initConnectionPools(final Database database) {
    final String initialPoolUsers = System.getProperty(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    if (initialPoolUsers != null && initialPoolUsers.length() > 0) {
      for (final String username : initialPoolUsers.split(",")) {
        final User user = new User(username.trim(), null);
        setConnectionPoolSettings(database, ConnectionPoolSettings.getDefault(user));
      }
    }
  }

  private void setActive() {
    active.add(this);
  }

  private void setInactive() {
    active.remove(this);
  }

  private EntityDb initializeProxy() {
    return (EntityDb) Proxy.newProxyInstance(EntityDbConnection.class.getClassLoader(),
            EntityDbConnection.class.getInterfaces(), new EntityDbRemoteProxy(this));
  }

  private void returnConnection(final User user, final EntityDbConnection connection) {
    final ConnectionPool connectionPool = connectionPools.get(user);
    if (connectionPool != null && connectionPool.getConnectionPoolSettings().isEnabled()) {
      connection.setLoggingEnabled(false);
      connectionPool.checkInConnection(connection);
    }
  }

  private EntityDbConnection getConnection(final User user) throws ClassNotFoundException, SQLException {
    final ConnectionPool connectionPool = connectionPools.get(user);
    if (connectionPool != null && connectionPool.getConnectionPoolSettings().isEnabled()) {
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
      entityDbConnection.setLoggingEnabled(methodLogger.isEnabled());
    }

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

    public EntityDbRemoteProxy(final EntityDbRemoteAdapter remoteAdapter) {
      this.remoteAdapter = remoteAdapter;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
      RequestCounter.requestsPerSecondCounter++;
      final String methodName = method.getName();
      Throwable ex = null;
      EntityDbConnection connection = null;
      final boolean logMethod = shouldMethodBeLogged(methodName);
      try {
        remoteAdapter.setActive();
        if (logMethod)
          remoteAdapter.methodLogger.logAccess("getConnection", new Object[] {remoteAdapter.clientInfo.getUser()});
        connection = remoteAdapter.getConnection(remoteAdapter.clientInfo.getUser());
        if (logMethod) {
          remoteAdapter.methodLogger.logExit("getConnection", null, null);
          remoteAdapter.methodLogger.logAccess(methodName, arguments);
        }

        return method.invoke(connection, arguments);
      }
      catch (InvocationTargetException ie) {
        log.error(this, ie);
        ex = ie.getCause();
        throw ie.getTargetException();
      }
      finally {
        try {
          remoteAdapter.setInactive();
          if (logMethod) {
            final long time = remoteAdapter.methodLogger.logExit(methodName, ex,
                    connection != null ? connection.getLogEntries() : null);
            if (time > RequestCounter.warningThreshold)
              RequestCounter.warningTimeExceededCounter++;
          }
          if (connection != null && !connection.isTransactionOpen())
            remoteAdapter.returnConnection(remoteAdapter.clientInfo.getUser(), connection);
        }
        catch (Exception e) {
          log.error(this, e);
        }
      }
    }
  }

  static class RemoteLogger extends MethodLogger {

    public RemoteLogger() {
      super(Integer.parseInt(System.getProperty(Configuration.SERVER_CONNECTION_LOG_SIZE, "40")));
    }

    @Override
    protected String parameterArrayToString(final Object[] arguments) {
      if (arguments == null)
        return "";

      final StringBuilder stringBuilder = new StringBuilder(arguments.length*42);
      for (int i = 0; i < arguments.length; i++) {
        parameterToString(arguments[i], stringBuilder);
        if (i < arguments.length-1)
          stringBuilder.append(", ");
      }

      return stringBuilder.toString();
    }

    private void parameterToString(final Object arg, final StringBuilder destination) {
      if (arg == null)
        return;

      if (arg instanceof EntityCriteria)
        appendEntityCriteria((EntityCriteria) arg, destination);
      else if (arg instanceof Object[] && ((Object[]) arg).length > 0)
        destination.append("[").append(parameterArrayToString((Object[]) arg)).append("]");
      else if (arg instanceof Collection && ((Collection) arg).size() > 0)
        destination.append("[").append(parameterArrayToString(((Collection) arg).toArray())).append("]");
      else if (arg instanceof Entity)
        destination.append(getEntityParameterString((Entity) arg));
      else if (arg instanceof JasperReport)
        destination.append(((JasperReport) arg).getName());
      else
        destination.append(arg.toString());
    }

    private void appendEntityCriteria(final EntityCriteria criteria, StringBuilder destination) {
      destination.append(criteria.getEntityID());
      final String whereClause = criteria.getWhereClause(true);
      if (whereClause != null && whereClause.length() > 0) {
        destination.append(", ").append(whereClause);
      }
      final List<?> values = criteria.getValues();
      if (values != null) {
        destination.append(", ");
        parameterToString(values, destination);
      }
    }

    private static String getEntityParameterString(final Entity entity) {
      final StringBuilder builder = new StringBuilder();
      builder.append(entity.getEntityID()).append(" {");
      for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, true, true)) {
        final boolean modified = entity.isModified(property.getPropertyID());
        if (property instanceof Property.PrimaryKeyProperty || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified)
            valueString.append(entity.getOriginalValue(property.getPropertyID())).append("->");
          valueString.append(entity.getValue(property.getPropertyID()));
          builder.append(property.getPropertyID()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }
  }

  private static class RequestCounter {
    static long requestsPerSecondTime = System.currentTimeMillis();
    static int requestsPerSecond = 0;
    static int requestsPerSecondCounter = 0;
    static int warningThreshold = 60;
    static int warningTimeExceededPerSecond = 0;
    static int warningTimeExceededCounter = 0;

    static void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - requestsPerSecondTime)/1000;
      if (seconds > 0) {
        requestsPerSecond = (int) ((double) requestsPerSecondCounter/seconds);
        warningTimeExceededPerSecond = (int) ((double) warningTimeExceededCounter/seconds);
        warningTimeExceededCounter = 0;
        requestsPerSecondCounter = 0;
        requestsPerSecondTime = current;
      }
    }
  }
}

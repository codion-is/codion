/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.AuthenticationException;
import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolStatistics;
import org.jminor.common.db.DbException;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Dbms;
import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.common.server.ServerLogEntry;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnection;
import org.jminor.framework.db.EntityDbConnectionPool;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.domain.Entity;

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
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An adapter for handling logging and database connection pooling
 */
public class EntityDbRemoteAdapter extends UnicastRemoteObject implements EntityDbRemote {

  private static final Logger log = Util.getLogger(EntityDbRemoteAdapter.class);

  public final Event evtLoggingOut = new Event();

  private final ClientInfo clientInfo;
  private final Dbms database;
  private final long creationDate = System.currentTimeMillis();
  private final EntityDb loggingEntityDbProxy;
  private EntityDbConnection entityDbConnection;
  private boolean connected = true;

  private final MethodLogger methodLogger = new MethodLogger();
  private static final boolean useSecureConnection = Integer.parseInt(System.getProperty(Configuration.SERVER_SECURE_CONNECTION, "1")) == 1;
  private static final List<EntityDbRemoteAdapter> active = Collections.synchronizedList(new ArrayList<EntityDbRemoteAdapter>());
  private static final Map<User, EntityDbConnectionPool> connectionPools =
          Collections.synchronizedMap(new HashMap<User, EntityDbConnectionPool>());

  static {
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        RequestCounter.updateRequestsPerSecond();
      }
    }, new Date(), 15000);
  }

  public EntityDbRemoteAdapter(final Dbms database, final ClientInfo clientInfo, final int dbRemotePort,
                               final boolean loggingEnabled) throws RemoteException {
    super(dbRemotePort, useSecureConnection ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            useSecureConnection ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    if (connectionPools.containsKey(clientInfo.getUser()))
      connectionPools.get(clientInfo.getUser()).setPassword(clientInfo.getUser().getPassword());
    this.database = database;
    this.clientInfo = clientInfo;
    final String sid = System.getProperty(Dbms.DATABASE_SID);
    if (sid != null && sid.length() != 0)
      this.clientInfo.getUser().setProperty(Dbms.DATABASE_SID, sid);
    this.loggingEntityDbProxy = initializeProxy();
    this.methodLogger.setLoggingEnabled(loggingEnabled);
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
      if (entityDbConnection != null)
        entityDbConnection.disconnect();

      entityDbConnection = null;
      connected = false;

      UnicastRemoteObject.unexportObject(this, true);
      evtLoggingOut.fire();
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
  public JasperPrint fillReport(final JasperReport report, final Map reportParams) throws JRException, RemoteException {
    try {
      return loggingEntityDbProxy.fillReport(report, reportParams);
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
  public Object executeStatement(final String statement, final int outParamType) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.executeStatement(statement, outParamType);
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
  public void endTransaction(final boolean commit) throws SQLException, RemoteException {
    try {
      loggingEntityDbProxy.endTransaction(commit);
    }
    catch (SQLException sqle) {
      throw sqle;
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
  public List<?> selectPropertyValues(final String entityID, final String propertyID,
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
  public Entity selectSingle(final EntityCriteria criteria) throws DbException, RemoteException {
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
  public List<Entity> selectMany(final EntityCriteria criteria) throws DbException, RemoteException {
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
  public Entity writeBlob(final Entity entity, final String propertyID, final byte[] blobData) throws DbException, RemoteException{
    try {
      return loggingEntityDbProxy.writeBlob(entity, propertyID, blobData);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public byte[] readBlob(final Entity entity, final String propertyID) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.readBlob(entity, propertyID);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  public ClientInfo getClientInfo() {
    return clientInfo;
  }

  public ServerLog getServerLog() {
    return new ServerLog(getClientInfo().getClientID(), creationDate, methodLogger.getLogEntries(),
            methodLogger.lastAccessDate, methodLogger.lastExitDate,methodLogger.lastAccessedMethod,
            methodLogger.lastAccessMessage, methodLogger.lastExitedMethod);
  }

  public MethodLogger getMethodLogger() {
    return methodLogger;
  }

  /**
   * @param timout the number of milliseconds
   * @return true if this connection has been inactive for <code>timeout</code> milliseconds or longer
   */
  public boolean hasBeenInactive(final long timout) {
    return System.currentTimeMillis() - methodLogger.lastAccessDate > timout;
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

  public static List<ConnectionPoolSettings> getActiveConnectionPoolSettings() {
    final List<ConnectionPoolSettings> poolSettings = new ArrayList<ConnectionPoolSettings>();
    for (final EntityDbConnectionPool pool : connectionPools.values()) {
      if (pool.getConnectionPoolSettings().isEnabled())
        poolSettings.add(pool.getConnectionPoolSettings());
    }

    return poolSettings;
  }

  public static ConnectionPoolSettings getConnectionPoolSettings(final User user) {
    return connectionPools.get(user).getConnectionPoolSettings();
  }

  public static void setConnectionPoolSettings(final Dbms database, final User user, final ConnectionPoolSettings settings) {
    EntityDbConnectionPool pool = connectionPools.get(user);
    if (pool == null)
      connectionPools.put(user, new EntityDbConnectionPool(database, user, settings));
    else
      pool.setConnectionPoolSettings(settings);
  }

  public static ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) {
    return connectionPools.get(user).getConnectionPoolStatistics(since);
  }

  public static void resetConnectionPoolStatistics(final User user) {
    connectionPools.get(user).resetPoolStatistics();
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

  static void initConnectionPools(final Dbms database) {
    final String initialPoolUsers = System.getProperty(Configuration.SERVER_POOLING_INITIAL);
    if (initialPoolUsers != null && initialPoolUsers.length() > 0) {
      for (final String username : initialPoolUsers.split(",")) {
        final User user = new User(username, null);
        setConnectionPoolSettings(database, user, ConnectionPoolSettings.getDefault(user));
      }
    }
  }

  private EntityDb initializeProxy() {
    return (EntityDb) Proxy.newProxyInstance(EntityDbConnection.class.getClassLoader(),
            EntityDbConnection.class.getInterfaces(), new InvocationHandler() {
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        RequestCounter.requestsPerSecondCounter++;
        final String methodName = method.getName();
        EntityDbConnection connection = null;
        try {
          active.add(EntityDbRemoteAdapter.this);
          methodLogger.logAccess(methodName, args);

          return method.invoke(connection = getConnection(clientInfo.getUser()), args);
        }
        catch (InvocationTargetException ie) {
          log.error(this, ie);
          throw ie.getTargetException();
        }
        finally {
          try {
            active.remove(EntityDbRemoteAdapter.this);
            final long time = methodLogger.logExit(methodName);
            if (connection != null && !connection.isTransactionOpen())
              returnConnection(clientInfo.getUser(), connection);
            if (time > RequestCounter.warningThreshold)
              RequestCounter.warningTimeExceededCounter++;
          }
          catch (Exception e) {
            log.error(this, e);
          }
        }
      }
    });
  }

  private void returnConnection(final User user, final EntityDbConnection connection) {
    if (connectionPools.containsKey(user) && connectionPools.get(user).getConnectionPoolSettings().isEnabled())
      connectionPools.get(user).checkInConnection(connection);
  }

  private EntityDbConnection getConnection(final User user) throws ClassNotFoundException, AuthenticationException {
    if (connectionPools.containsKey(user) && connectionPools.get(user).getConnectionPoolSettings().isEnabled()) {
      final EntityDbConnection dbConnection = connectionPools.get(user).checkOutConnection();
      if (dbConnection != null) {
        if (entityDbConnection != null) {//pool has been turned on since this one was created
          entityDbConnection.disconnect();//discard
          entityDbConnection = null;
        }

        return dbConnection;
      }
    }

    if (entityDbConnection == null)
      entityDbConnection = new EntityDbConnection(database, clientInfo.getUser());

    return entityDbConnection;
  }

  private static String parameterArrayToString(final Object[] args) {
    if (args == null)
      return "";

    final StringBuilder stringBuilder = new StringBuilder(args.length*40);//best guess ?
    for (int i = 0; i < args.length; i++) {
      parameterToString(args[i], stringBuilder);
      if (i < args.length-1)
        stringBuilder.append(", ");
    }

    return stringBuilder.toString();
  }

  private static void parameterToString(final Object arg, final StringBuilder dest) {
    if (arg instanceof Object[]) {
      if (((Object[]) arg).length > 0)
        dest.append("[").append(parameterArrayToString((Object[]) arg)).append("]");
    }
    else
      dest.append(arg.toString());
  }

  static class MethodLogger {

    private static final int logSize = Integer.parseInt(System.getProperty(Configuration.SERVER_CONNECTION_LOG_SIZE, "40"));
    private static final int IS_CONNECTED = "isConnected".hashCode();
    private static final int CONNECTION_VALID = "isConnectionValid".hashCode();
    private static final int GET_ACTIVE_USER = "getActiveUser".hashCode();

    private boolean loggingEnabled = false;
    private List<ServerLogEntry> logEntries;
    private int currentLogEntryIndex = 0;

    long lastAccessDate = System.currentTimeMillis();
    long lastExitDate = System.currentTimeMillis();
    String lastAccessedMethod;
    String lastAccessMessage;
    String lastExitedMethod;

    public List<ServerLogEntry> getLogEntries() {
      return new ArrayList<ServerLogEntry>(logEntries);
    }

    public void logAccess(final String method, final Object[] args) {
      this.lastAccessDate = System.currentTimeMillis();
      this.lastAccessedMethod = method;
      if (loggingEnabled && shouldMethodBeLogged(lastAccessedMethod.hashCode())) {
        this.lastAccessMessage = parameterArrayToString(args);
        addLogEntry(lastAccessedMethod, lastAccessMessage, lastAccessDate, false);
      }
    }

    public long logExit(final String method) {
      this.lastExitDate = System.currentTimeMillis();
      this.lastExitedMethod = method;
      if (loggingEnabled && shouldMethodBeLogged(lastExitedMethod.hashCode()))
        return addLogEntry(lastExitedMethod, lastAccessMessage, lastExitDate, true);

      return -1;
    }

    public boolean isLoggingEnabled() {
      return loggingEnabled;
    }

    public void setLoggingEnabled(final boolean loggingEnabled) {
      this.loggingEnabled = loggingEnabled;
      if (loggingEnabled)
        logEntries = initializeLogEntryList();
      else {
        logEntries = null;
        currentLogEntryIndex = 0;
      }
    }

    private boolean shouldMethodBeLogged(final int hashCode) {
      return hashCode != IS_CONNECTED && hashCode != CONNECTION_VALID && hashCode != GET_ACTIVE_USER;
    }

    private long addLogEntry(final String method, final String message, final long time, final boolean isExit) {
      if (!isExit) {
        if (currentLogEntryIndex > logEntries.size()-1)
          currentLogEntryIndex = 0;

        logEntries.get(currentLogEntryIndex).set(method, message, time);

        return -1;
      }
      else {//add to last log entry
        final ServerLogEntry lastEntry = logEntries.get(currentLogEntryIndex);
        assert lastEntry.method.equals(method);
        currentLogEntryIndex++;

        return lastEntry.setExitTime(time);
      }
    }

    private static List<ServerLogEntry> initializeLogEntryList() {
      final List<ServerLogEntry> logEntries = new ArrayList<ServerLogEntry>(logSize);
      for (int i = 0; i < logSize; i++)
        logEntries.add(new ServerLogEntry());

      return logEntries;
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
      if (seconds > 5) {
        requestsPerSecond = (int) ((double) requestsPerSecondCounter/seconds);
        warningTimeExceededPerSecond = (int) ((double) warningTimeExceededCounter/seconds);
        warningTimeExceededCounter = 0;
        requestsPerSecondCounter = 0;
        requestsPerSecondTime = current;
      }
    }
  }
}

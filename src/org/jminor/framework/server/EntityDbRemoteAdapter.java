/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.AuthenticationException;
import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.Database;
import org.jminor.common.db.DbException;
import org.jminor.common.db.DbLog;
import org.jminor.common.db.LogEntry;
import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.remote.RemoteClient;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbConnection;
import org.jminor.framework.db.EntityDbConnectionPool;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityCriteria;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;

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
public class EntityDbRemoteAdapter extends UnicastRemoteObject implements IEntityDbRemote {

  private static final Logger logger = Util.getLogger(EntityDbRemoteAdapter.class);

  public final Event evtLoggingOut = new Event("EntityDbRemoteAdapter.evtLoggingOut");

  private RemoteClient client;
  private final long creationDate = System.currentTimeMillis();

  private static final int logSize =
          Integer.parseInt(System.getProperty(FrameworkConstants.SERVER_CONNECTION_LOG_SIZE, "40"));
  private static final boolean useSecureConnection =
          Integer.parseInt(System.getProperty(FrameworkConstants.SERVER_SECURE_CONNECTION, "1")) == 1;
  private static final List<EntityDbRemoteAdapter> active = Collections.synchronizedList(new ArrayList<EntityDbRemoteAdapter>());

  private final IEntityDb loggingEntityDbProxy;
  private boolean loggingEnabled = false;
  private List<LogEntry> log;
  private int logIdx = 0;

  private long lastAccessDate = System.currentTimeMillis();
  private long lastExitDate = System.currentTimeMillis();//not quite logical
  private String lastAccessedMethod;
  private String lastAccessMessage;
  private String lastExitedMethod;

  private final EntityRepository repository;
  private final FrameworkSettings settings;
  private EntityDbConnection entityDbConnection;

  private final static Map<User, EntityDbConnectionPool> connectionPools =
          Collections.synchronizedMap(new HashMap<User, EntityDbConnectionPool>());
  private boolean loggedIn = true;
  private static long requestsPerSecondTime = System.currentTimeMillis();
  private static int requestsPerSecond = 0;
  private static int requestsPerSecondCounter = 0;
  private static int warningThreshold = 60;
  private static int warningTimeExceededPerSecond = 0;
  private static int warningTimeExceededCounter = 0;

  private static final int IS_LOGGED_IN = "isLoggedIn".hashCode();
  private static final int CONNECTION_VALID = "isConnectionValid".hashCode();
  private static final int GET_ACTIVE_USER = "getActiveUser".hashCode();

  static {
    final String initialPoolUsers = System.getProperty(FrameworkConstants.SERVER_POOLING_INITIAL);
    if (initialPoolUsers != null && initialPoolUsers.length() > 0) {
      for (final String username : initialPoolUsers.split(",")) {
        final User user = new User(username, null);
        setConnectionPoolSettings(user, ConnectionPoolSettings.getDefault(user));
      }
    }
    new Timer(true).schedule(new TimerTask() {
      public void run() {
        updateRequestsPerSecond();
      }
    }, new Date(), 15000);
  }

  public EntityDbRemoteAdapter(final RemoteClient client, final EntityRepository repository,
                               final FrameworkSettings settings, final int dbRemotePort,
                               final boolean loggingEnabled) throws RemoteException {
    super(dbRemotePort, useSecureConnection ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            useSecureConnection ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    if (connectionPools.containsKey(client.getUser()))
      connectionPools.get(client.getUser()).setPassword(client.getUser().getPassword());
    this.client = client;
    this.client.getUser().setProperty(Database.DATABASE_SID_PROPERTY,
            System.getProperty(Database.DATABASE_SID_PROPERTY));
    this.repository = repository;
    this.settings = settings;
    this.loggingEntityDbProxy = initializeProxy();
    this.loggingEnabled = loggingEnabled;
  }

  public static ConnectionPoolSettings getConnectionPoolSettings(final User user, final long includeStatsSince) {
    return connectionPools.get(user).getConnectionPoolSettings(true, includeStatsSince);
  }

  public static void setConnectionPoolSettings(final User user, final ConnectionPoolSettings settings) {
    EntityDbConnectionPool pool = connectionPools.get(user);
    if (pool == null)
      connectionPools.put(user, new EntityDbConnectionPool(user, settings));
    else
      pool.setConnectionPoolSettings(settings);
  }

  public static int getRequestsPerSecond() {
    return requestsPerSecond;
  }

  public static int getWarningTimeExceededPerSecond() {
    return warningTimeExceededPerSecond;
  }

  public boolean isWorking() {
    return active.contains(this);
  }

  public DbLog getEntityDbLog() {
    return new DbLog(getClient().getClientID(), creationDate, log, lastAccessDate, lastExitDate,
            lastAccessedMethod, lastAccessMessage, lastExitedMethod);
  }

  public RemoteClient getClient() {
    return client;
  }

  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public void setLoggingEnabled(final boolean logginEnabled) {
    this.loggingEnabled = logginEnabled;
    if (!logginEnabled)
      log.clear();
  }

  public boolean hasBeenInactive(final long timout) {
    return System.currentTimeMillis() - lastAccessDate > timout;
  }

  public static List<ConnectionPoolSettings> getActiveConnectionPoolSettings() {
    final List<ConnectionPoolSettings> ret = new ArrayList<ConnectionPoolSettings>();
    for (final EntityDbConnectionPool pool : connectionPools.values()) {
      if (pool.getConnectionPoolSettings().isEnabled())
        ret.add(pool.getConnectionPoolSettings());
    }

    return ret;
  }

  public static void resetConnectionPoolStats(final User user) {
    connectionPools.get(user).resetPoolStatistics();
  }

  /**
   * Only returns a valid value when logging is enabled
   * @return Value for property 'activeCount'.
   */
  public static int getActiveCount() {
    return active.size();
  }

  public static int getWarningThreshold() {
    return warningThreshold;
  }

  public static void setWarningThreshold(final int threshold) {
    warningThreshold = threshold;
  }

  /** {@inheritDoc} */
  public User getUser() throws RemoteException {
    return client.getUser();
  }

  /** {@inheritDoc} */
  public boolean isConnected() throws RemoteException {
    try {
      return entityDbConnection == null ? loggedIn : entityDbConnection.isConnected();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public void logout() throws RemoteException {
    try {
      if (entityDbConnection != null)
        entityDbConnection.logout();

      loggedIn = false;

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
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public Object executeCallable(final String statement, final int outParamType) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.executeCallable(statement, outParamType);
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
  public void startTransaction() throws IllegalStateException, RemoteException {
    try {
      loggingEntityDbProxy.startTransaction();
    }
    catch (IllegalStateException is) {
      throw is;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public void endTransaction(final boolean rollback) throws SQLException, RemoteException {
    try {
      loggingEntityDbProxy.endTransaction(rollback);
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
  public void setCheckDependencies(final boolean checkReferences) throws RemoteException {
    try {
      loggingEntityDbProxy.setCheckDependencies(checkReferences);
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public boolean getCheckDependencies() throws RemoteException {
    try {
      return loggingEntityDbProxy.getCheckDependencies();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public List<EntityKey> insert(final List<Entity> entities) throws DbException, RemoteException {
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
  public void delete(final List<Entity> entities) throws DbException, RemoteException {
    try {
      loggingEntityDbProxy.delete(entities);
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
                                      final boolean distinct, final boolean order) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectPropertyValues(entityID, propertyID, distinct, order);
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
  public Entity selectSingle(final EntityKey key) throws DbException, RemoteException {
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
  public Entity selectForUpdate(final EntityKey key) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectForUpdate(key);
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
  public List<Entity> selectMany(final List<EntityKey> keys) throws DbException, RemoteException {
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
  public List<Entity> selectAll(final String entityID, final boolean order) throws DbException, RemoteException {
    try {
      return loggingEntityDbProxy.selectAll(entityID, order);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  public Map<String, List<Entity>> getDependentEntities(final List<Entity> entities) throws DbException, UserException, RemoteException {
    try {
      return loggingEntityDbProxy.getDependentEntities(entities);
    }
    catch (UserException ue) {
      throw ue;
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  private IEntityDb initializeProxy() {
    return (IEntityDb) Proxy.newProxyInstance(EntityDbConnection.class.getClassLoader(),
            EntityDbConnection.class.getInterfaces(), new InvocationHandler() {
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        requestsPerSecondCounter++;
        final String methodName = method.getName();
        EntityDbConnection connection = null;
        try {
          active.add(EntityDbRemoteAdapter.this);
          logAccess(methodName, args);

          return method.invoke(connection = getConnection(client.getUser()), args);
        }
        catch (InvocationTargetException ie) {
          logger.error(this, ie);
          throw ie.getTargetException();
        }
        finally {
          try {
            active.remove(EntityDbRemoteAdapter.this);
            final long time = logExit(methodName);
            if (connection != null && !connection.isTransactionOpen())
              returnConnection(client.getUser(), connection);
            if (time > warningThreshold)
              warningTimeExceededCounter++;
          }
          catch (Exception e) {
            logger.error(this, e);
          }
        }
      }
    });
  }

  private void returnConnection(final User user, final EntityDbConnection connection) {
    if (connectionPools.containsKey(user) && connectionPools.get(user).getConnectionPoolSettings().isEnabled())
      connectionPools.get(user).checkInConnection(connection);
  }

  private EntityDbConnection getConnection(final User user) throws ClassNotFoundException, UserException, AuthenticationException {
    if (connectionPools.containsKey(user) && connectionPools.get(user).getConnectionPoolSettings().isEnabled()){
      final EntityDbConnection ret = connectionPools.get(user).checkOutConnection(repository, settings);
      if (ret != null) {
        if (entityDbConnection != null)//pool has been turned on since this one was created
          entityDbConnection.disconnect();//discard

        return ret;
      }
    }

    if (entityDbConnection == null)
      entityDbConnection = new EntityDbConnection(client.getUser(), repository, settings);

    return entityDbConnection;
  }

  private void logAccess(final String method, final Object[] args) {
    this.lastAccessDate = System.currentTimeMillis();
    this.lastAccessedMethod = method;
    if (loggingEnabled && shouldMethodBeLogged(lastAccessedMethod.hashCode())) {
      this.lastAccessMessage = parameterArrayToString(args);
      addLogEntry(lastAccessedMethod, lastAccessMessage, lastAccessDate, false);
    }
  }

  private long logExit(final String method) {
    this.lastExitDate = System.currentTimeMillis();
    this.lastExitedMethod = method;
    if (loggingEnabled && shouldMethodBeLogged(lastExitedMethod.hashCode()))
      return addLogEntry(lastExitedMethod, lastAccessMessage, lastExitDate, true);

    return -1;
  }

  private static boolean shouldMethodBeLogged(final int hashCode) {
    return hashCode != IS_LOGGED_IN && hashCode != CONNECTION_VALID && hashCode != GET_ACTIVE_USER;
  }

  private static List<LogEntry> initializeLog() {
    final List<LogEntry> log = new ArrayList<LogEntry>(logSize);
    for (int i = 0; i < logSize; i++)
      log.add(new LogEntry());

    return log;
  }

  private long addLogEntry(final String method, final String message, final long time, final boolean isExit) {
    if (log == null)
      log = initializeLog();
    if (!isExit) {
      if (logIdx > log.size()-1)
        logIdx = 0;

      log.get(logIdx).set(method, message, time);

      return -1;
    }
    else {//add to last log entry
      final LogEntry lastEntry = log.get(logIdx);
      assert lastEntry.method.equals(method);
      logIdx++;

      return lastEntry.setExitTime(time);
    }
  }

  private static String parameterArrayToString(final Object[] args) {
    if (args == null)
      return "";

    final StringBuffer ret = new StringBuffer(args.length*40);//best guess ?
    for (int i = 0; i < args.length; i++) {
      parameterToString(args[i], ret);
      if (i < args.length-1)
        ret.append(", ");
    }

    return ret.toString();
  }

  private static void parameterToString(final Object arg, final StringBuffer dest) {
    if (arg instanceof Object[]) {
      if (((Object[]) arg).length > 0)
        dest.append("[").append(parameterArrayToString((Object[]) arg)).append("]");
    }
    else if (arg instanceof Class)
      dest.append(((Class) arg).getSimpleName());
    else
      dest.append(arg.toString());
  }

  private static void updateRequestsPerSecond() {
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

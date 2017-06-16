/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.DaemonThreadFactory;
import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.Events;
import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.model.ExceptionUtil;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.local.LocalEntityConnections;

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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A base class for remote connections served by a {@link DefaultEntityConnectionServer}.
 */
public abstract class AbstractRemoteEntityConnection extends UnicastRemoteObject {

  private static final long serialVersionUID = 1;

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteEntityConnection.class);

  /**
   * A Proxy for logging method calls
   */
  protected final transient EntityConnection connectionProxy;

  /**
   * The proxy connection handler
   */
  private final transient RemoteEntityConnectionHandler connectionHandler;

  /**
   * An event notified when this connection is disconnected
   */
  private final transient Event<AbstractRemoteEntityConnection> disconnectedEvent = Events.event();

  /**
   * Instantiates a new AbstractRemoteEntityConnection and exports it on the given port number
   * @param connectionPool the connection pool to use, if none is provided a local connection is established
   * @param database defines the underlying database
   * @param clientInfo information about the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @param sslEnabled specifies whether or not ssl should be enabled
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   */
  protected AbstractRemoteEntityConnection(final ConnectionPool connectionPool, final Database database, final ClientInfo clientInfo,
                                           final int port, final boolean loggingEnabled, final boolean sslEnabled)
          throws DatabaseException, RemoteException {
    super(port, sslEnabled ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            sslEnabled ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.connectionHandler = new RemoteEntityConnectionHandler(
            this, connectionPool, clientInfo, database, loggingEnabled);
    this.connectionProxy = Util.initializeProxy(EntityConnection.class, connectionHandler);
    try {
      clientInfo.setClientHost(getClientHost());
    }
    catch (final ServerNotActiveException ignored) {/*ignored*/}
  }

  /**
   * @return {@link EntityConnection.Type#REMOTE}
   */
  public final EntityConnection.Type getType() {
    return EntityConnection.Type.REMOTE;
  }

  /**
   * @return the user this connection is using
   */
  public final User getUser() {
    return connectionHandler.clientInfo.getUser();
  }

  /**
   * @param methodLogger the method logger
   * @throws UnsupportedOperationException always
   */
  public final void setMethodLogger(final MethodLogger methodLogger) {
    throw new UnsupportedOperationException("setMethodLogger is not supported on remote connections");
  }

  /**
   * @return nothing
   * @throws UnsupportedOperationException always
   */
  public final DatabaseConnection getDatabaseConnection() {
    throw new UnsupportedOperationException("getDatabaseConnection is not supported on remote connections");
  }

  /**
   * @return true if this connection is connected
   */
  public final boolean isConnected() {
    synchronized (connectionProxy) {
      return connectionHandler.isConnected();
    }
  }

  /**
   * Disconnects this connection
   */
  public final void disconnect() {
    synchronized (connectionProxy) {
      try {
        UnicastRemoteObject.unexportObject(this, true);
      }
      catch (final NoSuchObjectException e) {
        LOG.error(e.getMessage(), e);
      }
      connectionHandler.disconnect();
    }
    disconnectedEvent.fire(this);
  }

  /**
   * @return information on the client using this remote connection
   */
  final ClientInfo getClientInfo() {
    return connectionHandler.clientInfo;
  }

  /**
   * @return a ClientLog instance containing information about this connections recent activity
   * @see org.jminor.framework.Configuration#SERVER_CONNECTION_LOG_SIZE
   */
  final ClientLog getClientLog() {
    return connectionHandler.getClientLog();
  }

  /**
   * @param timeout the number of milliseconds
   * @return true if this connection has been inactive for {@code timeout} milliseconds or longer
   */
  final boolean hasBeenInactive(final int timeout) {
    return System.currentTimeMillis() - connectionHandler.lastAccessTime > timeout;
  }

  final void setLoggingEnabled(final boolean status) {
    connectionHandler.methodLogger.setEnabled(status);
  }

  final boolean isLoggingEnabled() {
    return connectionHandler.methodLogger.isEnabled();
  }

  /**
   * @return true during a remote method call
   */
  final boolean isActive() {
    return RemoteEntityConnectionHandler.ACTIVE_CONNECTIONS.contains(this);
  }

  final void addDisconnectListener(final EventInfoListener<AbstractRemoteEntityConnection> listener) {
    disconnectedEvent.addInfoListener(listener);
  }

  final void removeDisconnectListener(final EventInfoListener listener) {
    disconnectedEvent.removeInfoListener(listener);
  }

  /**
   * @return the number of connections that are active at this moment
   */
  static int getActiveCount() {
    return RemoteEntityConnectionHandler.ACTIVE_CONNECTIONS.size();
  }

  static int getRequestsPerSecond() {
    return RemoteEntityConnectionHandler.REQUEST_COUNTER.getRequestsPerSecond();
  }

  private static final class RemoteEntityConnectionHandler implements InvocationHandler {

    private static final String LOG_IDENTIFIER_PROPERTY = "logIdentifier";

    private static final String GET_CONNECTION = "getConnection";

    private static final String RETURN_CONNECTION = "returnConnection";

    private static final RequestCounter REQUEST_COUNTER = new RequestCounter();

    /**
     * Contains the active remote connections, that is, those connections that are in the process of serving a request
     */
    private static final List<AbstractRemoteEntityConnection> ACTIVE_CONNECTIONS = Collections.synchronizedList(new ArrayList<>());

    /**
     * Contains information about the client using this connection
     */
    private final ClientInfo clientInfo;

    /**
     * Contains information about the underlying database
     */
    private final Database database;

    /**
     * The connection pool to use, if any
     */
    private final ConnectionPool connectionPool;

    /**
     * The remote entity connection using this connection handler
     */
    private final AbstractRemoteEntityConnection remoteEntityConnection;

    /**
     * The method call log
     */
    private final MethodLogger methodLogger = LocalEntityConnections.createLogger();

    /**
     * Identifies the log file being used for this connection
     */
    private final String logIdentifier;

    /**
     * The date and time when this remote connection was established
     */
    private final long creationDate = System.currentTimeMillis();

    /**
     * A local connection used in case no connection pool is provided, managed by getConnection()/returnConnection()
     */
    private EntityConnection localEntityConnection;

    /**
     * A local connection used in case of a connection pool, managed by getConnection()/returnConnection()
     */
    private EntityConnection poolEntityConnection;

    /**
     * The time this connection was last used
     */
    private long lastAccessTime = creationDate;

    /**
     * Indicates whether or not this remote connection has been disconnected
     */
    private boolean disconnected = false;

    private RemoteEntityConnectionHandler(final AbstractRemoteEntityConnection remoteEntityConnection, final ConnectionPool connectionPool,
                                          final ClientInfo clientInfo, final Database database, final boolean loggingEnabled) throws DatabaseException {
      this.clientInfo = clientInfo;
      this.database = database;
      this.connectionPool = connectionPool;
      this.remoteEntityConnection = remoteEntityConnection;
      this.methodLogger.setEnabled(loggingEnabled);
      this.logIdentifier = clientInfo.getUser().getUsername().toLowerCase() +"@" + clientInfo.getClientTypeID();
      try {
        if (connectionPool == null) {
          localEntityConnection = LocalEntityConnections.createConnection(this.database, this.clientInfo.getDatabaseUser());
          localEntityConnection.setMethodLogger(methodLogger);
        }
        else {
          poolEntityConnection = LocalEntityConnections.createConnection(this.database, connectionPool.getConnection());
          returnConnectionToPool();
        }
      }
      catch (final DatabaseException e) {
        disconnect();
        throw e;
      }
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      lastAccessTime = System.currentTimeMillis();
      final String methodName = method.getName();
      Exception exception = null;
      try {
        MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
        ACTIVE_CONNECTIONS.add(remoteEntityConnection);
        REQUEST_COUNTER.incrementRequestsPerSecondCounter();
        if (methodLogger.isEnabled()) {
          methodLogger.logAccess(methodName, args);
        }

        return method.invoke(getConnection(), args);
      }
      catch (final Exception e) {
        exception = ExceptionUtil.unwrapAndLog(e, InvocationTargetException.class, LOG,
                Collections.<Class<? extends Exception>>singletonList(ConnectionPoolException.NoConnectionAvailable.class));
        throw exception;
      }
      finally {
        ACTIVE_CONNECTIONS.remove(remoteEntityConnection);
        returnConnection();
        if (methodLogger.isEnabled()) {
          final MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
          final StringBuilder messageBuilder = new StringBuilder(clientInfo.toString()).append("\n");
          MethodLogger.appendLogEntry(messageBuilder, entry, 0);
          LOG.info(messageBuilder.toString());
        }
        MDC.remove(LOG_IDENTIFIER_PROPERTY);
      }
    }

    private EntityConnection getConnection() throws DatabaseException {
      Exception exception = null;
      try {
        if (methodLogger != null && methodLogger.isEnabled()) {
          methodLogger.logAccess(GET_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
        }
        if (connectionPool != null) {
          return getPooledEntityConnection();
        }

        return getLocalEntityConnection();
      }
      catch (final DatabaseException ex) {
        exception = ex;
        throw ex;
      }
      finally {
        if (methodLogger != null && methodLogger.isEnabled()) {
          String message = null;
          if (poolEntityConnection != null && poolEntityConnection.getDatabaseConnection().getRetryCount() > 0) {
            message = "retries: " + poolEntityConnection.getDatabaseConnection().getRetryCount();
          }
          methodLogger.logExit(GET_CONNECTION, exception, message);
        }
      }
    }

    private EntityConnection getPooledEntityConnection() throws DatabaseException {
      if (poolEntityConnection.isTransactionOpen()) {
        return poolEntityConnection;
      }
      poolEntityConnection.getDatabaseConnection().setConnection(connectionPool.getConnection());
      poolEntityConnection.setMethodLogger(methodLogger);

      return poolEntityConnection;
    }

    private EntityConnection getLocalEntityConnection() throws DatabaseException {
      if (!localEntityConnection.isConnected()) {
        localEntityConnection.disconnect();//just in case
        localEntityConnection = LocalEntityConnections.createConnection(database, clientInfo.getDatabaseUser());
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
        if (methodLogger != null && methodLogger.isEnabled()) {
          methodLogger.logAccess(RETURN_CONNECTION, new Object[]{clientInfo.getDatabaseUser(), clientInfo.getUser()});
        }
        poolEntityConnection.setMethodLogger(null);
        returnConnectionToPool();
      }
      catch (final Exception e) {
        LOG.info("Exception while returning connection to pool", e);
      }
      finally {
        if (methodLogger != null && methodLogger.isEnabled()) {
          methodLogger.logExit(RETURN_CONNECTION, null, null);
        }
      }
    }

    private void returnConnectionToPool() {
      final Connection connection = poolEntityConnection.getDatabaseConnection().getConnection();
      if (connection != null) {
        connectionPool.returnConnection(connection);
        poolEntityConnection.getDatabaseConnection().setConnection(null);
      }
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

    private boolean isConnected() {
      if (connectionPool != null) {
        return !disconnected;
      }

      return !disconnected && localEntityConnection != null && localEntityConnection.isConnected();
    }

    private void disconnect() {
      if (disconnected) {
        return;
      }
      disconnected = true;
      cleanupLocalConnections();
    }

    private ClientLog getClientLog() {
      synchronized (methodLogger) {
        return new ClientLog(clientInfo.getClientID(), creationDate, methodLogger.getEntries());
      }
    }
  }

  private static final class RequestCounter {

    private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

    private static final double THOUSAND = 1000d;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private long requestsPerSecondTime = System.currentTimeMillis();
    private int requestsPerSecond = 0;
    private int requestsPerSecondCounter = 0;

    private RequestCounter() {
      executorService.scheduleWithFixedDelay(this::updateRequestsPerSecond, 0, DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void updateRequestsPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - requestsPerSecondTime) / THOUSAND;
      if (seconds > 0) {
        requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
        requestsPerSecondCounter = 0;
        requestsPerSecondTime = current;
      }
    }

    private void incrementRequestsPerSecondCounter() {
      requestsPerSecondCounter++;
    }

    private int getRequestsPerSecond() {
      return requestsPerSecond;
    }
  }
}

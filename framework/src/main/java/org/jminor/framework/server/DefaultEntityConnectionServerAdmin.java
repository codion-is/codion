/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.Server;

import ch.qos.logback.classic.Level;
import com.sun.management.GarbageCollectionNotificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the EntityConnectionServerAdmin interface, providing admin access to a EntityConnectionServer instance.
 */
public final class DefaultEntityConnectionServerAdmin extends UnicastRemoteObject implements EntityConnectionServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityConnectionServerAdmin.class);

  private static final long serialVersionUID = 1;

  private static final int GC_INFO_MAX_LENGTH = 100;

  /**
   * The server being administrated
   */
  private final DefaultEntityConnectionServer server;
  private final String serverName;
  private final LinkedList<GcEvent> gcEventList = new LinkedList();

  /**
   * Instantiates a new DefaultEntityConnectionServerAdmin
   * @param server the server to administer
   * @param serverAdminPort the port on which to make the server admin available
   * @throws RemoteException in case of an exception
   * @throws NullPointerException in case {@code serverAdminPort} or {@code server} are not specified
   */
  public DefaultEntityConnectionServerAdmin(final DefaultEntityConnectionServer server, final Integer serverAdminPort) throws RemoteException {
    super(Objects.requireNonNull(serverAdminPort),
            Objects.requireNonNull(server).isSslEnabled() ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            server.isSslEnabled() ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    this.serverName = server.getServerInfo().getServerName();
    initializeGarbageCollectionListener();
  }

  /** {@inheritDoc} */
  @Override
  public Server.ServerInfo getServerInfo() {
    return server.getServerInfo();
  }

  /** {@inheritDoc} */
  @Override
  public String getSystemProperties() {
    return Util.getSystemProperties();
  }

  /** {@inheritDoc} */
  @Override
  public List<GcEvent> getGcEvents() {
    synchronized (gcEventList) {
      return gcEventList;
    }
  }

  /** {@inheritDoc} */
  @Override
  public ThreadStatistics getThreadStatistics() throws RemoteException {
    final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

    return new DefaultThreadStatistics(bean.getThreadCount(), bean.getDaemonThreadCount());
  }

  /** {@inheritDoc} */
  @Override
  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  /** {@inheritDoc} */
  @Override
  public Level getLoggingLevel() {
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    return rootLogger.getLevel();
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingLevel(final Level level) {
    LOG.info("setLoggingLevel({})", level);
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(level);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients(final String clientTypeID) {
    return server.getClients(clientTypeID);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ClientInfo> getClients() {
    return server.getClients();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getClientTypes() {
    final Set<String> clientTypes = new HashSet<>();
    for (final ClientInfo client : getClients()) {
      clientTypes.add(client.getClientTypeID());
    }

    return clientTypes;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect(final UUID clientID) throws RemoteException {
    LOG.info("disconnect({})", clientID);
    server.disconnect(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public void shutdown() throws RemoteException {
    server.shutdown();
  }

  /** {@inheritDoc} */
  @Override
  public int getActiveConnectionCount() {
    return DefaultRemoteEntityConnection.getActiveCount();
  }

  /** {@inheritDoc} */
  @Override
  public int getMaintenanceInterval() {
    return server.getMaintenanceInterval();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaintenanceInterval(final int interval) {
    LOG.info("setMaintenanceInterval({})", interval);
    server.setMaintenanceInterval(interval);
  }

  /** {@inheritDoc} */
  @Override
  public void removeConnections(final boolean timedOutOnly) throws RemoteException {
    LOG.info("removeConnections({})", timedOutOnly);
    server.removeConnections(timedOutOnly);
  }

  /** {@inheritDoc} */
  @Override
  public void resetConnectionPoolStatistics(final User user) {
    LOG.info("resetConnectionPoolStatistics({})", user);
    ConnectionPools.getConnectionPool(user).resetStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCollectFineGrainedPoolStatistics(final User user) {
    return ConnectionPools.getConnectionPool(user).isCollectFineGrainedStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public void setCollectFineGrainedPoolStatistics(final User user, final boolean value) {
    LOG.info("setCollectFineGrainedPoolStatistics({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setCollectFineGrainedStatistics(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getRequestsPerSecond() {
    return DefaultRemoteEntityConnection.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) {
    return ConnectionPools.getConnectionPool(user).getStatistics(since);
  }

  /** {@inheritDoc} */
  @Override
  public Database.Statistics getDatabaseStatistics() {
    return DatabaseUtil.getDatabaseStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public List<User> getConnectionPools() {
    final List<User> poolUsers = new ArrayList<>();
    for (final ConnectionPool pool : ConnectionPools.getConnectionPools()) {
      poolUsers.add(pool.getUser());
    }

    return poolUsers;
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionPoolCleanupInterval(final User user) {
    return ConnectionPools.getConnectionPool(user).getCleanupInterval();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) {
    LOG.info("setConnectionPoolCleanupInterval({}, {})", user, poolCleanupInterval);
    ConnectionPools.getConnectionPool(user).setCleanupInterval(poolCleanupInterval);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumConnectionPoolSize(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumPoolSize();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumConnectionPoolSize(final User user, final int value) {
    LOG.info("setMaximumConnectionPoolSize({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMaximumPoolSize(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getMinimumConnectionPoolSize(final User user) {
    return ConnectionPools.getConnectionPool(user).getMinimumPoolSize();
  }

  /** {@inheritDoc} */
  @Override
  public void setMinimumConnectionPoolSize(final User user, final int value) {
    LOG.info("setMinimumConnectionPoolSize({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMinimumPoolSize(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getPoolConnectionThreshold(final User user) {
    return ConnectionPools.getConnectionPool(user).getNewConnectionThreshold();
  }

  /** {@inheritDoc} */
  @Override
  public void setPoolConnectionThreshold(final User user, final int value) {
    LOG.info("setPoolConnectionThreshold({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setNewConnectionThreshold(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getPooledConnectionTimeout(final User user) {
    return ConnectionPools.getConnectionPool(user).getConnectionTimeout();
  }

  /** {@inheritDoc} */
  @Override
  public void setPooledConnectionTimeout(final User user, final int timeout) {
    LOG.info("setPooledConnectionTimeout({}, {})", user, timeout);
    ConnectionPools.getConnectionPool(user).setConnectionTimeout(timeout);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolRetryWaitPeriod(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumRetryWaitPeriod();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolRetryWaitPeriod(final User user, final int value) {
    LOG.info("setMaximumPoolRetryWaitPeriod({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMaximumRetryWaitPeriod(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolCheckOutTime(final User user) {
    return ConnectionPools.getConnectionPool(user).getMaximumCheckOutTime();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolCheckOutTime(final User user, final int value) {
    LOG.info("setMaximumPoolCheckOutTime({}, {})", user, value);
    ConnectionPools.getConnectionPool(user).setMaximumCheckOutTime(value);
  }

  /** {@inheritDoc} */
  @Override
  public long getAllocatedMemory() {
    return Util.getAllocatedMemory();
  }

  /** {@inheritDoc} */
  @Override
  public long getUsedMemory() {
    return Util.getUsedMemory();
  }

  /** {@inheritDoc} */
  @Override
  public long getMaxMemory() {
    return Util.getMaxMemory();
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionCount() {
    return server.getConnectionCount();
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionLimit() {
    return server.getConnectionLimit();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionLimit(final int value) {
    LOG.info("setConnectionLimit", value);
    server.setConnectionLimit(value);
  }

  /** {@inheritDoc} */
  @Override
  public ClientLog getClientLog(final UUID clientID) {
    return server.getClientLog(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoggingEnabled(final UUID clientID) {
    return server.isLoggingEnabled(clientID);
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingEnabled(final UUID clientID, final boolean status) {
    LOG.info("setLoggingEnabled({}, {})", clientID, status);
    server.setLoggingEnabled(clientID, status);
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionTimeout() {
    return server.getConnectionTimeout();
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionTimeout(final int timeout) {
    LOG.info("setConnectionTimeout({})", timeout);
    server.setConnectionTimeout(timeout);
  }

  /** {@inheritDoc} */
  @Override
  public Map<String,String> getEntityDefinitions() {
    return DefaultEntityConnectionServer.getEntityDefinitions();
  }

  private void initializeGarbageCollectionListener() {
    for (final GarbageCollectorMXBean collectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      ((NotificationEmitter) collectorMXBean).addNotificationListener(new GCNotifactionListener(), (NotificationFilter) notification ->
              notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION), null);
    }
  }

  private final class GCNotifactionListener implements NotificationListener {
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      synchronized (gcEventList) {
        final GarbageCollectionNotificationInfo notificationInfo =
                GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        gcEventList.addLast(new DefaultGcEvent(notification.getTimeStamp(), notificationInfo.getGcName(),
                notificationInfo.getGcInfo().getDuration()));
        if (gcEventList.size() > GC_INFO_MAX_LENGTH) {
          gcEventList.removeFirst();
        }
      }
    }
  }

  private static final class DefaultThreadStatistics implements ThreadStatistics, Serializable {

    private static final long serialVersionUID = 1;

    private final int threadCount;
    private final int daemonThreadCount;

    private DefaultThreadStatistics(final int threadCount, final int daemonThreadCount) {
      this.threadCount = threadCount;
      this.daemonThreadCount = daemonThreadCount;
    }

    @Override
    public int getThreadCount() {
      return threadCount;
    }

    @Override
    public int getDaemonThreadCount() {
      return daemonThreadCount;
    }
  }

  private static class DefaultGcEvent implements GcEvent, Serializable {

    private static final long serialVersionUID = 1;
    private final long timeStamp;
    private final String gcName;
    private final long duration;

    public DefaultGcEvent(final long timeStamp, final String gcName, final long duration) {
      this.timeStamp = timeStamp;
      this.gcName = gcName;
      this.duration = duration;
    }

    @Override
    public long getTimeStamp() {
      return timeStamp;
    }

    @Override
    public String getGcName() {
      return gcName;
    }

    @Override
    public long getDuration() {
      return duration;
    }
  }
}

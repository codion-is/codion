/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.LoggerProxy;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.ConnectionRequest;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.Server;

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
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
  private final LinkedList<GcEvent> gcEventList = new LinkedList();

  private final LoggerProxy loggerProxy = LoggerProxy.createLoggerProxy();

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
  public List<GcEvent> getGcEvents(final long since) {
    final List<GcEvent> gcEvents;
    synchronized (gcEventList) {
      gcEvents = new LinkedList<>(gcEventList);
    }
    gcEvents.removeIf(gcEvent -> gcEvent.getTimeStamp() < since);

    return gcEvents;
  }

  /** {@inheritDoc} */
  @Override
  public ThreadStatistics getThreadStatistics() throws RemoteException {
    final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    final Map<Thread.State, Integer> threadStateMap = new EnumMap<Thread.State, Integer>(Thread.State.class);
    for (final Long threadId : bean.getAllThreadIds()) {
      threadStateMap.compute(bean.getThreadInfo(threadId).getThreadState(), (threadState, value) -> value == null ? 1 : value + 1);
    }

    return new DefaultThreadStatistics(bean.getThreadCount(), bean.getDaemonThreadCount(), threadStateMap);
  }

  /** {@inheritDoc} */
  @Override
  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  /** {@inheritDoc} */
  @Override
  public Object getLoggingLevel() {
    if (loggerProxy != null) {
      return loggerProxy.getLogLevel();
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingLevel(final Object level) {
    LOG.info("setLoggingLevel({})", level);
    if (loggerProxy != null) {
      loggerProxy.setLogLevel(level);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<RemoteClient> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<RemoteClient> getClients(final String clientTypeId) {
    return server.getClients(clientTypeId);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<RemoteClient> getClients() {
    return server.getClients();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getClientTypes() {
    return getClients().stream().map(ConnectionRequest::getClientTypeId).collect(Collectors.toSet());
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect(final UUID clientId) throws RemoteException {
    LOG.info("disconnect({})", clientId);
    server.disconnect(clientId);
  }

  /** {@inheritDoc} */
  @Override
  public void shutdown() throws RemoteException {
    server.shutdown();
  }

  /** {@inheritDoc} */
  @Override
  public int getActiveConnectionCount() {
    return AbstractRemoteEntityConnection.getActiveCount();
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
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  /** {@inheritDoc} */
  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) {
    return ConnectionPools.getConnectionPool(user).getStatistics(since);
  }

  /** {@inheritDoc} */
  @Override
  public Database.Statistics getDatabaseStatistics() {
    return Databases.getDatabaseStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public List<User> getConnectionPools() {
    return ConnectionPools.getConnectionPools().stream().map(ConnectionPool::getUser).collect(Collectors.toList());
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
  public double getSystemCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  /** {@inheritDoc} */
  @Override
  public double getProcessCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
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
  public ClientLog getClientLog(final UUID clientId) {
    return server.getClientLog(clientId);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoggingEnabled(final UUID clientId) {
    return server.isLoggingEnabled(clientId);
  }

  /** {@inheritDoc} */
  @Override
  public void setLoggingEnabled(final UUID clientId, final boolean status) {
    LOG.info("setLoggingEnabled({}, {})", clientId, status);
    server.setLoggingEnabled(clientId, status);
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
    return server.getEntityDefinitions();
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
    private final Map<Thread.State, Integer> threadStateCount;

    private DefaultThreadStatistics(final int threadCount, final int daemonThreadCount,
                                    final Map<Thread.State, Integer> threadStateCount) {
      this.threadCount = threadCount;
      this.daemonThreadCount = daemonThreadCount;
      this.threadStateCount = threadStateCount;
    }

    @Override
    public int getThreadCount() {
      return threadCount;
    }

    @Override
    public int getDaemonThreadCount() {
      return daemonThreadCount;
    }

    @Override
    public Map<Thread.State, Integer> getThreadStateCount() {
      return threadStateCount;
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

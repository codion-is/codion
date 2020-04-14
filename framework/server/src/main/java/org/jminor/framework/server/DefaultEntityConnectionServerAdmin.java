/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.LoggerProxy;
import org.jminor.common.Memory;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.remote.server.ClientLog;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.remote.server.Server;
import org.jminor.common.user.User;

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
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jminor.common.db.pool.ConnectionPools.getConnectionPool;

/**
 * Implements the EntityConnectionServerAdmin interface, providing admin access to a EntityConnectionServer instance.
 */
final class DefaultEntityConnectionServerAdmin extends UnicastRemoteObject implements EntityConnectionServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityConnectionServerAdmin.class);

  private static final long serialVersionUID = 1;

  private static final int GC_INFO_MAX_LENGTH = 100;

  /**
   * The server being administrated
   */
  private final DefaultEntityConnectionServer server;
  private final LinkedList<GcEvent> gcEventList = new LinkedList<>();
  private final Util.PropertyWriter propertyWriter = new SystemPropertyWriter();

  private final LoggerProxy loggerProxy = LoggerProxy.createLoggerProxy();

  /**
   * Instantiates a new DefaultEntityConnectionServerAdmin
   * @param server the server to administer
   * @param serverAdminPort the port on which to make the server admin available
   * @throws RemoteException in case of an exception
   * @throws NullPointerException in case {@code serverAdminPort} or {@code server} are not specified
   */
  DefaultEntityConnectionServerAdmin(final DefaultEntityConnectionServer server, final Integer serverAdminPort) throws RemoteException {
    super(requireNonNull(serverAdminPort),
            requireNonNull(server).isSslEnabled() ? new SslRMIClientSocketFactory() : RMISocketFactory.getSocketFactory(),
            server.isSslEnabled() ? new SslRMIServerSocketFactory() : RMISocketFactory.getSocketFactory());
    this.server = server;
    initializeGarbageCollectionListener();
  }

  @Override
  public Server.ServerInfo getServerInfo() {
    return server.getServerInfo();
  }

  @Override
  public String getSystemProperties() {
    return Util.getSystemProperties(propertyWriter);
  }

  @Override
  public List<GcEvent> getGcEvents(final long since) {
    final List<GcEvent> gcEvents;
    synchronized (gcEventList) {
      gcEvents = new LinkedList<>(gcEventList);
    }
    gcEvents.removeIf(gcEvent -> gcEvent.getTimeStamp() < since);

    return gcEvents;
  }

  @Override
  public ThreadStatistics getThreadStatistics() throws RemoteException {
    final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    final Map<Thread.State, Integer> threadStateMap = new EnumMap<>(Thread.State.class);
    for (final Long threadId : bean.getAllThreadIds()) {
      threadStateMap.compute(bean.getThreadInfo(threadId).getThreadState(), (threadState, value) -> value == null ? 1 : value + 1);
    }

    return new DefaultThreadStatistics(bean.getThreadCount(), bean.getDaemonThreadCount(), threadStateMap);
  }

  @Override
  public String getDatabaseURL() {
    return server.getDatabase().getURL(null);
  }

  @Override
  public Object getLogLevel() {
    if (loggerProxy != null) {
      return loggerProxy.getLogLevel();
    }

    return null;
  }

  @Override
  public void setLogLevel(final Object level) {
    LOG.info("setLogLevel({})", level);
    if (loggerProxy != null) {
      loggerProxy.setLogLevel(level);
    }
  }

  @Override
  public Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  @Override
  public Collection<RemoteClient> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  @Override
  public Collection<RemoteClient> getClients(final String clientTypeId) {
    return server.getClients(clientTypeId);
  }

  @Override
  public Collection<RemoteClient> getClients() {
    return server.getClients();
  }

  @Override
  public Collection<String> getClientTypes() {
    return getClients().stream().map(ConnectionRequest::getClientTypeId).collect(toSet());
  }

  @Override
  public void disconnect(final UUID clientId) throws RemoteException {
    LOG.info("disconnect({})", clientId);
    server.disconnect(clientId);
  }

  @Override
  public void shutdown() throws RemoteException {
    server.shutdown();
  }

  @Override
  public int getMaintenanceInterval() {
    return server.getMaintenanceInterval();
  }

  @Override
  public void setMaintenanceInterval(final int interval) {
    LOG.info("setMaintenanceInterval({})", interval);
    server.setMaintenanceInterval(interval);
  }

  @Override
  public void disconnectTimedOutClients() throws RemoteException {
    LOG.info("disconnectTimedOutClients()");
    server.disconnectClients(true);
  }

  @Override
  public void disconnectAllClients() throws RemoteException {
    LOG.info("disconnectAllClients()");
    server.disconnectClients(false);
  }

  @Override
  public void resetConnectionPoolStatistics(final String username) {
    LOG.info("resetConnectionPoolStatistics({})", username);
    getConnectionPool(username).resetStatistics();
  }

  @Override
  public boolean isCollectPoolSnapshotStatistics(final String username) {
    return getConnectionPool(username).isCollectSnapshotStatistics();
  }

  @Override
  public void setCollectPoolSnapshotStatistics(final String username, final boolean value) {
    LOG.info("setCollectSnapshotPoolStatistics({}, {})", username, value);
    getConnectionPool(username).setCollectSnapshotStatistics(value);
  }

  @Override
  public int getRequestsPerSecond() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final String username, final long since) {
    return getConnectionPool(username).getStatistics(since);
  }

  @Override
  public Database.Statistics getDatabaseStatistics() {
    return server.getDatabaseStatistics();
  }

  @Override
  public List<String> getConnectionPools() {
    return ConnectionPools.getConnectionPools().stream().map(pool -> pool.getUser().getUsername()).collect(toList());
  }

  @Override
  public int getConnectionPoolCleanupInterval(final String username) {
    return getConnectionPool(username).getCleanupInterval();
  }

  @Override
  public void setConnectionPoolCleanupInterval(final String username, final int poolCleanupInterval) {
    LOG.info("setConnectionPoolCleanupInterval({}, {})", username, poolCleanupInterval);
    getConnectionPool(username).setCleanupInterval(poolCleanupInterval);
  }

  @Override
  public int getMaximumConnectionPoolSize(final String username) {
    return getConnectionPool(username).getMaximumPoolSize();
  }

  @Override
  public void setMaximumConnectionPoolSize(final String username, final int value) {
    LOG.info("setMaximumConnectionPoolSize({}, {})", username, value);
    getConnectionPool(username).setMaximumPoolSize(value);
  }

  @Override
  public int getMinimumConnectionPoolSize(final String username) {
    return getConnectionPool(username).getMinimumPoolSize();
  }

  @Override
  public void setMinimumConnectionPoolSize(final String username, final int value) {
    LOG.info("setMinimumConnectionPoolSize({}, {})", username, value);
    getConnectionPool(username).setMinimumPoolSize(value);
  }

  @Override
  public int getPooledConnectionTimeout(final String username) {
    return getConnectionPool(username).getConnectionTimeout();
  }

  @Override
  public void setPooledConnectionTimeout(final String username, final int timeout) {
    LOG.info("setPooledConnectionTimeout({}, {})", username, timeout);
    getConnectionPool(username).setConnectionTimeout(timeout);
  }

  @Override
  public int getMaximumPoolCheckOutTime(final String username) {
    return getConnectionPool(username).getMaximumCheckOutTime();
  }

  @Override
  public void setMaximumPoolCheckOutTime(final String username, final int value) {
    LOG.info("setMaximumPoolCheckOutTime({}, {})", username, value);
    getConnectionPool(username).setMaximumCheckOutTime(value);
  }

  @Override
  public long getAllocatedMemory() {
    return Memory.getAllocatedMemory();
  }

  @Override
  public long getUsedMemory() {
    return Memory.getUsedMemory();
  }

  @Override
  public long getMaxMemory() {
    return Memory.getMaxMemory();
  }

  @Override
  public double getSystemCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  @Override
  public double getProcessCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
  }

  @Override
  public int getConnectionCount() {
    return server.getConnectionCount();
  }

  @Override
  public int getConnectionLimit() {
    return server.getConnectionLimit();
  }

  @Override
  public void setConnectionLimit(final int value) {
    LOG.info("setConnectionLimit({})", value);
    server.setConnectionLimit(value);
  }

  @Override
  public ClientLog getClientLog(final UUID clientId) {
    return server.getClientLog(clientId);
  }

  @Override
  public boolean isLoggingEnabled(final UUID clientId) {
    return server.isLoggingEnabled(clientId);
  }

  @Override
  public void setLoggingEnabled(final UUID clientId, final boolean loggingEnabled) {
    LOG.info("setLoggingEnabled({}, {})", clientId, loggingEnabled);
    server.setLoggingEnabled(clientId, loggingEnabled);
  }

  @Override
  public int getConnectionTimeout() {
    return server.getConnectionTimeout();
  }

  @Override
  public void setConnectionTimeout(final int timeout) {
    LOG.info("setConnectionTimeout({})", timeout);
    server.setConnectionTimeout(timeout);
  }

  @Override
  public Map<String, String> getEntityDefinitions() {
    return server.getEntityDefinitions();
  }

  private void initializeGarbageCollectionListener() {
    for (final GarbageCollectorMXBean collectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      ((NotificationEmitter) collectorMXBean).addNotificationListener(new GcNotificationListener(), (NotificationFilter) notification ->
              notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION), null);
    }
  }

  private final class GcNotificationListener implements NotificationListener {
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

  private static final class SystemPropertyWriter implements Util.PropertyWriter {

    @Override
    public String writeValue(final String property, final String value) {
      if ("java.class.path".equals(property) && !value.isEmpty()) {
        return "\n" + String.join("\n", value.split(Util.PATH_SEPARATOR));
      }

      return value;
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

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.LoggerProxy;
import is.codion.common.Memory;
import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.ServerInformation;
import is.codion.common.user.User;
import is.codion.framework.domain.entity.Identity;

import com.sun.management.GarbageCollectionNotificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * Implements the EntityServerAdmin interface, providing admin access to a EntityServer instance.
 */
final class DefaultEntityServerAdmin extends UnicastRemoteObject implements EntityServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityServerAdmin.class);

  private static final long serialVersionUID = 1;

  private static final int GC_INFO_MAX_LENGTH = 100;

  /**
   * The server being administrated
   */
  private final EntityServer server;
  private final LinkedList<GcEvent> gcEventList = new LinkedList<>();
  private final Util.PropertyWriter propertyWriter = new SystemPropertyWriter();

  private final LoggerProxy loggerProxy = LoggerProxy.createLoggerProxy();

  /**
   * Instantiates a new DefaultEntityServerAdmin
   * @param server the server to administer
   * @param configuration the port on which to make the server admin available
   * @throws RemoteException in case of an exception
   * @throws NullPointerException in case {@code configuration} or {@code server} are not specified
   */
  DefaultEntityServerAdmin(final EntityServer server, final EntityServerConfiguration configuration) throws RemoteException {
    super(requireNonNull(configuration).getServerAdminPort(),
            configuration.getRmiClientSocketFactory(),
            configuration.getRmiServerSocketFactory());
    this.server = server;
    initializeGarbageCollectionListener();
  }

  @Override
  public ServerInformation getServerInfo() {
    return server.getServerInformation();
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
    gcEvents.removeIf(gcEvent -> gcEvent.getTimestamp() < since);

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
    return server.getDatabase().getUrl();
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
    server.getDatabase().getConnectionPool(username).resetStatistics();
  }

  @Override
  public boolean isCollectPoolSnapshotStatistics(final String username) {
    return server.getDatabase().getConnectionPool(username).isCollectSnapshotStatistics();
  }

  @Override
  public void setCollectPoolSnapshotStatistics(final String username, final boolean snapshotStatistics) {
    LOG.info("setCollectSnapshotPoolStatistics({}, {})", username, snapshotStatistics);
    server.getDatabase().getConnectionPool(username).setCollectSnapshotStatistics(snapshotStatistics);
  }

  @Override
  public int getRequestsPerSecond() {
    return AbstractRemoteEntityConnection.getRequestsPerSecond();
  }

  @Override
  public ConnectionPoolStatistics getConnectionPoolStatistics(final String username, final long since) {
    return server.getDatabase().getConnectionPool(username).getStatistics(since);
  }

  @Override
  public Database.Statistics getDatabaseStatistics() {
    return server.getDatabaseStatistics();
  }

  @Override
  public Collection<String> getConnectionPoolUsernames() {
    return server.getDatabase().getConnectionPoolUsernames();
  }

  @Override
  public int getConnectionPoolCleanupInterval(final String username) {
    return server.getDatabase().getConnectionPool(username).getCleanupInterval();
  }

  @Override
  public void setConnectionPoolCleanupInterval(final String username, final int poolCleanupInterval) {
    LOG.info("setConnectionPoolCleanupInterval({}, {})", username, poolCleanupInterval);
    server.getDatabase().getConnectionPool(username).setCleanupInterval(poolCleanupInterval);
  }

  @Override
  public int getMaximumConnectionPoolSize(final String username) {
    return server.getDatabase().getConnectionPool(username).getMaximumPoolSize();
  }

  @Override
  public void setMaximumConnectionPoolSize(final String username, final int value) {
    LOG.info("setMaximumConnectionPoolSize({}, {})", username, value);
    server.getDatabase().getConnectionPool(username).setMaximumPoolSize(value);
  }

  @Override
  public int getMinimumConnectionPoolSize(final String username) {
    return server.getDatabase().getConnectionPool(username).getMinimumPoolSize();
  }

  @Override
  public void setMinimumConnectionPoolSize(final String username, final int value) {
    LOG.info("setMinimumConnectionPoolSize({}, {})", username, value);
    server.getDatabase().getConnectionPool(username).setMinimumPoolSize(value);
  }

  @Override
  public int getPooledConnectionTimeout(final String username) {
    return server.getDatabase().getConnectionPool(username).getConnectionTimeout();
  }

  @Override
  public void setPooledConnectionTimeout(final String username, final int timeout) {
    LOG.info("setPooledConnectionTimeout({}, {})", username, timeout);
    server.getDatabase().getConnectionPool(username).setConnectionTimeout(timeout);
  }

  @Override
  public int getMaximumPoolCheckOutTime(final String username) {
    return server.getDatabase().getConnectionPool(username).getMaximumCheckOutTime();
  }

  @Override
  public void setMaximumPoolCheckOutTime(final String username, final int value) {
    LOG.info("setMaximumPoolCheckOutTime({}, {})", username, value);
    server.getDatabase().getConnectionPool(username).setMaximumCheckOutTime(value);
  }

  @Override
  public ServerStatistics getServerStatistics(final long since) throws RemoteException {
    return new DefaultServerStatistics(System.currentTimeMillis(), getConnectionCount(), getConnectionLimit(),
            getUsedMemory(), getMaxMemory(), getAllocatedMemory(), getRequestsPerSecond(), getSystemCpuLoad(),
            getProcessCpuLoad(), getThreadStatistics(), getGcEvents(since));
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
  public Map<Identity, String> getEntityDefinitions() {
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

  private static final class DefaultServerStatistics implements ServerStatistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp;
    private final int connectionCount;
    private final int connectionLimit;
    private final long usedMemory;
    private final long maximumMemory;
    private final long allocatedMemory;
    private final int requestsPerSecond;
    private final double systemCpuLoad;
    private final double processCpuLoad;
    private final ThreadStatistics threadStatistics;
    private final List<GcEvent> gcEvents;

    private DefaultServerStatistics(final long timestamp, final int connectionCount, final int connectionLimit,
                                    final long usedMemory, final long maximumMemory, final long allocatedMemory,
                                    final int requestsPerSecond, final double systemCpuLoad, final double processCpuLoad,
                                    final ThreadStatistics threadStatistics, final List<GcEvent> gcEvents) {
      this.timestamp = timestamp;
      this.connectionCount = connectionCount;
      this.connectionLimit = connectionLimit;
      this.usedMemory = usedMemory;
      this.maximumMemory = maximumMemory;
      this.allocatedMemory = allocatedMemory;
      this.requestsPerSecond = requestsPerSecond;
      this.systemCpuLoad = systemCpuLoad;
      this.processCpuLoad = processCpuLoad;
      this.threadStatistics = threadStatistics;
      this.gcEvents = gcEvents;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public int getConnectionCount() {
      return connectionCount;
    }

    @Override
    public int getConnectionLimit() {
      return connectionLimit;
    }

    @Override
    public long getUsedMemory() {
      return usedMemory;
    }

    @Override
    public long getMaximumMemory() {
      return maximumMemory;
    }

    @Override
    public long getAllocatedMemory() {
      return allocatedMemory;
    }

    @Override
    public int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    @Override
    public double getSystemCpuLoad() {
      return systemCpuLoad;
    }

    @Override
    public double getProcessCpuLoad() {
      return processCpuLoad;
    }

    @Override
    public ThreadStatistics getThreadStatistics() {
      return threadStatistics;
    }

    @Override
    public List<GcEvent> getGcEvents() {
      return gcEvents;
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

    private final long timestamp;
    private final String gcName;
    private final long duration;

    public DefaultGcEvent(final long timestamp, final String gcName, final long duration) {
      this.timestamp = timestamp;
      this.gcName = gcName;
      this.duration = duration;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
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

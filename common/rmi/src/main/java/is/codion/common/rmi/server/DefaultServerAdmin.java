/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.Memory;
import is.codion.common.Util;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;

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

public class DefaultServerAdmin extends UnicastRemoteObject implements ServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultServerAdmin.class);

  private static final long serialVersionUID = 1;

  private static final int GC_INFO_MAX_LENGTH = 100;

  private final AbstractServer<?, ? extends ServerAdmin> server;
  private final LinkedList<GcEvent> gcEventList = new LinkedList<>();
  private final Util.PropertyWriter propertyWriter = new SystemPropertyWriter();

  public DefaultServerAdmin(final AbstractServer<?, ? extends ServerAdmin> server, final ServerConfiguration configuration) throws RemoteException {
    super(requireNonNull(configuration, "configuration").getServerAdminPort(),
            configuration.getRmiClientSocketFactory(), configuration.getRmiServerSocketFactory());
    this.server = requireNonNull(server, "server");
    initializeGarbageCollectionListener();
  }

  @Override
  public final ServerInformation getServerInfo() {
    return server.getServerInformation();
  }

  @Override
  public final String getSystemProperties() {
    return Util.getSystemProperties(propertyWriter);
  }

  @Override
  public final List<GcEvent> getGcEvents(final long since) {
    final List<GcEvent> gcEvents;
    synchronized (gcEventList) {
      gcEvents = new LinkedList<>(gcEventList);
    }
    gcEvents.removeIf(gcEvent -> gcEvent.getTimestamp() < since);

    return gcEvents;
  }

  @Override
  public final ThreadStatistics getThreadStatistics() throws RemoteException {
    final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    final Map<Thread.State, Integer> threadStateMap = new EnumMap<>(Thread.State.class);
    for (final Long threadId : bean.getAllThreadIds()) {
      threadStateMap.compute(bean.getThreadInfo(threadId).getThreadState(), (threadState, value) -> value == null ? 1 : value + 1);
    }

    return new DefaultThreadStatistics(bean.getThreadCount(), bean.getDaemonThreadCount(), threadStateMap);
  }

  @Override
  public final Collection<User> getUsers() throws RemoteException {
    return server.getUsers();
  }

  @Override
  public final Collection<RemoteClient> getClients(final User user) throws RemoteException {
    return server.getClients(user);
  }

  @Override
  public final Collection<RemoteClient> getClients(final String clientTypeId) {
    return server.getClients(clientTypeId);
  }

  @Override
  public final Collection<RemoteClient> getClients() {
    return server.getClients();
  }

  @Override
  public final Collection<String> getClientTypes() {
    return getClients().stream().map(ConnectionRequest::getClientTypeId).collect(toSet());
  }

  @Override
  public final void disconnect(final UUID clientId) throws RemoteException {
    LOG.info("disconnect({})", clientId);
    server.disconnect(clientId);
  }

  @Override
  public final void shutdown() throws RemoteException {
    server.shutdown();
  }

  @Override
  public int getRequestsPerSecond() {
    return -1;
  }

  @Override
  public final ServerStatistics getServerStatistics(final long since) throws RemoteException {
    return new DefaultServerStatistics(System.currentTimeMillis(), getConnectionCount(), getConnectionLimit(),
            getUsedMemory(), getMaxMemory(), getAllocatedMemory(), getRequestsPerSecond(), getSystemCpuLoad(),
            getProcessCpuLoad(), getThreadStatistics(), getGcEvents(since));
  }

  @Override
  public final long getAllocatedMemory() {
    return Memory.getAllocatedMemory();
  }

  @Override
  public final long getUsedMemory() {
    return Memory.getUsedMemory();
  }

  @Override
  public final long getMaxMemory() {
    return Memory.getMaxMemory();
  }

  @Override
  public final double getSystemCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  @Override
  public final double getProcessCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
  }

  @Override
  public final int getConnectionCount() {
    return server.getConnectionCount();
  }

  @Override
  public final int getConnectionLimit() {
    return server.getConnectionLimit();
  }

  @Override
  public final void setConnectionLimit(final int value) {
    LOG.info("setConnectionLimit({})", value);
    server.setConnectionLimit(value);
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

  private static final class SystemPropertyWriter implements Util.PropertyWriter {

    @Override
    public String writeValue(final String property, final String value) {
      if ("java.class.path".equals(property) && !value.isEmpty()) {
        return "\n" + String.join("\n", value.split(Util.PATH_SEPARATOR));
      }

      return value;
    }
  }
}

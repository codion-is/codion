/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.Memory;
import is.codion.common.Separators;
import is.codion.common.property.PropertyStore;
import is.codion.common.property.PropertyStore.PropertyFormatter;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;

import com.sun.management.GarbageCollectionNotificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationEmitter;
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
 * A base server admin implementation.
 */
public class DefaultServerAdmin extends UnicastRemoteObject implements ServerAdmin {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultServerAdmin.class);

  private static final long serialVersionUID = 1;

  private static final int GC_INFO_MAX_LENGTH = 100;

  private final transient AbstractServer<?, ? extends ServerAdmin> server;
  private final transient LinkedList<GcEvent> gcEventList = new LinkedList<>();
  private final transient PropertyFormatter propertyFormatter = new SystemPropertyFormatter();

  /**
   * Instantiates a new DefaultServerAdmin instance.
   * @param server the server to administer
   * @param configuration the server configuration
   * @throws RemoteException in case of an exception
   */
  public DefaultServerAdmin(AbstractServer<?, ? extends ServerAdmin> server, ServerConfiguration configuration) throws RemoteException {
    super(requireNonNull(configuration, "configuration").adminPort(),
            configuration.rmiClientSocketFactory(), configuration.rmiServerSocketFactory());
    this.server = requireNonNull(server, "server");
    initializeGarbageCollectionListener();
  }

  @Override
  public final ServerInformation serverInformation() {
    return server.serverInformation();
  }

  @Override
  public final String systemProperties() {
    return PropertyStore.systemProperties(propertyFormatter);
  }

  @Override
  public final List<GcEvent> gcEvents(long since) {
    List<GcEvent> gcEvents;
    synchronized (gcEventList) {
      gcEvents = new LinkedList<>(gcEventList);
    }
    gcEvents.removeIf(gcEvent -> gcEvent.timestamp() < since);

    return gcEvents;
  }

  @Override
  public final ThreadStatistics threadStatistics() throws RemoteException {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    Map<Thread.State, Integer> threadStateMap = new EnumMap<>(Thread.State.class);
    for (Long threadId : bean.getAllThreadIds()) {
      threadStateMap.compute(bean.getThreadInfo(threadId).getThreadState(), (threadState, value) -> value == null ? 1 : value + 1);
    }

    return new DefaultThreadStatistics(bean.getThreadCount(), bean.getDaemonThreadCount(), threadStateMap);
  }

  @Override
  public final Collection<User> users() throws RemoteException {
    return server.users();
  }

  @Override
  public final Collection<RemoteClient> clients(User user) throws RemoteException {
    return server.clients(user);
  }

  @Override
  public final Collection<RemoteClient> clients(String clientTypeId) {
    return server.clients(clientTypeId);
  }

  @Override
  public final Collection<RemoteClient> clients() {
    return server.clients();
  }

  @Override
  public final Collection<String> clientTypes() {
    return clients().stream()
            .map(ConnectionRequest::clientTypeId)
            .collect(toSet());
  }

  @Override
  public final void disconnect(UUID clientId) throws RemoteException {
    LOG.info("disconnect({})", clientId);
    server.disconnect(clientId);
  }

  @Override
  public final void shutdown() throws RemoteException {
    server.shutdown();
  }

  @Override
  public int requestsPerSecond() {
    return -1;
  }

  @Override
  public final ServerStatistics serverStatistics(long since) throws RemoteException {
    return new DefaultServerStatistics(System.currentTimeMillis(), connectionCount(), getConnectionLimit(),
            usedMemory(), maxMemory(), allocatedMemory(), requestsPerSecond(), systemCpuLoad(),
            processCpuLoad(), threadStatistics(), gcEvents(since));
  }

  @Override
  public final long allocatedMemory() {
    return Memory.allocatedMemory();
  }

  @Override
  public final long usedMemory() {
    return Memory.usedMemory();
  }

  @Override
  public final long maxMemory() {
    return Memory.maxMemory();
  }

  @Override
  public final double systemCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
  }

  @Override
  public final double processCpuLoad() throws RemoteException {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
  }

  @Override
  public final int connectionCount() {
    return server.connectionCount();
  }

  @Override
  public final int getConnectionLimit() {
    return server.getConnectionLimit();
  }

  @Override
  public final void setConnectionLimit(int value) {
    LOG.info("setConnectionLimit({})", value);
    server.setConnectionLimit(value);
  }

  private void initializeGarbageCollectionListener() {
    for (GarbageCollectorMXBean collectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      ((NotificationEmitter) collectorMXBean).addNotificationListener(new GcNotificationListener(), notification ->
              notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION), null);
    }
  }

  private final class GcNotificationListener implements NotificationListener {
    @Override
    public void handleNotification(Notification notification, Object handback) {
      synchronized (gcEventList) {
        GarbageCollectionNotificationInfo notificationInfo =
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

    private DefaultServerStatistics(long timestamp, int connectionCount, int connectionLimit,
                                    long usedMemory, long maximumMemory, long allocatedMemory,
                                    int requestsPerSecond, double systemCpuLoad, double processCpuLoad,
                                    ThreadStatistics threadStatistics, List<GcEvent> gcEvents) {
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
    public long timestamp() {
      return timestamp;
    }

    @Override
    public int connectionCount() {
      return connectionCount;
    }

    @Override
    public int connectionLimit() {
      return connectionLimit;
    }

    @Override
    public long usedMemory() {
      return usedMemory;
    }

    @Override
    public long maximumMemory() {
      return maximumMemory;
    }

    @Override
    public long allocatedMemory() {
      return allocatedMemory;
    }

    @Override
    public int requestsPerSecond() {
      return requestsPerSecond;
    }

    @Override
    public double systemCpuLoad() {
      return systemCpuLoad;
    }

    @Override
    public double processCpuLoad() {
      return processCpuLoad;
    }

    @Override
    public ThreadStatistics threadStatistics() {
      return threadStatistics;
    }

    @Override
    public List<GcEvent> gcEvents() {
      return gcEvents;
    }
  }

  private static final class DefaultThreadStatistics implements ThreadStatistics, Serializable {

    private static final long serialVersionUID = 1;

    private final int threadCount;
    private final int daemonThreadCount;
    private final Map<Thread.State, Integer> threadStateCount;

    private DefaultThreadStatistics(int threadCount, int daemonThreadCount,
                                    Map<Thread.State, Integer> threadStateCount) {
      this.threadCount = threadCount;
      this.daemonThreadCount = daemonThreadCount;
      this.threadStateCount = threadStateCount;
    }

    @Override
    public int threadCount() {
      return threadCount;
    }

    @Override
    public int daemonThreadCount() {
      return daemonThreadCount;
    }

    @Override
    public Map<Thread.State, Integer> threadStateCount() {
      return threadStateCount;
    }
  }

  private static class DefaultGcEvent implements GcEvent, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp;
    private final String gcName;
    private final long duration;

    private DefaultGcEvent(long timestamp, String gcName, long duration) {
      this.timestamp = timestamp;
      this.gcName = gcName;
      this.duration = duration;
    }

    @Override
    public long timestamp() {
      return timestamp;
    }

    @Override
    public String gcName() {
      return gcName;
    }

    @Override
    public long duration() {
      return duration;
    }
  }

  private static final class SystemPropertyFormatter implements PropertyFormatter {

    @Override
    public String formatValue(String property, String value) {
      if (classOrModulePath(property) && !value.isEmpty()) {
        return "\n" + String.join("\n", value.split(Separators.PATH_SEPARATOR));
      }

      return value;
    }

    private static boolean classOrModulePath(String property) {
      return property.endsWith("class.path") || property.endsWith("module.path");
    }
  }
}

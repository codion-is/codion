/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.server.monitor;

import dev.codion.common.DateFormats;
import dev.codion.common.LoggerProxy;
import dev.codion.common.TaskScheduler;
import dev.codion.common.event.Event;
import dev.codion.common.event.EventListener;
import dev.codion.common.event.EventObserver;
import dev.codion.common.event.Events;
import dev.codion.common.rmi.server.Server;
import dev.codion.common.rmi.server.ServerInformation;
import dev.codion.common.rmi.server.exception.ServerAuthenticationException;
import dev.codion.common.user.User;
import dev.codion.common.value.Value;
import dev.codion.framework.server.EntityServerAdmin;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.Format;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

/**
 * A ServerMonitor
 */
public final class ServerMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ServerMonitor.class);
  private static final Format MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
  private static final double THOUSAND = 1000;
  private static final String GC_EVENT_PREFIX = "GC ";

  private final Event serverShutDownEvent = Events.event();
  private final Event statisticsUpdatedEvent = Events.event();
  private final Event logLevelChangedEvent = Events.event();
  private final Event<Integer> connectionLimitChangedEvent = Events.event();

  private final String hostName;
  private final ServerInformation serverInformation;
  private final int registryPort;
  private final EntityServerAdmin server;
  private final User serverAdminUser;

  private final TaskScheduler updateScheduler;
  private final Value<Integer> updateIntervalValue;

  private final DatabaseMonitor databaseMonitor;
  private final ClientUserMonitor clientMonitor;

  private final LoggerProxy loggerProxy = LoggerProxy.createLoggerProxy();

  private int connectionCount = 0;
  private boolean shutdown = false;

  private long memoryUsage;
  private final DefaultTableModel domainListModel = new DomainTableModel();
  private final XYSeries connectionRequestsPerSecondSeries = new XYSeries("Service requests per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();

  private final XYSeries allocatedMemorySeries = new XYSeries("Allocated memory");
  private final XYSeries usedMemorySeries = new XYSeries("Used memory");
  private final XYSeries maxMemorySeries = new XYSeries("Maximum memory");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();

  private final XYSeries connectionCountSeries = new XYSeries("Connection count");
  private final XYSeries connectionLimitSeries = new XYSeries("Maximum connection count");
  private final XYSeriesCollection connectionCountCollection = new XYSeriesCollection();

  private final Map<String, XYSeries> gcTypeSeries = new HashMap<>();
  private final XYSeriesCollection gcEventsCollection = new XYSeriesCollection();

  private final XYSeries threadCountSeries = new XYSeries("Threads");
  private final XYSeries daemonThreadCountSeries = new XYSeries("Daemon Threads");
  private final Map<Thread.State, XYSeries> threadStateSeries = new EnumMap<>(Thread.State.class);
  private final XYSeriesCollection threadCountCollection = new XYSeriesCollection();

  private final XYSeries systemLoadSeries = new XYSeries("System Load");
  private final XYSeries processLoadSeries = new XYSeries("Process Load");
  private final XYSeriesCollection systemLoadCollection = new XYSeriesCollection();

  private long lastStatisticsUpdateTime = System.currentTimeMillis();

  /**
   * Instantiates a new {@link ServerMonitor}
   * @param hostName the host name
   * @param serverInformation the server information
   * @param registryPort the registry port
   * @param serverAdminUser the admin user
   * @param updateRate the initial statistics update rate in seconds
   * @throws RemoteException in case of an exception
   * @throws ServerAuthenticationException in case the admin user credentials are incorrect
   */
  public ServerMonitor(final String hostName, final ServerInformation serverInformation, final int registryPort,
                       final User serverAdminUser, final int updateRate)
          throws RemoteException, ServerAuthenticationException {
    this.hostName = hostName;
    this.serverInformation = serverInformation;
    this.registryPort = registryPort;
    this.serverAdminUser = serverAdminUser;
    this.server = connectServer(serverInformation.getServerName());
    this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecondSeries);
    this.memoryUsageCollection.addSeries(maxMemorySeries);
    this.memoryUsageCollection.addSeries(allocatedMemorySeries);
    this.memoryUsageCollection.addSeries(usedMemorySeries);
    this.connectionCountCollection.addSeries(connectionCountSeries);
    this.connectionCountCollection.addSeries(connectionLimitSeries);
    this.threadCountCollection.addSeries(threadCountSeries);
    this.threadCountCollection.addSeries(daemonThreadCountSeries);
    this.systemLoadCollection.addSeries(systemLoadSeries);
    this.systemLoadCollection.addSeries(processLoadSeries);
    this.databaseMonitor = new DatabaseMonitor(server, updateRate);
    this.clientMonitor = new ClientUserMonitor(server, updateRate);
    this.updateScheduler = new TaskScheduler(this::updateStatistics, updateRate, 0, TimeUnit.SECONDS).start();
    this.updateIntervalValue = new IntervalValue(updateScheduler);
    refreshDomainList();
  }

  /**
   * Shuts down this server monitor
   */
  public void shutdown() {
    shutdown = true;
    updateScheduler.stop();
    databaseMonitor.shutdown();
    clientMonitor.shutdown();
  }

  /**
   * @return the server being monitored
   */
  public EntityServerAdmin getServer() {
    return server;
  }

  /**
   * @return the server information
   */
  public ServerInformation getServerInformation() {
    return serverInformation;
  }

  /**
   * @return the amount of memory being used by the server
   */
  public String getMemoryUsage() {
    return MEMORY_USAGE_FORMAT.format(memoryUsage) + " KB";
  }

  /**
   * @return the number of connected clients
   */
  public int getConnectionCount() {
    return connectionCount;
  }

  /**
   * @return the client monitor
   */
  public ClientUserMonitor getClientMonitor() {
    return clientMonitor;
  }

  /**
   * @return the database monitor
   */
  public DatabaseMonitor getDatabaseMonitor() {
    return databaseMonitor;
  }

  /**
   * @return the connection number limit
   * @throws RemoteException in case of an exception
   */
  public int getConnectionLimit() throws RemoteException {
    return server.getConnectionLimit();
  }

  /**
   * @param value the connection number limit
   * @throws RemoteException in case of an exception
   */
  public void setConnectionLimit(final int value) throws RemoteException {
    server.setConnectionLimit(value);
    connectionLimitChangedEvent.onEvent(value);
  }

  /**
   * @return the available log levels
   */
  public List getLogLevels() {
    if (loggerProxy == null) {
      return emptyList();
    }

    return loggerProxy.getLogLevels();
  }

  /**
   * @return the server log level
   * @throws RemoteException in case of an exception
   */
  public Object getLogLevel() throws RemoteException {
    return server.getLogLevel();
  }

  /**
   * @param level the server log level
   * @throws RemoteException in case of an exception
   */
  public void setLogLevel(final Object level) throws RemoteException {
    server.setLogLevel(level);
    logLevelChangedEvent.onEvent(level);
  }

  /**
   * @return the connection request dataset
   */
  public XYDataset getConnectionRequestsDataset() {
    return connectionRequestsPerSecondCollection;
  }

  /**
   * @return the memory usage dataset
   */
  public XYDataset getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  /**
   * @return the system load dataset
   */
  public XYDataset getSystemLoadDataset() {
    return systemLoadCollection;
  }

  /**
   * @return the connection count dataset
   */
  public XYDataset getConnectionCountDataset() {
    return connectionCountCollection;
  }

  /**
   * @return the garbage collection event dataset
   */
  public XYDataset getGcEventsDataset() {
    return gcEventsCollection;
  }

  /**
   * @return the thread count dataset
   */
  public XYDataset getThreadCountDataset() {
    return threadCountCollection;
  }

  /**
   * @return the server environment info
   * @throws RemoteException in case of a communication error
   */
  public String getEnvironmentInfo() throws RemoteException {
    final StringBuilder contents = new StringBuilder();
    final String startDate = DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP).format(serverInformation.getStartTime());
    contents.append("Server info:").append("\n");
    contents.append(serverInformation.getServerName()).append(" (").append(startDate).append(")").append(
            " port: ").append(serverInformation.getServerPort()).append("\n").append("\n");
    contents.append("Server version:").append("\n");
    contents.append(serverInformation.getServerVersion()).append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.getDatabaseURL()).append("\n").append("\n");
    contents.append("Server locale: ").append("\n");
    contents.append(serverInformation.getLocale()).append("\n");
    contents.append("Server time zone: ").append("\n");
    contents.append(serverInformation.getTimeZone()).append("\n");
    contents.append("System properties:").append("\n");
    contents.append(server.getSystemProperties());

    return contents.toString();
  }

  /**
   * Clears all collected statistics
   */
  public void clearStatistics() {
    connectionRequestsPerSecondSeries.clear();
    allocatedMemorySeries.clear();
    usedMemorySeries.clear();
    maxMemorySeries.clear();
    connectionCountSeries.clear();
    connectionLimitSeries.clear();
    threadCountSeries.clear();
    daemonThreadCountSeries.clear();
    systemLoadSeries.clear();
    processLoadSeries.clear();
    gcTypeSeries.values().forEach(XYSeries::clear);
    threadStateSeries.values().forEach(XYSeries::clear);
  }

  /**
   * Refreshes the domain model list
   * @throws RemoteException in case of an exception
   */
  public void refreshDomainList() throws RemoteException {
    domainListModel.setDataVector(new Object[][] {}, new Object[] {"Entity ID", "Table name"});
    final Map<String, String> definitions = server.getEntityDefinitions();
    for (final Map.Entry<String, String> definition : definitions.entrySet()) {
      domainListModel.addRow(new Object[] {definition.getKey(), definition.getValue()});
    }
  }

  /**
   * @return the table model for viewing the domain models
   */
  public TableModel getDomainTableModel() {
    return domainListModel;
  }

  /**
   * Shuts down the server
   */
  public void shutdownServer() {
    shutdown();
    try {
      server.shutdown();
    }
    catch (final RemoteException ignored) {/*ignored*/}
    serverShutDownEvent.onEvent();
  }

  /**
   * @return true if the server is reachable
   */
  public boolean isServerReachable() {
    try {
      server.getUsedMemory();
      return true;
    }
    catch (final Exception e) {
      return false;
    }
  }

  /**
   * @return the value controlling the update interval
   */
  public Value<Integer> getUpdateIntervalValue() {
    return updateIntervalValue;
  }

  /**
   * @param listener a listener notified when the server is shut down
   */
  public void addServerShutDownListener(final EventListener listener) {
    serverShutDownEvent.addListener(listener);
  }

  /**
   * @return a listener notified when the connection number limit is changed
   */
  public EventObserver<Integer> getConnectionLimitObserver() {
    return connectionLimitChangedEvent.getObserver();
  }

  /**
   * @return a listener notified when the statistics have been updated
   */
  public EventObserver getStatisticsUpdatedObserver() {
    return statisticsUpdatedEvent.getObserver();
  }

  /**
   * @return a listener notified when the log level has changed
   */
  public EventObserver getLogLevelObserver() {
    return logLevelChangedEvent.getObserver();
  }

  private EntityServerAdmin connectServer(final String serverName) throws RemoteException, ServerAuthenticationException {
    final long time = System.currentTimeMillis();
    try {
      final Server<?, EntityServerAdmin> theServer = (Server) LocateRegistry.getRegistry(hostName, registryPort).lookup(serverName);
      final EntityServerAdmin serverAdmin = theServer.getServerAdmin(serverAdminUser);
      //just some simple call to validate the remote connection
      serverAdmin.getUsedMemory();
      LOG.info("ServerMonitor connected to server: {}", serverName);
      return serverAdmin;
    }
    catch (final RemoteException e) {
      LOG.error("Server \"" + serverName + "\" is unreachable, host: " + hostName + ", registry port: " + registryPort, e);
      throw e;
    }
    catch (final NotBoundException e) {
      LOG.error(e.getMessage(), e);
      throw new RemoteException("Server " + serverName + " is not bound to registry on host: " + hostName + ", port: " + registryPort, e);
    }
    finally {
      LOG.debug("Registry.lookup(\"{}\"): {}", serverName, System.currentTimeMillis() - time);
    }
  }

  private void updateStatistics() {
    try {
      if (!shutdown) {
        final EntityServerAdmin.ServerStatistics statistics = server.getServerStatistics(lastStatisticsUpdateTime);
        final long timestamp = statistics.getTimestamp();
        lastStatisticsUpdateTime = timestamp;
        connectionCount = statistics.getConnectionCount();
        memoryUsage = statistics.getUsedMemory();
        connectionRequestsPerSecondSeries.add(timestamp, statistics.getRequestsPerSecond());
        maxMemorySeries.add(timestamp, statistics.getMaximumMemory() / THOUSAND);
        allocatedMemorySeries.add(timestamp, statistics.getAllocatedMemory() / THOUSAND);
        usedMemorySeries.add(timestamp, statistics.getUsedMemory() / THOUSAND);
        systemLoadSeries.add(timestamp, statistics.getSystemCpuLoad() * 100);
        processLoadSeries.add(timestamp, statistics.getProcessCpuLoad() * 100);
        connectionCountSeries.add(timestamp, statistics.getConnectionCount());
        connectionLimitSeries.add(timestamp, statistics.getConnectionLimit());
        addThreadStatistics(timestamp, statistics.getThreadStatistics());
        addGCInfo(statistics.getGcEvents());
        statisticsUpdatedEvent.onEvent();
      }
    }
    catch (final RemoteException ignored) {/*ignored*/}
  }

  private void addThreadStatistics(final long timestamp, final EntityServerAdmin.ThreadStatistics threadStatistics) {
    threadCountSeries.add(timestamp, threadStatistics.getThreadCount());
    daemonThreadCountSeries.add(timestamp, threadStatistics.getDaemonThreadCount());
    for (final Map.Entry<Thread.State, Integer> entry : threadStatistics.getThreadStateCount().entrySet()) {
      XYSeries stateSeries = threadStateSeries.get(entry.getKey());
      if (stateSeries == null) {
        stateSeries = new XYSeries(entry.getKey());
        threadStateSeries.put(entry.getKey(), stateSeries);
        threadCountCollection.addSeries(stateSeries);
      }
      stateSeries.add(timestamp, entry.getValue());
    }
  }

  private void addGCInfo(final List<EntityServerAdmin.GcEvent> gcEvents) {
    for (final EntityServerAdmin.GcEvent event : gcEvents) {
      XYSeries typeSeries = gcTypeSeries.get(GC_EVENT_PREFIX + event.getGcName());
      if (typeSeries == null) {
        typeSeries = new XYSeries(GC_EVENT_PREFIX + event.getGcName());
        gcTypeSeries.put(GC_EVENT_PREFIX + event.getGcName(), typeSeries);
        gcEventsCollection.addSeries(typeSeries);
      }
      typeSeries.add(event.getTimestamp(), event.getDuration());
    }
  }

  private static final class DomainTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(final int row, final int column) {
      return false;
    }
  }
}

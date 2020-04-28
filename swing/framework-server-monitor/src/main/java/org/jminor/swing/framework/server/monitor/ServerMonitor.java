/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.DateFormats;
import org.jminor.common.LoggerProxy;
import org.jminor.common.TaskScheduler;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.rmi.server.Server;
import org.jminor.common.rmi.server.ServerInformation;
import org.jminor.common.rmi.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;
import org.jminor.common.value.Value;
import org.jminor.framework.server.EntityServerAdmin;

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

  private final TaskScheduler updateScheduler = new TaskScheduler(this::updateStatistics,
          EntityServerMonitor.SERVER_MONITOR_UPDATE_RATE.get(), 2, TimeUnit.SECONDS).start();
  private final Value<Integer> updateIntervalValue = new IntervalValue(updateScheduler);

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
   * @throws RemoteException in case of an exception
   * @throws ServerAuthenticationException in case the admin user credentials are incorrect
   */
  public ServerMonitor(final String hostName, final ServerInformation serverInformation, final int registryPort,
                       final User serverAdminUser)
          throws RemoteException, ServerAuthenticationException {
    this.hostName = hostName;
    this.serverInformation = serverInformation;
    this.registryPort = registryPort;
    this.serverAdminUser = serverAdminUser;
    this.server = connectServer(serverInformation.getServerName());
    connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecondSeries);
    memoryUsageCollection.addSeries(maxMemorySeries);
    memoryUsageCollection.addSeries(allocatedMemorySeries);
    memoryUsageCollection.addSeries(usedMemorySeries);
    connectionCountCollection.addSeries(connectionCountSeries);
    connectionCountCollection.addSeries(connectionLimitSeries);
    threadCountCollection.addSeries(threadCountSeries);
    threadCountCollection.addSeries(daemonThreadCountSeries);
    systemLoadCollection.addSeries(systemLoadSeries);
    systemLoadCollection.addSeries(processLoadSeries);
    databaseMonitor = new DatabaseMonitor(server);
    clientMonitor = new ClientUserMonitor(server);
    refreshDomainList();
    updateStatistics();
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
   * Resets all connected statistics
   */
  public void resetStatistics() {
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
        final long time = System.currentTimeMillis();
        connectionCount = server.getConnectionCount();
        memoryUsage = server.getUsedMemory();
        connectionRequestsPerSecondSeries.add(time, server.getRequestsPerSecond());
        maxMemorySeries.add(time, server.getMaxMemory() / THOUSAND);
        allocatedMemorySeries.add(time, server.getAllocatedMemory() / THOUSAND);
        usedMemorySeries.add(time, server.getUsedMemory() / THOUSAND);
        systemLoadSeries.add(time, server.getSystemCpuLoad() * 100);
        processLoadSeries.add(time, server.getProcessCpuLoad() * 100);
        connectionCountSeries.add(time, server.getConnectionCount());
        connectionLimitSeries.add(time, server.getConnectionLimit());
        addThreadStatistics(server.getThreadStatistics());
        addGCInfo(server.getGcEvents(lastStatisticsUpdateTime));
        lastStatisticsUpdateTime = time;
        statisticsUpdatedEvent.onEvent();
      }
    }
    catch (final RemoteException ignored) {/*ignored*/}
  }

  private void addThreadStatistics(final EntityServerAdmin.ThreadStatistics threadStatistics) {
    final long time = threadStatistics.getTimestamp();
    threadCountSeries.add(time, threadStatistics.getThreadCount());
    daemonThreadCountSeries.add(time, threadStatistics.getDaemonThreadCount());
    for (final Map.Entry<Thread.State, Integer> entry : threadStatistics.getThreadStateCount().entrySet()) {
      XYSeries stateSeries = threadStateSeries.get(entry.getKey());
      if (stateSeries == null) {
        stateSeries = new XYSeries(entry.getKey());
        threadStateSeries.put(entry.getKey(), stateSeries);
        threadCountCollection.addSeries(stateSeries);
      }
      stateSeries.add(time, entry.getValue());
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

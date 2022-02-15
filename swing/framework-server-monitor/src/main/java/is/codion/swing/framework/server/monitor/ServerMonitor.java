/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.logging.LoggerProxy;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.rmi.server.ServerInformation;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.server.EntityServerAdmin;

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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A ServerMonitor
 */
public final class ServerMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ServerMonitor.class);
  private static final Format MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
  private static final double THOUSAND = 1000;
  private static final String GC_EVENT_PREFIX = "GC ";

  private final Event<?> serverShutDownEvent = Event.event();
  private final Value<Object> logLevelValue;
  private final Value<Integer> connectionLimitValue;

  private final String hostName;
  private final ServerInformation serverInformation;
  private final int registryPort;
  private final EntityServerAdmin server;
  private final User serverAdminUser;

  private final TaskScheduler updateScheduler;
  private final Value<Integer> updateIntervalValue;

  private final DatabaseMonitor databaseMonitor;
  private final ClientUserMonitor clientMonitor;

  private final LoggerProxy loggerProxy = LoggerProxy.loggerProxy();

  private boolean shutdown = false;

  private final Value<Integer> connectionCountValue = Value.value(0);
  private final Value<String> memoryUsageValue = Value.value("");
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
    this.connectionLimitValue = Value.value(this.server.getConnectionLimit());
    this.connectionLimitValue.addValidator(value -> {
      if (value == null || value < -1) {
        throw new IllegalArgumentException("Connection limit must be -1 or above");
      }
    });
    this.logLevelValue = Value.value(this.server.getLogLevel());
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
    this.updateScheduler = TaskScheduler.builder(this::updateStatistics)
            .interval(updateRate)
            .timeUnit(TimeUnit.SECONDS)
            .start();
    this.updateIntervalValue = new IntervalValue(updateScheduler);
    refreshDomainList();
    bindEvents();
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
  public ValueObserver<String> getMemoryUsageObserver() {
    return memoryUsageValue;
  }

  /**
   * @return the number of connected clients
   */
  public ValueObserver<Integer> getConnectionCountObserver() {
    return connectionCountValue;
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
   * @return the available log levels
   */
  public List<Object> getLogLevels() {
    return loggerProxy.getLogLevels();
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
    final String startDate = LocaleDateTimePattern.builder()
            .delimiterDash().yearFourDigits().hoursMinutesSeconds()
            .build().getFormatter().format(serverInformation.getStartTime());
    contents.append("Server info:").append("\n");
    contents.append(serverInformation.getServerName()).append(" (").append(startDate).append(")").append(
            " port: ").append(serverInformation.getServerPort()).append("\n").append("\n");
    contents.append("Server version:").append("\n");
    contents.append(serverInformation.getServerVersion()).append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.getDatabaseUrl()).append("\n").append("\n");
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
    domainListModel.setDataVector(new Object[][] {}, new Object[] {"Entity Type", "Table name"});
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
  public Value<Integer> getConnectionLimitValue() {
    return connectionLimitValue;
  }

  /**
   * @return a listener notified when the log level has changed
   */
  public Value<Object> getLogLevelValue() {
    return logLevelValue;
  }

  /**
   * @param value the connection number limit
   */
  private void setConnectionLimit(final int value) {
    try {
      server.setConnectionLimit(value);
    }
    catch (final RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param level the server log level
   */
  private void setLogLevel(final Object level) {
    try {
      server.setLogLevel(level);
    }
    catch (final RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private EntityServerAdmin connectServer(final String serverName) throws RemoteException, ServerAuthenticationException {
    final long time = System.currentTimeMillis();
    try {
      final Server<?, EntityServerAdmin> theServer = (Server<?, EntityServerAdmin>) LocateRegistry.getRegistry(hostName, registryPort).lookup(serverName);
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
        final ServerAdmin.ServerStatistics statistics = server.getServerStatistics(lastStatisticsUpdateTime);
        final long timestamp = statistics.getTimestamp();
        lastStatisticsUpdateTime = timestamp;
        connectionLimitValue.set(statistics.getConnectionLimit());
        connectionCountValue.set(statistics.getConnectionCount());
        memoryUsageValue.set(MEMORY_USAGE_FORMAT.format(statistics.getUsedMemory()) + " KB");
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
      }
    }
    catch (final RemoteException ignored) {/*ignored*/}
  }

  private void addThreadStatistics(final long timestamp, final ServerAdmin.ThreadStatistics threadStatistics) {
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

  private void addGCInfo(final List<ServerAdmin.GcEvent> gcEvents) {
    for (final ServerAdmin.GcEvent event : gcEvents) {
      XYSeries typeSeries = gcTypeSeries.get(GC_EVENT_PREFIX + event.getGcName());
      if (typeSeries == null) {
        typeSeries = new XYSeries(GC_EVENT_PREFIX + event.getGcName());
        gcTypeSeries.put(GC_EVENT_PREFIX + event.getGcName(), typeSeries);
        gcEventsCollection.addSeries(typeSeries);
      }
      typeSeries.add(event.getTimestamp(), event.getDuration());
    }
  }

  private void bindEvents() {
    connectionLimitValue.addDataListener(this::setConnectionLimit);
    logLevelValue.addDataListener(this::setLogLevel);
  }

  private static final class DomainTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(final int row, final int column) {
      return false;
    }
  }
}

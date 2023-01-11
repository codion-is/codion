/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.server.EntityServerAdmin.DomainEntityDefinition;
import is.codion.framework.server.EntityServerAdmin.DomainReport;

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
import java.util.Collection;
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

  private final LoggerProxy loggerProxy = LoggerProxy.instance();

  private boolean shutdown = false;

  private final Value<Integer> connectionCountValue = Value.value(0);
  private final Value<String> memoryUsageValue = Value.value("");
  private final DefaultTableModel domainTableModel = new ReadOnlyTableModel();
  private final DefaultTableModel reportTableModel = new ReadOnlyTableModel();
  private final XYSeries connectionRequestsPerSecondSeries = new XYSeries("Service requests per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();

  private final XYSeries allocatedMemorySeries = new XYSeries("Allocated memory");
  private final XYSeries usedMemorySeries = new XYSeries("Used memory");
  private final XYSeries maxMemorySeries = new XYSeries("Maximum memory");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();

  private final XYSeries connectionCountSeries = new XYSeries("Connection count");
  private final XYSeries connectionLimitSeries = new XYSeries("Connection limit");
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
  public ServerMonitor(String hostName, ServerInformation serverInformation, int registryPort,
                       User serverAdminUser, int updateRate)
          throws RemoteException, ServerAuthenticationException {
    this.hostName = hostName;
    this.serverInformation = serverInformation;
    this.registryPort = registryPort;
    this.serverAdminUser = serverAdminUser;
    this.server = connectServer(serverInformation.serverName());
    this.connectionLimitValue = Value.value(this::getConnectionLimit, this::setConnectionLimit, -1);
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
    this.updateIntervalValue = Value.value(updateScheduler::getInterval, updateScheduler::setInterval, 0);
    refreshDomainList();
    refreshReportList();
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
  public EntityServerAdmin server() {
    return server;
  }

  /**
   * @return the server information
   */
  public ServerInformation serverInformation() {
    return serverInformation;
  }

  /**
   * @return the amount of memory being used by the server
   */
  public ValueObserver<String> memoryUsageObserver() {
    return memoryUsageValue;
  }

  /**
   * @return the number of connected clients
   */
  public ValueObserver<Integer> connectionCountObserver() {
    return connectionCountValue;
  }

  /**
   * @return the client monitor
   */
  public ClientUserMonitor clientMonitor() {
    return clientMonitor;
  }

  /**
   * @return the database monitor
   */
  public DatabaseMonitor databaseMonitor() {
    return databaseMonitor;
  }

  /**
   * @return the available log levels
   */
  public List<Object> logLevels() {
    return loggerProxy.logLevels();
  }

  /**
   * @return the connection request dataset
   */
  public XYDataset connectionRequestsDataset() {
    return connectionRequestsPerSecondCollection;
  }

  /**
   * @return the memory usage dataset
   */
  public XYDataset memoryUsageDataset() {
    return memoryUsageCollection;
  }

  /**
   * @return the system load dataset
   */
  public XYDataset systemLoadDataset() {
    return systemLoadCollection;
  }

  /**
   * @return the connection count dataset
   */
  public XYDataset connectionCountDataset() {
    return connectionCountCollection;
  }

  /**
   * @return the garbage collection event dataset
   */
  public XYDataset gcEventsDataset() {
    return gcEventsCollection;
  }

  /**
   * @return the thread count dataset
   */
  public XYDataset threadCountDataset() {
    return threadCountCollection;
  }

  /**
   * @return the server environment info
   * @throws RemoteException in case of a communication error
   */
  public String environmentInfo() throws RemoteException {
    StringBuilder contents = new StringBuilder();
    String startDate = LocaleDateTimePattern.builder()
            .delimiterDash().yearFourDigits().hoursMinutesSeconds()
            .build().createFormatter().format(serverInformation.startTime());
    contents.append("Server info:").append("\n");
    contents.append(serverInformation.serverName()).append(" (").append(startDate).append(")").append(
            " port: ").append(serverInformation.serverPort()).append("\n").append("\n");
    contents.append("Server version:").append("\n");
    contents.append(serverInformation.serverVersion()).append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.databaseUrl()).append("\n").append("\n");
    contents.append("Server locale: ").append("\n");
    contents.append(serverInformation.locale()).append("\n");
    contents.append("Server time zone: ").append("\n");
    contents.append(serverInformation.timeZone()).append("\n");
    contents.append("System properties:").append("\n");
    contents.append(server.systemProperties());

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
   * Clears the server report cache
   * @throws RemoteException in case of an exception
   */
  public void clearReportCache() throws RemoteException {
    server.clearReportCache();
  }

  /**
   * Refreshes the domain model list
   * @throws RemoteException in case of an exception
   */
  public void refreshDomainList() throws RemoteException {
    domainTableModel.setDataVector(new Object[][] {}, new Object[] {"Domain Type", "Entity Type", "Table Name"});
    Map<String, Collection<DomainEntityDefinition>> definitions = server.domainEntityDefinitions();
    for (Map.Entry<String, Collection<DomainEntityDefinition>> domainDefinitions : definitions.entrySet()) {
      for (DomainEntityDefinition definition : domainDefinitions.getValue()) {
        domainTableModel.addRow(new Object[] {domainDefinitions.getKey(), definition.name(), definition.tableName()});
      }
    }
  }

  /**
   * Refreshes the report model list
   * @throws RemoteException in case of an exception
   */
  public void refreshReportList() throws RemoteException {
    reportTableModel.setDataVector(new Object[][] {}, new Object[] {"Domain Type", "Report Type", "Report Description", "Is Cached"});
    Map<String, Collection<DomainReport>> reports = server.domainReports();
    for (Map.Entry<String, Collection<DomainReport>> domainReports : reports.entrySet()) {
      for (DomainReport domainReport : domainReports.getValue()) {
        reportTableModel.addRow(new Object[] {domainReports.getKey(), domainReport.name(), domainReport.description(), domainReport.isCached()});
      }
    }
  }

  /**
   * @return the table model for viewing the domain models
   */
  public TableModel domainTableModel() {
    return domainTableModel;
  }

  /**
   * @return the table model for viewing reports
   */
  public TableModel reportTableModel() {
    return reportTableModel;
  }

  /**
   * Shuts down the server
   */
  public void shutdownServer() {
    shutdown();
    try {
      server.shutdown();
    }
    catch (RemoteException ignored) {/*ignored*/}
    serverShutDownEvent.onEvent();
  }

  /**
   * @return true if the server is reachable
   */
  public boolean isServerReachable() {
    try {
      server.usedMemory();
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * @return the value controlling the update interval
   */
  public Value<Integer> updateIntervalValue() {
    return updateIntervalValue;
  }

  /**
   * @param listener a listener notified when the server is shut down
   */
  public void addServerShutDownListener(EventListener listener) {
    serverShutDownEvent.addListener(listener);
  }

  /**
   * @return a listener notified when the connection number limit is changed
   */
  public Value<Integer> connectionLimitValue() {
    return connectionLimitValue;
  }

  /**
   * @return a listener notified when the log level has changed
   */
  public Value<Object> logLevelValue() {
    return logLevelValue;
  }

  private int getConnectionLimit() {
    try {
      return server.getConnectionLimit();
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param value the connection number limit
   */
  private void setConnectionLimit(Integer value) {
    if (value == null || value < -1) {
      throw new IllegalArgumentException("Connection limit must be -1 or above");
    }
    try {
      server.setConnectionLimit(value);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param level the server log level
   */
  private void setLogLevel(Object level) {
    try {
      server.setLogLevel(level);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  private EntityServerAdmin connectServer(String serverName) throws RemoteException, ServerAuthenticationException {
    long time = System.currentTimeMillis();
    try {
      Server<?, EntityServerAdmin> theServer = (Server<?, EntityServerAdmin>) LocateRegistry.getRegistry(hostName, registryPort).lookup(serverName);
      EntityServerAdmin serverAdmin = theServer.serverAdmin(serverAdminUser);
      //just some simple call to validate the remote connection
      serverAdmin.usedMemory();
      LOG.info("ServerMonitor connected to server: {}", serverName);
      return serverAdmin;
    }
    catch (RemoteException e) {
      LOG.error("Server \"" + serverName + "\" is unreachable, host: " + hostName + ", registry port: " + registryPort, e);
      throw e;
    }
    catch (NotBoundException e) {
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
        ServerAdmin.ServerStatistics statistics = server.serverStatistics(lastStatisticsUpdateTime);
        long timestamp = statistics.timestamp();
        lastStatisticsUpdateTime = timestamp;
        connectionLimitValue.set(statistics.connectionLimit());
        connectionCountValue.set(statistics.connectionCount());
        memoryUsageValue.set(MEMORY_USAGE_FORMAT.format(statistics.usedMemory()) + " KB");
        connectionRequestsPerSecondSeries.add(timestamp, statistics.requestsPerSecond());
        maxMemorySeries.add(timestamp, statistics.maximumMemory() / THOUSAND);
        allocatedMemorySeries.add(timestamp, statistics.allocatedMemory() / THOUSAND);
        usedMemorySeries.add(timestamp, statistics.usedMemory() / THOUSAND);
        systemLoadSeries.add(timestamp, statistics.systemCpuLoad() * 100);
        processLoadSeries.add(timestamp, statistics.processCpuLoad() * 100);
        connectionCountSeries.add(timestamp, statistics.connectionCount());
        connectionLimitSeries.add(timestamp, statistics.connectionLimit());
        addThreadStatistics(timestamp, statistics.threadStatistics());
        addGCInfo(statistics.gcEvents());
      }
    }
    catch (RemoteException ignored) {/*ignored*/}
  }

  private void addThreadStatistics(long timestamp, ServerAdmin.ThreadStatistics threadStatistics) {
    threadCountSeries.add(timestamp, threadStatistics.threadCount());
    daemonThreadCountSeries.add(timestamp, threadStatistics.daemonThreadCount());
    for (Map.Entry<Thread.State, Integer> entry : threadStatistics.threadStateCount().entrySet()) {
      XYSeries stateSeries = threadStateSeries.get(entry.getKey());
      if (stateSeries == null) {
        stateSeries = new XYSeries(entry.getKey());
        threadStateSeries.put(entry.getKey(), stateSeries);
        threadCountCollection.addSeries(stateSeries);
      }
      stateSeries.add(timestamp, entry.getValue());
    }
  }

  private void addGCInfo(List<ServerAdmin.GcEvent> gcEvents) {
    for (ServerAdmin.GcEvent event : gcEvents) {
      XYSeries typeSeries = gcTypeSeries.get(GC_EVENT_PREFIX + event.gcName());
      if (typeSeries == null) {
        typeSeries = new XYSeries(GC_EVENT_PREFIX + event.gcName());
        gcTypeSeries.put(GC_EVENT_PREFIX + event.gcName(), typeSeries);
        gcEventsCollection.addSeries(typeSeries);
      }
      typeSeries.add(event.timestamp(), event.duration());
    }
  }

  private void bindEvents() {
    connectionLimitValue.addDataListener(this::setConnectionLimit);
    logLevelValue.addDataListener(this::setLogLevel);
  }

  private static final class ReadOnlyTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  }
}

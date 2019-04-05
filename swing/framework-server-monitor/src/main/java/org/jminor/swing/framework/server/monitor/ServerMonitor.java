/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.DateFormats;
import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.LoggerProxy;
import org.jminor.common.TaskScheduler;
import org.jminor.common.User;
import org.jminor.common.remote.Server;
import org.jminor.common.remote.ServerException;
import org.jminor.framework.server.EntityConnectionServerAdmin;

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
import java.util.Collections;
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

  private final Event serverShutDownEvent = Events.event();
  private final Event<String> statisticsUpdatedEvent = Events.event();
  private final Event loggingLevelChangedEvent = Events.event();
  private final Event<Integer> connectionLimitChangedEvent = Events.event();

  private final String hostName;
  private final Server.ServerInfo serverInfo;
  private final int registryPort;
  private final EntityConnectionServerAdmin server;
  private final User serverAdminUser;

  private final TaskScheduler updateScheduler = new TaskScheduler(this::updateStatistics,
          EntityServerMonitor.SERVER_MONITOR_UPDATE_RATE.get(), 2, TimeUnit.SECONDS).start();

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
   * @param serverInfo the server info
   * @param registryPort the registry port
   * @param serverAdminUser the admin user
   * @throws RemoteException in case of an exception
   * @throws ServerException.AuthenticationException in case the admin user credentials are incorrect
   */
  public ServerMonitor(final String hostName, final Server.ServerInfo serverInfo, final int registryPort,
                       final User serverAdminUser)
          throws RemoteException, ServerException.AuthenticationException {
    this.hostName = hostName;
    this.serverInfo = serverInfo;
    this.registryPort = registryPort;
    this.serverAdminUser = serverAdminUser;
    this.server = connectServer(serverInfo.getServerName());
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
  public EntityConnectionServerAdmin getServer() {
    return server;
  }

  /**
   * @return the server into
   */
  public Server.ServerInfo getServerInfo() {
    return serverInfo;
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
    connectionLimitChangedEvent.fire(value);
  }

  /**
   * @return the available log levels
   */
  public List getLoggingLevels() {
    if (loggerProxy == null) {
      return Collections.emptyList();
    }

    return loggerProxy.getLogLevels();
  }

  /**
   * @return the server logging level
   * @throws RemoteException in case of an exception
   */
  public Object getLoggingLevel() throws RemoteException {
    return server.getLoggingLevel();
  }

  /**
   * @param level the server logging level
   * @throws RemoteException in case of an exception
   */
  public void setLoggingLevel(final Object level) throws RemoteException {
    server.setLoggingLevel(level);
    loggingLevelChangedEvent.fire(level);
  }

  /**
   * @return the connection request dataset
   */
  public XYSeriesCollection getConnectionRequestsDataset() {
    return connectionRequestsPerSecondCollection;
  }

  /**
   * @return the memory usage dataset
   */
  public XYSeriesCollection getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  /**
   * @return the system load dataset
   */
  public XYSeriesCollection getSystemLoadDataset() {
    return systemLoadCollection;
  }

  /**
   * @return the connection count dataset
   */
  public XYSeriesCollection getConnectionCountDataset() {
    return connectionCountCollection;
  }

  /**
   * @return the garbage collection event dataset
   */
  public XYSeriesCollection getGcEventsDataset() {
    return gcEventsCollection;
  }

  /**
   * @return the thread count dataset
   */
  public XYSeriesCollection getThreadCountDataset() {
    return threadCountCollection;
  }

  /**
   * @return the server environment info
   * @throws RemoteException in case of a communication error
   */
  public String getEnvironmentInfo() throws RemoteException {
    final StringBuilder contents = new StringBuilder();
    final String startDate = DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP).format(serverInfo.getStartTime());
    contents.append("Server info:").append("\n");
    contents.append(serverInfo.getServerName()).append(" (").append(startDate).append(")").append(
            " port: ").append(serverInfo.getServerPort()).append("\n").append("\n");
    contents.append("Server version:").append("\n");
    contents.append(serverInfo.getServerVersion()).append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.getDatabaseURL()).append("\n").append("\n");
    contents.append("Server locale: ").append("\n");
    contents.append(serverInfo.getLocale()).append("\n");
    contents.append("Server time zone: ").append("\n");
    contents.append(serverInfo.getTimeZone()).append("\n");
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
    serverShutDownEvent.fire();
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
   * @return the stat update scheduler
   */
  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
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
   * @return a listener notified when the logging level has changed
   */
  public EventObserver getLoggingLevelObserver() {
    return loggingLevelChangedEvent.getObserver();
  }

  private EntityConnectionServerAdmin connectServer(final String serverName) throws RemoteException, ServerException.AuthenticationException {
    final long time = System.currentTimeMillis();
    try {
      final Server<?, EntityConnectionServerAdmin> theServer = (Server) LocateRegistry.getRegistry(hostName, registryPort).lookup(serverName);
      final EntityConnectionServerAdmin serverAdmin = theServer.getServerAdmin(serverAdminUser);
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
        addThreadStatistics(time, server.getThreadStatistics());
        addGCInfo(server.getGcEvents(lastStatisticsUpdateTime));
        lastStatisticsUpdateTime = time;
        statisticsUpdatedEvent.fire();
      }
    }
    catch (final RemoteException ignored) {/*ignored*/}
  }

  private void addThreadStatistics(final long time, final EntityConnectionServerAdmin.ThreadStatistics threadStatistics) {
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

  private void addGCInfo(final List<EntityConnectionServerAdmin.GcEvent> gcEvents) {
    for (final EntityConnectionServerAdmin.GcEvent event : gcEvents) {
      XYSeries typeSeries = gcTypeSeries.get(GC_EVENT_PREFIX + event.getGcName());
      if (typeSeries == null) {
        typeSeries = new XYSeries(GC_EVENT_PREFIX + event.getGcName());
        gcTypeSeries.put(GC_EVENT_PREFIX + event.getGcName(), typeSeries);
        gcEventsCollection.addSeries(typeSeries);
      }
      typeSeries.add(event.getTimeStamp(), event.getDuration());
    }
  }

  private static final class DomainTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(final int row, final int column) {
      return false;
    }
  }
}

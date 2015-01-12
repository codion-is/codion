/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import ch.qos.logback.classic.Level;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.server.Server;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A ServerMonitor
 */
public final class ServerMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ServerMonitor.class);
  private static final double THOUSAND = 1000d;

  private final Event serverShutDownEvent = Events.event();
  private final Event<String> statisticsUpdatedEvent = Events.event();
  private final Event<Integer> warningThresholdChangedEvent = Events.event();
  private final Event<Level> loggingLevelChangedEvent = Events.event();
  private final Event<Integer> connectionLimitChangedEvent = Events.event();

  private final String hostName;
  private final Server.ServerInfo serverInfo;
  private final int registryPort;
  private final EntityConnectionServerAdmin server;

  private final TaskScheduler updateScheduler = new TaskScheduler(new Runnable() {
    @Override
    public void run() {
      try {
        if (!shutdown) {
          updateStatistics();
        }
      }
      catch (final RemoteException ignored) {}
    }
  }, Configuration.getIntValue(Configuration.SERVER_MONITOR_UPDATE_RATE), 2, TimeUnit.SECONDS).start();

  private final DatabaseMonitor databaseMonitor;
  private final ClientUserMonitor clientMonitor;

  private int connectionCount = 0;
  private volatile boolean shutdown = false;

  private String memoryUsage;
  private final DefaultTableModel domainListModel = new DomainTableModel();
  private final XYSeries connectionRequestsPerSecondSeries = new XYSeries("Service requests per second");
  private final XYSeries warningTimeExceededSecondSeries = new XYSeries("Service calls exceeding warning time per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();

  private final XYSeries allocatedMemorySeries = new XYSeries("Allocated memory");
  private final XYSeries usedMemorySeries = new XYSeries("Used memory");
  private final XYSeries maxMemorySeries = new XYSeries("Maximum memory");
  private final XYSeriesCollection memoryUsageCollection = new XYSeriesCollection();

  private final XYSeries connectionCountSeries = new XYSeries("Connection count");
  private final XYSeries connectionLimitSeries = new XYSeries("Maximum connection count");
  private final XYSeriesCollection connectionCountCollection = new XYSeriesCollection();

  public ServerMonitor(final String hostName, final Server.ServerInfo serverInfo, final int registryPort) throws RemoteException {
    this.hostName = hostName;
    this.serverInfo = serverInfo;
    this.registryPort = registryPort;
    Configuration.class.getName();
    this.server = connectServer(serverInfo.getServerName());
    connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecondSeries);
    connectionRequestsPerSecondCollection.addSeries(warningTimeExceededSecondSeries);
    memoryUsageCollection.addSeries(maxMemorySeries);
    memoryUsageCollection.addSeries(allocatedMemorySeries);
    memoryUsageCollection.addSeries(usedMemorySeries);
    connectionCountCollection.addSeries(connectionCountSeries);
    connectionCountCollection.addSeries(connectionLimitSeries);
    databaseMonitor = new DatabaseMonitor(server);
    clientMonitor = new ClientUserMonitor(server);
    refreshDomainList();
    updateStatistics();
  }

  public void shutdown() {
    shutdown = true;
    updateScheduler.stop();
    databaseMonitor.shutdown();
    clientMonitor.shutdown();
  }

  public EntityConnectionServerAdmin getServer() {
    return server;
  }

  public Server.ServerInfo getServerInfo() {
    return serverInfo;
  }

  public String getMemoryUsage() {
    return memoryUsage;
  }

  public int getConnectionCount() {
    return connectionCount;
  }

  public ClientUserMonitor getClientMonitor() {
    return clientMonitor;
  }

  public DatabaseMonitor getDatabaseMonitor() {
    return databaseMonitor;
  }

  public int getWarningThreshold() throws RemoteException {
    return server.getWarningTimeThreshold();
  }

  public void setWarningThreshold(final int threshold) throws RemoteException {
    server.setWarningTimeThreshold(threshold);
    warningThresholdChangedEvent.fire(threshold);
  }

  public int getConnectionLimit() throws RemoteException {
    return server.getConnectionLimit();
  }

  public void setConnectionLimit(final int value) throws RemoteException {
    server.setConnectionLimit(value);
    connectionLimitChangedEvent.fire(value);
  }

  public Level getLoggingLevel() throws RemoteException {
    return server.getLoggingLevel();
  }

  public void setLoggingLevel(final Level level) throws RemoteException {
    server.setLoggingLevel(level);
    loggingLevelChangedEvent.fire(level);
  }

  public XYSeriesCollection getConnectionRequestsDataset() {
    return connectionRequestsPerSecondCollection;
  }

  public XYSeriesCollection getMemoryUsageDataset() {
    return memoryUsageCollection;
  }

  public XYSeriesCollection getConnectionCountDataset() {
    return connectionCountCollection;
  }

  public String getEnvironmentInfo() throws RemoteException {
    final StringBuilder contents = new StringBuilder();
    final String startDate = DateFormats.getDateFormat(DateFormats.FULL_TIMESTAMP).format(new Date(serverInfo.getStartTime()));
    contents.append("Server info:").append("\n");
    contents.append(serverInfo.getServerName()).append(" (").append(startDate).append(")").append(
            " port: ").append(serverInfo.getServerPort()).append("\n").append("\n");
    contents.append("Server version:").append("\n");
    contents.append(serverInfo.getServerVersion()).append("\n");
    contents.append("Database URL:").append("\n");
    contents.append(server.getDatabaseURL()).append("\n").append("\n");
    contents.append("System properties:").append("\n");
    contents.append(server.getSystemProperties());

    return contents.toString();
  }

  public void performGC() throws RemoteException {
    server.performGC();
  }

  public void resetStatistics() {
    connectionRequestsPerSecondSeries.clear();
    warningTimeExceededSecondSeries.clear();
    allocatedMemorySeries.clear();
    usedMemorySeries.clear();
    maxMemorySeries.clear();
    connectionCountSeries.clear();
    connectionLimitSeries.clear();
  }

  public void refreshDomainList() throws RemoteException {
    domainListModel.setDataVector(new Object[][]{}, new Object[]{"Entity ID", "Table name"});
    final Map<String,String> definitions = server.getEntityDefinitions();
    for (final Map.Entry<String, String> definition : definitions.entrySet()) {
      domainListModel.addRow(new Object[] {definition.getKey(), definition.getValue()});
    }
  }

  public TableModel getDomainTableModel() {
    return domainListModel;
  }

  public void shutdownServer() {
    shutdown();
    try {
      server.shutdown();
    }
    catch (final RemoteException ignored) {}
    serverShutDownEvent.fire();
  }

  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
  }

  public EventObserver getServerShutDownObserver() {
    return serverShutDownEvent.getObserver();
  }

  public EventObserver<Integer> getWarningThresholdObserver() {
    return warningThresholdChangedEvent.getObserver();
  }

  public EventObserver<Integer> getConnectionLimitObserver() {
    return connectionLimitChangedEvent.getObserver();
  }

  public EventObserver getStatisticsUpdatedObserver() {
    return statisticsUpdatedEvent.getObserver();
  }

  public EventObserver<Level> getLoggingLevelObserver() {
    return loggingLevelChangedEvent.getObserver();
  }

  private EntityConnectionServerAdmin connectServer(final String serverName) throws RemoteException {
    final long time = System.currentTimeMillis();
    try {
      final EntityConnectionServerAdmin serverAdmin =
              (EntityConnectionServerAdmin) LocateRegistry.getRegistry(hostName, registryPort).lookup(Configuration.SERVER_ADMIN_PREFIX + serverName);
      //just some simple call to validate the remote connection
      serverAdmin.getMemoryUsage();
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

  private void updateStatistics() throws RemoteException {
    final long time = System.currentTimeMillis();
    connectionCount = server.getConnectionCount();
    memoryUsage = server.getMemoryUsage();
    connectionRequestsPerSecondSeries.add(time, server.getRequestsPerSecond());
    warningTimeExceededSecondSeries.add(time, server.getWarningTimeExceededPerSecond());
    maxMemorySeries.add(time, server.getMaxMemory() / THOUSAND);
    allocatedMemorySeries.add(time, server.getAllocatedMemory() / THOUSAND);
    usedMemorySeries.add(time, server.getUsedMemory() / THOUSAND);
    connectionCountSeries.add(time, server.getConnectionCount());
    connectionLimitSeries.add(time, server.getConnectionLimit());
    statisticsUpdatedEvent.fire();
  }

  private static final class DomainTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(final int row, final int column) {
      return false;
    }
  }
}

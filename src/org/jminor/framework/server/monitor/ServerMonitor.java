/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.server.RemoteServer;
import org.jminor.framework.Configuration;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import ch.qos.logback.classic.Level;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A ServerMonitor
 */
public final class ServerMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ServerMonitor.class);
  private static final double THOUSAND = 1000d;

  private final Event evtServerShutDown = Events.event();
  private final Event evtStatisticsUpdated = Events.event();
  private final Event evtWarningThresholdChanged = Events.event();
  private final Event evtLoggingLevelChanged = Events.event();
  private final Event evtConnectionLimitChanged = Events.event();

  private final String hostName;
  private final String serverName;
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
      catch (RemoteException ignored) {}
    }
  }, 2, 2, TimeUnit.SECONDS).start();

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

  public ServerMonitor(final String hostName, final String serverName, final int registryPort) throws RemoteException {
    this.hostName = hostName;
    this.serverName = removeAdminPrefix(serverName);
    this.registryPort = registryPort;
    Configuration.class.getName();
    this.server = connectServer(serverName);
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
  }

  public EntityConnectionServerAdmin getServer() {
    return server;
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
    evtWarningThresholdChanged.fire();
  }

  public int getConnectionLimit() throws RemoteException {
    return server.getConnectionLimit();
  }

  public void setConnectionLimit(final int value) throws RemoteException {
    server.setConnectionLimit(value);
    evtConnectionLimitChanged.fire();
  }

  public Level getLoggingLevel() throws RemoteException {
    return server.getLoggingLevel();
  }

  public void setLoggingLevel(final Level level) throws RemoteException {
    server.setLoggingLevel(level);
    evtLoggingLevelChanged.fire();
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
    catch (RemoteException ignored) {}
    evtServerShutDown.fire();
  }

  public String getServerName() {
    return serverName;
  }

  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
  }

  public EventObserver getServerShutDownObserver() {
    return evtServerShutDown.getObserver();
  }

  public EventObserver getWarningThresholdObserver() {
    return evtWarningThresholdChanged.getObserver();
  }

  public EventObserver getConnectionLimitObserver() {
    return evtConnectionLimitChanged.getObserver();
  }

  public EventObserver getStatisticsUpdatedObserver() {
    return evtStatisticsUpdated.getObserver();
  }

  public EventObserver getLoggingLevelObserver() {
    return evtLoggingLevelChanged.getObserver();
  }

  private EntityConnectionServerAdmin connectServer(final String serverName) throws RemoteException {
    final long time = System.currentTimeMillis();
    try {
      final EntityConnectionServerAdmin serverAdmin =
              (EntityConnectionServerAdmin) LocateRegistry.getRegistry(hostName, registryPort).lookup(serverName);
      //call to validate the remote connection
      serverAdmin.getServerPort();
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
    evtStatisticsUpdated.fire();
  }

  private static String removeAdminPrefix(final String serverName) {
    if (serverName.startsWith(RemoteServer.SERVER_ADMIN_PREFIX)) {
      return serverName.substring(RemoteServer.SERVER_ADMIN_PREFIX.length(), serverName.length());
    }

    return serverName;
  }

  private static final class DomainTableModel extends DefaultTableModel {
    /** {@inheritDoc} */
    @Override
    public boolean isCellEditable(final int row, final int column) {
      return false;
    }
  }
}

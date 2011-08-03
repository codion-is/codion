/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
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
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A ServerMonitor
 */
public final class ServerMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(ServerMonitor.class);
  private static final double THOUSAND = 1000d;

  private final Event evtStatsUpdateIntervalChanged = Events.event();
  private final Event evtServerShutDown = Events.event();
  private final Event evtStatsUpdated = Events.event();
  private final Event evtWarningThresholdChanged = Events.event();
  private final Event evtLoggingLevelChanged = Events.event();
  private final Event evtConnectionLimitChanged = Events.event();

  private final String hostName;
  private final String serverName;
  private final int registryPort;
  private final EntityConnectionServerAdmin server;

  private Timer updateTimer;
  private int statsUpdateInterval;

  private final DatabaseMonitor databaseMonitor;
  private final ClientUserMonitor clientMonitor;

  private int connectionCount = 0;
  private boolean shutdown = false;

  private String memoryUsage;
  private final DefaultTableModel domainListModel = new DefaultTableModel();
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
    setStatsUpdateInterval(2);
  }

  public void setStatsUpdateInterval(final int value) {
    if (value != this.statsUpdateInterval) {
      this.statsUpdateInterval = value;
      evtStatsUpdateIntervalChanged.fire();
      startUpdateTimer(value * 1000);
    }
  }

  public void shutdown() {
    shutdown = true;
    if (updateTimer != null) {
      updateTimer.cancel();
    }
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

  public XYSeriesCollection getConnectionRequestsDataSet() {
    return connectionRequestsPerSecondCollection;
  }

  public XYSeriesCollection getMemoryUsageDataSet() {
    return memoryUsageCollection;
  }

  public XYSeriesCollection getConnectionCountDataSet() {
    return connectionCountCollection;
  }

  public void performGC() throws RemoteException {
    server.performGC();
  }

  public void resetStats() {
    connectionRequestsPerSecondSeries.clear();
    warningTimeExceededSecondSeries.clear();
    allocatedMemorySeries.clear();
    usedMemorySeries.clear();
    maxMemorySeries.clear();
    connectionCountSeries.clear();
  }

  public void refreshDomainList() throws RemoteException {
    domainListModel.setDataVector(new Object[][]{}, new Object[] {"Entity ID", "Table name"});
    final Map<String,String> definitions = server.getEntityDefinitions();
    for (final Map.Entry<String, String> definition : definitions.entrySet()) {
      domainListModel.addRow(new Object[] {definition.getKey(), definition.getValue()});
    }
  }

  public TableModel getDomainTableModel() {
    return domainListModel;
  }

  public void shutdownServer() throws RemoteException {
    shutdown();
    try {
      server.shutdown();
    }
    catch (RemoteException e) {/**/}
    evtServerShutDown.fire();
  }

  public String getServerName() {
    return serverName;
  }

  public void addServerShutDownListener(final ActionListener listener) {
    evtServerShutDown.addListener(listener);
  }

  public void removeServerShutDownListener(final ActionListener listener) {
    evtServerShutDown.removeListener(listener);
  }

  public void addStatsUpdatedListener(final ActionListener listener) {
    evtStatsUpdated.addListener(listener);
  }

  public void removeStatsUpdatedListener(final ActionListener listener) {
    evtStatsUpdated.removeListener(listener);
  }

  public void addWarningThresholdListener(final ActionListener listener) {
    evtWarningThresholdChanged.addListener(listener);
  }

  public void removeWarningThresholdListener(final ActionListener listener) {
    evtWarningThresholdChanged.removeListener(listener);
  }

  public void addStatsUpdateIntervalListener(final ActionListener listener) {
    evtStatsUpdateIntervalChanged.addListener(listener);
  }

  public void removeStatsUpdateIntervalListener(final ActionListener listener) {
    evtStatsUpdateIntervalChanged.removeListener(listener);
  }

  public EventObserver getWarningThresholdObserver() {
    return evtWarningThresholdChanged.getObserver();
  }

  public EventObserver getConnectionLimitObserver() {
    return evtConnectionLimitChanged.getObserver();
  }

  public EventObserver getStatsUpdatedObserver() {
    return evtStatsUpdated.getObserver();
  }

  public EventObserver getLoggingLevelObserver() {
    return evtLoggingLevelChanged.getObserver();
  }

  private EntityConnectionServerAdmin connectServer(final String serverName) throws RemoteException {
    final long time = System.currentTimeMillis();
    try {
      final EntityConnectionServerAdmin db =
              (EntityConnectionServerAdmin) LocateRegistry.getRegistry(hostName, registryPort).lookup(serverName);
      //call to validate the remote connection
      db.getServerPort();
      LOG.info("ServerMonitor connected to server: {}", serverName);
      return db;
    }
    catch (RemoteException e) {
      LOG.error("Server \"" + serverName + "\" is unreachable", e);
      throw e;
    }
    catch (NotBoundException e) {
      LOG.error(e.getMessage(), e);
      throw new RemoteException("Server " + serverName + " is not bound", e);
    }
    finally {
      LOG.debug("Registry.lookup(\"{}\"): {}", serverName, System.currentTimeMillis() - time);
    }
  }

  private void updateStats() throws RemoteException {
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
    evtStatsUpdated.fire();
  }

  private void startUpdateTimer(final int delay) {
    if (delay <= 0) {
      return;
    }

    if (updateTimer != null) {
      updateTimer.cancel();
    }
    updateTimer = new Timer(true);
    updateTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          if (!shutdown) {
            updateStats();
          }
        }
        catch (RemoteException e) {/**/}
      }
    }, delay, delay);
  }

  private static String removeAdminPrefix(final String serverName) {
    if (serverName.startsWith(RemoteServer.SERVER_ADMIN_PREFIX)) {
      return serverName.substring(RemoteServer.SERVER_ADMIN_PREFIX.length(), serverName.length());
    }

    return serverName;
  }
}

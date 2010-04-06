/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.server.EntityDbServerAdmin;

import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: Bjorn Darri
 * Date: 4.12.2007
 * Time: 17:53:50
 */
public class ServerMonitor {

  private static final Logger log = Util.getLogger(ServerMonitor.class);

  private final Event evtServerShutDown = new Event();
  private final Event evtStatsUpdated = new Event();
  private final Event evtWarningThresholdChanged = new Event();
  private final Event evtConnectionTimeoutChanged = new Event();

  private final String hostName;
  private final String serverName;
  private final EntityDbServerAdmin server;

  private final Timer updateTimer;
  private final DatabaseMonitor databaseMonitor;
  private final ClientMonitor clientMonitor;
  private final UserMonitor userMonitor;

  private int connectionCount = 0;
  private boolean shutdown = false;

  private String memoryUsage;
  private final DefaultListModel domainListModel = new DefaultListModel();
  private final XYSeries connectionRequestsPerSecond = new XYSeries("Service requests per second");
  private final XYSeries warningTimeExceededSecond = new XYSeries("Service calls exceeding warning time per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();

  public ServerMonitor(final String hostName, final String serverName) throws RemoteException {
    this.hostName = hostName;
    this.serverName = serverName;
    this.server = connectServer(serverName);
    connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    connectionRequestsPerSecondCollection.addSeries(warningTimeExceededSecond);
    databaseMonitor = new DatabaseMonitor(server);
    clientMonitor = new ClientMonitor(server);
    userMonitor = new UserMonitor(server);
    refreshDomainList();
    updateTimer = new Timer(false);
    updateTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          if (!shutdown)
            updateStats();
        }
        catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    }, new Date(), 2000);
  }

  public void shutdown() {
    shutdown = true;
    if (updateTimer != null)
      updateTimer.cancel();
    databaseMonitor.shutdown();
    clientMonitor.shutdown();
    userMonitor.shutdown();
  }

  public EntityDbServerAdmin getServer() {
    return server;
  }

  public String getMemoryUsage() {
    return memoryUsage;
  }

  public int getConnectionCount() {
    return connectionCount;
  }

  public ClientMonitor getClientMonitor() {
    return clientMonitor;
  }

  public DatabaseMonitor getDatabaseMonitor() {
    return databaseMonitor;
  }

  public UserMonitor getUserMonitor() {
    return userMonitor;
  }

  public int getWarningThreshold() throws RemoteException {
    return server.getWarningTimeThreshold();
  }

  public void setWarningThreshold(final int threshold) throws RemoteException {
    server.setWarningTimeThreshold(threshold);
    evtWarningThresholdChanged.fire();
  }

  public int getConnectionTimeout() throws RemoteException {
    return server.getConnectionTimeout();
  }

  public void setConnectionTimeout(final int timeout) throws RemoteException {
    server.setConnectionTimeout(timeout);
    evtConnectionTimeoutChanged.fire();
  }

  public XYSeriesCollection getConnectionRequestsDataSet() {
    return connectionRequestsPerSecondCollection;
  }

  public void performGC() throws RemoteException {
    server.performGC();
  }

  public void loadDomainModel(final URL location, final String domainClassName) throws ClassNotFoundException, RemoteException,
          InstantiationException, IllegalAccessException {
    server.loadDomainModel(location, domainClassName);
  }

  public void refreshDomainList() throws RemoteException {
    domainListModel.clear();
    for (final Map.Entry<String, EntityDefinition> entry : server.getEntityDefinitions().entrySet())
      domainListModel.addElement(entry.getValue());
  }

  public ListModel getDomainListModel() {
    return domainListModel;
  }

  public void shutdownServer() throws RemoteException {
    shutdown();
    try {
      server.shutdown();
    }
    catch (RemoteException e) {
      e.printStackTrace();
    }
    System.out.println("Shutdown Server done");
    evtServerShutDown.fire();
  }

  public String getServerName() {
    return serverName;
  }

  public Event eventConnectionTimeoutChanged() {
    return evtConnectionTimeoutChanged;
  }

  public Event eventServerShutDown() {
    return evtServerShutDown;
  }

  public Event eventStatsUpdated() {
    return evtStatsUpdated;
  }

  public Event eventWarningThresholdChanged() {
    return evtWarningThresholdChanged;
  }

  private EntityDbServerAdmin connectServer(final String serverName) throws RemoteException {
    final long time = System.currentTimeMillis();
    try {
      final EntityDbServerAdmin db =
              (EntityDbServerAdmin) LocateRegistry.getRegistry(hostName).lookup(serverName);
      //call to validate the remote connection
      db.getServerPort();
      System.out.println("ServerMonitor connected to server: " + serverName);
      return db;
    }
    catch (RemoteException e) {
      System.out.println("Server \"" + serverName + "\" is unreachable");
      log.error("Server \"" + serverName + "\" is unreachable");
      throw e;
    }
    catch (NotBoundException e) {
      e.printStackTrace();
      throw new RemoteException("Server " + serverName + " is not bound", e);
    }
    finally {
      log.info("Registry.lookup(\"" + serverName + "\"): " + (System.currentTimeMillis() - time));
    }
  }

  private void updateStats() throws RemoteException {
    final long time = System.currentTimeMillis();
    connectionCount = server.getConnectionCount();
    memoryUsage = server.getMemoryUsage();
    connectionRequestsPerSecond.add(time, server.getRequestsPerSecond());
    warningTimeExceededSecond.add(time, server.getWarningTimeExceededPerSecond());
    evtStatsUpdated.fire();
  }
}

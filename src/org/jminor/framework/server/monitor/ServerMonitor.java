/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 17:53:50
 */
public class ServerMonitor extends DefaultMutableTreeNode {

  private static final Logger log = Util.getLogger(ServerMonitor.class);

  public final Event evtRefresh = new Event("ServerMonitor.evtRefresh");
  public final Event evtServerShuttingDown = new Event("ServerMonitor.evtServerShuttingDown");
  public final Event evtWarningThresholdChanged = new Event("ServerMonitor.evtWarningThresholdChanged");

  private final String hostName;
  private final String serverName;
  private final IEntityDbRemoteServerAdmin server;
  private final Timer updateTimer;

  private final XYSeries connectionRequestsPerSecond = new XYSeries("Service requests per second");
  private final XYSeries warningTimeExceededSecond = new XYSeries("Service calls exceeding warning time per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();

  private boolean shutdown = false;

  public ServerMonitor(final String hostName, final String serverName) throws RemoteException {
    this.hostName = hostName;
    this.serverName = serverName;
    this.server = connectServer(serverName);
    connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    connectionRequestsPerSecondCollection.addSeries(warningTimeExceededSecond);
    refresh();
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

  public void refresh() throws RemoteException {
    removeAllChildren();
    if (!shutdown) {
      add(new ConnectionPoolMonitor(server));
      add(new ClientMonitor(server));
      add(new UserMonitor(server));
    }
  }

  @Override
  public String toString() {
    return "Server: " + getServerName() + " (users: " + getChildCount() + ")";
  }

  public IEntityDbRemoteServerAdmin getServer() {
    return server;
  }

  public int getWarningThreshold() throws RemoteException {
    return server.getWarningTimeThreshold();
  }

  public void setWarningThreshold(final int threshold) throws RemoteException {
    server.setWarningTimeThreshold(threshold);
    evtWarningThresholdChanged.fire();
  }

  public XYSeriesCollection getConnectionRequestsDataSet() {
    return connectionRequestsPerSecondCollection;
  }

  public void performGC() throws RemoteException {
    server.performGC();
  }

  //todo the server monitor is not equipped to handle this at all, but it does shut down the server
  public void shutdownServer() throws RemoteException {
    evtServerShuttingDown.fire();
    shutdown = true;
    updateTimer.cancel();
    ((ConnectionPoolMonitor) getChildAt(0)).shutdown();
    ((ClientMonitor) getChildAt(1)).shutdown();
    ((UserMonitor) getChildAt(2)).shutdown();
    try {
      server.shutdown();
    }
    catch (RemoteException e) {
      e.printStackTrace();
    }
    System.out.println("Shutdown Server done");
  }

  public String getServerName() {
    return serverName;
  }

  private IEntityDbRemoteServerAdmin connectServer(final String serverName) throws RemoteException {
    final long time = System.currentTimeMillis();
    try {
      final IEntityDbRemoteServerAdmin db =
              (IEntityDbRemoteServerAdmin) LocateRegistry.getRegistry(hostName).lookup(serverName);
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
    connectionRequestsPerSecond.add(time, server.getRequestsPerSecond());
    warningTimeExceededSecond.add(time, server.getWarningTimeExceededPerSecond());
  }
}

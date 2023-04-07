/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.properties.PropertyValue;
import is.codion.common.user.User;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import static is.codion.common.NullOrEmpty.nullOrEmpty;

/**
 * A monitor for the EntityServer
 */
public final class EntityServerMonitor {

  private static final int DEFAULT_SERVER_MONITOR_UPDATE_RATE = 5;

  /**
   * Specifies the statistics polling rate for the server monitor, in seconds.
   * Value type: Integer<br>
   * Default value: 5
   */
  public static final PropertyValue<Integer> SERVER_MONITOR_UPDATE_RATE =
          Configuration.integerValue("codion.server.monitor.updateRate", DEFAULT_SERVER_MONITOR_UPDATE_RATE);

  private final Event<String> hostAddedEvent = Event.event();

  private final Collection<HostMonitor> hostMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link EntityServerMonitor}
   * @param hostNames a comma separated list of hostnames to monitor
   * @param registryPort the registry port
   * @param adminUser the admin user
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitor(String hostNames, int registryPort, User adminUser) throws RemoteException {
    if (nullOrEmpty(hostNames)) {
      throw new IllegalArgumentException("No server host names specified for server monitor");
    }
    for (String hostname : hostNames.split(",")) {
      addHost(hostname, registryPort, adminUser);
    }
  }

  /**
   * @return the host monitors for this server
   */
  public Collection<HostMonitor> hostMonitors() {
    return hostMonitors;
  }

  /**
   * Refreshes the servers
   * @throws RemoteException in case of a communication error
   */
  public void refresh() throws RemoteException {
    for (HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.refresh();
    }
  }

  public void setUpdateInterval(Integer interval) {
    for (HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.serverMonitors().forEach(serverMonitor -> {
        serverMonitor.updateIntervalValue().set(interval);
        serverMonitor.databaseMonitor().updateIntervalValue().set(interval);
        serverMonitor.databaseMonitor().connectionPoolMonitor().connectionPoolInstanceMonitors()
                .forEach(poolMonitor -> poolMonitor.updateIntervalValue().set(interval));
        serverMonitor.clientMonitor().updateIntervalValue().set(interval);
      });
    }
  }

  public void clearCharts() {
    for (HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.serverMonitors().forEach(serverMonitor -> {
        serverMonitor.clearStatistics();
        serverMonitor.databaseMonitor().clearStatistics();
        serverMonitor.databaseMonitor().connectionPoolMonitor().connectionPoolInstanceMonitors()
                .forEach(ConnectionPoolMonitor::clearStatistics);
      });
    }
  }

  private void addHost(String hostname, int registryPort, User adminUser) throws RemoteException {
    hostMonitors.add(new HostMonitor(hostname, registryPort, adminUser, SERVER_MONITOR_UPDATE_RATE.get()));
    hostAddedEvent.onEvent(hostname);
  }
}
/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.event.Events;
import is.codion.common.user.User;
import is.codion.common.value.PropertyValue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import static is.codion.common.Util.nullOrEmpty;

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
  public static final PropertyValue<Integer> SERVER_MONITOR_UPDATE_RATE = Configuration.integerValue("codion.server.monitor.updateRate", DEFAULT_SERVER_MONITOR_UPDATE_RATE);

  private final Event<String> hostAddedEvent = Events.event();

  private final Collection<HostMonitor> hostMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link EntityServerMonitor}
   * @param hostNames a comma separated list of hostnames to monitor
   * @param registryPort the registry port
   * @param adminUser the admin user
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitor(final String hostNames, final int registryPort, final User adminUser) throws RemoteException {
    if (nullOrEmpty(hostNames)) {
      throw new IllegalArgumentException("No server host names specified for server monitor");
    }
    for (final String hostname : hostNames.split(",")) {
      addHost(hostname, registryPort, adminUser);
    }
  }

  /**
   * @return the host monitors for this server
   */
  public Collection<HostMonitor> getHostMonitors() {
    return hostMonitors;
  }

  /**
   * Refreshes the servers
   * @throws RemoteException in case of a communication error
   */
  public void refresh() throws RemoteException {
    for (final HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.refresh();
    }
  }

  public void setUpdateInterval(final Integer interval) {
    for (final HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.getServerMonitors().forEach(serverMonitor -> {
        serverMonitor.getUpdateIntervalValue().set(interval);
        serverMonitor.getDatabaseMonitor().getUpdateIntervalValue().set(interval);
        serverMonitor.getDatabaseMonitor().getConnectionPoolMonitor().getConnectionPoolInstanceMonitors()
                .forEach(poolMonitor -> poolMonitor.getUpdateIntervalValue().set(interval));
        serverMonitor.getClientMonitor().getUpdateIntervalValue().set(interval);
      });
    }
  }

  private void addHost(final String hostname, final int registryPort, final User adminUser) throws RemoteException {
    hostMonitors.add(new HostMonitor(hostname, registryPort, adminUser, SERVER_MONITOR_UPDATE_RATE.get()));
    hostAddedEvent.onEvent(hostname);
  }
}
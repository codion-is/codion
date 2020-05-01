/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.Configuration;
import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.rmi.server.ServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.value.PropertyValue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import static org.jminor.common.Util.nullOrEmpty;

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
  public static final PropertyValue<Integer> SERVER_MONITOR_UPDATE_RATE = Configuration.integerValue("jminor.server.monitor.updateRate", DEFAULT_SERVER_MONITOR_UPDATE_RATE);

  private final Event<String> hostAddedEvent = Events.event();

  private final Collection<HostMonitor> hostMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link EntityServerMonitor}
   * @param hostNames a comma separated list of hostnames to monitor
   * @param registryPort the registry port
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitor(final String hostNames, final int registryPort) throws RemoteException {
    this(hostNames, registryPort, Users.parseUser(ServerConfiguration.SERVER_ADMIN_USER.get()));
  }

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
    hostMonitors.add(new HostMonitor(hostname, registryPort, adminUser));
    hostAddedEvent.onEvent(hostname);
  }
}
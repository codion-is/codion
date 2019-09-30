/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.Configuration;
import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.PropertyValue;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.remote.Server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A monitor for the EntityConnectionServer
 */
public final class EntityServerMonitor {

  private static final int DEFAULT_SERVER_MONITOR_UPDATE_RATE = 5;

  /**
   * Specifies the statistics polling rate for the server monitor, in seconds.
   * Value type: Integer<br>
   * Default value: 5
   */
  public static final PropertyValue<Integer> SERVER_MONITOR_UPDATE_RATE = Configuration.integerValue("jminor.server.monitor.updateRate", DEFAULT_SERVER_MONITOR_UPDATE_RATE);

  private static final User ADMIN_USER;

  static {
    ADMIN_USER = User.parseUser(Server.SERVER_ADMIN_USER.get());
  }

  private final Event<String> hostAddedEvent = Events.event();

  private final Collection<HostMonitor> hostMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link EntityServerMonitor}
   * @param hostNames a comma separated list of hostnames to monitor
   * @param registryPort the registry port
   * @throws RemoteException in case of an exception
   */
  public EntityServerMonitor(final String hostNames, final int registryPort) throws RemoteException {
    if (Util.nullOrEmpty(hostNames)) {
      throw new IllegalArgumentException("No server host names specified for server monitor");
    }
    for (final String hostname : hostNames.split(",")) {
      addHost(hostname, registryPort);
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

  private void addHost(final String hostname, final int registryPort) throws RemoteException {
    hostMonitors.add(new HostMonitor(hostname, registryPort, ADMIN_USER));
    hostAddedEvent.fire(hostname);
  }
}
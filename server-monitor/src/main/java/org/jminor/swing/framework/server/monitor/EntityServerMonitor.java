/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.framework.Configuration;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A monitor for the EntityConnectionServer
 */
public final class EntityServerMonitor {

  private static final User ADMIN_USER;

  static {
    final String adminUserString = Configuration.getStringValue(Configuration.SERVER_ADMIN_USER);
    ADMIN_USER = User.parseUser(adminUserString);
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
    for (final String hostname : Arrays.asList(hostNames.split(","))) {
      addHost(hostname, registryPort);
    }
  }

  /**
   * @return the host monitors for this server
   */
  public Collection<HostMonitor> getHostMonitors() {
    return hostMonitors;
  }

  private void addHost(final String hostname, final int registryPort) throws RemoteException {
    hostMonitors.add(new HostMonitor(hostname, registryPort, ADMIN_USER));
    hostAddedEvent.fire(hostname);
  }

  public void refresh() throws RemoteException {
    for (final HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.refresh();
    }
  }
}
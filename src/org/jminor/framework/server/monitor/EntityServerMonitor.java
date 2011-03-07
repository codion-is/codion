/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A monitor for the EntityConnectionServer
 */
public final class EntityServerMonitor {

  private final Event evtHostAdded = Events.event();
  private final Event evtHostRemoved = Events.event();

  private final Collection<HostMonitor> hostMonitors = new ArrayList<HostMonitor>();

  public EntityServerMonitor(final String hostNames, final int[] registryPorts) throws RemoteException {
    if (Util.nullOrEmpty(hostNames)) {
      throw new IllegalArgumentException("No server host names specified for server monitor");
    }
    for (final String hostname : Arrays.asList(hostNames.split(","))) {
      addHost(hostname, registryPorts);
    }
  }

  public void addHost(final String hostname, final int[] registryPorts) throws RemoteException {
    hostMonitors.add(new HostMonitor(hostname, registryPorts));
    evtHostAdded.fire();
  }

  public void removeHost(final HostMonitor hostMonitor) {
    hostMonitors.remove(hostMonitor);
    evtHostRemoved.fire();
  }

  public Collection<HostMonitor> getHostMonitors() {
    return hostMonitors;
  }

  public void refresh() throws RemoteException {
    for (final HostMonitor hostMonitor : hostMonitors) {
      hostMonitor.refresh();
    }
  }
}
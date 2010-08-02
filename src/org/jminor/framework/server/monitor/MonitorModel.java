/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * User: Bjorn Darri<br>
 * Date: 4.12.2007<br>
 * Time: 17:31:09<br>
 */
public final class MonitorModel {

  private final Event evtHostAdded = Events.event();
  private final Event evtHostRemoved = Events.event();

  private final Collection<HostMonitor> hostMonitors = new ArrayList<HostMonitor>();

  public MonitorModel(final String hostNames) throws RemoteException {
    if (hostNames == null || hostNames.isEmpty()) {
      throw new RuntimeException("No server host names specified for server monitor");
    }
    for (final String hostname : Arrays.asList(hostNames.split(","))) {
      addHost(hostname);
    }
  }

  public void addHost(final String hostname) throws RemoteException {
    hostMonitors.add(new HostMonitor(hostname));
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

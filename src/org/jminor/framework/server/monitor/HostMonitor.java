/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.server.RemoteServer;
import org.jminor.framework.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A HostMonitor
 */
public final class HostMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(HostMonitor.class);

  private final Event refreshedEvent = Events.event();
  private final Event serverMonitorRemovedEvent = Events.event();

  private final String hostName;
  private final int[] registryPorts;
  private final Collection<ServerMonitor> serverMonitors = new ArrayList<ServerMonitor>();

  public HostMonitor(final String hostName, final int[] registryPorts) throws RemoteException {
    this.hostName = hostName;
    this.registryPorts = registryPorts.clone();
    refresh();
  }

  public String getHostName() {
    return hostName;
  }

  public void refresh() throws RemoteException {
    for (final int registryPort : registryPorts) {
      for (final String serverName : getRemoteEntityServers(hostName, registryPort)) {
        if (!containsServerMonitor(serverName)) {
          final ServerMonitor serverMonitor = new ServerMonitor(hostName, serverName, registryPort);
          serverMonitor.getServerShutDownObserver().addListener(new EventListener() {
            @Override
            public void eventOccurred() {
              removeServer(serverMonitor);
            }
          });
          serverMonitors.add(serverMonitor);
        }
      }
    }
    refreshedEvent.fire();
  }

  public Collection<ServerMonitor> getServerMonitors() {
    return serverMonitors;
  }

  public EventObserver getServerMonitorRemovedObserver() {
    return serverMonitorRemovedEvent.getObserver();
  }

  public EventObserver getRefreshObserver() {
    return refreshedEvent.getObserver();
  }

  private void removeServer(final ServerMonitor serverMonitor) {
    serverMonitors.remove(serverMonitor);
    serverMonitorRemovedEvent.fire();
  }

  private boolean containsServerMonitor(final String serverName) {
    for (final ServerMonitor serverMonitor : serverMonitors) {
      if (serverMonitor.getServerName().equals(serverName)) {
        return true;
      }
    }

    return false;
  }

  private static List<String> getRemoteEntityServers(final String serverHostName, final int registryPort) {
    final List<String> serverNames = new ArrayList<String>();
    try {
      LOG.debug("HostMonitor locating registry on host: {}, port: {}: ", serverHostName, registryPort);
      final Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
      LOG.debug("HostMonitor located registry: {} on port: {}", registry, registryPort);
      final String[] boundNames = getEntityServers(registry);
      if (boundNames.length == 0) {
        LOG.debug("HostMonitor found no server bound to registry: {} on port: {}", registry, registryPort);
      }
      for (final String name : boundNames) {
        LOG.debug("HostMonitor found server '{}'", name);
        serverNames.add(name);
      }
    }
    catch (RemoteException e) {
      LOG.error(e.getMessage(), e);
    }

    return serverNames;
  }

  private static String[] getEntityServers(final Registry registry) throws RemoteException {
    final List<String> serverNames = new ArrayList<String>();
    final String[] boundNames = registry.list();
    for (final String name : boundNames) {
      if (name.startsWith(RemoteServer.SERVER_ADMIN_PREFIX + Configuration.getValue(Configuration.SERVER_NAME_PREFIX))) {
        serverNames.add(name);
      }
    }

    return serverNames.toArray(new String[serverNames.size()]);
  }
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;

import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: Bjorn Darri<br>
 * Date: 4.12.2007<br>
 * Time: 17:46:40<br>
 */
public class HostMonitor {

  private static final Logger LOG = Util.getLogger(HostMonitor.class);

  private final Event evtRefreshed = new Event();
  private final Event evtServerMonitorRemoved = new Event();

  private final State stLiveUpdate = new State();
  private final String hostName;
  private Collection<ServerMonitor> serverMonitors = new ArrayList<ServerMonitor>();

  public HostMonitor(final String hostName) throws RemoteException {
    this.hostName = hostName;
    refresh();
  }

  public String getHostName() {
    return hostName;
  }

  public void refresh() throws RemoteException {
    for (final String serverName : getEntityDbRemoteServers(hostName)) {
      if (!containsServerMonitor(serverName)) {
        final ServerMonitor serverMonitor = new ServerMonitor(hostName, serverName);
        serverMonitor.eventServerShutDown().addListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            removeServer(serverMonitor);
          }
        });
        serverMonitors.add(serverMonitor);
      }
    }
    evtRefreshed.fire();
  }

  public Collection<ServerMonitor> getServerMonitors() {
    return serverMonitors;
  }

  public boolean isLiveUpdate() {
    return stLiveUpdate.isActive();
  }

  public void setLiveUpdate(final boolean value) {
    stLiveUpdate.setActive(value);
  }

  public Event eventRefreshed() {
    return evtRefreshed;
  }

  public Event eventServerMonitorRemoved() {
    return evtServerMonitorRemoved;
  }

  private void removeServer(final ServerMonitor serverMonitor) {
    serverMonitors.remove(serverMonitor);
    evtServerMonitorRemoved.fire();
  }

  private boolean containsServerMonitor(final String serverName) {
    for (final ServerMonitor serverMonitor : serverMonitors) {
      if (serverMonitor.getServerName().equals(serverName)) {
        return true;
      }
    }

    return false;
  }

  private List<String> getEntityDbRemoteServers(final String serverHostName) {
    final List<String> serverNames = new ArrayList<String>();
    try {
      String message = "HostMonitor locating registry on host: " + serverHostName;
      LOG.debug(message);
      final Registry registry = LocateRegistry.getRegistry(serverHostName);
      message = "HostMonitor located registry: " + registry;
      LOG.debug(message);
      final String[] boundNames = getEntityServers(registry);
      if (boundNames.length == 0) {
        message = "HostMonitor found no server bound in registry: " + registry;
        LOG.debug(message);
      }
      for (final String name : boundNames) {
        message = "HostMonitor found server '" + name + "'";
        LOG.debug(message);
        serverNames.add(name);
      }
    }
    catch (RemoteException e) {
      LOG.error(this, e);
    }

    return serverNames;
  }

  private static String[] getEntityServers(final Registry registry) throws RemoteException {
    final List<String> serverNames = new ArrayList<String>();
    final String[] boundNames = registry.list();
    for (final String name : boundNames) {
      if (name.startsWith((String) Configuration.getValue(Configuration.SERVER_NAME_PREFIX))
              && name.endsWith("-admin")) {
        serverNames.add(name);
      }
    }

    return serverNames.toArray(new String[serverNames.size()]);
  }
}

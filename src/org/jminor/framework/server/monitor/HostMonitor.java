/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 17:46:40
 */
public class HostMonitor {

  private static final Logger log = Util.getLogger(HostMonitor.class);

  public final Event evtRefreshed = new Event("HostMonitor.evtRefreshed");

  public final State stLiveUpdate = new State();

  private final String hostName;
  private Collection<String> serverNames = new ArrayList<String>();

  public HostMonitor(final String hostName) throws RemoteException {
    this.hostName = hostName;
    refresh();
  }

  public String getHostName() {
    return hostName;
  }

  public void refresh() {
    serverNames.clear();
    for (final String serverName : getEntityDbRemoteServers(hostName))
      serverNames.add(serverName);
    evtRefreshed.fire();
  }

  public Collection<String> getServerNames() {
    return serverNames;
  }

  public boolean isLiveUpdate() {
    return stLiveUpdate.isActive();
  }

  public void setLiveUpdate(final boolean value) {
    stLiveUpdate.setActive(value);
  }

  private static String[] getEntityServers(final Registry registry) throws RemoteException {
    final ArrayList<String> ret = new ArrayList<String>();
    final String[] boundNames = registry.list();
    for (final String name : boundNames) {
      if (name.startsWith((String) FrameworkSettings.get().getProperty(FrameworkSettings.SERVER_NAME_PREFIX)))
        ret.add(name);
    }

    return ret.toArray(new String[ret.size()]);
  }

  private List<String> getEntityDbRemoteServers(final String serverHostName) {
    try {
      System.out.println("HostMonitor locating registry on host: " + serverHostName);
      log.info("HostMonitor locating registry on host: " + serverHostName);
      final Registry registry = LocateRegistry.getRegistry(serverHostName);
      final ArrayList<String> ret = new ArrayList<String>();
      System.out.println("HostMonitor located registry: " + registry);
      log.info("HostMonitor located registry: " + registry);
      final String[] boundNames = getEntityServers(registry);
      for (final String name : boundNames) {
        System.out.println("HostMonitor found server \"" + name + "\".");
        log.info("HostMonitor found server \"" + name + "\".");
        ret.add(name);
      }

      return ret;
    }
    catch (RemoteException e) {
      log.error(this, e);
      return new ArrayList<String>();
    }
  }
}

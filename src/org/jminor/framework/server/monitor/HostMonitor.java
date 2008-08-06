/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;

import org.apache.log4j.Logger;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 17:46:40
 */
public class HostMonitor extends DefaultMutableTreeNode {

  private static final Logger log = Util.getLogger(HostMonitor.class);

  public final State stLiveUpdate = new State();

  private final String hostName;

  public HostMonitor(final String hostName) throws RemoteException {
    this.hostName = hostName;
    refresh();
  }

  public void refresh() {
    removeAllChildren();
    for (final String serverName : getEntityDbRemoteServers(hostName)) {
      final ServerMonitor model;
      try {
        model = new ServerMonitor(hostName, serverName);
        model.evtServerShuttingDown.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            refresh();
          }
        });
        add(model);
      }
      catch (RemoteException e) {
        e.printStackTrace();
      }
    }
  }

  public String toString() {
    return "Host: " + hostName + " (servers: " + getChildCount() + ")";
  }

  public boolean isLiveUpdate() {
    return stLiveUpdate.isActive();
  }

  public void setLiveUpdate(final boolean value) {
    stLiveUpdate.setActive(value);
  }

  public void discoverServers() throws RemoteException {
    refresh();
  }

  private static String[] getEntityServers(final Registry registry) throws RemoteException {
    final ArrayList<String> ret = new ArrayList<String>();
    final String[] boundNames = registry.list();
    for (final String name : boundNames) {
      if (name.startsWith(FrameworkConstants.JMINOR_SERVER_NAME_PREFIX)) {
        ret.add(name);
      }
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

  public String getHostName() {
    return hostName;
  }
}

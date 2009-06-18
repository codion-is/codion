/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.Event;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 17:31:09
 */
public class MonitorModel {

  public final Event evtHostAdded = new Event("MonitorModel.evtHostAdded");
  public final Event evtHostRemoved = new Event("MonitorModel.evtHostRemoved");

  private final Collection<String> hostNames = new ArrayList<String>();

  public MonitorModel(final String hostNames) throws RemoteException {
    if (hostNames == null || hostNames.length() == 0)
      throw new RuntimeException("No server host names specified for server monitor");
    this.hostNames.addAll(Arrays.asList(hostNames.split(",")));
    refresh();
  }

  public void addHost(final String newHost) throws RemoteException {
    hostNames.add(newHost);
  }

  public Collection<String> getHostNames() {
    return hostNames;
  }

  public void refresh() throws RemoteException {
    //todo
  }
}

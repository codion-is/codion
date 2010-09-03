/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.framework.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public final class ServerUtil {

  private static final Logger LOG = LoggerFactory.getLogger(ServerUtil.class);

  private ServerUtil() {}

  public static RemoteServer getServer(final String serverHostName, final String serverName) throws RemoteException, NotBoundException {
    final List<RemoteServer> servers = getServers(serverHostName, serverName);
    if (!servers.isEmpty()) {
      return servers.get(0);
    }
    else {
      throw new NotBoundException("No reachable or suitable server found, "
              + serverName + " on " + serverHostName);
    }
  }

  public static List<RemoteServer> getServers(final String hostNames, final String serverNamePrefix) throws RemoteException {
    final List<RemoteServer> servers = new ArrayList<RemoteServer>();
    for (final String serverHostName : hostNames.split(",")) {
      final Registry registry = LocateRegistry.getRegistry(serverHostName);
      final String[] boundNames = registry.list();
      for (final String name : boundNames) {
        System.out.println("Server found: " + name);
        if (name.startsWith(serverNamePrefix)) {
          try {
            final RemoteServer server = checkServer((RemoteServer) registry.lookup(name));
            if (server != null) {
              servers.add(server);
            }
          }
          catch (Exception e) {
            LOG.error("Server \"" + name + "\" is unreachable", e);
          }
        }
      }
    }

    return servers;
  }

  public static RemoteServer checkServer(final RemoteServer server) throws RemoteException {
    final int port = server.getServerPort();
    final String requestedPort = Configuration.getStringValue(Configuration.SERVER_PORT);
    if (requestedPort == null || (!requestedPort.isEmpty() && port == Integer.parseInt(requestedPort))) {
      return server;
    }

    return null;
  }
}

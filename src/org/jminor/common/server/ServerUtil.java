/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A utility class for working with RemoteServer instances.
 */
public final class ServerUtil {

  private static final Logger LOG = LoggerFactory.getLogger(ServerUtil.class);

  private ServerUtil() {}

  /**
   * Initializes a Registry if one is not running
   * @param port the port on which to look for (or create) a registry
   * @throws java.rmi.RemoteException in case of an exception
   */
  public static void initializeRegistry(final int port) throws RemoteException {
    LOG.info("Initializing registry on port: {}", port);
    final Registry localRegistry = getRegistry(port);
    try {
      localRegistry.list();
    }
    catch (Exception e) {
      LOG.info("Trying to locate registry: {}", e.getMessage());
      LOG.info("Creating registry on port: {}", port);
      LocateRegistry.createRegistry(port);
    }
  }

  /**
   * @param port the port on which to look for a registry
   * @return the registry
   * @throws java.rmi.RemoteException in case of an exception
   */
  public static Registry getRegistry(final int port) throws RemoteException {
    return LocateRegistry.getRegistry(port);
  }

  /**
   * Retrieves a RemoteServer from a registry running on the given host, using the
   * given prefix as a criteria.
   * @param serverHostName the name of the host
   * @param serverNamePrefix the server name prefix, an empty string results in all servers being returned
   * @param registryPort the port on which to lookup the registry
   * @param serverPort the required server port, -1 for any port
   * @return the servers having a name with the given prefix
   * @throws RemoteException in case of a remote exception
   * @throws NotBoundException in case no such server is found
   */
  public static RemoteServer getServer(final String serverHostName, final String serverNamePrefix,
                                       final int registryPort, final int serverPort) throws RemoteException, NotBoundException {
    final List<RemoteServer> servers = getServers(serverHostName, serverNamePrefix, registryPort, serverPort);
    if (!servers.isEmpty()) {
      return servers.get(0);
    }
    else {
      throw new NotBoundException("No reachable or suitable server found, " + serverNamePrefix
              + " on host: " + serverHostName + ", port: " + serverPort + ", registryPort: " + registryPort);
    }
  }

  private static List<RemoteServer> getServers(final String hostNames, final String serverNamePrefix,
                                               final int registryPort, final int serverPort) throws RemoteException {
    final List<RemoteServer> servers = new ArrayList<>();
    for (final String serverHostName : hostNames.split(",")) {
      LOG.info("Searching for servers,  host: \"{}\", server name prefix: \"{}\", server port: {}, registry port {}",
              new Object[] {serverHostName, serverNamePrefix, serverPort, registryPort});
      final Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
      for (final String name : registry.list()) {
        LOG.info("Found server \"{}\"", name);
        if (name.startsWith(serverNamePrefix)) {
          try {
            final RemoteServer server = checkServer((RemoteServer) registry.lookup(name), serverPort);
            if (server != null) {
              LOG.info("Adding server \"{}\"", name);
              servers.add(server);
            }
          }
          catch (Exception e) {
            LOG.info("Server \"" + name + "\" is unreachable", e);
          }
        }
      }
      Collections.sort(servers, new ServerComparator());
    }

    return servers;
  }

  private static RemoteServer checkServer(final RemoteServer server, final int requestedPort) throws RemoteException {
    if (!server.connectionsAvailable()) {
      LOG.info("No connections available in server \"{}\"", server);
      return null;
    }
    final int port = server.getServerPort();
    if (requestedPort == -1 || port == requestedPort) {
      return server;
    }
    LOG.info("Server \"{}\" is serving on port {}, requested port was {}", new Object[] {server, port, requestedPort});

    return null;
  }

  private static final class ServerComparator implements Comparator<RemoteServer>, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    @Override
    public int compare(final RemoteServer o1, final RemoteServer o2) {
      try {
        return Integer.valueOf(o1.getServerLoad()).compareTo(o2.getServerLoad());
      }
      catch (RemoteException e) {
        return 1;
      }
    }
  }
}

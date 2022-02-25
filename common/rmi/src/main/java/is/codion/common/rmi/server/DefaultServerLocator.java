/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class DefaultServerLocator implements Server.Locator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultServerLocator.class);

  @Override
  public Registry initializeRegistry(int port) throws RemoteException {
    LOG.info("Initializing registry on port: {}", port);
    Registry localRegistry = LocateRegistry.getRegistry(port);
    try {
      localRegistry.list();
      LOG.info("Registry listing available on port: {}", port);

      return localRegistry;
    }
    catch (Exception e) {
      LOG.info("Trying to locate registry: {}", e.getMessage());
      LOG.info("Creating registry on port: {}", port);
      return LocateRegistry.createRegistry(port);
    }
  }

  @Override
  public <T extends Remote, A extends ServerAdmin> Server<T, A> getServer(String serverHostName,
                                                                          String serverNamePrefix,
                                                                          int registryPort,
                                                                          int requestedServerPort)
          throws RemoteException, NotBoundException {
    List<Server<T, A>> servers = getServers(serverHostName, registryPort, serverNamePrefix, requestedServerPort);
    if (!servers.isEmpty()) {
      return servers.get(0);
    }
    else {
      throw new NotBoundException("'" + serverNamePrefix + "' is not available, see LOG for details. Host: "
              + serverHostName + (requestedServerPort != -1 ? ", port: " + requestedServerPort : "") + ", registryPort: " + registryPort);
    }
  }

  private static <T extends Remote, A extends ServerAdmin> List<Server<T, A>> getServers(String hostNames,
                                                                                         int registryPort,
                                                                                         String serverNamePrefix,
                                                                                         int requestedServerPort)
          throws RemoteException {
    List<Server<T, A>> servers = new ArrayList<>();
    for (String serverHostName : hostNames.split(",")) {
      servers.addAll(getServersOnHost(serverHostName, registryPort, serverNamePrefix, requestedServerPort));
    }
    servers.sort(new ServerComparator<>());

    return servers;
  }

  private static <T extends Remote, A extends ServerAdmin> List<Server<T, A>> getServersOnHost(String serverHostName,
                                                                                               int registryPort,
                                                                                               String serverNamePrefix,
                                                                                               int requestedServerPort)
          throws RemoteException {
    LOG.info("Searching for servers,  host: \"{}\", server name prefix: \"{}\", requested server port: {}, registry port {}",
            serverHostName, serverNamePrefix, requestedServerPort, registryPort);
    List<Server<T, A>> servers = new ArrayList<>();
    Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
    for (String serverName : registry.list()) {
      if (serverName.startsWith(serverNamePrefix)) {
        addIfReachable(serverName, requestedServerPort, registry, servers);
      }
    }

    return servers;
  }

  private static <T extends Remote, A extends ServerAdmin> void addIfReachable(String serverName, int requestedServerPort,
                                                                               Registry registry, List<Server<T, A>> servers) {
    LOG.info("Found server \"{}\"", serverName);
    try {
      Server<T, A> server = getIfReachable((Server<T, A>) registry.lookup(serverName), requestedServerPort);
      if (server != null) {
        LOG.info("Adding server \"{}\"", serverName);
        servers.add(server);
      }
    }
    catch (Exception e) {
      LOG.error("Server \"" + serverName + "\" is unreachable", e);
    }
  }

  private static <T extends Remote, A extends ServerAdmin> Server<T, A> getIfReachable(Server<T, A> server,
                                                                                       int requestedServerPort) throws RemoteException {
    ServerInformation serverInformation = server.getServerInformation();
    if (requestedServerPort != -1 && serverInformation.getServerPort() != requestedServerPort) {
      LOG.error("Server \"{}\" is serving on port {}, requested port was {}",
              serverInformation.getServerName(), serverInformation.getServerPort(), requestedServerPort);
      return null;
    }
    if (server.connectionsAvailable()) {
      return server;
    }
    LOG.error("No connections available in server \"{}\"", serverInformation.getServerName());

    return null;
  }

  private static final class ServerComparator<T extends Remote, A extends ServerAdmin> implements Comparator<Server<T, A>>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(Server<T, A> o1, Server<T, A> o2) {
      try {
        return Integer.compare(o1.getServerLoad(), o2.getServerLoad());
      }
      catch (RemoteException e) {
        return 1;
      }
    }
  }
}

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
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

/**
 * A utility class for working with Server instances.
 */
public final class Servers {

  private static final Logger LOG = LoggerFactory.getLogger(Servers.class);

  private Servers() {}

  /**
   * Initializes a Registry if one is not running
   * @param port the port on which to look for (or create) a registry
   * @return the Registry
   * @throws java.rmi.RemoteException in case of an exception
   */
  public static Registry initializeRegistry(final int port) throws RemoteException {
    LOG.info("Initializing registry on port: {}", port);
    final Registry localRegistry = getRegistry(port);
    try {
      localRegistry.list();
      LOG.info("Registry listing available on port: {}", port);

      return localRegistry;
    }
    catch (final Exception e) {
      LOG.info("Trying to locate registry: {}", e.getMessage());
      LOG.info("Creating registry on port: {}", port);
      return LocateRegistry.createRegistry(port);
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
   * Retrieves a Server from a registry running on the given host, using the
   * given server name prefix as a condition. Returns the first server satisfying the condition.
   * @param serverHostName the name of the host
   * @param serverNamePrefix the server name prefix, an empty string results in all servers being returned
   * @param registryPort the port on which to lookup the registry
   * @param requestedServerPort the required server port, -1 for any port
   * @param <T> the Remote object type served by the server
   * @param <A> the server admin type supplied by the server
   * @return the servers having a name with the given prefix
   * @throws RemoteException in case of a remote exception
   * @throws NotBoundException in case no such server is found
   */
  public static <T extends Remote, A extends ServerAdmin> Server<T, A> getServer(final String serverHostName,
                                                                                 final String serverNamePrefix,
                                                                                 final int registryPort,
                                                                                 final int requestedServerPort)
          throws RemoteException, NotBoundException {
    final List<Server<T, A>> servers = getServers(serverHostName, registryPort, serverNamePrefix, requestedServerPort);
    if (!servers.isEmpty()) {
      return servers.get(0);
    }
    else {
      throw new NotBoundException("'" + serverNamePrefix + "' is not available, see LOG for details. Host: "
              + serverHostName + (requestedServerPort != -1 ? ", port: " + requestedServerPort : "") + ", registryPort: " + registryPort);
    }
  }

  private static <T extends Remote, A extends ServerAdmin> List<Server<T, A>> getServers(final String hostNames,
                                                                                         final int registryPort,
                                                                                         final String serverNamePrefix,
                                                                                         final int requestedServerPort)
          throws RemoteException {
    final List<Server<T, A>> servers = new ArrayList<>();
    for (final String serverHostName : hostNames.split(",")) {
      servers.addAll(getServersOnHost(serverHostName, registryPort, serverNamePrefix, requestedServerPort));
    }
    servers.sort(new ServerComparator<>());

    return servers;
  }

  private static <T extends Remote, A extends ServerAdmin> List<Server<T, A>> getServersOnHost(final String serverHostName,
                                                                                               final int registryPort,
                                                                                               final String serverNamePrefix,
                                                                                               final int requestedServerPort)
          throws RemoteException {
    LOG.info("Searching for servers,  host: \"{}\", server name prefix: \"{}\", requested server port: {}, registry port {}",
            new Object[] {serverHostName, serverNamePrefix, requestedServerPort, registryPort});
    final List<Server<T, A>> servers = new ArrayList<>();
    final Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
    for (final String serverName : registry.list()) {
      if (serverName.startsWith(serverNamePrefix)) {
        addIfReachable(serverName, requestedServerPort, registry, servers);
      }
    }

    return servers;
  }

  private static <T extends Remote, A extends ServerAdmin> void addIfReachable(final String serverName, final int requestedServerPort,
                                                                               final Registry registry, final List<Server<T, A>> servers) {
    LOG.info("Found server \"{}\"", serverName);
    try {
      final Server<T, A> server = getIfReachable((Server<T, A>) registry.lookup(serverName), requestedServerPort);
      if (server != null) {
        LOG.info("Adding server \"{}\"", serverName);
        servers.add(server);
      }
    }
    catch (final Exception e) {
      LOG.error("Server \"" + serverName + "\" is unreachable", e);
    }
  }

  private static <T extends Remote, A extends ServerAdmin> Server<T, A> getIfReachable(final Server<T, A> server,
                                                                                       final int requestedServerPort) throws RemoteException {
    final ServerInformation serverInformation = server.getServerInformation();
    if (requestedServerPort != -1 && serverInformation.getServerPort() != requestedServerPort) {
      LOG.error("Server \"{}\" is serving on port {}, requested port was {}",
              new Object[] {serverInformation.getServerName(), serverInformation.getServerPort(), requestedServerPort});
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
    public int compare(final Server<T, A> o1, final Server<T, A> o2) {
      try {
        return Integer.compare(o1.getServerLoad(), o2.getServerLoad());
      }
      catch (final RemoteException e) {
        return 1;
      }
    }
  }

}

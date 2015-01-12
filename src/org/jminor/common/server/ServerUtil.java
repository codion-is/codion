/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.jminor.common.model.Version;
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
import java.util.UUID;

/**
 * A utility class for working with Server instances.
 */
public final class ServerUtil {

  private static final Logger LOG = LoggerFactory.getLogger(ServerUtil.class);

  private ServerUtil() {}

  /**
   * Instantiates a new ClientInfo
   * @param connectionInfo the connection info
   */
  public static ClientInfo clientInfo(final ConnectionInfo connectionInfo) {
    return clientInfo(connectionInfo, connectionInfo.getUser());
  }

  /**
   * Instantiates a new ClientInfo
   * @param connectionInfo the connection info
   * @param databaseUser the user to use when connecting to the underlying database
   */
  public static ClientInfo clientInfo(final ConnectionInfo connectionInfo, final User databaseUser) {
    return new DefaultClientInfo(connectionInfo, databaseUser);
  }

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
      LOG.info("Registry listing available on port: {}", port);
    }
    catch (final Exception e) {
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
   * Retrieves a Server from a registry running on the given host, using the
   * given server name prefix as a criteria. Returns the first server satisfying the criteria.
   * @param serverHostName the name of the host
   * @param serverNamePrefix the server name prefix, an empty string results in all servers being returned
   * @param registryPort the port on which to lookup the registry
   * @param serverPort the required server port, -1 for any port
   * @return the servers having a name with the given prefix
   * @throws RemoteException in case of a remote exception
   * @throws NotBoundException in case no such server is found
   */
  public static Server getServer(final String serverHostName, final String serverNamePrefix,
                                 final int registryPort, final int serverPort) throws RemoteException, NotBoundException {
    final List<Server> servers = getServers(serverHostName, serverNamePrefix, registryPort, serverPort);
    if (!servers.isEmpty()) {
      return servers.get(0);
    }
    else {
      throw new NotBoundException("No reachable or suitable server found, " + serverNamePrefix
              + " on host: " + serverHostName + ", port: " + serverPort + ", registryPort: " + registryPort);
    }
  }

  private static List<Server> getServers(final String hostNames, final String serverNamePrefix,
                                         final int registryPort, final int serverPort) throws RemoteException {
    final List<Server> servers = new ArrayList<>();
    for (final String serverHostName : hostNames.split(",")) {
      LOG.info("Searching for servers,  host: \"{}\", server name prefix: \"{}\", server port: {}, registry port {}",
              new Object[] {serverHostName, serverNamePrefix, serverPort, registryPort});
      final Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
      for (final String name : registry.list()) {
        LOG.info("Found server \"{}\"", name);
        if (name.startsWith(serverNamePrefix)) {
          try {
            final Server server = checkServer((Server) registry.lookup(name), serverPort);
            if (server != null) {
              LOG.info("Adding server \"{}\"", name);
              servers.add(server);
            }
          }
          catch (final Exception e) {
            LOG.info("Server \"" + name + "\" is unreachable", e);
          }
        }
      }
      Collections.sort(servers, new ServerComparator());
    }

    return servers;
  }

  private static Server checkServer(final Server server, final int requestedPort) throws RemoteException {
    if (!server.connectionsAvailable()) {
      LOG.info("No connections available in server \"{}\"", server);
      return null;
    }
    final int port = server.getServerInfo().getServerPort();
    if (requestedPort == -1 || port == requestedPort) {
      return server;
    }
    LOG.info("Server \"{}\" is serving on port {}, requested port was {}", new Object[] {server, port, requestedPort});

    return null;
  }

  private static final class ServerComparator implements Comparator<Server>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final Server o1, final Server o2) {
      try {
        return Integer.valueOf(o1.getServerLoad()).compareTo(o2.getServerLoad());
      }
      catch (final RemoteException e) {
        return 1;
      }
    }
  }

  private static final class DefaultClientInfo implements ClientInfo, Serializable {

    private static final long serialVersionUID = 1;

    private final ConnectionInfo connectionInfo;
    private final User databaseUser;
    private String clientHost = "unknown";

    /**
     * Instantiates a new ClientInfo
     * @param connectionInfo the connection info
     * @param databaseUser the user to use when connecting to the underlying database
     */
    private DefaultClientInfo(final ConnectionInfo connectionInfo, final User databaseUser) {
      this.connectionInfo = connectionInfo;
      this.databaseUser = databaseUser;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
      return connectionInfo;
    }

    @Override
    public User getUser() {
      return connectionInfo.getUser();
    }

    @Override
    public User getDatabaseUser() {
      return databaseUser;
    }

    @Override
    public UUID getClientID() {
      return connectionInfo.getClientID();
    }

    @Override
    public String getClientTypeID() {
      return connectionInfo.getClientTypeID();
    }

    @Override
    public Version getClientVersion() {
      return connectionInfo.getClientVersion();
    }

    @Override
    public Version getFrameworkVersion() {
      return connectionInfo.getFrameworkVersion();
    }

    @Override
    public String getClientHost() {
      return clientHost;
    }

    @Override
    public void setClientHost(final String clientHost) {
      this.clientHost = clientHost;
    }

    @Override
    public int hashCode() {
      return connectionInfo.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      return this == obj || obj instanceof ClientInfo && connectionInfo.equals(((ClientInfo) obj).getConnectionInfo());
    }

    @Override
    public String toString() {
      final StringBuilder builder = new StringBuilder(connectionInfo.getUser().toString());
      if (databaseUser != null && !connectionInfo.getUser().equals(databaseUser)) {
        builder.append(" (databaseUser: ").append(databaseUser.toString()).append(")");
      }
      builder.append("@").append(clientHost).append(" [").append(connectionInfo.getClientTypeID())
              .append("] - ").append(connectionInfo.getClientID().toString());

      return builder.toString();
    }
  }
}

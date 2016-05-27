/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.Util;
import org.jminor.common.model.User;
import org.jminor.common.model.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
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

  /**
   * The system property key for specifying a ssl truststore
   */
  public static final String JAVAX_NET_NET_TRUSTSTORE = "javax.net.ssl.trustStore";
  private static final Logger LOG = LoggerFactory.getLogger(ServerUtil.class);
  private static final int INPUT_BUFFER_SIZE = 8192;

  private ServerUtil() {}

  /**
   * Instantiates a new ClientInfo
   * @param connectionInfo the connection info
   * @return a new ClientInfo instance
   */
  public static ClientInfo clientInfo(final ConnectionInfo connectionInfo) {
    return clientInfo(connectionInfo, connectionInfo.getUser());
  }

  /**
   * Instantiates a new ClientInfo
   * @param connectionInfo the connection info
   * @param databaseUser the user to use when connecting to the underlying database
   * @return a new ClientInfo instance
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
   * @param <T> the Remote object type served by the server
   * @return the servers having a name with the given prefix
   * @throws RemoteException in case of a remote exception
   * @throws NotBoundException in case no such server is found
   */
  public static <T extends Remote> Server<T> getServer(final String serverHostName, final String serverNamePrefix,
                                                       final int registryPort, final int serverPort)
          throws RemoteException, NotBoundException {
    final List<Server<T>> servers = getServers(serverHostName, serverNamePrefix, registryPort, serverPort);
    if (!servers.isEmpty()) {
      return servers.get(0);
    }
    else {
      throw new NotBoundException("'" + serverNamePrefix + "' is not available, see LOG for details. Host: "
              + serverHostName + (serverPort != -1 ? ", port: " + serverPort : "") + ", registryPort: " + registryPort);
    }
  }

  private static <T extends Remote> List<Server<T>> getServers(final String hostNames, final String serverNamePrefix,
                                         final int registryPort, final int serverPort) throws RemoteException {
    final List<Server<T>> servers = new ArrayList<>();
    for (final String serverHostName : hostNames.split(",")) {
      LOG.info("Searching for servers,  host: \"{}\", server name prefix: \"{}\", server port: {}, registry port {}",
              new Object[] {serverHostName, serverNamePrefix, serverPort, registryPort});
      final Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
      for (final String name : registry.list()) {
        if (name.startsWith(serverNamePrefix)) {
          LOG.info("Found server \"{}\"", name);
          try {
            final Server<T> server = checkServer((Server<T>) registry.lookup(name), serverPort);
            if (server != null) {
              LOG.info("Adding server \"{}\"", name);
              servers.add(server);
            }
          }
          catch (final Exception e) {
            LOG.error("Server \"" + name + "\" is unreachable", e);
          }
        }
      }
      Collections.sort(servers, new ServerComparator());
    }

    return servers;
  }

  /**
   * Reads the trust store specified by "javax.net.ssl.trustStore" from the classpath, copies it
   * to a temporary file and sets the trust store property so that it points to that temporary file.
   * If the trust store file specified is not found on the classpath this method has no effect.
   * @param temporaryFileNamePrefix the prefix to use for the temporary filename
   */
  public static void resolveTrustStoreFromClasspath(final String temporaryFileNamePrefix) {
    final String value = System.getProperty(JAVAX_NET_NET_TRUSTSTORE);
    if (Util.nullOrEmpty(value)) {
      LOG.debug("No trust store specified via {}", JAVAX_NET_NET_TRUSTSTORE);
      return;
    }
    FileOutputStream out = null;
    InputStream in = null;
    try {
      final ClassLoader loader = Util.class.getClassLoader();
      in = loader.getResourceAsStream(value);
      if (in == null) {
        LOG.debug("Specified trust store not found on classpath: {}", value);
        return;
      }
      final File file = File.createTempFile(temporaryFileNamePrefix, "tmp");
      file.deleteOnExit();
      out = new FileOutputStream(file);
      final byte[] buf = new byte[INPUT_BUFFER_SIZE];
      int br = in.read(buf);
      while (br > 0) {
        out.write(buf, 0, br);
        br = in.read(buf);
      }
      LOG.debug("Classpath trust store resolved to file: {} -> {}", JAVAX_NET_NET_TRUSTSTORE, file);

      System.setProperty(JAVAX_NET_NET_TRUSTSTORE, file.getPath());
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      Util.closeSilently(out, in);
    }
  }

  private static <T extends Remote> Server<T> checkServer(final Server<T> server, final int requestedPort) throws RemoteException {
    final Server.ServerInfo serverInfo = server.getServerInfo();
    if (requestedPort != -1 && serverInfo.getServerPort() != requestedPort) {
      LOG.error("Server \"{}\" is serving on port {}, requested port was {}",
            new Object[] {serverInfo.getServerName(), serverInfo.getServerPort(), requestedPort});
      return null;
    }
    if (server.connectionsAvailable()) {
      return server;
    }
    LOG.error("No connections available in server \"{}\"", serverInfo.getServerName());

    return null;
  }

  private static final class ServerComparator<T extends Remote> implements Comparator<Server<T>>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final Server<T> o1, final Server<T> o2) {
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
              .append(connectionInfo.getClientVersion() != null ? "-" + connectionInfo.getClientVersion() : "")
              .append("] - ").append(connectionInfo.getClientID().toString());

      return builder.toString();
    }
  }
}

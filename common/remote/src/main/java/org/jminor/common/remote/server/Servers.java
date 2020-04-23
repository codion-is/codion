/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.MethodLogger;
import org.jminor.common.Util;
import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * A utility class for working with Server instances.
 */
public final class Servers {

  private static final Logger LOG = LoggerFactory.getLogger(Servers.class);
  private static final int INPUT_BUFFER_SIZE = 8192;

  private Servers() {}

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @return a new RemoteClient instance
   */
  public static RemoteClient remoteClient(final ConnectionRequest connectionRequest) {
    return remoteClient(connectionRequest, connectionRequest.getUser());
  }

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @param databaseUser the user to use when connecting to the underlying database
   * @return a new RemoteClient instance
   */
  public static RemoteClient remoteClient(final ConnectionRequest connectionRequest, final User databaseUser) {
    return new DefaultRemoteClient(connectionRequest, databaseUser);
  }

  /**
   * Instantiates a new RemoteClient based on the given client
   * but with the specified database user
   * @param remoteClient the remote client to copy
   * @param databaseUser the database user to use
   * @return a new RemoteClient instance
   */
  public static RemoteClient remoteClient(final RemoteClient remoteClient, final User databaseUser) {
    final RemoteClient client = remoteClient(remoteClient.getConnectionRequest(), databaseUser);
    client.setClientHost(remoteClient.getClientHost());

    return client;
  }

  /**
   * Instantiates a new ClientLog instance.
   * @param clientId the ID of the client this log represents
   * @param connectionCreationDate the date and time this client connection was created
   * @param entries the log entries
   * @return a new ClientLog instance
   */
  public static ClientLog clientLog(final UUID clientId, final LocalDateTime connectionCreationDate,
                                    final List<MethodLogger.Entry> entries) {
    return new DefaultClientLog(clientId, connectionCreationDate, entries);
  }

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
  public static <T extends Remote, A extends Remote> Server<T, A> getServer(final String serverHostName,
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

  /**
   * Reads the trust store specified by "javax.net.ssl.trustStore" from the classpath, copies it
   * to a temporary file and sets the trust store property so that it points to that temporary file.
   * If the trust store file specified is not found on the classpath this method has no effect.
   * @param temporaryFileNamePrefix the prefix to use for the temporary filename
   * @see ServerConfiguration#TRUSTSTORE
   */
  public static void resolveTrustStoreFromClasspath(final String temporaryFileNamePrefix) {
    final String value = ServerConfiguration.TRUSTSTORE.get();
    if (nullOrEmpty(value)) {
      LOG.debug("No trust store specified via {}", Server.JAVAX_NET_TRUSTSTORE);
      return;
    }
    try (final InputStream inputStream = Util.class.getClassLoader().getResourceAsStream(value)) {
      if (inputStream == null) {
        LOG.debug("Specified trust store not found on classpath: {}", value);
        return;
      }
      final File file = File.createTempFile(temporaryFileNamePrefix, "tmp");
      Files.write(file.toPath(), getBytes(inputStream));
      file.deleteOnExit();
      LOG.debug("Classpath trust store written to file: {} -> {}", Server.JAVAX_NET_TRUSTSTORE, file);

      ServerConfiguration.TRUSTSTORE.set(file.getPath());
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static <T extends Remote, A extends Remote> List<Server<T, A>> getServers(final String hostNames,
                                                                                    final int registryPort,
                                                                                    final String serverNamePrefix,
                                                                                    final int requestedServerPort)
          throws RemoteException {
    final List<Server<T, A>> servers = new ArrayList<>();
    for (final String serverHostName : hostNames.split(",")) {
      servers.addAll(getServersOnHost(serverHostName, registryPort, serverNamePrefix, requestedServerPort));
    }
    servers.sort(new ServerComparator());

    return servers;
  }

  private static <T extends Remote, A extends Remote> List<Server<T, A>> getServersOnHost(final String serverHostName,
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

  private static <T extends Remote, A extends Remote> void addIfReachable(final String serverName, final int requestedServerPort,
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

  private static <T extends Remote, A extends Remote> Server<T, A> getIfReachable(final Server<T, A> server,
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

  private static final class ServerComparator<T extends Remote, A extends Remote> implements Comparator<Server<T, A>>, Serializable {
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

  private static byte[] getBytes(final InputStream stream) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final byte[] buffer = new byte[INPUT_BUFFER_SIZE];
    int line;
    while ((line = stream.read(buffer)) != -1) {
      os.write(buffer, 0, line);
    }
    os.flush();

    return os.toByteArray();
  }
}

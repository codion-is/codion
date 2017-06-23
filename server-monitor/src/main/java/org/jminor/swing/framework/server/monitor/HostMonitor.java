/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.Events;
import org.jminor.common.User;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A HostMonitor
 */
public final class HostMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(HostMonitor.class);

  private final Event serverAddedEvent = Events.event();
  private final Event serverRemovedEvent = Events.event();

  private final String hostName;
  private final int registryPort;
  private final User adminUser;
  private final Collection<ServerMonitor> serverMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link HostMonitor}
   * @param hostName the name of the host to monitor
   * @param registryPort the registry port
   * @param adminUser the admin user
   * @throws RemoteException in case of an exception
   */
  public HostMonitor(final String hostName, final int registryPort, final User adminUser) throws RemoteException {
    this.hostName = hostName;
    this.registryPort = registryPort;
    this.adminUser = adminUser;
    refresh();
  }

  /**
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * @return the registry port
   */
  public int getRegistryPort() {
    return registryPort;
  }

  /**
   * Refreshes the servers on this host
   * @throws RemoteException in case of an exception
   */
  public void refresh() throws RemoteException {
    removeUnreachableServers();
    try {
      for (final Server.ServerInfo serverInfo : getEntityServers(hostName, registryPort)) {
        if (!containsServerMonitor(serverInfo.getServerID())) {
          final ServerMonitor serverMonitor = new ServerMonitor(hostName, serverInfo, registryPort, adminUser);
          serverMonitor.addServerShutDownListener(() -> removeServer(serverMonitor));
          addServer(serverMonitor);
        }
      }
    }
    catch (final ServerException.AuthenticationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the server monitors for this host
   */
  public Collection<ServerMonitor> getServerMonitors() {
    return serverMonitors;
  }

  /**
   * @param listener a listener notified when a server is added to this monitor
   */
  public void addServerAddedListener(final EventInfoListener<ServerMonitor> listener) {
    serverAddedEvent.addInfoListener(listener);
  }

  /**
   * @param listener a listener notified when a server is removed from this monitor
   */
  public void addServerRemovedListener(final EventInfoListener<ServerMonitor> listener) {
    serverRemovedEvent.addInfoListener(listener);
  }

  private void addServer(final ServerMonitor serverMonitor) {
    serverMonitors.add(serverMonitor);
    serverAddedEvent.fire(serverMonitor);
  }

  private void removeServer(final ServerMonitor serverMonitor) {
    serverMonitors.remove(serverMonitor);
    serverRemovedEvent.fire(serverMonitor);
  }

  private boolean containsServerMonitor(final UUID serverID) {
    for (final ServerMonitor serverMonitor : serverMonitors) {
      if (serverMonitor.getServerInfo().getServerID().equals(serverID)) {
        return true;
      }
    }

    return false;
  }

  private void removeUnreachableServers() {
    final Collection<ServerMonitor> monitors = new ArrayList<>(serverMonitors);
    for (final ServerMonitor monitor : monitors) {
      if (!monitor.isServerReachable()) {
        removeServer(monitor);
      }
    }
  }

  private static List<Server.ServerInfo> getEntityServers(final String serverHostName, final int registryPort) {
    final List<Server.ServerInfo> servers = new ArrayList<>();
    try {
      LOG.debug("HostMonitor locating registry on host: {}, port: {}: ", serverHostName, registryPort);
      final Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
      LOG.debug("HostMonitor located registry: {} on port: {}", registry, registryPort);
      final Collection<String> boundNames = getEntityServers(registry);
      if (boundNames.isEmpty()) {
        LOG.debug("HostMonitor found no server bound to registry: {} on port: {}", registry, registryPort);
      }
      for (final String name : boundNames) {
        LOG.debug("HostMonitor found server '{}'", name);
        final Server server = (Server) LocateRegistry.getRegistry(serverHostName, registryPort).lookup(name);
        servers.add(server.getServerInfo());
      }
    }
    catch (final RemoteException | NotBoundException e) {
      LOG.error(e.getMessage(), e);
    }

    return servers;
  }

  private static Collection<String> getEntityServers(final Registry registry) throws RemoteException {
    final List<String> serverNames = new ArrayList<>();
    final String[] boundNames = registry.list();
    final String serverNamePrefix = Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX);
    for (final String name : boundNames) {
      if (name.startsWith(serverNamePrefix)) {
        serverNames.add(name);
      }
    }

    return serverNames;
  }
}

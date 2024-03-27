/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.event.Event;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.ServerInformation;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A HostMonitor
 */
public final class HostMonitor {

	private static final Logger LOG = LoggerFactory.getLogger(HostMonitor.class);

	private final Event<ServerMonitor> serverAddedEvent = Event.event();
	private final Event<ServerMonitor> serverRemovedEvent = Event.event();

	private final String hostName;
	private final int registryPort;
	private final User adminUser;
	private final int updateRate;
	private final Collection<ServerMonitor> serverMonitors = new ArrayList<>();

	/**
	 * Instantiates a new {@link HostMonitor}
	 * @param hostName the name of the host to monitor
	 * @param registryPort the registry port
	 * @param adminUser the admin user
	 * @param updateRate the initial statistics update rate in seconds
	 * @throws RemoteException in case of an exception
	 */
	public HostMonitor(String hostName, int registryPort, User adminUser, int updateRate) throws RemoteException {
		this.hostName = requireNonNull(hostName);
		this.registryPort = registryPort;
		this.adminUser = requireNonNull(adminUser);
		this.updateRate = updateRate;
		refresh();
	}

	/**
	 * @return the host name
	 */
	public String hostName() {
		return hostName;
	}

	/**
	 * @return the registry port
	 */
	public int registryPort() {
		return registryPort;
	}

	/**
	 * Refreshes the servers on this host
	 * @throws RemoteException in case of an exception
	 */
	public void refresh() throws RemoteException {
		removeUnreachableServers();
		try {
			for (ServerInformation serverInformation : findEntityServers(hostName, registryPort)) {
				if (!containsServerMonitor(serverInformation.serverId())) {
					ServerMonitor serverMonitor = new ServerMonitor(hostName, serverInformation, registryPort, adminUser, updateRate);
					serverMonitor.addServerShutDownListener(() -> removeServer(serverMonitor));
					addServer(serverMonitor);
				}
			}
		}
		catch (ServerAuthenticationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return the server monitors for this host
	 */
	public Collection<ServerMonitor> serverMonitors() {
		return serverMonitors;
	}

	/**
	 * @param listener a listener notified when a server is added to this monitor
	 */
	public void addServerAddedListener(Consumer<ServerMonitor> listener) {
		serverAddedEvent.addDataListener(listener);
	}

	/**
	 * @param listener a listener notified when a server is removed from this monitor
	 */
	public void addServerRemovedListener(Consumer<ServerMonitor> listener) {
		serverRemovedEvent.addDataListener(listener);
	}

	private void addServer(ServerMonitor serverMonitor) {
		serverMonitors.add(serverMonitor);
		serverAddedEvent.accept(serverMonitor);
	}

	private void removeServer(ServerMonitor serverMonitor) {
		serverMonitors.remove(serverMonitor);
		serverRemovedEvent.accept(serverMonitor);
	}

	private boolean containsServerMonitor(UUID serverId) {
		return serverMonitors.stream()
						.anyMatch(serverMonitor -> serverMonitor.serverInformation().serverId().equals(serverId));
	}

	private void removeUnreachableServers() {
		Collection<ServerMonitor> monitors = new ArrayList<>(serverMonitors);
		for (ServerMonitor monitor : monitors) {
			if (!monitor.serverReachable()) {
				removeServer(monitor);
			}
		}
	}

	private static List<ServerInformation> findEntityServers(String serverHostName, int registryPort) {
		List<ServerInformation> servers = new ArrayList<>();
		try {
			LOG.debug("HostMonitor locating registry on host: {}, port: {}: ", serverHostName, registryPort);
			Registry registry = LocateRegistry.getRegistry(serverHostName, registryPort);
			LOG.debug("HostMonitor located registry: {} on port: {}", registry, registryPort);
			Collection<String> boundNames = findEntityServers(registry);
			if (boundNames.isEmpty()) {
				LOG.debug("HostMonitor found no server bound to registry: {} on port: {}", registry, registryPort);
			}
			for (String name : boundNames) {
				LOG.debug("HostMonitor found server '{}'", name);
				Server<?, ?> server = (Server<?, ?>) LocateRegistry.getRegistry(serverHostName, registryPort).lookup(name);
				servers.add(server.serverInformation());
			}
		}
		catch (RemoteException | NotBoundException e) {
			LOG.error(e.getMessage(), e);
		}

		return servers;
	}

	private static Collection<String> findEntityServers(Registry registry) throws RemoteException {
		String[] boundNames = registry.list();
		String serverNamePrefix = ServerConfiguration.SERVER_NAME_PREFIX.get();

		return Arrays.stream(boundNames).filter(name -> name.startsWith(serverNamePrefix)).collect(Collectors.toList());
	}
}

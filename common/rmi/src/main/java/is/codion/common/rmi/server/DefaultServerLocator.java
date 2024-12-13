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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultServerLocator implements Server.Locator {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultServerLocator.class);

	private final String hostName;
	private final String namePrefix;
	private final int registryPort;
	private final int port;

	private DefaultServerLocator(DefaultBuilder builder) {
		this.hostName = requireNonNull(builder.hostName, "hostName must be specified");
		this.namePrefix = requireNonNull(builder.namePrefix, "namePrefix must be specified");
		this.registryPort = builder.registryPort;
		this.port = builder.port;
	}

	@Override
	public <T extends Remote, A extends ServerAdmin> Server<T, A> locateServer()
					throws RemoteException, NotBoundException {
		List<Server<T, A>> servers = findServersOnHost(hostName, registryPort, namePrefix, port);
		if (!servers.isEmpty()) {
			return servers.get(0);
		}

		throw new NotBoundException("'" + namePrefix + "' is not available, see LOG for details. Host: "
						+ hostName + (port != -1 ? ", port: " + port : "") + ", registryPort: " + registryPort);
	}

	static Registry initializeRegistry(int registryPort) throws RemoteException {
		LOG.info("Initializing registry on port: {}", registryPort);
		Registry localRegistry = LocateRegistry.getRegistry(registryPort);
		try {
			localRegistry.list();
			LOG.info("Registry listing available on port: {}", registryPort);

			return localRegistry;
		}
		catch (Exception e) {
			LOG.info("Trying to locate registry: {}", e.getMessage());
			LOG.info("Creating registry on port: {}", registryPort);
			return LocateRegistry.createRegistry(registryPort);
		}
	}

	private static <T extends Remote, A extends ServerAdmin> List<Server<T, A>> findServersOnHost(String hostName,
																																																int registryPort,
																																																String serverNamePrefix,
																																																int requestedServerPort)
					throws RemoteException {
		LOG.info("Searching for servers,  host: \"{}\", server name prefix: \"{}\", requested server port: {}, registry port {}",
						hostName, serverNamePrefix, requestedServerPort, registryPort);
		List<Server<T, A>> servers = new ArrayList<>();
		Registry registry = LocateRegistry.getRegistry(hostName, registryPort);
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
			LOG.error("Server \"{}\" is unreachable", serverName, e);
		}
	}

	private static <T extends Remote, A extends ServerAdmin> @Nullable Server<T, A> getIfReachable(Server<T, A> server,
																																																 int requestedServerPort) throws RemoteException {
		ServerInformation serverInformation = server.serverInformation();
		if (requestedServerPort != -1 && serverInformation.serverPort() != requestedServerPort) {
			LOG.warn("Server \"{}\" is serving on port {}, requested port was {}",
							serverInformation.serverName(), serverInformation.serverPort(), requestedServerPort);
			return null;
		}
		if (server.connectionsAvailable()) {
			return server;
		}
		LOG.warn("No connections available in server \"{}\"", serverInformation.serverName());

		return null;
	}

	static final class DefaultBuilder implements Server.Locator.Builder {

		private String hostName = ServerConfiguration.RMI_SERVER_HOSTNAME.getOrThrow();
		private String namePrefix = ServerConfiguration.SERVER_NAME_PREFIX.getOrThrow();
		private int registryPort = ServerConfiguration.REGISTRY_PORT.getOrThrow();
		private int port = ServerConfiguration.SERVER_PORT.getOrThrow();

		@Override
		public Builder hostName(String hostName) {
			this.hostName = requireNonNull(hostName);
			return this;
		}

		@Override
		public Builder namePrefix(String namePrefix) {
			this.namePrefix = requireNonNull(namePrefix);
			return this;
		}

		@Override
		public Builder registryPort(int registryPort) {
			this.registryPort = registryPort;
			return this;
		}

		@Override
		public Builder port(int port) {
			this.port = port;
			return this;
		}

		@Override
		public Server.Locator build() {
			return new DefaultServerLocator(this);
		}
	}
}

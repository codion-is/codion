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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.common.utilities.Text.nullOrEmpty;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Configuration values for a {@link EntityServer}.
 */
final class DefaultEntityServerConfiguration implements EntityServerConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityServerConfiguration.class);

	private final ServerConfiguration serverConfiguration;

	private final Database database;
	private final User adminUser;
	private final boolean methodTracing;
	private final int idleConnectionTimeout;
	private final String connectionPoolFactory;
	private final Collection<String> domainClasses;
	private final Collection<User> connectionPoolUsers;
	private final Map<String, Integer> clientTypeIdleConnectionTimeouts;

	DefaultEntityServerConfiguration(DefaultEntityServerConfiguration.DefaultBuilder builder) {
		this.serverConfiguration = requireNonNull(builder.serverConfigurationBuilder.build());
		this.database = builder.database;
		this.adminUser = builder.adminUser;
		this.methodTracing = builder.methodTracing;
		this.idleConnectionTimeout = builder.idleConnectionTimeout;
		this.connectionPoolFactory = builder.connectionPoolFactory;
		this.domainClasses = unmodifiableCollection(builder.domainClasses);
		this.connectionPoolUsers = unmodifiableCollection(builder.connectionPoolUsers);
		this.clientTypeIdleConnectionTimeouts = unmodifiableMap(builder.clientTypeIdleConnectionTimeouts);
	}

	@Override
	public String serverName() {
		return serverConfiguration.serverName();
	}

	@Override
	public int port() {
		return serverConfiguration.port();
	}

	@Override
	public Collection<String> auxiliaryServerFactory() {
		return serverConfiguration.auxiliaryServerFactory();
	}

	@Override
	public boolean sslEnabled() {
		return serverConfiguration.sslEnabled();
	}

	@Override
	public Optional<RMIClientSocketFactory> rmiClientSocketFactory() {
		return serverConfiguration.rmiClientSocketFactory();
	}

	@Override
	public Optional<RMIServerSocketFactory> rmiServerSocketFactory() {
		return serverConfiguration.rmiServerSocketFactory();
	}

	@Override
	public Optional<String> objectInputFilterFactory() {
		return serverConfiguration.objectInputFilterFactory();
	}

	@Override
	public boolean objectInputFilterFactoryRequired() {
		return serverConfiguration.objectInputFilterFactoryRequired();
	}

	@Override
	public int connectionMaintenanceInterval() {
		return serverConfiguration.connectionMaintenanceInterval();
	}

	@Override
	public int registryPort() {
		return serverConfiguration.registryPort();
	}

	@Override
	public int adminPort() {
		return serverConfiguration.adminPort();
	}

	@Override
	public int connectionLimit() {
		return serverConfiguration.connectionLimit();
	}

	@Override
	public Database database() {
		return database;
	}

	@Override
	public User adminUser() {
		return adminUser;
	}

	@Override
	public boolean methodTracing() {
		return methodTracing;
	}

	@Override
	public int idleConnectionTimeout() {
		return idleConnectionTimeout;
	}

	@Override
	public String connectionPoolFactory() {
		return connectionPoolFactory;
	}

	@Override
	public Collection<String> domainClasses() {
		return domainClasses;
	}

	@Override
	public Collection<User> connectionPoolUsers() {
		return connectionPoolUsers;
	}

	@Override
	public Map<String, Integer> clientTypeIdleConnectionTimeouts() {
		return clientTypeIdleConnectionTimeouts;
	}

	static EntityServerConfiguration.Builder builderFromSystemProperties() {
		Builder builder = new DefaultBuilder(SERVER_PORT.getOrThrow(), REGISTRY_PORT.getOrThrow())
						.auxiliaryServerFactory(Text.parseCSV(AUXILIARY_SERVER_FACTORIES.get()))
						.sslEnabled(SSL_ENABLED.getOrThrow())
						.adminPort(ADMIN_PORT.getOrThrow())
						.connectionLimit(CONNECTION_LIMIT.getOrThrow())
						.database(Database.instance())
						.domainClasses(Text.parseCSV(DOMAIN_CLASSES.get()))
						.connectionPoolUsers(Text.parseCSV(CONNECTION_POOL_USERS.get()).stream()
										.map(User::parse)
										.collect(toList()));
		Map<String, Integer> clientTypeIdleConnectionTimeoutMap = new HashMap<>();
		for (String clientTimeout : Text.parseCSV(CLIENT_CONNECTION_TIMEOUT.get())) {
			String[] split = clientTimeout.split(":");
			if (split.length < 2) {
				throw new IllegalArgumentException("Expecting a ':' delimiter");
			}
			clientTypeIdleConnectionTimeoutMap.put(split[0], Integer.parseInt(split[1]));
		}
		builder.clientTypeIdleConnectionTimeouts(clientTypeIdleConnectionTimeoutMap);
		String adminUserString = ADMIN_USER.get();
		User adminUser = nullOrEmpty(adminUserString) ? null : User.parse(adminUserString);
		if (adminUser == null) {
			LOG.info("No admin user specified");
		}
		else {
			LOG.info("Admin user: {}", adminUser);
			builder.adminUser(adminUser);
		}

		return builder;
	}

	static final class DefaultBuilder implements Builder {

		private final ServerConfiguration.Builder<?> serverConfigurationBuilder;

		private Database database;
		private User adminUser;
		private boolean methodTracing = METHOD_TRACING.getOrThrow();
		private int idleConnectionTimeout = IDLE_CONNECTION_TIMEOUT.getOrThrow();
		private String connectionPoolFactory = CONNECTION_POOL_FACTORY.get();
		private final Collection<String> domainClasses = new HashSet<>();
		private final Collection<User> connectionPoolUsers = new HashSet<>();
		private final Map<String, Integer> clientTypeIdleConnectionTimeouts = new HashMap<>();

		DefaultBuilder(int serverPort, int registryPort) {
			serverConfigurationBuilder = ServerConfiguration.builder(serverPort, registryPort);
			serverConfigurationBuilder.serverName(() -> {
				if (database == null) {
					throw new IllegalStateException("Database must be set before initializing server name");
				}

				String serverNamePrefix = SERVER_NAME_PREFIX.getOrThrow();
				if (serverNamePrefix.isEmpty()) {
					throw new IllegalArgumentException("serverNamePrefix must not be empty");
				}

				return serverNamePrefix + " " +
								Version.versionString() + "@" + database.name().toUpperCase();
			});
		}

		@Override
		public Builder serverName(Supplier<String> serverName) {
			serverConfigurationBuilder.serverName(serverName);
			return this;
		}

		@Override
		public Builder serverName(String serverName) {
			serverConfigurationBuilder.serverName(serverName);
			return this;
		}

		@Override
		public Builder auxiliaryServerFactory(Collection<String> auxiliaryServerFactory) {
			serverConfigurationBuilder.auxiliaryServerFactory(auxiliaryServerFactory);
			return this;
		}

		@Override
		public Builder sslEnabled(boolean sslEnabled) {
			serverConfigurationBuilder.sslEnabled(sslEnabled);
			return this;
		}

		@Override
		public Builder objectInputFilterFactory(String objectInputFilterFactory) {
			serverConfigurationBuilder.objectInputFilterFactory(objectInputFilterFactory);
			return this;
		}

		@Override
		public Builder objectInputFilterFactoryRequired(boolean objectInputFilterFactoryRequired) {
			serverConfigurationBuilder.objectInputFilterFactoryRequired(objectInputFilterFactoryRequired);
			return this;
		}

		@Override
		public Builder connectionMaintenanceInterval(int connectionMaintenanceInterval) {
			serverConfigurationBuilder.connectionMaintenanceInterval(connectionMaintenanceInterval);
			return this;
		}

		@Override
		public Builder adminPort(int adminPort) {
			serverConfigurationBuilder.adminPort(adminPort);
			return this;
		}

		@Override
		public Builder connectionLimit(int connectionLimit) {
			serverConfigurationBuilder.connectionLimit(connectionLimit);
			return this;
		}

		@Override
		public Builder database(Database database) {
			this.database = requireNonNull(database);
			return this;
		}

		@Override
		public Builder adminUser(User adminUser) {
			this.adminUser = requireNonNull(adminUser);
			return this;
		}

		@Override
		public Builder methodTracing(boolean methodTracing) {
			this.methodTracing = methodTracing;
			return this;
		}

		@Override
		public Builder idleConnectionTimeout(int idleConnectionTimeout) {
			this.idleConnectionTimeout = idleConnectionTimeout;
			return this;
		}

		@Override
		public Builder connectionPoolFactory(String connectionPoolFactory) {
			this.connectionPoolFactory = requireNonNull(connectionPoolFactory);
			return this;
		}

		@Override
		public Builder domainClasses(Collection<String> domainClasses) {
			this.domainClasses.addAll(requireNonNull(domainClasses));
			return this;
		}

		@Override
		public Builder connectionPoolUsers(Collection<User> connectionPoolUsers) {
			this.connectionPoolUsers.addAll(requireNonNull(connectionPoolUsers));
			return this;
		}

		@Override
		public Builder clientTypeIdleConnectionTimeouts(Map<String, Integer> clientTypeIdleConnectionTimeouts) {
			this.clientTypeIdleConnectionTimeouts.putAll(requireNonNull(clientTypeIdleConnectionTimeouts));
			return this;
		}

		@Override
		public EntityServerConfiguration build() {
			return new DefaultEntityServerConfiguration(this);
		}
	}
}

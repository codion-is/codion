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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;


import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.property.PropertyValue;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Configuration values for a {@link EntityServer}.
 * @see #builder(int, int)
 * @see #builderFromSystemProperties()
 */
public interface EntityServerConfiguration extends ServerConfiguration {

	Logger LOG = LoggerFactory.getLogger(EntityServerConfiguration.class);

	int DEFAULT_CONNECTION_LIMIT = -1;

	/**
	 * Specifies maximum number of concurrent connections the server accepts<br>
	 * -1 indicates no limit and 0 indicates a closed server.
	 * Value type: Integer<br>
	 * Default value: -1
	 */
	PropertyValue<Integer> CONNECTION_LIMIT = Configuration.integerValue("codion.server.connectionLimit", DEFAULT_CONNECTION_LIMIT);

	/**
	 * Specifies the class name of the connection pool factory to use.<br>
	 * Value type: String<br>
	 * Default value: none
	 * @see ConnectionPoolFactory
	 */
	PropertyValue<String> CONNECTION_POOL_FACTORY_CLASS = Configuration.stringValue("codion.server.pooling.poolFactoryClass");

	/**
	 * Specifies the default client connection timeout (ms) in a comma separated list.
	 * Example: is.codion.demos.employees.client.ui.EmployeesAppPanel:60000,is.codion.demos.chinook.ui.ChinookAppPanel:120000
	 * Value type: String<br>
	 * Default value: none
	 */
	PropertyValue<String> CLIENT_CONNECTION_TIMEOUT = Configuration.stringValue("codion.server.clientConnectionTimeout");

	/**
	 * The initial connection logging status on the server, either true (on) or false (off)<br>
	 * Value type: Boolean<br>
	 * Default value: false
	 */
	PropertyValue<Boolean> CLIENT_LOGGING = Configuration.booleanValue("codion.server.clientLogging", false);

	/**
	 * Specifies a comma separated list of username:password combinations for which to create connection pools on startup
	 * Example: scott:tiger,john:foo,paul:bar
	 */
	PropertyValue<String> CONNECTION_POOL_USERS = Configuration.stringValue("codion.server.connectionPoolUsers");

	/**
	 * Specifies a comma separated list of domain model class names, these classes must be
	 * available on the server classpath
	 */
	PropertyValue<String> DOMAIN_MODEL_CLASSES = Configuration.stringValue("codion.server.domain.classes");

	/**
	 * @return the Database implementation
	 */
	Database database();

	/**
	 * @return the admin user
	 */
	User adminUser();

	/**
	 * @return the maximum number of concurrent connections, -1 for no limit
	 */
	int connectionLimit();

	/**
	 * @return true if client logging should be enabled on startup
	 */
	boolean clientLogging();

	/**
	 * @return the idle connection timeout
	 */
	int idleConnectionTimeout();

	/**
	 * @return the connection pool factory classname
	 */
	String connectionPoolFactory();

	/**
	 * @return the domain model classes to load on startup
	 */
	Collection<String> domainClassNames();

	/**
	 * @return the users for which to initialize connection pools on startup
	 */
	Collection<User> connectionPoolUsers();

	/**
	 * @return client type specific idle connection timeouts, mapped to clientTypeId
	 */
	Map<String, Integer> clientTypeIdleConnectionTimeouts();

	/**
	 * A Builder for EntityServerConfiguration
	 */
	interface Builder extends ServerConfiguration.Builder<EntityServerConfiguration.Builder> {

		/**
		 * @param database the Database implementation
		 * @return this builder instance
		 */
		Builder database(Database database);

		/**
		 * @param adminUser the admin user
		 * @return this builder instance
		 */
		Builder adminUser(User adminUser);

		/**
		 * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
		 * @return this builder instance
		 */
		Builder connectionLimit(int connectionLimit);

		/**
		 * @param clientLogging if true then client logging is enabled on startup
		 * @return this builder instance
		 */
		Builder clientLogging(boolean clientLogging);

		/**
		 * @param idleConnectionTimeout the idle client connection timeout
		 * @return this builder instance
		 */
		Builder idleConnectionTimeout(int idleConnectionTimeout);

		/**
		 * @param connectionPoolFactory the connection pool factory classname
		 * @return this builder instance
		 */
		Builder connectionPoolFactory(String connectionPoolFactory);

		/**
		 * @param domainClassNames the domain model classes to load on startup
		 * @return this builder instance
		 */
		Builder domainClassNames(Collection<String> domainClassNames);

		/**
		 * @param connectionPoolUsers the users for which to initialize connection pools on startup
		 * @return this builder instance
		 */
		Builder connectionPoolUsers(Collection<User> connectionPoolUsers);

		/**
		 * @param clientTypeIdleConnectionTimeouts client type specific idle connection timeouts, mapped to clientTypeId
		 * @return this builder instance
		 */
		Builder clientTypeIdleConnectionTimeouts(Map<String, Integer> clientTypeIdleConnectionTimeouts);

		/**
		 * @return a new EntityServerConfiguration instance based on this builder
		 */
		EntityServerConfiguration build();
	}

	/**
	 * @param serverPort the server port
	 * @param registryPort the registry port
	 * @return a default entity connection server configuration builder
	 */
	static EntityServerConfiguration.Builder builder(int serverPort, int registryPort) {
		return new DefaultEntityServerConfiguration.DefaultBuilder(serverPort, registryPort);
	}

	/**
	 * Returns a Builder initialized with values from system properties.
	 * @return an entity server configuration builder initialized with values from system properties.
	 */
	static EntityServerConfiguration.Builder builderFromSystemProperties() {
		Builder builder = builder(SERVER_PORT.getOrThrow(), REGISTRY_PORT.getOrThrow())
						.auxiliaryServerFactoryClassNames(Text.parseCommaSeparatedValues(AUXILIARY_SERVER_FACTORY_CLASS_NAMES.get()))
						.sslEnabled(SSL_ENABLED.get())
						.objectInputFilterFactoryClassName(OBJECT_INPUT_FILTER_FACTORY_CLASS_NAME.get())
						.adminPort(requireNonNull(ADMIN_PORT.get(), ADMIN_PORT.toString()))
						.connectionLimit(CONNECTION_LIMIT.get())
						.database(Database.instance())
						.domainClassNames(Text.parseCommaSeparatedValues(DOMAIN_MODEL_CLASSES.get()))
						.connectionPoolUsers(Text.parseCommaSeparatedValues(CONNECTION_POOL_USERS.get()).stream()
										.map(User::parse)
										.collect(toList()))
						.clientLogging(CLIENT_LOGGING.get())
						.idleConnectionTimeout(IDLE_CONNECTION_TIMEOUT.get());
		Map<String, Integer> clientTypeIdleConnectionTimeoutMap = new HashMap<>();
		for (String clientTimeout : Text.parseCommaSeparatedValues(CLIENT_CONNECTION_TIMEOUT.get())) {
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
			EntityServerConfiguration.LOG.info("No admin user specified");
		}
		else {
			EntityServerConfiguration.LOG.info("Admin user: {}", adminUser);
			builder.adminUser(adminUser);
		}

		return builder;
	}
}

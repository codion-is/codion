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
package is.codion.common.rmi.server;

import is.codion.common.Text;
import is.codion.common.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.common.Configuration.*;

/**
 * Configuration values for a {@link Server}.
 * @see #builder(int)
 * @see #builder(int, int)
 * @see #builderFromSystemProperties()
 */
public interface ServerConfiguration {

	/**
	 * The default idle connection timeout in milliseconds.
	 */
	int DEFAULT_IDLE_CONNECTION_TIMEOUT = 120_000;

	/**
	 * The default connection maintenance interval in milliseconds.
	 */
	int DEFAULT_CONNECTION_MAINTENANCE_INTERVAL = 30_000;

	/**
	 * The system property key for specifying a ssl keystore
	 */
	String JAVAX_NET_KEYSTORE = "javax.net.ssl.keyStore";

	/**
	 * The system property key for specifying a ssl keystore password
	 */
	String JAVAX_NET_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

	/**
	 * Localhost
	 */
	String LOCALHOST = "localhost";

	/**
	 * Specifies the rmi server hostname<br>
	 * Note that this is the standard Java property 'java.rmi.server.hostname
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: localhost
	 * </ul>
	 */
	PropertyValue<String> RMI_SERVER_HOSTNAME = stringValue("java.rmi.server.hostname", LOCALHOST);

	/**
	 * Specifies the prefix used when exporting/looking up the Codion server
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: Codion Server
	 * </ul>
	 */
	PropertyValue<String> SERVER_NAME_PREFIX = stringValue("codion.server.namePrefix", "Codion Server");

	/**
	 * The port on which the server is made available to clients.<br>
	 * If specified on the client side, the client will only connect to a server running on this port,
	 * use -1 or no value if the client should connect to any available server
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: -1
	 * </ul>
	 */
	PropertyValue<Integer> SERVER_PORT = integerValue("codion.server.port", -1);

	/**
	 * The port on which to locate the server registry
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link Registry#REGISTRY_PORT} (1099)
	 * </ul>
	 */
	PropertyValue<Integer> REGISTRY_PORT = integerValue("codion.server.registryPort", Registry.REGISTRY_PORT);

	/**
	 * The rmi ssl keystore to use on the classpath, this will be resolved to a temporary file and set
	 * as the javax.net.ssl.keyStore system property on server start
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	PropertyValue<String> CLASSPATH_KEYSTORE = stringValue("codion.server.classpathKeyStore");

	/**
	 * The rmi ssl keystore to use
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 * @see #CLASSPATH_KEYSTORE
	 */
	PropertyValue<String> KEYSTORE = stringValue(JAVAX_NET_KEYSTORE);

	/**
	 * The rmi ssl keystore password to use
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	PropertyValue<String> KEYSTORE_PASSWORD = stringValue(JAVAX_NET_KEYSTORE_PASSWORD);

	/**
	 * The port on which the server should export the remote admin interface
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 0 (admin not exported)
	 * </ul>
	 */
	PropertyValue<Integer> ADMIN_PORT = integerValue("codion.server.admin.port", 0);

	/**
	 * Specifies a username:password combination representing the server admin user<br>
	 * Example: scott:tiger<br>
	 * <ul>
	 * <li>Default value: none
	 * </ul>
	 */
	PropertyValue<String> ADMIN_USER = stringValue("codion.server.admin.user");

	/**
	 * Specifies whether the server should establish connections using a secure sockets layer, true (on) or false (off
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SSL_ENABLED = booleanValue("codion.server.connection.sslEnabled", true);

	/**
	 * Specifies the default idle client connection timeout in milliseconds.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 120.000ms (2 minutes)
	 * </ul>
	 */
	PropertyValue<Integer> IDLE_CONNECTION_TIMEOUT = integerValue("codion.server.idleConnectionTimeout", DEFAULT_IDLE_CONNECTION_TIMEOUT);

	/**
	 * A comma separated list of auxiliary server factories, providing servers to run alongside this Server<br>
	 * Those must extend {@link AuxiliaryServerFactory}
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: none
	 * </ul>
	 * @see AuxiliaryServer
	 */
	PropertyValue<String> AUXILIARY_SERVER_FACTORY_CLASS_NAMES = stringValue("codion.server.auxiliaryServerFactoryClassNames");

	/**
	 * Specifies the {@link ObjectInputFilterFactory} class to use
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: none
	 * </ul>
	 * @see ObjectInputFilterFactory
	 */
	PropertyValue<String> OBJECT_INPUT_FILTER_FACTORY_CLASS_NAME = stringValue("codion.server.objectInputFilterFactoryClassName");

	/**
	 * Specifies the interval between server connection maintenance runs, in milliseconds
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 30_000ms (30 seconds)
	 * </ul>
	 */
	PropertyValue<Integer> CONNECTION_MAINTENANCE_INTERVAL = integerValue("codion.server.connectionMaintenanceInterval", DEFAULT_CONNECTION_MAINTENANCE_INTERVAL);

	/**
	 * @return the server name
	 * @throws IllegalArgumentException in case the supplied server name is null or empty
	 * @see Builder#serverName(Supplier)
	 */
	String serverName();

	/**
	 * @return the server port
	 */
	int port();

	/**
	 * @return the registry port to use
	 */
	int registryPort();

	/**
	 * @return the port on which to make the server admin interface accessible
	 */
	int adminPort();

	/**
	 * @return the class names of auxiliary server factories, providing the servers to run alongside this server
	 */
	Collection<String> auxiliaryServerFactoryClassNames();

	/**
	 * @return true if ssl is enabled
	 */
	boolean sslEnabled();

	/**
	 * @return the rmi client socket factory to use, or an empty Optional if none is specified
	 */
	Optional<RMIClientSocketFactory> rmiClientSocketFactory();

	/**
	 * @return the rmi server socket factory to use, or an empty Optional if none is specified
	 */
	Optional<RMIServerSocketFactory> rmiServerSocketFactory();

	/**
	 * @return the object input filter factory class name, or an empty Optional if none is specified
	 */
	Optional<String> objectInputFilterFactoryClassName();

	/**
	 * @return the interval between server connection maintenance runs, in milliseconds.
	 */
	int connectionMaintenanceInterval();

	/**
	 * @return the maximum number of concurrent connections, -1 for no limit
	 */
	int connectionLimit();

	/**
	 * A Builder for ServerConfiguration
	 * @param <B> the builder type
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * @param adminPort the port on which to make the server admin interface accessible
		 * @return this builder instance
		 */
		B adminPort(int adminPort);

		/**
		 * @param serverName the server name supplier, must return a non-empty string
		 * @return this builder instance
		 */
		B serverName(Supplier<String> serverName);

		/**
		 * @param serverName the server name
		 * @return this builder instance
		 * @throws IllegalArgumentException in case serverName is null or empty
		 */
		B serverName(String serverName);

		/**
		 * @param auxiliaryServerFactoryClassNames the class names of auxiliary server factories,
		 * providing the servers to run alongside this server
		 * @return this builder instance
		 */
		B auxiliaryServerFactoryClassNames(Collection<String> auxiliaryServerFactoryClassNames);

		/**
		 * When set to true this also sets the rmi client/server socket factories.
		 * @param sslEnabled if true then ssl is enabled
		 * @return this builder instance
		 */
		B sslEnabled(boolean sslEnabled);

		/**
		 * @param objectInputFilterFactoryClassName the object input filter factory class name
		 * @return this builder instance
		 */
		B objectInputFilterFactoryClassName(@Nullable String objectInputFilterFactoryClassName);

		/**
		 * @param connectionMaintenanceInterval the interval between server connection maintenance runs, in milliseconds.
		 * @return this builder instance
		 */
		B connectionMaintenanceInterval(int connectionMaintenanceInterval);

		/**
		 * @param connectionLimit the maximum number of concurrent connections, -1 for no limit
		 * @return this builder instance
		 */
		B connectionLimit(int connectionLimit);

		/**
		 * @return a new ServerConfiguration instance based on this builder
		 */
		ServerConfiguration build();
	}

	/**
	 * @param serverPort the server port
	 * @param <B> the builder type
	 * @return a default server configuration
	 */
	static <B extends Builder<B>> Builder<B> builder(int serverPort) {
		return (Builder<B>) new DefaultServerConfiguration.DefaultBuilder(serverPort, Registry.REGISTRY_PORT);
	}

	/**
	 * @param serverPort the server port
	 * @param registryPort the registry port
	 * @param <B> the builder type
	 * @return a default server configuration
	 */
	static <B extends Builder<B>> Builder<B> builder(int serverPort, int registryPort) {
		return (Builder<B>) new DefaultServerConfiguration.DefaultBuilder(serverPort, registryPort);
	}

	/**
	 * Returns a Builder initialized with values from system properties.
	 * @param <B> the builder type
	 * @return a server configuration builder initialized with values from system properties.
	 */
	static <B extends Builder<B>> Builder<B> builderFromSystemProperties() {
		return (Builder<B>) builder(SERVER_PORT.getOrThrow(), REGISTRY_PORT.getOrThrow())
						.auxiliaryServerFactoryClassNames(Text.parseCSV(AUXILIARY_SERVER_FACTORY_CLASS_NAMES.get()))
						.adminPort(ADMIN_PORT.getOrThrow())
						.sslEnabled(SSL_ENABLED.getOrThrow())
						.connectionMaintenanceInterval(CONNECTION_MAINTENANCE_INTERVAL.getOrThrow());
	}
}

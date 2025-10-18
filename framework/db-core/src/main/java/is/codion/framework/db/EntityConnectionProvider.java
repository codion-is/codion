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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Consumer;

import static is.codion.common.Configuration.stringValue;
import static is.codion.common.Configuration.value;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides a managed {@link EntityConnection} instance with automatic reconnection handling.
 * <p>
 * EntityConnectionProvider serves as the primary entry point for database access in Codion applications.
 * It manages connection lifecycle, handles reconnection for both local and remote connections,
 * and provides a consistent interface regardless of the underlying connection type.
 *
 * <h2>Connection Types</h2>
 * <ul>
 *   <li>{@link #CONNECTION_TYPE_LOCAL} - Direct JDBC connection</li>
 *   <li>{@link #CONNECTION_TYPE_REMOTE} - RMI-based remote connection</li>
 *   <li>{@link #CONNECTION_TYPE_HTTP} - HTTP-based remote connection</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * {@snippet :
 * // Local connection
 * EntityConnectionProvider provider = LocalEntityConnectionProvider.builder()
 *     .domainType(DOMAIN)
 *     .user(User.parse("scott:tiger"))
 *     .build();
 *
 * // Remote connection
 * EntityConnectionProvider provider = RemoteEntityConnectionProvider.builder()
 *     .domainType(DOMAIN)
 *     .hostName("localhost")
 *     .port(2223)
 *     .registryPort(1099)
 *     .user(User.parse("scott:tiger"))
 *     .build();
 *
 * // Use the connection
 * try (EntityConnectionProvider connProvider = provider) {
 *     EntityConnection connection = connProvider.connection();
 *     List<Entity> customers = connection.select(all(Customer.TYPE));
 * }
 *}
 *
 * <h2>Configuration-based Creation</h2>
 * {@snippet :
 * // Configure via system properties
 * System.setProperty("codion.client.connectionType", "remote");
 * System.setProperty("codion.client.domainType", "chinook:Chinook");
 *
 * // Create provider based on configuration
 * EntityConnectionProvider provider = EntityConnectionProvider.builder()
 *     .user(User.parse("scott:tiger"))
 *     .build();
 *}
 * @see EntityConnection
 * @see #builder()
 */
public interface EntityConnectionProvider extends AutoCloseable {

	/**
	 * Indicates a local database connection
	 * @see #CLIENT_CONNECTION_TYPE
	 */
	String CONNECTION_TYPE_LOCAL = "local";

	/**
	 * Indicates a remote database connection
	 * @see #CLIENT_CONNECTION_TYPE
	 */
	String CONNECTION_TYPE_REMOTE = "remote";

	/**
	 * Indicates a http database connection
	 * @see #CLIENT_CONNECTION_TYPE
	 */
	String CONNECTION_TYPE_HTTP = "http";

	/**
	 * Specifies the domain type required for a client connection
	 * <ul>
	 * <li>Value type: is.codion.framework.domain.DomainType
	 * <li>Default value: null
	 * </ul>
	 */
	PropertyValue<DomainType> CLIENT_DOMAIN_TYPE = value("codion.client.domainType", DomainType::domainType);

	/**
	 * Specifies whether the client should connect locally, via rmi or http,
	 * accepted values: local, remote, http
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: {@link #CONNECTION_TYPE_LOCAL}
	 * </ul>
	 * @see #CONNECTION_TYPE_LOCAL
	 * @see #CONNECTION_TYPE_REMOTE
	 * @see #CONNECTION_TYPE_HTTP
	 */
	PropertyValue<String> CLIENT_CONNECTION_TYPE = stringValue("codion.client.connectionType", CONNECTION_TYPE_LOCAL);

	/**
	 * Specifies a connection provider description, overriding the default one
	 * which usually provides a hostname or other connection based information.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 * @see #description()
	 */
	PropertyValue<String> DESCRIPTION = stringValue(EntityConnectionProvider.class.getName() + ".description");

	/**
	 * Returns the domain entities this connection is based on
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * Provides a EntityConnection object, is responsible for returning a healthy EntityConnection object,
	 * that is, it must reconnect an invalid connection whether remotely or locally
	 * @return a EntityConnection instance
	 */
	EntityConnection connection();

	/**
	 * Returns a String specifying the type of connection provided by this connection provider
	 * @return a String specifying the type of connection, e.g. "local" or "remote"
	 */
	String connectionType();

	/**
	 * @return a description of the database provider or an empty Optional in case no description is provided
	 * @see #DESCRIPTION
	 */
	Optional<String> description();

	/**
	 * @return true if a connection has been establised and the connection is in a valid state
	 */
	boolean connectionValid();

	/**
	 * @return an observer notified when a connection is established
	 */
	Observer<EntityConnection> connected();

	/**
	 * Closes the underlying connection and performs cleanup if required
	 * @see EntityConnectionProvider.Builder#onClose(Consumer)
	 */
	void close();

	/**
	 * @return the user used by this connection provider
	 */
	User user();

	/**
	 * @return the domain type
	 */
	DomainType domainType();

	/**
	 * @return the UUID identifying this client connection
	 */
	UUID clientId();

	/**
	 * @return the String identifying the client type for this connection provider
	 */
	Optional<String> clientType();

	/**
	 * @return the client version
	 */
	Optional<Version> clientVersion();

	/**
	 * Creates a connection provider builder based on system configuration.
	 * {@snippet :
	 * // Configure connection type and domain
	 * System.setProperty("codion.client.connectionType", "remote");
	 * System.setProperty("codion.client.domainType", "Chinook");
	 *
	 * // Create builder based on configuration
	 * EntityConnectionProvider provider = EntityConnectionProvider.builder()
	 *     .user(User.parse("scott:tiger"))
	 *     .build();
	 *}
	 * @return an unconfigured {@link Builder} instance, based on the
	 * {@link EntityConnectionProvider#CLIENT_CONNECTION_TYPE} and {@link EntityConnectionProvider#CLIENT_DOMAIN_TYPE} configuration values
	 * @throws IllegalStateException in case the required connection provider builder is not available on the classpath
	 * @see EntityConnectionProvider#CLIENT_CONNECTION_TYPE
	 */
	static Builder<?, ?> builder() {
		String clientConnectionType = CLIENT_CONNECTION_TYPE.getOrThrow();
		try {
			return stream(ServiceLoader.load(Builder.class).spliterator(), false)
							.filter(builder -> builder.connectionType().equalsIgnoreCase(clientConnectionType))
							.map(builder -> {
								CLIENT_DOMAIN_TYPE.optional().ifPresent(builder::domain);

								return builder;
							})
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("No connection provider builder available for requested client connection type: " + clientConnectionType));
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}

	/**
	 * Builds {@link EntityConnectionProvider} instances.
	 * {@snippet :
	 * // Local connection with full configuration
	 * EntityConnectionProvider local = LocalEntityConnectionProvider.builder()
	 *     .domain(new MyDomain())
	 *     .user(User.parse("scott:tiger"))
	 *     .clientId(UUID.randomUUID())
	 *     .clientType("MyApplication")
	 *     .clientVersion(Version.parse("1.0.0"))
	 *     .onClose(provider -> System.out.println("Connection closed"))
	 *     .build();
	 *
	 * // Remote connection
	 * EntityConnectionProvider remote = RemoteEntityConnectionProvider.builder()
	 *     .domain(MyDomain.DOMAIN)
	 *     .hostName("server.example.com")
	 *     .port(2223)
	 *     .user(User.parse("scott:tiger"))
	 *     .clientType("MyApplication")
	 *     .build();
	 *}
	 * @param <T> the connection provider type
	 * @param <B> the builder type
	 */
	interface Builder<T extends EntityConnectionProvider, B extends Builder<T, B>> {

		/**
		 * Returns a String specifying the type of connection provided by this connection provider builder
		 * @return a String specifying the type of connection, e.g. "local" or "remote"
		 */
		String connectionType();

		/**
		 * @param user the user
		 * @return this builder instance
		 */
		B user(User user);

		/**
		 * @param domain the domain type to base this connection on
		 * @return this builder instance
		 */
		B domain(DomainType domain);

		/**
		 * @param clientId the UUID identifying this client connection
		 * @return this builder instance
		 */
		B clientId(UUID clientId);

		/**
		 * If no client type is specified, {@link DomainType#name()} is used.
		 * @param clientType a String identifying the client type for this connection provider
		 * @return this builder instance
		 * @see #domain(DomainType)
		 */
		B clientType(String clientType);

		/**
		 * @param clientVersion the client version
		 * @return this builder instance
		 */
		B clientVersion(@Nullable Version clientVersion);

		/**
		 * @param onClose called when this connection provider has been closed
		 * @return this builder instance
		 * @see EntityConnectionProvider#close()
		 */
		B onClose(Consumer<EntityConnectionProvider> onClose);

		/**
		 * Builds a {@link EntityConnectionProvider} instance based on this builder
		 * @return a new {@link EntityConnectionProvider} instance
		 */
		T build();
	}
}

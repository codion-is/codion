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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);

	private final Lock lock = new Lock() {};

	private final User user;
	private final DomainType domainType;
	private final UUID clientId;
	private final String clientType;
	private final @Nullable Version clientVersion;

	private volatile @Nullable EntityConnection entityConnection;
	private volatile @Nullable Entities entities;
	private volatile long validated;

	/**
	 * @param builder the builder
	 */
	protected AbstractEntityConnectionProvider(AbstractBuilder<?, ?> builder) {
		requireNonNull(builder);
		this.user = requireNonNull(builder.user, "A user must be specified");
		this.domainType = requireNonNull(builder.domain, "A domain must be specified");
		this.clientId = builder.clientId;
		this.clientType = builder.clientType == null ? domainType.name() : builder.clientType;
		this.clientVersion = builder.clientVersion;
	}

	@Override
	public final Entities entities() {
		Entities entities = this.entities;
		if (entities == null) {
			synchronized (lock) {
				if (this.entities == null) {
					doConnect();
				}
				entities = this.entities;
			}
		}

		return entities;
	}

	@Override
	public final User user() {
		return user;
	}

	@Override
	public final DomainType domainType() {
		return domainType;
	}

	@Override
	public final UUID clientId() {
		return clientId;
	}

	@Override
	public final String clientType() {
		return clientType;
	}

	@Override
	public final Optional<Version> clientVersion() {
		return Optional.ofNullable(clientVersion);
	}

	@Override
	public EntityConnection connection() {
		return validConnection();
	}

	@Override
	public final void close() {
		EntityConnection connection;
		synchronized (lock) {
			connection = entityConnection;
			entityConnection = null;
		}
		//validate and close without holding the lock
		if (connection != null && valid(connection)) {
			close(connection);
		}
	}

	/**
	 * @return a new valid connection
	 */
	protected abstract EntityConnection connect();

	/**
	 * Closes the given connection
	 * @param connection the connection to be closed
	 */
	protected abstract void close(EntityConnection connection);

	/**
	 * Returns a valid connection or throws an exception in case one can not be established
	 * @return a valid connection
	 */
	protected final EntityConnection validConnection() {
		// the validity check blocks for the duration of any operation in progress
		// on the connection, so it must not be performed while holding the lock
		EntityConnection connection = entityConnection;
		if (connection != null && validated(connection)) {
			return connection;
		}
		synchronized (lock) {
			if (entityConnection != null && entityConnection != connection) {
				return entityConnection; //reconnected by another thread while we were validating
			}
			if (entityConnection != null) {
				LOG.info("Previous connection invalid, reconnecting");
				try {//try to disconnect just in case
					entityConnection.close();
				}
				catch (Exception ignored) {/*ignored*/}
				entityConnection = null;
			}
			doConnect();

			return entityConnection;
		}
	}

	private void doConnect() {
		entityConnection = connect();
		entities = entityConnection.entities();
		validated = System.nanoTime();
	}

	/**
	 * Note that the timestamp records the last successful <i>check</i>, not the last use, so that a connection
	 * dying during continuous use is still rechecked once the interval elapses. Stamping on each call would
	 * refresh the interval indefinitely, leaving a broken connection to fail every operation forever.
	 * @param connection the connection to validate
	 * @return true if the connection was validated within the interval or is valid
	 */
	private boolean validated(EntityConnection connection) {
		long now = System.nanoTime();
		if (now - validated < MILLISECONDS.toNanos(VALIDITY_CHECK_INTERVAL.getOrThrow())) {
			return true;
		}
		if (valid(connection)) {
			validated = now;

			return true;
		}

		return false;
	}

	private static boolean valid(EntityConnection connection) {
		try {
			return connection.connected();
		}
		catch (Exception e) {
			LOG.debug("Connection deemed invalid", e);
			return false;
		}
	}

	/**
	 * An abstract {@link EntityConnectionProvider.Builder}.
	 * @param <T> the {@link EntityConnectionProvider} type built by this builder
	 * @param <B> the builder type
	 */
	public abstract static class AbstractBuilder<T extends EntityConnectionProvider,
					B extends Builder<T, B>> implements Builder<T, B> {

		private final String connectionType;

		private @Nullable User user;
		private @Nullable DomainType domain;
		private UUID clientId = UUID.randomUUID();
		private @Nullable String clientType;
		private @Nullable Version clientVersion;

		/**
		 * @param connectionType a string describing the connection type
		 */
		protected AbstractBuilder(String connectionType) {
			this.connectionType = requireNonNull(connectionType);
		}

		@Override
		public final boolean supports(String connectionType) {
			return this.connectionType.equalsIgnoreCase(requireNonNull(connectionType));
		}

		@Override
		public final B user(User user) {
			this.user = requireNonNull(user);
			return self();
		}

		@Override
		public final B domain(DomainType domain) {
			this.domain = requireNonNull(domain);
			return self();
		}

		@Override
		public final B clientId(UUID clientId) {
			this.clientId = requireNonNull(clientId);
			return self();
		}

		@Override
		public final B clientType(String clientType) {
			this.clientType = requireNonNull(clientType);
			return self();
		}

		@Override
		public final B clientVersion(@Nullable Version clientVersion) {
			this.clientVersion = clientVersion;
			return self();
		}

		private B self() {
			return (B) this;
		}
	}

	private interface Lock {}
}

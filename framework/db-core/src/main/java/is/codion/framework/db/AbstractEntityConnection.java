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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * <p>An abstract self-managing {@link EntityConnection}, one which establishes the underlying connection on
 * demand, validates it before each operation and re-establishes it when it has gone bad. The same instance
 * therefore serves for the lifetime of a client, there being no need to fetch a connection per operation.
 * <p>The transaction state lives on this instance rather than being asked of the underlying connection,
 * which, once it has gone bad, can no longer answer: a connection with an open transaction is never validated
 * nor replaced, so that an operation on one which has gone bad fails loudly instead of the transaction being
 * silently discarded, and {@link #transactionOpen()} answers without a round trip. See
 * {@link #commitTransaction()} and {@link #rollbackTransaction()} for how ending a transaction on a
 * connection which has gone bad behaves.
 * <p>Subclasses supply the transport by implementing {@link #connect()}, and reach the current underlying
 * connection via {@link #delegate()} should they need to serve methods of their own.
 * @see EntityConnection#builder()
 */
public abstract class AbstractEntityConnection implements EntityConnection {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnection.class);

	private final Lock lock = new Lock() {};

	private final User user;
	private final DomainType domainType;
	private final UUID clientId;
	private final String clientType;
	private final @Nullable Version clientVersion;

	private volatile @Nullable EntityConnection connection;
	private volatile @Nullable EntityConnection transactionConnection;
	private volatile @Nullable Entities entities;
	private volatile long validated;

	/**
	 * @param builder the builder
	 */
	protected AbstractEntityConnection(AbstractBuilder<?, ?> builder) {
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
	public final UUID clientId() {
		return clientId;
	}

	@Override
	public final Optional<Version> clientVersion() {
		return Optional.ofNullable(clientVersion);
	}

	/**
	 * Reports the state of the underlying connection without healing it, this being the question
	 * a connection is asked, not a request to establish one.
	 * @return true if the underlying connection has been established and is valid
	 */
	@Override
	public final boolean connected() {
		EntityConnection connection = this.connection;

		return connection != null && valid(connection);
	}

	/**
	 * Closes the underlying connection, rolling back any open transaction with it. This instance
	 * survives, a subsequent operation establishes a new underlying connection.
	 */
	@Override
	public final void close() {
		EntityConnection connection;
		synchronized (lock) {
			connection = this.connection;
			this.connection = null;
			this.transactionConnection = null;
		}
		//validate and close without holding the lock
		if (connection != null && valid(connection)) {
			close(connection);
		}
	}

	/**
	 * Answered from the transaction state maintained by this instance, without consulting,
	 * or establishing, the underlying connection.
	 * @return true if a transaction is open
	 */
	@Override
	public final boolean transactionOpen() {
		return transactionConnection != null;
	}

	@Override
	public final void startTransaction() {
		if (transactionConnection != null) {
			throw new IllegalStateException("Transaction already open");
		}
		EntityConnection connection = delegate();
		connection.startTransaction();
		transactionConnection = connection;
	}

	/**
	 * <p>Rolls back the open transaction. Should the rollback call itself fail, the underlying connection
	 * is discarded and the failure logged rather than propagated - the disconnect rolls the transaction
	 * back, which is the outcome the caller asked for, and the exception which caused the caller to roll
	 * back remains the one being reported. The next operation establishes a fresh connection.
	 */
	@Override
	public final void rollbackTransaction() {
		EntityConnection connection = transactionConnection();
		try {
			connection.rollbackTransaction();
			transactionConnection = null;
		}
		catch (Exception e) {
			LOG.warn("Rollback failed, discarding the connection - the disconnect rolls the transaction back", e);
			discard(connection);
		}
	}

	/**
	 * <p>Commits the open transaction. Should the commit call fail on a connection which is no longer
	 * valid, the connection is discarded - the transaction died with it - and the next operation
	 * establishes a fresh one. A failed commit on a valid connection leaves the transaction open,
	 * the caller decides whether to retry or roll back.
	 */
	@Override
	public final void commitTransaction() {
		EntityConnection connection = transactionConnection();
		try {
			connection.commitTransaction();
			transactionConnection = null;
		}
		catch (Exception e) {
			if (!valid(connection)) {
				discard(connection);
			}
			throw e;
		}
	}

	@Override
	public final QueryCache cacheQueries() {
		return delegate().cacheQueries();
	}

	@Override
	public final <C extends EntityConnection, P, R> @Nullable R execute(FunctionType<C, P, R> functionType) {
		return delegate().execute(functionType);
	}

	@Override
	public final <C extends EntityConnection, P, R> @Nullable R execute(FunctionType<C, P, R> functionType, @Nullable P parameter) {
		return delegate().execute(functionType, parameter);
	}

	@Override
	public final <C extends EntityConnection, P> void execute(ProcedureType<C, P> procedureType) {
		delegate().execute(procedureType);
	}

	@Override
	public final <C extends EntityConnection, P> void execute(ProcedureType<C, P> procedureType, @Nullable P parameter) {
		delegate().execute(procedureType, parameter);
	}

	@Override
	public final Entity.Key insert(Entity entity) {
		return delegate().insert(entity);
	}

	@Override
	public final Entity insertSelect(Entity entity) {
		return delegate().insertSelect(entity);
	}

	@Override
	public final Collection<Entity.Key> insert(Collection<Entity> entities) {
		return delegate().insert(entities);
	}

	@Override
	public final Collection<Entity> insertSelect(Collection<Entity> entities) {
		return delegate().insertSelect(entities);
	}

	@Override
	public final void update(Entity entity) {
		delegate().update(entity);
	}

	@Override
	public final Entity updateSelect(Entity entity) {
		return delegate().updateSelect(entity);
	}

	@Override
	public final void update(Collection<Entity> entities) {
		delegate().update(entities);
	}

	@Override
	public final Collection<Entity> updateSelect(Collection<Entity> entities) {
		return delegate().updateSelect(entities);
	}

	@Override
	public final int update(Update update) {
		return delegate().update(update);
	}

	@Override
	public final void delete(Entity.Key key) {
		delegate().delete(key);
	}

	@Override
	public final void delete(Collection<Entity.Key> keys) {
		delegate().delete(keys);
	}

	@Override
	public final int delete(Condition condition) {
		return delegate().delete(condition);
	}

	@Override
	public final <T> List<T> select(Column<T> column) {
		return delegate().select(column);
	}

	@Override
	public final <T> List<T> select(Column<T> column, Condition condition) {
		return delegate().select(column, condition);
	}

	@Override
	public final <T> List<T> select(Column<T> column, Select select) {
		return delegate().select(column, select);
	}

	@Override
	public final Entity select(Entity.Key key) {
		return delegate().select(key);
	}

	@Override
	public final Entity selectSingle(Condition condition) {
		return delegate().selectSingle(condition);
	}

	@Override
	public final Entity selectSingle(Select select) {
		return delegate().selectSingle(select);
	}

	@Override
	public final Collection<Entity> select(Collection<Entity.Key> keys) {
		return delegate().select(keys);
	}

	@Override
	public final List<Entity> select(Condition condition) {
		return delegate().select(condition);
	}

	@Override
	public final List<Entity> select(Select select) {
		return delegate().select(select);
	}

	@Override
	public final Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) {
		return delegate().dependencies(entities);
	}

	@Override
	public final int count(Count count) {
		return delegate().count(count);
	}

	@Override
	public final <P, R> R report(ReportType<P, R> reportType, @Nullable P parameter) {
		return delegate().report(reportType, parameter);
	}

	@Override
	public final EntityResultIterator iterator(Condition condition) {
		return delegate().iterator(condition);
	}

	@Override
	public final EntityResultIterator iterator(Select select) {
		return delegate().iterator(select);
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + user;
	}

	/**
	 * @return the domain type this connection is based on
	 * @see Builder#domain(DomainType)
	 */
	protected final DomainType domainType() {
		return domainType;
	}

	/**
	 * @return the client type identifying this connection to the server
	 * @see Builder#clientType(String)
	 */
	protected final String clientType() {
		return clientType;
	}

	/**
	 * <p>Returns the underlying connection, establishing it if this is the first use and re-establishing it if
	 * it has gone bad. Called before each operation, hence {@link EntityConnection#VALIDITY_CHECK_INTERVAL}.
	 * <p>Note that a connection with an open transaction is returned as is, without validation, so that an
	 * operation on one which has gone bad fails rather than the transaction being discarded without the
	 * caller ever hearing about it, see {@link #startTransaction()}.
	 * @return the underlying connection
	 */
	protected final EntityConnection delegate() {
		// during an open transaction the connection is used as is - replacing it would silently
		// discard the transaction, and validating it would be a wasted round trip ahead of an
		// operation which must go to this connection regardless of the answer
		EntityConnection transactionConnection = this.transactionConnection;
		if (transactionConnection != null) {
			return transactionConnection;
		}
		// the validity check blocks for the duration of any operation in progress
		// on the connection, so it must not be performed while holding the lock
		EntityConnection connection = this.connection;
		if (connection != null && validated(connection)) {
			return connection;
		}
		synchronized (lock) {
			if (this.connection != null && this.connection != connection) {
				return this.connection; //re-established by another thread while we were validating
			}
			if (this.connection != null) {
				LOG.info("Previous connection invalid, reconnecting");
				try {//try to disconnect just in case
					this.connection.close();
				}
				catch (Exception ignored) {/*ignored*/}
				this.connection = null;
			}
			doConnect();

			return this.connection;
		}
	}

	/**
	 * @return a new underlying connection
	 */
	protected abstract EntityConnection connect();

	/**
	 * Closes the given connection, called when this connection is closed or when a bad
	 * connection is being replaced.
	 * @param connection the connection to close
	 */
	protected void close(EntityConnection connection) {
		connection.close();
	}

	private EntityConnection transactionConnection() {
		EntityConnection connection = transactionConnection;
		if (connection == null) {
			throw new IllegalStateException("Transaction is not open");
		}

		return connection;
	}

	/**
	 * Discards the given connection, along with any transaction state, closing it best effort.
	 * The next operation establishes a fresh connection.
	 */
	private void discard(EntityConnection connection) {
		synchronized (lock) {
			if (this.connection == connection) {
				this.connection = null;
			}
			this.transactionConnection = null;
		}
		try {//try to disconnect just in case
			connection.close();
		}
		catch (Exception ignored) {/*ignored*/}
	}

	private void establish() {
		synchronized (lock) {
			if (connection == null) {
				doConnect();
			}
		}
	}

	private void doConnect() {
		connection = connect();
		entities = connection.entities();
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
	 * An abstract {@link EntityConnection.Builder}.
	 * @param <T> the {@link EntityConnection} type built by this builder
	 * @param <B> the builder type
	 */
	public abstract static class AbstractBuilder<T extends EntityConnection,
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
		public B domain(DomainType domain) {
			this.domain = requireNonNull(domain);
			return self();
		}

		@Override
		public B domain(Domain domain) {
			return domain(requireNonNull(domain).type());
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

		/**
		 * Builds the connection and establishes the underlying one, so that one which can not be
		 * established is reported here rather than on first use, which login flows rely on.
		 * @return a new connected {@link EntityConnection} instance
		 */
		@Override
		public final T build() {
			T connection = createConnection();
			//safe by construction, every connection built by this builder extends AbstractEntityConnection
			((AbstractEntityConnection) connection).establish();

			return connection;
		}

		/**
		 * @return a new, as yet unconnected, {@link AbstractEntityConnection} based on this builder
		 */
		protected abstract T createConnection();

		private B self() {
			return (B) this;
		}
	}

	private interface Lock {}
}

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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * A default abstract implementation of the Database interface.
 */
public abstract class AbstractDatabase implements Database {

	/**
	 * {@code FOR UPDATE}
	 */
	protected static final String FOR_UPDATE = "FOR UPDATE";

	/**
	 * {@code FOR UPDATE NOWAIT}
	 */
	protected static final String FOR_UPDATE_NOWAIT = "FOR UPDATE NOWAIT";

	private static final String FETCH_NEXT = "FETCH NEXT ";
	private static final String ROWS = " ROWS";
	private static final String ONLY = " ONLY";
	private static final String OFFSET = "OFFSET ";
	private static final String LIMIT = "LIMIT ";

	static {
		DriverManager.setLoginTimeout(Database.LOGIN_TIMEOUT.getOrThrow());
	}

	private static @Nullable Database instance;

	private final Map<String, ConnectionPoolWrapper> connectionPools = new HashMap<>();
	private final int validityCheckTimeout = CONNECTION_VALIDITY_CHECK_TIMEOUT.getOrThrow();
	private final	@Nullable Integer transactionIsolation = TRANSACTION_ISOLATION.get();
	private final DefaultQueryCounter queryCounter = new DefaultQueryCounter();
	private final String url;

	private ConnectionProvider connectionProvider = new ConnectionProvider() {};

	/**
	 * Instantiates a new AbstractDatabase.
	 * @param url the jdbc url
	 */
	protected AbstractDatabase(String url) {
		this.url = requireNonNull(url);
	}

	@Override
	public final String url() {
		return url;
	}

	@Override
	public final Connection createConnection() {
		try {
			Connection connection = connectionProvider.connection(url);
			if (transactionIsolation != null) {
				connection.setTransactionIsolation(transactionIsolation);
			}

			return connection;
		}
		catch (SQLException e) {
			throw new DatabaseException(e, errorMessage(e, Operation.OTHER));
		}
	}

	@Override
	public final Connection createConnection(User user) {
		try {
			Connection connection = connectionProvider.connection(user, url);
			if (transactionIsolation != null) {
				connection.setTransactionIsolation(transactionIsolation);
			}

			return connection;
		}
		catch (SQLException e) {
			if (isAuthenticationException(e)) {
				throw new AuthenticationException(errorMessage(e, Operation.OTHER));
			}
			throw new DatabaseException(e, errorMessage(e, Operation.OTHER));
		}
	}

	@Override
	public final boolean connectionValid(Connection connection) {
		requireNonNull(connection);
		try {
			return connection.isValid(validityCheckTimeout);
		}
		catch (SQLException e) {
			return false;
		}
	}

	@Override
	public final QueryCounter queryCounter() {
		return queryCounter;
	}

	@Override
	public final Statistics statistics() {
		return queryCounter.collectAndResetStatistics();
	}

	@Override
	public final ConnectionPoolWrapper createConnectionPool(ConnectionPoolFactory connectionPoolFactory,
																													User poolUser) {
		requireNonNull(connectionPoolFactory);
		requireNonNull(poolUser);
		if (connectionPools.containsKey(poolUser.username().toLowerCase())) {
			throw new IllegalStateException("Connection pool for user " + poolUser.username() + " has already been created");
		}
		ConnectionPoolWrapper connectionPool = connectionPoolFactory.createConnectionPool(this, poolUser);
		connectionPools.put(poolUser.username().toLowerCase(), connectionPool);

		return connectionPool;
	}

	@Override
	public final boolean containsConnectionPool(String username) {
		return connectionPools.containsKey(requireNonNull(username).toLowerCase());
	}

	@Override
	public final ConnectionPoolWrapper connectionPool(String username) {
		ConnectionPoolWrapper connectionPoolWrapper = connectionPools.get(requireNonNull(username).toLowerCase());
		if (connectionPoolWrapper == null) {
			throw new IllegalArgumentException("No connection pool available for user: " + username);
		}

		return connectionPoolWrapper;
	}

	@Override
	public final void closeConnectionPool(String username) {
		ConnectionPoolWrapper connectionPoolWrapper = connectionPools.remove(requireNonNull(username).toLowerCase());
		if (connectionPoolWrapper != null) {
			connectionPoolWrapper.close();
		}
	}

	@Override
	public final void closeConnectionPools() {
		for (ConnectionPoolWrapper pool : connectionPools.values()) {
			closeConnectionPool(pool.user().username());
		}
	}

	@Override
	public final Collection<String> connectionPoolUsernames() {
		return new ArrayList<>(connectionPools.keySet());
	}

	@Override
	public final void connectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = requireNonNull(connectionProvider);
	}

	@Override
	public boolean subqueryRequiresAlias() {
		return false;
	}

	@Override
	public int maximumNumberOfParameters() {
		return Integer.MAX_VALUE;
	}

	@Override
	public String sequenceQuery(String sequenceName) {
		throw new UnsupportedOperationException("Sequence support is not implemented for database: " + getClass().getSimpleName());
	}

	@Override
	public @Nullable String errorMessage(SQLException exception, Operation operation) {
		requireNonNull(exception);
		requireNonNull(operation);

		return exception.getMessage();
	}

	@Override
	public boolean isAuthenticationException(SQLException exception) {
		requireNonNull(exception);
		return false;
	}

	@Override
	public boolean isReferentialIntegrityException(SQLException exception) {
		requireNonNull(exception);
		return false;
	}

	@Override
	public boolean isUniqueConstraintException(SQLException exception) {
		requireNonNull(exception);
		return false;
	}

	@Override
	public boolean isTimeoutException(SQLException exception) {
		requireNonNull(exception);
		return false;
	}

	@Override
	public DatabaseException exception(SQLException exception, Operation operation) {
		requireNonNull(exception);
		requireNonNull(operation);
		if (isUniqueConstraintException(exception)) {
			return new UniqueConstraintException(exception, errorMessage(exception, operation));
		}
		else if (isReferentialIntegrityException(exception)) {
			return new ReferentialIntegrityException(exception, errorMessage(exception, operation), operation);
		}
		else if (isTimeoutException(exception)) {
			return new QueryTimeoutException(exception, errorMessage(exception, operation));
		}

		return new DatabaseException(exception, errorMessage(exception, operation));
	}

	static Database instance() {
		try {
			synchronized (AbstractDatabase.class) {
				String databaseUrl = DATABASE_URL.getOrThrow();
				if (AbstractDatabase.instance == null || !AbstractDatabase.instance.url().equals(databaseUrl)) {
					Database previousInstance = AbstractDatabase.instance;
					//replace the instance
					AbstractDatabase.instance = DatabaseFactory.instance().create(databaseUrl);
					if (previousInstance != null) {
						//cleanup
						previousInstance.closeConnectionPools();
					}
				}

				return AbstractDatabase.instance;
			}
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a limit/offset clause of the form {@code LIMIT {limit} OFFSET {offset}}.
	 * Returns a partial clause if either value is null.
	 * If both values are null, an empty string is returned.
	 * @param limit the limit, may be null
	 * @param offset the offset, may be null
	 * @return a limit/offset clause
	 */
	protected static String createLimitOffsetClause(@Nullable Integer limit, @Nullable Integer offset) {
		StringBuilder builder = new StringBuilder();
		if (limit != null) {
			builder.append(LIMIT).append(limit);
		}
		if (offset != null) {
			builder.append(builder.isEmpty() ? "" : " ").append(OFFSET).append(offset);
		}

		return builder.toString();
	}

	/**
	 * Creates a offset/fetch next clause of the form {@code OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY}.
	 * Returns a partial clause if either value is null.
	 * If both values are null, an empty string is returned.
	 * @param limit the limit, may be null
	 * @param offset the offset, may be null
	 * @return a limit/offset clause
	 */
	protected static String createOffsetFetchNextClause(@Nullable Integer limit, @Nullable Integer offset) {
		StringBuilder builder = new StringBuilder();
		if (offset != null) {
			builder.append(OFFSET).append(offset).append(ROWS);
		}
		if (limit != null) {
			builder.append(builder.isEmpty() ? "" : " ").append(FETCH_NEXT).append(limit).append(ROWS).append(ONLY);
		}

		return builder.toString();
	}

	/**
	 * Removes the given prefixes along with any options and parameters from the given jdbc url.
	 * @param url the url
	 * @param prefixes the prefixes to remove
	 * @return the given url without prefixes, options and parameters
	 */
	protected static String removeUrlPrefixOptionsAndParameters(String url, String... prefixes) {
		String result = url;
		for (String prefix : prefixes) {
			if (url.toLowerCase().startsWith(prefix.toLowerCase())) {
				result = url.substring(prefix.length());
				break;
			}
		}
		if (result.contains(";")) {
			result = result.substring(0, result.indexOf(';'));
		}
		if (result.contains("?")) {
			result = result.substring(0, result.indexOf('?'));
		}

		return result;
	}

	private static final class DefaultQueryCounter implements QueryCounter {

		private static final double THOUSAND = 1000d;

		private final AtomicLong queriesPerSecondTime = new AtomicLong(System.currentTimeMillis());
		private final AtomicInteger queriesPerSecondCounter = new AtomicInteger();
		private final AtomicInteger selectsPerSecondCounter = new AtomicInteger();
		private final AtomicInteger insertsPerSecondCounter = new AtomicInteger();
		private final AtomicInteger updatesPerSecondCounter = new AtomicInteger();
		private final AtomicInteger deletesPerSecondCounter = new AtomicInteger();
		private final AtomicInteger otherPerSecondCounter = new AtomicInteger();

		private final boolean enabled = COUNT_QUERIES.getOrThrow();

		@Override
		public void select() {
			if (enabled) {
				selectsPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void insert() {
			if (enabled) {
				insertsPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void update() {
			if (enabled) {
				updatesPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void delete() {
			if (enabled) {
				deletesPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		@Override
		public void other() {
			if (enabled) {
				otherPerSecondCounter.incrementAndGet();
				queriesPerSecondCounter.incrementAndGet();
			}
		}

		private Database.Statistics collectAndResetStatistics() {
			long currentTime = System.currentTimeMillis();
			double seconds = (currentTime - queriesPerSecondTime.getAndSet(currentTime)) / THOUSAND;
			if (seconds > 0) {
				int queriesPerSecond = (int) (queriesPerSecondCounter.getAndSet(0) / seconds);
				int selectsPerSecond = (int) (selectsPerSecondCounter.getAndSet(0) / seconds);
				int insertsPerSecond = (int) (insertsPerSecondCounter.getAndSet(0) / seconds);
				int deletesPerSecond = (int) (deletesPerSecondCounter.getAndSet(0) / seconds);
				int updatesPerSecond = (int) (updatesPerSecondCounter.getAndSet(0) / seconds);
				int otherPerSecond = (int) (otherPerSecondCounter.getAndSet(0) / seconds);

				return new DefaultDatabaseStatistics(currentTime, queriesPerSecond, selectsPerSecond,
								insertsPerSecond, deletesPerSecond, updatesPerSecond, otherPerSecond);
			}

			return new DefaultDatabaseStatistics();
		}
	}

	/**
	 * A default Database.Statistics implementation.
	 */
	private static final class DefaultDatabaseStatistics implements Database.Statistics, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final long timestamp;
		private final int queriesPerSecond;
		private final int selectsPerSecond;
		private final int insertsPerSecond;
		private final int deletesPerSecond;
		private final int updatesPerSecond;
		private final int otherPerSecond;

		private DefaultDatabaseStatistics() {
			this(0, 0, 0, 0, 0, 0, 0);
		}

		private DefaultDatabaseStatistics(long timestamp, int queriesPerSecond, int selectsPerSecond,
																			int insertsPerSecond, int deletesPerSecond, int updatesPerSecond,
																			int otherPerSecond) {
			this.timestamp = timestamp;
			this.queriesPerSecond = queriesPerSecond;
			this.selectsPerSecond = selectsPerSecond;
			this.insertsPerSecond = insertsPerSecond;
			this.deletesPerSecond = deletesPerSecond;
			this.updatesPerSecond = updatesPerSecond;
			this.otherPerSecond = otherPerSecond;
		}

		@Override
		public int queriesPerSecond() {
			return queriesPerSecond;
		}

		@Override
		public int deletesPerSecond() {
			return deletesPerSecond;
		}

		@Override
		public int insertsPerSecond() {
			return insertsPerSecond;
		}

		@Override
		public int selectsPerSecond() {
			return selectsPerSecond;
		}

		@Override
		public int updatesPerSecond() {
			return updatesPerSecond;
		}

		@Override
		public int otherPerSecond() {
			return otherPerSecond;
		}

		@Override
		public long timestamp() {
			return timestamp;
		}
	}
}

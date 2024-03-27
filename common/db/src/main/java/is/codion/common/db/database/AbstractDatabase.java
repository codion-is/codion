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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
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

	protected static final String FOR_UPDATE = "FOR UPDATE";
	protected static final String FOR_UPDATE_NOWAIT = "FOR UPDATE NOWAIT";

	private static final String FETCH_NEXT = "FETCH NEXT ";
	private static final String ROWS = " ROWS";
	private static final String ONLY = " ONLY";
	private static final String OFFSET = "OFFSET ";
	private static final String LIMIT = "LIMIT ";

	static {
		DriverManager.setLoginTimeout(Database.LOGIN_TIMEOUT.getOrThrow());
	}

	private static Database instance;

	private final Map<String, ConnectionPoolWrapper> connectionPools = new HashMap<>();
	private final int validityCheckTimeout = CONNECTION_VALIDITY_CHECK_TIMEOUT.get();
	private final Integer transactionIsolation = TRANSACTION_ISOLATION.get();
	private final DefaultQueryCounter queryCounter = new DefaultQueryCounter();
	private final String url;

	private ConnectionProvider connectionProvider = new ConnectionProvider() {};

	/**
	 * Instantiates a new AbstractDatabase.
	 * @param url the jdbc url
	 */
	protected AbstractDatabase(String url) {
		this.url = requireNonNull(url, "url");
	}

	@Override
	public final String url() {
		return url;
	}

	@Override
	public final Connection createConnection(User user) throws DatabaseException {
		try {
			Connection connection = connectionProvider.connection(user, url);
			if (transactionIsolation != null) {
				connection.setTransactionIsolation(transactionIsolation);
			}

			return connection;
		}
		catch (SQLException e) {
			if (isAuthenticationException(e)) {
				throw new AuthenticationException(e.getMessage());
			}
			throw new DatabaseException(e, errorMessage(e, Operation.OTHER));
		}
	}

	@Override
	public final boolean connectionValid(Connection connection) {
		requireNonNull(connection, "connection");
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
	public final void createConnectionPool(ConnectionPoolFactory connectionPoolFactory,
																				 User poolUser) throws DatabaseException {
		requireNonNull(connectionPoolFactory, "connectionPoolFactory");
		requireNonNull(poolUser, "poolUser");
		if (connectionPools.containsKey(poolUser.username().toLowerCase())) {
			throw new IllegalStateException("Connection pool for user " + poolUser.username() + " has already been created");
		}
		connectionPools.put(poolUser.username().toLowerCase(), connectionPoolFactory.createConnectionPool(this, poolUser));
	}

	@Override
	public final boolean containsConnectionPool(String username) {
		return connectionPools.containsKey(requireNonNull(username).toLowerCase());
	}

	@Override
	public final ConnectionPoolWrapper connectionPool(String username) {
		ConnectionPoolWrapper connectionPoolWrapper = connectionPools.get(requireNonNull(username, "username").toLowerCase());
		if (connectionPoolWrapper == null) {
			throw new IllegalArgumentException("No connection pool available for user: " + username);
		}

		return connectionPoolWrapper;
	}

	@Override
	public final void closeConnectionPool(String username) {
		ConnectionPoolWrapper connectionPoolWrapper = connectionPools.remove(requireNonNull(username, "username").toLowerCase());
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
		this.connectionProvider = connectionProvider == null ? new ConnectionProvider() {} : connectionProvider;
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
	public String errorMessage(SQLException exception, Operation operation) {
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
	public DatabaseException databaseException(SQLException exception, Operation operation) {
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
				String databaseUrl = DATABASE_URL.get();
				if (AbstractDatabase.instance == null || !AbstractDatabase.instance.url().equals(databaseUrl)) {
					Database previousInstance = AbstractDatabase.instance;
					//replace the instance
					AbstractDatabase.instance = DatabaseFactory.instance().createDatabase(databaseUrl);
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

	protected static String createLimitOffsetClause(Integer limit, Integer offset) {
		/* LIMIT {limit} OFFSET {offset} */
		StringBuilder builder = new StringBuilder();
		if (limit != null) {
			builder.append(LIMIT).append(limit);
		}
		if (offset != null) {
			builder.append(builder.length() == 0 ? "" : " ").append(OFFSET).append(offset);
		}

		return builder.toString();
	}

	protected static String createOffsetFetchNextClause(Integer limit, Integer offset) {
		/* OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY */
		StringBuilder builder = new StringBuilder();
		if (offset != null) {
			builder.append(OFFSET).append(offset).append(ROWS);
		}
		if (limit != null) {
			builder.append(builder.length() == 0 ? "" : " ").append(FETCH_NEXT).append(limit).append(ROWS).append(ONLY);
		}

		return builder.toString();
	}

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

		private final boolean enabled = COUNT_QUERIES.get();

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

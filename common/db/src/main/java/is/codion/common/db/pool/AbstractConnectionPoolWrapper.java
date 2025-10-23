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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import is.codion.common.db.database.ConnectionFactory;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.proxy.ProxyBuilder.ProxyMethod;
import is.codion.common.user.User;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default base implementation of the ConnectionPool wrapper, handling the collection of statistics
 * @param <T> the type representing the actual pool object
 */
public abstract class AbstractConnectionPoolWrapper<T> implements ConnectionPoolWrapper {

	private static final String GET_CONNECTION = "getConnection";
	private static final String CLOSE = "close";

	private static final boolean VALIDATE = VALIDATE_CONNECTIONS_ON_CHECKOUT.getOrThrow();

	/**
	 * The actual connection pool object
	 */
	private final T connectionPool;
	private final ConnectionFactory connectionFactory;
	private final User user;
	private final DefaultConnectionPoolCounter counter;

	/**
	 * Instantiates a new AbstractConnectionPool instance.
	 * @param connectionFactory the connection factory
	 * @param user the connection pool user
	 * @param dataSource the data source
	 * @param poolFactory creates the actual connection pool based on the given data source
	 */
	protected AbstractConnectionPoolWrapper(ConnectionFactory connectionFactory, User user, DataSource dataSource,
																					Function<DataSource, T> poolFactory) {
		this.connectionFactory = requireNonNull(connectionFactory, "connectionFactory");
		this.user = requireNonNull(user, "user");
		this.counter = new DefaultConnectionPoolCounter(this);
		this.connectionPool = requireNonNull(poolFactory, "poolFactory")
						.apply(createDataSourceProxy(requireNonNull(dataSource, "dataSource")));
	}

	@Override
	public final User user() {
		return user;
	}

	@Override
	public final Connection connection(User user) {
		requireNonNull(user, "user");
		checkConnectionPoolCredentials(user);
		long startTime = counter.isCollectCheckOutTimes() ? System.nanoTime() : 0;
		try {
			counter.incrementRequestCounter();

			return validate(fetchConnection());
		}
		catch (SQLException e) {
			counter.incrementFailedRequestCounter();
			throw new DatabaseException(e, "Failed to fetch connection from pool for user: " + user.username());
		}
		finally {
			if (counter.isCollectCheckOutTimes() && startTime > 0L) {
				counter.addCheckOutTime((int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime));
			}
		}
	}

	@Override
	public final void resetStatistics() {
		counter.resetStatistics();
	}

	@Override
	public final boolean isCollectSnapshotStatistics() {
		return counter.isCollectSnapshotStatistics();
	}

	@Override
	public final void setCollectSnapshotStatistics(boolean collectSnapshotStatistics) {
		counter.setCollectSnapshotStatistics(collectSnapshotStatistics);
	}

	@Override
	public final boolean isCollectCheckOutTimes() {
		return counter.isCollectCheckOutTimes();
	}

	@Override
	public final void setCollectCheckOutTimes(boolean collectCheckOutTimes) {
		counter.setCollectCheckOutTimes(collectCheckOutTimes);
	}

	@Override
	public final ConnectionPoolStatistics statistics(long since) {
		return counter.collectStatistics(since);
	}

	private Connection validate(Connection connection) throws SQLException {
		if (VALIDATE && !connectionFactory.connectionValid(connection)) {
			try {
				connection.close();
			}
			catch (SQLException ignored) {/*ignored*/}
			throw new SQLException("Connection validation failed: Retrieved invalid connection from pool '" +
							user.username() + "' at " + connectionFactory.url());
		}

		return connection;
	}

	/**
	 * Fetches a connection from the underlying pool.
	 * @return a connection from the underlying pool
	 * @throws SQLException in case of an exception.
	 */
	protected abstract Connection fetchConnection() throws SQLException;

	/**
	 * @return the underlying connection pool instance
	 */
	protected final T connectionPool() {
		return connectionPool;
	}

	/**
	 * @return the number of available connections in this pool
	 */
	protected abstract int available();

	/**
	 * @return the number of connections in active use
	 */
	protected abstract int inUse();

	/**
	 * @return the number of waiting connection requests
	 */
	protected abstract int waiting();

	/**
	 * Updates the given state instance with the current pool state.
	 * @param state the state to update
	 * @return the updated state
	 */
	final DefaultConnectionPoolState updateState(DefaultConnectionPoolState state) {
		return state.set(System.currentTimeMillis(), available(), inUse(), waiting());
	}

	/**
	 * Cleans up statistics collection resources to prevent resource leaks.
	 * This method should be called by concrete implementations in their close() methods
	 * before shutting down the actual connection pool.
	 */
	protected final void closeStatisticsCollection() {
		counter.close();
	}

	/**
	 * Checks the given credentials against the credentials found in the connection pool user
	 * @param user the user credentials to check
	 * @throws AuthenticationException in case the username or password do not match the ones in the connection pool
	 */
	private void checkConnectionPoolCredentials(User user) throws AuthenticationException {
		if (!this.user.username().equalsIgnoreCase(user.username()) || !Arrays.equals(this.user.password(), user.password())) {
			throw new AuthenticationException("Wrong username or password");
		}
	}

	private DataSource createDataSourceProxy(DataSource dataSource) {
		GetConnection getConnection = new GetConnection();

		return ProxyBuilder.of(DataSource.class)
						.delegate(dataSource)
						.method(GET_CONNECTION, getConnection)
						.method(GET_CONNECTION, asList(String.class, String.class), getConnection)
						.build();
	}

	private final class GetConnection implements ProxyMethod<DataSource> {

		@Override
		public Object invoke(Parameters<DataSource> parameters) throws Throwable {
			Connection connection = connectionFactory.createConnection(user);
			counter.incrementConnectionsCreatedCounter();

			return ProxyBuilder.of(Connection.class)
							.delegate(connection)
							.method(CLOSE, new Close())
							.build();
		}
	}

	private final class Close implements ProxyMethod<Connection> {

		@Override
		public @Nullable Object invoke(Parameters<Connection> parameters) throws Throwable {
			Connection connection = parameters.delegate();
			if (!connection.isClosed()) {
				counter.incrementConnectionsDestroyedCounter();
			}
			connection.close();
			return null;
		}
	}
}

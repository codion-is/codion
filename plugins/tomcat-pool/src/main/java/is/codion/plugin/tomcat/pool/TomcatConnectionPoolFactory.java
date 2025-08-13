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
package is.codion.plugin.tomcat.pool;

import is.codion.common.db.connection.ConnectionFactory;
import is.codion.common.db.pool.AbstractConnectionPoolWrapper;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.Validator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A Tomcat connection pool based {@link ConnectionPoolFactory} implementation
 */
public final class TomcatConnectionPoolFactory implements ConnectionPoolFactory {

	/**
	 * Creates a Tomcat based connection pool wrapper
	 * @param connectionFactory the connection factory
	 * @param user the user
	 * @return a connection pool
	 */
	@Override
	public ConnectionPoolWrapper createConnectionPool(ConnectionFactory connectionFactory, User user) {
		return new DataSourceWrapper(connectionFactory, user, createDataSource(connectionFactory, user));
	}

	private static DataSource createDataSource(ConnectionFactory connectionFactory, User user) {
		PoolProperties properties = new PoolProperties();
		properties.setUrl(connectionFactory.url());
		properties.setDefaultAutoCommit(false);
		properties.setName(user.username());
		//Codion does not validate connections coming from a connection pool
		properties.setTestOnBorrow(true);
		properties.setValidator(new ConnectionValidator(connectionFactory));
		properties.setMaxActive(ConnectionPoolWrapper.MAXIMUM_POOL_SIZE.getOrThrow());
		properties.setInitialSize(ConnectionPoolWrapper.MINIMUM_POOL_SIZE.getOrThrow());
		properties.setMaxIdle(ConnectionPoolWrapper.MAXIMUM_POOL_SIZE.getOrThrow());
		properties.setMinIdle(ConnectionPoolWrapper.MINIMUM_POOL_SIZE.getOrThrow());
		properties.setSuspectTimeout(ConnectionPoolWrapper.IDLE_TIMEOUT.getOrThrow() / 1000);
		properties.setMaxWait(ConnectionPoolWrapper.CHECK_OUT_TIMEOUT.getOrThrow());

		return new DataSource(properties);
	}

	private static final class DataSourceWrapper extends AbstractConnectionPoolWrapper<DataSource> {

		private DataSourceWrapper(ConnectionFactory connectionFactory, User user, DataSource dataSource) {
			super(connectionFactory, user, dataSource,
							dataSourceProxy -> {
								dataSource.setDataSource(dataSourceProxy);

								return dataSource;
							});
		}

		@Override
		public void close() {
			connectionPool().close();
			closeStatisticsCollection();
		}

		@Override
		public int getCleanupInterval() {
			return connectionPool().getTimeBetweenEvictionRunsMillis();
		}

		@Override
		public void setCleanupInterval(int poolCleanupInterval) {
			connectionPool().setTimeBetweenEvictionRunsMillis(poolCleanupInterval);
		}

		@Override
		public int getIdleTimeout() {
			return connectionPool().getSuspectTimeout() * 1000;
		}

		@Override
		public void setIdleTimeout(int idleTimeout) {
			connectionPool().setSuspectTimeout(idleTimeout / 1000);
		}

		@Override
		public int getMinimumPoolSize() {
			return connectionPool().getMinIdle();
		}

		@Override
		public void setMinimumPoolSize(int minimumPoolSize) {
			connectionPool().setMinIdle(minimumPoolSize);
		}

		@Override
		public int getMaximumPoolSize() {
			return connectionPool().getMaxActive();
		}

		@Override
		public void setMaximumPoolSize(int maximumPoolSize) {
			connectionPool().setMaxActive(maximumPoolSize);
			connectionPool().setMaxIdle(maximumPoolSize);
		}

		@Override
		public int getMaximumCheckOutTime() {
			return connectionPool().getMaxWait();
		}

		@Override
		public void setMaximumCheckOutTime(int maximumCheckOutTime) {
			connectionPool().setMaxWait(maximumCheckOutTime);
		}

		@Override
		protected Connection fetchConnection() throws SQLException {
			return connectionPool().getConnection();
		}

		@Override
		protected int available() {
			return connectionPool().getSize() - connectionPool().getActive();
		}

		@Override
		protected int inUse() {
			return connectionPool().getActive();
		}

		@Override
		protected int waiting() {
			return connectionPool().getWaitCount();
		}
	}

	private static final class ConnectionValidator implements Validator {

		private final ConnectionFactory connectionFactory;

		private ConnectionValidator(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}

		@Override
		public boolean validate(Connection connection, int i) {
			return connectionFactory.connectionValid(connection);
		}
	}
}

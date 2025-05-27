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
package is.codion.plugin.hikari.pool;

import is.codion.common.db.connection.ConnectionFactory;
import is.codion.common.db.pool.AbstractConnectionPoolWrapper;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.DriverDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A HikariCP connection pool based {@link ConnectionPoolFactory} implementation
 */
public final class HikariConnectionPoolFactory implements ConnectionPoolFactory {

	/**
	 * Creates a HikariCP based connection pool wrapper
	 * @param connectionFactory the connection factory
	 * @param user the user
	 * @return a connection pool
	 */
	@Override
	public ConnectionPoolWrapper createConnectionPool(ConnectionFactory connectionFactory, User user) {
		return new HikariConnectionPoolWrapper(connectionFactory, user, createConfig(connectionFactory, user));
	}

	private static HikariConfig createConfig(ConnectionFactory connectionFactory, User user) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(connectionFactory.url());
		config.setAutoCommit(false);
		config.setUsername(user.username());
		config.setMaximumPoolSize(ConnectionPoolWrapper.DEFAULT_MAXIMUM_POOL_SIZE.getOrThrow());
		config.setMinimumIdle(ConnectionPoolWrapper.DEFAULT_MINIMUM_POOL_SIZE.getOrThrow());
		config.setIdleTimeout(ConnectionPoolWrapper.DEFAULT_IDLE_TIMEOUT.getOrThrow());

		return config;
	}

	private static final class HikariConnectionPoolWrapper extends AbstractConnectionPoolWrapper<HikariPool> {

		private final HikariConfig config;

		private HikariConnectionPoolWrapper(ConnectionFactory connectionFactory, User user, HikariConfig config) {
			super(connectionFactory, user, new DriverDataSource(connectionFactory.url(), null,
											new Properties(), user.username(), String.valueOf(user.password())),
							dataSourceProxy -> {
								config.setDataSource(dataSourceProxy);

								return new HikariPool(config);
							});
			this.config = config;
		}

		@Override
		public void close() {
			try {
				connectionPool().shutdown();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public int getCleanupInterval() {
			return 0;
		}

		@Override
		public void setCleanupInterval(int poolCleanupInterval) {/*non-configurable, com.zaxxer.hikari.housekeeping.periodMs*/}

		@Override
		public int getIdleTimeout() {
			return (int) config.getIdleTimeout();
		}

		@Override
		public void setIdleTimeout(int idleTimeout) {
			config.setIdleTimeout(idleTimeout);
		}

		@Override
		public int getMinimumPoolSize() {
			return config.getMinimumIdle();
		}

		@Override
		public void setMinimumPoolSize(int minimumPoolSize) {
			config.setMinimumIdle(minimumPoolSize);
		}

		@Override
		public int getMaximumPoolSize() {
			return config.getMaximumPoolSize();
		}

		@Override
		public void setMaximumPoolSize(int maximumPoolSize) {
			config.setMaximumPoolSize(maximumPoolSize);
		}

		@Override
		protected int available() {
			return connectionPool().getIdleConnections();
		}

		@Override
		protected int inUse() {
			return connectionPool().getActiveConnections();
		}

		@Override
		public int getMaximumCheckOutTime() {
			return (int) config.getConnectionTimeout();
		}

		@Override
		public void setMaximumCheckOutTime(int maximumCheckOutTime) {
			config.setConnectionTimeout(maximumCheckOutTime);
		}

		@Override
		protected Connection fetchConnection() throws SQLException {
			return connectionPool().getConnection();
		}

		@Override
		protected int waiting() {
			return connectionPool().getThreadsAwaitingConnection();
		}
	}
}

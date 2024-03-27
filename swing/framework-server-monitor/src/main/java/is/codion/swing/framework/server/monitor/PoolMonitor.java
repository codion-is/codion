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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.framework.server.EntityServerAdmin;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for monitoring the connection pools of a given EntityServer.
 */
public final class PoolMonitor {

	private final EntityServerAdmin server;

	private final Collection<ConnectionPoolMonitor> connectionPoolMonitors = new ArrayList<>();

	/**
	 * Instantiates a new {@link PoolMonitor}
	 * @param server the server
	 * @param updateRate the initial statistics update rate in seconds
	 * @throws RemoteException in case of an exception
	 */
	public PoolMonitor(EntityServerAdmin server, int updateRate) throws RemoteException {
		this.server = requireNonNull(server);
		for (String username : this.server.connectionPoolUsernames()) {
			connectionPoolMonitors.add(new ConnectionPoolMonitor(new MonitorPoolWrapper(username, this.server), updateRate));
		}
	}

	/**
	 * @return the available {@link ConnectionPoolMonitor} instances
	 */
	public Collection<ConnectionPoolMonitor> connectionPoolInstanceMonitors() {
		return connectionPoolMonitors;
	}

	/**
	 * Shuts down this pool monitor
	 */
	public void shutdown() {
		for (ConnectionPoolMonitor monitor : connectionPoolMonitors) {
			monitor.shutdown();
		}
	}

	private static final class MonitorPoolWrapper implements ConnectionPoolWrapper {

		private final EntityServerAdmin server;
		private final User user;

		private MonitorPoolWrapper(String username, EntityServerAdmin server) {
			this.user = User.user(username);
			this.server = server;
		}

		@Override
		public int getMaximumPoolSize() {
			try {
				return server.getMaximumConnectionPoolSize(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getMinimumPoolSize() {
			try {
				return server.getMinimumConnectionPoolSize(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getCleanupInterval() {
			try {
				return server.getConnectionPoolCleanupInterval(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getIdleConnectionTimeout() {
			try {
				return server.getPooledConnectionIdleTimeout(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setMaximumPoolSize(int maximumPoolSize) {
			if (maximumPoolSize < 0) {
				throw new IllegalArgumentException("Maximum pool size must be a positive integer");
			}
			if (maximumPoolSize < getMinimumPoolSize()) {
				throw new IllegalArgumentException("Maximum pool size must be equal to or exceed minimum pool size");
			}
			try {
				server.setMaximumConnectionPoolSize(user.username(), maximumPoolSize);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setMinimumPoolSize(int minimumPoolSize) {
			if (minimumPoolSize < 0) {
				throw new IllegalArgumentException("Minimum pool size must be a positive integer");
			}
			if (minimumPoolSize > getMaximumPoolSize()) {
				throw new IllegalArgumentException("Minimum pool size equal to or below maximum pool size time");
			}
			try {
				server.setMinimumConnectionPoolSize(user.username(), minimumPoolSize);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setIdleConnectionTimeout(int idleConnectionTimeout) {
			if (idleConnectionTimeout < 0) {
				throw new IllegalArgumentException("Idle connection timeout must be a positive integer");
			}
			try {
				server.setPooledConnectionIdleTimeout(user.username(), idleConnectionTimeout);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getMaximumCheckOutTime() {
			try {
				return server.getMaximumPoolCheckOutTime(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setMaximumCheckOutTime(int maximumCheckOutTime) {
			if (maximumCheckOutTime < 0) {
				throw new IllegalArgumentException("Maximum check out time must be a positive integer");
			}
			try {
				server.setMaximumPoolCheckOutTime(user.username(), maximumCheckOutTime);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setCleanupInterval(int poolCleanupInterval) {
			if (poolCleanupInterval < 0) {
				throw new IllegalArgumentException("Cleanup interval must be a positive integer");
			}
			try {
				server.setConnectionPoolCleanupInterval(user.username(), poolCleanupInterval);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public ConnectionPoolStatistics statistics(long since) {
			try {
				return server.connectionPoolStatistics(user.username(), since);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public User user() {
			return user;
		}

		@Override
		public boolean isCollectSnapshotStatistics() {
			try {
				return server.isCollectPoolSnapshotStatistics(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setCollectSnapshotStatistics(boolean collectSnapshotStatistics) {
			try {
				server.setCollectPoolSnapshotStatistics(user.username(), collectSnapshotStatistics);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean isCollectCheckOutTimes() {
			try {
				return server.isCollectPoolCheckOutTimes(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setCollectCheckOutTimes(boolean collectCheckOutTimes) {
			try {
				server.setCollectPoolCheckOutTimes(user.username(), collectCheckOutTimes);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void resetStatistics() {
			try {
				server.resetConnectionPoolStatistics(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Connection connection(User user) {
			return null;
		}

		@Override
		public DataSource poolDataSource() {
			return null;
		}

		@Override
		public void close() {/*Not required*/}
	}
}

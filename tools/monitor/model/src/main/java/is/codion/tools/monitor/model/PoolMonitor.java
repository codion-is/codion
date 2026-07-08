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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.model;

import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.utilities.user.User;
import is.codion.framework.server.EntityServerAdmin;

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
		public int maximumPoolSize() {
			try {
				return server.maximumConnectionPoolSize(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int minimumPoolSize() {
			try {
				return server.minimumConnectionPoolSize(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int cleanupInterval() {
			try {
				return server.connectionPoolCleanupInterval(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int idleTimeout() {
			try {
				return server.pooledConnectionIdleTimeout(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void maximumPoolSize(int maximumPoolSize) {
			if (maximumPoolSize < 0) {
				throw new IllegalArgumentException("Maximum pool size must be a positive integer");
			}
			if (maximumPoolSize < minimumPoolSize()) {
				throw new IllegalArgumentException("Maximum pool size must be equal to or exceed minimum pool size");
			}
			try {
				server.maximumConnectionPoolSize(user.username(), maximumPoolSize);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void minimumPoolSize(int minimumPoolSize) {
			if (minimumPoolSize < 0) {
				throw new IllegalArgumentException("Minimum pool size must be a positive integer");
			}
			if (minimumPoolSize > maximumPoolSize()) {
				throw new IllegalArgumentException("Minimum pool size equal to or below maximum pool size time");
			}
			try {
				server.minimumConnectionPoolSize(user.username(), minimumPoolSize);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void idleTimeout(int idleTimeout) {
			if (idleTimeout < 0) {
				throw new IllegalArgumentException("Idle connection timeout must be a positive integer");
			}
			try {
				server.pooledConnectionIdleTimeout(user.username(), idleTimeout);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int maximumCheckOutTime() {
			try {
				return server.maximumPoolCheckOutTime(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void maximumCheckOutTime(int maximumCheckOutTime) {
			if (maximumCheckOutTime < 0) {
				throw new IllegalArgumentException("Maximum check out time must be a positive integer");
			}
			try {
				server.maximumPoolCheckOutTime(user.username(), maximumCheckOutTime);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void cleanupInterval(int poolCleanupInterval) {
			if (poolCleanupInterval < 0) {
				throw new IllegalArgumentException("Cleanup interval must be a positive integer");
			}
			try {
				server.connectionPoolCleanupInterval(user.username(), poolCleanupInterval);
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
		public boolean collectSnapshotStatistics() {
			try {
				return server.collectPoolSnapshotStatistics(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void collectSnapshotStatistics(boolean collectSnapshotStatistics) {
			try {
				server.collectPoolSnapshotStatistics(user.username(), collectSnapshotStatistics);
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean collectCheckOutTimes() {
			try {
				return server.collectPoolCheckOutTimes(user.username());
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void collectCheckOutTimes(boolean collectCheckOutTimes) {
			try {
				server.collectPoolCheckOutTimes(user.username(), collectCheckOutTimes);
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
		public void close() {/*Not required*/}
	}
}

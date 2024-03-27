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
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.logging.LoggerProxy;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.DefaultServerAdmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the EntityServerAdmin interface, providing admin access to a EntityServer instance.
 */
final class DefaultEntityServerAdmin extends DefaultServerAdmin implements EntityServerAdmin {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityServerAdmin.class);

	private static final long serialVersionUID = 1;

	/**
	 * The server being administrated
	 */
	private final EntityServer server;

	private final LoggerProxy loggerProxy = LoggerProxy.instance();

	/**
	 * Instantiates a new DefaultEntityServerAdmin
	 * @param server the server to administer
	 * @param configuration the port on which to make the server admin available
	 * @throws RemoteException in case of an exception
	 * @throws NullPointerException in case {@code configuration} or {@code server} are not specified
	 */
	DefaultEntityServerAdmin(EntityServer server, EntityServerConfiguration configuration) throws RemoteException {
		super(server, configuration);
		this.server = server;
	}

	@Override
	public String databaseUrl() {
		return server.database().url();
	}

	@Override
	public Object getLogLevel() {
		return loggerProxy.getLogLevel();
	}

	@Override
	public void setLogLevel(Object level) {
		LOG.info("setLogLevel({})", level);
		loggerProxy.setLogLevel(level);
	}

	@Override
	public int getMaintenanceInterval() {
		return server.getMaintenanceInterval();
	}

	@Override
	public void setMaintenanceInterval(int interval) {
		LOG.info("setMaintenanceInterval({})", interval);
		server.setMaintenanceInterval(interval);
	}

	@Override
	public void disconnectTimedOutClients() throws RemoteException {
		LOG.info("disconnectTimedOutClients()");
		server.disconnectClients(true);
	}

	@Override
	public void disconnectAllClients() throws RemoteException {
		LOG.info("disconnectAllClients()");
		server.disconnectClients(false);
	}

	@Override
	public void resetConnectionPoolStatistics(String username) {
		LOG.info("resetConnectionPoolStatistics({})", username);
		server.database().connectionPool(username).resetStatistics();
	}

	@Override
	public boolean isCollectPoolSnapshotStatistics(String username) {
		return server.database().connectionPool(username).isCollectSnapshotStatistics();
	}

	@Override
	public void setCollectPoolSnapshotStatistics(String username, boolean snapshotStatistics) {
		LOG.info("setCollectPoolSnapshotStatistics({}, {})", username, snapshotStatistics);
		server.database().connectionPool(username).setCollectSnapshotStatistics(snapshotStatistics);
	}

	@Override
	public boolean isCollectPoolCheckOutTimes(String username) throws RemoteException {
		return server.database().connectionPool(username).isCollectCheckOutTimes();
	}

	@Override
	public void setCollectPoolCheckOutTimes(String username, boolean collectCheckOutTimes) throws RemoteException {
		LOG.info("setCollectPoolCheckOutTimes({}, {})", username, collectCheckOutTimes);
		server.database().connectionPool(username).setCollectCheckOutTimes(collectCheckOutTimes);
	}

	@Override
	public int requestsPerSecond() {
		return AbstractRemoteEntityConnection.requestsPerSecond();
	}

	@Override
	public ConnectionPoolStatistics connectionPoolStatistics(String username, long since) {
		return server.database().connectionPool(username).statistics(since);
	}

	@Override
	public Database.Statistics databaseStatistics() {
		return server.databaseStatistics();
	}

	@Override
	public Collection<String> connectionPoolUsernames() {
		return server.database().connectionPoolUsernames();
	}

	@Override
	public int getConnectionPoolCleanupInterval(String username) {
		return server.database().connectionPool(username).getCleanupInterval();
	}

	@Override
	public void setConnectionPoolCleanupInterval(String username, int poolCleanupInterval) {
		LOG.info("setConnectionPoolCleanupInterval({}, {})", username, poolCleanupInterval);
		server.database().connectionPool(username).setCleanupInterval(poolCleanupInterval);
	}

	@Override
	public int getMaximumConnectionPoolSize(String username) {
		return server.database().connectionPool(username).getMaximumPoolSize();
	}

	@Override
	public void setMaximumConnectionPoolSize(String username, int value) {
		LOG.info("setMaximumConnectionPoolSize({}, {})", username, value);
		server.database().connectionPool(username).setMaximumPoolSize(value);
	}

	@Override
	public int getMinimumConnectionPoolSize(String username) {
		return server.database().connectionPool(username).getMinimumPoolSize();
	}

	@Override
	public void setMinimumConnectionPoolSize(String username, int value) {
		LOG.info("setMinimumConnectionPoolSize({}, {})", username, value);
		server.database().connectionPool(username).setMinimumPoolSize(value);
	}

	@Override
	public int getPooledConnectionIdleTimeout(String username) {
		return server.database().connectionPool(username).getIdleConnectionTimeout();
	}

	@Override
	public void setPooledConnectionIdleTimeout(String username, int pooledConnectionIdleTimeout) {
		LOG.info("setPooledConnectionIdleTimeout({}, {})", username, pooledConnectionIdleTimeout);
		server.database().connectionPool(username).setIdleConnectionTimeout(pooledConnectionIdleTimeout);
	}

	@Override
	public int getMaximumPoolCheckOutTime(String username) {
		return server.database().connectionPool(username).getMaximumCheckOutTime();
	}

	@Override
	public void setMaximumPoolCheckOutTime(String username, int value) {
		LOG.info("setMaximumPoolCheckOutTime({}, {})", username, value);
		server.database().connectionPool(username).setMaximumCheckOutTime(value);
	}

	@Override
	public ClientLog clientLog(UUID clientId) {
		return server.clientLog(clientId);
	}

	@Override
	public boolean isLoggingEnabled(UUID clientId) {
		return server.isLoggingEnabled(clientId);
	}

	@Override
	public void setLoggingEnabled(UUID clientId, boolean loggingEnabled) {
		LOG.info("setLoggingEnabled({}, {})", clientId, loggingEnabled);
		server.setLoggingEnabled(clientId, loggingEnabled);
	}

	@Override
	public int getIdleConnectionTimeout() {
		return server.getIdleConnectionTimeout();
	}

	@Override
	public void setIdleConnectionTimeout(int idleConnectionTimeout) {
		LOG.info("setIdleConnectionTimeout({})", idleConnectionTimeout);
		server.setIdleConnectionTimeout(idleConnectionTimeout);
	}

	@Override
	public Map<String, Collection<DomainEntityDefinition>> domainEntityDefinitions() {
		Map<String, Collection<DomainEntityDefinition>> domainEntityDefinitions = new HashMap<>();
		server.domainEntityDefinitions().forEach((domainType, domainDefinitions) ->
						domainEntityDefinitions.put(domainType.name(), domainDefinitions));

		return domainEntityDefinitions;
	}

	@Override
	public Map<String, Collection<DomainReport>> domainReports() throws RemoteException {
		Map<String, Collection<DomainReport>> domainReports = new HashMap<>();
		server.domainReports().forEach((domainType, reports) -> domainReports.put(domainType.name(), reports));

		return domainReports;
	}

	@Override
	public Map<String, Collection<DomainOperation>> domainOperations() throws RemoteException {
		Map<String, Collection<DomainOperation>> domainOperations = new HashMap<>();
		server.domainOperations().forEach((domainType, operations) -> domainOperations.put(domainType.name(), operations));

		return domainOperations;
	}

	@Override
	public void clearReportCache() throws RemoteException {
		server.clearReportCache();
	}
}

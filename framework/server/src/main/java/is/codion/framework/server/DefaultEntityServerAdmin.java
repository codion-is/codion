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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.rmi.server.AbstractServerAdmin;
import is.codion.common.utilities.logging.LoggerProxy;
import is.codion.common.utilities.logging.MethodTrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the EntityServerAdmin interface, providing admin access to a EntityServer instance.
 */
final class DefaultEntityServerAdmin extends AbstractServerAdmin implements EntityServerAdmin {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityServerAdmin.class);

	@Serial
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
	public Object logLevel(String logger) throws RemoteException {
		return loggerProxy.getLogLevel(logger);
	}

	@Override
	public void logLevel(String logger, Object level) throws RemoteException {
		LOG.info("setLogLevel({}, {})", logger, level);
		loggerProxy.setLogLevel(logger, level);
	}

	@Override
	public Collection<String> loggers() throws RemoteException {
		return loggerProxy.loggers();
	}

	@Override
	public String rootLogger() throws RemoteException {
		return loggerProxy.rootLogger();
	}

	@Override
	public Collection<Object> logLevels() throws RemoteException {
		return loggerProxy.levels();
	}

	@Override
	public int maintenanceInterval() {
		return server.maintenanceInterval();
	}

	@Override
	public void maintenanceInterval(int interval) {
		LOG.info("maintenanceInterval({})", interval);
		server.maintenanceInterval(interval);
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
	public boolean collectPoolSnapshotStatistics(String username) {
		return server.database().connectionPool(username).collectSnapshotStatistics();
	}

	@Override
	public void collectPoolSnapshotStatistics(String username, boolean snapshotStatistics) {
		LOG.info("collectPoolSnapshotStatistics({}, {})", username, snapshotStatistics);
		server.database().connectionPool(username).collectSnapshotStatistics(snapshotStatistics);
	}

	@Override
	public boolean collectPoolCheckOutTimes(String username) throws RemoteException {
		return server.database().connectionPool(username).collectCheckOutTimes();
	}

	@Override
	public void collectPoolCheckOutTimes(String username, boolean collectCheckOutTimes) throws RemoteException {
		LOG.info("collectPoolCheckOutTimes({}, {})", username, collectCheckOutTimes);
		server.database().connectionPool(username).collectCheckOutTimes(collectCheckOutTimes);
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
	public int connectionPoolCleanupInterval(String username) {
		return server.database().connectionPool(username).cleanupInterval();
	}

	@Override
	public void connectionPoolCleanupInterval(String username, int poolCleanupInterval) {
		LOG.info("connectionPoolCleanupInterval({}, {})", username, poolCleanupInterval);
		server.database().connectionPool(username).cleanupInterval(poolCleanupInterval);
	}

	@Override
	public int maximumConnectionPoolSize(String username) {
		return server.database().connectionPool(username).maximumPoolSize();
	}

	@Override
	public void maximumConnectionPoolSize(String username, int value) {
		LOG.info("maximumConnectionPoolSize({}, {})", username, value);
		server.database().connectionPool(username).maximumPoolSize(value);
	}

	@Override
	public int minimumConnectionPoolSize(String username) {
		return server.database().connectionPool(username).minimumPoolSize();
	}

	@Override
	public void minimumConnectionPoolSize(String username, int value) {
		LOG.info("minimumConnectionPoolSize({}, {})", username, value);
		server.database().connectionPool(username).minimumPoolSize(value);
	}

	@Override
	public int pooledConnectionIdleTimeout(String username) {
		return server.database().connectionPool(username).idleTimeout();
	}

	@Override
	public void pooledConnectionIdleTimeout(String username, int pooledConnectionIdleTimeout) {
		LOG.info("pooledConnectionIdleTimeout({}, {})", username, pooledConnectionIdleTimeout);
		server.database().connectionPool(username).idleTimeout(pooledConnectionIdleTimeout);
	}

	@Override
	public int maximumPoolCheckOutTime(String username) {
		return server.database().connectionPool(username).maximumCheckOutTime();
	}

	@Override
	public void maximumPoolCheckOutTime(String username, int value) {
		LOG.info("maximumPoolCheckOutTime({}, {})", username, value);
		server.database().connectionPool(username).maximumCheckOutTime(value);
	}

	@Override
	public List<MethodTrace> methodTraces(UUID clientId) {
		return server.methodTraces(clientId);
	}

	@Override
	public boolean traceToFile(UUID clientId) {
		return server.traceToFile(clientId);
	}

	@Override
	public void traceToFile(UUID clientId, boolean traceToFile) {
		LOG.info("traceToFile({}, {})", clientId, traceToFile);
		server.traceToFile(clientId, traceToFile);
	}

	@Override
	public boolean tracingEnabled(UUID clientId) {
		return server.tracingEnabled(clientId);
	}

	@Override
	public void tracingEnabled(UUID clientId, boolean tracingEnabled) {
		LOG.info("tracingEnabled({}, {})", clientId, tracingEnabled);
		server.tracingEnabled(clientId, tracingEnabled);
	}

	@Override
	public int idleConnectionTimeout() {
		return server.idleConnectionTimeout();
	}

	@Override
	public void idleConnectionTimeout(int idleConnectionTimeout) {
		LOG.info("idleConnectionTimeout({})", idleConnectionTimeout);
		server.idleConnectionTimeout(idleConnectionTimeout);
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

	@Override
	protected int requestsPerSecond() {
		return AbstractRemoteEntityConnection.requestsPerSecond();
	}
}

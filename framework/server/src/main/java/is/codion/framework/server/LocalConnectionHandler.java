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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.utilities.logging.MethodTrace;
import is.codion.common.utilities.scheduler.TaskScheduler;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.tracer.MethodTracer;
import is.codion.framework.db.local.tracer.MethodTracer.Traceable;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static is.codion.framework.db.local.LocalEntityConnection.TRACES;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;
import static java.lang.System.currentTimeMillis;

final class LocalConnectionHandler implements InvocationHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LocalConnectionHandler.class);
	private static final Logger TRACER = LoggerFactory.getLogger("tracer");

	private static final String LOG_IDENTIFIER_PROPERTY = "logIdentifier";
	private static final String FETCH_CONNECTION = "fetchConnection";
	private static final String RETURN_CONNECTION = "returnConnection";
	private static final String CREATE_CONNECTION = "createConnection";
	private static final String ENTITIES = "entities";

	static final RequestCounter REQUEST_COUNTER = new RequestCounter();

	private final Domain domain;
	private final RemoteClient remoteClient;
	private final Database database;
	private final ConnectionPoolWrapper connectionPool;
	private final String logIdentifier;
	private final String userDescription;
	private final long creationTime = currentTimeMillis();
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final LocalEntityConnection entityConnection;

	private MethodTracer tracer = MethodTracer.NO_OP;
	private boolean traceToFile = false;
	private volatile long lastAccessTime = creationTime;
	private volatile boolean closed = false;

	LocalConnectionHandler(Domain domain, RemoteClient remoteClient, Database database) {
		this.domain = domain;
		this.remoteClient = remoteClient;
		String databaseUsername = remoteClient.databaseUser().username();
		this.connectionPool = database.containsConnectionPool(databaseUsername) ? database.connectionPool(databaseUsername) : null;
		this.database = database;
		this.logIdentifier = remoteClient.user().username().toLowerCase() + "@" + remoteClient.clientType();
		this.userDescription = "Remote user: " + remoteClient.user().username() + ", database user: " + databaseUsername;
		this.entityConnection = initializeConnection();
	}

	@Override
	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
		String methodName = method.getName();
		if (methodName.equals(ENTITIES)) {
			return entities();
		}
		active.set(true);
		lastAccessTime = currentTimeMillis();
		Exception exception = null;
		logEntry(methodName, args);
		try {
			prepareConnection();

			return method.invoke(entityConnection, args);
		}
		catch (InvocationTargetException e) {
			//Wrapped exception has already been logged during the actual method call
			throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
		}
		catch (Exception e) {
			LOG.error(e.getMessage(), e);
			exception = e;
			throw exception;
		}
		finally {
			returnConnection();
			logExit(methodName, exception);
			active.set(false);
		}
	}

	private Entities entities() {
		active.set(true);
		lastAccessTime = currentTimeMillis();
		try {
			logEntry(ENTITIES, null);

			return domain.entities();
		}
		finally {
			logExit(ENTITIES, null);
			active.set(false);
		}
	}

	private void logEntry(String methodName, Object[] args) {
		MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
		REQUEST_COUNTER.incrementRequestsPerSecondCounter();
		if (args == null || args.length == 0) {
			tracer.enter(methodName);
		}
		else {
			tracer.enter(methodName, args);
		}
	}

	private void logExit(String methodName, Exception exception) {
		MethodTrace trace = tracer.exit(methodName, exception);
		if (tracer != MethodTracer.NO_OP && traceToFile) {
			StringBuilder messageBuilder = new StringBuilder(remoteClient.toString()).append("\n");
			trace.appendTo(messageBuilder);
			TRACER.trace(messageBuilder.toString());
		}
		MDC.remove(LOG_IDENTIFIER_PROPERTY);
	}

	boolean connected() {
		if (connectionPool != null) {
			return !closed;
		}

		return !closed && entityConnection.connected();
	}

	void close() {
		if (closed) {
			return;
		}
		closed = true;
		rollbackIfRequired(entityConnection);
		if (connectionPool != null) {
			returnToPool(entityConnection);
		}
		else {
			entityConnection.close();
		}
	}

	List<MethodTrace> methodTraces() {
		synchronized (tracer) {
			return tracer.entries();
		}
	}

	RemoteClient remoteClient() {
		return remoteClient;
	}

	long lastAccessTime() {
		return lastAccessTime;
	}

	boolean active() {
		return active.get();
	}

	boolean closed() {
		return closed;
	}

	private void prepareConnection() {
		if (connectionPool == null) {
			prepareLocalConnection();
		}
		else {
			preparePooledConnection();
		}
	}

	private void preparePooledConnection() {
		if (entityConnection.transactionOpen()) {
			return;
		}
		DatabaseException exception = null;
		try {
			tracer.enter(FETCH_CONNECTION, userDescription);
			entityConnection.setConnection(connectionPool.connection(remoteClient.databaseUser()));
		}
		catch (DatabaseException ex) {
			exception = ex;
			throw ex;
		}
		finally {
			tracer.exit(FETCH_CONNECTION, exception);
		}
	}

	private void prepareLocalConnection() {
		if (!entityConnection.connected()) {
			entityConnection.close();//just in case
			DatabaseException exception = null;
			try {
				tracer.enter(CREATE_CONNECTION, userDescription);
				entityConnection.setConnection(database.createConnection(remoteClient.databaseUser()));
			}
			catch (DatabaseException ex) {
				exception = ex;
				throw ex;
			}
			finally {
				tracer.exit(CREATE_CONNECTION, exception);
			}
		}
	}

	/**
	 * Returns the pooled connection to a connection pool if the connection is not within an open transaction
	 */
	private void returnConnection() {
		if (connectionPool == null || entityConnection.transactionOpen()) {
			return;
		}
		Exception exception = null;
		try {
			tracer.enter(RETURN_CONNECTION, userDescription);
			returnToPool(entityConnection);
		}
		catch (Exception e) {
			exception = e;
			LOG.info("Exception while returning connection to pool", e);
		}
		finally {
			tracer.exit(RETURN_CONNECTION, exception);
		}
	}

	private static void returnToPool(LocalEntityConnection connection) {
		if (connection.getConnection() != null) {
			closeSilently(connection.getConnection());
			connection.setConnection(null);
		}
	}

	private void rollbackIfRequired(LocalEntityConnection entityConnection) {
		if (entityConnection.transactionOpen()) {
			LOG.info("Rollback open transaction on disconnect: {}", remoteClient);
			try {
				entityConnection.rollbackTransaction();
			}
			catch (DatabaseException e) {
				LOG.error("Rollback on disconnect failed: {}", remoteClient, e);
			}
		}
	}

	private LocalEntityConnection initializeConnection() {
		LocalEntityConnection connection = localEntityConnection(database, domain, connectionPool == null ?
						database.createConnection(remoteClient.databaseUser()) :
						connectionPool.connection(remoteClient.databaseUser()));
		((Traceable) connection).tracer(tracer);
		if (connectionPool != null) {
			rollbackSilently(connection.getConnection());
			returnToPool(connection);
		}

		return connection;
	}

	private static void rollbackSilently(Connection connection) {
		try {
			//otherwise the connection's commit state is dirty, so it gets discarded by the connection pool when we try to return it
			connection.rollback();
		}
		catch (SQLException e) {/*Silently*/}
	}

	private static void closeSilently(AutoCloseable closeable) {
		try {
			closeable.close();
		}
		catch (Exception ignored) {/*ignored*/}
	}

	synchronized void setTraceToFile(boolean traceToFile) {
		this.traceToFile = traceToFile;
	}

	synchronized boolean isTraceToFile() {
		return traceToFile;
	}

	synchronized void setTracingEnabled(boolean enabled) {
		tracer = enabled ? methodTracer(TRACES.getOrThrow()) : MethodTracer.NO_OP;
		((Traceable) entityConnection).tracer(tracer);
	}

	synchronized boolean isTracingEnabled() {
		return tracer != MethodTracer.NO_OP;
	}

	static final class RequestCounter {

		private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

		private static final double THOUSAND = 1000d;

		private final AtomicLong requestsPerSecondTime = new AtomicLong(currentTimeMillis());
		private final AtomicInteger requestsPerSecond = new AtomicInteger();
		private final AtomicInteger requestsPerSecondCounter = new AtomicInteger();

		private RequestCounter() {
			TaskScheduler.builder()
							.task(this::updateRequestsPerSecond)
							.interval(DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
							.name("Request counter")
							.start();
		}

		int requestsPerSecond() {
			return requestsPerSecond.get();
		}

		private void updateRequestsPerSecond() {
			long current = currentTimeMillis();
			double seconds = (current - requestsPerSecondTime.getAndSet(current)) / THOUSAND;
			if (seconds > 0) {
				requestsPerSecond.set((int) (requestsPerSecondCounter.getAndSet(0) / seconds));
			}
		}

		private void incrementRequestsPerSecondCounter() {
			requestsPerSecondCounter.incrementAndGet();
		}
	}
}

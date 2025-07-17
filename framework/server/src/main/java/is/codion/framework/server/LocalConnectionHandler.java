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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.logging.MethodTrace;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.tracer.MethodTracer;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

final class LocalConnectionHandler implements InvocationHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LocalConnectionHandler.class);

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
	private final MethodTracer tracer;
	private final String logIdentifier;
	private final String userDescription;
	private final long creationTime = System.currentTimeMillis();
	private final AtomicBoolean active = new AtomicBoolean(false);
	private LocalEntityConnection localEntityConnection;
	private LocalEntityConnection poolEntityConnection;
	private long lastAccessTime = creationTime;
	private boolean closed = false;

	LocalConnectionHandler(Domain domain, RemoteClient remoteClient, Database database) {
		this.domain = domain;
		this.remoteClient = remoteClient;
		String databaseUsername = remoteClient.databaseUser().username();
		this.connectionPool = database.containsConnectionPool(databaseUsername) ? database.connectionPool(databaseUsername) : null;
		this.database = database;
		this.tracer = methodTracer(LocalEntityConnection.CONNECTION_LOG_SIZE.getOrThrow(), new EntityArgumentToString());
		this.logIdentifier = remoteClient.user().username().toLowerCase() + "@" + remoteClient.clientType();
		this.userDescription = "Remote user: " + remoteClient.user().username() + ", database user: " + databaseUsername;
		try {
			if (connectionPool == null) {
				localEntityConnection = LocalEntityConnection.localEntityConnection(database, domain, remoteClient.databaseUser());
				((MethodTracer.Traceable) localEntityConnection).tracer(tracer);
			}
			else {
				poolEntityConnection = LocalEntityConnection.localEntityConnection(database, domain, connectionPool.connection(remoteClient.databaseUser()));
				rollbackSilently(poolEntityConnection.databaseConnection());
				returnConnectionToPool();
			}
		}
		catch (DatabaseException e) {
			close();
			throw e;
		}
	}

	@Override
	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
		if (method.getName().equals(ENTITIES)) {
			return entities();
		}
		active.set(true);
		lastAccessTime = System.currentTimeMillis();
		String methodName = method.getName();
		Exception exception = null;
		try {
			logEntry(methodName, args);

			return method.invoke(connection(), args);
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
		lastAccessTime = System.currentTimeMillis();
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
		if (tracer.isEnabled()) {
			tracer.enter(methodName, args);
		}
	}

	private void logExit(String methodName, Exception exception) {
		MethodTrace trace = tracer.exit(methodName, exception);
		if (trace != null) {
			StringBuilder messageBuilder = new StringBuilder(remoteClient.toString()).append("\n");
			trace.appendTo(messageBuilder);
			LOG.trace(messageBuilder.toString());
		}
		MDC.remove(LOG_IDENTIFIER_PROPERTY);
	}

	boolean connected() {
		if (connectionPool != null) {
			return !closed;
		}

		return !closed && localEntityConnection != null && localEntityConnection.connected();
	}

	void close() {
		if (closed) {
			return;
		}
		closed = true;
		cleanupLocalConnections();
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

	MethodTracer methodLogger() {
		return tracer;
	}

	boolean active() {
		return active.get();
	}

	boolean closed() {
		return closed;
	}

	private EntityConnection connection() {
		if (connectionPool != null) {
			return pooledEntityConnection();
		}

		return localEntityConnection();
	}

	private EntityConnection pooledEntityConnection() {
		if (poolEntityConnection.transactionOpen()) {
			return poolEntityConnection;
		}
		DatabaseException exception = null;
		try {
			if (tracer.isEnabled()) {
				tracer.enter(FETCH_CONNECTION, userDescription);
			}
			poolEntityConnection.databaseConnection().setConnection(connectionPool.connection(remoteClient.databaseUser()));
			((MethodTracer.Traceable) poolEntityConnection).tracer(tracer);
		}
		catch (DatabaseException ex) {
			exception = ex;
			throw ex;
		}
		finally {
			if (tracer.isEnabled()) {
				tracer.exit(FETCH_CONNECTION, exception);
			}
		}

		return poolEntityConnection;
	}

	private EntityConnection localEntityConnection() {
		if (!localEntityConnection.connected()) {
			localEntityConnection.close();//just in case
			DatabaseException exception = null;
			try {
				if (tracer.isEnabled()) {
					tracer.enter(CREATE_CONNECTION, userDescription);
				}
				localEntityConnection = LocalEntityConnection.localEntityConnection(database, domain, remoteClient.databaseUser());
				((MethodTracer.Traceable) localEntityConnection).tracer(tracer);
			}
			catch (DatabaseException ex) {
				exception = ex;
				throw ex;
			}
			finally {
				if (tracer.isEnabled()) {
					tracer.exit(CREATE_CONNECTION, exception);
				}
			}
		}

		return localEntityConnection;
	}

	/**
	 * Returns the pooled connection to a connection pool if the connection is not within an open transaction
	 */
	private void returnConnection() {
		if (poolEntityConnection == null || poolEntityConnection.transactionOpen()) {
			return;
		}
		Exception exception = null;
		try {
			if (tracer.isEnabled()) {
				tracer.enter(RETURN_CONNECTION, userDescription);
			}
			((MethodTracer.Traceable) poolEntityConnection).tracer(null);
			returnConnectionToPool();
		}
		catch (Exception e) {
			exception = e;
			LOG.info("Exception while returning connection to pool", e);
		}
		finally {
			if (tracer.isEnabled()) {
				tracer.exit(RETURN_CONNECTION, exception);
			}
		}
	}

	private void returnConnectionToPool() {
		DatabaseConnection connection = poolEntityConnection.databaseConnection();
		if (connection.connected()) {
			closeSilently(connection.getConnection());
			connection.setConnection(null);
		}
	}

	private void cleanupLocalConnections() {
		if (poolEntityConnection != null) {
			rollbackIfRequired(poolEntityConnection);
			returnConnectionToPool();
			poolEntityConnection = null;
		}
		if (localEntityConnection != null) {
			rollbackIfRequired(localEntityConnection);
			localEntityConnection.close();
			localEntityConnection = null;
		}
	}

	private void rollbackIfRequired(LocalEntityConnection entityConnection) {
		if (entityConnection.transactionOpen()) {
			LOG.info("Rollback open transaction on disconnect: {}", remoteClient);
			try {
				entityConnection.rollbackTransaction();
			}
			catch (DatabaseException e) {
				LOG.error("Rollback on disconnect failed: " + remoteClient, e);
			}
		}
	}

	private static void rollbackSilently(DatabaseConnection databaseConnection) {
		try {
			//otherwise the connection's commit state is dirty, so it gets discarded by the connection pool when we try to return it
			databaseConnection.rollback();
		}
		catch (SQLException e) {/*Silently*/}
	}

	private static void closeSilently(AutoCloseable closeable) {
		try {
			closeable.close();
		}
		catch (Exception ignored) {/*ignored*/}
	}

	static final class RequestCounter {

		private static final int DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL = 2500;

		private static final double THOUSAND = 1000d;

		private final ScheduledExecutorService executorService =
						Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
		private final AtomicLong requestsPerSecondTime = new AtomicLong(System.currentTimeMillis());
		private final AtomicInteger requestsPerSecond = new AtomicInteger();
		private final AtomicInteger requestsPerSecondCounter = new AtomicInteger();

		private RequestCounter() {
			executorService.scheduleWithFixedDelay(this::updateRequestsPerSecond, DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL,
							DEFAULT_REQUEST_COUNTER_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
		}

		int requestsPerSecond() {
			return requestsPerSecond.get();
		}

		private void updateRequestsPerSecond() {
			long current = System.currentTimeMillis();
			double seconds = (current - requestsPerSecondTime.getAndSet(current)) / THOUSAND;
			if (seconds > 0) {
				requestsPerSecond.set((int) (requestsPerSecondCounter.getAndSet(0) / seconds));
			}
		}

		private void incrementRequestsPerSecondCounter() {
			requestsPerSecondCounter.incrementAndGet();
		}
	}

	private static final class DaemonThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);

			return thread;
		}
	}

	/**
	 * An implementation tailored for EntityConnections.
	 */
	private static final class EntityArgumentToString implements MethodTracer.ArgumentFormatter {

		private static final String BRACKET_OPEN = "[";
		private static final String BRACKET_CLOSE = "]";

		private static final String PREPARE_STATEMENT = "prepareStatement";

		@Override
		public String format(String methodName, @Nullable Object object) {
			return toString(methodName, object);
		}

		private String toString(String methodName, Object object) {
			if (ENTITIES.equals(methodName)) {
				return "";
			}
			if (PREPARE_STATEMENT.equals(methodName)) {
				return (String) object;
			}

			return toString(object);
		}

		private String toString(Object object) {
			if (object == null) {
				return "null";
			}
			if (object instanceof String) {
				return "'" + object + "'";
			}
			if (object instanceof Entity) {
				return entityToString((Entity) object);
			}
			else if (object instanceof Entity.Key) {
				return entityKeyToString((Entity.Key) object);
			}
			if (object instanceof List) {
				return toString((List<?>) object);
			}
			if (object instanceof Collection) {
				return toString((Collection<?>) object);
			}
			if (object instanceof byte[]) {
				return "byte[" + ((byte[]) object).length + "]";
			}
			if (object.getClass().isArray()) {
				return toString((Object[]) object);
			}

			return object.toString();
		}

		private static String entityToString(Entity entity) {
			StringBuilder builder = new StringBuilder(entity.type().name()).append(" {");
			for (ColumnDefinition<?> columnDefinition : entity.definition().columns().definitions()) {
				boolean modified = entity.modified(columnDefinition.attribute());
				if (columnDefinition.primaryKey() || modified) {
					StringBuilder valueString = new StringBuilder();
					if (modified) {
						valueString.append(entity.original(columnDefinition.attribute())).append("->");
					}
					valueString.append(entity.string(columnDefinition.attribute()));
					builder.append(columnDefinition.attribute()).append(":").append(valueString).append(",");
				}
			}
			builder.deleteCharAt(builder.length() - 1);

			return builder.append("}").toString();
		}

		private String toString(List<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}
			if (arguments.size() == 1) {
				return toString(arguments.get(0));
			}

			return arguments.stream()
							.map(this::toString)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private String toString(Collection<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}

			return arguments.stream()
							.map(this::toString)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private String toString(Object[] arguments) {
			if (arguments.length == 0) {
				return "";
			}
			if (arguments.length == 1) {
				return toString(arguments[0]);
			}

			return stream(arguments)
							.map(this::toString)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private static String entityKeyToString(Entity.Key key) {
			return key.type() + " {" + key + "}";
		}
	}
}

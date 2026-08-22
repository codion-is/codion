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

import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.rmi.server.RemoteSession;
import is.codion.common.utilities.logging.MethodTrace;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.local.ConnectionHolder;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static is.codion.framework.db.local.LocalEntityConnection.TRACES;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;

final class LocalConnectionHandler implements InvocationHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LocalConnectionHandler.class);
	private static final Logger TRACER = LoggerFactory.getLogger("tracer");

	private static final String LOG_IDENTIFIER_PROPERTY = "logIdentifier";
	private static final String FETCH_CONNECTION = "fetchConnection";
	private static final String PREPARE_CONNECTION = "prepareConnection";
	private static final String RELEASE_CONNECTION = "releaseConnection";
	private static final String RETURN_CONNECTION = "returnConnection";
	private static final String CREATE_CONNECTION = "createConnection";
	private static final String ENTITIES = "entities";

	private final Domain domain;
	private final RemoteSession session;
	private final Database database;
	private final ClientInfo clientInfo;
	private final boolean stampClientInfo;
	private final SessionContexts sessionContexts;
	private final ConnectionPoolWrapper connectionPool;
	private final String logIdentifier;
	private final String userDescription;
	private final String clientDescription;
	private final long creationTime = currentTimeMillis();
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final LocalEntityConnection entityConnection;
	private final ConnectionHolder connectionHolder;

	private MethodTracer tracer = MethodTracer.NO_OP;
	private boolean traceToFile = false;
	private int openIterators = 0;
	/**
	 * Whether the dedicated connection has been stamped and had the contexts applied, there being no
	 * check out to do that on: once, on first use, and again after the connection has been replaced.
	 */
	private boolean prepared = false;
	private volatile long lastAccessTime = creationTime;
	private volatile boolean closed = false;

	LocalConnectionHandler(Domain domain, RemoteSession session, Database database) {
		this.domain = domain;
		this.session = session;
		String databaseUsername = session.databaseUser().username();
		this.connectionPool = database.containsConnectionPool(databaseUsername) ? database.connectionPool(databaseUsername) : null;
		this.database = database;
		this.clientInfo = clientInfo(session);
		this.stampClientInfo = Database.CLIENT_INFO.getOrThrow();
		this.sessionContexts = new SessionContexts(clientInfo, SessionContexts.contexts(clientInfo.clientType()));
		this.logIdentifier = session.request().user().username().toLowerCase() + "@" + session.request().clientType();
		this.userDescription = "Remote user: " + session.request().user().username() + ", database user: " + databaseUsername;
		this.clientDescription = clientInfo.toString();
		this.entityConnection = initializeConnection();
		this.connectionHolder = (ConnectionHolder) entityConnection;
	}

	@Override
	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Exception {
		if (closed) {
			throw new IllegalStateException("Connection closed: " + session);
		}
		String methodName = method.getName();
		if (methodName.equals(ENTITIES)) {
			return entities();
		}
		active.set(true);
		lastAccessTime = currentTimeMillis();
		Exception exception = null;
		long startNanoseconds = nanoTime();
		logEntry(methodName, args);
		try {
			prepareConnection();
			Object result = method.invoke(entityConnection, args);
			if (result instanceof EntityResultIterator) {
				//pin the connection until the iterator is closed, see returnConnection()/iteratorClosed()
				openIterators++;
			}

			return result;
		}
		catch (InvocationTargetException e) {
			//Wrapped exception has already been logged during the actual method call
			throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
		}
		catch (Exception e) {
			//a failed prepare has already been logged, along with the context it failed in
			exception = e;
			throw exception;
		}
		finally {
			returnConnection();
			logExit(methodName, exception);
			ServerMetrics.INSTANCE.record(methodName, nanoTime() - startNanoseconds);
			active.set(false);
		}
	}

	private Entities entities() {
		active.set(true);
		lastAccessTime = currentTimeMillis();
		long startNanoseconds = nanoTime();
		try {
			logEntry(ENTITIES, null);

			return domain.entities();
		}
		finally {
			logExit(ENTITIES, null);
			ServerMetrics.INSTANCE.record(ENTITIES, nanoTime() - startNanoseconds);
			active.set(false);
		}
	}

	private void logEntry(String methodName, Object[] args) {
		MDC.put(LOG_IDENTIFIER_PROPERTY, logIdentifier);
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
			StringBuilder messageBuilder = new StringBuilder(session.toString()).append("\n");
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
			returnToPool();
		}
		else {
			closeConnection();
		}
	}

	synchronized List<MethodTrace> methodTraces() {
		return tracer.entries();
	}

	RemoteSession session() {
		return session;
	}

	ConnectionHolder connectionHolder() {
		return connectionHolder;
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
		Connection connection = traced(FETCH_CONNECTION, userDescription,
						() -> connectionPool.connection(session.databaseUser()));
		traced(PREPARE_CONNECTION, clientDescription, () -> preparePooled(connection));
	}

	private void preparePooled(Connection connection) {
		try {
			stamp(connection);
			connectionHolder.attach(connection);
		}
		catch (RuntimeException e) {
			//not yet held by the holder, so invoke() can not return it, the pool would be one short
			closeSilently(connection);
			throw e;
		}
		//attached, so a failure here leaves the connection for invoke() to return in the usual way
		sessionContexts.prepare(connection);
	}

	private void prepareLocalConnection() {
		if (!entityConnection.connected()) {
			//the state applied to the old connection died with it, as does the count of it
			sessionContexts.reset();
			prepared = false;
			entityConnection.close();//just in case
			traced(CREATE_CONNECTION, userDescription,
							() -> connectionHolder.attach(database.createConnection(session.databaseUser())));
		}
		if (!prepared) {
			traced(PREPARE_CONNECTION, clientDescription, this::prepareLocal);
		}
	}

	/**
	 * Once per connection rather than per invocation, the connection being this client's alone, and only
	 * once it has succeeded, so that a failed prepare is retried rather than skipped.
	 */
	private void prepareLocal() {
		Connection connection = entityConnection.connection();
		stamp(connection);
		sessionContexts.prepare(connection);
		prepared = true;
	}

	/**
	 * Notifies this handler that a remote iterator has been closed, returning the pooled connection
	 * once the last open iterator is closed (and no transaction is open).
	 */
	synchronized void iteratorClosed() {
		if (openIterators > 0) {
			openIterators--;
			if (openIterators == 0) {
				returnConnection();
			}
		}
	}

	/**
	 * Returns the pooled connection to a connection pool if the connection is not within an open transaction
	 * and no remote iterators are still open (see {@link #iteratorClosed()})
	 */
	private void returnConnection() {
		if (connectionPool == null || entityConnection.transactionOpen() || openIterators > 0) {
			return;
		}
		try {
			returnToPool();
		}
		catch (Exception e) {
			//swallowed, this runs in invoke()'s finally where throwing would mask the client's own failure
			LOG.info("Exception while returning connection to pool", e);
		}
	}

	/**
	 * Runs the given operation between a matching {@link MethodTracer} enter and exit, the exit carrying the
	 * exception should it throw. Every traced region in here is this same eight line bracket, and nesting
	 * two of them by hand is what made this class hard to read.
	 * @param method the method name to trace
	 * @param argument the argument to record with the entry
	 * @param operation the operation to run
	 */
	private void traced(String method, String argument, Runnable operation) {
		tracer.enter(method, argument);
		RuntimeException exception = null;
		try {
			operation.run();
		}
		catch (RuntimeException e) {
			exception = e;
			throw e;
		}
		finally {
			tracer.exit(method, exception);
		}
	}

	/**
	 * @param method the method name to trace
	 * @param argument the argument to record with the entry
	 * @param operation the operation to run
	 * @param <T> the operation result type
	 * @return the operation result
	 * @see #traced(String, String, Runnable)
	 */
	private <T> T traced(String method, String argument, Supplier<T> operation) {
		tracer.enter(method, argument);
		RuntimeException exception = null;
		try {
			return operation.get();
		}
		catch (RuntimeException e) {
			exception = e;
			throw e;
		}
		finally {
			tracer.exit(method, exception);
		}
	}

	/**
	 * Stamps the connection with the identity of the client it is about to serve, where the database
	 * supports it, so that a shared database user does not hide who is actually doing the work.
	 */
	private void stamp(Connection connection) {
		if (stampClientInfo) {
			database.clientInfo(connection, clientInfo);
		}
	}

	/**
	 * Removing the session state and handing the connection back are traced apart, being separate costs:
	 * the first is whatever the application's contexts do, the second is the pool.
	 */
	private void returnToPool() {
		Connection connection = connectionHolder.detach();
		if (connection != null) {
			traced(RELEASE_CONNECTION, clientDescription, () -> release(connection));
			traced(RETURN_CONNECTION, userDescription, () -> closeSilently(connection));
		}
	}

	private void release(Connection connection) {
		if (!sessionContexts.release(connection)) {
			discard(connection);
		}
	}

	/**
	 * The dedicated connection is released when the client goes, there being no check in to do it on. What
	 * the release leaves behind does not matter, the connection going with it, so its verdict is ignored.
	 */
	private void closeConnection() {
		try {
			if (!sessionContexts.empty()) {
				Connection connection = connectionHolder.detach();
				if (connection != null) {
					traced(RELEASE_CONNECTION, clientDescription, () -> sessionContexts.release(connection));
					closeSilently(connection);
				}
			}
		}
		finally {
			entityConnection.close();
		}
	}

	/**
	 * Has the pool discard a connection whose session state could not be removed, rather than hand it, and
	 * whatever is still set on it, to the next client. Marked here and destroyed by the pool as it is
	 * returned, the close which follows handing it back for the pool to destroy rather than pool.
	 */
	private void discard(Connection connection) {
		try {
			connectionPool.evict(connection);
		}
		catch (Exception e) {
			LOG.error("Unable to discard a connection whose session state could not be removed", e);
		}
	}

	/**
	 * The client host is unknown often enough to be worth leaving out rather than stamping the connection
	 * with the string standing in for it.
	 */
	private static ClientInfo clientInfo(RemoteSession session) {
		String clientHost = session.clientHost();

		return new ClientInfo(session.request().user().username(), session.request().clientType(),
						RemoteSession.UNKNOWN_CLIENT_HOST.equals(clientHost) ? null : clientHost);
	}

	private void rollbackIfRequired(LocalEntityConnection entityConnection) {
		if (entityConnection.transactionOpen()) {
			LOG.info("Rollback open transaction on disconnect: {}", session);
			try {
				entityConnection.rollbackTransaction();
			}
			catch (DatabaseException e) {
				LOG.error("Rollback on disconnect failed: {}", session, e);
			}
		}
	}

	private LocalEntityConnection initializeConnection() {
		LocalEntityConnection connection = localEntityConnection(database, domain, connectionPool == null ?
						database.createConnection(session.databaseUser()) :
						connectionPool.connection(session.databaseUser()));
		((Traceable) connection).tracer(tracer);
		if (connectionPool != null) {
			Connection jdbcConnection = ((ConnectionHolder) connection).detach();
			rollbackSilently(jdbcConnection);
			closeSilently(jdbcConnection);
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
}

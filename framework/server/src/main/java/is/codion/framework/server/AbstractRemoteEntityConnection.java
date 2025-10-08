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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.logging.MethodTrace;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.rmi.RemoteEntityResultIterator;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static is.codion.common.Configuration.longValue;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.newSetFromMap;

/**
 * A base class for remote connections served by a {@link EntityServer}.
 * Handles logging of service calls and database connection pooling.
 */
public abstract class AbstractRemoteEntityConnection extends UnicastRemoteObject {

	@Serial
	private static final long serialVersionUID = 1;

	private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteEntityConnection.class);

	/**
	 * Specifies the timeout in milliseconds for idle remote iterators.
	 * Iterators that remain idle for longer than this timeout are automatically closed server-side.
	 * <ul>
	 * <li>Value type: Long
	 * <li>Default value: 300000 (5 minutes)
	 * </ul>
	 */
	static final PropertyValue<Long> ITERATOR_TIMEOUT = longValue("codion.db.remote.iteratorTimeout", TimeUnit.MINUTES.toMillis(5));

	/**
	 * A Proxy for logging method calls
	 */
	protected final transient EntityConnection connectionProxy;

	/**
	 * The proxy connection handler
	 */
	private final transient LocalConnectionHandler connectionHandler;

	/**
	 * An event triggered when this connection is closed
	 */
	private final transient Event<AbstractRemoteEntityConnection> closed = Event.event();

	private final Set<DefaultRemoteEntityResultIterator> remoteIterators = newSetFromMap(new ConcurrentHashMap<>());

	private final transient int connectionPort;
	private final transient RMIClientSocketFactory clientSocketFactory;
	private final transient RMIServerSocketFactory serverSocketFactory;

	/**
	 * Instantiates a new AbstractRemoteEntityConnection and exports it on the given port number
	 * @param domain the domain model
	 * @param database defines the underlying database
	 * @param remoteClient information about the client requesting the connection
	 * @param port the port to use when exporting this remote connection
	 * @param clientSocketFactory the client socket factory to use, null for default
	 * @param serverSocketFactory the server socket factory to use, null for default
	 * @throws RemoteException in case of an exception
	 * @throws DatabaseException in case a database connection can not be established, for example
	 * if a wrong username or password is provided
	 */
	protected AbstractRemoteEntityConnection(Domain domain, Database database,
																					 RemoteClient remoteClient, int port,
																					 RMIClientSocketFactory clientSocketFactory,
																					 RMIServerSocketFactory serverSocketFactory)
					throws RemoteException {
		super(port, clientSocketFactory, serverSocketFactory);
		this.connectionHandler = new LocalConnectionHandler(domain, remoteClient, database);
		this.connectionProxy = (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
						new Class[] {EntityConnection.class}, connectionHandler);
		this.clientSocketFactory = clientSocketFactory;
		this.serverSocketFactory = serverSocketFactory;
		this.connectionPort = port;
	}

	/**
	 * @return the user this connection is using
	 */
	public final User user() {
		return connectionHandler.remoteClient().user();
	}

	/**
	 * @return true if this connection is connected
	 */
	public final boolean connected() {
		synchronized (connectionProxy) {
			return connectionHandler.connected();
		}
	}

	/**
	 * Disconnects this connection
	 */
	public final void close() {
		synchronized (connectionProxy) {
			if (connectionHandler.closed()) {
				return;
			}
			try {
				UnicastRemoteObject.unexportObject(this, true);
			}
			catch (NoSuchObjectException e) {
				LOG.error(e.getMessage(), e);
			}
			remoteIterators.forEach(this::close);
			connectionHandler.close();
		}
		closed.accept(this);
	}

	/**
	 * @return the remote client using this remote connection
	 */
	final RemoteClient remoteClient() {
		return connectionHandler.remoteClient();
	}

	/**
	 * @return method traces
	 */
	final List<MethodTrace> methodTraces() {
		return connectionHandler.methodTraces();
	}

	/**
	 * @param timeout the number of milliseconds
	 * @return true if this connection has been inactive for {@code timeout} milliseconds or longer
	 */
	final boolean timedOut(int timeout) {
		return currentTimeMillis() - connectionHandler.lastAccessTime() > timeout;
	}

	final void setTraceToFile(boolean traceToFile) {
		connectionHandler.setTraceToFile(traceToFile);
	}

	final boolean isTraceToFile() {
		return connectionHandler.isTraceToFile();
	}

	final void setTracingEnabled(boolean tracingEnabled) {
		connectionHandler.setTracingEnabled(tracingEnabled);
	}

	final boolean isTracingEnabled() {
		return connectionHandler.isTracingEnabled();
	}

	/**
	 * @return true during a remote method call
	 */
	final boolean active() {
		return connectionHandler.active();
	}

	final Observer<AbstractRemoteEntityConnection> closed() {
		return closed.observer();
	}

	final RemoteEntityResultIterator remoteIterator(EntityResultIterator iterator) throws RemoteException {
		return new DefaultRemoteEntityResultIterator(connectionPort, clientSocketFactory, serverSocketFactory, iterator);
	}

	final void cleanupIterators() {
		remoteIterators.stream()
						.filter(DefaultRemoteEntityResultIterator::timedOut)
						.forEach(this::close);
	}

	private void close(RemoteEntityResultIterator iterator) {
		try {
			LOG.debug("Closing iterator for {}", user());
			iterator.close();
		}
		catch (Exception e) {
			LOG.error("Failed to close iterator for {}: {}", user(), e.getMessage(), e);
		}
	}

	static int requestsPerSecond() {
		return LocalConnectionHandler.REQUEST_COUNTER.requestsPerSecond();
	}

	private final class DefaultRemoteEntityResultIterator extends UnicastRemoteObject implements RemoteEntityResultIterator {

		@Serial
		private static final long serialVersionUID = 1;

		private final EntityResultIterator iterator;

		private final long timeout = ITERATOR_TIMEOUT.getOrThrow();

		private long lastAccessTime = currentTimeMillis();

		private DefaultRemoteEntityResultIterator(int port, RMIClientSocketFactory clientSocketFactory,
																							RMIServerSocketFactory serverSocketFactory,
																							EntityResultIterator iterator) throws RemoteException {
			super(port, clientSocketFactory, serverSocketFactory);
			this.iterator = iterator;
			remoteIterators.add(this);
		}

		@Override
		public boolean hasNext() throws RemoteException {
			lastAccessTime = currentTimeMillis();

			return iterator.hasNext();
		}

		@Override
		public Entity next() throws RemoteException {
			lastAccessTime = currentTimeMillis();

			return iterator.next();
		}

		@Override
		public void close() throws RemoteException {
			try {
				iterator.close();
			}
			catch (Exception ignored) {/*ignored*/}
			unexportObject(this, true);
			remoteIterators.remove(this);
		}

		boolean timedOut() {
			return currentTimeMillis() - lastAccessTime > timeout;
		}
	}
}

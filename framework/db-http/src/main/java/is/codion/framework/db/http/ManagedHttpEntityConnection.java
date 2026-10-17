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
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.framework.db.AbstractEntityConnection;
import is.codion.framework.db.EntityConnection;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A self-managing {@link HttpEntityConnection}, connecting on demand and reconnecting
 * when the underlying connection has gone bad.
 * @see HttpEntityConnection#builder()
 */
final class ManagedHttpEntityConnection extends AbstractEntityConnection
				implements HttpEntityConnection {

	private static final Logger LOG = LoggerFactory.getLogger(ManagedHttpEntityConnection.class);

	private final String hostname;
	private final int port;
	private final int securePort;
	private final boolean https;
	private final boolean json;
	private final int socketTimeout;
	private final int connectTimeout;

	private final Lock lock = new Lock() {};

	//the connection holding this client's session on the server, guarded by the lock
	private @Nullable EntityConnection current;

	ManagedHttpEntityConnection(DefaultHttpEntityConnectionBuilder builder) {
		super(builder);
		this.hostname = requireNonNull(builder.hostname, "hostname must be specified");
		this.port = builder.port;
		this.securePort = builder.securePort;
		this.https = builder.https;
		this.json = builder.json;
		this.socketTimeout = builder.socketTimeout;
		this.connectTimeout = builder.connectTimeout;
	}

	@Override
	public Optional<String> description() {
		return Optional.of(DESCRIPTION.optional().orElse(hostname));
	}

	@Override
	protected EntityConnection connect() {
		synchronized (lock) {
			EntityConnection previous = current;
			if (previous != null) {
				//the server hands the session it holds for an id back to a client connecting with it, so the session
				//of the connection being replaced is ended first, along with any transaction left open in it
				current = null;
				closeSilently(previous);
			}
			try {
				LOG.debug("Initializing connection for {}", user());
				AbstractHttpEntityConnection.DefaultBuilder builder =
								new AbstractHttpEntityConnection.DefaultBuilder()
												.domain(domainType());
				domain().ifPresent(builder::domain);
				EntityConnection connection = builder.hostname(hostname)
								.port(port)
								.securePort(securePort)
								.user(user())
								.clientType(clientType())
								.connectionId(id())
								.clientVersion(clientVersion().orElse(null))
								.json(json)
								.https(https)
								.socketTimeout(socketTimeout)
								.connectTimeout(connectTimeout)
								.build();
				current = connection;

				return connection;
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
		}
	}

	/**
	 * Closes the given connection, ending this client's session on the server, should it hold the session, the
	 * server knowing a client by its id alone. A connection which has been replaced does not, its session having
	 * been ended when its replacement connected, and closing it would end the session of its replacement.
	 * @param connection the connection to close
	 */
	@Override
	protected void close(EntityConnection connection) {
		synchronized (lock) {
			if (connection == current) {
				current = null;
				connection.close();
			}
		}
	}

	private static void closeSilently(EntityConnection connection) {
		try {
			connection.close();
		}
		catch (Exception e) {
			LOG.debug("Unable to close a replaced connection", e);
		}
	}

	private interface Lock {}
}

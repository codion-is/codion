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
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a HttpEntityConnection.
 * @see HttpEntityConnectionProvider#builder()
 */
final class DefaultHttpEntityConnectionProvider extends AbstractEntityConnectionProvider
				implements HttpEntityConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpEntityConnectionProvider.class);

	private final String hostName;
	private final int port;
	private final int securePort;
	private final boolean https;
	private final boolean json;
	private final int socketTimeout;
	private final int connectTimeout;
	private final Executor executor;

	DefaultHttpEntityConnectionProvider(DefaultHttpEntityConnectionProviderBuilder builder) {
		super(builder);
		this.hostName = requireNonNull(builder.hostName, "hostName must be specified");
		this.port = builder.port;
		this.securePort = builder.securePort;
		this.https = builder.https;
		this.json = builder.json;
		this.socketTimeout = builder.socketTimeout;
		this.connectTimeout = builder.connectTimeout;
		this.executor = builder.executor;
	}

	@Override
	public String connectionType() {
		return CONNECTION_TYPE_HTTP;
	}

	@Override
	public String description() {
		return hostName;
	}

	@Override
	protected EntityConnection connect() {
		try {
			LOG.debug("Initializing connection for {}", user());
			return HttpEntityConnection.builder()
							.domainType(domainType())
							.hostName(hostName)
							.port(port)
							.securePort(securePort)
							.user(user())
							.clientType(clientType())
							.clientId(clientId())
							.json(json)
							.https(https)
							.socketTimeout(socketTimeout)
							.connectTimeout(connectTimeout)
							.executor(executor)
							.build();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void close(EntityConnection connection) {
		connection.close();
	}
}

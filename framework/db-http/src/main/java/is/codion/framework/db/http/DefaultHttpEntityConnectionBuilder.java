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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.AbstractEntityConnection.AbstractBuilder;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Builds a self-managing {@link HttpEntityConnection}.
 * @see HttpEntityConnection#builder()
 */
public final class DefaultHttpEntityConnectionBuilder
				extends AbstractBuilder<HttpEntityConnection, HttpEntityConnection.Builder>
				implements HttpEntityConnection.Builder {

	String hostname = HttpEntityConnection.HOSTNAME.get();
	int port = HttpEntityConnection.PORT.getOrThrow();
	int securePort = HttpEntityConnection.SECURE_PORT.getOrThrow();
	boolean https = HttpEntityConnection.SECURE.getOrThrow();
	boolean json = HttpEntityConnection.JSON.getOrThrow();
	int socketTimeout = HttpEntityConnection.SOCKET_TIMEOUT.getOrThrow();
	int connectTimeout = HttpEntityConnection.CONNECT_TIMEOUT.getOrThrow();
	@Nullable Domain domain;

	/**
	 * Instantiates a new {@link DefaultHttpEntityConnectionBuilder}
	 */
	public DefaultHttpEntityConnectionBuilder() {
		super(EntityConnection.CONNECTION_TYPE_HTTP);
	}

	@Override
	public HttpEntityConnection.Builder domain(Domain domain) {
		this.domain = requireNonNull(domain);
		return domain(domain.type());
	}

	@Override
	public HttpEntityConnection.Builder hostname(String hostname) {
		this.hostname = requireNonNull(hostname);
		return this;
	}

	@Override
	public HttpEntityConnection.Builder port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public HttpEntityConnection.Builder securePort(int securePort) {
		this.securePort = securePort;
		return this;
	}

	@Override
	public HttpEntityConnection.Builder https(boolean https) {
		this.https = https;
		return this;
	}

	@Override
	public HttpEntityConnection.Builder json(boolean json) {
		this.json = json;
		return this;
	}

	@Override
	public HttpEntityConnection.Builder socketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
		return this;
	}

	@Override
	public HttpEntityConnection.Builder connectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	@Override
	protected HttpEntityConnection createConnection() {
		return new ManagedHttpEntityConnection(this);
	}
}

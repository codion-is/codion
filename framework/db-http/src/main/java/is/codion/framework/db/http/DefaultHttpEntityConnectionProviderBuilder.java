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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.db.EntityConnectionProvider;

import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

/**
 * Builds a {@link HttpEntityConnectionProvider} instance.
 * @see HttpEntityConnectionProvider#builder()
 */
public final class DefaultHttpEntityConnectionProviderBuilder
				extends AbstractBuilder<HttpEntityConnectionProvider, HttpEntityConnectionProvider.Builder>
				implements HttpEntityConnectionProvider.Builder {

	String hostName = HttpEntityConnection.HOSTNAME.get();
	int port = HttpEntityConnection.PORT.get();
	int securePort = HttpEntityConnection.SECURE_PORT.get();
	boolean https = HttpEntityConnection.SECURE.get();
	boolean json = HttpEntityConnection.JSON.get();
	int socketTimeout = HttpEntityConnection.SOCKET_TIMEOUT.get();
	int connectTimeout = HttpEntityConnection.CONNECT_TIMEOUT.get();
	Executor executor = AbstractHttpEntityConnection.DEFAULT_EXECUTOR;

	public DefaultHttpEntityConnectionProviderBuilder() {
		super(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
	}

	@Override
	public HttpEntityConnectionProvider.Builder hostName(String hostName) {
		this.hostName = requireNonNull(hostName);
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder securePort(int securePort) {
		this.securePort = securePort;
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder https(boolean https) {
		this.https = https;
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder json(boolean json) {
		this.json = json;
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder socketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder connectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	@Override
	public HttpEntityConnectionProvider.Builder executor(Executor executor) {
		this.executor = requireNonNull(executor);
		return this;
	}

	@Override
	public HttpEntityConnectionProvider build() {
		return new DefaultHttpEntityConnectionProvider(this);
	}
}

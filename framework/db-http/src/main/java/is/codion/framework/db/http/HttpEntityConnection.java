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

import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnection;

import static is.codion.common.utilities.Configuration.*;

/**
 * A factory for http based {@link EntityConnection} instances.
 * <p>
 * <b>Limitations:</b> {@code iterator(Condition)} and {@code iterator(Select)} are not
 * supported on http connections; they throw {@link UnsupportedOperationException}.
 * @see #HOSTNAME
 * @see #PORT
 * @see #SECURE_PORT
 * @see #SECURE
 * @see #JSON
 * @see #SOCKET_TIMEOUT
 * @see #CONNECT_TIMEOUT
 */
public interface HttpEntityConnection extends EntityConnection {

	/**
	 * The host on which to locate the http server
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: localhost
	 * </ul>
	 */
	PropertyValue<String> HOSTNAME = stringValue("codion.client.http.hostname", "localhost");

	/**
	 * The port which the http client should use.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 8080
	 * </ul>
	 */
	PropertyValue<Integer> PORT = integerValue("codion.client.http.port", 8080);

	/**
	 * The port which the https client should use
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 4443
	 * </ul>
	 */
	PropertyValue<Integer> SECURE_PORT = integerValue("codion.client.http.securePort", 4443);

	/**
	 * Specifies whether https should be used
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SECURE = booleanValue("codion.client.http.secure", true);

	/**
	 * Specifies whether json serialization should be used instead of standard Java serialization.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> JSON = booleanValue("codion.client.http.json", true);

	/**
	 * <p>The socket timeout in milliseconds, that is, how long an operation may wait for the server's response.
	 * <p>Zero (the default) means no timeout, matching the RMI transport, which does not impose one either. A
	 * finite value caps every operation, so it must exceed the slowest one the application performs - a report,
	 * a bulk update or a domain function may run far longer than a select.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 0 ms (no timeout)
	 * </ul>
	 */
	PropertyValue<Integer> SOCKET_TIMEOUT = integerValue("codion.client.http.socketTimeout", 0);

	/**
	 * <p>The connect timeout in milliseconds, that is, how long establishing the connection may take. Bounded
	 * work, unlike {@link #SOCKET_TIMEOUT}, so this one has a finite default - a misconfigured hostname or port
	 * should fail promptly rather than hang.
	 * <p>Zero means no timeout.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 10000 ms
	 * </ul>
	 */
	PropertyValue<Integer> CONNECT_TIMEOUT = integerValue("codion.client.http.connectTimeout", 10_000);

	/**
	 * <p>Instantiates a builder for a self-managing {@link HttpEntityConnection}, one which connects on demand
	 * and reconnects when the underlying connection has gone bad, serving for the lifetime of a client.
	 * @return a new builder instance
	 * @see is.codion.framework.db.AbstractEntityConnection
	 */
	static Builder builder() {
		return new DefaultHttpEntityConnectionBuilder();
	}

	/**
	 * Builds a self-managing http based {@link EntityConnection}.
	 * @see HttpEntityConnection#builder()
	 */
	interface Builder extends EntityConnection.Builder<HttpEntityConnection, Builder> {

		/**
		 * @param hostname the http server hostname
		 * @return this builder instance
		 */
		Builder hostname(String hostname);

		/**
		 * @param port the http server port
		 * @return this builder instance
		 */
		Builder port(int port);

		/**
		 * @param securePort the https server port
		 * @return this builder instance
		 */
		Builder securePort(int securePort);

		/**
		 * @param https true if https should be used
		 * @return this builder instance
		 */
		Builder https(boolean https);

		/**
		 * @param json true if json serialization should be used
		 * @return this builder instance
		 */
		Builder json(boolean json);

		/**
		 * @param socketTimeout the socket timeout
		 * @return this builder instance
		 */
		Builder socketTimeout(int socketTimeout);

		/**
		 * @param connectTimeout the connect timeout
		 * @return this builder instance
		 */
		Builder connectTimeout(int connectTimeout);
	}
}

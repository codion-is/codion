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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;

import java.util.UUID;
import java.util.concurrent.Executor;

import static is.codion.common.Configuration.*;

/**
 * A factory for http based EntityConnection builder.
 * @see #HOSTNAME
 * @see #PORT
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
	 * The port which the http client should use.<br>
	 * <ul>
	 * <li>Value type: Integer<br>
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
	 * <li>Value type: boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SECURE = booleanValue("codion.client.http.secure", true);

	/**
	 * Specifies whether json serialization should be used instead of standard Java serialization
	 * Value types: Boolean
	 * <ul>
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> JSON = booleanValue("codion.client.http.json", false);

	/**
	 * The socket timeout in milliseconds
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 2000 ms
	 * </ul>
	 */
	PropertyValue<Integer> SOCKET_TIMEOUT = integerValue("codion.client.http.socketTimeout", 2000);

	/**
	 * The connect timeout in milliseconds
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 2000 ms
	 * </ul>
	 */
	PropertyValue<Integer> CONNECT_TIMEOUT = integerValue("codion.client.http.connectTimeout", 2000);

	/**
	 * Specifies whether HTTP connections should send a close request to the server when closing.
	 * <p>
	 * When {@code true} (default), the connection will notify the server when closing by sending
	 * a "close" request. This allows stateful servers to clean up resources associated with the
	 * connection.
	 * <p>
	 * When {@code false}, the connection will close immediately without notifying the server.
	 * This is useful for stateless deployments such as serverless functions (e.g. AWS Lambda),
	 * where sending a close request would unnecessarily invoke the function, causing delays
	 * and incurring costs.
	 * <p>
	 * Value types: Boolean
	 * <ul>
	 * <li>Default value: true
	 * </ul>
	 * @see #close()
	 */
	PropertyValue<Boolean> DISCONNECT_ON_CLOSE = booleanValue("codion.client.http.disconnectOnClose", true);

	/**
	 * @return a new builder instance
	 */
	static Builder builder() {
		return new AbstractHttpEntityConnection.DefaultBuilder();
	}

	/**
	 * Builds a http based EntityConnection
	 */
	interface Builder {

		/**
		 * @param domainType the domain model type
		 * @return this builder instance
		 */
		Builder domainType(DomainType domainType);

		/**
		 * @param hostName the http server host name
		 * @return this builder instance
		 */
		Builder hostName(String hostName);

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

		/**
		 * @param user the user
		 * @return this builder instance
		 */
		Builder user(User user);

		/**
		 * @param clientType the client type
		 * @return this builder instance
		 */
		Builder clientType(String clientType);

		/**
		 * @param clientId the client id
		 * @return this builder instance
		 */
		Builder clientId(UUID clientId);

		/**
		 * By default, the http client uses a shared thread pool executor.
		 * @param executor the http client executor to use
		 * @return this builder instance
		 */
		Builder executor(Executor executor);

		/**
		 * @return a http based EntityConnection
		 */
		EntityConnection build();
	}
}

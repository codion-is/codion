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
 * Copyright (c) 2015 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.rmi.client.ConnectionRequest.Builder.UserStep;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;

import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Encapsulates information about a client required by a server for establishing a connection
 */
public interface ConnectionRequest {

	/**
	 * @return the user
	 */
	User user();

	/**
	 * @return the client id
	 */
	UUID clientId();

	/**
	 * @return the client type
	 */
	String clientType();

	/**
	 * @return the client locale, captured from the client JVM default when the request is built
	 */
	Locale locale();

	/**
	 * @return the client time zone, captured from the client JVM default when the request is built
	 */
	ZoneId timeZone();

	/**
	 * @return the client version
	 */
	Optional<Version> version();

	/**
	 * @return the version of Codion the client is using
	 */
	Version frameworkVersion();

	/**
	 * @return misc. parameters, an empty map if none are specified
	 */
	Map<String, Object> parameters();

	/**
	 * @return a copy of this connection request with a copy of the user instance
	 */
	ConnectionRequest copy();

	/**
	 * @return a {@link UserStep}
	 */
	static UserStep builder() {
		return DefaultConnectionRequest.DefaultBuilder.USER;
	}

	/**
	 * A builder for ConnectionRequest
	 */
	interface Builder {

		/**
		 * Specifies the connection user
		 */
		interface UserStep {

			/**
			 * @param user the user, required
			 * @return a {@link ClientTypeStep} instance
			 */
			ClientTypeStep user(User user);
		}

		/**
		 * Specifies the client type
		 */
		interface ClientTypeStep {

			/**
			 * @param clientType the client type, required
			 * @return a Builder instance
			 */
			Builder clientType(String clientType);
		}

		/**
		 * @param clientId the client id, a random {@link UUID} by default
		 * @return this Builder instance
		 */
		Builder clientId(UUID clientId);

		/**
		 * @param version the client version
		 * @return this Builder instance
		 */
		Builder version(@Nullable Version version);

		/**
		 * @param key the key
		 * @param value the value
		 * @return this Builder instance
		 */
		Builder parameter(String key, Object value);

		/**
		 * @return a new ConnectionRequest instance
		 * @throws NullPointerException in case the user or client type have not been specified
		 */
		ConnectionRequest build();
	}
}

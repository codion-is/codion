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
 * Copyright (c) 2015 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

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
	 * @return the client locale
	 */
	Locale locale();

	/**
	 * @return the client time zone
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
	 * @return a {@link ConnectionRequest.Builder}
	 */
	static Builder builder() {
		return new DefaultConnectionRequest.DefaultBuilder();
	}

	/**
	 * A builder for ConnectionRequest
	 */
	interface Builder {

		/**
		 * @param user the user
		 * @return this Builder instance
		 */
		Builder user(User user);

		/**
		 * @param clientId the client id
		 * @return this Builder instance
		 */
		Builder clientId(UUID clientId);

		/**
		 * @param clientType the client type
		 * @return this Builder instance
		 */
		Builder clientType(String clientType);

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
		 */
		ConnectionRequest build();
	}
}

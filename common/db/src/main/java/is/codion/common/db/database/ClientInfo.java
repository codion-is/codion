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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * <p>Identifies the client on whose behalf a database connection is being used, for
 * {@link Database#clientInfo(java.sql.Connection, ClientInfo)}.
 * <p>Note that {@link #user()} is the <i>application's</i> user, not the database user. The two coincide
 * only when clients log in to the database directly; the whole point of carrying this is the case where
 * they do not, and the database would otherwise see nothing but the shared user every client shares.
 */
public final class ClientInfo {

	private final String user;
	private final String clientType;
	private final @Nullable String host;

	/**
	 * @param user the application user the connection is being used on behalf of
	 * @param clientType the client type
	 * @param host the host the client is running on, null if unknown
	 */
	public ClientInfo(String user, String clientType, @Nullable String host) {
		this.user = requireNonNull(user);
		this.clientType = requireNonNull(clientType);
		this.host = host;
	}

	/**
	 * @return the application user, not the database user
	 */
	public String user() {
		return user;
	}

	/**
	 * @return the client type
	 */
	public String clientType() {
		return clientType;
	}

	/**
	 * @return the client host, an empty {@link Optional} if unknown
	 */
	public Optional<String> host() {
		return Optional.ofNullable(host);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof ClientInfo)) {
			return false;
		}
		ClientInfo that = (ClientInfo) object;

		return user.equals(that.user) && clientType.equals(that.clientType) && Objects.equals(host, that.host);
	}

	@Override
	public int hashCode() {
		return Objects.hash(user, clientType, host);
	}

	@Override
	public String toString() {
		return clientType + " (" + user + (host == null ? "" : "@" + host) + ")";
	}
}

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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultRemoteClient implements RemoteClient, Serializable {

	private static final long serialVersionUID = 1;

	private final ConnectionRequest connectionRequest;
	private final User databaseUser;
	private final String clientHost;
	private final LocalDateTime creationTime;

	DefaultRemoteClient(ConnectionRequest connectionRequest, User databaseUser, String clientHost) {
		this(connectionRequest, databaseUser, clientHost, LocalDateTime.now());
	}

	DefaultRemoteClient(ConnectionRequest connectionRequest, User databaseUser, String clientHost, LocalDateTime creationTime) {
		this.connectionRequest = requireNonNull(connectionRequest, "connectionRequest");
		this.databaseUser = requireNonNull(databaseUser, "databaseUser");
		this.clientHost = clientHost;
		this.creationTime = creationTime;
	}

	@Override
	public ConnectionRequest connectionRequest() {
		return connectionRequest;
	}

	@Override
	public LocalDateTime creationTime() {
		return creationTime;
	}

	@Override
	public User user() {
		return connectionRequest.user();
	}

	@Override
	public User databaseUser() {
		return databaseUser;
	}

	@Override
	public UUID clientId() {
		return connectionRequest.clientId();
	}

	@Override
	public String clientTypeId() {
		return connectionRequest.clientTypeId();
	}

	@Override
	public Locale clientLocale() {
		return connectionRequest.clientLocale();
	}

	@Override
	public ZoneId clientTimeZone() {
		return connectionRequest.clientTimeZone();
	}

	@Override
	public Version clientVersion() {
		return connectionRequest.clientVersion();
	}

	@Override
	public Version frameworkVersion() {
		return connectionRequest.frameworkVersion();
	}

	@Override
	public Map<String, Object> parameters() {
		return connectionRequest.parameters();
	}

	@Override
	public String clientHost() {
		return clientHost;
	}

	@Override
	public RemoteClient withDatabaseUser(User databaseUser) {
		return new DefaultRemoteClient(connectionRequest, databaseUser, clientHost, creationTime);
	}

	@Override
	public RemoteClient copy() {
		return new DefaultRemoteClient(connectionRequest.copy(), databaseUser.copy(), clientHost, creationTime);
	}

	@Override
	public int hashCode() {
		return connectionRequest.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof RemoteClient && connectionRequest.equals(((RemoteClient) obj).connectionRequest());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(connectionRequest.user().toString());
		if (databaseUser != null && !connectionRequest.user().equals(databaseUser)) {
			builder.append(" (databaseUser: ").append(databaseUser).append(")");
		}
		builder.append("@").append(clientHost == null ? "unknown" : clientHost).append(" [").append(connectionRequest.clientTypeId())
						.append(connectionRequest.clientVersion() != null ? "-" + connectionRequest.clientVersion() : "")
						.append("] - ").append(connectionRequest.clientId().toString());

		return builder.toString();
	}
}

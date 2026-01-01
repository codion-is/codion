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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultRemoteClient implements RemoteClient, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final ConnectionRequest connectionRequest;
	private final User databaseUser;
	private final String clientHost;
	private final LocalDateTime creationTime;

	DefaultRemoteClient(DefaultBuilder builder) {
		this.connectionRequest = builder.connectionRequest;
		this.databaseUser = builder.databaseUser;
		this.clientHost = builder.clientHost;
		this.creationTime = LocalDateTime.now();
	}

	DefaultRemoteClient(DefaultRemoteClient remoteClient) {
		this.connectionRequest = remoteClient.connectionRequest.copy();
		this.databaseUser = remoteClient.databaseUser.copy();
		this.clientHost = remoteClient.clientHost;
		this.creationTime = remoteClient.creationTime;
	}

	DefaultRemoteClient(DefaultRemoteClient remoteClient, User databaseUser) {
		this.connectionRequest = remoteClient.connectionRequest;
		this.databaseUser = databaseUser;
		this.clientHost = remoteClient.clientHost;
		this.creationTime = remoteClient.creationTime;
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
	public String clientType() {
		return connectionRequest.clientType();
	}

	@Override
	public Locale locale() {
		return connectionRequest.locale();
	}

	@Override
	public ZoneId timeZone() {
		return connectionRequest.timeZone();
	}

	@Override
	public Optional<Version> version() {
		return connectionRequest.version();
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
		return new DefaultRemoteClient(this, requireNonNull(databaseUser));
	}

	@Override
	public RemoteClient copy() {
		return new DefaultRemoteClient(this);
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
		builder.append("@").append(clientHost).append(" [").append(connectionRequest.clientType())
						.append(connectionRequest.version().map(version -> "-" + version).orElse(""))
						.append("] - ").append(connectionRequest.clientId());

		return builder.toString();
	}

	static final class DefaultBuilder implements Builder {

		private final ConnectionRequest connectionRequest;

		private String clientHost = UNKNOWN_CLIENT_HOST;
		private User databaseUser;

		DefaultBuilder(ConnectionRequest connectionRequest) {
			this.connectionRequest = requireNonNull(connectionRequest);
			this.databaseUser = connectionRequest.user().copy();
		}

		@Override
		public Builder clientHost(String clientHost) {
			this.clientHost = requireNonNull(clientHost);
			return this;
		}

		@Override
		public Builder databaseUser(User databaseUser) {
			this.databaseUser = requireNonNull(databaseUser);
			return this;
		}

		@Override
		public RemoteClient build() {
			return new DefaultRemoteClient(this);
		}
	}
}

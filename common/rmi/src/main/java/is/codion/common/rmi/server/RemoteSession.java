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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.utilities.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>Contains basic information about a remote client session, the server's view of a connected client.
 * <p>A session lasts exactly as long as the connection it serves and is identified by its
 * {@link ConnectionRequest#connectionId() connectionId}, see {@link #id()}. A client establishing more
 * than one connection therefore has more than one session.
 * @see #builder(ConnectionRequest)
 */
public sealed interface RemoteSession permits DefaultRemoteSession {

	/**
	 * @see #clientHost()
	 */
	String UNKNOWN_CLIENT_HOST = "unknown host";

	/**
	 * @return the initial connection request this session is based on
	 */
	ConnectionRequest request();

	/**
	 * <p>The id of the connection this session serves, and thereby the session's own, the two sharing a
	 * lifetime. This is the id the client knows the connection by, see {@code EntityConnection.id()}.
	 * @return the id of this session
	 */
	UUID id();

	/**
	 * @return the time when this session was created
	 */
	LocalDateTime creationTime();

	/**
	 * @return the user used when connecting to the underlying database
	 */
	User databaseUser();

	/**
	 * Note that if the client host is not known {@link #UNKNOWN_CLIENT_HOST} is returned.
	 * @return the client hostname
	 */
	String clientHost();

	/**
	 * Instantiates a new {@link RemoteSession} based on this instance
	 * but with the specified database user
	 * @param databaseUser the database user to use
	 * @return a new {@link RemoteSession} instance
	 */
	RemoteSession withDatabaseUser(User databaseUser);

	/**
	 * @return a copy of this remote session with copies of its user instances
	 */
	RemoteSession copy();

	/**
	 * Instantiates a new {@link RemoteSession.Builder}.
	 * @param connectionRequest the connection request
	 * @return a new builder
	 */
	static Builder builder(ConnectionRequest connectionRequest) {
		return new DefaultRemoteSession.DefaultBuilder(connectionRequest);
	}

	/**
	 * Builds a {@link RemoteSession}
	 */
	interface Builder {

		/**
		 * @param clientHost the client host
		 * @return this builder instance
		 */
		Builder clientHost(String clientHost);

		/**
		 * @param databaseUser the database user
		 * @return this builder instance
		 */
		Builder databaseUser(User databaseUser);

		/**
		 * @return a new {@link RemoteSession} instance based on this builder
		 */
		RemoteSession build();
	}
}
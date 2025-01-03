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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;

import java.time.LocalDateTime;

/**
 * Contains basic information about a remote client
 * @see #builder(ConnectionRequest)
 */
public interface RemoteClient extends ConnectionRequest {

	/**
	 * @see #clientHost()
	 */
	String UNKNOWN_HOST = "unknown host";

	/**
	 * @return the initial connection request this client is based on
	 */
	ConnectionRequest connectionRequest();

	/**
	 * @return the time when this client connection was created
	 */
	LocalDateTime creationTime();

	/**
	 * @return the user used when connecting to the underlying database
	 */
	User databaseUser();

	/**
	 * Note that if the client host is not known {@link #UNKNOWN_HOST} is returned.
	 * @return the client hostname
	 */
	String clientHost();

	/**
	 * Instantiates a new RemoteClient based on this instance
	 * but with the specified database user
	 * @param databaseUser the database user to use
	 * @return a new RemoteClient instance
	 */
	RemoteClient withDatabaseUser(User databaseUser);

	/**
	 * @return a copy of this remote client with copies of its user instances
	 */
	RemoteClient copy();

	/**
	 * Instantiates a new {@link RemoteClient.Builder}.
	 * @param connectionRequest the connection request
	 * @return a new builder
	 */
	static Builder builder(ConnectionRequest connectionRequest) {
		return new DefaultRemoteClient.DefaultBuilder(connectionRequest);
	}

	/**
	 * Builds a {@link RemoteClient}
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
		 * @return a new {@link RemoteClient} instance based on this builder
		 */
		RemoteClient build();
	}
}
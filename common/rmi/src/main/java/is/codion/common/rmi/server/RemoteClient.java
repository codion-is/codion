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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;

import java.time.LocalDateTime;

/**
 * Contains basic information about a remote client
 */
public interface RemoteClient extends ConnectionRequest {

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
	 * Instantiates a new RemoteClient
	 * @param connectionRequest the connection request
	 * @return a new RemoteClient instance
	 */
	static RemoteClient remoteClient(ConnectionRequest connectionRequest) {
		return remoteClient(connectionRequest, connectionRequest.user());
	}

	/**
	 * Instantiates a new RemoteClient
	 * @param connectionRequest the connection request
	 * @param clientHost the client hostname
	 * @return a new RemoteClient instance
	 */
	static RemoteClient remoteClient(ConnectionRequest connectionRequest, String clientHost) {
		return remoteClient(connectionRequest, connectionRequest.user(), clientHost);
	}

	/**
	 * Instantiates a new RemoteClient
	 * @param connectionRequest the connection request
	 * @param databaseUser the user to use when connecting to the underlying database
	 * @return a new RemoteClient instance
	 */
	static RemoteClient remoteClient(ConnectionRequest connectionRequest, User databaseUser) {
		return remoteClient(connectionRequest, databaseUser, null);
	}

	/**
	 * Instantiates a new RemoteClient
	 * @param connectionRequest the connection request
	 * @param databaseUser the user to use when connecting to the underlying database
	 * @param clientHost the client hostname
	 * @return a new RemoteClient instance
	 */
	static RemoteClient remoteClient(ConnectionRequest connectionRequest, User databaseUser, String clientHost) {
		return new DefaultRemoteClient(connectionRequest, databaseUser, clientHost);
	}
}
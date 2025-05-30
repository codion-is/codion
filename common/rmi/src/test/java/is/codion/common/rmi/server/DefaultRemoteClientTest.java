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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class DefaultRemoteClientTest {

	@Test
	void copy() {
		RemoteClient client = RemoteClient.builder(ConnectionRequest.builder()
										.user(User.user("scott"))
										.type("DefaultRemoteClientTest")
										.build())
						.databaseUser(User.user("john"))
						.build();
		RemoteClient copy = client.copy();
		assertNotSame(client.connectionRequest(), copy.connectionRequest());
		assertNotSame(client.user(), copy.user());
		assertNotSame(client.databaseUser(), copy.databaseUser());
		assertSame(client.creationTime(), copy.creationTime());
		assertSame(client.clientHost(), copy.clientHost());
	}

	@Test
	void withDatabaseUser() {
		RemoteClient client = RemoteClient.builder(ConnectionRequest.builder()
										.user(User.user("scott"))
										.type("DefaultRemoteClientTest")
										.build())
						.databaseUser(User.user("john"))
						.build();
		RemoteClient copy = client.withDatabaseUser(User.user("peter"));
		assertSame(client.connectionRequest(), copy.connectionRequest());
		assertSame(client.user(), copy.user());
		assertNotSame(client.databaseUser(), copy.databaseUser());
		assertSame(client.creationTime(), copy.creationTime());
		assertSame(client.clientHost(), copy.clientHost());
	}
}

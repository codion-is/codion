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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class DefaultRemoteSessionTest {

	@Test
	void copy() {
		RemoteSession session = RemoteSession.builder(ConnectionRequest.builder()
										.user(User.user("scott"))
										.clientType("DefaultRemoteSessionTest")
										.build())
						.databaseUser(User.user("john"))
						.build();
		RemoteSession copy = session.copy();
		assertNotSame(session.request(), copy.request());
		assertNotSame(session.request().user(), copy.request().user());
		assertNotSame(session.databaseUser(), copy.databaseUser());
		assertSame(session.creationTime(), copy.creationTime());
		assertSame(session.clientHost(), copy.clientHost());
	}

	@Test
	void withDatabaseUser() {
		RemoteSession session = RemoteSession.builder(ConnectionRequest.builder()
										.user(User.user("scott"))
										.clientType("DefaultRemoteSessionTest")
										.build())
						.databaseUser(User.user("john"))
						.build();
		RemoteSession copy = session.withDatabaseUser(User.user("peter"));
		assertSame(session.request(), copy.request());
		assertSame(session.request().user(), copy.request().user());
		assertNotSame(session.databaseUser(), copy.databaseUser());
		assertSame(session.creationTime(), copy.creationTime());
		assertSame(session.clientHost(), copy.clientHost());
	}
}

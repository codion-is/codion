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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientsTest {

	@Test
	void connectionRequest() {
		User user = User.parse("scott:tiger");
		UUID uuid = UUID.randomUUID();
		ConnectionRequest request = ConnectionRequest.builder()
						.user(user)
						.id(uuid)
						.type("test")
						.build();
		assertEquals(user, request.user());
		assertEquals(uuid, request.id());
		assertFalse(request.version().isPresent());
		assertEquals(Version.version(), request.frameworkVersion());
		assertEquals(uuid.hashCode(), request.hashCode());
		assertEquals("test", request.type());
		assertTrue(request.toString().contains(user.username()));
	}
}

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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class HttpEntityConnectionTest {

	@Test
	void connectionTypeHttp_resolvesHttpBuilder() {
		EntityConnection.CLIENT_CONNECTION_TYPE.set(EntityConnection.CONNECTION_TYPE_HTTP);
		try {
			//building would connect, there being no server to connect to here
			assertInstanceOf(HttpEntityConnection.Builder.class, EntityConnection.builder()
							.domain(TestDomain.DOMAIN)
							.clientType("test")
							.user(User.parse("scott:tiger")));
		}
		finally {
			EntityConnection.CLIENT_CONNECTION_TYPE.set(null);
		}
	}
}

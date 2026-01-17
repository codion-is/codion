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
import is.codion.framework.db.EntityConnectionProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class HttpEntityConnectionProviderTest {

	@Test
	void entityConnectionProviderBuilder() {
		EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
		try {
			EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
							.domain(TestDomain.DOMAIN)
							.clientType("test")
							.user(User.parse("scott:tiger"))
							.build();
			assertInstanceOf(HttpEntityConnectionProvider.class, connectionProvider);
		}
		finally {
			EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(null);
		}
	}
}

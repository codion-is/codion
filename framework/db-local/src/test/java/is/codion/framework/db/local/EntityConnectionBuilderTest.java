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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.TestDomain.Department;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityConnectionBuilderTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void builder() {
		EntityConnection.CLIENT_CONNECTION_TYPE.set(EntityConnection.CONNECTION_TYPE_LOCAL);
		try (EntityConnection connection = EntityConnection.builder()
						.domain(new TestDomain())
						.clientType(EntityConnectionBuilderTest.class.getSimpleName())
						.user(UNIT_TEST_USER)
						.build()) {
			assertTrue(connection.connected());
			assertNotNull(connection.entities().definition(Department.TYPE));
			assertFalse(connection.select(all(Department.TYPE)).isEmpty());

			//close is terminal, and closing again via try-with-resources has no effect
			connection.close();
			assertFalse(connection.connected());
			assertThrows(IllegalStateException.class, () -> connection.select(all(Department.TYPE)));
		}
		finally {
			EntityConnection.CLIENT_CONNECTION_TYPE.remove();
		}
	}
}

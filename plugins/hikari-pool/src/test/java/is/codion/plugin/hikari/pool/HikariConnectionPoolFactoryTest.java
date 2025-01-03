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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.hikari.pool;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HikariConnectionPoolFactoryTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void test() throws Exception {
		long startTime = System.currentTimeMillis();
		HikariConnectionPoolFactory provider = new HikariConnectionPoolFactory();
		ConnectionPoolWrapper pool = provider.createConnectionPool(
						H2DatabaseFactory.createDatabase("jdbc:h2:mem:HikariConnectionPoolFactoryTest",
										Database.DATABASE_INIT_SCRIPTS.get()), UNIT_TEST_USER);
		pool.setCollectSnapshotStatistics(true);
		assertTrue(pool.isCollectSnapshotStatistics());
		pool.connection(UNIT_TEST_USER).close();
		pool.connection(UNIT_TEST_USER).close();
		pool.connection(UNIT_TEST_USER).close();
		pool.statistics(startTime).snapshot();
		pool.close();
	}
}

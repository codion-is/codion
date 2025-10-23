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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.local.TestDomain.Department;
import is.codion.framework.domain.entity.Entities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class TransactionTest {

	private static final User UNIT_TEST_USER = User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Database DATABASE = Database.instance();

	private LocalEntityConnection connection;

	@BeforeEach
	void setup() {
		connection = createConnection();
		// Clean up any test data
		cleanupTestData();
	}

	@AfterEach
	void tearDown() {
		try {
			if (connection != null && connection.transactionOpen()) {
				connection.rollbackTransaction();
			}
			cleanupTestData();
			if (connection != null) {
				connection.close();
			}
		}
		catch (Exception ignored) {
			// Ignore cleanup errors
		}
	}


	@Test
	@DisplayName("Savepoints work correctly within transactions")
	void transaction_savepoints_shouldWorkCorrectly() throws SQLException {
		connection.startTransaction();

		Entities entities = connection.entities();

		// Insert first record
		connection.insert(entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 99)
						.with(Department.DNAME, "FIRST")
						.with(Department.LOC, "CITY1")
						.build());

		// Create savepoint
		Savepoint savepoint1 = connection.getConnection().setSavepoint("sp1");

		// Insert second record
		connection.insert(entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 98)
						.with(Department.DNAME, "SECOND")
						.with(Department.LOC, "CITY2")
						.build());

		// Verify both exist
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(99))));
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(98))));

		// Rollback to savepoint
		connection.getConnection().rollback(savepoint1);

		// First should still exist, second should not
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(99))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(98))));

		connection.commitTransaction();

		// Verify persistence after commit
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(99))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(98))));
	}

	@Test
	@DisplayName("Nested savepoints work correctly")
	void transaction_nestedSavepoints_shouldWorkCorrectly() throws SQLException {
		Entities entities = connection.entities();

		connection.startTransaction();

		Connection conn = connection.getConnection();

		// Level 1
		connection.insert(entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 97)
						.with(Department.DNAME, "LEVEL1")
						.with(Department.LOC, "LOC1")
						.build());
		Savepoint sp1 = conn.setSavepoint("sp1");

		// Level 2
		connection.insert(entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 96)
						.with(Department.DNAME, "LEVEL2")
						.with(Department.LOC, "LOC2")
						.build());
		Savepoint sp2 = conn.setSavepoint("sp2");

		// Level 3
		connection.insert(entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 95)
						.with(Department.DNAME, "LEVEL3")
						.with(Department.LOC, "LOC4")
						.build());

		// All should exist
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(97))));
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(96))));
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(95))));

		// Rollback to sp2 - should lose level 3
		conn.rollback(sp2);
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(97))));
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(96))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(95))));

		// Rollback to sp1 - should lose level 2
		conn.rollback(sp1);
		assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(97))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(96))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(95))));

		connection.rollbackTransaction();

		// All should be gone
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(97))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(96))));
		assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(95))));
	}

	@Test
	@DisplayName("Transaction with exception maintains consistency")
	void transaction_withException_shouldMaintainConsistency() {
		Entities entities = connection.entities();
		connection.startTransaction();
		try {
			// Insert valid record
			connection.insert(entities.entity(Department.TYPE)
							.with(Department.DEPTNO, 93)
							.with(Department.DNAME, "VALID")
							.with(Department.LOC, "LOC")
							.build());
			assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(93))));

			// Try to insert duplicate (should fail)
			assertThrows(DatabaseException.class, () ->
							connection.insert(entities.entity(Department.TYPE)
											.with(Department.DEPTNO, 93)
											.with(Department.DNAME, "DUPLICATE")
											.with(Department.LOC, "LOC")
											.build()));

			// Original should still be in transaction
			assertEquals(1, connection.count(Count.where(Department.DEPTNO.equalTo(93))));

			// Rollback due to error
			connection.rollbackTransaction();

			// Should be gone
			assertEquals(0, connection.count(Count.where(Department.DEPTNO.equalTo(93))));
		}
		catch (Exception e) {
			if (connection.transactionOpen()) {
				connection.rollbackTransaction();
			}
			throw e;
		}
	}

	private void cleanupTestData() {
		connection.delete(Department.DEPTNO.between(90, 99));
	}

	private static LocalEntityConnection createConnection() {
		return new DefaultLocalEntityConnection(DATABASE, new TestDomain(), UNIT_TEST_USER);
	}
}
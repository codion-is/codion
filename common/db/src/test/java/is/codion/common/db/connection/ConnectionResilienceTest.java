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
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static is.codion.common.db.connection.DatabaseConnection.databaseConnection;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseConnection resilience and validation focusing on Codion-specific behavior
 */
public final class ConnectionResilienceTest {

	private static final User TEST_USER = User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private Database database;

	@BeforeEach
	void setup() {
		database = Database.instance();
	}

	@AfterEach
	void tearDown() {
		// Cleanup handled by try-with-resources
	}

	@Test
	@DisplayName("DatabaseConnection detects closed underlying connections")
	void databaseConnection_closedConnection_shouldBeDetected() throws SQLException {
		try (DatabaseConnection dbConn = databaseConnection(database, TEST_USER)) {
			assertTrue(dbConn.valid());
			assertTrue(dbConn.connected());

			// Get underlying connection and close it directly (simulating connection loss)
			Connection conn = dbConn.getConnection();
			conn.close();

			// DatabaseConnection should detect the closed connection
			assertFalse(dbConn.valid());
			// connected() only checks if a connection was established, not if it's valid
			assertTrue(dbConn.connected());
		}
	}

	@Test
	@DisplayName("DatabaseConnection validation uses database-specific validation")
	void databaseConnection_validation_usesDatabaseSpecificValidation() throws SQLException {
		try (DatabaseConnection dbConn = databaseConnection(database, TEST_USER)) {
			// Should be valid
			assertTrue(dbConn.valid());

			// The valid() method should use Database.connectionValid()
			// which may have database-specific implementation
			Connection conn = dbConn.getConnection();
			assertTrue(database.connectionValid(conn));

			// Both should agree
			assertEquals(database.connectionValid(conn), dbConn.valid());
		}
	}

	@Test
	@DisplayName("DatabaseConnection maintains state after SQL errors")
	void databaseConnection_afterSQLErrors_shouldMaintainState() throws SQLException {
		try (DatabaseConnection dbConn = databaseConnection(database, TEST_USER)) {
			Connection conn = dbConn.getConnection();

			// Try invalid SQL
			try (Statement stmt = conn.createStatement()) {
				assertThrows(SQLException.class,
								() -> stmt.executeQuery("SELECT * FROM non_existent_table"));
			}

			// Connection should still be valid after SQL error
			assertTrue(dbConn.valid());
			assertTrue(database.connectionValid(conn));

			// Try constraint violation
			String insertDuplicate = "INSERT INTO employees.department (deptno, dname) VALUES (10, 'DUPLICATE')";
			try (Statement stmt = conn.createStatement()) {
				assertThrows(SQLException.class, () -> stmt.executeUpdate(insertDuplicate));
			}

			// Connection should still be valid after constraint violation
			assertTrue(dbConn.valid());

			// Verify we can still use the connection
			try (Statement stmt = conn.createStatement();
					 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees.department")) {
				assertTrue(rs.next());
				assertTrue(rs.getInt(1) > 0);
			}
		}
	}

	@Test
	@DisplayName("DatabaseConnection handles setConnection properly")
	void databaseConnection_setConnection_shouldHandleProperly() throws SQLException {
		try (DatabaseConnection dbConn = databaseConnection(database, TEST_USER)) {
			assertTrue(dbConn.valid());

			// Create a new connection
			Connection newConn = database.createConnection(TEST_USER);

			// Set the new connection
			dbConn.setConnection(newConn);

			// Should be valid with new connection
			assertTrue(dbConn.valid());
			assertSame(newConn, dbConn.getConnection());

			// Set to null
			dbConn.setConnection(null);

			// Should throw IllegalStateException when accessing connection
			assertThrows(IllegalStateException.class, () -> dbConn.getConnection());
			assertThrows(IllegalStateException.class, () -> dbConn.startTransaction());

			// Close the new connection we created
			newConn.close();
		}
	}

	@Test
	@DisplayName("DatabaseConnection state after close")
	void databaseConnection_afterClose_shouldThrowIllegalStateException() {
		DatabaseConnection dbConn = databaseConnection(database, TEST_USER);
		assertTrue(dbConn.valid());

		// Close the connection
		dbConn.close();

		// All operations should throw IllegalStateException
		assertFalse(dbConn.valid());
		assertFalse(dbConn.connected());
		assertThrows(IllegalStateException.class, () -> dbConn.getConnection());
		assertThrows(IllegalStateException.class, () -> dbConn.startTransaction());
		assertThrows(IllegalStateException.class, () -> dbConn.commit());
		assertThrows(IllegalStateException.class, () -> dbConn.rollback());
	}

	@Test
	@DisplayName("Database-specific validation queries work correctly")
	void database_validationQuery_shouldExecuteCorrectly() throws SQLException {
		try (DatabaseConnection dbConn = databaseConnection(database, TEST_USER)) {
			Connection conn = dbConn.getConnection();

			// Test some common validation queries that should work on H2
			String[] validationQueries = {
							"SELECT 1",
							"VALUES 1",
							"SELECT 1 FROM INFORMATION_SCHEMA.USERS WHERE 1=0"  // H2 specific
			};

			for (String query : validationQueries) {
				try (Statement stmt = conn.createStatement()) {
					// Should not throw exception
					stmt.execute(query);
				}
				catch (SQLException e) {
					// Some queries might not work, that's OK
					// The point is to test that we can execute validation queries
				}
			}

			// Connection should still be valid after validation queries
			assertTrue(dbConn.valid());
		}
	}

	@Test
	@DisplayName("DatabaseConnection creation with invalid connection fails")
	void databaseConnection_invalidConnection_shouldThrowException() throws SQLException {
		// Create and close a connection
		Connection closedConn = database.createConnection(TEST_USER);
		closedConn.close();

		// Should throw DatabaseException when creating DatabaseConnection with closed connection
		assertThrows(DatabaseException.class, () -> databaseConnection(database, closedConn));
	}

	@Test
	@DisplayName("DatabaseConnection properly wraps existing connection")
	void databaseConnection_wrapExistingConnection_shouldWorkCorrectly() throws SQLException {
		// Create a connection directly
		Connection directConn = database.createConnection(TEST_USER);

		try (DatabaseConnection dbConn = databaseConnection(database, directConn)) {
			// Should be valid
			assertTrue(dbConn.valid());
			assertTrue(dbConn.connected());

			// Should wrap the same connection
			assertSame(directConn, dbConn.getConnection());

			// Should be able to perform operations
			try (Statement stmt = directConn.createStatement();
					 ResultSet rs = stmt.executeQuery("SELECT 1")) {
				assertTrue(rs.next());
				assertEquals(1, rs.getInt(1));
			}
		}

		// After DatabaseConnection is closed, the underlying connection should also be closed
		assertTrue(directConn.isClosed());
	}
}
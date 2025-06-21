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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.logging.MethodLogger;
import is.codion.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static is.codion.common.db.connection.DatabaseConnection.databaseConnection;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test relies on the emp/dept schema
 */
public class DefaultDatabaseConnectionTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));
	private static final String INVALID_USERNAME = "foo";
	private static final String INVALID_PASSWORD = "xxxxx";
	private static final int METHOD_LOGGER_STACK_DEPTH = 20;

	private final Database DATABASE = Database.instance();
	private DefaultDatabaseConnection dbConnection;

	@BeforeEach
	void before() {
		dbConnection = new DefaultDatabaseConnection(DATABASE, UNIT_TEST_USER);
	}

	@AfterEach
	void after() {
		try {
			if (dbConnection != null) {
				dbConnection.close();
			}
		}
		catch (Exception ignored) {/*ignored*/}
	}

	@Nested
	@DisplayName("Connection creation tests")
	class ConnectionCreationTests {

		@Test
		@DisplayName("Create connection with valid credentials")
		void createConnection_validCredentials_shouldSucceed() throws SQLException {
			try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
				DatabaseConnection databaseConnection = databaseConnection(DATABASE, connection);
				assertTrue(databaseConnection.connected());
				assertNotNull(databaseConnection.user());
				assertTrue(UNIT_TEST_USER.username().equalsIgnoreCase(databaseConnection.user().username()));
			}
		}

		@Test
		@DisplayName("Create connection with closed connection throws exception")
		void createConnection_closedConnection_shouldThrowException() {
			assertThrows(DatabaseException.class, () -> {
				try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
					connection.close();
					databaseConnection(DATABASE, connection);
				}
			});
		}

		@Test
		@DisplayName("Constructor with valid connection")
		void constructor_validConnection_shouldCloseOnDatabaseConnectionClose() throws SQLException {
			Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
			new DefaultDatabaseConnection(DATABASE, connection).close();
			assertTrue(connection.isClosed());
		}

		@Test
		@DisplayName("Constructor with invalid connection throws exception")
		void constructor_invalidConnection_shouldThrowException() throws SQLException {
			Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
			connection.close();
			assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, connection));
		}
	}

	@Nested
	@DisplayName("Authentication tests")
	class AuthenticationTests {

		@Test
		@DisplayName("Connection with wrong username throws exception")
		void connection_wrongUsername_shouldThrowException() {
			assertThrows(DatabaseException.class,
							() -> new DefaultDatabaseConnection(DATABASE, User.user(INVALID_USERNAME, "bar".toCharArray())));
		}

		@Test
		@DisplayName("Connection with wrong password throws exception")
		void connection_wrongPassword_shouldThrowException() {
			assertThrows(DatabaseException.class,
							() -> new DefaultDatabaseConnection(DATABASE, User.user(UNIT_TEST_USER.username(), INVALID_PASSWORD.toCharArray())));
		}
	}

	@Nested
	@DisplayName("Basic operations tests")
	class BasicOperationsTests {

		@Test
		@DisplayName("Database getter returns correct database")
		void database_getter_shouldReturnCorrectDatabase() {
			assertEquals(DATABASE, dbConnection.database());
		}

		@Test
		@DisplayName("User getter returns correct user")
		void user_getter_shouldReturnCorrectUser() {
			assertEquals(UNIT_TEST_USER, dbConnection.user());
		}

		@Test
		@DisplayName("Get connection returns non-null connection")
		void getConnection_shouldReturnNonNull() {
			assertNotNull(dbConnection.getConnection());
		}

		@Test
		@DisplayName("Method logger can be set and retrieved")
		void methodLogger_setAndGet_shouldWork() {
			try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
				MethodLogger methodLogger = MethodLogger.methodLogger(METHOD_LOGGER_STACK_DEPTH);
				methodLogger.setEnabled(true);
				connection.setMethodLogger(methodLogger);
				assertSame(methodLogger, connection.getMethodLogger());
			}
		}

		@Test
		@DisplayName("Commit and rollback work outside transaction")
		void commitAndRollback_outsideTransaction_shouldWork() {
			try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
				assertDoesNotThrow(() -> connection.commit());
				assertDoesNotThrow(() -> connection.rollback());
			}
		}

		@Test
		@DisplayName("toString returns non-null value")
		void toString_shouldReturnNonNull() {
			try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
				assertNotNull(connection.toString());
			}
		}
	}

	@Nested
	@DisplayName("Connection state tests")
	class ConnectionStateTests {

		@Test
		@DisplayName("Close connection changes state correctly")
		void close_shouldChangeStateCorrectly() {
			dbConnection.close();
			assertFalse(dbConnection.valid());
			assertFalse(dbConnection.connected());
		}

		@Test
		@DisplayName("Operations on closed connection throw IllegalStateException")
		void closedConnection_operations_shouldThrowIllegalStateException() {
			dbConnection.close();

			assertThrows(IllegalStateException.class, () -> dbConnection.getConnection());
			assertThrows(IllegalStateException.class, () -> dbConnection.startTransaction());
			assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
			assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
			assertThrows(IllegalStateException.class, () -> dbConnection.commit());
			assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
		}
	}

	@Nested
	@DisplayName("Transaction tests")
	class TransactionTests {

		@Test
		@DisplayName("Transaction lifecycle works correctly")
		void transaction_lifecycle_shouldWorkCorrectly() throws SQLException {
			try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
				// Cannot rollback without transaction
				assertThrows(IllegalStateException.class, () -> connection.rollbackTransaction());

				// Start transaction
				connection.startTransaction();
				assertTrue(connection.transactionOpen());

				// Cannot start another transaction
				assertThrows(IllegalStateException.class, () -> connection.startTransaction());

				// Commit transaction
				connection.commitTransaction();
				assertFalse(connection.transactionOpen());

				// Cannot commit again
				assertThrows(IllegalStateException.class, () -> connection.commitTransaction());

				// Start new transaction
				connection.startTransaction();
				assertTrue(connection.transactionOpen());

				// Rollback transaction
				connection.rollbackTransaction();
				assertFalse(connection.transactionOpen());

				// Cannot rollback again
				assertThrows(IllegalStateException.class, () -> connection.rollbackTransaction());
			}
		}

		@Test
		@DisplayName("Cannot start transaction when one is already open")
		void startTransaction_alreadyOpen_shouldThrowException() {
			dbConnection.startTransaction();
			assertThrows(IllegalStateException.class, () -> dbConnection.startTransaction());
		}

		@Test
		@DisplayName("Cannot commit within transaction using commit()")
		void commit_withinTransaction_shouldThrowException() {
			dbConnection.startTransaction();
			assertThrows(IllegalStateException.class, () -> dbConnection.commit());
		}

		@Test
		@DisplayName("Cannot rollback within transaction using rollback()")
		void rollback_withinTransaction_shouldThrowException() {
			dbConnection.startTransaction();
			assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
		}

		@Test
		@DisplayName("Cannot commit transaction after already committed")
		void commitTransaction_alreadyCommitted_shouldThrowException() throws SQLException {
			dbConnection.startTransaction();
			dbConnection.commitTransaction();
			assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
		}

		@Test
		@DisplayName("Cannot rollback transaction after already rolled back")
		void rollbackTransaction_alreadyRolledBack_shouldThrowException() throws SQLException {
			dbConnection.startTransaction();
			dbConnection.rollbackTransaction();
			assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
		}
	}
}
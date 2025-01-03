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

	@Test
	void createConnection() throws SQLException {
		try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
			DatabaseConnection databaseConnection = databaseConnection(DATABASE, connection);
			assertTrue(databaseConnection.connected());
			assertNotNull(databaseConnection.user());
			assertTrue(UNIT_TEST_USER.username().equalsIgnoreCase(databaseConnection.user().username()));
		}
	}

	@Test
	void createConnectionWithClosedConnection() {
		assertThrows(DatabaseException.class, () -> {
			try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
				connection.close();
				databaseConnection(DATABASE, connection);
			}
		});
	}

	@Test
	void test() {
		try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
			MethodLogger methodLogger = MethodLogger.methodLogger(20);
			methodLogger.setEnabled(true);
			connection.setMethodLogger(methodLogger);
			assertSame(methodLogger, connection.getMethodLogger());
			connection.commit();
			connection.rollback();
			connection.toString();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void transaction() {
		try (DatabaseConnection connection = databaseConnection(DATABASE, UNIT_TEST_USER)) {
			assertThrows(IllegalStateException.class, () -> connection.rollbackTransaction());
			connection.startTransaction();
			assertThrows(IllegalStateException.class, () -> connection.startTransaction());
			connection.commitTransaction();
			assertThrows(IllegalStateException.class, () -> connection.commitTransaction());
			connection.startTransaction();
			connection.rollbackTransaction();
			assertThrows(IllegalStateException.class, () -> connection.rollbackTransaction());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void constructorWithConnection() throws SQLException {
		Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
		new DefaultDatabaseConnection(DATABASE, connection).close();
		assertTrue(connection.isClosed());
	}

	@Test
	void constructorWithInvalidConnection() throws SQLException {
		Connection connection = DATABASE.createConnection(UNIT_TEST_USER);
		connection.close();
		assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, connection));
	}

	@Test
	void wrongUsername() {
		assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, User.user("foo", "bar".toCharArray())));
	}

	@Test
	void wrongPassword() {
		assertThrows(DatabaseException.class, () -> new DefaultDatabaseConnection(DATABASE, User.user(UNIT_TEST_USER.username(), "xxxxx".toCharArray())));
	}

	@Test
	void database() {
		assertEquals(DATABASE, dbConnection.database());
	}

	@Test
	void user() {
		assertEquals(UNIT_TEST_USER, dbConnection.user());
	}

	@Test
	void close() {
		dbConnection.close();
		assertFalse(dbConnection.valid());
		assertFalse(dbConnection.connected());
		assertThrows(IllegalStateException.class, () -> dbConnection.getConnection());
		assertThrows(IllegalStateException.class, () -> dbConnection.startTransaction());
		assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
		assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
		assertThrows(IllegalStateException.class, () -> dbConnection.commit());
		assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
	}

	@Test
	void getConnection() {
		assertNotNull(dbConnection.getConnection());
	}

	@Test
	void startTransactionAlreadyOpen() {
		dbConnection.startTransaction();
		assertThrows(IllegalStateException.class, () -> dbConnection.startTransaction());
	}

	@Test
	void commitWithinTransaction() {
		dbConnection.startTransaction();
		assertThrows(IllegalStateException.class, () -> dbConnection.commit());
	}

	@Test
	void rollbackWithinTransaction() {
		dbConnection.startTransaction();
		assertThrows(IllegalStateException.class, () -> dbConnection.rollback());
	}

	@Test
	void commitTransactionAlreadyCommitted() throws SQLException {
		dbConnection.startTransaction();
		dbConnection.commitTransaction();
		assertThrows(IllegalStateException.class, () -> dbConnection.commitTransaction());
	}

	@Test
	void rollbackTransactionAlreadyRollbacked() throws SQLException {
		dbConnection.startTransaction();
		dbConnection.rollbackTransaction();
		assertThrows(IllegalStateException.class, () -> dbConnection.rollbackTransaction());
	}

	@Test
	void commitTransaction() throws SQLException {
		dbConnection.startTransaction();
		assertTrue(dbConnection.transactionOpen());
		dbConnection.commitTransaction();
		assertFalse(dbConnection.transactionOpen());
	}

	@Test
	void rollbackTransaction() throws SQLException {
		dbConnection.startTransaction();
		assertTrue(dbConnection.transactionOpen());
		dbConnection.rollbackTransaction();
		assertFalse(dbConnection.transactionOpen());
	}
}

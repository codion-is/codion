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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractDatabaseTest {

	private static final String TEST_DATABASE_NAME = "name";
	private static final String H2_URL = "jdbc:h2:mem:h2db";
	private static final String TEST_USER_CREDENTIALS = "scott:tiger";
	private static final String SA_USER = "sa";
	private static final String NON_EXISTENT_USER = "john";

	// Test data for limit/offset
	private static final Integer LIMIT_10 = 10;
	private static final Integer OFFSET_5 = 5;

	private AbstractDatabase database;

	@BeforeEach
	void setup() {
		database = new TestDatabase();
	}

	@AfterEach
	void tearDown() {
		// Reset any modified global state
		Database.TRANSACTION_ISOLATION.set(null);
	}

	@Nested
	@DisplayName("Basic database operations")
	class BasicOperationsTests {

		@Test
		@DisplayName("Database name returns correctly")
		void name_shouldReturnCorrectName() {
			assertEquals(TEST_DATABASE_NAME, database.name());
		}

		@Test
		@DisplayName("Select for update clause returns expected value")
		void selectForUpdateClause_shouldReturnExpectedValue() {
			assertEquals(AbstractDatabase.FOR_UPDATE_NOWAIT, database.selectForUpdateClause());
		}

		@Test
		@DisplayName("Error message handles SQLException")
		void errorMessage_shouldHandleSQLException() {
			// Just verify it doesn't throw
			assertDoesNotThrow(() ->
							database.errorMessage(new SQLException("Test error"), Operation.OTHER));
		}
	}

	@Nested
	@DisplayName("Connection provider tests")
	class ConnectionProviderTests {

		@Test
		@DisplayName("Custom connection provider overrides default")
		void connectionProvider_custom_shouldOverrideDefault() throws SQLException {
			User sa = User.user(SA_USER);
			Connection originalConnection = database.createConnection(sa);

			// Set custom provider
			database.connectionProvider(new ConnectionProvider() {
				@Override
				public Connection connection(User user, String url) {
					return originalConnection;
				}
			});

			// Should get same connection from provider
			Connection providedConnection = database.createConnection(sa);
			assertSame(originalConnection, providedConnection);

			// Reset to default provider
			database.connectionProvider(new ConnectionProvider() {});
			Connection newConnection = database.createConnection(sa);
			assertNotSame(originalConnection, newConnection);

			// Cleanup
			originalConnection.close();
			newConnection.close();
		}
	}

	@Nested
	@DisplayName("Limit and offset clause tests")
	class LimitOffsetTests {

		@Test
		@DisplayName("Offset fetch next clause with null values")
		void offsetFetchNextClause_nullValues_shouldReturnEmpty() {
			assertEquals("", AbstractDatabase.createOffsetFetchNextClause(null, null));
		}

		@Test
		@DisplayName("Offset fetch next clause with only offset")
		void offsetFetchNextClause_onlyOffset_shouldReturnOffsetClause() {
			assertEquals("OFFSET 5 ROWS", AbstractDatabase.createOffsetFetchNextClause(null, OFFSET_5));
		}

		@Test
		@DisplayName("Offset fetch next clause with only limit")
		void offsetFetchNextClause_onlyLimit_shouldReturnFetchClause() {
			assertEquals("FETCH NEXT 10 ROWS ONLY", AbstractDatabase.createOffsetFetchNextClause(LIMIT_10, null));
		}

		@Test
		@DisplayName("Offset fetch next clause with both values")
		void offsetFetchNextClause_bothValues_shouldReturnFullClause() {
			assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY",
							AbstractDatabase.createOffsetFetchNextClause(LIMIT_10, OFFSET_5));
		}

		@Test
		@DisplayName("Limit offset clause variations")
		void limitOffsetClause_variations_shouldReturnCorrectClauses() {
			assertEquals("", database.limitOffsetClause(null, null));
			assertEquals("OFFSET 5", database.limitOffsetClause(null, OFFSET_5));
			assertEquals("LIMIT 10", database.limitOffsetClause(LIMIT_10, null));
			assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(LIMIT_10, OFFSET_5));
		}
	}

	@Nested
	@DisplayName("Transaction isolation tests")
	class TransactionIsolationTests {

		@Test
		@DisplayName("Default transaction isolation is READ_COMMITTED")
		void transactionIsolation_default_shouldBeReadCommitted() throws SQLException {
			Database db = new TestDatabase();
			User sa = User.user(SA_USER);

			try (Connection connection = db.createConnection(sa)) {
				assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection.getTransactionIsolation());
			}
		}

		@Test
		@DisplayName("Transaction isolation can be changed globally")
		void transactionIsolation_change_shouldApplyToNewConnections() throws SQLException {
			// Set to SERIALIZABLE
			Database.TRANSACTION_ISOLATION.set(Connection.TRANSACTION_SERIALIZABLE);

			Database db = new TestDatabase();
			User sa = User.user(SA_USER);

			try (Connection connection = db.createConnection(sa)) {
				assertEquals(Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation());
			}
		}
	}

	@Nested
	@DisplayName("Connection pool tests")
	class ConnectionPoolTests {

		@Test
		@DisplayName("Connection pool lifecycle works correctly")
		void connectionPool_lifecycle_shouldWorkCorrectly() {
			Database db = new TestDatabase();
			User testUser = User.parse(TEST_USER_CREDENTIALS);

			// Create pool
			db.createConnectionPool(ConnectionPoolFactory.instance(), testUser);

			// Verify pool exists (case-insensitive)
			assertTrue(db.containsConnectionPool("ScotT"));
			assertTrue(db.containsConnectionPool("SCOTT"));
			assertTrue(db.containsConnectionPool("scott"));

			// Cannot get pool for non-existent user
			assertThrows(IllegalArgumentException.class, () -> db.connectionPool(NON_EXISTENT_USER));

			// Can get pool for existing user
			assertNotNull(db.connectionPool("scott"));

			// Close pool
			db.closeConnectionPool("scott");

			// Pool no longer exists
			assertFalse(db.containsConnectionPool("ScotT"));
		}
	}

	/**
	 * Test implementation of AbstractDatabase
	 */
	private static final class TestDatabase extends AbstractDatabase {

		private TestDatabase() {
			super(H2_URL);
		}

		@Override
		public String name() {
			return TEST_DATABASE_NAME;
		}

		@Override
		public String autoIncrementQuery(String idSource) {
			return "";
		}

		@Override
		public String selectForUpdateClause() {
			return FOR_UPDATE_NOWAIT;
		}

		@Override
		public String limitOffsetClause(Integer limit, Integer offset) {
			return createLimitOffsetClause(limit, offset);
		}
	}
}
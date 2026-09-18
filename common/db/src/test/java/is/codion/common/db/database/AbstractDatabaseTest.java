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
package is.codion.common.db.database;

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Locale;
import java.util.ResourceBundle;

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
		@DisplayName("Exception handles SQLException")
		void exception_shouldHandleSQLException() {
			// Just verify it doesn't throw
			assertDoesNotThrow(() -> database.exception(new SQLException("Test error"), Operation.OTHER));
		}
	}

	@Nested
	@DisplayName("Error handling tests")
	class ErrorHandlingTests {

		// independent of the default locale
		private static String message(String key) {
			return ResourceBundle.getBundle(Database.class.getName()).getString(key);
		}

		@Test
		@DisplayName("Errors are recognized by SQL state by default")
		void exception_sqlState_shouldReturnTypedException() {
			assertInstanceOf(UniqueConstraintException.class, database.exception(new SQLException("unique", "23505"), Operation.INSERT));
			assertInstanceOf(ReferentialIntegrityException.class, database.exception(new SQLException("fk", "23503"), Operation.DELETE));
			assertInstanceOf(ReferentialIntegrityException.class, database.exception(new SQLException("fk", "23504"), Operation.DELETE));
			assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), Operation.SELECT));
			assertInstanceOf(AuthenticationException.class, database.exception(new SQLException("login", "28000"), Operation.OTHER));
			assertSame(DatabaseException.class, database.exception(new SQLException("login"), Operation.OTHER).getClass());
			DatabaseException exception = database.exception(new SQLException("unknown", "XX000"), Operation.OTHER);
			assertSame(DatabaseException.class, exception.getClass());
			assertEquals("unknown", exception.getMessage());
		}

		@Test
		@DisplayName("Recognized errors get a message, unrecognized ones the exception message")
		void errorMessage_shouldBeBasedOnErrorType() {
			assertEquals(message("unique_constraint"),
							database.exception(new SQLException("unique", "23505"), Operation.INSERT).getMessage());
			assertEquals(message("null_value"), database.exception(new SQLException("null", "23502"), Operation.INSERT).getMessage());
			assertEquals("unknown", database.exception(new SQLException("unknown", "XX000"), Operation.OTHER).getMessage());
			assertNull(database.exception(new SQLException(), Operation.OTHER).getMessage());
		}

		@Test
		@DisplayName("The operation decides the message when the database does not report which way a foreign key was violated")
		void errorMessage_referentialIntegrity_shouldDependOnOperation() {
			SQLException exception = new SQLException("fk", "23503");
			assertEquals(message("parent_missing"), database.exception(exception, Operation.INSERT).getMessage());
			assertEquals(message("child_exists"), database.exception(exception, Operation.DELETE).getMessage());
			assertEquals(message("referential_integrity"), database.exception(exception, Operation.UPDATE).getMessage());
		}

		@Test
		@DisplayName("The detail is appended to the message")
		void errorMessage_detail_shouldBeAppended() {
			Database detailed = new TestDatabase() {
				@Override
				protected String errorDetail(SQLException exception, ErrorType errorType) {
					return errorType == ErrorType.NULL_VALUE ? "NAME" : null;
				}
			};
			assertEquals(message("null_value") + ": NAME", detailed.exception(new SQLException("null", "23502"), Operation.INSERT).getMessage());
			assertEquals(message("unique_constraint"), detailed.exception(new SQLException("unique", "23505"), Operation.INSERT).getMessage());
		}

		@Test
		@DisplayName("An exception thrown while handling an exception never replaces it")
		void errorHandling_throwingImplementation_shouldBeIgnored() {
			Database throwing = new TestDatabase() {
				@Override
				protected ErrorType errorType(SQLException exception) {
					if ("23505".equals(exception.getSQLState())) {
						return ErrorType.UNIQUE_CONSTRAINT;
					}
					throw new StringIndexOutOfBoundsException();
				}

				@Override
				protected String errorDetail(SQLException exception, ErrorType errorType) {
					throw new StringIndexOutOfBoundsException();
				}

				@Override
				protected String message(SQLException exception) {
					throw new StringIndexOutOfBoundsException();
				}
			};
			assertEquals(message("unique_constraint"), throwing.exception(new SQLException("unique", "23505"), Operation.INSERT).getMessage());
			assertEquals("unknown", throwing.exception(new SQLException("unknown", "XX000"), Operation.OTHER).getMessage());
			assertSame(DatabaseException.class, throwing.exception(new SQLException("unknown", "XX000"), Operation.OTHER).getClass());
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
	@DisplayName("Url prefix tests")
	class UrlPrefixTests {

		@Test
		@DisplayName("Database or host from a url without prefix, options and parameters")
		void databaseOrHost_shouldReturnDatabaseOrHost() {
			assertEquals("db", AbstractDatabase.databaseOrHost("//host:1234/db"));
			assertEquals("db", AbstractDatabase.databaseOrHost("//host1:1234,host2:1234/db"));
			assertEquals("db", AbstractDatabase.databaseOrHost("db"));
			assertEquals("host:1234", AbstractDatabase.databaseOrHost("//host:1234/"));
			assertEquals("host:1234", AbstractDatabase.databaseOrHost("//host:1234"));
			assertEquals("", AbstractDatabase.databaseOrHost("/"));
			assertEquals("", AbstractDatabase.databaseOrHost(""));
		}

		@Test
		@DisplayName("An upper case url matches its prefix on a Turkish machine")
		void removeUrlPrefix_upperCaseUrlTurkishLocale_shouldMatch() {
			// Both sides are lower-cased, which looks symmetric and is not: the prefix is a lower case literal
			// already, so only the url is transformed. Under Turkish "THIN" becomes "thın" and no longer starts with
			// "thin", leaving the dbms unrecognised for anyone who writes their url in upper case.
			Locale locale = Locale.getDefault();
			try {
				Locale.setDefault(Locale.forLanguageTag("tr"));
				assertEquals("host:1521/db", AbstractDatabase.removeUrlPrefixOptionsAndParameters(
								"JDBC:ORACLE:THIN:@host:1521/db", "jdbc:oracle:thin:@"));
			}
			finally {
				Locale.setDefault(locale);
			}
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
			assertEquals("", database.limitOffsetClause(null, null, false));
			assertEquals("OFFSET 5", database.limitOffsetClause(null, OFFSET_5, false));
			assertEquals("LIMIT 10", database.limitOffsetClause(LIMIT_10, null, false));
			assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(LIMIT_10, OFFSET_5, false));
		}

		@Test
		@DisplayName("Limit offset clause for databases requiring a limit with an offset")
		void limitOffsetClause_noLimit_shouldSubstituteTheLimit() {
			assertEquals("", AbstractDatabase.createLimitOffsetClause(null, null, "-1"));
			assertEquals("LIMIT -1 OFFSET 5", AbstractDatabase.createLimitOffsetClause(null, OFFSET_5, "-1"));
			assertEquals("LIMIT 10", AbstractDatabase.createLimitOffsetClause(LIMIT_10, null, "-1"));
			assertEquals("LIMIT 10 OFFSET 5", AbstractDatabase.createLimitOffsetClause(LIMIT_10, OFFSET_5, "-1"));
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

		@Test
		@DisplayName("closeConnectionPools closes multiple pools without error")
		void closeConnectionPools_multiplePools_shouldNotThrow() throws SQLException {
			Database db = new TestDatabase();
			// Open an admin (sa) connection first, so the fresh in-memory DB is owned by sa, then register
			// a second user. The open connection keeps the DB alive while both pools are created.
			try (Connection admin = db.createConnection(User.user(SA_USER))) {
				admin.createStatement().execute("CREATE USER IF NOT EXISTS scott PASSWORD 'tiger'");

				db.createConnectionPool(ConnectionPoolFactory.instance(), User.user(SA_USER));
				db.createConnectionPool(ConnectionPoolFactory.instance(), User.parse(TEST_USER_CREDENTIALS));

				assertEquals(2, db.connectionPoolUsernames().size());

				// Two or more pools previously triggered a ConcurrentModificationException here
				assertDoesNotThrow(db::closeConnectionPools);

				assertTrue(db.connectionPoolUsernames().isEmpty());
			}
		}
	}

	/**
	 * Test implementation of AbstractDatabase
	 */
	private static class TestDatabase extends AbstractDatabase {

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
		public String limitOffsetClause(Integer limit, Integer offset, boolean ordered) {
			return createLimitOffsetClause(limit, offset);
		}
	}
}
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

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractDatabaseTest {

	private final AbstractDatabase database = new TestDatabase();

	@Test
	void test() {
		assertEquals(AbstractDatabase.FOR_UPDATE_NOWAIT, database.selectForUpdateClause());
		assertEquals("name", database.name());
		database.errorMessage(new SQLException(), Operation.OTHER);
	}

	@Test
	void connectionProvider() throws SQLException {
		User sa = User.user("sa");
		Connection connection = database.createConnection(sa);
		database.connectionProvider(new ConnectionProvider() {
			@Override
			public Connection connection(User user, String url) {
				return connection;
			}
		});
		assertSame(connection, database.createConnection(sa));
		database.connectionProvider(new ConnectionProvider() {});
		Connection newConnection = database.createConnection(sa);
		assertNotSame(connection, newConnection);
		connection.close();
		newConnection.close();
	}

	@Test
	void limitOffset() {
		assertEquals("", AbstractDatabase.createOffsetFetchNextClause(null, null));
		assertEquals("OFFSET 5 ROWS", AbstractDatabase.createOffsetFetchNextClause(null, 5));
		assertEquals("FETCH NEXT 10 ROWS ONLY", AbstractDatabase.createOffsetFetchNextClause(10, null));
		assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", AbstractDatabase.createOffsetFetchNextClause(10, 5));
		assertEquals("", database.limitOffsetClause(null, null));
		assertEquals("OFFSET 5", database.limitOffsetClause(null, 5));
		assertEquals("LIMIT 10", database.limitOffsetClause(10, null));
		assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(10, 5));
	}

	@Test
	void transactionIsolation() throws SQLException {
		Database db = new TestDatabase();
		User sa = User.user("sa");
		Connection connection = db.createConnection(sa);
		assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection.getTransactionIsolation());
		connection.close();
		Database.TRANSACTION_ISOLATION.set(Connection.TRANSACTION_SERIALIZABLE);
		db = new TestDatabase();
		connection = db.createConnection(sa);
		assertEquals(Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation());
		connection.close();
		Database.TRANSACTION_ISOLATION.set(null);
	}

	@Test
	void connectionPool() {
		Database db = new TestDatabase();
		db.createConnectionPool(ConnectionPoolFactory.instance(), User.parse("scott:tiger"));
		assertTrue(db.containsConnectionPool("ScotT"));
		assertThrows(IllegalArgumentException.class, () -> db.connectionPool("john"));
		assertNotNull(db.connectionPool("scott"));
		db.closeConnectionPool("scott");
		assertFalse(db.containsConnectionPool("ScotT"));
	}

	private static final class TestDatabase extends AbstractDatabase {

		private TestDatabase() {
			super("jdbc:h2:mem:h2db");
		}

		@Override
		public String name() {
			return "name";
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

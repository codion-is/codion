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
package is.codion.dbms.h2;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

import static is.codion.common.db.database.Database.Operation.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class H2DatabaseTest {

	private static final H2Database DATABASE = new H2Database("jdbc:h2:mem:test");

	@Test
	void databaseName() {
		assertEquals("C:/data/sample", H2Database.databaseName("jdbc:h2:file:C:/data/sample;trace_level_file=3;trace_level_system_out=3"));
		assertEquals("C:/data/sample", H2Database.databaseName("jdbc:h2:C:/data/sample;trace_level_file=3;trace_level_system_out=3"));
		assertEquals("sampleDb", H2Database.databaseName("jdbc:h2:mem:sampleDb;trace_level_file=3;trace_level_system_out=3"));
		assertEquals("private", H2Database.databaseName("jdbc:h2:mem:"));
		assertEquals("sample.db:1234/db", H2Database.databaseName("jdbc:h2:tcp://sample.db:1234/db"));
		assertEquals("sample.db:1234/db", H2Database.databaseName("jdbc:h2:tcp://sample.db:1234/db;trace_level_file=3;trace_level_system_out=3"));
		assertEquals("db.zip!/h2db", H2Database.databaseName("jdbc:h2:zip:db.zip!/h2db"));
		assertEquals("db.zip!/h2db", H2Database.databaseName("jdbc:h2:zip:db.zip!/h2db;trace_level_file=3;trace_level_system_out=3"));
	}

	@Test
	void sequenceSQLNullSequence() {
		assertThrows(NullPointerException.class, () -> new H2Database("jdbc:h2:mem:test").sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		// IDENTITY() was removed in H2 2.0
		assertThrows(UnsupportedOperationException.class, () -> DATABASE.autoIncrementQuery("table"));
	}

	@Test
	void sequenceQuery() {
		final String idSource = "seq";
		assertEquals("select next value for " + idSource, DATABASE.sequenceQuery(idSource));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new H2Database(null));
	}

	@Test
	void multipleDatabases(@TempDir Path tempDir) throws SQLException, IOException {
		Path file1 = tempDir.resolve("h2db_test_1.sql");
		Path file2 = tempDir.resolve("h2db_test_2.sql");
		Files.write(file1, singletonList("create schema employees; create table employees.test1 (id int);"));
		Files.write(file2, singletonList("create schema employees; create table employees.test2 (id int);"));

		final String url1 = "jdbc:h2:mem:test1";
		final String url2 = "jdbc:h2:mem:test2";

		User user = User.user("sa");
		H2Database db1 = new H2Database(url1, singletonList(file1.toFile().getAbsolutePath()));
		H2Database db2 = new H2Database(url2, singletonList(file2.toFile().getAbsolutePath()));
		Connection connection1 = db1.createConnection(user);
		Connection connection2 = db2.createConnection(user);
		connection1.prepareCall("select id from employees.test1").executeQuery();
		connection2.prepareCall("select id from employees.test2").executeQuery();
		connection1.close();
		connection2.close();
	}

	@Test
	void fileDatabase(@TempDir Path tempDir) throws SQLException {
		String url = "jdbc:h2:file:" + tempDir.toFile().getAbsolutePath() + "/h2db/database";
		File dbFile = new File(tempDir.toFile().getAbsolutePath() + "/h2db/database.mv.db");
		dbFile.deleteOnExit();
		assertFalse(dbFile.exists());

		H2Database database = new H2Database(url, singletonList("src/test/resources/create_schema.sql"));
		assertTrue(dbFile.exists());

		User user = User.parse("scott:tiger");

		Connection connection = database.createConnection(user);
		connection.prepareStatement("select id from test.test_table").execute();
		connection.close();

		H2Database database2 = new H2Database(url, singletonList("src/test/resources/create_schema.sql"));
		connection = database2.createConnection(user);
		connection.prepareStatement("select id from test.test_table").execute();
		connection.close();

		//test old url type
		H2Database database3 = new H2Database("jdbc:h2:" + tempDir.toFile()
						.getAbsolutePath() + "/h2db/database", singletonList("src/test/resources/create_schema.sql"));
		connection = database3.createConnection(user);
		connection.prepareStatement("select id from test.test_table").execute();
		connection.close();

		File parentDir = dbFile.getParentFile();
		dbFile.delete();
		parentDir.delete();
	}

	@Test
	void closeMixedCaseUrl() throws SQLException {
		// the database must be initialized again after having been closed, whatever the case of the url
		for (String url : new String[] {"jdbc:h2:mem:closelowercase", "jdbc:h2:mem:CloseMixedCase"}) {
			for (int i = 0; i < 2; i++) {
				H2Database database = new H2Database(url, singletonList("src/test/resources/create_schema.sql"));
				try (Connection connection = database.createConnection(User.parse("scott:tiger"))) {
					connection.prepareStatement("select id from test.test_table").execute();
				}
				finally {
					database.close();
				}
			}
		}
	}

	@Test
	void relativeScriptPath() throws SQLException {
		H2Database database = new H2Database("jdbc:h2:mem:relativescriptpath", singletonList("../h2/src/test/resources/create_schema.sql"));
		try (Connection connection = database.createConnection(User.parse("scott:tiger"))) {
			connection.prepareStatement("select id from test.test_table").execute();
		}
		finally {
			database.close();
		}
		assertThrows(SecurityException.class, () -> new H2Database("jdbc:h2:mem:scriptinjection", singletonList("script.sql';drop all objects")));
	}

	@Test
	void server() throws Exception {
		// the driver is a runtime dependency only
		Class<?> serverClass = Class.forName("org.h2.tools.Server");
		Object server = serverClass.getMethod("createTcpServer", String[].class)
						.invoke(null, (Object) new String[] {"-tcpPort", "0", "-ifNotExists"});
		serverClass.getMethod("start").invoke(server);
		try {
			String url = "jdbc:h2:tcp://localhost:" + serverClass.getMethod("getPort").invoke(server) + "/mem:served";
			// a password protected sysadmin, which the embedded initialization would fail to log in as
			try (Connection connection = DriverManager.getConnection(url + ";DB_CLOSE_DELAY=-1", "sa", "secret")) {
				connection.prepareStatement("create user scott password 'tiger'").execute();
			}
			H2Database database = new H2Database(url, singletonList("src/test/resources/create_schema.sql"));
			try (Connection connection = database.createConnection(User.parse("scott:tiger"))) {
				// not initialized
				assertThrows(SQLException.class, () -> connection.prepareStatement("select id from test.test_table").execute());
			}
			database.close();
			// not shut down, in which case the in-memory database would be gone along with its users
			DriverManager.getConnection(url + ";IFEXISTS=TRUE", "scott", "tiger").close();
		}
		finally {
			serverClass.getMethod("stop").invoke(server);
		}
	}

	@Test
	void exceptions() throws SQLException {
		// the driver being available, the actual exceptions are used
		H2Database database = new H2Database("jdbc:h2:mem:exceptions");
		try (Connection connection = database.createConnection(User.user("sa"));
				 Connection connection2 = database.createConnection(User.user("sa"))) {
			execute(connection, "create table parent (id int primary key, name varchar(10) not null, code varchar(5), amount decimal(5,2), "
							+ "constraint parent_uk unique (code), constraint parent_ck check (amount >= 0))");
			execute(connection, "create table child (id int primary key, parent_id int not null, "
							+ "constraint child_fk foreign key (parent_id) references parent(id))");
			execute(connection, "insert into parent (id, name, code, amount) values (1, 'a', 'A', 1)");
			execute(connection, "insert into child (id, parent_id) values (1, 1)");

			SQLException unique = failure(connection, "insert into parent (id, name, code) values (2, 'b', 'A')");
			assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
			assertEquals(message("unique_constraint"), database.errorMessage(unique, INSERT));

			SQLException parentMissing = failure(connection, "insert into child (id, parent_id) values (2, 99)");
			assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
			assertEquals(message("parent_missing"), database.errorMessage(parentMissing, INSERT));
			SQLException childExists = failure(connection, "delete from parent where id = 1");
			assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
			assertEquals(message("child_exists"), database.errorMessage(childExists, DELETE));
			SQLException referencedKey = failure(connection, "update parent set id = 5 where id = 1");
			assertEquals(message("child_exists"), database.errorMessage(referencedKey, UPDATE));

			assertEquals(message("null_value") + ": NAME", database.errorMessage(
							failure(connection, "insert into parent (id, name) values (3, null)"), INSERT));
			// quoted identifiers in the statement, which is appended to the message
			assertEquals(message("null_value") + ": NAME", database.errorMessage(
							failure(connection, "insert into \"PARENT\" (\"ID\", \"NAME\") values (3, null)"), INSERT));
			assertEquals(message("check_constraint"), database.errorMessage(
							failure(connection, "update parent set amount = -1 where id = 1"), UPDATE));
			assertEquals(message("value_too_large") + ": NAME", database.errorMessage(
							failure(connection, "update parent set name = 'abcdefghijklmnop' where id = 1"), UPDATE));
			assertEquals(message("value_too_large") + ": AMOUNT", database.errorMessage(
							failure(connection, "update parent set amount = 123456.78 where id = 1"), UPDATE));
			assertEquals(message("table_not_found"), database.errorMessage(failure(connection, "select * from missing"), SELECT));

			// unrecognized, without the statement
			SQLException syntax = failure(connection, "selec * from parent");
			assertSame(DatabaseException.class, database.exception(syntax, SELECT).getClass());
			assertFalse(database.errorMessage(syntax, SELECT).contains("; SQL statement:"));

			// a locked row, which the driver reports as a timeout when NOWAIT is used
			connection.setAutoCommit(false);
			connection2.setAutoCommit(false);
			String selectForUpdate = "select id from parent where id = 1 " + database.selectForUpdateClause();
			execute(connection, selectForUpdate);
			SQLException locked = failure(connection2, selectForUpdate);
			assertSame(DatabaseException.class, database.exception(locked, SELECT).getClass());
			assertEquals(message("row_locked"), database.errorMessage(locked, SELECT));
			connection.rollback();
			connection2.rollback();

			execute(connection, "create user scott password 'tiger'");
			connection.commit();
			AuthenticationException authentication = assertThrows(AuthenticationException.class,
							() -> database.createConnection(User.parse("scott:wrong")));
			assertEquals(message("authentication"), authentication.getMessage());
		}
		finally {
			database.close();
		}
		// a translated or missing message must not throw, replacing the actual exception
		assertEquals(message("null_value"), DATABASE.errorMessage(new SQLException("NULL nicht zul\u00E4ssig f\u00FCr Feld NAME", "23502", 23502), INSERT));
		assertEquals(message("null_value"), DATABASE.errorMessage(new SQLException(null, "23502", 23502), INSERT));
		assertInstanceOf(QueryTimeoutException.class, DATABASE.exception(new SQLException("Statement was canceled", "57014", 57014), SELECT));
	}

	private static void execute(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private static SQLException failure(Connection connection, String sql) {
		return assertThrows(SQLException.class, () -> execute(connection, sql));
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}

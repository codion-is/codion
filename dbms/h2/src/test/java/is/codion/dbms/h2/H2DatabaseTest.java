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

import is.codion.common.db.database.Database.Lock;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.common.db.exception.Operation.*;
import static java.util.Arrays.asList;
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
		// the file systems it is opened through, stackable, not part of its name
		assertEquals("/data/sample", H2Database.databaseName("jdbc:h2:async:/data/sample;trace_level_file=3"));
		assertEquals("/data/sample", H2Database.databaseName("jdbc:h2:retry:/data/sample"));
		assertEquals("/data/sample", H2Database.databaseName("jdbc:h2:retry:nio:/data/sample"));
		assertEquals("/data/sample", H2Database.databaseName("jdbc:h2:split:10:nioMapped:/data/sample"));
		assertEquals("/data/sample", H2Database.databaseName("jdbc:h2:nioMemLZF:1:/data/sample"));
		assertEquals("C:/data/sample", H2Database.databaseName("jdbc:h2:async:C:/data/sample"));
		assertEquals("~/sample", H2Database.databaseName("jdbc:h2:async:~/sample"));
		// neither a colon in a path nor a host taken for one
		assertEquals("/data/a:b/sample", H2Database.databaseName("jdbc:h2:/data/a:b/sample"));
		assertEquals("localhost:9092/db", H2Database.databaseName("jdbc:h2:tcp://localhost:9092/db"));
	}

	@Test
	void databasePath() {
		assertEquals("/data/sample", H2Database.databasePath("jdbc:h2:/data/sample;trace_level_file=3"));
		assertEquals("/data/sample", H2Database.databasePath("jdbc:h2:file:/data/sample;trace_level_file=3"));
		assertEquals("/data/sample", H2Database.databasePath("jdbc:h2:async:/data/sample;trace_level_file=3"));
		assertEquals("/data/sample", H2Database.databasePath("jdbc:h2:retry:/data/sample"));
		assertEquals("/data/sample", H2Database.databasePath("jdbc:h2:retry:nio:/data/sample"));
		assertEquals("C:/data/sample", H2Database.databasePath("jdbc:h2:C:/data/sample"));
		assertEquals("C:/data/sample", H2Database.databasePath("jdbc:h2:retry:nio:C:/data/sample"));
		assertEquals("./sample", H2Database.databasePath("jdbc:h2:./sample"));
		assertEquals("./sample", H2Database.databasePath("jdbc:h2:async:./sample"));
		assertEquals("/data/a:b/sample", H2Database.databasePath("jdbc:h2:/data/a:b/sample"));
		// the user's home, as H2 expands it
		String home = System.getProperty("user.home");
		assertEquals(home + "/sample", H2Database.databasePath("jdbc:h2:~/sample"));
		assertEquals(home + "/sample", H2Database.databasePath("jdbc:h2:file:~/sample"));
		assertEquals(home + "/sample", H2Database.databasePath("jdbc:h2:async:~/sample"));
		assertEquals(home + "\\sample", H2Database.databasePath("jdbc:h2:~\\sample"));
		assertEquals("~sample", H2Database.databasePath("jdbc:h2:~sample"));
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
	void initScripts(@TempDir Path tempDir) throws SQLException, IOException {
		Path first = tempDir.resolve("first.sql");
		Path second = tempDir.resolve("second.sql");
		Path third = tempDir.resolve("third.sql");
		Path failing = tempDir.resolve("failing.sql");
		Files.write(first, singletonList("create schema scripts; create table scripts.t1 (id int);"));
		Files.write(second, singletonList("create table scripts.t2 (id int);"));
		Files.write(third, singletonList("create table scripts.t3 (id int);"));
		Files.write(failing, singletonList("create table scripts.t4 (id int); not sql;"));
		String url = "jdbc:h2:file:" + tempDir.resolve("database").toAbsolutePath();

		// a new database runs the scripts listed
		H2Database database = database(url, first);
		assertEquals(1, scriptsRun(database));
		database.close();

		// the script added runs alone, the first running again failing to create its schema
		database = database(url, first, second);
		assertEquals(2, scriptsRun(database));
		database.close();

		// with the line endings of a checkout on Windows, unchanged
		Files.write(second, "create table scripts.t2 (id int);\r\n".getBytes(StandardCharsets.UTF_8));
		database = database(url, first, second);
		assertEquals(2, scriptsRun(database));
		database.close();

		// a script changed since it ran
		Files.write(second, singletonList("create table scripts.t2 (id bigint);"));
		DatabaseException changed = assertThrows(DatabaseException.class, () -> database(url, first, second));
		assertTrue(changed.getMessage().contains(path(second)));
		Files.write(second, singletonList("create table scripts.t2 (id int);"));

		// a script run, no longer listed
		DatabaseException unlisted = assertThrows(DatabaseException.class, () -> database(url, first));
		assertTrue(unlisted.getMessage().contains(path(second)));

		// a script not yet run, listed before one that has
		DatabaseException order = assertThrows(DatabaseException.class, () -> database(url, first, third, second));
		assertTrue(order.getMessage().contains(path(third)));

		// a script failing is not recorded, running again the next time
		DatabaseException failed = assertThrows(DatabaseException.class, () -> database(url, first, second, failing));
		assertTrue(failed.getMessage().contains(path(failing)));
		Files.write(failing, singletonList("create table if not exists scripts.t4 (id int);"));
		database = database(url, first, second, failing);
		assertEquals(3, scriptsRun(database));
		try (Connection connection = database.createConnection(User.user("sa"))) {
			connection.prepareStatement("select id from scripts.t4").execute();
		}
		database.close();
	}

	@Test
	void initScriptsMoved(@TempDir Path tempDir) throws SQLException, IOException {
		Path first = tempDir.resolve("first.sql");
		Path second = tempDir.resolve("second.sql");
		Files.write(first, singletonList("create schema scripts; create table scripts.t1 (id int);"));
		Files.write(second, singletonList("create table scripts.t2 (id int);"));
		String url = "jdbc:h2:file:" + tempDir.resolve("database").toAbsolutePath();
		H2Database database = database(url, first, second);
		Map<String, Timestamp> recorded = scriptsRecorded(database);
		database.close();

		// moved and renamed, the second with the line endings of a checkout on Windows, neither running again, running the
		// first again failing to create its schema, each record following its script
		Path movedFirst = Files.createDirectory(tempDir.resolve("moved")).resolve("first.sql");
		Path movedSecond = tempDir.resolve("moved").resolve("renamed.sql");
		Files.move(first, movedFirst);
		Files.write(movedSecond, "create table scripts.t2 (id int);\r\n".getBytes(StandardCharsets.UTF_8));
		Files.delete(second);
		database = database(url, movedFirst, movedSecond);
		Map<String, Timestamp> followed = scriptsRecorded(database);
		assertEquals(new HashSet<>(asList(path(movedFirst), path(movedSecond))), followed.keySet());
		assertEquals(recorded.get(path(first)), followed.get(path(movedFirst)));
		assertEquals(recorded.get(path(second)), followed.get(path(movedSecond)));
		database.close();

		// as recorded from now on
		database = database(url, movedFirst, movedSecond);
		assertEquals(followed, scriptsRecorded(database));
		database.close();

		// moved back, with a script added after them, which runs
		Path third = tempDir.resolve("third.sql");
		Files.write(third, singletonList("create table scripts.t3 (id int);"));
		Files.move(movedFirst, first);
		Files.move(movedSecond, second);
		database = database(url, first, second, third);
		assertEquals(new HashSet<>(asList(path(first), path(second), path(third))), scriptsRecorded(database).keySet());
		try (Connection connection = database.createConnection(User.user("sa"))) {
			connection.prepareStatement("select id from scripts.t3").execute();
		}
		database.close();
	}

	@Test
	void initScriptsMovedRefused(@TempDir Path tempDir) throws SQLException, IOException {
		Path first = tempDir.resolve("first.sql");
		Path second = tempDir.resolve("second.sql");
		Files.write(first, singletonList("create schema scripts; create table scripts.t1 (id int);"));
		Files.write(second, singletonList("create table scripts.t2 (id int);"));
		String url = "jdbc:h2:file:" + tempDir.resolve("database").toAbsolutePath();
		H2Database database = database(url, first, second);
		Set<String> recorded = scriptsRecorded(database).keySet();
		database.close();
		Path moved = tempDir.resolve("moved.sql");
		Path changed = tempDir.resolve("changed.sql");
		Path third = tempDir.resolve("third.sql");
		Files.write(moved, singletonList("create table scripts.t2 (id int);"));
		Files.write(changed, singletonList("create table scripts.t2 (id bigint);"));
		Files.write(third, singletonList("create table scripts.t3 (id int);"));

		// moved and changed, which is not a move, and which does not run
		DatabaseException moveChanged = assertThrows(DatabaseException.class, () -> database(url, first, changed));
		assertTrue(moveChanged.getMessage().contains(path(second)));
		assertEquals(recorded, scriptPaths(url));

		// one moved, the other gone
		DatabaseException gone = assertThrows(DatabaseException.class, () -> database(url, moved));
		assertTrue(gone.getMessage().contains(path(first)));
		assertEquals(recorded, scriptPaths(url));

		// moved, listed after a script not yet run
		DatabaseException order = assertThrows(DatabaseException.class, () -> database(url, first, third, moved));
		assertTrue(order.getMessage().contains(path(third)));
		assertEquals(recorded, scriptPaths(url));

		// listed, but not there, with another no longer listed
		assertThrows(DatabaseException.class, () -> database(url, first, tempDir.resolve("missing.sql")));
		assertEquals(recorded, scriptPaths(url));

		// no script run by any of them
		assertThrows(SQLException.class, () -> {
			try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
				connection.prepareStatement("select id from scripts.t3").execute();
			}
		});
		database = database(url, first, second);
		assertEquals(recorded, scriptsRecorded(database).keySet());
		database.close();
	}

	@Test
	void initScriptsMovedAlike(@TempDir Path tempDir) throws SQLException, IOException {
		Path schema = tempDir.resolve("schema.sql");
		Files.write(schema, singletonList("create schema scripts; create table scripts.log (id int);"));
		Path one = tempDir.resolve("one.sql");
		Path other = tempDir.resolve("other.sql");
		String insert = "insert into scripts.log values (1);";
		Files.write(one, singletonList(insert));
		Files.write(other, singletonList(insert));
		String url = "jdbc:h2:file:" + tempDir.resolve("database").toAbsolutePath();
		H2Database database = database(url, schema, one, other);
		assertEquals(2, rows(database));
		database.close();

		// two scripts alike both moved, neither running again
		Path movedOne = tempDir.resolve("movedOne.sql");
		Path movedOther = tempDir.resolve("movedOther.sql");
		Files.write(movedOne, singletonList(insert));
		Files.write(movedOther, singletonList(insert));
		database = database(url, schema, movedOne, movedOther);
		assertEquals(2, rows(database));
		database.close();

		// one of them dropped, not moved, so refused, naming the one left over
		DatabaseException dropped = assertThrows(DatabaseException.class, () -> database(url, schema, movedOne));
		assertTrue(dropped.getMessage().contains(path(movedOther)));

		// a copy, the original still listed, is not a move, so runs, as any script added does
		Path copy = tempDir.resolve("copy.sql");
		Files.write(copy, singletonList(insert));
		database = database(url, schema, movedOne, movedOther, copy);
		assertEquals(3, rows(database));
		database.close();

		// one moved, listed by two paths, one following its record, the other a script added
		Path movedTwice = tempDir.resolve("movedTwice.sql");
		Files.write(movedTwice, singletonList(insert));
		Path added = tempDir.resolve("added.sql");
		Files.write(added, singletonList(insert));
		database = database(url, schema, movedOne, movedOther, movedTwice, added);
		assertEquals(4, rows(database));
		assertEquals(5, scriptsRun(database));
		database.close();
	}

	@Test
	void initScriptsBaseline(@TempDir Path tempDir) throws SQLException, IOException {
		Path first = tempDir.resolve("first.sql");
		Path second = tempDir.resolve("second.sql");
		Files.write(first, singletonList("create schema scripts; create table scripts.t1 (id int);"));
		Files.write(second, singletonList("create table scripts.t2 (id int);"));
		String url = "jdbc:h2:file:" + tempDir.resolve("database").toAbsolutePath();
		// a database created from the first script before the scripts were recorded
		try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
			execute(connection, "runscript from '" + path(first) + "'");
		}

		// a script missing leaves no record behind, which would have the next time skip the baseline
		assertThrows(DatabaseException.class, () -> database(url, first, tempDir.resolve("missing.sql")));
		try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
			assertFalse(connection.createStatement().executeQuery("select 1 from information_schema.tables " +
							"where table_schema = 'CODION' and table_name = 'INIT_SCRIPT'").next());
		}

		// the scripts listed recorded as run, running none, the first running again failing to create its schema
		H2Database database = database(url, first);
		assertEquals(1, scriptsRun(database));
		database.close();

		// a script added later runs
		database = database(url, first, second);
		assertEquals(2, scriptsRun(database));
		try (Connection connection = database.createConnection(User.user("sa"))) {
			connection.prepareStatement("select id from scripts.t2").execute();
		}
		database.close();
	}

	@Test
	void initScriptsFileSystemPrefix(@TempDir Path tempDir) throws SQLException, IOException {
		Path script = tempDir.resolve("script.sql");
		Files.write(script, singletonList("create schema scripts; create table scripts.t1 (id int);"));
		for (String prefix : asList("async:", "retry:", "retry:nio:")) {
			// a database created through the file system, the script run and recorded, then opened again
			String created = "jdbc:h2:" + prefix + tempDir.resolve("created" + prefix.replace(":", "")).toAbsolutePath();
			H2Database database = database(created, script);
			assertEquals(1, scriptsRun(database));
			database.close();
			database = database(created, script);
			assertEquals(1, scriptsRun(database));
			database.close();

			// one created before the scripts were recorded, found through the file system, so taken for what it is, the script
			// recorded as run, running none, running again failing to create its schema
			Path existing = tempDir.resolve("existing" + prefix.replace(":", "")).toAbsolutePath();
			try (Connection connection = DriverManager.getConnection("jdbc:h2:file:" + existing, "sa", "")) {
				execute(connection, "runscript from '" + path(script) + "'");
			}
			database = database("jdbc:h2:" + prefix + existing, script);
			assertEquals(1, scriptsRun(database));
			database.close();
		}
	}

	@Test
	void initScriptsAdministered(@TempDir Path tempDir) throws SQLException, IOException {
		Path script = tempDir.resolve("script.sql");
		Files.write(script, singletonList("create schema scripts; create table scripts.t1 (id int);"));
		String url = "jdbc:h2:file:" + tempDir.resolve("database").toAbsolutePath();
		// sa given a password since the database was created, and an admin of its own
		try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
			execute(connection, "create user owner password 'owner' admin");
			execute(connection, "alter user sa set password 'secret'");
		}

		// not ours to initialize, nothing run nor recorded
		H2Database database = database(url, script);
		try (Connection connection = database.createConnection(User.parse("owner:owner"));
				 ResultSet schemas = connection.createStatement().executeQuery("select schema_name from information_schema.schemata " +
								 "where schema_name in ('SCRIPTS', 'CODION')")) {
			assertFalse(schemas.next());
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
			assertEquals(message("unique_constraint"), database.exception(unique, INSERT).getMessage());

			SQLException parentMissing = failure(connection, "insert into child (id, parent_id) values (2, 99)");
			assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
			assertEquals(message("parent_missing") + ": CHILD_FK", database.exception(parentMissing, INSERT).getMessage());
			SQLException childExists = failure(connection, "delete from parent where id = 1");
			assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
			assertEquals(message("child_exists") + ": CHILD_FK", database.exception(childExists, DELETE).getMessage());
			SQLException referencedKey = failure(connection, "update parent set id = 5 where id = 1");
			assertEquals(message("child_exists") + ": CHILD_FK", database.exception(referencedKey, UPDATE).getMessage());

			assertEquals(message("null_value") + ": NAME", database.exception(
							failure(connection, "insert into parent (id, name) values (3, null)"), INSERT).getMessage());
			// quoted identifiers in the statement, which is appended to the message
			assertEquals(message("null_value") + ": NAME", database.exception(
							failure(connection, "insert into \"PARENT\" (\"ID\", \"NAME\") values (3, null)"), INSERT).getMessage());
			assertEquals(message("check_constraint") + ": PARENT_CK", database.exception(
							failure(connection, "update parent set amount = -1 where id = 1"), UPDATE).getMessage());
			assertEquals(message("value_too_large") + ": NAME", database.exception(
							failure(connection, "update parent set name = 'abcdefghijklmnop' where id = 1"), UPDATE).getMessage());
			assertEquals(message("value_too_large") + ": AMOUNT", database.exception(
							failure(connection, "update parent set amount = 123456.78 where id = 1"), UPDATE).getMessage());
			assertEquals(message("table_not_found"), database.exception(failure(connection, "select * from missing"), SELECT).getMessage());

			// unrecognized, without the statement
			SQLException syntax = failure(connection, "selec * from parent");
			assertSame(DatabaseException.class, database.exception(syntax, SELECT).getClass());
			assertFalse(database.exception(syntax, SELECT).getMessage().contains("; SQL statement:"));

			// a locked row, which the driver reports as a timeout when NOWAIT is used
			connection.setAutoCommit(false);
			connection2.setAutoCommit(false);
			String selectForUpdate = "select id from parent where id = 1 " + database.selectForUpdateClause(Lock.NOWAIT);
			execute(connection, selectForUpdate);
			SQLException locked = failure(connection2, selectForUpdate);
			assertSame(DatabaseException.class, database.exception(locked, SELECT).getClass());
			assertEquals(message("row_locked"), database.exception(locked, SELECT).getMessage());
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
		assertEquals(message("null_value"), DATABASE.exception(new SQLException("NULL nicht zul\u00E4ssig f\u00FCr Feld NAME", "23502", 23502), INSERT).getMessage());
		assertEquals(message("null_value"), DATABASE.exception(new SQLException(null, "23502", 23502), INSERT).getMessage());
		assertInstanceOf(QueryTimeoutException.class, DATABASE.exception(new SQLException("Statement was canceled", "57014", 57014), SELECT));
	}

	private static H2Database database(String url, Path... scripts) {
		List<String> paths = new ArrayList<>();
		for (Path script : scripts) {
			paths.add(path(script));
		}

		return new H2Database(url, paths);
	}

	private static Map<String, Timestamp> scriptsRecorded(H2Database database) throws SQLException {
		Map<String, Timestamp> recorded = new HashMap<>();
		try (Connection connection = database.createConnection(User.user("sa"));
				 ResultSet resultSet = connection.createStatement().executeQuery("select path, run_at from codion.init_script")) {
			while (resultSet.next()) {
				recorded.put(resultSet.getString(1), resultSet.getTimestamp(2));
			}
		}

		return recorded;
	}

	// As recorded, read directly, opening an H2Database moving the records as it finds them
	private static Set<String> scriptPaths(String url) throws SQLException {
		Set<String> paths = new HashSet<>();
		try (Connection connection = DriverManager.getConnection(url, "sa", "");
				 ResultSet resultSet = connection.createStatement().executeQuery("select path from codion.init_script")) {
			while (resultSet.next()) {
				paths.add(resultSet.getString(1));
			}
		}

		return paths;
	}

	private static int rows(H2Database database) throws SQLException {
		try (Connection connection = database.createConnection(User.user("sa"));
				 ResultSet resultSet = connection.createStatement().executeQuery("select count(*) from scripts.log")) {
			resultSet.next();

			return resultSet.getInt(1);
		}
	}

	private static String path(Path script) {
		return script.toAbsolutePath().toString().replace("\\", "/");
	}

	private static int scriptsRun(H2Database database) throws SQLException {
		try (Connection connection = database.createConnection(User.user("sa"));
				 ResultSet resultSet = connection.createStatement().executeQuery("select count(*) from codion.init_script")) {
			resultSet.next();

			return resultSet.getInt(1);
		}
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
		return ResourceBundle.getBundle(DatabaseException.class.getName()).getString(key);
	}
}

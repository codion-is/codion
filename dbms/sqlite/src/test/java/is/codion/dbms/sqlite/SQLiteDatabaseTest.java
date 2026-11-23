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
 * Copyright (c) 2018 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.Database.Lock;
import is.codion.common.db.database.SetValue;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ErrorType;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.utilities.proxy.ProxyBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import static is.codion.common.db.exception.Operation.*;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class SQLiteDatabaseTest {

	@Test
	void name() {
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		assertEquals("/path/to/file.db", database.name());
		database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db;options");
		assertEquals("/path/to/file.db", database.name());
	}

	@Test
	void autoIncrementQuery() {
		SQLiteDatabase database = new SQLiteDatabase("test");
		assertEquals("SELECT LAST_INSERT_ROWID()", database.autoIncrementQuery(null));
	}

	@Test
	void limitOffsetClause() {
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		assertEquals("", database.limitOffsetClause(null, null, false));
		assertEquals("LIMIT 10", database.limitOffsetClause(10, null, false));
		assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(10, 5, true));
		assertEquals("LIMIT -1 OFFSET 5", database.limitOffsetClause(null, 5, false));
	}

	@Test
	void selectForUpdateClause() {
		// not supported, the database being locked as a whole when written to
		assertEquals("", new SQLiteDatabase("jdbc:sqlite:/path/to/file.db").selectForUpdateClause(Lock.NOWAIT));
	}

	@Test
	void exceptions() {
		// messages as reported by sqlite-jdbc 3.47, the error code being 19 for all constraints and the state null
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		SQLException unique = new SQLException("[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: parent.code)", null, 19);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint"), database.exception(unique, INSERT).getMessage());
		assertInstanceOf(UniqueConstraintException.class, database.exception(new SQLException(
						"[SQLITE_CONSTRAINT_PRIMARYKEY] A PRIMARY KEY constraint failed (UNIQUE constraint failed: parent.id)", null, 19), INSERT));
		// which way is not reported, the operation deciding
		SQLException foreignKey = new SQLException("[SQLITE_CONSTRAINT_FOREIGNKEY] A foreign key constraint failed (FOREIGN KEY constraint failed)", null, 19);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(foreignKey, DELETE));
		assertEquals(message("parent_missing"), database.exception(foreignKey, INSERT).getMessage());
		assertEquals(message("child_exists"), database.exception(foreignKey, DELETE).getMessage());
		assertEquals(message("referential_integrity"), database.exception(foreignKey, UPDATE).getMessage());
		// a delete refused by ON DELETE RESTRICT, as reported by sqlite-jdbc 3.54, raised by a trigger
		SQLException restrict = new SQLException("[SQLITE_CONSTRAINT_TRIGGER] A RAISE function within a trigger fired, "
						+ "causing the SQL statement to abort (FOREIGN KEY constraint failed)", null, 19);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(restrict, DELETE));
		assertEquals(message("child_exists"), database.exception(restrict, DELETE).getMessage());
		// a trigger's own RAISE is not a foreign key failing
		SQLException raised = new SQLException("[SQLITE_CONSTRAINT_TRIGGER] A RAISE function within a trigger fired, "
						+ "causing the SQL statement to abort (no deletes on weekends)", null, 19);
		assertSame(DatabaseException.class, database.exception(raised, DELETE).getClass());
		assertEquals(message("null_value") + ": name", database.exception(new SQLException(
						"[SQLITE_CONSTRAINT_NOTNULL] A NOT NULL constraint failed (NOT NULL constraint failed: parent.name)", null, 19), INSERT).getMessage());
		assertEquals(message("null_value"), database.exception(new SQLException("[SQLITE_CONSTRAINT_NOTNULL] unexpected", null, 19), INSERT).getMessage());
		SQLException check = new SQLException("[SQLITE_CONSTRAINT_CHECK] A CHECK constraint failed (CHECK constraint failed: parent_ck)", null, 19);
		assertSame(DatabaseException.class, database.exception(check, UPDATE).getClass());
		assertEquals(message("check_constraint") + ": parent_ck", database.exception(check, UPDATE).getMessage());
		assertEquals(message("table_not_found"), database.exception(new SQLException(
						"[SQLITE_ERROR] SQL error or missing database (no such table: missing)", null, 1), SELECT).getMessage());
		assertEquals(message("row_locked"), database.exception(new SQLException(
						"[SQLITE_BUSY] The database file is locked (database is locked)", null, 5), UPDATE).getMessage());
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), SELECT));
		SQLException unknown = new SQLException("[SQLITE_ERROR] SQL error or missing database (near \"selec\": syntax error)", null, 1);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.exception(unknown, SELECT).getMessage());
		assertNull(database.exception(new SQLException(), OTHER).getMessage());
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(DatabaseException.class.getName()).getString(key);
	}

	@Test
	void offsetValuesParsed() throws SQLException {
		// the driver does not support getObject(index, OffsetDateTime.class), it stores them as ISO text
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		ResultSet resultSet = ProxyBuilder.of(ResultSet.class)
						.method("getString", int.class, parameters -> {
							switch ((int) parameters.arguments().get(0)) {
								case 1:
									return "2026-09-26T13:45:30.123456+02:00";
								case 2:
									return "2026-09-26 13:45:30+02:00";// SQLite's own format
								case 3:
									return "13:45+02:00";// the zero seconds omitted by toString()
								default:
									return null;
							}
						})
						.build();
		ZoneOffset offset = ZoneOffset.ofHours(2);
		assertEquals(OffsetDateTime.of(2026, 9, 26, 13, 45, 30, 123_456_000, offset),
						database.getter(Types.TIMESTAMP_WITH_TIMEZONE).get(resultSet, 1));
		assertEquals(OffsetDateTime.of(2026, 9, 26, 13, 45, 30, 0, offset),
						database.getter(Types.TIMESTAMP_WITH_TIMEZONE).get(resultSet, 2));
		assertEquals(OffsetTime.of(13, 45, 0, 0, offset), database.getter(Types.TIME_WITH_TIMEZONE).get(resultSet, 3));
		assertNull(database.getter(Types.TIMESTAMP_WITH_TIMEZONE).get(resultSet, 4));
		assertNull(database.getter(Types.TIME_WITH_TIMEZONE).get(resultSet, 4));
	}

	@Test
	void uuidParsed() throws SQLException {
		// the driver does not support getObject(index, UUID.class), nor returns a UUID from getObject(index), it stores them as text
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		UUID uuid = UUID.randomUUID();
		ResultSet resultSet = ProxyBuilder.of(ResultSet.class)
						.method("getString", int.class, parameters -> (int) parameters.arguments().get(0) == 1 ? uuid.toString() : null)
						.build();
		assertEquals(uuid, database.getter(UUID.class).get(resultSet, 1));
		assertNull(database.getter(UUID.class).get(resultSet, 2));
	}

	@Test
	void localValuesParsed() throws SQLException {
		// the driver's getDate(), getTime() and getTimestamp(), read with LEGACY_JDBC, parse its own format only, not ISO text
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		ResultSet resultSet = ProxyBuilder.of(ResultSet.class)
						.method("getString", int.class, parameters -> {
							switch ((int) parameters.arguments().get(0)) {
								case 1:
									return "2026-09-26";
								case 2:
									return "13:45:30.123456";
								case 3:
									return "13:45";// the zero seconds omitted by toString()
								case 4:
									return "2026-09-26T13:45:30.123456";
								case 5:
									return "2026-09-26 13:45:30";// SQLite's own format
								default:
									return null;
							}
						})
						.build();
		assertEquals(LocalDate.of(2026, 9, 26), database.getter(Types.DATE).get(resultSet, 1));
		assertEquals(LocalTime.of(13, 45, 30, 123_456_000), database.getter(Types.TIME).get(resultSet, 2));
		assertEquals(LocalTime.of(13, 45), database.getter(Types.TIME).get(resultSet, 3));
		assertEquals(LocalDateTime.of(2026, 9, 26, 13, 45, 30, 123_456_000), database.getter(Types.TIMESTAMP).get(resultSet, 4));
		assertEquals(LocalDateTime.of(2026, 9, 26, 13, 45, 30), database.getter(Types.TIMESTAMP).get(resultSet, 5));
		assertNull(database.getter(Types.DATE).get(resultSet, 6));
		assertNull(database.getter(Types.TIME).get(resultSet, 6));
		assertNull(database.getter(Types.TIMESTAMP).get(resultSet, 6));
	}

	@Test
	void localValuesWrittenInSQLitesFormat() throws SQLException {
		// as SQLite's date and time functions write them, current_timestamp for example
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
		List<String> calls = new ArrayList<>();
		PreparedStatement statement = ProxyBuilder.of(PreparedStatement.class)
						.method("setString", List.of(int.class, String.class), parameters -> {
							calls.add(parameters.arguments().get(0) + " " + parameters.arguments().get(1));
							return null;
						})
						.method("setNull", List.of(int.class, int.class), parameters -> {
							calls.add(parameters.arguments().get(0) + " null " + parameters.arguments().get(1));
							return null;
						})
						.build();
		set(database, statement, 1, Types.DATE, LocalDate.of(2026, 9, 26));
		set(database, statement, 2, Types.TIME, LocalTime.of(13, 45));
		set(database, statement, 3, Types.TIME, LocalTime.of(13, 45, 30, 500_000_000));
		set(database, statement, 4, Types.TIME, LocalTime.of(13, 45, 30, 123_456_000));
		set(database, statement, 5, Types.TIMESTAMP, LocalDateTime.of(2026, 9, 26, 13, 45));
		set(database, statement, 6, Types.TIMESTAMP, LocalDateTime.of(2000, 1, 1, 0, 0, 0, 1_000));
		set(database, statement, 7, Types.TIMESTAMP, LocalDateTime.of(2000, 1, 1, 0, 0, 0, 1));
		set(database, statement, 8, Types.DATE, null);
		set(database, statement, 9, Types.TIME, null);
		set(database, statement, 10, Types.TIMESTAMP, null);
		// to the microsecond, what is past it left out, not rounded
		set(database, statement, 11, Types.TIME, LocalTime.of(13, 45, 30, 123_456_789));
		set(database, statement, 12, Types.TIME, LocalTime.of(23, 59, 59, 999_999_999));
		set(database, statement, 13, Types.TIMESTAMP, LocalDateTime.of(2026, 12, 31, 23, 59, 59, 999_999_500));
		assertEquals(List.of("1 2026-09-26", "2 13:45:00", "3 13:45:30.5", "4 13:45:30.123456", "5 2026-09-26 13:45:00",
						"6 2000-01-01 00:00:00.000001", "7 2000-01-01 00:00:00",
						"8 null " + Types.DATE, "9 null " + Types.TIME, "10 null " + Types.TIMESTAMP,
						"11 13:45:30.123456", "12 23:59:59.999999", "13 2026-12-31 23:59:59.999999"), calls);
		// and read back as written
		int[] sqlTypes = {Types.DATE, Types.TIME, Types.TIME, Types.TIME, Types.TIMESTAMP, Types.TIMESTAMP, Types.TIMESTAMP};
		for (int i = 0; i < sqlTypes.length; i++) {
			String text = calls.get(i).substring(calls.get(i).indexOf(' ') + 1);
			ResultSet resultSet = ProxyBuilder.of(ResultSet.class)
							.method("getString", int.class, parameters -> text)
							.build();
			assertEquals(text, format(database, sqlTypes[i], database.getter(sqlTypes[i]).get(resultSet, 1)));
		}
	}

	@Test
	void offsetValuesWrittenToTheMicrosecond(@TempDir Path directory) throws SQLException {
		// as the driver stores them, their ISO text, what is past the microsecond left out, as with the local values
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:" + directory.resolve("device.db"));
		OffsetDateTime dateTime = OffsetDateTime.of(2026, 11, 4, 7, 0, 15, 123_456_789, ZoneOffset.ofHours(1));
		OffsetTime time = OffsetTime.of(7, 0, 15, 999_999_999, ZoneOffset.UTC);
		try (Connection connection = database.createConnection(); Statement statement = connection.createStatement()) {
			statement.execute("create table reading (id integer primary key, taken text, taken_time text)");
			try (PreparedStatement insert = connection.prepareStatement("insert into reading (id, taken, taken_time) values (?, ?, ?)")) {
				for (int id = 1; id <= 4; id++) {
					insert.setInt(1, id);
					set(database, insert, 2, Types.TIMESTAMP_WITH_TIMEZONE, id == 1 ? dateTime : id == 2 ? dateTime.withNano(500_000_000) : id == 3 ? dateTime.withNano(0).withSecond(0) : null);
					set(database, insert, 3, Types.TIME_WITH_TIMEZONE, id == 1 ? time : id == 2 ? time.withNano(0) : null);
					insert.executeUpdate();
				}
			}
			List<String> stored = new ArrayList<>();
			try (ResultSet resultSet = statement.executeQuery("select taken, taken_time from reading order by id")) {
				while (resultSet.next()) {
					stored.add(resultSet.getString(1) + " " + resultSet.getString(2));
					if (resultSet.getRow() == 1) {
						// read back as stored
						assertEquals(dateTime.withNano(123_456_000), database.getter(Types.TIMESTAMP_WITH_TIMEZONE).get(resultSet, 1));
						assertEquals(time.withNano(999_999_000), database.getter(Types.TIME_WITH_TIMEZONE).get(resultSet, 2));
					}
				}
			}
			assertEquals(List.of("2026-11-04T07:00:15.123456+01:00 07:00:15.999999Z", "2026-11-04T07:00:15.500+01:00 07:00:15Z",
							"2026-11-04T07:00+01:00 null", "null null"), stored);
		}
	}

	private static void set(SQLiteDatabase database, PreparedStatement statement, int index, int sqlType, Object value) throws SQLException {
		((SetValue<Object>) database.setter(sqlType)).set(statement, index, value);
	}

	private static String format(SQLiteDatabase database, int sqlType, Object value) throws SQLException {
		List<String> written = new ArrayList<>();
		PreparedStatement statement = ProxyBuilder.of(PreparedStatement.class)
						.method("setString", List.of(int.class, String.class), parameters -> written.add((String) parameters.arguments().get(1)))
						.build();
		set(database, statement, 1, sqlType, value);

		return written.get(0);
	}

	@Test
	void schemaFile() {
		// beside the main file, named after it and the schema
		assertEquals("/dir/fieldbook-sync.db", SQLiteDatabase.schemaFile("/dir/fieldbook.db", "sync"));
		assertEquals("/dir/fieldbook-sync", SQLiteDatabase.schemaFile("/dir/fieldbook", "sync"));
		assertEquals("/dir.d/fieldbook-sync", SQLiteDatabase.schemaFile("/dir.d/fieldbook", "sync"));
		assertEquals("/dir/.fieldbook-sync", SQLiteDatabase.schemaFile("/dir/.fieldbook", "sync"));
	}

	@Test
	void schemasRefused() {
		// a database without a file of its own
		assertThrows(IllegalArgumentException.class, () -> new SQLiteDatabase("jdbc:sqlite::memory:", "sync"));
		assertThrows(IllegalArgumentException.class, () -> new SQLiteDatabase("jdbc:sqlite:", "sync"));
		assertThrows(IllegalArgumentException.class, () -> new SQLiteDatabase("jdbc:sqlite:file:x?mode=memory", "sync"));
		// a name SQLite reserves, one not an identifier, and an empty one
		assertThrows(IllegalArgumentException.class, () -> new SQLiteDatabase("jdbc:sqlite:/dir/x.db", "main"));
		assertThrows(IllegalArgumentException.class, () -> new SQLiteDatabase("jdbc:sqlite:/dir/x.db", "sync-ledger"));
		assertThrows(IllegalArgumentException.class, () -> new SQLiteDatabase("jdbc:sqlite:/dir/x.db", "sync,,fieldbook"));
	}

	@Test
	void schemasAttached(@TempDir Path directory) throws SQLException {
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:" + directory.resolve("fieldbook.db") + "?synchronous=EXTRA", "fieldbook, sync");
		try (Connection connection = database.createConnection(); Statement statement = connection.createStatement()) {
			statement.execute("create table fieldbook.observation (id integer primary key)");
			statement.execute("create table sync.row_state (id integer primary key)");
			// with the main database's journal mode and synchronous setting
			assertEquals("delete", pragma(statement, "fieldbook.journal_mode"));
			assertEquals("3", pragma(statement, "sync.synchronous"));
		}
		assertTrue(Files.exists(directory.resolve("fieldbook-fieldbook.db")));
		assertTrue(Files.exists(directory.resolve("fieldbook-sync.db")));
		// on every connection
		try (Connection connection = database.createConnection(); Statement statement = connection.createStatement()) {
			assertEquals(1, statement.executeUpdate("insert into fieldbook.observation values (1)"));
			assertEquals(1, statement.executeUpdate("insert into sync.row_state values (1)"));
		}
	}

	@Test
	void schemasRefusedInWalMode(@TempDir Path directory) throws SQLException {
		// by the url
		SQLiteDatabase wal = new SQLiteDatabase("jdbc:sqlite:" + directory.resolve("wal.db") + "?journal_mode=WAL", "fieldbook");
		IllegalStateException exception = assertThrows(IllegalStateException.class, wal::createConnection);
		assertTrue(exception.getMessage().contains("WAL"), exception.getMessage());
		// nothing attached, so no file created for the schema
		assertFalse(Files.exists(directory.resolve("wal-fieldbook.db")));
		// by the file, which keeps the mode once set
		String url = "jdbc:sqlite:" + directory.resolve("kept.db");
		try (Connection connection = new SQLiteDatabase(url).createConnection(); Statement statement = connection.createStatement()) {
			assertEquals("wal", pragma(statement, "journal_mode = wal"));
			statement.execute("create table setting (id integer primary key)");
		}
		assertThrows(IllegalStateException.class, () -> new SQLiteDatabase(url, "fieldbook").createConnection());
		// and with init scripts, which run on a connection
		assertThrows(IllegalStateException.class, () -> new SQLiteDatabase(url, "fieldbook", asList("classpath:init_scripts/schema.sql")));
		// without schemas the mode is the application's to choose
		try (Connection connection = new SQLiteDatabase(url).createConnection(); Statement statement = connection.createStatement()) {
			assertEquals("wal", pragma(statement, "journal_mode"));
		}
		// back in a rollback journal, the schemas attach
		try (Connection connection = new SQLiteDatabase(url + "?journal_mode=DELETE", "fieldbook").createConnection();
				 Statement statement = connection.createStatement()) {
			assertEquals("delete", pragma(statement, "fieldbook.journal_mode"));
		}
	}

	@Test
	void readDuringALargeTransaction(@TempDir Path directory) throws SQLException {
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:" + directory.resolve("device.db") + "?busy_timeout=100", "fieldbook");
		List<String> tables = asList("main.setting", "fieldbook.area");
		try (Connection writer = database.createConnection(); Connection reader = database.createConnection();
				 Statement write = writer.createStatement(); Statement read = reader.createStatement()) {
			for (String table : tables) {
				write.execute("create table " + table + " (id integer primary key, name text)");
			}
			writer.setAutoCommit(false);
			database.startTransaction(writer);
			// 8 MB in each file, four times what SQLite's page cache holds by default, which it would otherwise make room
			// for by writing the file, under its exclusive lock, kept until the transaction ends
			for (String table : tables) {
				write.execute("with recursive x(n) as (select 1 union all select n + 1 from x where n < 8000) " +
								"insert into " + table + " (name) select printf('%1000s', n) from x");
			}
			// a reader on another connection is not locked out, and sees the transaction once it is committed
			for (String table : tables) {
				assertEquals("0", query(read, "select count(*) from " + table));
			}
			writer.commit();
			for (String table : tables) {
				assertEquals("8000", query(read, "select count(*) from " + table));
			}
		}
	}

	private static String query(Statement statement, String query) throws SQLException {
		try (ResultSet resultSet = statement.executeQuery(query)) {
			resultSet.next();

			return resultSet.getString(1);
		}
	}

	private static String pragma(Statement statement, String pragma) throws SQLException {
		try (ResultSet resultSet = statement.executeQuery("pragma " + pragma)) {
			resultSet.next();

			return resultSet.getString(1);
		}
	}

	@Test
	void startTransaction(@TempDir Path directory) throws Exception {
		SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:" + directory.resolve("device.db") + "?busy_timeout=5000", "fieldbook, sync");
		try (Connection first = database.createConnection(); Connection second = database.createConnection();
				 Statement firstStatement = first.createStatement(); Statement secondStatement = second.createStatement()) {
			first.setAutoCommit(false);
			second.setAutoCommit(false);
			firstStatement.execute("create table fieldbook.area (id integer primary key)");
			firstStatement.execute("create table sync.row_state (id integer primary key)");
			first.commit();

			database.startTransaction(first);
			firstStatement.execute("insert into fieldbook.area values (1)");
			// the second waits for the first, holding the write lock, to commit
			long[] started = new long[1];
			Thread thread = new Thread(() -> {
				database.startTransaction(second);
				started[0] = System.nanoTime();
			});
			thread.start();
			Thread.sleep(300);
			long committed = System.nanoTime();
			first.commit();
			thread.join();
			assertTrue(started[0] >= committed);
			// reading before it writes, in another file
			try (ResultSet resultSet = secondStatement.executeQuery("select count(*) from fieldbook.area")) {
				resultSet.next();
				assertEquals(1, resultSet.getInt(1));
			}
			secondStatement.execute("insert into sync.row_state values (1)");
			second.commit();
		}
		// another writer not finishing in time, the connection left as the driver expects it
		SQLiteDatabase impatient = new SQLiteDatabase("jdbc:sqlite:" + directory.resolve("device.db") + "?busy_timeout=100", "fieldbook, sync");
		try (Connection first = impatient.createConnection(); Connection second = impatient.createConnection();
				 Statement firstStatement = first.createStatement(); Statement secondStatement = second.createStatement()) {
			first.setAutoCommit(false);
			second.setAutoCommit(false);
			impatient.startTransaction(first);
			firstStatement.execute("insert into fieldbook.area values (2)");
			DatabaseException locked = assertThrows(DatabaseException.class, () -> impatient.startTransaction(second));
			assertEquals(ErrorType.ROW_LOCKED, locked.errorType().orElse(null));
			second.rollback();
			first.commit();
			impatient.startTransaction(second);
			secondStatement.execute("insert into fieldbook.area values (3)");
			second.commit();
		}
	}
}

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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SQLiteInitScriptsTest {

	private static final String SCHEMA = "create table fieldbook.area (id integer primary key, name text not null);\n" +
					"create table fieldbook.area_log (entry text not null);\n" +
					"create table sync.row_state (uuid text primary key);\n" +
					"-- a trigger, its body's semicolons within it, writing only its own file, as one not TEMP must\n" +
					"create trigger fieldbook.area_logged after insert on area begin\n" +
					"  insert into area_log values ('area ' || new.id);\n" +
					"  insert into area_log values ('logged; twice');\n" +
					"end;\n";

	@Test
	void runOnceEach(@TempDir Path directory) throws Exception {
		Path schema = script(directory, "schema.sql", SCHEMA);
		Path seed = script(directory, "seed.sql", "insert into fieldbook.area (name) values ('Þórsmörk; the first');");
		String url = url(directory);
		SQLiteDatabase database = database(url, schema, seed);
		assertEquals(List.of(path(schema), path(seed)), recorded(database));
		assertEquals(List.of("Þórsmörk; the first"), query(database, "select name from fieldbook.area"));
		// the trigger ran, its body whole
		assertEquals(List.of("area 1", "logged; twice"), query(database, "select entry from fieldbook.area_log"));
		database.close();

		// the scripts run, one added
		Path more = script(directory, "more.sql", "insert into fieldbook.area (name) values ('Hekla');");
		database = database(url, schema, seed, more);
		assertEquals(List.of(path(more), path(schema), path(seed)), recorded(database));
		assertEquals(List.of("Þórsmörk; the first", "Hekla"), query(database, "select name from fieldbook.area order by id"));
		database.close();

		// once per JVM, until closed
		database = database(url, schema, seed, more);
		SQLiteDatabase again = database(url, schema, seed, more, script(directory, "not_run.sql", "insert into nowhere values (1);"));
		assertEquals(3, recorded(again).size());
		database.close();
	}

	@Test
	void refused(@TempDir Path directory) throws Exception {
		Path schema = script(directory, "schema.sql", SCHEMA);
		Path seed = script(directory, "seed.sql", "insert into fieldbook.area (name) values ('Hekla');");
		String url = url(directory);
		database(url, schema, seed).close();

		Files.write(seed, singletonList("insert into fieldbook.area (name) values ('Katla');"), StandardCharsets.UTF_8);
		assertEquals("Init script " + path(seed) + " has changed since it was run",
						assertThrows(DatabaseException.class, () -> database(url, schema, seed)).getMessage());
		assertEquals("Init scripts run, but no longer listed: " + path(seed),
						assertThrows(DatabaseException.class, () -> database(url, schema)).getMessage());
		// left as it was, and validated as opening it refuses it, read only
		Files.write(seed, singletonList("insert into fieldbook.area (name) values ('Hekla');"), StandardCharsets.UTF_8);
		try (Connection connection = DriverManager.getConnection(url + "?open_mode=1")) {
			SQLiteDatabaseFactory.validateInitScripts(connection, path(schema), path(seed));
			assertThrows(DatabaseException.class, () -> SQLiteDatabaseFactory.validateInitScripts(connection, path(schema)));
		}
		SQLiteDatabase database = database(url, schema, seed);
		assertEquals(List.of("Hekla"), query(database, "select name from fieldbook.area"));
		database.close();
		// a script path refused before the database is opened
		Path other = directory.resolve("other");
		Files.createDirectories(other);
		assertThrows(SecurityException.class, () -> new SQLiteDatabase(url(other), "fieldbook, sync", singletonList("x.sql';drop")));
		assertFalse(Files.exists(other.resolve("device.db")));
	}

	@Test
	void failingScriptLeavesNothing(@TempDir Path directory) throws Exception {
		Path schema = script(directory, "schema.sql", SCHEMA);
		Path failing = script(directory, "failing.sql", "create table fieldbook.observation (id integer primary key);\n" +
						"insert into fieldbook.area (name) values ('Askja');\n" +
						"insert into nowhere values (1);\n");
		String url = url(directory);
		DatabaseException exception = assertThrows(DatabaseException.class, () -> database(url, schema, failing));
		assertTrue(exception.getMessage().startsWith("Init script " + path(failing) + " failed: "));
		SQLiteDatabase database = database(url, schema);
		// the schema ran and is recorded, the failing script left nothing, its table, row or record
		assertEquals(List.of(path(schema)), recorded(database));
		assertEquals(List.of(), query(database, "select name from fieldbook.sqlite_schema where name = 'observation'"));
		assertEquals(List.of(), query(database, "select name from fieldbook.area"));
		database.close();
		// fixed, it runs
		script(directory, "failing.sql", "create table fieldbook.observation (id integer primary key);\n");
		database = database(url, schema, failing);
		assertEquals(2, recorded(database).size());
		database.close();
	}

	@Test
	void classpathAndBaseline(@TempDir Path directory) throws Exception {
		String url = url(directory);
		SQLiteDatabase database = new SQLiteDatabase(url, "fieldbook, sync", singletonList("classpath:init_scripts/schema.sql"));
		assertEquals(List.of("classpath:init_scripts/schema.sql"), recorded(database));
		assertEquals(List.of("area"), query(database, "select name from fieldbook.sqlite_schema where type = 'table'"));
		database.close();

		// an existing database without the record, all listed recorded as run, none running
		Path without = directory.resolve("without");
		Files.createDirectories(without);
		try (Connection connection = DriverManager.getConnection(url(without));
				 Statement statement = connection.createStatement()) {
			statement.execute("create table existing (id integer)");
		}
		Path schema = script(directory, "schema.sql", SCHEMA);
		database = database(url(without), schema);
		assertEquals(List.of(path(schema)), recorded(database));
		assertEquals(List.of(), query(database, "select name from fieldbook.sqlite_schema where type = 'table'"));
		database.close();
	}

	private static SQLiteDatabase database(String url, Path... scripts) {
		List<String> paths = new ArrayList<>();
		for (Path script : scripts) {
			paths.add(path(script));
		}

		return new SQLiteDatabase(url, "fieldbook, sync", paths);
	}

	private static List<String> recorded(SQLiteDatabase database) throws SQLException {
		return query(database, "select path from main.codion_init_script order by path");
	}

	private static List<String> query(SQLiteDatabase database, String query) throws SQLException {
		List<String> values = new ArrayList<>();
		try (Connection connection = database.createConnection(); Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(query)) {
			while (resultSet.next()) {
				values.add(resultSet.getString(1));
			}
		}

		return values;
	}

	private static Path script(Path directory, String name, String content) throws IOException {
		return Files.write(directory.resolve(name), asList(content.split("\n")), StandardCharsets.UTF_8);
	}

	private static String url(Path directory) {
		return "jdbc:sqlite:" + directory.resolve("device.db");
	}

	private static String path(Path script) {
		return script.toAbsolutePath().toString().replace("\\", "/");
	}
}

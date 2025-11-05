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
package is.codion.dbms.h2;

import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

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
		assertEquals(H2Database.AUTO_INCREMENT_QUERY, DATABASE.autoIncrementQuery(null));
	}

	@Test
	void sequenceQuery() {
		final String idSource = "seq";
		assertEquals(H2Database.SEQUENCE_VALUE_QUERY + idSource, DATABASE.sequenceQuery(idSource));
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
}
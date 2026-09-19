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

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ResourceBundle;

import static is.codion.common.db.exception.Operation.*;
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
		assertEquals("", new SQLiteDatabase("jdbc:sqlite:/path/to/file.db").selectForUpdateClause());
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
}

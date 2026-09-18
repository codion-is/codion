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
package is.codion.dbms.postgresql;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ResourceBundle;

import static is.codion.common.db.database.Database.Operation.*;
import static org.junit.jupiter.api.Assertions.*;

public class PostgreSQLDatabaseTest {

	private static final String URL = "jdbc:postgresql://host:1234/sid";

	@Test
	void name() {
		PostgreSQLDatabase database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid", true);
		assertEquals("sid", database.name());
		database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid;options", true);
		assertEquals("sid", database.name());
		database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters", true);
		assertEquals("sid", database.name());
		database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters;options", true);
		assertEquals("sid", database.name());
	}

	@Test
	void sequenceQueryNullSequence() {
		assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(URL, true).sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		PostgreSQLDatabase db = new PostgreSQLDatabase(URL, true);
		assertEquals("SELECT CURRVAL('seq')", db.autoIncrementQuery("seq"));
	}

	@Test
	void sequenceQuery() {
		PostgreSQLDatabase db = new PostgreSQLDatabase(URL, true);
		assertEquals("SELECT NEXTVAL('seq')", db.sequenceQuery("seq"));
	}

	@Test
	void constructorNullHost() {
		assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(null, true));
	}

	@Test
	void exceptions() {
		// states and messages as reported by PostgreSQL 18.4, pgjdbc 42.7.11
		PostgreSQLDatabase database = new PostgreSQLDatabase(URL, true);
		SQLException unique = new SQLException("ERROR: duplicate key value violates unique constraint \"parent_uk\"\n"
						+ "  Detail: Key (code, name)=(A, b) already exists.", "23505");
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint") + ": (code, name)=(A, b)", database.errorMessage(unique, INSERT));

		SQLException parentMissing = new SQLException("ERROR: insert or update on table \"child\" violates foreign key constraint \"child_fk\"\n"
						+ "  Detail: Key (parent_id)=(99) is not present in table \"parent\".", "23503");
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, UPDATE));
		assertEquals(message("parent_missing"), database.errorMessage(parentMissing, UPDATE));

		SQLException childExists = new SQLException("ERROR: update or delete on table \"parent\" violates foreign key constraint \"child_fk\" on table \"child\"\n"
						+ "  Detail: Key (id)=(1) is still referenced from table \"child\".", "23503");
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.errorMessage(childExists, DELETE));
		// updating a referenced key, not a missing parent
		assertEquals(message("child_exists"), database.errorMessage(childExists, UPDATE));

		// a server with translated messages, the operation then deciding
		SQLException translated = new SQLException("FEHLER: Aktualisieren oder L\u00F6schen in Tabelle \u00BBparent\u00AB verletzt Fremdschl\u00FCssel-Constraint", "23503");
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(translated, DELETE));
		assertEquals(message("child_exists"), database.errorMessage(translated, DELETE));
		assertEquals(message("referential_integrity"), database.errorMessage(translated, UPDATE));

		SQLException nullValue = new SQLException("ERROR: null value in column \"name\" of relation \"parent\" violates not-null constraint\n"
						+ "  Detail: Failing row contains (3, null, null, null).", "23502");
		assertEquals(message("null_value") + ": name", database.errorMessage(nullValue, INSERT));
		// before version 13
		assertEquals(message("null_value") + ": name", database.errorMessage(
						new SQLException("ERROR: null value in column \"name\" violates not-null constraint", "23502"), INSERT));
		assertEquals(message("null_value"), database.errorMessage(
						new SQLException("FEHLER: NULL-Wert in Spalte \u00BBname\u00AB verletzt Not-Null-Constraint", "23502"), INSERT));
		assertEquals(message("null_value"), database.errorMessage(new SQLException(null, "23502"), INSERT));

		assertEquals(message("check_constraint"), database.errorMessage(new SQLException("check", "23514"), UPDATE));
		assertEquals(message("value_too_large"), database.errorMessage(new SQLException("too long", "22001"), UPDATE));
		assertEquals(message("value_too_large"), database.errorMessage(new SQLException("numeric field overflow", "22003"), UPDATE));
		assertEquals(message("missing_privileges"), database.errorMessage(new SQLException("permission denied", "42501"), SELECT));
		assertEquals(message("table_not_found"), database.errorMessage(new SQLException("relation does not exist", "42P01"), SELECT));
		assertEquals(message("row_locked"), database.errorMessage(new SQLException("could not obtain lock on row", "55P03"), SELECT));

		SQLException timeout = new SQLException("ERROR: canceling statement due to user request", "57014");
		assertInstanceOf(QueryTimeoutException.class, database.exception(timeout, SELECT));
		assertEquals(message("timeout"), database.errorMessage(timeout, SELECT));

		SQLException authentication = new SQLException("FATAL: password authentication failed for user \"scott\"", "28P01");
		assertTrue(database.isAuthenticationException(authentication));
		assertEquals(message("authentication"), database.errorMessage(authentication, OTHER));

		SQLException unknown = new SQLException("ERROR: syntax error at or near \"selec\"", "42601");
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.errorMessage(unknown, SELECT));
		assertEquals("connection refused", database.errorMessage(new SQLException("connection refused"), OTHER));
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}
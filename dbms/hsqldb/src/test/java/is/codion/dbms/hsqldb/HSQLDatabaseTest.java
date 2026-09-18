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
package is.codion.dbms.hsqldb;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ResourceBundle;

import static is.codion.common.db.database.Database.Operation.*;
import static org.junit.jupiter.api.Assertions.*;

public class HSQLDatabaseTest {

	private static final String URL = "jdbc:hsqldb:hsql//host:1234/sid";

	@Test
	void name() {
		HSQLDatabase database = new HSQLDatabase("jdbc:hsqldb:file:C:/data/sample;option=true;option2=false");
		assertEquals("C:/data/sample", database.name());
		database = new HSQLDatabase("jdbc:hsqldb:mem:sampleDb;option=true;option2=false");
		assertEquals("sampleDb", database.name());
		database = new HSQLDatabase("jdbc:hsqldb:mem:");
		assertEquals("private", database.name());
		database = new HSQLDatabase("jdbc:hsqldb:res:/dir/db");
		assertEquals("/dir/db", database.name());
	}

	@Test
	void sequenceSQLNullSequence() {
		assertThrows(NullPointerException.class, () -> new HSQLDatabase(URL).sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		HSQLDatabase db = new HSQLDatabase(URL);
		assertEquals("CALL IDENTITY()", db.autoIncrementQuery(null));
	}

	@Test
	void sequenceQuery() {
		HSQLDatabase db = new HSQLDatabase(URL);
		final String idSource = "seq";
		assertEquals("CALL NEXT VALUE FOR " + idSource, db.sequenceQuery(idSource));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new HSQLDatabase(null));
	}

	@Test
	void selectForUpdateClause() {
		// NOWAIT is not supported
		assertEquals("FOR UPDATE", new HSQLDatabase(URL).selectForUpdateClause());
	}

	@Test
	void exceptions() {
		// codes, states and messages as reported by HSQLDB 2.7.4
		HSQLDatabase database = new HSQLDatabase(URL);
		SQLException unique = new SQLException("integrity constraint violation: unique constraint or index violation ; PARENT_UK table: PARENT", "23505", -104);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint"), database.errorMessage(unique, INSERT));
		SQLException parentMissing = new SQLException("integrity constraint violation: foreign key no parent ; CHILD_FK table: CHILD value: 99", "23503", -177);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, UPDATE));
		assertEquals(message("parent_missing"), database.errorMessage(parentMissing, UPDATE));
		SQLException childExists = new SQLException("integrity constraint violation: foreign key no action ; CHILD_FK table: CHILD", "23504", -8);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.errorMessage(childExists, UPDATE));
		assertEquals(message("null_value") + ": NAME", database.errorMessage(new SQLException(
						"integrity constraint violation: NOT NULL check constraint ; SYS_CT_10093 table: PARENT column: NAME", "23502", -10), INSERT));
		assertEquals(message("null_value"), database.errorMessage(new SQLException(null, "23502", -10), INSERT));
		assertEquals(message("check_constraint"), database.errorMessage(new SQLException(
						"integrity constraint violation: check constraint ; PARENT_CK table: PARENT", "23513", -157), UPDATE));
		assertEquals(message("value_too_large"), database.errorMessage(new SQLException("data exception: string data, right truncation", "22001", -3401), UPDATE));
		assertEquals(message("value_too_large"), database.errorMessage(new SQLException("data exception: numeric value out of range", "22003", -3403), UPDATE));
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), SELECT));
		assertTrue(database.isAuthenticationException(new SQLException("invalid authorization specification: SA", "28000", -4000)));
		assertTrue(database.isAuthenticationException(new SQLException("invalid authorization specification - not found: scott", "28501", -4001)));
		// a missing privilege and a missing object are reported as one
		SQLException unknown = new SQLException("user lacks privilege or object not found: MISSING", "42501", -5501);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.errorMessage(unknown, SELECT));
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}

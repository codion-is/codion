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
import is.codion.common.db.exception.AuthenticationException;
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
		assertEquals(message("unique_constraint"), database.exception(unique, INSERT).getMessage());
		SQLException parentMissing = new SQLException("integrity constraint violation: foreign key no parent ; CHILD_FK table: CHILD value: 99", "23503", -177);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, UPDATE));
		assertEquals(message("parent_missing"), database.exception(parentMissing, UPDATE).getMessage());
		SQLException childExists = new SQLException("integrity constraint violation: foreign key no action ; CHILD_FK table: CHILD", "23504", -8);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.exception(childExists, UPDATE).getMessage());
		assertEquals(message("null_value") + ": NAME", database.exception(new SQLException(
						"integrity constraint violation: NOT NULL check constraint ; SYS_CT_10093 table: PARENT column: NAME", "23502", -10), INSERT).getMessage());
		assertEquals(message("null_value"), database.exception(new SQLException(null, "23502", -10), INSERT).getMessage());
		assertEquals(message("check_constraint"), database.exception(new SQLException(
						"integrity constraint violation: check constraint ; PARENT_CK table: PARENT", "23513", -157), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(new SQLException("data exception: string data, right truncation", "22001", -3401), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(new SQLException("data exception: numeric value out of range", "22003", -3403), UPDATE).getMessage());
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), SELECT));
		assertInstanceOf(AuthenticationException.class, database.exception(new SQLException("invalid authorization specification: SA", "28000", -4000), OTHER));
		assertInstanceOf(AuthenticationException.class, database.exception(new SQLException("invalid authorization specification - not found: scott", "28501", -4001), OTHER));
		// a missing privilege and a missing object are reported as one
		SQLException unknown = new SQLException("user lacks privilege or object not found: MISSING", "42501", -5501);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.exception(unknown, SELECT).getMessage());
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}

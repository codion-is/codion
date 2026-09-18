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
package is.codion.dbms.derby;

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

public class DerbyDatabaseTest {

	private static final String URL = "jdbc:derby://host:1234/sid";

	@Test
	void name() {
		DerbyDatabase database = new DerbyDatabase("jdbc:derby:C:/data/sample;option=true;option2=false");
		assertEquals("C:/data/sample", database.name());
		database = new DerbyDatabase("jdbc:derby://sample.db:1234;option=true;option2=false");
		assertEquals("sample.db:1234", database.name());
		database = new DerbyDatabase("jdbc:derby://sample.db:1234");
		assertEquals("sample.db:1234", database.name());
		database = new DerbyDatabase("jdbc:derby://sample.db:1234/dbname");
		assertEquals("dbname", database.name());
	}

	@Test
	void sequenceQuery() {
		assertEquals("VALUES NEXT VALUE FOR seq", new DerbyDatabase(URL).sequenceQuery("seq"));
		assertThrows(NullPointerException.class, () -> new DerbyDatabase(URL).sequenceQuery(null));
	}

	@Test
	void supportsNoWait() {
		DerbyDatabase db = new DerbyDatabase(URL);
		assertEquals("FOR UPDATE", db.selectForUpdateClause());
	}

	@Test
	void autoIncrementQuery() {
		DerbyDatabase db = new DerbyDatabase(URL);
		assertEquals("VALUES IDENTITY_VAL_LOCAL()", db.autoIncrementQuery("id_source"));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new DerbyDatabase(null));
	}

	@Test
	void supports() {
		DerbyDatabaseFactory factory = new DerbyDatabaseFactory();
		// 10.14 and earlier
		assertTrue(factory.supports("org.apache.derby.jdbc.AutoloadedDriver"));
		assertTrue(factory.supports("org.apache.derby.jdbc.ClientDriver"));
		// 10.15 and later
		assertTrue(factory.supports("org.apache.derby.iapi.jdbc.AutoloadedDriver"));
		assertTrue(factory.supports("org.apache.derby.client.ClientAutoloadedDriver"));
		assertFalse(factory.supports("org.h2.Driver"));
	}

	@Test
	void exceptions() {
		// states and messages as reported by Derby 10.17, the error code being a severity
		DerbyDatabase database = new DerbyDatabase(URL);
		SQLException unique = new SQLException("The statement was aborted because it would have caused a duplicate key value in a unique or "
						+ "primary key constraint or unique index identified by 'PARENT_UK' defined on 'PARENT'.", "23505", 30000);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint"), database.errorMessage(unique, INSERT));
		// which way is not reported, the operation deciding
		SQLException parentMissing = new SQLException("INSERT on table 'CHILD' caused a violation of foreign key constraint 'CHILD_FK' for key (99).  "
						+ "The statement has been rolled back.", "23503", 30000);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
		assertEquals(message("parent_missing"), database.errorMessage(parentMissing, INSERT));
		SQLException childExists = new SQLException("DELETE on table 'PARENT' caused a violation of foreign key constraint 'CHILD_FK' for key (1).  "
						+ "The statement has been rolled back.", "23503", 30000);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.errorMessage(childExists, DELETE));
		assertEquals(message("referential_integrity"), database.errorMessage(childExists, UPDATE));
		assertEquals(message("null_value") + ": NAME", database.errorMessage(new SQLException("Column 'NAME'  cannot accept a NULL value.", "23502", 30000), INSERT));
		assertEquals(message("null_value"), database.errorMessage(new SQLException(null, "23502", 30000), INSERT));
		assertEquals(message("check_constraint"), database.errorMessage(new SQLException(
						"The check constraint 'PARENT_CK' was violated while performing an INSERT or UPDATE on table '\"APP\".\"PARENT\"'.", "23513", 30000), UPDATE));
		assertEquals(message("value_too_large"), database.errorMessage(new SQLException(
						"A truncation error was encountered trying to shrink VARCHAR 'abcdefghijklmnop' to length 10.", "22001", 30000), UPDATE));
		assertEquals(message("table_not_found"), database.errorMessage(new SQLException("Table/View 'MISSING' does not exist.", "42X05", 30000), SELECT));
		assertEquals(message("row_locked"), database.errorMessage(new SQLException("A lock could not be obtained within the time requested", "40XL1", 30000), UPDATE));
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLException("The statement has been cancelled or timed out.", "XCL52", 30000), SELECT));
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), SELECT));
		assertTrue(database.isAuthenticationException(new SQLException("Connection authentication failure occurred.  Reason: Invalid authentication..", "08004", 40000)));
		SQLException unknown = new SQLException("Syntax error: Encountered \"selec\" at line 1, column 1.", "42X01", 30000);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.errorMessage(unknown, SELECT));
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}

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
package is.codion.dbms.sqlserver;

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

public class SQLServerDatabaseTest {

	private static final String URL = "jdbc:sqlserver://host:1234;databaseName=sid";

	@Test
	void name() {
		SQLServerDatabase database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234");
		assertEquals("instance", database.name());
		database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234;options");
		assertEquals("instance", database.name());
	}

	@Test
	void sequenceQuery() {
		assertEquals("SELECT NEXT VALUE FOR seq", new SQLServerDatabase(URL).sequenceQuery("seq"));
		assertThrows(NullPointerException.class, () -> new SQLServerDatabase(URL).sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		SQLServerDatabase db = new SQLServerDatabase(URL);
		assertThrows(UnsupportedOperationException.class, () -> db.autoIncrementQuery("table"));
	}

	@Test
	void constructorNullHost() {
		assertThrows(NullPointerException.class, () -> new SQLServerDatabase(null));
	}

	@Test
	void maximumParameters() {
		assertEquals(2098, new SQLServerDatabase(URL).maximumParameters());
	}

	@Test
	void limitOffsetClause() {
		SQLServerDatabase database = new SQLServerDatabase(URL);
		assertEquals("", database.limitOffsetClause(null, null, false));
		assertEquals("", database.limitOffsetClause(null, null, true));
		assertEquals("OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", database.limitOffsetClause(10, null, true));
		assertEquals("OFFSET 5 ROWS", database.limitOffsetClause(null, 5, true));
		assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", database.limitOffsetClause(10, 5, true));
		assertEquals("ORDER BY (SELECT NULL) OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", database.limitOffsetClause(10, null, false));
		assertEquals("ORDER BY (SELECT NULL) OFFSET 5 ROWS", database.limitOffsetClause(null, 5, false));
		assertEquals("ORDER BY (SELECT NULL) OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", database.limitOffsetClause(10, 5, false));
	}

	@Test
	void exceptions() {
		// codes, states and messages as reported by SQL Server 2022, mssql-jdbc 12.2
		SQLServerDatabase database = new SQLServerDatabase(URL);
		SQLException unique = new SQLException("Violation of UNIQUE KEY constraint 'parent_uk'. Cannot insert duplicate key in object 'dbo.parent'. "
						+ "The duplicate key value is (A, b).", "23000", 2627);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint") + ": (A, b)", database.exception(unique, INSERT).getMessage());
		SQLException uniqueIndex = new SQLException("Cannot insert duplicate key row in object 'dbo.parent' with unique index 'parent_idx'. "
						+ "The duplicate key value is (A).", "23000", 2601);
		assertInstanceOf(UniqueConstraintException.class, database.exception(uniqueIndex, INSERT));

		// the same error code for foreign key, reference and check constraints
		SQLException parentMissing = new SQLException("The INSERT statement conflicted with the FOREIGN KEY constraint \"child_fk\". "
						+ "The conflict occurred in database \"db\", table \"dbo.parent\", column 'id'.", "23000", 547);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
		assertEquals(message("parent_missing"), database.exception(parentMissing, INSERT).getMessage());
		SQLException childExists = new SQLException("The DELETE statement conflicted with the REFERENCE constraint \"child_fk\". "
						+ "The conflict occurred in database \"db\", table \"dbo.child\", column 'parent_id'.", "23000", 547);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.exception(childExists, DELETE).getMessage());
		assertEquals(message("child_exists"), database.exception(childExists, UPDATE).getMessage());
		SQLException selfReference = new SQLException("The DELETE statement conflicted with the SAME TABLE REFERENCE constraint \"parent_fk\". "
						+ "The conflict occurred in database \"db\", table \"dbo.parent\", column 'parent_id'.", "23000", 547);
		assertEquals(message("child_exists"), database.exception(selfReference, DELETE).getMessage());
		SQLException check = new SQLException("The UPDATE statement conflicted with the CHECK constraint \"REFERENCE_CK\". "
						+ "The conflict occurred in database \"db\", table \"dbo.parent\", column 'amount'.", "23000", 547);
		assertSame(DatabaseException.class, database.exception(check, UPDATE).getClass());
		assertEquals(message("check_constraint"), database.exception(check, UPDATE).getMessage());
		// not recognizable
		assertSame(DatabaseException.class, database.exception(new SQLException("conflict", "23000", 547), UPDATE).getClass());
		assertSame(DatabaseException.class, database.exception(new SQLException(null, "23000", 547), UPDATE).getClass());

		assertEquals(message("null_value") + ": name", database.exception(new SQLException(
						"Cannot insert the value NULL into column 'name', table 'db.dbo.parent'; column does not allow nulls. INSERT fails.", "23000", 515), INSERT).getMessage());
		assertEquals(message("value_too_large") + ": name", database.exception(new SQLException(
						"String or binary data would be truncated in table 'db.dbo.parent', column 'name'. Truncated value: 'abcdefghij'.", "S0001", 2628), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(new SQLException("String or binary data would be truncated.", "22001", 8152), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(new SQLException(
						"Arithmetic overflow error converting numeric to data type numeric.", "S0008", 8115), UPDATE).getMessage());
		assertEquals(message("table_not_found"), database.exception(new SQLException("Invalid object name 'missing'.", "S0002", 208), SELECT).getMessage());
		assertEquals(message("missing_privileges"), database.exception(new SQLException(
						"The SELECT permission was denied on the object 'parent', database 'db', schema 'dbo'.", "S0005", 229), SELECT).getMessage());
		assertEquals(message("row_locked"), database.exception(new SQLException("Lock request time out period exceeded.", "S0001", 1222), SELECT).getMessage());

		// reported with error code 0
		SQLException timeout = new SQLTimeoutException("The query has timed out.", "HY008", 0);
		assertInstanceOf(QueryTimeoutException.class, database.exception(timeout, SELECT));
		assertEquals(message("timeout"), database.exception(timeout, SELECT).getMessage());

		SQLException authentication = new SQLException("Login failed for user 'scott'. ClientConnectionId:cdfadf91-a88b-4170-8dee-4ec0a4b62b68", "S0001", 18456);
		assertInstanceOf(AuthenticationException.class, database.exception(authentication, OTHER));
		assertEquals(message("authentication"), database.exception(authentication, OTHER).getMessage());
		SQLException accountDisabled = new SQLException("Login failed for user 'scott'. Reason: The account is disabled.", "S0001", 18470);
		assertInstanceOf(AuthenticationException.class, database.exception(accountDisabled, OTHER));
		assertEquals(message("account_locked"), database.exception(accountDisabled, OTHER).getMessage());
		SQLException mustChange = new SQLException("Login failed for user 'scott'.  Reason: The password of the account must be changed.", "S0001", 18488);
		assertInstanceOf(AuthenticationException.class, database.exception(mustChange, OTHER));
		assertEquals(message("password_expired"), database.exception(mustChange, OTHER).getMessage());

		SQLException unknown = new SQLException("Incorrect syntax near 'selec'.", "S0001", 102);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.exception(unknown, SELECT).getMessage());
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}
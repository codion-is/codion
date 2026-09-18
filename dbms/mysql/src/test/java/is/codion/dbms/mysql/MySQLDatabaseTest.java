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
package is.codion.dbms.mysql;

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

public class MySQLDatabaseTest {

	private static final String URL = "jdbc:mysql://host:1234/sid";

	@Test
	void name() {
		MySQLDatabase database = new MySQLDatabase("jdbc:mysql://host.com:1234/dbname");
		assertEquals("dbname", database.name());
		database = new MySQLDatabase("jdbc:mysql://host.com:1234/dbname;option=true;option2=false");
		assertEquals("dbname", database.name());
	}

	@Test
	void sequenceQuery() {
		assertThrows(UnsupportedOperationException.class, () -> new MySQLDatabase(URL).sequenceQuery("seq"));
	}

	@Test
	void autoIncrementQuery() {
		MySQLDatabase db = new MySQLDatabase(URL);
		assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new MySQLDatabase(null));
	}

	@Test
	void limitOffsetClause() {
		MySQLDatabase database = new MySQLDatabase(URL);
		assertEquals("", database.limitOffsetClause(null, null, false));
		assertEquals("LIMIT 10", database.limitOffsetClause(10, null, false));
		assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(10, 5, true));
		assertEquals("LIMIT 18446744073709551615 OFFSET 5", database.limitOffsetClause(null, 5, false));
	}

	@Test
	void selectForUpdateClause() {
		assertEquals("FOR UPDATE NOWAIT", new MySQLDatabase(URL, true).selectForUpdateClause());
		assertEquals("FOR UPDATE", new MySQLDatabase(URL, false).selectForUpdateClause());
	}

	@Test
	void exceptions() {
		// codes, states and messages as reported by MySQL 8.4, Connector/J 9.1
		MySQLDatabase database = new MySQLDatabase(URL);
		SQLException unique = new SQLException("Duplicate entry 'A-b' for key 'parent_uk'", "23000", 1062);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint") + ": 'A-b'", database.errorMessage(unique, INSERT));

		SQLException parentMissing = new SQLException("Cannot add or update a child row: a foreign key constraint fails "
						+ "(`db`.`child`, CONSTRAINT `child_fk` FOREIGN KEY (`parent_id`) REFERENCES `parent` (`id`))", "23000", 1452);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
		assertEquals(message("parent_missing"), database.errorMessage(parentMissing, INSERT));
		// deleting a referenced row
		SQLException childExists = new SQLException("Cannot delete or update a parent row: a foreign key constraint fails "
						+ "(`db`.`child`, CONSTRAINT `child_fk` FOREIGN KEY (`parent_id`) REFERENCES `parent` (`id`))", "23000", 1451);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.errorMessage(childExists, DELETE));
		assertEquals(message("child_exists"), database.errorMessage(childExists, UPDATE));

		assertEquals(message("null_value") + ": name", database.errorMessage(
						new SQLException("Column 'name' cannot be null", "23000", 1048), INSERT));
		assertEquals(message("null_value") + ": name", database.errorMessage(
						new SQLException("Field 'name' doesn't have a default value", "HY000", 1364), INSERT));
		assertEquals(message("null_value"), database.errorMessage(new SQLException(null, "23000", 1048), INSERT));
		assertEquals(message("check_constraint"), database.errorMessage(new SQLException("Check constraint 'parent_ck' is violated.", "HY000", 3819), UPDATE));
		assertEquals(message("value_too_large") + ": name", database.errorMessage(
						new SQLException("Data too long for column 'name' at row 1", "22001", 1406), UPDATE));
		assertEquals(message("value_too_large") + ": amount", database.errorMessage(
						new SQLException("Out of range value for column 'amount' at row 1", "22003", 1264), UPDATE));
		assertEquals(message("table_not_found"), database.errorMessage(
						new SQLException("Table 'db.missing' doesn't exist", "42S02", 1146), SELECT));
		assertEquals(message("missing_privileges"), database.errorMessage(
						new SQLException("SELECT command denied to user 'scott'@'localhost' for table 'parent'", "42000", 1142), SELECT));
		// NOWAIT
		assertEquals(message("row_locked"), database.errorMessage(new SQLException(
						"Statement aborted because lock(s) could not be acquired immediately and NOWAIT is set.", "HY000", 3572), SELECT));
		assertEquals(message("row_locked"), database.errorMessage(
						new SQLException("Lock wait timeout exceeded; try restarting transaction", "HY000", 1205), SELECT));
		// the driver reports neither an error code nor a state for a query timeout
		SQLException timeout = new SQLTimeoutException("Statement cancelled due to timeout or client request");
		assertInstanceOf(QueryTimeoutException.class, database.exception(timeout, SELECT));
		assertEquals(message("timeout"), database.errorMessage(timeout, SELECT));
		assertInstanceOf(QueryTimeoutException.class, database.exception(
						new SQLException("Query execution was interrupted, maximum statement execution time exceeded", "HY000", 3024), SELECT));

		SQLException authentication = new SQLException("Access denied for user 'scott'@'172.17.0.1' (using password: YES)", "28000", 1045);
		assertTrue(database.isAuthenticationException(authentication));
		assertEquals(message("authentication"), database.errorMessage(authentication, OTHER));

		SQLException unknown = new SQLException("You have an error in your SQL syntax", "42000", 1064);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals("You have an error in your SQL syntax", database.errorMessage(unknown, SELECT));
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}
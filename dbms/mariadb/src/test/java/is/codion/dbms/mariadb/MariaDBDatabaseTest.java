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
package is.codion.dbms.mariadb;

import is.codion.common.db.exception.AuthenticationException;
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

public class MariaDBDatabaseTest {

	private static final String URL = "jdbc:mariadb://host:1234/sid";

	@Test
	void name() {
		MariaDBDatabase database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname");
		assertEquals("dbname", database.name());
		database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname;option=true;option2=false");
		assertEquals("dbname", database.name());
		assertEquals("dbname", new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname?useSSL=false").name());
		assertEquals("dbname", new MariaDBDatabase("jdbc:mariadb:replication://host1,host2/dbname").name());
		assertEquals("host.com:1234", new MariaDBDatabase("jdbc:mariadb://host.com:1234/").name());
		assertEquals("host.com", new MariaDBDatabase("jdbc:mariadb://host.com").name());
	}

	@Test
	void sequenceQuery() {
		assertEquals("SELECT NEXT VALUE FOR seq", new MariaDBDatabase(URL).sequenceQuery("seq"));
		assertThrows(NullPointerException.class, () -> new MariaDBDatabase(URL).sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		MariaDBDatabase db = new MariaDBDatabase(URL);
		assertEquals(MariaDBDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new MariaDBDatabase(null));
	}

	@Test
	void limitOffsetClause() {
		MariaDBDatabase database = new MariaDBDatabase(URL);
		assertEquals("", database.limitOffsetClause(null, null, false));
		assertEquals("LIMIT 10", database.limitOffsetClause(10, null, false));
		assertEquals("LIMIT 10 OFFSET 5", database.limitOffsetClause(10, 5, true));
		assertEquals("LIMIT 18446744073709551615 OFFSET 5", database.limitOffsetClause(null, 5, false));
	}

	@Test
	void selectForUpdateClause() {
		assertEquals("FOR UPDATE NOWAIT", new MariaDBDatabase(URL, true).selectForUpdateClause());
		assertEquals("FOR UPDATE", new MariaDBDatabase(URL, false).selectForUpdateClause());
	}

	@Test
	void exceptions() {
		// codes, states and messages as reported by MariaDB 11.8, driver 3.5.1, which prefixes each message with the connection id
		MariaDBDatabase database = new MariaDBDatabase(URL);
		SQLException unique = new SQLException("(conn=3) Duplicate entry 'A-b' for key 'parent_uk'", "23000", 1062);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint") + ": 'A-b'", database.exception(unique, INSERT).getMessage());

		SQLException parentMissing = new SQLException("(conn=3) Cannot add or update a child row: a foreign key constraint fails "
						+ "(`db`.`child`, CONSTRAINT `child_fk` FOREIGN KEY (`parent_id`) REFERENCES `parent` (`id`))", "23000", 1452);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
		assertEquals(message("parent_missing") + ": child_fk", database.exception(parentMissing, INSERT).getMessage());
		// deleting a referenced row
		SQLException childExists = new SQLException("(conn=3) Cannot delete or update a parent row: a foreign key constraint fails "
						+ "(`db`.`child`, CONSTRAINT `child_fk` FOREIGN KEY (`parent_id`) REFERENCES `parent` (`id`))", "23000", 1451);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists") + ": child_fk", database.exception(childExists, DELETE).getMessage());
		assertEquals(message("child_exists") + ": child_fk", database.exception(childExists, UPDATE).getMessage());

		assertEquals(message("null_value") + ": name", database.exception(
						new SQLException("(conn=3) Column 'name' cannot be null", "23000", 1048), INSERT).getMessage());
		assertEquals(message("null_value") + ": name", database.exception(
						new SQLException("(conn=3) Field 'name' doesn't have a default value", "HY000", 1364), INSERT).getMessage());
		assertEquals(message("null_value"), database.exception(new SQLException(null, "23000", 1048), INSERT).getMessage());
		assertEquals(message("check_constraint") + ": parent_ck", database.exception(new SQLException("(conn=3) CONSTRAINT `parent_ck` failed for `db`.`parent`", "23000", 4025), UPDATE).getMessage());
		assertEquals(message("value_too_large") + ": name", database.exception(
						new SQLException("(conn=3) Data too long for column 'name' at row 1", "22001", 1406), UPDATE).getMessage());
		assertEquals(message("value_too_large") + ": amount", database.exception(
						new SQLException("(conn=3) Out of range value for column 'amount' at row 1", "22003", 1264), UPDATE).getMessage());
		assertEquals(message("table_not_found"), database.exception(
						new SQLException("(conn=3) Table 'db.missing' doesn't exist", "42S02", 1146), SELECT).getMessage());
		assertEquals(message("missing_privileges"), database.exception(
						new SQLException("(conn=3) SELECT command denied to user 'scott'@'localhost' for table 'parent'", "42000", 1142), SELECT).getMessage());
		// NOWAIT
		assertEquals(message("row_locked"), database.exception(
						new SQLException("(conn=6) Lock wait timeout exceeded; try restarting transaction", "HY000", 1205), SELECT).getMessage());
		SQLException timeout = new SQLTimeoutException("(conn=3) Query execution was interrupted (max_statement_time exceeded)", "70100", 1969);
		assertInstanceOf(QueryTimeoutException.class, database.exception(timeout, SELECT));
		assertEquals(message("timeout"), database.exception(timeout, SELECT).getMessage());

		SQLException authentication = new SQLException("(conn=3) Access denied for user 'scott'@'172.17.0.1' (using password: YES)", "28000", 1045);
		assertInstanceOf(AuthenticationException.class, database.exception(authentication, OTHER));
		assertEquals(message("authentication"), database.exception(authentication, OTHER).getMessage());
		SQLException accountLocked = new SQLException("(conn=4) Access denied, this account is locked", "HY000", 4151);
		assertInstanceOf(AuthenticationException.class, database.exception(accountLocked, OTHER));
		assertEquals(message("account_locked"), database.exception(accountLocked, OTHER).getMessage());
		// logging in is allowed by default, the first statement failing
		SQLException passwordExpired = new SQLException("(conn=7) You must SET PASSWORD before executing this statement", "HY000", 1820);
		assertInstanceOf(AuthenticationException.class, database.exception(passwordExpired, OTHER));
		assertEquals(message("password_expired"), database.exception(passwordExpired, OTHER).getMessage());

		SQLException unknown = new SQLException("(conn=3) You have an error in your SQL syntax", "42000", 1064);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals("You have an error in your SQL syntax", database.exception(unknown, SELECT).getMessage());
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(DatabaseException.class.getName()).getString(key);
	}
}
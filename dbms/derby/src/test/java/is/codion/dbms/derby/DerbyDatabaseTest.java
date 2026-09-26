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

import is.codion.common.db.database.SetValue;
import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.db.exception.Operation.*;
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
	void temporalValuesViaSqlTypes() throws SQLException {
		// Derby does not support java.time
		DerbyDatabase database = new DerbyDatabase(URL);
		List<String> calls = new ArrayList<>();
		PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {PreparedStatement.class}, (proxy, method, arguments) -> {
							calls.add(method.getName() + "(" + arguments[0] + ", " + arguments[1] + ")");
							return null;
						});
		((SetValue<LocalDate>) database.setter(Types.DATE)).set(statement, 1, LocalDate.of(2026, 9, 26));
		((SetValue<LocalTime>) database.setter(Types.TIME)).set(statement, 2, LocalTime.of(13, 45, 30));
		((SetValue<LocalDateTime>) database.setter(Types.TIMESTAMP)).set(statement, 3, LocalDateTime.of(2026, 9, 26, 13, 45, 30, 123_456_000));
		database.setter(Types.DATE).set(statement, 4, null);
		((SetValue<Integer>) database.setter(Types.INTEGER)).set(statement, 5, 1);
		assertEquals(List.of("setDate(1, 2026-09-26)", "setTime(2, 13:45:30)", "setTimestamp(3, 2026-09-26 13:45:30.123456)",
						"setDate(4, null)", "setInt(5, 1)"), calls);

		ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {ResultSet.class}, (proxy, method, arguments) -> switch (method.getName()) {
							case "getDate" -> (int) arguments[0] == 1 ? Date.valueOf("2026-09-26") : null;
							case "getTime" -> Time.valueOf("13:45:30");
							case "getTimestamp" -> Timestamp.valueOf("2026-09-26 13:45:30.123456");
							default -> throw new UnsupportedOperationException(method.getName());
						});
		assertEquals(LocalDate.of(2026, 9, 26), database.getter(Types.DATE).get(resultSet, 1));
		assertNull(database.getter(Types.DATE).get(resultSet, 2));
		assertEquals(LocalTime.of(13, 45, 30), database.getter(Types.TIME).get(resultSet, 1));
		assertEquals(LocalDateTime.of(2026, 9, 26, 13, 45, 30, 123_456_000), database.getter(Types.TIMESTAMP).get(resultSet, 1));
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
		assertEquals(message("unique_constraint") + ": PARENT_UK", database.exception(unique, INSERT).getMessage());
		// which way is not reported, the operation deciding
		SQLException parentMissing = new SQLException("INSERT on table 'CHILD' caused a violation of foreign key constraint 'CHILD_FK' for key (99).  "
						+ "The statement has been rolled back.", "23503", 30000);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
		assertEquals(message("parent_missing") + ": CHILD_FK", database.exception(parentMissing, INSERT).getMessage());
		SQLException childExists = new SQLException("DELETE on table 'PARENT' caused a violation of foreign key constraint 'CHILD_FK' for key (1).  "
						+ "The statement has been rolled back.", "23503", 30000);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists") + ": CHILD_FK", database.exception(childExists, DELETE).getMessage());
		assertEquals(message("referential_integrity") + ": CHILD_FK", database.exception(childExists, UPDATE).getMessage());
		assertEquals(message("null_value") + ": NAME", database.exception(new SQLException("Column 'NAME'  cannot accept a NULL value.", "23502", 30000), INSERT).getMessage());
		assertEquals(message("null_value"), database.exception(new SQLException(null, "23502", 30000), INSERT).getMessage());
		assertEquals(message("check_constraint") + ": PARENT_CK", database.exception(new SQLException(
						"The check constraint 'PARENT_CK' was violated while performing an INSERT or UPDATE on table '\"APP\".\"PARENT\"'.", "23513", 30000), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(new SQLException(
						"A truncation error was encountered trying to shrink VARCHAR 'abcdefghijklmnop' to length 10.", "22001", 30000), UPDATE).getMessage());
		assertEquals(message("table_not_found"), database.exception(new SQLException("Table/View 'MISSING' does not exist.", "42X05", 30000), SELECT).getMessage());
		assertEquals(message("row_locked"), database.exception(new SQLException("A lock could not be obtained within the time requested", "40XL1", 30000), UPDATE).getMessage());
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLException("The statement has been cancelled or timed out.", "XCL52", 30000), SELECT));
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), SELECT));
		assertInstanceOf(AuthenticationException.class, database.exception(new SQLException("Connection authentication failure occurred.  Reason: Invalid authentication..", "08004", 40000), OTHER));
		SQLException unknown = new SQLException("Syntax error: Encountered \"selec\" at line 1, column 1.", "42X01", 30000);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.exception(unknown, SELECT).getMessage());
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(DatabaseException.class.getName()).getString(key);
	}
}

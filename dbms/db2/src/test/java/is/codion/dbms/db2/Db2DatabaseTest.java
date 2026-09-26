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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.db2;

import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

import static is.codion.common.db.exception.Operation.*;
import static org.junit.jupiter.api.Assertions.*;

public class Db2DatabaseTest {

	private static final String URL = "jdbc:db2://server:6789/database";

	@Test
	void name() {
		Db2Database database = new Db2Database(URL);
		assertEquals("database", database.name());
		database = new Db2Database(URL + ";options");
		assertEquals("database", database.name());
		assertEquals("database", new Db2Database("jdbc:db2://server:6789/database:user=scott;password=tiger;").name());
		assertEquals("database", new Db2Database("jdbc:db2://server:6789/database:retrieveMessagesFromServerOnGetMessage=true;").name());
		assertEquals("database", new Db2Database("jdbc:db2:database").name());
	}

	@Test
	void autoIncrementQuery() {
		Db2Database database = new Db2Database("test");
		assertEquals("VALUES PREVIOUS VALUE FOR seq", database.autoIncrementQuery("seq"));
	}

	@Test
	void sequenceSQLNullSequence() {
		assertThrows(NullPointerException.class, () -> new Db2Database(URL).sequenceQuery(null));
	}

	@Test
	void sequenceQuery() {
		assertEquals("VALUES NEXT VALUE FOR seq", new Db2Database(URL).sequenceQuery("seq"));
	}

	@Test
	void autoIncrementQueryNullIdSource() {
		assertThrows(NullPointerException.class, () -> new Db2Database(URL).autoIncrementQuery(null));
	}

	@Test
	void temporalValuesViaSqlTypes() throws SQLException {
		// drivers before 12 throw a NullPointerException from getObject(index, LocalDate.class) for a null value
		Db2Database database = new Db2Database(URL);
		ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {ResultSet.class}, (proxy, method, arguments) -> {
							if ((int) arguments[0] == 2) {
								return null;
							}
							switch (method.getName()) {
								case "getDate":
									return Date.valueOf("2026-09-26");
								case "getTime":
									return Time.valueOf("13:45:30");
								case "getTimestamp":
									return Timestamp.valueOf("2026-09-26 13:45:30.123456");
								default:
									throw new UnsupportedOperationException(method.getName());
							}
						});
		assertEquals(LocalDate.of(2026, 9, 26), database.getter(Types.DATE).get(resultSet, 1));
		assertEquals(LocalTime.of(13, 45, 30), database.getter(Types.TIME).get(resultSet, 1));
		assertEquals(LocalDateTime.of(2026, 9, 26, 13, 45, 30, 123_456_000), database.getter(Types.TIMESTAMP).get(resultSet, 1));
		assertNull(database.getter(Types.DATE).get(resultSet, 2));
		assertNull(database.getter(Types.TIME).get(resultSet, 2));
		assertNull(database.getter(Types.TIMESTAMP).get(resultSet, 2));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new Db2Database(null));
	}

	@Test
	void limitOffsetClause() {
		Db2Database database = new Db2Database(URL);
		assertEquals("", database.limitOffsetClause(null, null, false));
		assertEquals("FETCH NEXT 10 ROWS ONLY", database.limitOffsetClause(10, null, false));
		assertEquals("OFFSET 5 ROWS", database.limitOffsetClause(null, 5, false));
		assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", database.limitOffsetClause(10, 5, true));
	}

	@Test
	void exceptions() {
		// codes and states as reported by Db2 11.5.8, JCC 4.33
		Db2Database database = new Db2Database(URL);
		SQLException unique = exception(-803, "23505", "2;DB2INST1.PARENT");
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint"), database.exception(unique, INSERT).getMessage());

		SQLException parentMissing = exception(-530, "23503", "DB2INST1.CHILD.CHILD_FK");
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, UPDATE));
		assertEquals(message("parent_missing") + ": CHILD_FK", database.exception(parentMissing, UPDATE).getMessage());
		SQLException childExists = exception(-532, "23504", "DB2INST1.CHILD.CHILD_FK");
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists") + ": CHILD_FK", database.exception(childExists, DELETE).getMessage());
		// updating a referenced key
		SQLException referencedKey = exception(-531, "23504", "DB2INST1.CHILD.CHILD_FK");
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(referencedKey, UPDATE));
		assertEquals(message("child_exists") + ": CHILD_FK", database.exception(referencedKey, UPDATE).getMessage());

		assertEquals(message("null_value"), database.exception(exception(-407, "23502", "TBSPACEID=2, TABLEID=4, COLNO=1"), INSERT).getMessage());
		assertEquals(message("check_constraint") + ": PARENT_CK", database.exception(exception(-545, "23513", "DB2INST1.PARENT.PARENT_CK"), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(exception(-433, "22001", "abcdefghijklmnop"), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(exception(-413, "22003", "null"), UPDATE).getMessage());
		assertEquals(message("table_not_found"), database.exception(exception(-204, "42704", "DB2INST1.MISSING"), SELECT).getMessage());
		assertEquals(message("missing_privileges"), database.exception(exception(-551, "42501", "SCOTT;SELECT;DB2INST1.PARENT"), SELECT).getMessage());
		// a lock timeout or deadlock, no longer a query timeout
		assertSame(DatabaseException.class, database.exception(exception(-911, "40001", "68"), UPDATE).getClass());
		assertEquals(message("row_locked"), database.exception(exception(-911, "40001", "68"), UPDATE).getMessage());
		assertEquals(message("row_locked"), database.exception(exception(-913, "57033", "68"), UPDATE).getMessage());
		assertInstanceOf(QueryTimeoutException.class, database.exception(exception(-952, "57014", "null"), SELECT));
		assertInstanceOf(QueryTimeoutException.class, database.exception(new SQLTimeoutException("timeout"), SELECT));

		SQLException authentication = new SQLException("[jcc][t4][2013][11249][4.33.31] Connection authorization failure occurred.  "
						+ "Reason: User ID or Password invalid. ERRORCODE=-4214, SQLSTATE=28000", "28000", -4214);
		assertInstanceOf(AuthenticationException.class, database.exception(authentication, OTHER));
		assertEquals(message("authentication"), database.exception(authentication, OTHER).getMessage());

		SQLException unknown = exception(-104, "42601", "END-OF-STATEMENT");
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals(unknown.getMessage(), database.exception(unknown, SELECT).getMessage());
	}

	private static SQLException exception(int sqlCode, String sqlState, String tokens) {
		return new SQLException("DB2 SQL Error: SQLCODE=" + sqlCode + ", SQLSTATE=" + sqlState + ", SQLERRMC=" + tokens + ", DRIVER=4.33.31", sqlState, sqlCode);
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(DatabaseException.class.getName()).getString(key);
	}
}

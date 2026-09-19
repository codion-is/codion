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
package is.codion.dbms.oracle;

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

import static is.codion.common.db.exception.Operation.*;
import static org.junit.jupiter.api.Assertions.*;

public class OracleDatabaseTest {

	public static final String URL = "jdbc:oracle:thin:@host:1234:sid";

	@Test
	void name() {
		OracleDatabase database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234:sid");
		assertEquals("sid", database.name());
		database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234:sid;option=true;option2=false");
		assertEquals("sid", database.name());
		database = new OracleDatabase("jdbc:oracle:thin:@host.com:1234/sid;option=true;option2=false");
		assertEquals("sid", database.name());
		database = new OracleDatabase("jdbc:oracle:thin:/@sid");
		assertEquals("sid", database.name());
		assertEquals("service", new OracleDatabase("jdbc:oracle:thin:@//host.com:1234/service").name());
		assertEquals("service", new OracleDatabase("jdbc:oracle:thin:@host.com:1234/service:dedicated").name());
		assertEquals("service", new OracleDatabase("jdbc:oracle:thin:@tcps://host.com:1234/service?wallet_location=/path/to/wallet").name());
		assertEquals("alias", new OracleDatabase("jdbc:oracle:thin:@alias").name());
		assertEquals("alias", new OracleDatabase("jdbc:oracle:oci:@alias").name());
		assertEquals("sid", new OracleDatabase("jdbc:oracle:thin:scott/tiger@host.com:1234:sid").name());
		assertEquals("service", new OracleDatabase("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=host.com)(PORT=1234))(CONNECT_DATA=(SERVICE_NAME=service)))").name());
		assertEquals("sid", new OracleDatabase("jdbc:oracle:thin:@(description=(address=(protocol=tcp)(host=host.com)(port=1234))(connect_data=(sid = sid)))").name());
	}

	@Test
	void sequenceSQLNullSequence() {
		assertThrows(NullPointerException.class, () -> new OracleDatabase(URL).sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		OracleDatabase db = new OracleDatabase(URL);
		assertEquals("SELECT seq.CURRVAL FROM DUAL", db.autoIncrementQuery("seq"));
	}

	@Test
	void sequenceQuery() {
		OracleDatabase db = new OracleDatabase(URL);
		assertEquals("SELECT seq.NEXTVAL FROM DUAL", db.sequenceQuery("seq"));
	}

	@Test
	void url() {
		OracleDatabase db = new OracleDatabase(URL);
		assertEquals("jdbc:oracle:thin:@host:1234:sid", db.url());
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new OracleDatabase(null));
	}

	@Test
	void maximumParameters() {
		assertEquals(65_535, new OracleDatabase(URL).maximumParameters());
	}

	@Test
	void exceptions() {
		// codes, states and messages as reported by Oracle 21.3, ojdbc17 23.26
		OracleDatabase database = new OracleDatabase(URL);
		SQLException unique = exception("ORA-00001: unique constraint (SCOTT.PARENT_UK) violated", "23000", 1);
		assertInstanceOf(UniqueConstraintException.class, database.exception(unique, INSERT));
		assertEquals(message("unique_constraint"), database.exception(unique, INSERT).getMessage());

		SQLException parentMissing = exception("ORA-02291: integrity constraint (SCOTT.CHILD_FK) violated - parent key not found", "23000", 2291);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(parentMissing, INSERT));
		assertEquals(message("parent_missing"), database.exception(parentMissing, INSERT).getMessage());

		SQLException childExists = exception("ORA-02292: integrity constraint (SCOTT.CHILD_FK) violated - child record found", "23000", 2292);
		assertInstanceOf(ReferentialIntegrityException.class, database.exception(childExists, DELETE));
		assertEquals(message("child_exists"), database.exception(childExists, DELETE).getMessage());
		assertEquals(message("child_exists"), database.exception(childExists, UPDATE).getMessage());

		assertEquals(message("null_value") + ": NAME", database.exception(
						exception("ORA-01400: cannot insert NULL into (\"SCOTT\".\"PARENT\".\"NAME\")", "23000", 1400), INSERT).getMessage());
		assertEquals(message("null_value") + ": NAME", database.exception(
						exception("ORA-01407: cannot update (\"SCOTT\".\"PARENT\".\"NAME\") to NULL", "72000", 1407), UPDATE).getMessage());
		// an unexpected or missing message must not throw, replacing the actual exception
		assertEquals(message("null_value"), database.exception(new SQLException("ORA-01400: unexpected", "23000", 1400), INSERT).getMessage());
		assertEquals(message("null_value"), database.exception(new SQLException(null, "23000", 1400), INSERT).getMessage());

		assertEquals(message("value_too_large") + ": NAME", database.exception(
						exception("ORA-12899: value too large for column \"SCOTT\".\"PARENT\".\"NAME\" (actual: 16, maximum: 10)", "72000", 12899), UPDATE).getMessage());
		assertEquals(message("value_too_large"), database.exception(
						exception("ORA-01438: value larger than specified precision allowed for this column", "22003", 1438), UPDATE).getMessage());
		assertEquals(message("check_constraint"), database.exception(
						exception("ORA-02290: check constraint (SCOTT.PARENT_CK) violated", "23000", 2290), UPDATE).getMessage());
		assertEquals(message("missing_privileges"), database.exception(exception("ORA-01031: insufficient privileges", "42000", 1031), SELECT).getMessage());
		assertEquals(message("missing_privileges"), database.exception(
						exception("ORA-01045: user SCOTT lacks CREATE SESSION privilege; logon denied", "72000", 1045), OTHER).getMessage());
		assertEquals(message("table_not_found"), database.exception(exception("ORA-00942: table or view does not exist", "42000", 942), SELECT).getMessage());
		// not recognized, an Oracle only error
		assertEquals("ORA-04063: view \"SCOTT.V\" has errors", database.exception(exception("ORA-04063: view \"SCOTT.V\" has errors", "72000", 4063), SELECT).getMessage());
		assertEquals(message("row_locked"), database.exception(
						exception("ORA-00054: resource busy and acquire with NOWAIT specified or timeout expired", "61000", 54), SELECT).getMessage());

		SQLException timeout = new SQLTimeoutException("ORA-01013: user requested cancel of current operation", "72000", 1013);
		assertInstanceOf(QueryTimeoutException.class, database.exception(timeout, SELECT));
		assertEquals(message("timeout"), database.exception(timeout, SELECT).getMessage());
		// cancelled, not timed out
		assertSame(DatabaseException.class, database.exception(exception("ORA-01013: user requested cancel of current operation", "72000", 1013), SELECT).getClass());

		SQLException authentication = exception("ORA-01017: invalid username/password; logon denied", "72000", 1017);
		assertInstanceOf(AuthenticationException.class, database.exception(authentication, OTHER));
		assertEquals(message("authentication"), database.exception(authentication, OTHER).getMessage());
		SQLException accountLocked = exception("ORA-28000: The account is locked.", "99999", 28000);
		assertInstanceOf(AuthenticationException.class, database.exception(accountLocked, OTHER));
		assertEquals(message("account_locked"), database.exception(accountLocked, OTHER).getMessage());
		SQLException passwordExpired = exception("ORA-28001: the password has expired", "99999", 28001);
		assertInstanceOf(AuthenticationException.class, database.exception(passwordExpired, OTHER));
		assertEquals(message("password_expired"), database.exception(passwordExpired, OTHER).getMessage());

		// unrecognized, without the link to the documentation
		SQLException unknown = exception("ORA-00904: \"NMAE\": invalid identifier", "42000", 904);
		assertSame(DatabaseException.class, database.exception(unknown, SELECT).getClass());
		assertEquals("ORA-00904: \"NMAE\": invalid identifier", database.exception(unknown, SELECT).getMessage());
		assertEquals("no link", database.exception(new SQLException("no link", "72000", 904), SELECT).getMessage());
	}

	/**
	 * The driver appends a link to the documentation to each message
	 */
	private static SQLException exception(String message, String sqlState, int errorCode) {
		String code = message.substring(0, message.indexOf(':')).toLowerCase();

		return new SQLException(message + "\n\nhttps://docs.oracle.com/error-help/db/" + code + "/", sqlState, errorCode);
	}

	// independent of the default locale
	private static String message(String key) {
		return ResourceBundle.getBundle(Database.class.getName()).getString(key);
	}
}
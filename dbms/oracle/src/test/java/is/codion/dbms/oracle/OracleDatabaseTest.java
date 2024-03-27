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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.dbms.oracle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
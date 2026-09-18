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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
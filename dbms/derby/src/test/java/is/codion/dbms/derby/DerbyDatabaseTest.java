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

import org.junit.jupiter.api.Test;

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
}

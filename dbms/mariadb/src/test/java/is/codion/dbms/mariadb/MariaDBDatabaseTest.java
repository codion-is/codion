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
package is.codion.dbms.mariadb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MariaDBDatabaseTest {

	private static final String URL = "jdbc:mariadb://host:1234/sid";

	@Test
	void name() {
		MariaDBDatabase database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname");
		assertEquals("dbname", database.name());
		database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname;option=true;option2=false");
		assertEquals("dbname", database.name());
	}

	@Test
	void sequenceQuery() {
		assertThrows(UnsupportedOperationException.class, () -> new MariaDBDatabase(URL).sequenceQuery("seq"));
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
}
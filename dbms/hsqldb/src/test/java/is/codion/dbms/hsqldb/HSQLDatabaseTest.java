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
package is.codion.dbms.hsqldb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HSQLDatabaseTest {

	private static final String URL = "jdbc:hsqldb:hsql//host:1234/sid";

	@Test
	void name() {
		HSQLDatabase database = new HSQLDatabase("jdbc:hsqldb:file:C:/data/sample;option=true;option2=false");
		assertEquals("C:/data/sample", database.name());
		database = new HSQLDatabase("jdbc:hsqldb:mem:sampleDb;option=true;option2=false");
		assertEquals("sampleDb", database.name());
		database = new HSQLDatabase("jdbc:hsqldb:mem:");
		assertEquals("private", database.name());
		database = new HSQLDatabase("jdbc:hsqldb:res:/dir/db");
		assertEquals("/dir/db", database.name());
	}

	@Test
	void sequenceSQLNullSequence() {
		assertThrows(NullPointerException.class, () -> new HSQLDatabase(URL).sequenceQuery(null));
	}

	@Test
	void autoIncrementQuery() {
		HSQLDatabase db = new HSQLDatabase(URL);
		assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
	}

	@Test
	void sequenceQuery() {
		HSQLDatabase db = new HSQLDatabase(URL);
		final String idSource = "seq";
		assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.sequenceQuery(idSource));
	}

	@Test
	void constructorNullUrl() {
		assertThrows(NullPointerException.class, () -> new HSQLDatabase(null));
	}
}
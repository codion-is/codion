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
package is.codion.dbms.derby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    assertThrows(UnsupportedOperationException.class, () -> new DerbyDatabase(URL).sequenceQuery("seq"));
  }

  @Test
  void supportsNoWait() {
    DerbyDatabase db = new DerbyDatabase(URL);
    assertEquals("FOR UPDATE", db.selectForUpdateClause());
  }

  @Test
  void autoIncrementQuery() {
    DerbyDatabase db = new DerbyDatabase(URL);
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.autoIncrementQuery(idSource));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new DerbyDatabase(null));
  }
}

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
package is.codion.common.db.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatabaseExceptionTest {

  @Test
  void test() {
    assertThrows(DatabaseException.class, () -> {
      throw new DatabaseException("hello");
    });
    DatabaseException dbException = new DatabaseException("hello", "statement");
    assertEquals("statement", dbException.statement());
    dbException = new DatabaseException(new SQLException(), "message", "statement");
    assertEquals("message", dbException.getMessage());
    assertEquals("statement", dbException.statement());
    dbException = new DatabaseException(null, "test", "stmt");
    assertEquals(-1, dbException.errorCode());
  }
}

/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatabaseExceptionTest {

  @Test
  public void test() {
    assertThrows(DatabaseException.class, () -> {
      throw new DatabaseException("hello");
    });
    DatabaseException dbException = new DatabaseException("hello", "statement");
    assertEquals("statement", dbException.getStatement());
    dbException = new DatabaseException(new SQLException(), "message", "statement");
    assertEquals("message", dbException.getMessage());
    assertEquals("statement", dbException.getStatement());
    dbException = new DatabaseException(null, "test", "stmt");
    assertEquals(-1, dbException.getErrorCode());
  }
}

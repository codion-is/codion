package org.jminor.common.db.exception;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.sql.SQLException;

public class DatabaseExceptionTest {

  @Test
  public void test() {
    new DatabaseException("hello");
    final DatabaseException dbException = new DatabaseException("hello", "statement");
    assertEquals("statement", dbException.getStatement());
    new DatabaseException(new SQLException(), "statement", "message");
    assertEquals("statement", dbException.getStatement());
  }
}

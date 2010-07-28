package org.jminor.common.db.exception;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.sql.SQLException;

public class DbExceptionTest {

  @Test
  public void test() {
    new DbException("hello");
    final DbException dbException = new DbException("hello", "statement");
    assertEquals("statement", dbException.getStatement());
    new DbException(new SQLException(), "statement", "message");
    assertEquals("statement", dbException.getStatement());
  }
}

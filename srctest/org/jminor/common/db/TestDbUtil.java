package org.jminor.common.db;

import junit.framework.TestCase;

public class TestDbUtil extends TestCase {

  public TestDbUtil() {
    super("TestDbUtil");
  }

  public void testGenerateSelectSql() throws Exception {
    final String generated = DbUtil.generateSelectSql("table", "col, col2", "where col = 1", "col2");
    assertEquals("Generate select should be working", "select col, col2 from table where col = 1 order by col2", generated);
  }
}

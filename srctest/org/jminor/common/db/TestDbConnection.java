package org.jminor.common.db;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.List;

public class TestDbConnection extends TestCase {

  public TestDbConnection() {
    super("TestDbConnection");
  }

  public void testQueryObjects() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("scott", "tiger"));
      final List<List> result = dbConnection.queryObjects("select deptno, dname, loc from scott.dept", 1);
      assertTrue(result.size() == 1);
      final List row = result.get(0);
      assertEquals(row.size(), 3);
      assertTrue(row.get(0).getClass().equals(BigDecimal.class));
      assertTrue(row.get(1).getClass().equals(String.class));
      assertTrue(row.get(2).getClass().equals(String.class));
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }
}

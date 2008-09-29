package org.jminor.common.db;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This test relies on the emp/dept schema
 */
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

  public void testQuery() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("scott", "tiger"));
      final List ret = dbConnection.query("select deptno, dname, loc from scott.dept", new IResultPacker() {
        public List pack(final ResultSet resultSet, final int recordCount) throws SQLException {
          final List<List> ret = new ArrayList<List>();
          int counter = 0;
          while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
            final List<Object> row = new ArrayList<Object>();
            row.add(resultSet.getInt(1));
            row.add(resultSet.getString(2));
            row.add(resultSet.getString(3));
            ret.add(row);
          }
          return ret;
        }
      }, 1);
      assertEquals("Result from query should include one row", 1, ret.size());
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  public void testQueryInt() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("scott", "tiger"));
      final Integer ret = dbConnection.queryInteger("select deptno from scott.dept");
      assertNotNull("queryInteger should return a value", ret);
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  public void testQueryIntegers() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("scott", "tiger"));
      final List<Integer> ret = dbConnection.queryIntegers("select deptno from scott.dept");
      assertTrue("queryIntegers should return a value", ret.size() > 0);
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  public void testQueryStrings() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("scott", "tiger"));
      final List<String> ret = dbConnection.queryStrings("select dname from scott.dept");
      assertTrue("queryStrings should return a value", ret.size() > 0);
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }
}

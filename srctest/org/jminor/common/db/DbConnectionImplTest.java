/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.User;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This test relies on the emp/dept schema
 */
public class DbConnectionImplTest {

  private static final Database DATABASE = DatabaseProvider.createInstance();
  private DbConnectionImpl dbConnection;

  /*public void testBlob() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("ops$darri", ""));
      dbConnection.execute("delete blob_test");
      dbConnection.execute("insert into blob_test(id) values (1)");

      File file = new File("/home/stofa/darri/data/downloads/NavigableImagePanel.zip");

      dbConnection.writeBlobField(Util.getBytesFromFile(file), "blob_test", "data", "where id = 1");

      final byte[] data = dbConnection.readBlobField("blob_test", "data", "where id = 1");

      File file2 = new File("/home/stofa/darri/data/downloads/NavigableImagePanel2.zip");
      file2.createNewFile();

      FileOutputStream stream = new FileOutputStream(file2);
      stream.write(data);
      stream.flush();
      stream.close();

      dbConnection.rollback();
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }*/

  @Before
  public void before() throws Exception {
    dbConnection = new DbConnectionImpl(DATABASE, User.UNIT_TEST_USER);
    dbConnection.setLoggingEnabled(true);
  }

  @After
  public void after() {
    try {
      if (dbConnection != null) {
        dbConnection.disconnect();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test() throws Exception {
    dbConnection.toString();
    assertEquals(dbConnection.getUser(), User.UNIT_TEST_USER);
    assertTrue(dbConnection.isLoggingEnabled());
    assertEquals(DATABASE, dbConnection.getDatabase());
    dbConnection.setPoolTime(10);
    assertEquals(10, dbConnection.getPoolTime());
    dbConnection.setRetryCount(2);
    assertEquals(2, dbConnection.getRetryCount());
    DbConnectionImpl.getDatabaseStatistics();
  }

  @Test
  public void testLogging() throws Exception {
    dbConnection.queryInteger("select deptno from scott.dept where deptno = 20");
    assertEquals("query", dbConnection.getMethodLogger().getLastAccessedMethod());
    assertTrue(dbConnection.getLogEntries().size() > 0);
  }

  @Test
  public void disconnect() throws Exception {
    dbConnection.disconnect();
    assertFalse(dbConnection.isConnected());
  }

  @Test
  public void getConnection() throws Exception {
    assertNotNull(dbConnection.getConnection());
  }

  @Test
  public void execute() throws Exception {
    try {
      dbConnection.execute("insert into scott.dept(deptno, dname, loc) values(42, 'name', 'loc')");
      dbConnection.execute(Arrays.asList(
            "update scott.dept set dname = 'newname' where deptno = 42",
            "update scott.dept set loc = 'newloc' where deptno = 42"));
      dbConnection.execute(Arrays.asList("update scott.dept set dname = 'aname' where deptno = 42"));
    }
    finally {
      dbConnection.rollback();
    }
    try {
      try {
        dbConnection.execute("insert into scott.dept(deptnoNO, dname, loc) values(42, 'name', 'loc')");
        fail();
      }
      catch (SQLException e) {}
      try {
        dbConnection.execute(Arrays.asList(
            "update scott.dept set dname = 'newname' where deptno = 42",
              "update scott.dept set locII = 'newloc' where deptno = 42"));
        fail();
      }
      catch (SQLException e) {}
    }
    finally {
      dbConnection.rollback();
    }
  }

  @Test
  public void executeCallableStatement() throws Exception {
    dbConnection.executeCallableStatement("select 1 from dual", -1);
  }

  @Test
  public void queryObjects() throws Exception {
    final List<List> result = dbConnection.queryObjects("select deptno, dname, loc from scott.dept", 1);
    assertTrue(result.size() == 1);
    final List row = result.get(0);
    assertEquals(row.size(), 3);
    assertSame(Integer.class, row.get(0).getClass());
    assertSame(String.class, row.get(1).getClass());
    assertSame(String.class, row.get(2).getClass());
  }

  @Test
  public void query() throws Exception {
    dbConnection = new DbConnectionImpl(DATABASE, User.UNIT_TEST_USER);
    final List ret = dbConnection.query("select deptno, dname, loc from scott.dept", new ResultPacker() {
      public List pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
        final List<List> result = new ArrayList<List>();
        int counter = 0;
        while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
          final List<Object> row = new ArrayList<Object>();
          row.add(resultSet.getInt(1));
          row.add(resultSet.getString(2));
          row.add(resultSet.getString(3));
          result.add(row);
        }
        return result;
      }
    }, 1);
    assertEquals("Result from query should include one row", 1, ret.size());
  }

  @Test
  public void queryInt() throws Exception {
    dbConnection = new DbConnectionImpl(DATABASE, User.UNIT_TEST_USER);
    final Integer ret = dbConnection.queryInteger("select deptno from scott.dept");
    assertNotNull("queryInteger should return a value", ret);
  }

  @Test
  public void queryIntegers() throws Exception {
    dbConnection = new DbConnectionImpl(DATABASE, User.UNIT_TEST_USER);
    final List<Integer> ret = dbConnection.queryIntegers("select deptno from scott.dept");
    assertTrue("queryIntegers should return a value", ret.size() > 0);
  }

  @Test
  public void queryStrings() throws Exception {
    dbConnection = new DbConnectionImpl(DATABASE, User.UNIT_TEST_USER);
    final List<String> ret = dbConnection.queryStrings("select dname from scott.dept");
    assertTrue("queryStrings should return a value", ret.size() > 0);
  }

  @Test
  public void beginTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    try {
      dbConnection.beginTransaction();
      fail("IllegalStateException should have been thrown");
    }
    catch (IllegalStateException e) {}
    try {
      dbConnection.commit();
      fail("IllegalStateException should have been thrown");
    }
    catch (IllegalStateException e) {}
    try {
      dbConnection.rollback();
      fail("IllegalStateException should have been thrown");
    }
    catch (IllegalStateException e) {}
  }

  @Test
  public void commitTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    dbConnection.commitTransaction();
    assertFalse(dbConnection.isTransactionOpen());
    try {
      dbConnection.commitTransaction();
      fail("IllegalStateException should have been thrown");
    }
    catch (IllegalStateException e) {}
  }

  @Test
  public void rollbackTransaction() throws Exception {
    dbConnection.beginTransaction();
    assertTrue(dbConnection.isTransactionOpen());
    dbConnection.rollbackTransaction();
    assertFalse(dbConnection.isTransactionOpen());
    try {
      dbConnection.rollbackTransaction();
      fail("IllegalStateException should have been thrown");
    }
    catch (IllegalStateException e) {}
  }

  @Test
  public void commit() throws Exception {
    dbConnection.execute("update scott.dept set dname = dname where deptno = 20");
    dbConnection.commit();
  }

  @Test
  public void rollback() throws Exception {
    dbConnection.execute("update scott.dept set dname = dname where deptno = 20");
    dbConnection.rollback();
  }
}

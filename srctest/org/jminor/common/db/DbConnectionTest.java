/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This test relies on the emp/dept schema
 */
public class DbConnectionTest {

  private final Database database = DatabaseProvider.createInstance();

  /*public void testBlob() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(new User("ops$darri", ""));
      dbConnection.execute("delete blob_test");
      dbConnection.execute("insert into blob_test(id) values (1)");

      File file = new File("/home/stofa/darri/data/downloads/NavigableImagePanel.zip");

      dbConnection.writeBlobField(DbUtil.getBytesFromFile(file), "blob_test", "data", "where id = 1");

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

  @Test
  public void queryObjects() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(database, new User("scott", "tiger"));
      final List<List> result = dbConnection.queryObjects("select deptno, dname, loc from scott.dept", 1);
      assertTrue(result.size() == 1);
      final List row = result.get(0);
      assertEquals(row.size(), 3);
      assertEquals(Integer.class, row.get(0).getClass());
      assertEquals(String.class, row.get(1).getClass());
      assertEquals(String.class, row.get(2).getClass());
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  @Test
  public void query() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(database, new User("scott", "tiger"));
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
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  @Test
  public void queryInt() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(database, new User("scott", "tiger"));
      final Integer ret = dbConnection.queryInteger("select deptno from scott.dept");
      assertNotNull("queryInteger should return a value", ret);
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  @Test
  public void queryIntegers() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(database, new User("scott", "tiger"));
      final List<Integer> ret = dbConnection.queryIntegers("select deptno from scott.dept");
      assertTrue("queryIntegers should return a value", ret.size() > 0);
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }

  @Test
  public void queryStrings() throws Exception {
    DbConnection dbConnection = null;
    try {
      dbConnection = new DbConnection(database, new User("scott", "tiger"));
      final List<String> ret = dbConnection.queryStrings("select dname from scott.dept");
      assertTrue("queryStrings should return a value", ret.size() > 0);
    }
    finally {
      if (dbConnection != null)
        dbConnection.disconnect();
    }
  }
}

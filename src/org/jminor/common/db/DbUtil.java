/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A static utility class
 */
public class DbUtil {

  public static byte[] getBytesFromFile(final File file) throws IOException {
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);

      // Get the size of the file
      final long length = file.length();

      // Create the byte array to hold the data
      final byte[] bytes = new byte[(int)length];

      // Read in the bytes
      int offset = 0;
      int numRead;
      while (offset < bytes.length && (numRead = inputStream.read(bytes, offset, bytes.length-offset)) >= 0)
        offset += numRead;

      // Ensure all the bytes have been read in
      if (offset < bytes.length)
        throw new IOException("Could not completely read file "+file.getName());

      return bytes;
    }
    finally {
      try {
        if (inputStream != null)
          inputStream.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static final ResultPacker<Integer> INT_PACKER = new ResultPacker<Integer>() {
    public List<Integer> pack(final ResultSet rs, final int fetchCount) throws SQLException {
      final List<Integer> integers = new ArrayList<Integer>();
      int counter = 0;
      while (rs.next() && (fetchCount < 0 || counter++ < fetchCount))
        integers.add(rs.getInt(1));

      return integers;
    }
  };

  public static final ResultPacker<String> STRING_PACKER = new ResultPacker<String>() {
    public List<String> pack(final ResultSet rs, final int fetchCount) throws SQLException {
      final List<String> strings = new ArrayList<String>();
      int counter = 0;
      while (rs.next() && (fetchCount < 0 || counter++ < fetchCount))
        strings.add(rs.getString(1));

      return strings;
    }
  };

  /**
   * Generates a sql select query with the given parameters
   * @param table the name of the table from which to select
   * @param columns the columns to select, example: "col1, col2"
   * @param whereCondition the where condition
   * @param orderByClause a string specifying the columns 'ORDER BY' clause,
   * "col1, col2" as input results in the following order by clause "order by col1, col2"
   * @return the generated sql query
   */
  public static String generateSelectSql(final String table, final String columns, final String whereCondition,
                                         final String orderByClause) {
    final StringBuilder sql = new StringBuilder("select ");
    sql.append(columns);
    sql.append(" from ");
    sql.append(table);
    if (whereCondition != null && whereCondition.length() > 0)
      sql.append(" ").append(whereCondition);
    if (orderByClause != null && orderByClause.length() > 0) {
      sql.append(" order by ");
      sql.append(orderByClause);
    }

    return sql.toString();
  }
}

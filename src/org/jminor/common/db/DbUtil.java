/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.i18n.Messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A static utility class
 */
public class DbUtil {

  public static final int ORA_INVALID_IDENTIFIER_ERR_CODE = 904;
  public static final int ORA_NULL_VALUE_ERR_CODE = 1400;
  public static final int ORA_INTEGRITY_CONSTRAINT_ERR_CODE = 2291;
  public static final int ORA_CHILD_RECORD_ERR_CODE = 2292;

  public static final HashMap<Integer, String> oracleSqlErrorCodes = new HashMap<Integer, String>();

  static {
    oracleSqlErrorCodes.put(1, Messages.get(Messages.UNIQUE_KEY_ERROR));
    oracleSqlErrorCodes.put(ORA_CHILD_RECORD_ERR_CODE, Messages.get(Messages.CHILD_RECORD_ERROR));
    oracleSqlErrorCodes.put(ORA_NULL_VALUE_ERR_CODE, Messages.get(Messages.NULL_VALUE_ERROR));
    oracleSqlErrorCodes.put(ORA_INTEGRITY_CONSTRAINT_ERR_CODE, Messages.get(Messages.INTEGRITY_CONSTRAINT_ERROR));
    oracleSqlErrorCodes.put(2290, Messages.get(Messages.CHECK_CONSTRAINT_ERROR));
    oracleSqlErrorCodes.put(1407, Messages.get(Messages.NULL_VALUE_ERROR));
    oracleSqlErrorCodes.put(1031, Messages.get(Messages.MISSING_PRIVILEGES_ERROR));
    oracleSqlErrorCodes.put(1017, Messages.get(Messages.LOGIN_CREDENTIALS_ERROR));
    oracleSqlErrorCodes.put(942, Messages.get(Messages.TABLE_NOT_FOUND_ERROR));
    oracleSqlErrorCodes.put(1045, Messages.get(Messages.USER_UNABLE_TO_CONNECT_ERROR));
    oracleSqlErrorCodes.put(1401, Messages.get(Messages.VALUE_TOO_LARGE_FOR_COLUMN_ERROR));
    oracleSqlErrorCodes.put(4063, Messages.get(Messages.VIEW_HAS_ERRORS_ERROR));
  }

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
      while (offset < bytes.length && (numRead = inputStream.read(bytes, offset, bytes.length-offset)) >= 0) {
        offset += numRead;
      }

      // Ensure all the bytes have been read in
      if (offset < bytes.length) {
        throw new IOException("Could not completely read file "+file.getName());
      }

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

  public static final IResultPacker<Integer> INT_PACKER = new IResultPacker<Integer>() {
    public List<Integer> pack(final ResultSet rs, final int recordCount) throws SQLException {
      final ArrayList<Integer> ret = new ArrayList<Integer>();
      int counter = 0;
      while (rs.next() && (recordCount < 0 || counter++ < recordCount))
        ret.add(rs.getInt(1));

      return ret;
    }
  };

  public static final IResultPacker<String> STRING_PACKER = new IResultPacker<String>() {
    public List<String> pack(final ResultSet rs, final int recordCount) throws SQLException {
      final ArrayList<String> ret = new ArrayList<String>();
      int counter = 0;
      while (rs.next() && (recordCount < 0 || counter++ < recordCount))
        ret.add(rs.getString(1));

      return ret;
    }
  };

  /**
   * Generates a sql select query with the given parameters
   * @param table the table from which to select
   * @param columns the columns to select
   * @param whereCondition the where condition
   * @param orderByColumns a string for the 'ORDER BY' clause ["col1, col2"] = " order by col1, col2"
   * @return the generated sql query
   */
  public static String generateSelectSql(final String table, final String columns,
                                         final String whereCondition, final String orderByColumns) {
    final StringBuffer sql = new StringBuffer("select ");
    sql.append(columns);
    sql.append(" from ");
    sql.append(table);
    if (whereCondition != null && whereCondition.length() > 0)
      sql.append(" ").append(whereCondition);
    if (orderByColumns != null && orderByColumns.length() > 0) {
      sql.append(" order by ");
      sql.append(orderByColumns);
    }

    return sql.toString();
  }
}

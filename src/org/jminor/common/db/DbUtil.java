/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.db;

import org.jminor.common.Constants;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.framework.FrameworkConstants;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A static utility class
 */
public class DbUtil {

  public static final int INVALID_IDENTIFIER_ERR_CODE = 904;
  public static final int NULL_VALUE_ERR_CODE = 1400;
  public static final int INTEGRITY_CONSTRAINT_ERR_CODE = 2291;
  public static final int CHILD_RECORD_ERR_CODE = 2292;

  public static final HashMap<Integer, String> oracleSqlErrorCodes = new HashMap<Integer, String>();
  public static final String ORACLE = "oracle";
  public static final String MYSQL = "mysql";

  public static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.driver.OracleDriver";
  public static final String ORACLE_DRIVER_CLASS_14 = "oracle.jdbc.OracleDriver";//todo wtf?
  public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
  public static final String SQLITE_DRIVER = "org.sqlite.JDBC";

  public static final String DB_TYPE = System.getProperty(FrameworkConstants.DATABASE_TYPE_PROPERTY, ORACLE);

  static {
    oracleSqlErrorCodes.put(1, Messages.get(Messages.UNIQUE_KEY_ERROR));
    oracleSqlErrorCodes.put(CHILD_RECORD_ERR_CODE, Messages.get(Messages.CHILD_RECORD_ERROR));
    oracleSqlErrorCodes.put(NULL_VALUE_ERR_CODE, Messages.get(Messages.NULL_VALUE_ERROR));
    oracleSqlErrorCodes.put(INTEGRITY_CONSTRAINT_ERR_CODE, Messages.get(Messages.INTEGRITY_CONSTRAINT_ERROR));
    oracleSqlErrorCodes.put(2290, Messages.get(Messages.CHECK_CONSTRAINT_ERROR));
    oracleSqlErrorCodes.put(1407, Messages.get(Messages.NULL_VALUE_ERROR));
    oracleSqlErrorCodes.put(1031, Messages.get(Messages.MISSING_PRIVILEGES_ERROR));
    oracleSqlErrorCodes.put(1017, Messages.get(Messages.LOGIN_CREDENTIALS_ERROR));
    oracleSqlErrorCodes.put(942, Messages.get(Messages.TABLE_NOT_FOUND_ERROR));
    oracleSqlErrorCodes.put(1045, Messages.get(Messages.USER_UNABLE_TO_CONNECT_ERROR));
    oracleSqlErrorCodes.put(1401, Messages.get(Messages.VALUE_TOO_LARGE_FOR_COLUMN_ERROR));
    oracleSqlErrorCodes.put(4063, Messages.get(Messages.VIEW_HAS_ERRORS_ERROR));
  }

  public static final IResultPacker<PrimaryKey> PRIMARY_KEY_PACKER = new IResultPacker<PrimaryKey>() {
    public List<PrimaryKey> pack(final ResultSet rs) throws SQLException {
      final List<String> columns = new ArrayList<String>();
      String keyName = null;
      while (rs.next()) {
        if (keyName == null)
          keyName = rs.getString(1);
        columns.add(rs.getString(2));
      }
      final List<PrimaryKey> ret = new ArrayList<PrimaryKey>(1);
      ret.add(new PrimaryKey(keyName, columns.toArray(new String[columns.size()])));

      return ret;
    }
  };

  public static final IResultPacker<Object> OBJECT_PACKER = new IResultPacker<Object>() {
    public List<Object> pack(final ResultSet rs) throws SQLException {
      final ArrayList<Object> ret = new ArrayList<Object>();
      while (rs.next())
        ret.add(rs.getObject(1));

      return ret;
    }
  };

  public static final IResultPacker<Integer> INT_PACKER = new IResultPacker<Integer>() {
    public List<Integer> pack(final ResultSet rs) throws SQLException {
      final ArrayList<Integer> ret = new ArrayList<Integer>();
      while (rs.next())
        ret.add(rs.getInt(1));

      return ret;
    }
  };

  public static final IResultPacker<String> STRING_PACKER = new IResultPacker<String>() {
    public List<String> pack(final ResultSet rs) throws SQLException {
      final ArrayList<String> ret = new ArrayList<String>();
      while (rs.next())
        ret.add(rs.getString(1));

      return ret;
    }
  };

  public static final IResultPacker<TableStatus> TABLE_STATUS_PACKER = new IResultPacker<TableStatus>() {
    public List<TableStatus> pack(final ResultSet rs) throws SQLException {
      final TableStatus stat = new TableStatus();
      rs.next();
      stat.setRecordCount(rs.getInt(1));
      if (rs.getMetaData().getColumnCount() == 2) {
        stat.setTableHasAuditColumns(true);
        final Timestamp t = rs.getTimestamp(2);
        if (!rs.wasNull())
          stat.setLastChange(t.getTime());
      }
      else {
        stat.setTableHasAuditColumns(false);
      }
      final List<TableStatus> ret = new ArrayList<TableStatus>(1);
      ret.add(stat);

      return ret;
    }
  };

  public static Date nvl(final Date val) {
    return val == null ? Constants.TIMESTAMP_NULL_VALUE : val;
  }

  public static Timestamp nvl(final Timestamp val) {
    return val == null ? Constants.TIMESTAMP_NULL_VALUE : val;
  }

  public static String nvl(final String val) {
    return val == null ? "" : val;
  }

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

  public static boolean isMySQL() {
    return DB_TYPE.equals(MYSQL);
  }

  public static boolean isOracle() {
    return DB_TYPE.equals(ORACLE);
  }

  public static String getDatabaseDriver() {
    if (isOracle())
      return ORACLE_DRIVER_CLASS;
    if (isMySQL())
      return MYSQL_DRIVER;
    else
      throw new RuntimeException("Unsopported database type: " + DB_TYPE);
  }

  public static String getDatabaseURL(final String host, final String port, final String sid) {
   if (isOracle())
      return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
    else if (isMySQL())
      return "jdbc:mysql://" + host + ":" + port + "/" + sid;
    else
      throw new RuntimeException("Unsopported database type: " + DB_TYPE);
  }

  /**
   * Returns a query string for retrieving the last automatically generated id for the given entityID,
   * in Oracle this means the value from a defined sequence and in MySQL the value fetched from last_inserted_id()
   * @param idSource the source for the id, for example a sequence name
   * @return a query string for retrieving the last auto-increment value for entityID
   */
  public static String getAutoIncrementValueSQL(final String idSource) {
    if (isMySQL())
      return "select last_insert_id() from dual";
    else if (isOracle())
      return "select " + idSource + ".currval from dual";
    else
      throw new RuntimeException("getAutoIncrementValue, unsupported db type: " + DB_TYPE);
  }

  public static String getSQLDateString(final Date value, final boolean longDate) {
    if (isOracle())
      return longDate ?
              "to_date('" + LongDateFormat.get().format(value) + "', 'DD-MM-YYYY HH24:MI')" :
              "to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
    else if (isMySQL())
      return longDate ?
              "str_to_date('" + LongDateFormat.get().format(value) + "', '%d-%m-%Y %H:%i')" :
              "str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y')";
    else
      throw new RuntimeException("SQL string to date, unsupported db type: " + DB_TYPE);
  }

  public static class PrimaryKey implements Serializable {

    private final String name;
    private final String[] columns;

    public PrimaryKey(final String name, final String[] columns) {
      this.name = name;
      this.columns = columns;
    }

    /**
     * @return Value for property 'columns'.
     */
    public String[] getColumns() {
      return columns;
    }

    /**
     * @return Value for property 'name'.
     */
    public String getName() {
      return name;
    }
  }

  public static class TableDependenciesPacker implements IResultPacker<Object> {
    private final TableDependencies info;

    public TableDependenciesPacker(final TableDependencies info) {
      this.info = info;
    }

    /** {@inheritDoc} */
    public List<Object> pack(final ResultSet resultSet) throws SQLException {
      final HashMap<String, ArrayList<String>> dependencyMap = new HashMap<String, ArrayList<String>>();
      while (resultSet.next()) {
        final String tableName = resultSet.getString(1);
        ArrayList<String> columnNames = dependencyMap.get(tableName);
        if (columnNames == null)
          dependencyMap.put(tableName, columnNames = new ArrayList<String>());
        columnNames.add(resultSet.getString(2));
      }
      for (final Map.Entry<String, ArrayList<String>> entry : dependencyMap.entrySet())
        info.addDependency(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));

      return null;
    }
  }
}

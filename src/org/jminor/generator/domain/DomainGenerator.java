/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.generator.domain;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.IResultPacker;
import org.jminor.common.ui.LoginPanel;
import org.jminor.framework.domain.Type;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 22.8.2009
 * Time: 16:17:21
 */
public class DomainGenerator {

  public static void main(String[] args) {
    try {
      System.out.println(getDomainClass("PETSTORE"));
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String getDomainClass(String schema) throws Exception {
    if (schema == null || schema.length() == 0)
      throw new IllegalArgumentException("Schema must be specified");

    final DbConnection dbConnection = new DbConnection(LoginPanel.getUser(null, null));

    final DatabaseMetaData metaData = dbConnection.getConnection().getMetaData();
    List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
    List<Table> tables = new TablePacker().pack(metaData.getTables(null, schema, null, null), -1);
    for (final Table table : tables)
      foreignKeys.addAll(new ForeignKeyPacker().pack(metaData.getExportedKeys(null, table.schemaName, table.tableName), -1));

    StringBuilder builder = new StringBuilder("public class ").append(schema.substring(0,1).
            toUpperCase()).append(schema.substring(1).toLowerCase()).append(" {\n\n");
    for (final Table table : tables) {
      List<Column> columns = new ColumnPacker().pack(metaData.getColumns(null, table.schemaName, table.tableName, null), -1);
      builder.append(getConstants(table.schemaName, table.tableName, columns, foreignKeys));
      builder.append("\n");
    }
    builder.append("  static {\n");
    builder.append("  //initilize your entities here\n");
    builder.append("  }\n");

    builder.append("}");

    dbConnection.disconnect();

    return builder.toString();
  }

  public static String getConstants(String schemaName, String tableName, List<Column> columns, List<ForeignKey> foreignKeys) {
    StringBuilder builder = new StringBuilder("  public static final String T_").append(tableName.toUpperCase()).append(
            " = \"").append(schemaName.toLowerCase()).append(".").append(tableName.toLowerCase()).append("\";").append("\n");
    for (Column column : columns) {
      builder.append("  ").append("public static final String ").append(tableName.toUpperCase()).append("_").append(
              column.columnName.toUpperCase()).append(" = \"").append(column.columnName.toLowerCase()).append("\";").append("\n");
      if (isForeignKeyColumn(column, foreignKeys))
        builder.append("  ").append("public static final String ").append(tableName.toUpperCase()).append("_").append(
                column.columnName.toUpperCase()).append("_FK").append(" = \"").append(column.columnName.toLowerCase()).append("_fk\";").append("\n");
    }

    return builder.toString();
  }

  private static boolean isForeignKeyColumn(Column column, List<ForeignKey> foreignKeys) {
    for (ForeignKey foreignKey : foreignKeys)
      if (foreignKey.fkTableName.equals(column.tableName) && foreignKey.fkColumnName.equals(column.columnName))
        return true;

    return false;
  }

  public static Type translate(final int sqlType) {
    switch (sqlType) {
      case Types.BIGINT:
      case Types.INTEGER:
      case Types.ROWID:
      case Types.SMALLINT:
        return Type.INT;
      case Types.CHAR:
        return Type.CHAR;
      case Types.DATE:
        return Type.SHORT_DATE;
      case Types.DECIMAL:
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.NUMERIC:
      case Types.REAL:
        return Type.DOUBLE;
      case Types.TIME:
      case Types.TIMESTAMP:
        return Type.LONG_DATE;
      case Types.VARCHAR:
        return Type.STRING;
      case Types.BLOB:
        return Type.BLOB;
      case Types.BOOLEAN:
        return Type.BOOLEAN;
    }

    throw new IllegalArgumentException("Unsupported sql type: " + sqlType);
  }

  static class Schema {
    final String schemaName;

    public Schema(final String schemaName) {
      this.schemaName = schemaName;
    }

    @Override
    public String toString() {
      return schemaName;
    }
  }

  static class SchemaPacker implements IResultPacker<Schema> {
    public List<Schema> pack(ResultSet resultSet, int recordCount) throws SQLException {
      final List<Schema> ret = new ArrayList<Schema>();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
        ret.add(new Schema(resultSet.getString("TABLE_SCHEM")));
      }

      return ret;
    }
  }

  static class Table {
    final String schemaName;
    final String tableName;

    public Table(final String schemaName, final String tableName) {
      this.schemaName = schemaName;
      this.tableName = tableName;
    }

    @Override
    public String toString() {
      return schemaName + "." + tableName;
    }
  }

  static class TablePacker implements IResultPacker<Table> {
    public List<Table> pack(ResultSet resultSet, int recordCount) throws SQLException {
      final List<Table> ret = new ArrayList<Table>();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
        ret.add(new Table(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME")));
      }

      return ret;
    }
  }

  static class Column {
    final String schemaName;
    final String tableName;
    final String columnName;
    final int columnType;

    public Column(final String schemaName, final String tableName, final String columnName, final int columnType) {
      this.schemaName = schemaName;
      this.tableName = tableName;
      this.columnName = columnName;
      this.columnType = columnType;
    }

    @Override
    public String toString() {
      return schemaName + "." + tableName + "." + columnName + "(" + translate(columnType) + ")";
    }
  }

  static class ColumnPacker implements IResultPacker<Column> {
    public List<Column> pack(ResultSet resultSet, int recordCount) throws SQLException {
      final List<Column> ret = new ArrayList<Column>();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
        ret.add(new Column(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME"),
                resultSet.getString("COLUMN_NAME"), resultSet.getInt("DATA_TYPE")));
      }

      return ret;
    }
  }

  static class ForeignKey {
    final String pkSchemaName;
    final String pkTableName;
    final String pkColumnName;
    final String fkSchemaName;
    final String fkTableName;
    final String fkColumnName;
    final short keySeq;

    public ForeignKey(final String pkSchemaName, final String pkTableName, final String pkColumnName,
                      final String fkSchemaName, final String fkTableName, final String fkColumnName,
                      final short keySeq) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.pkColumnName = pkColumnName;
      this.fkSchemaName = fkSchemaName;
      this.fkTableName = fkTableName;
      this.fkColumnName = fkColumnName;
      this.keySeq = keySeq;
    }

    @Override
    public String toString() {
      return fkSchemaName + "." + fkTableName + "." + fkColumnName + " -> " + pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }
  }

  static class ForeignKeyPacker implements IResultPacker<ForeignKey> {
    public List<ForeignKey> pack(ResultSet resultSet, int recordCount) throws SQLException {
      final List<ForeignKey> ret = new ArrayList<ForeignKey>();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
        ret.add(new ForeignKey(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
                resultSet.getString("PKCOLUMN_NAME"), resultSet.getString("FKTABLE_SCHEM"),
                resultSet.getString("FKTABLE_NAME"),  resultSet.getString("FKCOLUMN_NAME"),
                resultSet.getShort("KEY_SEQ")));
      }

      return ret;
    }
  }

  static class PrimaryKey {
    final String pkSchemaName;
    final String pkTableName;
    final String pkColumnName;
    final short keySeq;

    public PrimaryKey(final String pkSchemaName, final String pkTableName, final String pkColumnName, final short keySeq) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.pkColumnName = pkColumnName;
      this.keySeq = keySeq;
    }

    @Override
    public String toString() {
      return pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }
  }

  static class PrimaryKeyPacker implements IResultPacker<PrimaryKey> {
    public List<PrimaryKey> pack(ResultSet resultSet, int recordCount) throws SQLException {
      final List<PrimaryKey> ret = new ArrayList<PrimaryKey>();
      int counter = 0;
      while (resultSet.next() && (recordCount < 0 || counter++ < recordCount)) {
        ret.add(new PrimaryKey(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME"),
                resultSet.getString("COLUMN_NAME"), resultSet.getShort("KEY_SEQ")));
      }

      return ret;
    }
  }
}

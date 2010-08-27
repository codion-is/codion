/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools;

import org.jminor.common.db.DbConnectionImpl;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A DomainClassGenerator
 */
public final class DomainClassGenerator {

  private static final String TABLE_SCHEMA = "TABLE_SCHEM";

  private DomainClassGenerator() {}

  public static void main(final String[] arguments) throws Exception {
    final JTextField txtSchemaName = new JTextField();
    UiUtil.makeUpperCase(txtSchemaName);
    final JTextField txtPackageName = new JTextField();
    UiUtil.makeLowerCase(txtPackageName);
    final JTextArea txtTablesToInclude = new JTextArea(3,10);
    UiUtil.makeUpperCase(txtTablesToInclude);
    final JCheckBox chkClipboard = new JCheckBox("To clipboard", true);
    final JPanel panel = new JPanel(new FlexibleGridLayout(7,1,5,5,false,true));
    panel.add(new JLabel("Schema name"));
    panel.add(txtSchemaName);
    panel.add(new JLabel("Package name"));
    panel.add(txtPackageName);
    panel.add(new JLabel("Tables to include (comma separated)"));
    panel.add(txtTablesToInclude);
    panel.add(chkClipboard);

    final int option = JOptionPane.showConfirmDialog(null, panel, "Settings", JOptionPane.OK_CANCEL_OPTION);
    if (option == JOptionPane.OK_OPTION) {
      try {
        final String username = txtSchemaName.getText();
        final User user = LoginPanel.getUser(null, username != null ? new User(username, null) : null);
        final String schemaName = txtSchemaName.getText();
        final String domainClassName = getDomainClassName(schemaName);
        final String domainClass = getDomainClass(domainClassName, schemaName, txtPackageName.getText(),
                user.getUsername(), user.getPassword(), txtTablesToInclude.getText());
        if (!chkClipboard.isSelected()) {
          Util.writeFile(domainClass, UiUtil.chooseFileToSave(null, null, domainClassName + ".java"));
        }
        else {
          System.out.println(domainClass);
          Util.setClipboard(domainClass);
        }

        JOptionPane.showMessageDialog(null, "Domain class has been exported", "Done", JOptionPane.INFORMATION_MESSAGE);
      }
      catch (CancelException uce) {/**/}
    }
    System.exit(0);
  }

  public static String getDomainClassName(final String schemaName) {
    return schemaName.substring(0,1).toUpperCase() + schemaName.substring(1).toLowerCase();
  }

  public static String getDomainClass(final String domainClassName, final String schema, final String packageName,
                                      final String username, final String password, final String tableList) throws Exception {
    Util.rejectNullValue(schema, "schema");
    final DbConnectionImpl dbConnection = new DbConnectionImpl(DatabaseProvider.createInstance(), new User(username, password));
    try {

      final StringBuilder builder = new StringBuilder("package ").append(packageName).append(";\n\n");

      builder.append("import org.jminor.framework.domain.Entities;\n");
      builder.append("import org.jminor.framework.domain.Properties;\n\n");
      builder.append("import java.sql.Types;\n\n");

      builder.append("public class ").append(domainClassName).append(" {\n\n");

      final DatabaseMetaData metaData = dbConnection.getConnection().getMetaData();

      final List<Table> tablesToProcess = new TablePacker(tableList, schema).pack(metaData.getTables(schema, schema, null, null), -1);

      if (tablesToProcess.isEmpty()) {
        throw new IllegalArgumentException("No tables to process");
      }

      for (final Table table : tablesToProcess) {
        appendPropertyConstants(builder, table, metaData);
      }

      builder.append("  static {\n");

      for (final Table table : tablesToProcess) {
        appendEntityDefinition(builder, table);
      }

      builder.append("  }\n");

      builder.append("}");

      return builder.toString();
    }
    finally {
      dbConnection.disconnect();
    }
  }

  public static void appendEntityDefinition(final StringBuilder builder, final Table table) {
    builder.append("    Entities.define(").append(getEntityID(table)).append(",\n");
    for (final Column column : table.getColumns()) {
      builder.append(getPropertyDefinition(table, column))
              .append(table.getColumns().indexOf(column) < table.getColumns().size() - 1 ? ", " : "").append("\n");
    }
    builder.append("    );\n\n");
  }

  public static void appendPropertyConstants(final StringBuilder builder, final Table table,
                                             final DatabaseMetaData metaData) throws SQLException {
    final List<ForeignKey> foreignKeys = getForeignKeys(metaData, table.schemaName);
    final List<PrimaryKey> primaryKeys = getPrimaryKeys(metaData, table.schemaName);
    final List<Column> columns = new ColumnPacker().pack(metaData.getColumns(table.schemaName, table.schemaName, table.tableName, null), -1);
    for (final Column column : columns) {
      column.setForeignKey(getForeignKey(column, foreignKeys));
      column.setPrimaryKey(getPrimaryKey(column, primaryKeys));
    }
    table.setColumns(columns);
    builder.append(getConstants(table));
    builder.append("\n");
    System.out.println("Done processing table: " + table);
  }

  public static String getConstants(final Table table) {
    final String schemaName = table.schemaName;
    final StringBuilder builder = new StringBuilder("  public static final String ").append(getEntityID(table)).append(
            " = \"").append(schemaName.toLowerCase()).append(".").append(table.tableName.toLowerCase()).append("\";").append("\n");
    for (final Column column : table.getColumns()) {
      builder.append("  ").append("public static final String ").append(getPropertyID(table, column, false))
              .append(" = \"").append(column.columnName.toLowerCase()).append("\";\n");
      if (column.foreignKey != null) {
        builder.append("  ").append("public static final String ").append(getPropertyID(table, column, true))
                .append(" = \"").append(column.columnName.toLowerCase()).append("_fk\";").append("\n");
      }
    }

    return builder.toString();
  }

  private static String getPropertyDefinition(final Table table, final Column column) {
    String ret;
    final String propertyID = getPropertyID(table, column, column.foreignKey != null);
    if (column.foreignKey != null) {
      final String referencePropertyID = getPropertyID(table, column, false);
      ret = "        Properties.foreignKeyProperty(" + propertyID + ", \"" + propertyID + "\", " + getEntityID(column.foreignKey.getReferencedTable()) + ",\n";
      if (column.columnType == Types.INTEGER) {
        ret += "                Properties.columnProperty(" + referencePropertyID + "))";
      }
      else {
        ret += "                Properties.columnProperty(" + referencePropertyID + ", " + column.columnTypeName + "))";
      }
    }
    else if (column.primaryKey != null) {
      if (column.columnType == Types.INTEGER) {
        ret = "        Properties.primaryKeyProperty(" + propertyID + ")";
      }
      else {
        ret = "        Properties.primaryKeyProperty(" + propertyID + ", " + column.columnTypeName + ")";
      }
    }
    else {
      ret = "        Properties.columnProperty(" + propertyID + ", " + column.columnTypeName + ", \"" + propertyID + "\")";
    }
    if (column.foreignKey == null && column.hasDefaultValue) {
      ret += "\n                .setColumnHasDefaultValue(true)";
    }

    if (column.nullable == DatabaseMetaData.columnNoNulls && column.primaryKey == null) {
      ret += "\n                .setNullable(false)";
    }
    if (column.columnTypeName.equals("Types.VARCHAR")) {
      ret += "\n                .setMaxLength(" + column.columnSize + ")";
    }
    if (column.decimalDigits >= 1) {
      ret += "\n                .setMaximumFractionDigits(" + column.decimalDigits + ")";
    }
    if (!Util.nullOrEmpty(column.comment)) {
      ret += "\n                .setDescription(" + column.comment + ")";
    }

    return ret;
  }

  private static String getEntityID(final Table table) {
    return "T_" + table.tableName.toUpperCase();
  }

  private static String getPropertyID(final Table table, final Column column, final boolean isForeignKey) {
    return table.tableName.toUpperCase() + "_" + column.columnName.toUpperCase() + (isForeignKey ? "_FK" : "");
  }

  private static ForeignKey getForeignKey(final Column column, final List<ForeignKey> foreignKeys) {
    for (final ForeignKey foreignKey : foreignKeys) {
      if (foreignKey.fkTableName.equals(column.tableName)
              && foreignKey.fkColumnName.equals(column.columnName)) {
        System.out.println("foreignKey: " + column.tableName + ", " + column.columnName);
        return foreignKey;
      }
    }

    return null;
  }

  private static PrimaryKey getPrimaryKey(final Column column, final List<PrimaryKey> primaryKeys) {
    for (final PrimaryKey primaryKey : primaryKeys) {
      if (primaryKey.pkTableName.equals(column.tableName)
              && primaryKey.pkColumnName.equals(column.columnName)) {
        return primaryKey;
      }
    }

    return null;
  }

  private static List<PrimaryKey> getPrimaryKeys(final DatabaseMetaData metaData, final String schema) throws SQLException {
    final List<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>();
    final List<Table> allSchemaTables = new TablePacker(null, schema).pack(metaData.getTables(schema, schema, null, null), -1);
    for (final Table table : allSchemaTables) {
      primaryKeys.addAll(new PrimaryKeyPacker().pack(metaData.getPrimaryKeys(table.schemaName, table.schemaName, table.tableName), -1));
    }
    return primaryKeys;
  }

  private static List<ForeignKey> getForeignKeys(final DatabaseMetaData metaData, final String schema) throws SQLException {
    final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
    final List<Table> allSchemaTables = new TablePacker(null, schema).pack(metaData.getTables(schema, schema, null, null), -1);
    for (final Table table : allSchemaTables) {
      foreignKeys.addAll(new ForeignKeyPacker().pack(metaData.getExportedKeys(table.schemaName, table.schemaName, table.tableName), -1));
    }
    return foreignKeys;
  }

  public static String translateType(final int sqlType) {
    switch (sqlType) {
      case Types.BIGINT:
      case Types.INTEGER:
      case Types.ROWID:
      case Types.SMALLINT:
        return "Types.INTEGER";
      case Types.CHAR:
        return "Types.CHAR";
      case Types.DATE:
        return "Types.DATE";
      case Types.DECIMAL:
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.NUMERIC:
      case Types.REAL:
        return "Types.DOUBLE";
      case Types.TIME:
      case Types.TIMESTAMP:
        return "Types.TIMESTAMP";
      case Types.LONGVARCHAR:
      case Types.VARCHAR:
        return "Types.VARCHAR";
      case Types.BLOB:
        return "Types.BLOB";
      case Types.BOOLEAN:
        return "Types.BOOLEAN";
    }

    return null;
  }

  static class Schema {
    private final String schemaName;

    Schema(final String schemaName) {
      this.schemaName = schemaName;
    }

    @Override
    public String toString() {
      return schemaName;
    }
  }

  static class SchemaPacker implements ResultPacker<Schema> {
    public List<Schema> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Schema> schemas = new ArrayList<Schema>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        schemas.add(new Schema(resultSet.getString(TABLE_SCHEMA)));
      }

      return schemas;
    }
  }

  static class Table {
    private final String schemaName;
    private final String tableName;
    private List<Column> columns;

    Table(final String schemaName, final String tableName) {
      this.schemaName = schemaName;
      this.tableName = tableName;
    }

    public List<Column> getColumns() {
      return columns;
    }

    public void setColumns(final List<Column> columns) {
      this.columns = columns;
    }

    @Override
    public String toString() {
      return schemaName + "." + tableName;
    }
  }

  static class TablePacker implements ResultPacker<Table> {

    private final Collection<String> tablesToInclude;
    private final String schema;

    TablePacker(final String tablesToInclude, final String schema) {
      this.tablesToInclude = !Util.nullOrEmpty(tablesToInclude) ? getTablesToInclude(tablesToInclude) : null;
      this.schema = schema;
    }

    public List<Table> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Table> tables = new ArrayList<Table>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        final String tableName = resultSet.getString("TABLE_NAME");
        if (tablesToInclude == null || tablesToInclude.contains(tableName)) {
          String dbSchema = resultSet.getString(TABLE_SCHEMA);
          if (dbSchema == null) {
            dbSchema = this.schema;
          }
          tables.add(new Table(dbSchema, tableName));
        }
      }

      return tables;
    }

    private List<String> getTablesToInclude(final String tablesToInclude) {
      final List<String> ret = new ArrayList<String>();
      for (final String tableName : tablesToInclude.split(",")) {
        ret.add(tableName.trim());
      }

      return ret;
    }
  }

  static class Column {
    private final String schemaName;
    private final String tableName;
    private final String columnName;
    private final int columnType;
    private final String columnTypeName;
    private final int columnSize;
    private final int decimalDigits;
    private final int nullable;
    private final boolean hasDefaultValue;
    private final String comment;
    private ForeignKey foreignKey;
    private PrimaryKey primaryKey;

    Column(final String schemaName, final String tableName, final String columnName, final int columnType,
           final String columnTypeName, final int columnSize, final int decimalDigits, final int nullable,
           final boolean hasDefaultValue,
           final String comment) {
      this.schemaName = schemaName;
      this.tableName = tableName;
      this.columnName = columnName;
      this.columnType = columnType;
      this.columnTypeName = columnTypeName;
      this.columnSize = columnSize;
      this.decimalDigits = decimalDigits;
      this.nullable = nullable;
      this.hasDefaultValue = hasDefaultValue;
      this.comment = comment;
    }

    public void setForeignKey(final ForeignKey foreignKeyColumn) {
      this.foreignKey = foreignKeyColumn;
    }

    public void setPrimaryKey(final PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
    }

    @Override
    public String toString() {
      return schemaName + "." + tableName + "." + columnName;
    }
  }

  static class ColumnPacker implements ResultPacker<Column> {
    public List<Column> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Column> columns = new ArrayList<Column>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        final int dataType = resultSet.getInt("DATA_TYPE");
        final String translatedType = translateType(dataType);
        if (translatedType != null) {
          int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
          if (resultSet.wasNull()) {
            decimalDigits = -1;
          }
          columns.add(new Column(resultSet.getString(TABLE_SCHEMA), resultSet.getString("TABLE_NAME"),
                  resultSet.getString("COLUMN_NAME"), dataType, translatedType,
                  resultSet.getInt("COLUMN_SIZE"), decimalDigits, resultSet.getInt("NULLABLE"),
                  resultSet.getObject("COLUMN_DEF") != null, resultSet.getString("REMARKS")));
        }
      }

      return columns;
    }
  }

  static class ForeignKey {
    private final String pkSchemaName;
    private final String pkTableName;
    private final String pkColumnName;
    private final String fkSchemaName;
    private final String fkTableName;
    private final String fkColumnName;
    private final int keySeq;

    ForeignKey(final String pkSchemaName, final String pkTableName, final String pkColumnName,
               final String fkSchemaName, final String fkTableName, final String fkColumnName,
               final int keySeq) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.pkColumnName = pkColumnName;
      this.fkSchemaName = fkSchemaName;
      this.fkTableName = fkTableName;
      this.fkColumnName = fkColumnName;
      this.keySeq = keySeq;
    }

    public Table getReferencedTable() {
      return new Table(pkSchemaName, pkTableName);
    }

    @Override
    public String toString() {
      return fkSchemaName + "." + fkTableName + "." + fkColumnName + " -> " + pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }

    public int getKeySeq() {
      return keySeq;
    }
  }

  static class ForeignKeyPacker implements ResultPacker<ForeignKey> {
    public List<ForeignKey> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        foreignKeys.add(new ForeignKey(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
                resultSet.getString("PKCOLUMN_NAME"), resultSet.getString("FKTABLE_SCHEM"),
                resultSet.getString("FKTABLE_NAME"), resultSet.getString("FKCOLUMN_NAME"),
                resultSet.getShort("KEY_SEQ")));
      }

      return foreignKeys;
    }
  }

  static class PrimaryKey {
    private final String pkSchemaName;
    private final String pkTableName;
    private final String pkColumnName;
    private final int keySeq;

    PrimaryKey(final String pkSchemaName, final String pkTableName, final String pkColumnName, final int keySeq) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.pkColumnName = pkColumnName;
      this.keySeq = keySeq;
    }

    @Override
    public String toString() {
      return pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }

    public int getKeySeq() {
      return keySeq;
    }
  }

  static class PrimaryKeyPacker implements ResultPacker<PrimaryKey> {
    public List<PrimaryKey> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        primaryKeys.add(new PrimaryKey(resultSet.getString(TABLE_SCHEMA), resultSet.getString("TABLE_NAME"),
                resultSet.getString("COLUMN_NAME"), resultSet.getInt("KEY_SEQ")));
      }

      return primaryKeys;
    }
  }
}

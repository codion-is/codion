/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;

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
 * User: Bjorn Darri<br>
 * Date: 22.8.2009<br>
 * Time: 16:17:21<br>
 */
public class DomainClassGenerator {

  public static void main(String[] arguments) {
    final JTextField txtSchemaName = new JTextField();
    UiUtil.makeUpperCase(txtSchemaName);
    final JTextField txtPackageName = new JTextField();
    UiUtil.makeLowerCase(txtPackageName);
    final JTextArea txtTablesToInclude = new JTextArea(3,10);
    UiUtil.makeUpperCase(txtTablesToInclude);
    final JPanel panel = new JPanel(new FlexibleGridLayout(6,1,5,5,false,true));
    panel.add(new JLabel("Schema name"));
    panel.add(txtSchemaName);
    panel.add(new JLabel("Package name"));
    panel.add(txtPackageName);
    panel.add(new JLabel("Tables to include (comma separated)"));
    panel.add(txtTablesToInclude);

    final int option = JOptionPane.showConfirmDialog(null, panel, "Settings", JOptionPane.OK_CANCEL_OPTION);
    if (option == JOptionPane.OK_OPTION) {
      try {
        final User user = LoginPanel.getUser(null, null);
        final String schemaName = txtSchemaName.getText();
        final String domainClassName = getDomainClassName(schemaName);
        Util.writeFile(getDomainClass(domainClassName, schemaName, txtPackageName.getText(),
                user.getUsername(), user.getPassword(), txtTablesToInclude.getText()),
                UiUtil.chooseFileToSave(null, null, domainClassName + ".java"));
      }
      catch (CancelException uce) {/**/}
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.exit(0);
  }

  public static String getDomainClassName(final String schemaName) {
    return schemaName.substring(0,1).toUpperCase() + schemaName.substring(1).toLowerCase();
  }

  public static String getDomainClass(final String domainClassName, final String schema, final String packageName,
                                      final String username, final String password, final String tableList) throws Exception {
    if (schema == null || schema.length() == 0)
      throw new IllegalArgumentException("Schema must be specified");

    final DbConnection dbConnection = new DbConnection(DatabaseProvider.createInstance(), new User(username, password));
    try {
      final DatabaseMetaData metaData = dbConnection.getConnection().getMetaData();
      final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
      final List<Table> allSchemaTables = new TablePacker(null).pack(metaData.getTables(null, schema, null, null), -1);
      for (final Table table : allSchemaTables)
        foreignKeys.addAll(new ForeignKeyPacker().pack(metaData.getExportedKeys(null, table.schemaName, table.tableName), -1));

      final StringBuilder builder = new StringBuilder("package ").append(packageName).append(";\n\n");

      builder.append("import org.jminor.framework.domain.EntityDefinition;\n");
      builder.append("import org.jminor.framework.domain.EntityRepository;\n");
      builder.append("import org.jminor.framework.domain.Property;\n\n");
      builder.append("import java.sql.Types;");

      builder.append("public class ").append(domainClassName).append(" {\n\n");

      final List<Table> tablesToProcess = new TablePacker(tableList).pack(metaData.getTables(null, schema, null, null), -1);
      final List<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>();
      for (final Table table : tablesToProcess)
        primaryKeys.addAll(new PrimaryKeyPacker().pack(metaData.getPrimaryKeys(null, table.schemaName, table.tableName), -1));

      for (final Table table : tablesToProcess) {
        System.out.println("Processing table: " + table);
        final List<Column> columns = new ColumnPacker().pack(metaData.getColumns(null, table.schemaName, table.tableName, null), -1);
        for (final Column column : columns) {
          column.setForeignKey(getForeignKey(column, foreignKeys));
          column.setPrimaryKey(getPrimaryKey(column, primaryKeys));
        }
        table.setColumns(columns);
        builder.append(getConstants(table));
        builder.append("\n");
        System.out.println("Done processing table: " + table);
      }
      builder.append("  static {\n");
      for (final Table table : tablesToProcess) {
        builder.append("    EntityRepository.add(new EntityDefinition(").append(getEntityID(table)).append(",\n");
        for (final Column column : table.getColumns()) {
          builder.append(getPropertyDefinition(table, column))
                  .append(table.getColumns().indexOf(column) < table.getColumns().size() - 1 ? ", " : "").append("\n");
        }
        builder.append("    ));\n\n");
      }
      builder.append("  }\n");

      builder.append("}");

      return builder.toString();
    }
    finally {
      dbConnection.disconnect();
    }
  }

  public static String getConstants(final Table table) {
    final StringBuilder builder = new StringBuilder("  public static final String ").append(getEntityID(table)).append(
            " = \"").append(table.schemaName.toLowerCase()).append(".").append(table.tableName.toLowerCase()).append("\";").append("\n");
    for (final Column column : table.getColumns()) {
      builder.append("  ").append("public static final String ").append(getPropertyID(table, column, false))
              .append(" = \"").append(column.columnName.toLowerCase()).append("\";\n");
      if (column.foreignKey != null)
        builder.append("  ").append("public static final String ").append(getPropertyID(table, column, true))
                .append(" = \"").append(column.columnName.toLowerCase()).append("_fk\";").append("\n");
    }

    return builder.toString();
  }

  private static String getPropertyDefinition(final Table table, final Column column) {
    String ret;
    if (column.foreignKey != null) {
      ret = "        new Property.ForeignKeyProperty(" + getPropertyID(table, column, true) + ", \"Caption\", " + getEntityID(column.foreignKey.getReferencedTable()) + ",\n"
              + "                new Property(" + getPropertyID(table, column, false) + ", " + column.columnType + "))";
    }
    else if (column.primaryKey != null) {
      ret = "        new Property.PrimaryKeyProperty(" + getPropertyID(table, column, false) + ", " + column.columnType + ")";
    }
    else {
      ret = "        new Property(" + getPropertyID(table, column, false) + ", " + column.columnType + ", \"Caption\")";
    }

    if (column.nullable == DatabaseMetaData.columnNoNulls && column.primaryKey == null)
      ret += "\n                .setNullable(false)";
    if (column.hasDefaultValue)
      ret += "\n                .setColumnHasDefaultValue(false)";
    if (column.columnType.equals("Types.VARCHAR"))
      ret += "\n                .setMaxLength(" + column.columnSize + ")";

    return ret;
  }

  private static String getEntityID(final Table table) {
    return "T_" + table.tableName.toUpperCase();
  }

  private static String getPropertyID(final Table table, final Column column, final boolean isForeignKey) {
    return table.tableName.toUpperCase() + "_" + column.columnName.toUpperCase() + (isForeignKey ? "_FK" : "");
  }

  private static ForeignKey getForeignKey(final Column column, final List<ForeignKey> foreignKeys) {
    for (final ForeignKey foreignKey : foreignKeys)
      if (foreignKey.fkTableName.equals(column.tableName)
              && foreignKey.fkColumnName.equals(column.columnName)) {
        System.out.println("foreignKey: " + column.tableName + ", " + column.columnName);
        return foreignKey;
      }

    return null;
  }

  private static PrimaryKey getPrimaryKey(final Column column, final List<PrimaryKey> primaryKeys) {
    for (final PrimaryKey primaryKey : primaryKeys)
      if (primaryKey.pkTableName.equals(column.tableName)
              && primaryKey.pkColumnName.equals(column.columnName)) {
        return primaryKey;
      }

    return null;
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
    final String schemaName;

    public Schema(final String schemaName) {
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
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount))
        schemas.add(new Schema(resultSet.getString("TABLE_SCHEM")));

      return schemas;
    }
  }

  static class Table {
    final String schemaName;
    final String tableName;
    List<Column> columns;

    public Table(final String schemaName, final String tableName) {
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

    public TablePacker(final String tablesToInclude) {
      this.tablesToInclude = tablesToInclude != null && tablesToInclude.length() > 0
              ? getTablesToInclude(tablesToInclude) : null;
    }

    public List<Table> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Table> tables = new ArrayList<Table>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        final String tableName = resultSet.getString("TABLE_NAME");
        if (tablesToInclude == null || tablesToInclude.contains(tableName))
          tables.add(new Table(resultSet.getString("TABLE_SCHEM"), tableName));
      }

      return tables;
    }

    private List<String> getTablesToInclude(final String tablesToInclude) {
      final List<String> ret = new ArrayList<String>();
      for (final String tableName : tablesToInclude.split(","))
        ret.add(tableName.trim());

      return ret;
    }
  }

  static class Column {
    final String schemaName;
    final String tableName;
    final String columnName;
    final String columnType;
    final int columnSize;
    final int nullable;
    final boolean hasDefaultValue;
    ForeignKey foreignKey;
    PrimaryKey primaryKey;

    public Column(final String schemaName, final String tableName, final String columnName, final String columnType,
                  final int columnSize, final int nullable, final boolean hasDefaultValue) {
      this.schemaName = schemaName;
      this.tableName = tableName;
      this.columnName = columnName;
      this.columnType = columnType;
      this.columnSize = columnSize;
      this.nullable = nullable;
      this.hasDefaultValue = hasDefaultValue;
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
        final String translatedType = translateType(resultSet.getInt("DATA_TYPE"));
        if (translatedType != null)
          columns.add(new Column(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME"),
                  resultSet.getString("COLUMN_NAME"), translatedType,
                  resultSet.getInt("COLUMN_SIZE"), resultSet.getInt("NULLABLE"), resultSet.getObject("COLUMN_DEF") != null));
      }

      return columns;
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

    public Table getReferencedTable() {
      return new Table(pkSchemaName, pkTableName);
    }

    @Override
    public String toString() {
      return fkSchemaName + "." + fkTableName + "." + fkColumnName + " -> " + pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }
  }

  static class ForeignKeyPacker implements ResultPacker<ForeignKey> {
    public List<ForeignKey> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount))
        foreignKeys.add(new ForeignKey(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
                resultSet.getString("PKCOLUMN_NAME"), resultSet.getString("FKTABLE_SCHEM"),
                resultSet.getString("FKTABLE_NAME"),  resultSet.getString("FKCOLUMN_NAME"),
                resultSet.getShort("KEY_SEQ")));

      return foreignKeys;
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

  static class PrimaryKeyPacker implements ResultPacker<PrimaryKey> {
    public List<PrimaryKey> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount))
        primaryKeys.add(new PrimaryKey(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME"),
                resultSet.getString("COLUMN_NAME"), resultSet.getShort("KEY_SEQ")));

      return primaryKeys;
    }
  }
}

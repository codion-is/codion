/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.generator;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.DefaultColumnSearchModel;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A model class for generating entity definitions.
 * @see org.jminor.framework.tools.generator.ui.EntityGeneratorPanel
 */
public final class EntityGeneratorModel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityGeneratorModel.class);

  private static final String TABLE_SCHEMA = "TABLE_SCHEM";
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private final Connection connection;
  private final String schema;
  private final String catalog;
  private final DatabaseMetaData metaData;
  private final TableModel tableModel;
  private final Document document;
  private final Event evtRefreshStarted = Events.event();
  private final Event evtRefreshEnded = Events.event();
  private final List<ForeignKey> foreignKeys;
  private final List<PrimaryKey> primaryKeys;

  /**
   * Instantiates a new EntityGeneratorModel.
   * @param user the user
   * @param schema the schema name
   * @throws ClassNotFoundException in case the JDBC driver class was not found on the classpath
   * @throws SQLException in case of an exception while connecting to the database
   */
  public EntityGeneratorModel(final User user, final String schema) throws ClassNotFoundException, SQLException {
    this.schema = schema;
    final Database database = Databases.createInstance();
    this.catalog = database.getDatabaseType().equals(Database.MYSQL) ? schema : null;
    this.connection = database.createConnection(user);
    this.metaData = connection.getMetaData();
    this.tableModel = initializeTableModel();
    this.tableModel.refresh();
    this.document = new DefaultStyledDocument();
    this.foreignKeys = getForeignKeys(metaData, catalog, schema);
    this.primaryKeys = getPrimaryKeys(metaData, catalog, schema);
    bindEvents();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        exit();
      }
    });
  }

  /**
   * @return a table model containing the tables from the generator schema
   */
  public AbstractFilteredTableModel<Table, Integer> getTableListModel() {
    return tableModel;
  }

  /**
   * @return the text document containing the entity definitions of the selected tables
   */
  public Document getDocument() {
    return document;
  }

  /**
   * @param listener a listener notified each time a refresh has started
   */
  public void addRefreshStartedListener(final ActionListener listener) {
    evtRefreshStarted.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeRefreshStartedListener(final ActionListener listener) {
    evtRefreshStarted.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time a refresh has ended
   */
  public void addRefreshEndedListener(final ActionListener listener) {
    evtRefreshEnded.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeRefreshEndedListener(final ActionListener listener) {
    evtRefreshEnded.removeListener(listener);
  }

  /**
   * Closes the connection to the database
   */
  public void exit() {
    try {
      if (connection != null) {
        connection.close();
        LOG.info("EntityGeneratorModel closed connection");
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private TableModel initializeTableModel() throws ClassNotFoundException, SQLException {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final TableColumn column = new TableColumn();
    column.setIdentifier(0);
    column.setHeaderValue("Table");
    columnModel.addColumn(column);

    return new TableModel(columnModel, metaData, schema, catalog);
  }

  private void bindEvents() {
    tableModel.addSelectionChangedListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          evtRefreshStarted.fire();
          refreshDocument(tableModel.getSelectedItems());
        }
        finally {
          evtRefreshEnded.fire();
        }
      }
    });
  }

  private void refreshDocument(final List<Table> tables) {
    try {
      document.remove(0, document.getLength());
      for (final Table table : tables) {
        final String constantsString = getPropertyConstants(table, foreignKeys, primaryKeys);
        document.insertString(document.getLength(), constantsString, null);
      }
      for (final Table table : tables) {
        final String entityString = getEntityDefinition(table);
        document.insertString(document.getLength(), entityString, null);
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
    catch (SQLException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private String getEntityDefinition(final Table table) {
    final StringBuilder builder = new StringBuilder();
    appendEntityDefinition(builder, table);

    return builder.toString();
  }

  private String getPropertyConstants(final Table table, final List<ForeignKey> foreignKeys, final List<PrimaryKey> primaryKeys) throws SQLException {
    final List<Column> columns = new ColumnPacker().pack(metaData.getColumns(catalog, table.schemaName, table.tableName, null), -1);
    final StringBuilder builder = new StringBuilder();
    appendPropertyConstants(builder, table, columns, foreignKeys, primaryKeys);

    return builder.toString();
  }

  private static void appendEntityDefinition(final StringBuilder builder, final Table table) {
    builder.append("Entities.define(").append(getEntityID(table)).append(",").append(LINE_SEPARATOR);
    for (final Column column : table.getColumns()) {
      builder.append(getPropertyDefinition(table, column))
              .append(table.getColumns().indexOf(column) < table.getColumns().size() - 1 ? ", " : "").append(LINE_SEPARATOR);
    }
    builder.append(");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
  }

  private static void appendPropertyConstants(final StringBuilder builder, final Table table,
                                              final List<Column> columns, final List<ForeignKey> foreignKeys,
                                              final List<PrimaryKey> primaryKeys) throws SQLException {
    for (final Column column : columns) {
      column.setForeignKey(getForeignKey(column, foreignKeys));
      column.setPrimaryKey(getPrimaryKey(column, primaryKeys));
    }
    table.setColumns(columns);
    builder.append(getConstants(table));
    builder.append(LINE_SEPARATOR);
  }

  private static ForeignKey getForeignKey(final Column column, final List<ForeignKey> foreignKeys) {
    for (final ForeignKey foreignKey : foreignKeys) {
      if (foreignKey.fkTableName.equals(column.tableName)
              && foreignKey.fkColumnName.equals(column.columnName)) {
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

  private static String getConstants(final Table table) {
    final String schemaName = table.schemaName;
    final StringBuilder builder = new StringBuilder("public static final String ").append(getEntityID(table)).append(
            " = \"").append(schemaName.toLowerCase()).append(".").append(table.tableName.toLowerCase()).append("\";").append(LINE_SEPARATOR);
    for (final Column column : table.getColumns()) {
      builder.append("public static final String ").append(getPropertyID(table, column, false))
              .append(" = \"").append(column.columnName.toLowerCase()).append("\";").append(LINE_SEPARATOR);
      if (column.foreignKey != null) {
        builder.append("public static final String ").append(getPropertyID(table, column, true))
                .append(" = \"").append(column.columnName.toLowerCase()).append("_fk\";").append(LINE_SEPARATOR);
      }
    }

    return builder.toString();
  }

  private static String getPropertyDefinition(final Table table, final Column column) {
    String ret;
    final String propertyID = getPropertyID(table, column, column.foreignKey != null);
    final String caption = getCaption(column);
    if (column.foreignKey != null) {
      final String referencePropertyID = getPropertyID(table, column, false);
      ret = "        Properties.foreignKeyProperty(" + propertyID + ", \"" + caption + "\", " +
              getEntityID(column.foreignKey.getReferencedTable()) + "," + LINE_SEPARATOR;
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
      ret = "        Properties.columnProperty(" + propertyID + ", " + column.columnTypeName + ", \"" + caption + "\")";
    }
    if (column.foreignKey == null && column.hasDefaultValue) {
      ret += LINE_SEPARATOR + "                .setColumnHasDefaultValue(true)";
    }

    if (column.nullable == DatabaseMetaData.columnNoNulls && column.primaryKey == null) {
      ret += LINE_SEPARATOR + "                .setNullable(false)";
    }
    if (column.columnTypeName.equals("Types.VARCHAR")) {
      ret += LINE_SEPARATOR + "                .setMaxLength(" + column.columnSize + ")";
    }
    if (column.decimalDigits >= 1) {
      ret += LINE_SEPARATOR + "                .setMaximumFractionDigits(" + column.decimalDigits + ")";
    }
    if (!Util.nullOrEmpty(column.comment)) {
      ret += LINE_SEPARATOR + "                .setDescription(" + column.comment + ")";
    }

    return ret;
  }

  private static String getEntityID(final Table table) {
    return "T_" + table.tableName.toUpperCase();
  }

  private static String getPropertyID(final Table table, final Column column, final boolean isForeignKey) {
    return table.tableName.toUpperCase() + "_" + column.columnName.toUpperCase() + (isForeignKey ? "_FK" : "");
  }

  private static String getCaption(final Column column) {
    String columnName = column.columnName.toLowerCase().replaceAll("_", " ");

    return columnName.substring(0, 1).toUpperCase() + columnName.substring(1, columnName.length());
  }

  private static List<PrimaryKey> getPrimaryKeys(final DatabaseMetaData metaData, final String catalog, final String schema) throws SQLException {
    final List<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>();
    final List<Table> allSchemaTables = new TablePacker(null, schema).pack(metaData.getTables(catalog, schema, null, null), -1);
    for (final Table table : allSchemaTables) {
      primaryKeys.addAll(new PrimaryKeyPacker().pack(metaData.getPrimaryKeys(catalog, table.schemaName, table.tableName), -1));
    }
    return primaryKeys;
  }

  private static List<ForeignKey> getForeignKeys(final DatabaseMetaData metaData, final String catalog, final String schema) throws SQLException {
    final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
    final List<Table> allSchemaTables = new TablePacker(null, schema).pack(metaData.getTables(catalog, schema, null, null), -1);
    for (final Table table : allSchemaTables) {
      final List<ForeignKey> fks = new ForeignKeyPacker().pack(metaData.getExportedKeys(catalog, table.schemaName, table.tableName), -1);
      foreignKeys.addAll(fks);
    }
    return foreignKeys;
  }

  private static String translateType(final int sqlType) {
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

  public static final class Table {
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return schemaName + "." + tableName;
    }
  }

  private static final class TablePacker implements ResultPacker<Table> {

    private final Collection<String> tablesToInclude;
    private final String schema;

    private TablePacker(final String tablesToInclude, final String schema) {
      this.tablesToInclude = !Util.nullOrEmpty(tablesToInclude) ? getTablesToInclude(tablesToInclude) : null;
      this.schema = schema;
    }

    /** {@inheritDoc} */
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

      resultSet.close();

      return tables;
    }

    private static List<String> getTablesToInclude(final String tablesToInclude) {
      final List<String> ret = new ArrayList<String>();
      for (final String tableName : tablesToInclude.split(",")) {
        ret.add(tableName.trim());
      }

      return ret;
    }
  }

  private static final class Column {
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

    private Column(final String schemaName, final String tableName, final String columnName, final int columnType,
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

    private void setForeignKey(final ForeignKey foreignKeyColumn) {
      this.foreignKey = foreignKeyColumn;
    }

    private void setPrimaryKey(final PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return schemaName + "." + tableName + "." + columnName;
    }
  }

  private static final class ColumnPacker implements ResultPacker<Column> {
    /** {@inheritDoc} */
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

      resultSet.close();

      return columns;
    }
  }

  private static final class ForeignKey {
    private final String pkSchemaName;
    private final String pkTableName;
    private final String pkColumnName;
    private final String fkSchemaName;
    private final String fkTableName;
    private final String fkColumnName;
//    private final int keySeq;

    private ForeignKey(final String pkSchemaName, final String pkTableName, final String pkColumnName,
                       final String fkSchemaName, final String fkTableName, final String fkColumnName/*,
               final int keySeq*/) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.pkColumnName = pkColumnName;
      this.fkSchemaName = fkSchemaName;
      this.fkTableName = fkTableName;
      this.fkColumnName = fkColumnName;
//      this.keySeq = keySeq;
    }

    private Table getReferencedTable() {
      return new Table(pkSchemaName, pkTableName);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return fkSchemaName + "." + fkTableName + "." + fkColumnName + " -> " + pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }

//    private int getKeySeq() {
//      return keySeq;
//    }
  }

  private static final class ForeignKeyPacker implements ResultPacker<ForeignKey> {
    /** {@inheritDoc} */
    public List<ForeignKey> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        foreignKeys.add(new ForeignKey(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
                resultSet.getString("PKCOLUMN_NAME"), resultSet.getString("FKTABLE_SCHEM"),
                resultSet.getString("FKTABLE_NAME"), resultSet.getString("FKCOLUMN_NAME")/*,
                resultSet.getShort("KEY_SEQ")*/));
      }

      resultSet.close();

      return foreignKeys;
    }
  }

  private static final class PrimaryKey {
    private final String pkSchemaName;
    private final String pkTableName;
    private final String pkColumnName;
//    private final int keySeq;

    PrimaryKey(final String pkSchemaName, final String pkTableName, final String pkColumnName/*, final int keySeq*/) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.pkColumnName = pkColumnName;
//      this.keySeq = keySeq;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return pkSchemaName + "." + pkTableName + "." + pkColumnName;
    }

//    private int getKeySeq() {
//      return keySeq;
//    }
  }

  private static final class PrimaryKeyPacker implements ResultPacker<PrimaryKey> {
    /** {@inheritDoc} */
    public List<PrimaryKey> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        primaryKeys.add(new PrimaryKey(resultSet.getString(TABLE_SCHEMA), resultSet.getString("TABLE_NAME"),
                resultSet.getString("COLUMN_NAME")/*, resultSet.getInt("KEY_SEQ")*/));
      }

      resultSet.close();

      return primaryKeys;
    }
  }

  private static final class TableModel extends AbstractFilteredTableModel<Table, Integer> {

    private final DatabaseMetaData metaData;
    private final String schema;
    private final String catalog;

    private TableModel(final TableColumnModel columnModel, final DatabaseMetaData metaData,
                       final String schema, final String catalog) {
      super(columnModel, Arrays.asList(new DefaultColumnSearchModel<Integer>(0, Types.VARCHAR, "%")));
      this.metaData = metaData;
      this.schema = schema;
      this.catalog = catalog;
    }

    @Override
    protected void doRefresh() {
      try {
        clear();
        final List<Table> schemaTables = new TablePacker(null, schema).pack(metaData.getTables(catalog, schema, null, null), -1);
        addItems(schemaTables, true);
      }
      catch (SQLException e) {
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex).tableName;
    }

    @Override
    protected Comparable getComparable(final Object object, final Integer columnIdentifier) {
      return ((Table) object).tableName;
    }
  }
}

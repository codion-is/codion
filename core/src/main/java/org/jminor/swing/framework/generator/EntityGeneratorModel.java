/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.generator;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.DefaultColumnCriteriaModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A model class for generating entity definitions.
 * @see org.jminor.swing.framework.generator.ui.EntityGeneratorPanel
 */
public final class EntityGeneratorModel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityGeneratorModel.class);

  private static final String TABLE_SCHEMA = "TABLE_SCHEM";
  private static final String FOREIGN_KEY_PROPERTY_SUFFIX = "_FK";
  private static final String ENTITY_ID_PREFIX = "T_";
  private static final String PROPERTIES_COLUMN_PROPERTY = "        Properties.columnProperty(";

  static final Integer SCHEMA_COLUMN_ID = 0;
  static final Integer TABLE_COLUMN_ID = 1;

  private final Connection connection;
  private final String schema;
  private final String catalog;
  private final DatabaseMetaData metaData;
  private final AbstractFilteredTableModel<Table, Integer> tableModel;
  private final Document document = new DefaultStyledDocument();
  private final Event refreshStartedEvent = Events.event();
  private final Event refreshEndedEvent = Events.event();

  /**
   * Instantiates a new EntityGeneratorModel.
   * @param user the user
   * @param schema the schema name
   * @throws DatabaseException in case of an exception while connecting to the database
   */
  public EntityGeneratorModel(final User user, final String schema) throws DatabaseException {
    this(Databases.createInstance(), user, schema);
  }

  /**
   * Instantiates a new EntityGeneratorModel.
   * @param database the database
   * @param user the user
   * @param schema the schema name
   * @throws DatabaseException in case of an exception while connecting to the database
   */
  public EntityGeneratorModel(final Database database, final User user, final String schema) throws DatabaseException {
    try {
      this.schema = schema;
      this.catalog = database.getDatabaseType().equals(Database.MYSQL) ? schema : null;
      this.connection = database.createConnection(user);
      this.metaData = connection.getMetaData();
      this.tableModel = initializeTableModel();
      this.tableModel.refresh();
      bindEvents();
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, database.getErrorMessage(e));
    }
  }

  /**
   * @return a table model containing the tables from the generator schema
   */
  public AbstractFilteredTableModel<Table, Integer> getTableModel() {
    return tableModel;
  }

  /**
   * @return the text document containing the entity definitions of the selected tables
   */
  public Document getDocument() {
    return document;
  }

  /**
   * @return the text from the entity definition document
   */
  public String getDocumentText() {
    try {
      return document.getText(0, document.getLength());
    }
    catch (final BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param listener a listener notified each time a refresh has started
   */
  public void addRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time a refresh has ended
   */
  public void addRefreshDoneListener(final EventListener listener) {
    refreshEndedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeRefreshDoneListener(final EventListener listener) {
    refreshEndedEvent.removeListener(listener);
  }

  /**
   * Closes the connection to the database
   */
  public void exit() {
    DatabaseUtil.closeSilently(connection);
  }

  private TableModel initializeTableModel() {
    final TableColumn schemaColumn = new TableColumn(SCHEMA_COLUMN_ID);
    schemaColumn.setIdentifier(SCHEMA_COLUMN_ID);
    schemaColumn.setHeaderValue("Schema");
    final TableColumn tableColumn = new TableColumn(TABLE_COLUMN_ID);
    tableColumn.setIdentifier(TABLE_COLUMN_ID);
    tableColumn.setHeaderValue("Table");

    return new TableModel(Arrays.asList(schemaColumn, tableColumn), metaData, schema, catalog);
  }

  private void bindEvents() {
    tableModel.getSelectionModel().addSelectionChangedListener(new EventListener() {
      @Override
      public void eventOccurred() {
        try {
          refreshStartedEvent.fire();
          generateDefinitions(tableModel.getSelectionModel().getSelectedItems());
        }
        finally {
          refreshEndedEvent.fire();
        }
      }
    });
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        exit();
      }
    });
  }

  private void generateDefinitions(final List<Table> tables) {
    try {
      int counter = 0;
      document.remove(0, document.getLength());
      for (final Table table : tables) {
        final String constantsString = getPropertyConstants(table);
        document.insertString(document.getLength(), constantsString, null);
        document.insertString(document.getLength(), Util.LINE_SEPARATOR, null);
        final String entityString = getEntityDefinition(table);
        document.insertString(document.getLength(), entityString, null);
        if (counter++ < tables.size() - 1) {
          document.insertString(document.getLength(), Util.LINE_SEPARATOR + Util.LINE_SEPARATOR, null);
        }
      }
    }
    catch (final BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  private String getPropertyConstants(final Table table) {
    final StringBuilder builder = new StringBuilder();
    appendPropertyConstants(builder, table);

    return builder.toString();
  }

  private static void appendEntityDefinition(final StringBuilder builder, final Table table) {
    builder.append("static {").append(Util.LINE_SEPARATOR);
    builder.append("  Entities.define(").append(getEntityID(table)).append(",").append(Util.LINE_SEPARATOR);
    for (final Column column : table.getColumns()) {
      builder.append("  ").append(getPropertyDefinition(table, column))
              .append(table.getColumns().indexOf(column) < table.getColumns().size() - 1 ? "," : "").append(Util.LINE_SEPARATOR);
    }
    builder.append("  );").append(Util.LINE_SEPARATOR);
    builder.append("}");
  }

  private static void appendPropertyConstants(final StringBuilder builder, final Table table) {
    builder.append(getConstants(table));
  }

  private static String getConstants(final Table table) {
    final String schemaName = table.getSchemaName();
    final StringBuilder builder = new StringBuilder("public static final String ").append(getEntityID(table))
            .append(" = \"").append(schemaName.toLowerCase()).append(".").append(table.getTableName().toLowerCase())
            .append("\";").append(Util.LINE_SEPARATOR);
    for (final Column column : table.getColumns()) {
      builder.append("public static final String ").append(getPropertyID(table, column, false))
              .append(" = \"").append(column.getColumnName().toLowerCase()).append("\";").append(Util.LINE_SEPARATOR);
      if (column.foreignKeyColumn != null) {
        builder.append("public static final String ").append(getPropertyID(table, column, true))
                .append(" = \"").append(column.getColumnName().toLowerCase())
                .append(FOREIGN_KEY_PROPERTY_SUFFIX.toLowerCase()).append("\";").append(Util.LINE_SEPARATOR);
      }
    }

    return builder.toString();
  }

  private static String getPropertyDefinition(final Table table, final Column column) {
    if (column.getForeignKeyColumn() != null) {
      return getForeignKeyPropertyDefinition(table, column);
    }

    return getColumnPropertyDefinition(table, column, false);
  }

  private static String getForeignKeyPropertyDefinition(final Table table, final Column column) {
    final String columnPropertyDefinition = getColumnPropertyDefinition(table, column, true);
    final StringBuilder builder = new StringBuilder();
    final String foreignKeyID = getPropertyID(table, column, true);
    final String caption = getCaption(column);
    builder.append("        Properties.foreignKeyProperty(").append(foreignKeyID).append(", \"").append(caption)
            .append("\", ").append(getEntityID(column.getForeignKeyColumn().getReferencedTable()))
            .append(",").append(Util.LINE_SEPARATOR);
    builder.append("        ").append(columnPropertyDefinition).append(")");

    if (column.getNullable() == DatabaseMetaData.columnNoNulls) {
      builder.append(Util.LINE_SEPARATOR).append("                .setNullable(false)");
    }

    return builder.toString();
  }

  private static String getColumnPropertyDefinition(final Table table, final Column column, final boolean foreignKeyColumn) {
    final StringBuilder builder = new StringBuilder();
    addPropertyDefinition(builder, table, column, foreignKeyColumn);
    if (column.getForeignKeyColumn() == null && column.hasDefaultValue()) {
      builder.append(Util.LINE_SEPARATOR).append("                .setColumnHasDefaultValue(true)");
    }
    if (column.getNullable() == DatabaseMetaData.columnNoNulls && column.getKeySeq() == -1 && column.getForeignKeyColumn() == null) {
      builder.append(Util.LINE_SEPARATOR).append("                .setNullable(false)");
    }
    if (column.getColumnTypeName().equals("Types.VARCHAR")) {
      builder.append(Util.LINE_SEPARATOR).append("                .setMaxLength(").append(column.getColumnSize()).append(")");
    }
    if (column.getColumnTypeName().equals("Types.DOUBLE") && column.getDecimalDigits() >= 1) {
      builder.append(Util.LINE_SEPARATOR).append("                .setMaximumFractionDigits(").append(column.getDecimalDigits()).append(")");
    }
    if (!Util.nullOrEmpty(column.getComment())) {
      builder.append(Util.LINE_SEPARATOR).append("                .setDescription(").append(column.getComment()).append(")");
    }

    return builder.toString();
  }

  private static void addPropertyDefinition(final StringBuilder builder, final Table table, final Column column,
                                            final boolean foreignKeyColumn) {
    final String propertyID = getPropertyID(table, column, false);
    final String caption = getCaption(column);
    if (column.getKeySeq() != -1) {
      if (column.getColumnTypeName().equals("Types.INTEGER")) {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyID).append(")");
      }
      else {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyID).append(", ")
                .append(column.getColumnTypeName()).append(")");
      }
      if (column.getKeySeq() > 0) {
        builder.append(Util.LINE_SEPARATOR);
        if (foreignKeyColumn) {
          builder.append("        ");
        }
        builder.append("                .setPrimaryKeyIndex(").append(column.getKeySeq() - 1).append(")");
      }
    }
    else {
      if (column.getForeignKeyColumn() != null && column.getColumnType() == Types.INTEGER) {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyID).append(")");
      }
      else {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyID).append(", ")
                .append(column.getColumnTypeName()).append(", \"").append(caption).append("\")");
      }
    }
  }

  private static String getEntityID(final Table table) {
    return ENTITY_ID_PREFIX + table.getTableName().toUpperCase();
  }

  private static String getPropertyID(final Table table, final Column column, final boolean isForeignKey) {
    return table.getTableName().toUpperCase() + "_" + column.getColumnName().toUpperCase()
            + (isForeignKey ? FOREIGN_KEY_PROPERTY_SUFFIX : "");
  }

  private static String getCaption(final Column column) {
    final String columnName = column.getColumnName().toLowerCase().replaceAll("_", " ");

    return columnName.substring(0, 1).toUpperCase() + columnName.substring(1, columnName.length());
  }

  private static String getEntityDefinition(final Table table) {
    final StringBuilder builder = new StringBuilder();
    appendEntityDefinition(builder, table);

    return builder.toString();
  }

  private static String translateType(final int sqlType, final int decimalDigits) {
    switch (sqlType) {
      case Types.BIGINT:
        return "Types.BIGINT";
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
      case Types.REAL:
      case Types.NUMERIC:
        return decimalDigits == 0 ? "Types.INTEGER" : "Types.DOUBLE";
      case Types.TIME:
        return "Types.TIME";
      case Types.TIMESTAMP:
        return "Types.TIMESTAMP";
      case Types.LONGVARCHAR:
      case Types.VARCHAR:
        return "Types.VARCHAR";
      case Types.BLOB:
        return "Types.BLOB";
      case Types.BOOLEAN:
        return "Types.BOOLEAN";
      default:
        return null;
    }
  }

  private static final class TableModel extends AbstractFilteredTableModel<Table, Integer> {

    private final DatabaseMetaData metaData;
    private final String schema;
    private final String catalog;

    private TableModel(final List<TableColumn> columns, final DatabaseMetaData metaData,
                       final String schema, final String catalog) {
      super(new AbstractTableSortModel<Table, Integer>(columns) {
        @Override
        public Class getColumnClass(final Integer columnIdentifier) {
          return String.class;
        }

        @Override
        protected Comparable getComparable(final Table rowObject, final Integer columnIdentifier) {
          if (columnIdentifier.equals(SCHEMA_COLUMN_ID)) {
            return rowObject.getSchemaName();
          }
          else {
            return rowObject.getTableName();
          }
        }
      }, Arrays.asList(new DefaultColumnCriteriaModel<>(0, Types.VARCHAR, "%"),
              new DefaultColumnCriteriaModel<>(1, Types.VARCHAR, "%")));
      this.metaData = metaData;
      this.schema = schema;
      this.catalog = catalog;
    }

    @Override
    protected void doRefresh() {
      try {
        clear();
        final Set<Table> items = new HashSet<>();
        final List<Table> tables = new TablePacker(schema).pack(metaData.getTables(catalog, schema, null, null), -1);
        for (final Table table : tables) {
          populateTable(table);
          for (final ForeignKeyColumn foreignKeyColumn : table.getForeignKeyColumns()) {
            final Table referencedTable = foreignKeyColumn.getReferencedTable();
            populateTable(referencedTable);
            items.add(referencedTable);
          }
        }
        items.addAll(tables);
        addItems(new ArrayList<>(items), true);
      }
      catch (final SQLException e) {
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }

    private void populateTable(final Table table) throws SQLException {
      table.setForeignKeys(new ForeignKeyColumnPacker().pack(metaData.getImportedKeys(catalog, table.getSchemaName(), table.getTableName()), -1));
      table.setPrimaryKeyColumns(new PrimaryKeyColumnPacker().pack(metaData.getPrimaryKeys(catalog, table.getSchemaName(), table.getTableName()), -1));
      table.setColumns(new ColumnPacker(table).pack(metaData.getColumns(catalog, table.getSchemaName(), table.getTableName(), null), -1));
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      final Table table = getItemAt(rowIndex);
      if (columnIndex == 0) {
        return table.getSchemaName();
      }
      else {
        return table.getTableName();
      }
    }
  }

  public static final class Table {

    private final String schemaName;
    private final String tableName;
    private List<Column> columns;
    private Collection<ForeignKeyColumn> foreignKeys;
    private Collection<PrimaryKeyColumn> primaryKeyColumns;

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

    public Collection<PrimaryKeyColumn> getPrimaryKeyColumns() {
      return primaryKeyColumns;
    }

    public void setPrimaryKeyColumns(final Collection<PrimaryKeyColumn> primaryKeyColumns) {
      this.primaryKeyColumns = primaryKeyColumns;
    }

    public Collection<ForeignKeyColumn> getForeignKeyColumns() {
      return foreignKeys;
    }

    public void setForeignKeys(final Collection<ForeignKeyColumn> foreignKeys) {
      this.foreignKeys = foreignKeys;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final Table table = (Table) o;

      return Util.equal(schemaName, table.getSchemaName()) && Util.equal(tableName, table.getTableName());
    }

    @Override
    public int hashCode() {
      int result = schemaName != null ? schemaName.hashCode() : 0;
      result = result + (tableName != null ? tableName.hashCode() : 0);

      return result;
    }

    public String getTableName() {
      return tableName;
    }

    public String getSchemaName() {
      return schemaName;
    }
  }

  private static final class Column {

    private final String columnName;
    private final int columnType;
    private final String columnTypeName;
    private final int columnSize;
    private final int decimalDigits;
    private final int nullable;
    private final boolean hasDefaultValue;
    private final String comment;
    private final int keySeq;
    private final ForeignKeyColumn foreignKeyColumn;

    public Column(final String columnName, final int columnType, final String columnTypeName, final int columnSize,
                  final int decimalDigits, final int nullable, final boolean hasDefaultValue, final String comment,
                  final ForeignKeyColumn foreignKeyColumn, final int keySeq) {
      this.columnName = columnName;
      this.columnType = columnType;
      this.columnTypeName = columnTypeName;
      this.columnSize = columnSize;
      this.decimalDigits = decimalDigits;
      this.nullable = nullable;
      this.hasDefaultValue = hasDefaultValue;
      this.comment = comment;
      this.foreignKeyColumn = foreignKeyColumn;
      this.keySeq = keySeq;
    }

    public String getColumnName() {
      return columnName;
    }

    public int getKeySeq() {
      return keySeq;
    }

    public int getColumnType() {
      return columnType;
    }

    public String getColumnTypeName() {
      return columnTypeName;
    }

    public boolean hasDefaultValue() {
      return hasDefaultValue;
    }

    public int getNullable() {
      return nullable;
    }

    public int getColumnSize() {
      return columnSize;
    }

    public int getDecimalDigits() {
      return decimalDigits;
    }

    public String getComment() {
      return comment;
    }

    public ForeignKeyColumn getForeignKeyColumn() {
      return foreignKeyColumn;
    }
  }

  private static final class PrimaryKeyColumn {

    private final String columnName;
    private final int keySeq;

    public PrimaryKeyColumn(final String columnName, final int keySeq) {
      this.columnName = columnName;
      this.keySeq = keySeq;
    }

    public String getColumnName() {
      return columnName;
    }

    public int getKeySeq() {
      return keySeq;
    }
  }

  private static final class ForeignKeyColumn {

    private final String pkSchemaName;
    private final String pkTableName;
    private final String fkTableName;
    private final String fkColumnName;

    public ForeignKeyColumn(final String pkSchemaName, final String pkTableName, final String fkTableName,
                            final String fkColumnName) {
      this.pkSchemaName = pkSchemaName;
      this.pkTableName = pkTableName;
      this.fkTableName = fkTableName;
      this.fkColumnName = fkColumnName;
    }

    public Table getReferencedTable() {
      return new Table(pkSchemaName, pkTableName);
    }

    public String getFkTableName() {
      return fkTableName;
    }

    public String getFkColumnName() {
      return fkColumnName;
    }
  }

  private static final class TablePacker implements ResultPacker<Table> {

    private final String schema;

    private TablePacker(final String schema) {
      this.schema = schema;
    }

    @Override
    public List<Table> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Table> tables = new ArrayList<>();
      while (resultSet.next()) {
        final String tableName = resultSet.getString("TABLE_NAME");
        String dbSchema = resultSet.getString(TABLE_SCHEMA);
        if (dbSchema == null) {
          dbSchema = this.schema;
        }
        tables.add(new Table(dbSchema, tableName));
      }

      resultSet.close();

      return tables;
    }
  }

  private static final class ColumnPacker implements ResultPacker<Column> {

    private final Table table;

    private ColumnPacker(final Table table) {
      this.table = table;
    }

    @Override
    public List<Column> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Column> columns = new ArrayList<>();
      while (resultSet.next()) {
        final int dataType = resultSet.getInt("DATA_TYPE");
        int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
        if (resultSet.wasNull()) {
          decimalDigits = -1;
        }
        final String translatedType = translateType(dataType, decimalDigits);
        if (translatedType != null) {
          final String tableName = resultSet.getString("TABLE_NAME");
          final String columnName = resultSet.getString("COLUMN_NAME");
          columns.add(new Column(columnName, dataType, translatedType,
                  resultSet.getInt("COLUMN_SIZE"), decimalDigits, resultSet.getInt("NULLABLE"),
                  resultSet.getObject("COLUMN_DEF") != null, resultSet.getString("REMARKS"),
                  getForeignKeyColumn(tableName, columnName), getPrimaryKeyColumnIndex(columnName)));
        }
      }

      resultSet.close();

      return columns;
    }

    private int getPrimaryKeyColumnIndex(final String columnName) {
      for (final PrimaryKeyColumn primaryKeyColumn : table.getPrimaryKeyColumns()) {
        if (columnName.equals(primaryKeyColumn.getColumnName())) {
          return primaryKeyColumn.getKeySeq();
        }
      }

      return -1;
    }

    private ForeignKeyColumn getForeignKeyColumn(final String tableName, final String columnName) {
      for (final ForeignKeyColumn foreignKeyColumn : table.getForeignKeyColumns()) {
        if (foreignKeyColumn.getFkTableName().equals(tableName) && foreignKeyColumn.getFkColumnName().equals(columnName)) {
          return foreignKeyColumn;
        }
      }

      return null;
    }
  }

  private static final class ForeignKeyColumnPacker implements ResultPacker<ForeignKeyColumn> {
    @Override
    public List<ForeignKeyColumn> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<ForeignKeyColumn> foreignKeys = new ArrayList<>();
      while (resultSet.next()) {
        foreignKeys.add(new ForeignKeyColumn(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
                resultSet.getString("FKTABLE_NAME"), resultSet.getString("FKCOLUMN_NAME")));
      }

      resultSet.close();

      return foreignKeys;
    }
  }

  private static final class PrimaryKeyColumnPacker implements ResultPacker<PrimaryKeyColumn> {
    @Override
    public List<PrimaryKeyColumn> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<PrimaryKeyColumn> primaryKeys = new ArrayList<>();
      while (resultSet.next()) {
        primaryKeys.add(new PrimaryKeyColumn(resultSet.getString("COLUMN_NAME"), resultSet.getInt("KEY_SEQ")));
      }

      resultSet.close();

      return primaryKeys;
    }
  }
}

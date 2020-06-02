/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.generator;

import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.AbstractTableSortModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.TableColumn;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * A model class for generating entity definitions.
 */
public class EntityGeneratorModel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityGeneratorModel.class);

  private static final String TYPES_INTEGER = "Types.INTEGER";
  private static final String PUBLIC_STATIC_FINAL_STRING = "public static final String ";
  private static final String EQUALS = " = \"";
  private static final String TABLE_SCHEMA = "TABLE_SCHEM";
  private static final String FOREIGN_KEY_PROPERTY_SUFFIX = "_FK";
  private static final String ENTITY_TYPE_PREFIX = "T_";
  private static final String PROPERTIES_COLUMN_PROPERTY = "        columnProperty(";

  static final Integer SCHEMA_COLUMN_ID = 0;
  static final Integer TABLE_COLUMN_ID = 1;

  private final Database database;
  private final Connection connection;
  private final DatabaseMetaData metaData;
  private final SchemaModel schemaModel;
  private final TableModel tableModel;
  private final Value<String> definitionTextValue = Values.value();
  private final Event refreshStartedEvent = Events.event();
  private final Event refreshEndedEvent = Events.event();

  /**
   * Instantiates a new EntityGeneratorModel.
   * @param user the user
   * @throws DatabaseException in case of an exception while connecting to the database
   */
  public EntityGeneratorModel(final User user) throws DatabaseException {
    this(Databases.getInstance(), user);
  }

  /**
   * Instantiates a new EntityGeneratorModel.
   * @param database the database
   * @param user the user
   * @throws DatabaseException in case of an exception while connecting to the database
   */
  public EntityGeneratorModel(final Database database, final User user) throws DatabaseException {
    try {
      this.database = database;
      this.connection = database.createConnection(user);
      this.metaData = connection.getMetaData();
      this.schemaModel = initializeSchemaModel();
      this.tableModel = initializeTableModel();
      this.schemaModel.refresh();
      bindEvents();
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, database.getErrorMessage(e));
    }
  }

  /**
   * @return a table model containing the database schemas
   */
  public final AbstractFilteredTableModel<Schema, Integer> getSchemaModel() {
    return schemaModel;
  }

  /**
   * @return a table model containing the tables from the generator schema
   */
  public final AbstractFilteredTableModel<Table, Integer> getTableModel() {
    return tableModel;
  }

  /**
   * @return the entity definition text value
   */
  public final Value<String> getDefinitionTextValue() {
    return definitionTextValue;
  }

  /**
   * @param listener a listener notified each time a refresh has started
   */
  public final void addRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time a refresh has ended
   */
  public final void addRefreshDoneListener(final EventListener listener) {
    refreshEndedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeRefreshDoneListener(final EventListener listener) {
    refreshEndedEvent.removeListener(listener);
  }

  /**
   * Closes the connection to the database
   */
  public final void exit() {
    Database.closeSilently(connection);
  }

  public static final String getEntityType(final Table table) {
    return table.getSchema() + "." + table.getTableName();
  }

  private SchemaModel initializeSchemaModel() {
    final TableColumn schemaColumn = new TableColumn(SCHEMA_COLUMN_ID);
    schemaColumn.setIdentifier(SCHEMA_COLUMN_ID);
    schemaColumn.setHeaderValue("Schema");

    return new SchemaModel(schemaColumn, metaData);
  }

  private TableModel initializeTableModel() {
    final TableColumn schemaColumn = new TableColumn(SCHEMA_COLUMN_ID);
    schemaColumn.setIdentifier(SCHEMA_COLUMN_ID);
    schemaColumn.setHeaderValue("Schema");
    final TableColumn tableColumn = new TableColumn(TABLE_COLUMN_ID);
    tableColumn.setIdentifier(TABLE_COLUMN_ID);
    tableColumn.setHeaderValue("Table");

    return new TableModel(asList(schemaColumn, tableColumn), database, metaData);
  }

  private void bindEvents() {
    schemaModel.getSelectionModel().addSelectedItemsListener(tableModel::setSchemas);
    tableModel.getSelectionModel().addSelectedItemsListener(selected -> {
      try {
        refreshStartedEvent.onEvent();
        generateDefinitions(selected);
      }
      finally {
        refreshEndedEvent.onEvent();
      }
    });
    Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
  }

  private void generateDefinitions(final List<Table> tables) {
    final StringBuilder builder = new StringBuilder();
    int counter = 0;
    for (final Table table : tables) {
      final String constantsString = getPropertyConstants(table);
      builder.append(constantsString).append(Util.LINE_SEPARATOR);
      final String entityString = getEntityDefinition(table);
      builder.append(entityString);
      if (counter++ < tables.size() - 1) {
        builder.append(Util.LINE_SEPARATOR + Util.LINE_SEPARATOR);
      }
    }
    definitionTextValue.set(builder.toString());
  }

  private static String getPropertyConstants(final Table table) {
    final StringBuilder builder = new StringBuilder();
    appendPropertyConstants(builder, table);

    return builder.toString();
  }

  private static void appendEntityDefinition(final StringBuilder builder, final Table table) {
    builder.append("void " + getDefineMethodName(table) + "() {").append(Util.LINE_SEPARATOR);
    builder.append("  define(").append(getEntityTypeConstant(table)).append(",").append(Util.LINE_SEPARATOR);
    for (final Column column : table.columns) {
      builder.append("  ").append(getPropertyDefinition(table, column))
              .append(table.columns.indexOf(column) < table.columns.size() - 1 ? "," : "").append(Util.LINE_SEPARATOR);
    }
    builder.append("  );").append(Util.LINE_SEPARATOR);
    builder.append("}");
  }

  private static String getDefineMethodName(final Table table) {
    final StringBuilder builder = new StringBuilder(table.getTableName().toLowerCase());
    int underscoreIndex = builder.indexOf("_");
    while (underscoreIndex >= 0) {
      builder.replace(underscoreIndex, underscoreIndex + 1, "");
      builder.replace(underscoreIndex, underscoreIndex + 1, builder.substring(underscoreIndex, underscoreIndex + 1).toUpperCase());
      underscoreIndex = builder.indexOf("_");
    }

    return builder.toString();
  }

  private static void appendPropertyConstants(final StringBuilder builder, final Table table) {
    builder.append(getConstants(table));
  }

  private static String getConstants(final Table table) {
    final String schemaName = table.getSchema().getName();
    final StringBuilder builder = new StringBuilder(PUBLIC_STATIC_FINAL_STRING).append(getEntityTypeConstant(table))
            .append(EQUALS).append(schemaName.toLowerCase()).append(".").append(table.getTableName().toLowerCase())
            .append("\";").append(Util.LINE_SEPARATOR);
    for (final Column column : table.columns) {
      builder.append(PUBLIC_STATIC_FINAL_STRING).append(getPropertyIdConstant(table, column, false))
              .append(EQUALS).append(column.getColumnName().toLowerCase()).append("\";").append(Util.LINE_SEPARATOR);
      if (column.foreignKeyColumn != null) {
        builder.append(PUBLIC_STATIC_FINAL_STRING).append(getPropertyIdConstant(table, column, true))
                .append(EQUALS).append(column.getColumnName().toLowerCase())
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
    final String foreignKeyId = getPropertyIdConstant(table, column, true);
    final String caption = getCaption(column);
    builder.append("        foreignKeyProperty(").append(foreignKeyId).append(", \"").append(caption)
            .append("\", ").append(getEntityTypeConstant(column.getForeignKeyColumn().getReferencedTable()))
            .append(",").append(Util.LINE_SEPARATOR);
    builder.append("        ").append(columnPropertyDefinition).append(")");

    if (column.getNullable() == DatabaseMetaData.columnNoNulls) {
      builder.append(Util.LINE_SEPARATOR).append("                .nullable(false)");
    }

    return builder.toString();
  }

  private static String getColumnPropertyDefinition(final Table table, final Column column, final boolean foreignKeyColumn) {
    final StringBuilder builder = new StringBuilder();
    addPropertyDefinition(builder, table, column, foreignKeyColumn);
    if (column.getForeignKeyColumn() == null && column.hasDefaultValue()) {
      builder.append(Util.LINE_SEPARATOR).append("                .columnHasDefaultValue(true)");
    }
    if (column.getNullable() == DatabaseMetaData.columnNoNulls && column.getKeySeq() == -1 && column.getForeignKeyColumn() == null) {
      builder.append(Util.LINE_SEPARATOR).append("                .nullable(false)");
    }
    if ("Types.VARCHAR".equals(column.getColumnTypeName())) {
      builder.append(Util.LINE_SEPARATOR).append("                .maximumLength(").append(column.getColumnSize()).append(")");
    }
    if ("Types.DOUBLE".equals(column.getColumnTypeName()) && column.getDecimalDigits() >= 1) {
      builder.append(Util.LINE_SEPARATOR).append("                .maximumFractionDigits(").append(column.getDecimalDigits()).append(")");
    }
    if (!nullOrEmpty(column.getComment())) {
      builder.append(Util.LINE_SEPARATOR).append("                .description(").append(column.getComment()).append(")");
    }

    return builder.toString();
  }

  private static void addPropertyDefinition(final StringBuilder builder, final Table table, final Column column,
                                            final boolean foreignKeyColumn) {
    final String propertyId = getPropertyIdConstant(table, column, false);
    final String caption = getCaption(column);
    if (column.getKeySeq() != -1) {
      if (TYPES_INTEGER.equals(column.getColumnTypeName())) {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyId).append(")");
      }
      else {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyId).append(", ")
                .append(column.getColumnTypeName()).append(")");
      }
      if (column.getKeySeq() > 0) {
        builder.append(Util.LINE_SEPARATOR);
        if (foreignKeyColumn) {
          builder.append("        ");
        }
        builder.append("                .primaryKeyIndex(").append(column.getKeySeq() - 1).append(")");
      }
    }
    else {
      if (column.getForeignKeyColumn() != null && column.getColumnType() == Types.INTEGER) {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyId).append(")");
      }
      else {
        builder.append(PROPERTIES_COLUMN_PROPERTY).append(propertyId).append(", ")
                .append(column.getColumnTypeName()).append(", \"").append(caption).append("\")");
      }
    }
  }

  private static String getEntityTypeConstant(final Table table) {
    return ENTITY_TYPE_PREFIX + table.getTableName().toUpperCase();
  }

  private static String getPropertyIdConstant(final Table table, final Column column, final boolean isForeignKey) {
    return table.getTableName().toUpperCase() + "_" + column.getColumnName().toUpperCase()
            + (isForeignKey ? FOREIGN_KEY_PROPERTY_SUFFIX : "");
  }

  protected static String getCaption(final Column column) {
    final String columnName = column.getColumnName().toLowerCase().replaceAll("_", " ");

    return columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
  }

  private static String getEntityDefinition(final Table table) {
    final StringBuilder builder = new StringBuilder();
    appendEntityDefinition(builder, table);

    return builder.toString();
  }

  private static final class SchemaModel extends AbstractFilteredTableModel<Schema, Integer> {

    private final DatabaseMetaData metaData;

    private SchemaModel(final TableColumn column, final DatabaseMetaData metaData) {
      super(new AbstractTableSortModel<Schema, Integer>(singletonList(column)) {
        @Override
        public Class getColumnClass(final Integer columnIdentifier) {
          return Schema.class;
        }

        @Override
        protected Comparable getComparable(final Schema row, final Integer columnIdentifier) {
          return row.getName();
        }
      }, singletonList(new DefaultColumnConditionModel<>(0, Schema.class, "%")));
      this.metaData = metaData;
    }

    @Override
    protected void doRefresh() {
      try {
        clear();
        final ResultSet resultSet = metaData.getSchemas();
        final List<Schema> schemas = new SchemaPacker().pack(resultSet, -1);
        resultSet.close();
        final Set<Schema> items = new HashSet<>(schemas);
        addItemsAt(0, new ArrayList<>(items));
      }
      catch (final SQLException e) {
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }
  }

  private static final class TableModel extends AbstractFilteredTableModel<Table, Integer> {

    private final Database database;
    private final DatabaseMetaData metaData;
    private final List<Schema> schemas = new ArrayList<>();

    private TableModel(final List<TableColumn> columns, final Database database, final DatabaseMetaData metaData) {
      super(new AbstractTableSortModel<Table, Integer>(columns) {
        @Override
        public Class getColumnClass(final Integer columnIdentifier) {
          if (columnIdentifier.equals(SCHEMA_COLUMN_ID)) {
            return Schema.class;
          }

          return String.class;
        }

        @Override
        protected Comparable getComparable(final Table row, final Integer columnIdentifier) {
          if (columnIdentifier.equals(SCHEMA_COLUMN_ID)) {
            return row.getSchema().getName();
          }
          else {
            return row.getTableName();
          }
        }
      }, asList(new DefaultColumnConditionModel<>(0, Schema.class, "%"),
              new DefaultColumnConditionModel<>(1, String.class, "%")));
      this.database = database;
      this.metaData = metaData;
    }

    public void setSchemas(final List<Schema> schemas) {
      this.schemas.clear();
      this.schemas.addAll(schemas);
      refresh();
    }

    @Override
    protected void doRefresh() {
      try {
        clear();
        if (schemas.isEmpty()) {
          return;
        }
        final Set<Table> items = new HashSet<>();
        for (final Schema schema : schemas) {
          final String catalog = database.getClass().getName().equals("is.codion.dbms.mysql.MySQLDatabase") ? schema.getName() : null;
          final ResultSet resultSet = metaData.getTables(catalog, schema.getName(), null, null);
          final List<Table> tables = new TablePacker(schema.getName()).pack(resultSet, -1);
          resultSet.close();
          for (final Table table : tables) {
            populateTable(catalog, table);
            for (final ForeignKeyColumn foreignKeyColumn : table.foreignKeys) {
              final Table referencedTable = foreignKeyColumn.getReferencedTable();
              populateTable(catalog, referencedTable);
              items.add(referencedTable);
            }
          }

          items.addAll(tables);
        }
        addItemsAt(0, new ArrayList<>(items));
      }
      catch (final SQLException e) {
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }

    private void populateTable(final String catalog, final Table table) throws SQLException {
      ResultSet resultSet = metaData.getImportedKeys(catalog, table.getSchema().getName(), table.getTableName());
      table.foreignKeys = new ForeignKeyColumnPacker().pack(resultSet, -1);
      resultSet.close();
      resultSet = metaData.getPrimaryKeys(catalog, table.getSchema().getName(), table.getTableName());
      table.primaryKeyColumns = new PrimaryKeyColumnPacker().pack(resultSet, -1);
      resultSet.close();
      resultSet = metaData.getColumns(catalog, table.getSchema().getName(), table.getTableName(), null);
      table.columns = new ColumnPacker(table).pack(resultSet, -1);
      resultSet.close();
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      final Table table = getItemAt(rowIndex);
      if (columnIndex == 0) {
        return table.getSchema();
      }
      else {
        return table.getTableName();
      }
    }
  }

  public static final class Schema {

    private final String name;

    public Schema(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(final Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }
      final Schema schema = (Schema) object;

      return name.equals(schema.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  /**
   * Represents a database table
   */
  public static final class Table {

    private final Schema schema;
    private final String tableName;
    private List<Column> columns;
    private Collection<ForeignKeyColumn> foreignKeys;
    private Collection<PrimaryKeyColumn> primaryKeyColumns;

    public Table(final Schema schema, final String tableName) {
      this.schema = schema;
      this.tableName = tableName;
    }

    @Override
    public String toString() {
      return schema + "." + tableName;
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

      return Objects.equals(schema, table.getSchema()) && Objects.equals(tableName, table.getTableName());
    }

    @Override
    public int hashCode() {
      int result = schema != null ? schema.getName().hashCode() : 0;
      result = result + (tableName != null ? tableName.hashCode() : 0);

      return result;
    }

    public String getTableName() {
      return tableName;
    }

    public Schema getSchema() {
      return schema;
    }

    public List<Column> getColumns() {
      return columns;
    }
  }

  protected static final class Column {

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

    @Override
    public String toString() {
      return columnName;
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

    @Override
    public String toString() {
      return columnName;
    }
  }

  protected static final class ForeignKeyColumn {

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
      return new Table(new Schema(pkSchemaName), pkTableName);
    }

    public String getFkTableName() {
      return fkTableName;
    }

    public String getFkColumnName() {
      return fkColumnName;
    }
  }

  private static final class SchemaPacker implements ResultPacker<Schema> {

    @Override
    public Schema fetch(final ResultSet resultSet) throws SQLException {
      return new Schema(resultSet.getString("TABLE_SCHEM"));
    }
  }

  private static final class TablePacker implements ResultPacker<Table> {

    private final Schema schema;

    private TablePacker(final String schemaName) {
      this.schema = new Schema(schemaName);
    }

    @Override
    public Table fetch(final ResultSet resultSet) throws SQLException {
      final String tableName = resultSet.getString("TABLE_NAME");
      final Schema schema;
      final String dbSchemaName = resultSet.getString(TABLE_SCHEMA);
      if (dbSchemaName == null) {
        schema = this.schema;
      }
      else {
        schema = new Schema(dbSchemaName);
      }

      return new Table(schema, tableName);
    }
  }

  private static final class ColumnPacker implements ResultPacker<Column> {

    private final Table table;

    private ColumnPacker(final Table table) {
      this.table = table;
    }

    @Override
    public Column fetch(final ResultSet resultSet) throws SQLException {
      final int dataType = resultSet.getInt("DATA_TYPE");
      int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
      if (resultSet.wasNull()) {
        decimalDigits = -1;
      }
      final String translatedType = translateType(dataType, decimalDigits);
      if (translatedType != null) {
        final String tableName = resultSet.getString("TABLE_NAME");
        final String columnName = resultSet.getString("COLUMN_NAME");

        return new Column(columnName, dataType, translatedType,
                resultSet.getInt("COLUMN_SIZE"), decimalDigits, resultSet.getInt("NULLABLE"),
                resultSet.getObject("COLUMN_DEF") != null, resultSet.getString("REMARKS"),
                getForeignKeyColumn(tableName, columnName), getPrimaryKeyColumnIndex(columnName));
      }

      return null;
    }

    private int getPrimaryKeyColumnIndex(final String columnName) {
      return table.primaryKeyColumns.stream().filter(primaryKeyColumn ->
              columnName.equals(primaryKeyColumn.getColumnName())).findFirst().map(PrimaryKeyColumn::getKeySeq).orElse(-1);
    }

    private ForeignKeyColumn getForeignKeyColumn(final String tableName, final String columnName) {
      return table.foreignKeys.stream().filter(foreignKeyColumn ->
              foreignKeyColumn.getFkTableName().equals(tableName)
                      && foreignKeyColumn.getFkColumnName().equals(columnName)).findFirst().orElse(null);
    }

    private static String translateType(final int sqlType, final int decimalDigits) {
      switch (sqlType) {
        case Types.BIGINT:
          return "Types.BIGINT";
        case Types.INTEGER:
        case Types.ROWID:
        case Types.SMALLINT:
          return TYPES_INTEGER;
        case Types.CHAR:
          return "Types.CHAR";
        case Types.DATE:
          return "Types.DATE";
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.REAL:
        case Types.NUMERIC:
          return decimalDigits == 0 ? TYPES_INTEGER : "Types.DOUBLE";
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
  }

  private static final class ForeignKeyColumnPacker implements ResultPacker<ForeignKeyColumn> {
    @Override
    public ForeignKeyColumn fetch(final ResultSet resultSet) throws SQLException {
      return new ForeignKeyColumn(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
              resultSet.getString("FKTABLE_NAME"), resultSet.getString("FKCOLUMN_NAME"));
    }
  }

  private static final class PrimaryKeyColumnPacker implements ResultPacker<PrimaryKeyColumn> {
    @Override
    public PrimaryKeyColumn fetch(final ResultSet resultSet) throws SQLException {
      return new PrimaryKeyColumn(resultSet.getString("COLUMN_NAME"), resultSet.getInt("KEY_SEQ"));
    }
  }
}

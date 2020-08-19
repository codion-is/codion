/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a database table
 */
public final class Table {

  private final Schema schema;
  private final String tableName;
  private final Collection<ForeignKeyColumn> foreignKeyColumns;
  private final Map<String, Column> columns = new LinkedHashMap<>();
  private final Map<Table, ForeignKey> foreignKeys = new HashMap<>();

  Table(final Schema schema, final String tableName, final List<Column> columns, final Collection<ForeignKeyColumn> foreignKeyColumns) {
    this.schema = schema;
    this.tableName = tableName;
    columns.forEach(column -> this.columns.put(column.getColumnName(), column));
    this.foreignKeyColumns = foreignKeyColumns;
  }

  public String getTableName() {
    return tableName;
  }

  public Schema getSchema() {
    return schema;
  }

  public Map<String, Column> getColumns() {
    return columns;
  }

  public Map<Table, ForeignKey> getForeignKeys() {
    return foreignKeys;
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

  void resolveForeignKeys(final Map<String, Schema> schemas) {
    foreignKeyColumns.forEach(foreignKeyColumn -> {
      final Table referencedTable = schemas.get(foreignKeyColumn.getPkSchemaName()).getTables().get(foreignKeyColumn.getPkTableName());
      foreignKeys.computeIfAbsent(referencedTable, table -> new ForeignKey(referencedTable))
              .addReference(columns.get(foreignKeyColumn.getFkColumnName()), referencedTable.columns.get(foreignKeyColumn.getPkColumnName()));
    });
  }
}

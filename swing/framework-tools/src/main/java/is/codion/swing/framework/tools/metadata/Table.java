/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;

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
    return unmodifiableMap(columns);
  }

  public Collection<String> getReferencedSchemas() {
    return foreignKeyColumns.stream()
            .filter(foreignKeyColumn -> !foreignKeyColumn.getPkSchemaName().equals(schema.getName()))
            .map(ForeignKeyColumn::getPkSchemaName).collect(Collectors.toSet());
  }

  public Map<Table, ForeignKey> getForeignKeys() {
    return unmodifiableMap(foreignKeys);
  }

  @Override
  public String toString() {
    return schema.getName() + "." + tableName;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    final Table table = (Table) object;

    return Objects.equals(schema, table.getSchema()) && Objects.equals(tableName, table.getTableName());
  }

  @Override
  public int hashCode() {
    int result = schema != null ? schema.getName().hashCode() : 0;
    result = result + (tableName != null ? tableName.hashCode() : 0);

    return result;
  }

  void resolveForeignKeys(final Map<String, Schema> schemas) {
    Util.map(foreignKeyColumns, foreignKeyColumn ->
            getReferencedTable(foreignKeyColumn, schemas)).forEach((referencedTable, foreignKeyColumns) -> {
      final ForeignKey foreignKey = foreignKeys.computeIfAbsent(referencedTable, ForeignKey::new);
      foreignKeyColumns.forEach(foreignKeyColumn ->
              foreignKey.addReference(columns.get(foreignKeyColumn.getFkColumnName()),
                      referencedTable.columns.get(foreignKeyColumn.getPkColumnName())));
    });
  }

  private static Table getReferencedTable(final ForeignKeyColumn foreignKeyColumn, final Map<String, Schema> schemas) {
    final Table referencedTable = schemas.get(foreignKeyColumn.getPkSchemaName()).getTables().get(foreignKeyColumn.getPkTableName());
    if (referencedTable == null) {
      throw new IllegalStateException("Referenced table not found: " + foreignKeyColumn.getPkSchemaName() + "." + foreignKeyColumn.getPkTableName());
    }

    return referencedTable;
  }
}

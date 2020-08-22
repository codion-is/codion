/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Represents a database table
 */
public final class Table {

  private final Schema schema;
  private final String tableName;
  private final List<ForeignKeyColumn> foreignKeyColumns;
  private final Map<String, Column> columns = new LinkedHashMap<>();
  private final Map<Table, ForeignKey> foreignKeys = new LinkedHashMap<>();

  Table(final Schema schema, final String tableName, final List<Column> columns,
        final List<ForeignKeyColumn> foreignKeyColumns) {
    this.schema = requireNonNull(schema);
    this.tableName = requireNonNull(tableName);
    this.foreignKeyColumns = requireNonNull(foreignKeyColumns);
    requireNonNull(columns).forEach(column -> this.columns.put(column.getColumnName(), column));
  }

  public String getTableName() {
    return tableName;
  }

  public Schema getSchema() {
    return schema;
  }

  public List<Column> getColumns() {
    return unmodifiableList(new ArrayList<>(columns.values()));
  }

  public Collection<String> getReferencedSchemaNames() {
    return foreignKeyColumns.stream()
            .filter(foreignKeyColumn -> !foreignKeyColumn.getPkSchemaName().equals(schema.getName()))
            .map(ForeignKeyColumn::getPkSchemaName).collect(Collectors.toSet());
  }

  public List<ForeignKey> getForeignKeys() {
    return unmodifiableList(new ArrayList<>(foreignKeys.values()));
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
    return Objects.hash(schema, tableName);
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

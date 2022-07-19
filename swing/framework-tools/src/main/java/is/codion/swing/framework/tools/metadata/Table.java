/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * Represents a database table
 */
public final class Table {

  private final Schema schema;
  private final String tableName;
  private final List<ForeignKeyColumn> foreignKeyColumns;
  private final Map<String, Column> columns = new LinkedHashMap<>();
  private final List<ForeignKeyConstraint> foreignKeys = new ArrayList<>();

  Table(Schema schema, String tableName, List<Column> columns,
        List<ForeignKeyColumn> foreignKeyColumns) {
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
            .filter(this::referencesExternalSchema)
            .map(ForeignKeyColumn::getPkSchemaName)
            .collect(toSet());
  }

  public Collection<ForeignKeyConstraint> getForeignKeys() {
    return unmodifiableCollection(foreignKeys);
  }

  @Override
  public String toString() {
    return schema.getName() + "." + tableName;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    Table table = (Table) object;

    return Objects.equals(schema, table.getSchema()) && Objects.equals(tableName, table.getTableName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, tableName);
  }

  void resolveForeignKeys(Map<String, Schema> schemas) {
    for (ForeignKeyColumn foreignKeyColumn : foreignKeyColumns) {
      Table referencedTable = getReferencedTable(foreignKeyColumn, schemas);
      ForeignKeyConstraint foreignKeyConstraint;
      if (foreignKeyColumn.getKeySeq() == 1) {//new key
        foreignKeyConstraint = new ForeignKeyConstraint(referencedTable);
        foreignKeys.add(foreignKeyConstraint);
      }
      else {//add to previous
        foreignKeyConstraint = foreignKeys.get(foreignKeys.size() - 1);
      }
      foreignKeyConstraint.addReference(columns.get(foreignKeyColumn.getFkColumnName()),
              referencedTable.columns.get(foreignKeyColumn.getPkColumnName()));
    }
  }

  private boolean referencesExternalSchema(ForeignKeyColumn foreignKeyColumn) {
    return !foreignKeyColumn.getPkSchemaName().equals(schema.getName());
  }

  private static Table getReferencedTable(ForeignKeyColumn foreignKeyColumn, Map<String, Schema> schemas) {
    Table referencedTable = schemas.get(foreignKeyColumn.getPkSchemaName()).getTables().get(foreignKeyColumn.getPkTableName());
    if (referencedTable == null) {
      throw new IllegalStateException("Referenced table not found: " + foreignKeyColumn.getPkSchemaName() + "." + foreignKeyColumn.getPkTableName());
    }

    return referencedTable;
  }
}

/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

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
  private final String comment;
  private final List<ForeignKeyColumn> foreignKeyColumns;
  private final Map<String, MetadataColumn> columns = new LinkedHashMap<>();
  private final List<ForeignKeyConstraint> foreignKeys = new ArrayList<>();

  Table(Schema schema, String tableName, String comment,
        List<MetadataColumn> columns, List<ForeignKeyColumn> foreignKeyColumns) {
    this.schema = requireNonNull(schema);
    this.tableName = requireNonNull(tableName);
    this.comment = comment;
    this.foreignKeyColumns = requireNonNull(foreignKeyColumns);
    requireNonNull(columns).forEach(column -> this.columns.put(column.columnName(), column));
  }

  public String tableName() {
    return tableName;
  }

  public Schema schema() {
    return schema;
  }

  public String comment() {
    return comment;
  }

  public List<MetadataColumn> columns() {
    return unmodifiableList(new ArrayList<>(columns.values()));
  }

  public Collection<String> referencedSchemaNames() {
    return foreignKeyColumns.stream()
            .filter(this::referencesExternalSchema)
            .map(ForeignKeyColumn::pkSchemaName)
            .collect(toSet());
  }

  public Collection<ForeignKeyConstraint> foreignKeys() {
    return unmodifiableCollection(foreignKeys);
  }

  @Override
  public String toString() {
    return schema.name() + "." + tableName;
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

    return Objects.equals(schema, table.schema()) && Objects.equals(tableName, table.tableName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, tableName);
  }

  void resolveForeignKeys(Map<String, Schema> schemas) {
    for (ForeignKeyColumn foreignKeyColumn : foreignKeyColumns) {
      Table referencedTable = referencedTable(foreignKeyColumn, schemas);
      ForeignKeyConstraint foreignKeyConstraint;
      if (foreignKeyColumn.keySeq() == 1) {//new key
        foreignKeyConstraint = new ForeignKeyConstraint(referencedTable);
        foreignKeys.add(foreignKeyConstraint);
      }
      else {//add to previous
        foreignKeyConstraint = foreignKeys.get(foreignKeys.size() - 1);
      }
      foreignKeyConstraint.addReference(columns.get(foreignKeyColumn.fkColumnName()),
              referencedTable.columns.get(foreignKeyColumn.pkColumnName()));
    }
  }

  private boolean referencesExternalSchema(ForeignKeyColumn foreignKeyColumn) {
    return !foreignKeyColumn.pkSchemaName().equals(schema.name());
  }

  private static Table referencedTable(ForeignKeyColumn foreignKeyColumn, Map<String, Schema> schemas) {
    Table referencedTable = schemas.get(foreignKeyColumn.pkSchemaName()).tables().get(foreignKeyColumn.pkTableName());
    if (referencedTable == null) {
      throw new IllegalStateException("Referenced table not found: " + foreignKeyColumn.pkSchemaName() + "." + foreignKeyColumn.pkTableName());
    }

    return referencedTable;
  }
}

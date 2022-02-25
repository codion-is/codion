/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.event.EventDataListener;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public final class Schema {

  private final String name;
  private final Map<String, Table> tables = new HashMap<>();

  private boolean populated = false;

  Schema(final String name) {
    this.name = requireNonNull(name);
  }

  public String getName() {
    return name;
  }

  public Map<String, Table> getTables() {
    return unmodifiableMap(tables);
  }

  public void populate(final DatabaseMetaData metaData, final Map<String, Schema> schemas,
                       final EventDataListener<String> schemaNotifier) {
    if (!populated) {
      schemaNotifier.onEvent(name);
      try (final ResultSet resultSet = metaData.getTables(null, name, null, new String[] {"TABLE", "VIEW"})) {
        tables.putAll(new TablePacker(this, metaData, null).pack(resultSet).stream()
                .collect(toMap(Table::getTableName, table -> table)));
        tables.values().stream()
                .flatMap(table -> table.getReferencedSchemaNames().stream())
                .map(schemas::get)
                .forEach(schema -> schema.populate(metaData, schemas, schemaNotifier));
        tables.values().forEach(table -> table.resolveForeignKeys(schemas));
        populated = true;
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public boolean isPopulated() {
    return populated;
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
    Schema schema = (Schema) object;

    return name.equals(schema.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}

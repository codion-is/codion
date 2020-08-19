/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Schema {

  private final String name;
  private final Map<String, Table> tables = new HashMap<>();

  Schema(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map<String, Table> getTables() {
    return tables;
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

  void setTables(final List<Table> tables) {
    tables.forEach(table -> this.tables.put(table.getTableName(), table));
  }
}

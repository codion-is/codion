/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public final class Schema {

  private final String name;
  private final Map<String, Table> tables = new HashMap<>();

  private boolean populated = false;

  Schema(String name) {
    this.name = requireNonNull(name);
  }

  public String name() {
    return name;
  }

  public Map<String, Table> tables() {
    return unmodifiableMap(tables);
  }

  public void populate(DatabaseMetaData metaData, Map<String, Schema> schemas,
                       Consumer<String> schemaNotifier) {
    schemaNotifier.accept(name);
    tables.clear();
    try (ResultSet resultSet = metaData.getTables(null, name, null, new String[] {"TABLE", "VIEW"})) {
      tables.putAll(new TablePacker(this, metaData, null).pack(resultSet).stream()
              .collect(toMap(Table::tableName, Function.identity())));
      tables.values().stream()
              .flatMap(table -> table.referencedSchemaNames().stream())
              .map(schemas::get)
              .forEach(schema -> schema.populate(metaData, schemas, schemaNotifier));
      tables.values().forEach(table -> table.resolveForeignKeys(schemas));
      populated = true;
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean populated() {
    return populated;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object object) {
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

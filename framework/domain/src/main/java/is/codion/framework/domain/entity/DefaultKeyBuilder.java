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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Entity.Key.Builder {

  private final EntityDefinition definition;
  private final Map<Column<?>, Object> values = new HashMap<>();

  private boolean primaryKey = true;

  DefaultKeyBuilder(Entity.Key key) {
    this(key.entityDefinition());
    this.primaryKey = key.isPrimaryKey();
    key.columns().forEach(column -> values.put(column, key.get(column)));
  }

  DefaultKeyBuilder(EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Entity.Key.Builder with(Column<T> column, T value) {
    ColumnDefinition<T> columnDefinition = definition.columns().definition(column);
    if (!columnDefinition.isPrimaryKeyColumn()) {
      primaryKey = false;
    }
    values.put(column, value);

    return this;
  }

  @Override
  public Entity.Key build() {
    return new DefaultKey(definition, initializeValues(new HashMap<>(values)), primaryKey);
  }

  private Map<Column<?>, Object> initializeValues(Map<Column<?>, Object> values) {
    if (primaryKey && !values.isEmpty()) {
      //populate any missing primary key attributes with null values,
      //DefaultKey.equals() relies on the key attributes being present
      definition.primaryKey().columns().forEach(attribute -> values.putIfAbsent(attribute, null));
    }

    return values;
  }
}

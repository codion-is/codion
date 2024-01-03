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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;

final class DenormalizedValueProvider<T> implements DerivedAttribute.Provider<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<Entity> entityAttribute;
  private final Attribute<T> denormalizedAttribute;

  DenormalizedValueProvider(Attribute<Entity> entityAttribute, Attribute<T> denormalizedAttribute) {
    this.entityAttribute = entityAttribute;
    this.denormalizedAttribute = denormalizedAttribute;
  }

  @Override
  public T get(DerivedAttribute.SourceValues sourceValues) {
    Entity foreignKeyValue = sourceValues.get(entityAttribute);

    return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
  }
}

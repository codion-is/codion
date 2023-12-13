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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultAllCondition extends AbstractCondition implements Condition.All, Serializable {

  private static final long serialVersionUID = 1;

  DefaultAllCondition(EntityType entityType) {
    super(entityType, emptyList(), emptyList());
  }

  @Override
  public String toString(EntityDefinition definition) {
    requireNonNull(definition);
    return "";
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof All)) {
      return false;
    }
    All that = (All) object;
    return Objects.equals(entityType(), that.entityType());
  }

  @Override
  public int hashCode() {
    return entityType().hashCode();
  }

  @Override
  public String toString() {
    return "DefaultAllCondition{entityType=" + entityType() + "}";
  }
}

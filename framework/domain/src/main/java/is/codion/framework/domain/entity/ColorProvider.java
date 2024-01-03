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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;

import java.io.Serializable;

/**
 * Provides background/foreground colors for entities.
 */
public interface ColorProvider extends Serializable {

  /**
   * Returns the Object representing the specific color to use for the given attribute
   * in the given entity, null in case of no specific color
   * @param entity the entity
   * @param attribute the attribute
   * @return the color to use for this entity and attribute, null if no color is specified
   */
  Object color(Entity entity, Attribute<?> attribute);
}

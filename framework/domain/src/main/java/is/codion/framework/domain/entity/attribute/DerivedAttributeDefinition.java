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

import java.util.List;

/**
 * A definition for attributes which value is derived from the values of one or more attribute.
 * @param <T> the underlying type
 */
public interface DerivedAttributeDefinition<T> extends AttributeDefinition<T> {

  /**
   * @return the attributes this attribute derives from.
   */
  List<Attribute<?>> sourceAttributes();

  /**
   * @return the value provider, providing the derived value
   */
  DerivedAttribute.Provider<T> valueProvider();
}

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

import java.io.Serializable;
import java.util.Optional;

public interface DerivedAttribute<T> extends Attribute<T> {

  /**
   * Provides the source values from which to derive the value.
   */
  interface SourceValues {

    /**
     * Returns the source value associated with the given attribute.
     * @param attribute the attribute which value to retrieve
     * @param <T> the value type
     * @return the value associated with attribute
     */
    <T> T get(Attribute<T> attribute);

    /**
     * Returns the source value associated with the given attribute.
     * @param attribute the attribute which value to retrieve
     * @param <T> the value type
     * @return the value associated with attribute, an empty Optional in case of null
     */
    default <T> Optional<T> optional(Attribute<T> attribute) {
      return Optional.ofNullable(get(attribute));
    }
  }

  /**
   * Responsible for providing values derived from other values
   * @param <T> the underlying type
   */
  interface Provider<T> extends Serializable {

    /**
     * @param sourceValues the source values, mapped to their respective attributes
     * @return the derived value
     */
    T get(SourceValues sourceValues);
  }
}

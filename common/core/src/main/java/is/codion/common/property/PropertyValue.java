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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.property;

import is.codion.common.value.Value;

/**
 * A Value associated with a named property.
 * @param <T> the value type
 */
public interface PropertyValue<T> extends Value<T> {

  /**
   * @return the name of the property this value represents
   */
  String propertyName();

  /**
   * Returns the underlying value, if the value is null then a {@link IllegalStateException} is thrown.
   * @return the value, if available
   * @throws IllegalStateException in case the underlying value is null
   */
  T getOrThrow() throws IllegalStateException;

  /**
   * Returns the underlying value, if the value is null then a {@link IllegalStateException} is thrown.
   * @param message the error message to use when throwing
   * @return the value, if available
   * @throws IllegalStateException in case the underlying value is null
   */
  T getOrThrow(String message) throws IllegalStateException;

  /**
   * Sets this value to null as well as removing it from the underlying store and clearing the system property.
   */
  void clear();
}

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
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.attribute.Attribute;

import static java.util.Objects.requireNonNull;

/**
 * An exception used to indicate that an attribute value is invalid.
 */
public class ValidationException extends Exception {

  private final Attribute<?> attribute;
  private final Object value;

  /**
   * Instantiates a new ValidationException.
   * @param attribute the attribute of the value being validated
   * @param value the value
   * @param message the exception message
   */
  public ValidationException(Attribute<?> attribute, Object value, String message) {
    super(message);
    this.attribute = requireNonNull(attribute, "attribute");
    this.value = value;
  }

  /**
   * @return the value attribute
   */
  public final Attribute<?> attribute() {
    return attribute;
  }

  /**
   * @return the invalid value
   */
  public final Object value() {
    return value;
  }
}

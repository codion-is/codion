/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.Attribute;

/**
 * An exception used to indicate that an item value is invalid.
 */
public final class ItemValidationException extends ValidationException {

  /**
   * Instantiates a new ItemValidationException.
   * @param attribute the attribute of the value being validated
   * @param value the value
   * @param message the exception message
   */
  public ItemValidationException(Attribute<?> attribute, Object value, String message) {
    super(attribute, value, message);
  }
}

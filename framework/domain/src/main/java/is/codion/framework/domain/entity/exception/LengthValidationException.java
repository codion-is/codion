/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.attribute.Attribute;

/**
 * An exception used to indicate that a value associated with a key exceeds the allowed length.
 */
public class LengthValidationException extends ValidationException {

  /**
   * Instantiates a new LengthValidationException
   * @param attribute the attribute
   * @param value the value that exceeds the allowed length
   * @param message the message
   */
  public LengthValidationException(final Attribute<?> attribute, final Object value, final String message) {
    super(attribute, value, message);
  }
}

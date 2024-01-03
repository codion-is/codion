/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.attribute.Attribute;

/**
 * An exception used to indicate that a value associated with
 * a key which not fall within the allowed range of values.
 */
public class RangeValidationException extends ValidationException {

  /**
   * Instantiates a new RangeValidationException
   * @param attribute the attribute
   * @param value the value that is out of range
   * @param message the message
   */
  public RangeValidationException(Attribute<?> attribute, Object value, String message) {
    super(attribute, value, message);
  }
}

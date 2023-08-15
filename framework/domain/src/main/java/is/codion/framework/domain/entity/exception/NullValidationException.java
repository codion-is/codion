/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.attribute.Attribute;

/**
 * An exception used to indicate that a null value was being associated with
 * a key which does not allow null values.
 */
public class NullValidationException extends ValidationException {

  /**
   * Instantiates a new NullValidationException
   * @param attribute the attribute with which the null value is associated
   * @param message the message
   */
  public NullValidationException(Attribute<?> attribute, String message) {
    super(attribute, null, message);
  }
}

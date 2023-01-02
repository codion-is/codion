/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.Attribute;

import static java.util.Objects.requireNonNull;

/**
 * An exception used to indicate that an invalid value is being associated with a key.
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

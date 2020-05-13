/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.entity.exception;

import static java.util.Objects.requireNonNull;

/**
 * An exception used to indicate that an invalid value is being associated with a key.
 */
public class ValidationException extends Exception {

  private final String propertyId;
  private final Object value;

  /**
   * Instantiates a new ValidationException.
   * @param propertyId the key of the value being validated
   * @param value the value
   * @param message the exception message
   */
  public ValidationException(final String propertyId, final Object value, final String message) {
    super(message);
    this.propertyId = requireNonNull(propertyId, "propertyId");
    this.value = value;
  }

  /**
   * @return the value property id
   */
  public final String getPropertyId() {
    return propertyId;
  }

  /**
   * @return the value
   */
  public final Object getValue() {
    return value;
  }
}

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.entity.exception;

/**
 * An exception used to indicate that a value associated with
 * a key which not fall within the allowed range of values.
 */
public class RangeValidationException extends ValidationException {

  /**
   * Instantiates a new RangeValidationException
   * @param propertyId the propertyId
   * @param value the value that is out of range
   * @param message the message
   */
  public RangeValidationException(final String propertyId, final Object value, final String message) {
    super(propertyId, value, message);
  }
}

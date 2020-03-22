/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity.exception;

/**
 * An exception used to indicate that a value associated with a key exceeds the allowed length.
 */
public class LengthValidationException extends ValidationException {

  /**
   * Instantiates a new LengthValidationException
   * @param propertyId the propertyId
   * @param value the value that exceeds the allowed length
   * @param message the message
   */
  public LengthValidationException(final String propertyId, final Object value, final String message) {
    super(propertyId, value, message);
  }
}

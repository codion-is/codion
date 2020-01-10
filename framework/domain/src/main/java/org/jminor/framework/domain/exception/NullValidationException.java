/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.exception;

/**
 * An exception used to indicate that a null value was being associated with
 * a key which does not allow null values.
 */
public class NullValidationException extends ValidationException {

  /**
   * Instantiates a new NullValidationException
   * @param propertyId the propertyId with which the null value is associated
   * @param message the message
   */
  public NullValidationException(final String propertyId, final String message) {
    super(propertyId, null, message);
  }
}

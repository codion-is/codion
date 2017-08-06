/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap.exception;

/**
 * An exception used to indicate that a value was being associated with
 * a key which does not fall within the allowed range of values.
 */
public class RangeValidationException extends ValidationException {

  /**
   * Instantiates a new RangeValidationException
   * @param key the key
   * @param value the value that is out of range
   * @param message the message
   */
  public RangeValidationException(final Object key, final Object value, final String message) {
    super(key, value, message);
  }
}

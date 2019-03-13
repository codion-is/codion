/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap.exception;

/**
 * An exception used to indicate that a value associated with a key exceeds the allowed length.
 */
public class LengthValidationException extends ValidationException {

  /**
   * Instantiates a new LengthValidationException
   * @param key the key
   * @param value the value that exceeds the allowed length
   * @param message the message
   */
  public LengthValidationException(final Object key, final Object value, final String message) {
    super(key, value, message);
  }
}

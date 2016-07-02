/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap.exception;

/**
 * An exception used to indicate that a null value was being associated with
 * a key which does not allow null values.
 */
public class NullValidationException extends ValidationException {

  /**
   * Instantiates a new NullValidationException
   * @param key the key with which the null value is associated
   * @param message the message
   */
  public NullValidationException(final Object key, final String message) {
    super(key, null, message);
  }
}

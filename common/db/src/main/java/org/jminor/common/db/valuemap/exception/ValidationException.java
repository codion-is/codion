/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap.exception;

/**
 * An exception used to indicate that an invalid value is being associated with a key.
 */
public class ValidationException extends Exception {

  private final Object key;
  private final Object value;

  /**
   * Instantiates a new ValidationException.
   * @param message the exception message
   */
  public ValidationException(final String message) {
    this(null, null, message);
  }

  /**
   * Instantiates a new ValidationException.
   * @param key the key of the value being validated
   * @param value the value
   * @param message the exception message
   */
  public ValidationException(final Object key, final Object value, final String message) {
    super(message);
    this.key = key;
    this.value = value;
  }

  /**
   * @return the value key
   */
  public final Object getKey() {
    return key;
  }

  /**
   * @return the value
   */
  public final Object getValue() {
    return value;
  }
}

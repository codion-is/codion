/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap.exception;

/**
 * An exception used to indicate that an invalid value is being associated with a key.
 */
public class ValidationException extends Exception {

  private final Object key;
  private final Object value;

  public ValidationException(final String message) {
    this(null, null, message);
  }

  public ValidationException(final Object key, final Object value, final String message) {
    super(message);
    this.key = key;
    this.value = value;
  }

  public Object getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }
}

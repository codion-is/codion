/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap.exception;

/**
 * An exception used to indicate that a value was being associated with
 * a key which does not fall within the allowed range of values.
 */
public class RangeValidationException extends ValidationException {

  public RangeValidationException(final Object key, final Object value, final String message) {
    super(key, value, message);
  }
}

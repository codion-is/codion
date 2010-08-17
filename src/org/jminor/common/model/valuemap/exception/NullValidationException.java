/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap.exception;

/**
 * An exception used to indicate that a null value was being associated with
 * a key which does not allow null values.
 */
public class NullValidationException extends ValidationException {

  public NullValidationException(final Object key, final String message) {
    super(key, null, message);
  }
}

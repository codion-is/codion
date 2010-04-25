/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap.exception;

/**
 * User: Björn Darri<br>
 * Date: 20.4.2010<br>
 * Time: 23:15:02<br>
 */
public class RangeValidationException extends ValidationException {

  public RangeValidationException(final Object key, final Object value, final String message) {
    super(key, value, message);
  }
}

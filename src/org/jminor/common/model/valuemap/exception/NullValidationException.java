/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap.exception;

/**
 * User: Björn Darri<br>
 * Date: 20.4.2010<br>
 * Time: 23:13:09<br>
 */
public class NullValidationException extends ValidationException {

  public NullValidationException(final Object key, final String message) {
    super(key, null, message);
  }
}

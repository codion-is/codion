/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.exception;

import org.jminor.framework.domain.Property;

/**
 * User: Björn Darri<br>
 * Date: 20.4.2010<br>
 * Time: 23:15:02<br>
 */
public class RangeValidationException extends ValidationException {

  public RangeValidationException(final Property property, final Object value, final String message) {
    super(property, value, message);
  }
}

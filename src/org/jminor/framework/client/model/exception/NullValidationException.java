/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.exception;

import org.jminor.framework.domain.Property;

/**
 * User: Björn Darri<br>
 * Date: 20.4.2010<br>
 * Time: 23:13:09<br>
 */
public class NullValidationException extends ValidationException {

  public NullValidationException(final Property property, final String message) {
    super(property, null, message);
  }
}

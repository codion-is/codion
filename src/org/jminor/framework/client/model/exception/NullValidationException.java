/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.exception;

import org.jminor.framework.domain.Property;

/**
 * User: Björn Darri
 * Date: 20.4.2010
 * Time: 23:13:09
 */
public class NullValidationException extends ValidationException {

  public NullValidationException(final Property property, final String message) {
    super(property, null, message);
  }
}

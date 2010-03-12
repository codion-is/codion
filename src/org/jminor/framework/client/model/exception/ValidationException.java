/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.exception;

import org.jminor.framework.domain.Property;

public class ValidationException extends Exception {

  private final Property property;
  private final Object value;

  public ValidationException(final String message) {
    this(null, null, message);
  }

  public ValidationException(final Property property, final Object value, final String message) {
    super(message);
    this.property = property;
    this.value = value;
  }

  public Property getProperty() {
    return property;
  }

  public Object getValue() {
    return value;
  }
}

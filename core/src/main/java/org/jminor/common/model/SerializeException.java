/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An exception occurring during serialization
 */
public final class SerializeException extends Exception {

  /**
   * Instantiates a new SerializeException.
   * @param message the message
   * @param cause the root cause
   */
  public SerializeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

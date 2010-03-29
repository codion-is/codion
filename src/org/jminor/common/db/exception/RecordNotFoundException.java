/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

/**
 * Exception class to be used when an expected record was not found
 */
public class RecordNotFoundException extends DbException {

  public RecordNotFoundException(final String message) {
    super(message);
  }
}

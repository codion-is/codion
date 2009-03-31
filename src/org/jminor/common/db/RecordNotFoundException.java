/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Exception class to be used when an expected record was not found
 */
public class RecordNotFoundException extends DbException {

  public RecordNotFoundException(final String message) {
    super(message);
  }
}

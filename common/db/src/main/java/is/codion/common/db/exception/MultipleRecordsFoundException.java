/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

/**
 * Exception used when one record was expected but many were found.
 */
public class MultipleRecordsFoundException extends DatabaseException {

  /**
   * Instantiates a new MultipleRecordsFoundException
   * @param message the exception message
   */
  public MultipleRecordsFoundException(String message) {
    super(message);
  }
}

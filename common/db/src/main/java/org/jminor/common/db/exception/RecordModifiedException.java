/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

/**
 * An exception indicating that the row in question has been modified or deleted since it was loaded.
 */
public final class RecordModifiedException extends UpdateException {

  private final Object row;
  private final Object modifiedRow;

  /**
   * Instantiates a new RecordModifiedException
   * @param row the row being updated
   * @param modifiedRow the current (modified) version of the row, null if it has been deleted
   * @param message a message describing the modification
   */
  public RecordModifiedException(final Object row, final Object modifiedRow, final String message) {
    super(message);
    this.row = row;
    this.modifiedRow = modifiedRow;
  }

  /**
   * @return the row being updated
   */
  public Object getRow() {
    return row;
  }

  /**
   * @return the current (modified) version of the row, null if it has been deleted
   */
  public Object getModifiedRow() {
    return modifiedRow;
  }
}

/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.jminor.common.i18n.Messages;

/**
 * An exception indicating that the row in question has been modified or deleted since it was loaded.
 */
public final class RecordModifiedException extends UpdateException {

  private static final String MESSAGE = Messages.get(Messages.RECORD_MODIFIED_EXCEPTION);
  private final Object row;
  private final Object modifiedRow;

  /**
   * Instantiates a new RecordModifiedException
   * @param row the row being updated
   * @param modifiedRow the current (modified) version of the row, null if it has been deleted
   */
  public RecordModifiedException(final Object row, final Object modifiedRow) {
    super(MESSAGE);
    this.row = row;
    this.modifiedRow = modifiedRow;
  }

  /**
   * @return the row bing updated
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

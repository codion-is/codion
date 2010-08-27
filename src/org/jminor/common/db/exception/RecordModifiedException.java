/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.jminor.common.i18n.Messages;

/**
 * An exception indicating that the row in question has been modified since it was loaded.
 */
public final class RecordModifiedException extends DbException {

  private final Object row;
  private final Object modifiedRow;

  /**
   * Instantiates a new RecordModifiedException
   * @param row the row being updated
   * @param modifiedRow the current (modified) version of the row
   */
  public RecordModifiedException(final Object row, final Object modifiedRow) {
    super(Messages.get(Messages.RECORD_MODIFIED_EXCEPTION));
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
   * @return the current (modified) version of the row
   */
  public Object getModifiedRow() {
    return modifiedRow;
  }
}

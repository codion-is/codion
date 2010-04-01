/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.jminor.common.i18n.Messages;

/**
 * An exception indicating that the row in question has been modified since it was loaded.
 * User: Bjorn Darri
 * Date: 26.9.2009
 * Time: 15:35:18
 */
public class RecordModifiedException extends DbException {

  private final Object row;
  private final Object modifiedRow;

  public RecordModifiedException(final Object row, final Object modifiedRow) {
    super(Messages.get(Messages.RECORD_MODIFIED_EXCEPTION));
    this.row = row;
    this.modifiedRow = modifiedRow;
  }

  public Object getRow() {
    return row;
  }

  public Object getModifiedRow() {
    return modifiedRow;
  }
}

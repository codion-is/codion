/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.jminor.common.i18n.Messages;

/**
 * An exception indicating that the row in question has been modified since it was loaded.<br>
 * User: Bjorn Darri<br>
 * Date: 26.9.2009<br>
 * Time: 15:35:18<br>
 */
public final class RecordModifiedException extends DbException {

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

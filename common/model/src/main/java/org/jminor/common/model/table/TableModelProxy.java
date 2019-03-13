/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventDataListener;

import java.util.List;

/**
 * A proxy for connecting to a table model
 * @param <R> the row type
 */
public interface TableModelProxy<R> {
  /**
   * @return the size of the table model
   */
  int getRowCount();

  /**
   * @param item the item
   * @return the index of the item in the table model
   */
  int indexOf(final R item);

  /**
   * @param index the index
   * @return the item at the given index in the table model
   */
  R getItemAt(final int index);

  /**
   * @return true if an impending selection change should be allowed
   */
  boolean allowSelectionChange();

  /**
   * Adds a listener that is notified each time rows are deleted from the data model.
   * @param listener the listener, the data list contains the fromIndex and toIndex as items at index 0 and 1
   */
  void addRowsDeletedListener(final EventDataListener<List<Integer>> listener);
}

/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import javax.swing.event.TableModelListener;

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
   * Adds a listener to the list that's notified each time a change to the data model occurs.
   * @param listener the listener
   */
  void addTableModelListener(TableModelListener listener);
}

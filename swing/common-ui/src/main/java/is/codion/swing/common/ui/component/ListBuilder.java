/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 * Builds a single-selection JList.
 * @param <T> the value type
 */
public interface ListBuilder<T> extends ComponentBuilder<T, JList<T>, ListBuilder<T>> {

  /**
   * @param visibleRowCount the visible row count
   * @return this builder instance
   */
  ListBuilder<T> visibleRowCount(int visibleRowCount);

  /**
   * @param layoutOrientation the list layout orientation
   * @return thi builder instance
   */
  ListBuilder<T> layoutOrientation(int layoutOrientation);

  /**
   * @param fixedCellHeight the fixed cell height
   * @return this builder instance
   */
  ListBuilder<T> fixedCellHeight(int fixedCellHeight);

  /**
   * @param fixedCellWidth the fixed cell width
   * @return this builder instance
   */
  ListBuilder<T> fixedCellWidth(int fixedCellWidth);

  /**
   * @param cellRenderer the cell renderer
   * @return this builder instance
   */
  ListBuilder<T> cellRenderer(ListCellRenderer<T> cellRenderer);

  /**
   * @param selectionMode the list selection model
   * @return this builder instance
   */
  ListBuilder<T> selectionMode(int selectionMode);

  /**
   * @param listSelectionModel the list selection model
   * @return this builder instance
   */
  ListBuilder<T> listSelectionModel(ListSelectionModel listSelectionModel);

  /**
   * @param listSelectionListener the list selection listener
   * @return this builder instance
   */
  ListBuilder<T> listSelectionListener(ListSelectionListener listSelectionListener);
}

/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JList;

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
}

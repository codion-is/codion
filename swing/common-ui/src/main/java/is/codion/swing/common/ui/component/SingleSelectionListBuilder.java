/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JList;
import javax.swing.JScrollPane;
import java.util.function.Consumer;

/**
 * Builds a single-selection JList.
 * @param <T> the value type
 */
public interface SingleSelectionListBuilder<T> extends ComponentBuilder<T, JList<T>, SingleSelectionListBuilder<T>> {

  /**
   * @param visibleRowCount the visible row count
   * @return this builder instance
   */
  SingleSelectionListBuilder<T> visibleRowCount(int visibleRowCount);

  /**
   * @param layoutOrientation the list layout orientation
   * @return thi builder instance
   */
  SingleSelectionListBuilder<T> layoutOrientation(int layoutOrientation);

  /**
   * @param fixedCellHeight the fixed cell height
   * @return this builder instance
   */
  SingleSelectionListBuilder<T> fixedCellHeight(int fixedCellHeight);

  /**
   * @param fixedCellWidth the fixed cell width
   * @return this builder instance
   */
  SingleSelectionListBuilder<T> fixedCellWidth(int fixedCellWidth);

  /**
   * Builds the list and returns a scroll pane containing it, note that subsequent calls return the same scroll pane.
   * @return a scroll pane containing the list
   */
  JScrollPane buildScrollPane();

  /**
   * Builds the list and returns a scroll pane containing it
   * @param onBuild called after the first call when the component is built, not called on subsequent calls.
   * @return a scroll pane containing the list
   */
  JScrollPane buildScrollPane(Consumer<JScrollPane> onBuild);
}

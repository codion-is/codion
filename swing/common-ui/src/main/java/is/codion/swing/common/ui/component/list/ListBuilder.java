/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.value.ValueSet;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.util.Set;

/**
 * Builds a multi-selection JList.
 * @param <T> the value type
 */
public interface ListBuilder<T> extends ComponentBuilder<Set<T>, JList<T>, ListBuilder<T>> {

  /**
   * @param visibleRowCount the visible row count
   * @return this builder instance
   * @see JList#setVisibleRowCount(int)
   */
  ListBuilder<T> visibleRowCount(int visibleRowCount);

  /**
   * @param layoutOrientation the list layout orientation
   * @return thi builder instance
   * @see JList#setLayoutOrientation(int)
   */
  ListBuilder<T> layoutOrientation(int layoutOrientation);

  /**
   * @param fixedCellHeight the fixed cell height
   * @return this builder instance
   * @see JList#setFixedCellHeight(int)
   */
  ListBuilder<T> fixedCellHeight(int fixedCellHeight);

  /**
   * @param fixedCellWidth the fixed cell width
   * @return this builder instance
   * @see JList#setFixedCellWidth(int)
   */
  ListBuilder<T> fixedCellWidth(int fixedCellWidth);

  /**
   * @param cellRenderer the cell renderer
   * @return this builder instance
   * @see JList#setCellRenderer(ListCellRenderer)
   */
  ListBuilder<T> cellRenderer(ListCellRenderer<T> cellRenderer);

  /**
   * @param selectionMode the list selection model
   * @return this builder instance
   * @see JList#setSelectionMode(int)
   */
  ListBuilder<T> selectionMode(int selectionMode);

  /**
   * @param selectionModel the list selection model
   * @return this builder instance
   * @see JList#setSelectionModel(ListSelectionModel)
   */
  ListBuilder<T> selectionModel(ListSelectionModel selectionModel);

  /**
   * @param listSelectionListener the list selection listener
   * @return this builder instance
   * @see JList#addListSelectionListener(ListSelectionListener)
   */
  ListBuilder<T> listSelectionListener(ListSelectionListener listSelectionListener);

  /**
   * @param <T> the list element type
   * @param listModel the list model
   * @return a new builder instance
   */
  static <T> ListBuilder<T> builder(ListModel<T> listModel) {
    return new DefaultListBuilder<>(listModel, null);
  }

  /**
   * @param <T> the list element type
   * @param listModel the list model
   * @param linkedValueSet value set to link to the component
   * @return a new builder instance
   */
  static <T> ListBuilder<T> builder(ListModel<T> listModel, ValueSet<T> linkedValueSet) {
    return new DefaultListBuilder<>(listModel, linkedValueSet);
  }
}

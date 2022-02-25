/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import static java.util.Objects.requireNonNull;

final class DefaultListBuilder<T> extends AbstractComponentBuilder<T, JList<T>, ListBuilder<T>> implements ListBuilder<T> {

  private final ListModel<T> listModel;

  private int visibleRowCount;
  private int layoutOrientation = JList.VERTICAL;
  private int fixedCellHeight = -1;
  private int fixedCellWidth = -1;

  DefaultListBuilder(ListModel<T> listModel, Value<T> linkedValue) {
    super(linkedValue);
    this.listModel = requireNonNull(listModel);
  }

  @Override
  public ListBuilder<T> visibleRowCount(int visibleRowCount) {
    this.visibleRowCount = visibleRowCount;
    return this;
  }

  @Override
  public ListBuilder<T> layoutOrientation(int layoutOrientation) {
    this.layoutOrientation = layoutOrientation;
    return this;
  }

  @Override
  public ListBuilder<T> fixedCellHeight(int fixedCellHeight) {
    this.fixedCellHeight = fixedCellHeight;
    return this;
  }

  @Override
  public ListBuilder<T> fixedCellWidth(int fixedCellWidth) {
    this.fixedCellWidth = fixedCellWidth;
    return this;
  }

  @Override
  protected JList<T> buildComponent() {
    JList<T> list = new JList<>(listModel);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(visibleRowCount);
    list.setLayoutOrientation(layoutOrientation);
    list.setFixedCellHeight(fixedCellHeight);
    list.setFixedCellWidth(fixedCellWidth);

    return list;
  }

  @Override
  protected ComponentValue<T, JList<T>> buildComponentValue(JList<T> component) {
    return ComponentValues.list(component);
  }

  @Override
  protected void setInitialValue(JList<T> component, T initialValue) {
    component.setSelectedValue(initialValue, true);
  }
}

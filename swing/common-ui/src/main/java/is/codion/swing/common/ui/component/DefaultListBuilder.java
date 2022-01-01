/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultListBuilder<T> extends AbstractComponentBuilder<T, JList<T>, ListBuilder<T>> implements ListBuilder<T> {

  private final ListModel<T> listModel;

  private int visibleRowCount;
  private int layoutOrientation = JList.VERTICAL;
  private int fixedCellHeight = -1;
  private int fixedCellWidth = -1;

  private JScrollPane scrollPane;

  DefaultListBuilder(final ListModel<T> listModel, final Value<T> linkedValue) {
    super(linkedValue);
    this.listModel = requireNonNull(listModel);
  }

  @Override
  public ListBuilder<T> visibleRowCount(final int visibleRowCount) {
    this.visibleRowCount = visibleRowCount;
    return this;
  }

  @Override
  public ListBuilder<T> layoutOrientation(final int layoutOrientation) {
    this.layoutOrientation = layoutOrientation;
    return this;
  }

  @Override
  public ListBuilder<T> fixedCellHeight(final int fixedCellHeight) {
    this.fixedCellHeight = fixedCellHeight;
    return this;
  }

  @Override
  public ListBuilder<T> fixedCellWidth(final int fixedCellWidth) {
    this.fixedCellWidth = fixedCellWidth;
    return this;
  }

  @Override
  protected JList<T> buildComponent() {
    final JList<T> list = new JList<>(listModel);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(visibleRowCount);
    list.setLayoutOrientation(layoutOrientation);
    list.setFixedCellHeight(fixedCellHeight);
    list.setFixedCellWidth(fixedCellWidth);

    return list;
  }

  @Override
  public JScrollPane buildScrollPane() {
    return buildScrollPane(null);
  }

  @Override
  public JScrollPane buildScrollPane(final Consumer<JScrollPane> onBuild) {
    if (scrollPane == null) {
      scrollPane = new JScrollPane(build());
      if (onBuild != null) {
        onBuild.accept(scrollPane);
      }
    }

    return scrollPane;
  }

  @Override
  protected ComponentValue<T, JList<T>> buildComponentValue(final JList<T> component) {
    return ComponentValues.list(component);
  }

  @Override
  protected void setInitialValue(final JList<T> component, final T initialValue) {
    component.setSelectedValue(initialValue, true);
  }
}

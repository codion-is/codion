/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultSingleSelectionListBuilder<T> extends AbstractComponentBuilder<T, JList<T>, SingleSelectionListBuilder<T>>
        implements SingleSelectionListBuilder<T> {

  private final ListModel<T> listModel;

  private int visibleRowCount;
  private int layoutOrientation = JList.VERTICAL;
  private int fixedCellHeight = -1;
  private int fixedCellWidth = -1;

  private JScrollPane scrollPane;

  DefaultSingleSelectionListBuilder(final ListModel<T> listModel) {
    this.listModel = requireNonNull(listModel);
  }

  @Override
  public SingleSelectionListBuilder<T> visibleRowCount(final int visibleRowCount) {
    this.visibleRowCount = visibleRowCount;
    return this;
  }

  @Override
  public SingleSelectionListBuilder<T> layoutOrientation(final int layoutOrientation) {
    this.layoutOrientation = layoutOrientation;
    return this;
  }

  @Override
  public SingleSelectionListBuilder<T> fixedCellHeight(final int fixedCellHeight) {
    this.fixedCellHeight = fixedCellHeight;
    return this;
  }

  @Override
  public SingleSelectionListBuilder<T> fixedCellWidth(final int fixedCellWidth) {
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
    return ComponentValues.listSingleSelection(component);
  }

  @Override
  protected void setInitialValue(final JList<T> component, final T initialValue) {
    component.setSelectedValue(initialValue, true);
  }
}

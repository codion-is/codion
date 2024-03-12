/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.value.ValueSet;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultListBuilder<T> extends AbstractComponentBuilder<Set<T>, JList<T>, ListBuilder<T>> implements ListBuilder<T> {

  private final ListModel<T> listModel;
  private final List<ListSelectionListener> listSelectionListeners = new ArrayList<>();

  private ListCellRenderer<T> cellRenderer;
  private ListSelectionModel selectionModel;

  private int visibleRowCount;
  private int layoutOrientation = JList.VERTICAL;
  private int fixedCellHeight = -1;
  private int fixedCellWidth = -1;
  private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

  DefaultListBuilder(ListModel<T> listModel, ValueSet<T> linkedValueSet) {
    super(linkedValueSet);
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
  public ListBuilder<T> cellRenderer(ListCellRenderer<T> cellRenderer) {
    this.cellRenderer = requireNonNull(cellRenderer);
    return this;
  }

  @Override
  public ListBuilder<T> selectionMode(int selectionMode) {
    this.selectionMode = selectionMode;
    return this;
  }

  @Override
  public ListBuilder<T> selectionModel(ListSelectionModel selectionModel) {
    this.selectionModel = requireNonNull(selectionModel);
    return this;
  }

  @Override
  public ListBuilder<T> listSelectionListener(ListSelectionListener listSelectionListener) {
    this.listSelectionListeners.add(requireNonNull(listSelectionListener));
    return this;
  }

  @Override
  protected JList<T> createComponent() {
    JList<T> list = new JList<>(listModel);
    if (cellRenderer != null) {
      list.setCellRenderer(cellRenderer);
    }
    if (selectionModel != null) {
      list.setSelectionModel(selectionModel);
    }
    listSelectionListeners.forEach(new AddListSelectionListener(list));
    list.setVisibleRowCount(visibleRowCount);
    list.setLayoutOrientation(layoutOrientation);
    list.setFixedCellHeight(fixedCellHeight);
    list.setFixedCellWidth(fixedCellWidth);
    list.setSelectionMode(selectionMode);

    return list;
  }

  @Override
  protected ComponentValue<Set<T>, JList<T>> createComponentValue(JList<T> component) {
    return new ListValue<>(component);
  }

  @Override
  protected void setInitialValue(JList<T> component, Set<T> initialValue) {
    ListValue.selectValues(component, initialValue);
  }

  private static final class AddListSelectionListener implements Consumer<ListSelectionListener> {

    private final JList<?> list;

    private AddListSelectionListener(JList<?> list) {
      this.list = list;
    }

    @Override
    public void accept(ListSelectionListener listener) {
      list.addListSelectionListener(listener);
    }
  }
}

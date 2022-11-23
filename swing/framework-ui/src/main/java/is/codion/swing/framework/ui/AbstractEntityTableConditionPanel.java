/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JPanel;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A base class for condition panels based on {@link SwingEntityTableModel} and {@link EntityTableConditionModel}.
 */
public abstract class AbstractEntityTableConditionPanel extends JPanel {

  private final EntityTableConditionModel tableConditionModel;
  private final Collection<FilteredTableColumn<Attribute<?>>> tableColumns;
  private final State advancedViewState = State.state();

  /**
   * Instantiates a new AbstractEntityTableConditionPanel.
   * @param tableConditionModel the table condition model
   * @param tableColumns the table columns
   */
  protected AbstractEntityTableConditionPanel(EntityTableConditionModel tableConditionModel,
                                              Collection<FilteredTableColumn<Attribute<?>>> tableColumns) {
    this.tableConditionModel = requireNonNull(tableConditionModel);
    this.tableColumns = requireNonNull(tableColumns);
    bindEvents();
  }

  /**
   * @return the condition model this condition panel is based on
   */
  public final EntityTableConditionModel tableConditionModel() {
    return tableConditionModel;
  }

  /**
   * @return all columns from the underlying column model
   */
  public final Collection<FilteredTableColumn<Attribute<?>>> tableColumns() {
    return tableColumns;
  }

  /**
   * @return the state controlling the advanced state of this condition panel
   */
  public final State advancedState() {
    return advancedViewState;
  }

  /**
   * @return the controls provided by this condition panel, such as toggling the advanced mode and clearing the condition,
   * an empty {@link Controls} instance in case of no controls.
   */
  public Controls controls() {
    return Controls.controls();
  }

  /**
   * @return true if this panel has an advanced view which can be toggled on/off
   */
  public boolean hasAdvancedView() {
    return false;
  }

  /**
   * Selects a condition panel, if there are more than one available the user gets to choose.
   */
  public void selectConditionPanel() {}

  /**
   * @param listener a listener notified each time the advanced search state changes
   */
  public final void addAdvancedViewListener(EventDataListener<Boolean> listener) {
    advancedViewState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAdvancedViewListener(EventDataListener<Boolean> listener) {
    advancedViewState.removeDataListener(listener);
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  public void addFocusGainedListener(EventDataListener<Attribute<?>> listener) {}

  /**
   * Sets the advanced search view state, if supported
   * @param advanced true
   * @throws UnsupportedOperationException by default
   * @see #hasAdvancedView()
   */
  protected void setAdvancedView(boolean advanced) {
    throw new UnsupportedOperationException();
  }

  private void bindEvents() {
    advancedViewState.addDataListener(this::setAdvancedView);
  }
}

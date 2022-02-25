/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A base class for condition panels based on {@link SwingEntityTableModel} and {@link EntityTableConditionModel}.
 */
public abstract class AbstractEntityTableConditionPanel extends JPanel {

  private final EntityTableConditionModel tableConditionModel;
  private final Collection<TableColumn> tableColumns;
  private final State advancedState = State.state();

  /**
   * Instantiates a new AbstractEntityTableConditionPanel.
   * @param tableConditionModel the table condition model
   * @param tableColumns the table columns
   */
  public AbstractEntityTableConditionPanel(EntityTableConditionModel tableConditionModel,
                                           Collection<TableColumn> tableColumns) {
    this.tableConditionModel = requireNonNull(tableConditionModel);
    this.tableColumns = requireNonNull(tableColumns);
    bindEvents();
  }

  /**
   * @return the condition model this condition panel is based on
   */
  public final EntityTableConditionModel getTableConditionModel() {
    return tableConditionModel;
  }

  /**
   * @return all columns from the underlying column model
   */
  public final Collection<TableColumn> getTableColumns() {
    return tableColumns;
  }

  /**
   * @return the state controlling the advanced state of this condition panel
   */
  public final State getAdvancedState() {
    return advancedState;
  }

  /**
   * @return the controls provided by this condition panel, such as toggling the advanced mode and clearing the condition,
   * an empty {@link Controls} instance in case of no controls.
   */
  public Controls getControls() {
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
  public final void addAdvancedListener(EventDataListener<Boolean> listener) {
    advancedState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAdvancedListener(EventDataListener<Boolean> listener) {
    advancedState.removeDataListener(listener);
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  public void addFocusGainedListener(EventDataListener<Attribute<?>> listener) {}

  /**
   * Sets the advanced search state, if supported
   * @param advanced true
   * @throws UnsupportedOperationException by default
   * @see #hasAdvancedView()
   */
  protected void setAdvanced(boolean advanced) {
    throw new UnsupportedOperationException();
  }

  private void bindEvents() {
    advancedState.addDataListener(this::setAdvanced);
  }
}

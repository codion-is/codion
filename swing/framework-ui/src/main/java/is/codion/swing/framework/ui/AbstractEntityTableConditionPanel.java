/*
 * Copyright (c) 2020 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.state.States;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A base class for condition panels based on {@link SwingEntityTableModel} and {@link EntityTableConditionModel}.
 */
public abstract class AbstractEntityTableConditionPanel extends JPanel {

  private final EntityTableConditionModel tableConditionModel;
  private final List<TableColumn> tableColumns;
  private final State advancedState = States.state();

  /**
   * Instantiates a new AbstractEntityTableConditionPanel.
   * @param tableConditionModel the table condition model
   * @param tableColumns the table columns
   */
  public AbstractEntityTableConditionPanel(final EntityTableConditionModel tableConditionModel,
                                           final List<TableColumn> tableColumns) {
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
  public final List<TableColumn> getTableColumns() {
    return tableColumns;
  }

  /**
   * @return the state controlling the advanced state of this condition panel
   */
  public final State getAdvancedState() {
    return advancedState;
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  public ControlList getControls() {
    return Controls.controlList();
  }

  /**
   * @return true if this panel has an advanced view which can be toggled on/off
   */
  public boolean canToggleAdvanced() {
    return false;
  }

  /**
   * Selects a condition panel, if there are more than one available the user gets to choose.
   */
  public void selectConditionPanel() {}

  /**
   * @param listener a listener notified each time the advanced search state changes
   */
  public final void addAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedState.removeDataListener(listener);
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  public void addFocusGainedListener(final EventDataListener<Property<?>> listener) {}

  /**
   * Sets the advanced search state, if supported
   * @param advanced true
   * @throws UnsupportedOperationException by default
   * @see #canToggleAdvanced()
   */
  protected void setAdvanced(final boolean advanced) {
    throw new UnsupportedOperationException();
  }

  private void bindEvents() {
    advancedState.addDataListener(this::setAdvanced);
  }
}

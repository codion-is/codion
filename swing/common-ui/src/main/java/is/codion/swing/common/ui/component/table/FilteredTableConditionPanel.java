/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.common.ui.component.table.TableColumnComponentPanel.tableColumnComponentPanel;
import static java.util.Objects.requireNonNull;

/**
 * Contains the filter panels.
 * @param <C> the column identifier type
 */
public final class FilteredTableConditionPanel<T extends FilteredTableModel<?, C>, C> extends JPanel {

  private final T tableModel;
  private final TableColumnComponentPanel<C, ColumnConditionPanel<C, ?>> componentPanel;
  private final State advancedViewState = State.state();

  /**
   * @param tableModel the table model
   * @param conditionPanelFactory the condition panel factory
   */
  public FilteredTableConditionPanel(T tableModel, ConditionPanelFactory conditionPanelFactory) {
    this.tableModel = requireNonNull(tableModel);
    this.componentPanel = tableColumnComponentPanel(tableModel.columnModel(),
            createConditionPanels(tableModel.columnModel(), requireNonNull(conditionPanelFactory)));
    setLayout(new BorderLayout());
    add(componentPanel, BorderLayout.CENTER);
    advancedViewState.addDataListener(this::setAdvancedView);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(componentPanel);
  }

  /**
   * @return the table model
   */
  public T tableModel() {
    return tableModel;
  }

  /**
   * @return the underlying component panel
   */
  public TableColumnComponentPanel<C, ColumnConditionPanel<C, ?>> componentPanel() {
    return componentPanel;
  }

  /**
   * @return the state controlling the advanced view state of this condition panel
   */
  public State advancedViewState() {
    return advancedViewState;
  }

  /**
   * @param <T> the column value type
   * @param columnIdentifier the column identifier
   * @return the condition panel associated with the given column
   * @throws IllegalArgumentException in case no condition panel exists for the given column
   */
  public <T> ColumnConditionPanel<C, T> conditionPanel(C columnIdentifier) {
    for (FilteredTableColumn<C> column : tableModel.columnModel().columns()) {
      if (column.getIdentifier().equals(columnIdentifier)) {
        return (ColumnConditionPanel<C, T>) componentPanel.columnComponents().get(column);
      }
    }

    throw new IllegalArgumentException("No condition panel available for column: " + columnIdentifier);
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  public Controls controls() {
    return Controls.builder()
            .control(ToggleControl.builder(advancedViewState)
                    .caption(Messages.advanced()))
            .control(Control.builder(this::clearConditions)
                    .caption(Messages.clear()))
            .build();
  }

  /**
   * @param listener a listener notified when a condition panel receives focus
   */
  public void addFocusGainedListener(EventDataListener<C> listener) {
    componentPanel().columnComponents().values().forEach(panel -> panel.addFocusGainedListener(listener));
  }

  /**
   * @param listener a listener notified each time the advanced search state changes
   */
  public void addAdvancedViewListener(EventDataListener<Boolean> listener) {
    advancedViewState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedViewListener(EventDataListener<Boolean> listener) {
    advancedViewState.removeDataListener(listener);
  }

  private void clearConditions() {
    componentPanel.columnComponents().values().
            stream()
            .map(ColumnConditionPanel::model)
            .forEach(ColumnConditionModel::clearCondition);
  }

  private void setAdvancedView(boolean advanced) {
    componentPanel.columnComponents().forEach((column, panel) -> panel.setAdvancedView(advanced));
  }

  private Map<FilteredTableColumn<C>, ColumnConditionPanel<C, ?>> createConditionPanels(
          FilteredTableColumnModel<C> columnModel, ConditionPanelFactory conditionPanelFactory) {
    Map<FilteredTableColumn<C>, ColumnConditionPanel<C, ?>> conditionPanels = new HashMap<>();
    columnModel.columns().forEach(column -> {
      ColumnConditionPanel<C, Object> conditionPanel = conditionPanelFactory.createConditionPanel(column);
      if (conditionPanel != null) {
        conditionPanels.put(column, conditionPanel);
      }
    });

    return conditionPanels;
  }
}

/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel;
import static java.util.Objects.requireNonNull;

/**
 * Contains the filter panels.
 * @param <C> the column identifier type
 * @see #filteredTableConditionPanel(TableConditionModel, FilteredTableColumnModel, ColumnConditionPanel.Factory)
 */
public final class FilteredTableConditionPanel<C> extends JPanel {

  private final TableConditionModel<C> conditionModel;
  private final FilteredTableColumnComponentPanel<C, ColumnConditionPanel<C, ?>> componentPanel;
  private final State advancedViewState = State.state();

  private FilteredTableConditionPanel(TableConditionModel<C> conditionModel, FilteredTableColumnModel<C> columnModel,
                                      ColumnConditionPanel.Factory<C> conditionPanelFactory) {
    this.conditionModel = requireNonNull(conditionModel);
    this.componentPanel = filteredTableColumnComponentPanel(requireNonNull(columnModel),
            createConditionPanels(columnModel, requireNonNull(conditionPanelFactory)));
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
   * @return the underlying component panel
   */
  public FilteredTableColumnComponentPanel<C, ColumnConditionPanel<C, ?>> componentPanel() {
    return componentPanel;
  }

  /**
   * @return the state controlling the advanced view status of this condition panel
   */
  public State advancedView() {
    return advancedViewState;
  }

  /**
   * @param <T> the column value type
   * @param columnIdentifier the column identifier
   * @return the condition panel associated with the given column
   * @throws IllegalArgumentException in case no condition panel exists for the given column
   */
  public <T> ColumnConditionPanel<C, T> conditionPanel(C columnIdentifier) {
    ColumnConditionPanel<C, ?> conditionPanel = componentPanel.columnComponents().get(requireNonNull(columnIdentifier));
    if (conditionPanel == null) {
      throw new IllegalArgumentException("No condition panel available for column: " + columnIdentifier);
    }

    return (ColumnConditionPanel<C, T>) conditionPanel;
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  public Controls controls() {
    return Controls.builder()
            .control(ToggleControl.builder(advancedViewState)
                    .name(Messages.advanced()))
            .control(Control.builder(this::clearConditions)
                    .name(Messages.clear()))
            .build();
  }

  /**
   * @param listener a listener notified when a condition panel receives focus
   */
  public void addFocusGainedListener(Consumer<C> listener) {
    componentPanel().columnComponents().values().forEach(panel -> panel.addFocusGainedListener(listener));
  }

  /**
   * @param <C> the column identifier type
   * @param conditionModel the condition model
   * @param columnModel the column model
   * @param conditionPanelFactory the condition panel factory
   * @return a new {@link FilteredTableConditionPanel}
   */
  public static <C> FilteredTableConditionPanel<C> filteredTableConditionPanel(TableConditionModel<C> conditionModel,
                                                                               FilteredTableColumnModel<C> columnModel,
                                                                               ColumnConditionPanel.Factory<C> conditionPanelFactory) {
    return new FilteredTableConditionPanel<>(conditionModel, columnModel, conditionPanelFactory);
  }

  private void clearConditions() {
    componentPanel.columnComponents().values().stream()
            .map(ColumnConditionPanel::model)
            .forEach(ColumnConditionModel::clear);
  }

  private void setAdvancedView(boolean advanced) {
    componentPanel.columnComponents().forEach((column, panel) -> panel.advancedView().set(advanced));
  }

  private Map<C, ColumnConditionPanel<C, ?>> createConditionPanels(
          FilteredTableColumnModel<C> columnModel, ColumnConditionPanel.Factory<C> conditionPanelFactory) {
    return columnModel.columns().stream()
            .map(column -> conditionModel.conditionModels().get(column.getIdentifier()))
            .filter(Objects::nonNull)
            .map(conditionPanelFactory::createConditionPanel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(conditionPanel -> conditionPanel.model().columnIdentifier(), Function.identity()));
  }
}

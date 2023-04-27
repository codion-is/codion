/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ConditionPanelFactory;
import is.codion.swing.common.ui.component.table.TableColumnComponentPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.swing.common.ui.component.table.TableColumnComponentPanel.tableColumnComponentPanel;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A UI component based on the EntityTableConditionModel.
 * For instances use the {@link #entityTableConditionPanel(EntityTableConditionModel, FilteredTableColumnModel)} or
 * {@link #entityTableConditionPanel(EntityTableConditionModel, FilteredTableColumnModel, ConditionPanelFactory)} factory methods.
 * @see EntityTableConditionModel
 * @see ColumnConditionPanel
 * @see #entityTableConditionPanel(EntityTableConditionModel, FilteredTableColumnModel)
 * @see #entityTableConditionPanel(EntityTableConditionModel, FilteredTableColumnModel, ConditionPanelFactory)
 */
public final class EntityTableConditionPanel extends JPanel {

  private final EntityTableConditionModel tableConditionModel;
  private final FilteredTableColumnModel<Attribute<?>> columnModel;
  private final TableColumnComponentPanel<Attribute<?>, ColumnConditionPanel<Attribute<?>, ?>> conditionPanel;
  private final State advancedViewState = State.state();

  private EntityTableConditionPanel(EntityTableConditionModel tableConditionModel,
                                    FilteredTableColumnModel<Attribute<?>> columnModel,
                                    ConditionPanelFactory conditionPanelFactory) {
    this.tableConditionModel = requireNonNull(tableConditionModel);
    this.conditionPanel = tableColumnComponentPanel(columnModel, createConditionPanels(columnModel, requireNonNull(conditionPanelFactory)));
    this.columnModel = columnModel;
    setLayout(new BorderLayout());
    add(conditionPanel, BorderLayout.CENTER);
    advancedViewState.addDataListener(this::setAdvancedView);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(conditionPanel);
  }

  /**
   * Allows the user to select one of the available condition panels for keyboard input focus,
   * if only one condition panel is available that one is selected automatically.
   */
  public void selectConditionPanel() {
    List<Property<?>> conditionProperties = conditionPanelProperties();
    if (!conditionProperties.isEmpty()) {
      if (conditionProperties.size() == 1) {
        conditionPanel(conditionProperties.get(0).attribute()).requestInputFocus();
      }
      else {
        conditionProperties.sort(Property.propertyComparator());
        Dialogs.selectionDialog(conditionProperties)
                .owner(this)
                .title(FrameworkMessages.selectSearchField())
                .selectSingle()
                .ifPresent(property -> conditionPanel(property.attribute()).requestInputFocus());
      }
    }
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  public Controls controls() {
    Controls.Builder controls = Controls.builder()
            .caption(FrameworkMessages.search())
            .smallIcon(FrameworkIcons.instance().filter());
    controls.control(ToggleControl.builder(advancedViewState)
            .caption(FrameworkMessages.advanced()));
    controls.control(Control.builder(tableConditionModel::clearConditions)
            .caption(FrameworkMessages.clear()));

    return controls.build();
  }

  /**
   * @return the state controlling the advanced view state of this condition panel
   */
  public State advancedViewState() {
    return advancedViewState;
  }

  /**
   * @param <C> the attribute type
   * @param <T> the value type
   * @param attribute the attribute
   * @return the condition panel associated with the given property
   * @throws IllegalArgumentException in case no condition panel exists for the given attribute
   */
  public <C extends Attribute<T>, T> ColumnConditionPanel<C, T> conditionPanel(C attribute) {
    for (FilteredTableColumn<Attribute<?>> column : columnModel.columns()) {
      if (column.getIdentifier().equals(attribute)) {
        return (ColumnConditionPanel<C, T>) conditionPanel.columnComponents().get(column);
      }
    }

    throw new IllegalArgumentException("No condition panel available for attribute: " + attribute);
  }

  /**
   * @param listener a listener notified when a condition panel receives focus
   */
  public void addFocusGainedListener(EventDataListener<Attribute<?>> listener) {
    conditionPanel.columnComponents().values().forEach(panel -> panel.addFocusGainedListener(listener));
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

  /**
   * Instantiates a new {@link EntityTableConditionPanel} with a default condition panel setup, based on
   * an {@link TableColumnComponentPanel} containing {@link ColumnConditionPanel}s
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @return a new {@link EntityTableConditionPanel}
   */
  public static EntityTableConditionPanel entityTableConditionPanel(EntityTableConditionModel tableConditionModel,
                                                                    FilteredTableColumnModel<Attribute<?>> columnModel) {
    return entityTableConditionPanel(tableConditionModel, columnModel, new EntityConditionPanelFactory(tableConditionModel));
  }

  /**
   * Instantiates a new {@link EntityTableConditionPanel} with a default condition panel setup, based on
   * an {@link TableColumnComponentPanel} containing {@link ColumnConditionPanel}s
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @param conditionPanelFactory the condition panel factory
   * @return a new {@link EntityTableConditionPanel}
   */
  public static EntityTableConditionPanel entityTableConditionPanel(EntityTableConditionModel tableConditionModel,
                                                                    FilteredTableColumnModel<Attribute<?>> columnModel,
                                                                    ConditionPanelFactory conditionPanelFactory) {
    return new EntityTableConditionPanel(tableConditionModel, columnModel, conditionPanelFactory);
  }

  private void setAdvancedView(boolean advanced) {
    conditionPanel.columnComponents().forEach((column, panel) -> panel.setAdvancedView(advanced));
  }

  private List<Property<?>> conditionPanelProperties() {
    return conditionPanel.columnComponents().values().stream()
            .filter(panel -> columnModel.isColumnVisible(panel.model().columnIdentifier()))
            .map(panel -> tableConditionModel.entityDefinition().property(panel.model().columnIdentifier()))
            .collect(toList());
  }

  private static Map<FilteredTableColumn<Attribute<?>>, ColumnConditionPanel<Attribute<?>, ?>> createConditionPanels(
          FilteredTableColumnModel<Attribute<?>> columnModel, ConditionPanelFactory conditionPanelFactory) {
    Map<FilteredTableColumn<Attribute<?>>, ColumnConditionPanel<Attribute<?>, ?>> conditionPanels = new HashMap<>();
    columnModel.columns().forEach(column -> {
      ColumnConditionPanel<Attribute<?>, Object> conditionPanel = conditionPanelFactory.createConditionPanel(column);
      if (conditionPanel != null) {
        conditionPanels.put(column, conditionPanel);
      }
    });

    return conditionPanels;
  }
}

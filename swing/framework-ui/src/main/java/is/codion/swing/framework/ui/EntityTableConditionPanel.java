/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.TableColumnComponentPanel;

import javax.swing.JComponent;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;

/**
 * A UI component based on the EntityTableConditionModel
 * @see EntityTableConditionModel
 * @see PropertyConditionPanel
 */
public final class EntityTableConditionPanel extends AbstractEntityTableConditionPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTableConditionPanel.class.getName());

  private final TableColumnComponentPanel<ColumnConditionPanel<Entity, ?, ?>> conditionPanel;
  private final ToggleControl conditionRequiredControl;

  /**
   * Instantiates a new EntityTableConditionPanel with a default condition panel setup, based on
   * an {@link TableColumnComponentPanel} containing {@link PropertyConditionPanel}s
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @param onSearchListener notified when this condition panel triggers a search
   * @param queryConditionRequiredState the state indicating whether a condition is required
   */
  public EntityTableConditionPanel(final EntityTableConditionModel tableConditionModel,
                                   final SwingFilteredTableColumnModel<?, ?> columnModel,
                                   final EventListener onSearchListener,
                                   final State queryConditionRequiredState) {
    super(tableConditionModel, columnModel.getAllColumns());
    this.conditionPanel = new TableColumnComponentPanel<>(columnModel, createPropertyConditionPanels(tableConditionModel, columnModel));
    this.conditionRequiredControl = Controls.toggleControl(queryConditionRequiredState, MESSAGES.getString("require_query_condition"));
    this.conditionRequiredControl.setDescription(MESSAGES.getString("require_query_condition_description"));
    setLayout(new BorderLayout());
    add(conditionPanel, BorderLayout.CENTER);
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            Controls.control(onSearchListener::onEvent, getTableConditionModel().getConditionObserver()));
  }

  /**
   * @return true if this panel has an advanced view which can be toggled on/off
   */
  public boolean canToggleAdvanced() {
    return true;
  }

  /**
   * Allows the user to select one of the available condition panels for keyboard input focus,
   * if only one condition panel is available that one is selected automatically.
   */
  public void selectConditionPanel() {
    final List<Property<?>> conditionProperties = new ArrayList<>();
    conditionPanel.getColumnComponents().forEach((column, panel) -> {
      if (panel instanceof ColumnConditionPanel) {
        conditionProperties.add((Property<?>) column.getIdentifier());
      }
    });
    if (!conditionProperties.isEmpty()) {
      Properties.sort(conditionProperties);
      final Property<?> property = conditionProperties.size() == 1 ? conditionProperties.get(0) :
              Dialogs.selectValue(this, conditionProperties, Messages.get(Messages.SELECT_INPUT_FIELD));
      if (property != null) {
        final ColumnConditionPanel<Entity, Property<?>, ?> conditionPanel = getConditionPanel(property.getAttribute());
        if (conditionPanel != null) {
          conditionPanel.requestInputFocus();
        }
      }
    }
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  public void addFocusGainedListener(final EventDataListener<Property<?>> listener) {
    conditionPanel.getColumnComponents().values().forEach(panel -> ((ColumnConditionPanel<?, Property<?>, ?>) panel).addFocusGainedListener(listener));
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  public ControlList getControls() {
    final ControlList controls = Controls.controlList(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controls.setIcon(frameworkIcons().filter());
    if (canToggleAdvanced()) {
      controls.add(Controls.toggleControl(getAdvancedState(), FrameworkMessages.get(FrameworkMessages.ADVANCED)));
    }
    controls.add(Controls.control(getTableConditionModel()::clearConditionModels, FrameworkMessages.get(FrameworkMessages.CLEAR)));
    controls.addSeparator();
    controls.add(conditionRequiredControl);

    return controls;
  }

  /**
   * @param  attribute the attribute
   * @return the condition panel associated with the given property, null if none is specified
   */
  public ColumnConditionPanel<Entity, Property<?>, ?> getConditionPanel(final Attribute<?> attribute) {
    for (final TableColumn column : getTableColumns()) {
      final Property<?> property = (Property<?>) column.getIdentifier();
      if (property.getAttribute().equals(attribute)) {
        return (ColumnConditionPanel<Entity, Property<?>, ?>) conditionPanel.getColumnComponents().get(column);
      }
    }

    return null;
  }

  @Override
  protected void setAdvanced(final boolean advanced) {
    conditionPanel.getColumnComponents().forEach((column, panel) -> panel.setAdvanced(advanced));
  }

  private static Map<TableColumn, ColumnConditionPanel<Entity, ?, ?>> createPropertyConditionPanels(final EntityTableConditionModel conditionModel,
                                                                                                    final SwingFilteredTableColumnModel<?, ?> columnModel) {
    final Map<TableColumn, ColumnConditionPanel<Entity, ?, ?>> components = new HashMap<>();
    columnModel.getAllColumns().forEach(column -> {
      final Property<?> property = (Property<?>) column.getIdentifier();
      if (conditionModel.containsConditionModel(property.getAttribute())) {
        components.put(column, initializeConditionPanel(conditionModel.getConditionModel(property.getAttribute())));
      }
    });

    return components;
  }

  /**
   * Initializes a ColumnConditionPanel for the given model
   * @param propertyConditionModel the {@link ColumnConditionModel} for which to create a condition panel
   * @return a ColumnConditionPanel based on the given model
   */
  private static <C extends Property<T>, T> ColumnConditionPanel<Entity, C, T> initializeConditionPanel(
          final ColumnConditionModel<Entity, C, T> propertyConditionModel) {
    if (propertyConditionModel instanceof ForeignKeyConditionModel) {
      return (ColumnConditionPanel<Entity, C, T>) new ForeignKeyConditionPanel((ForeignKeyConditionModel) propertyConditionModel);
    }

    return new PropertyConditionPanel<>(propertyConditionModel);
  }
}

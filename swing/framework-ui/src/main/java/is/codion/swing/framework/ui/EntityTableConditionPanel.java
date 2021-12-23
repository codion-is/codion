/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.ConditionPanelFactory;
import is.codion.swing.common.ui.table.TableColumnComponentPanel;

import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the EntityTableConditionModel
 * @see EntityTableConditionModel
 * @see ColumnConditionPanel
 */
public final class EntityTableConditionPanel extends AbstractEntityTableConditionPanel {

  private final TableColumnComponentPanel<ColumnConditionPanel<Attribute<?>, ?>> conditionPanel;
  private final SwingFilteredTableColumnModel<Attribute<?>> columnModel;

  /**
   * Instantiates a new EntityTableConditionPanel with a default condition panel setup, based on
   * an {@link TableColumnComponentPanel} containing {@link ColumnConditionPanel}s
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   */
  public EntityTableConditionPanel(final EntityTableConditionModel tableConditionModel,
                                   final SwingFilteredTableColumnModel<Attribute<?>> columnModel) {
    this(tableConditionModel, columnModel, new EntityConditionPanelFactory(tableConditionModel));
  }

  /**
   * Instantiates a new EntityTableConditionPanel with a default condition panel setup, based on
   * an {@link TableColumnComponentPanel} containing {@link ColumnConditionPanel}s
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @param conditionPanelFactory the condition panel factory
   */
  public EntityTableConditionPanel(final EntityTableConditionModel tableConditionModel,
                                   final SwingFilteredTableColumnModel<Attribute<?>> columnModel,
                                   final ConditionPanelFactory conditionPanelFactory) {
    super(tableConditionModel, requireNonNull(columnModel).getAllColumns());
    requireNonNull(conditionPanelFactory);
    this.conditionPanel = new TableColumnComponentPanel<>(columnModel, createConditionPanels(columnModel, conditionPanelFactory));
    this.columnModel = columnModel;
    setLayout(new BorderLayout());
    add(conditionPanel, BorderLayout.CENTER);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(conditionPanel);
  }

  /**
   * @return true if this panel has an advanced view which can be toggled on/off
   */
  @Override
  public boolean hasAdvancedView() {
    return true;
  }

  /**
   * Allows the user to select one of the available condition panels for keyboard input focus,
   * if only one condition panel is available that one is selected automatically.
   */
  @Override
  public void selectConditionPanel() {
    final List<Property<?>> conditionProperties = getConditionProperties();
    if (!conditionProperties.isEmpty()) {
      Properties.sort(conditionProperties);
      Dialogs.selectionDialog(conditionProperties)
              .owner(this)
              .title(Messages.get(Messages.SELECT_INPUT_FIELD))
              .selectSingle()
              .ifPresent(property -> getConditionPanel(property.getAttribute()).requestInputFocus());
    }
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  @Override
  public void addFocusGainedListener(final EventDataListener<Attribute<?>> listener) {
    conditionPanel.getColumnComponents().values().forEach(panel -> panel.addFocusGainedListener(listener));
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  @Override
  public Controls getControls() {
    final Controls.Builder controls = Controls.builder()
            .caption(FrameworkMessages.get(FrameworkMessages.SEARCH))
            .icon(frameworkIcons().filter());
    if (hasAdvancedView()) {
      controls.control(ToggleControl.builder(getAdvancedState())
              .caption(FrameworkMessages.get(FrameworkMessages.ADVANCED)));
    }
    controls.control(Control.builder(getTableConditionModel()::clearConditions)
            .caption(FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controls.build();
  }

  /**
   * @param <C> the attribute type
   * @param <T> the value type
   * @param attribute the attribute
   * @return the condition panel associated with the given property
   * @throws IllegalArgumentException in case no condition panel exists for the given attribute
   */
  public <C extends Attribute<T>, T> ColumnConditionPanel<C, T> getConditionPanel(final C attribute) {
    for (final TableColumn column : getTableColumns()) {
      if (column.getIdentifier().equals(attribute)) {
        return (ColumnConditionPanel<C, T>) conditionPanel.getColumnComponents().get(column);
      }
    }

    throw new IllegalArgumentException("No condition panel available for attribute: " + attribute);
  }

  @Override
  protected void setAdvanced(final boolean advanced) {
    conditionPanel.getColumnComponents().forEach((column, panel) -> panel.setAdvanced(advanced));
  }

  private List<Property<?>> getConditionProperties() {
    return conditionPanel.getColumnComponents().values().stream()
            .filter(panel -> columnModel.isColumnVisible(panel.getModel().getColumnIdentifier()))
            .map(panel -> getTableConditionModel().getEntityDefinition().getProperty(panel.getModel().getColumnIdentifier()))
            .collect(Collectors.toList());
  }

  private static Map<TableColumn, ColumnConditionPanel<Attribute<?>, ?>> createConditionPanels(
          final SwingFilteredTableColumnModel<Attribute<?>> columnModel, final ConditionPanelFactory conditionPanelFactory) {
    final Map<TableColumn, ColumnConditionPanel<Attribute<?>, ?>> conditionPanels = new HashMap<>();
    columnModel.getAllColumns().forEach(column -> {
      final ColumnConditionPanel<Attribute<?>, Object> conditionPanel = (ColumnConditionPanel<Attribute<?>, Object>) conditionPanelFactory.createConditionPanel(column);
      if (conditionPanel != null) {
        conditionPanels.put(column, conditionPanel);
      }
    });

    return conditionPanels;
  }
}

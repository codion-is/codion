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
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.ConditionPanelFactory;
import is.codion.swing.common.ui.table.TableColumnComponentPanel;

import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the EntityTableConditionModel
 * @see EntityTableConditionModel
 * @see ColumnConditionPanel
 */
public final class EntityTableConditionPanel extends AbstractEntityTableConditionPanel {

  private final TableColumnComponentPanel<ColumnConditionPanel<?, ?>> conditionPanel;
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
    final List<Property<?>> conditionProperties = new ArrayList<>();
    conditionPanel.getColumnComponents().forEach((column, panel) -> {
      if (panel instanceof ColumnConditionPanel && columnModel.isColumnVisible((Attribute<?>) column.getIdentifier())) {
        conditionProperties.add(getTableConditionModel().getEntityDefinition().getProperty((Attribute<?>) column.getIdentifier()));
      }
    });
    if (!conditionProperties.isEmpty()) {
      Properties.sort(conditionProperties);
      final Optional<Property<?>> optionalProperty = conditionProperties.size() == 1 ? Optional.of(conditionProperties.get(0)) :
              Dialogs.selectionDialogBuilder(conditionProperties)
                      .owner(this)
                      .title(Messages.get(Messages.SELECT_INPUT_FIELD))
                      .selectSingle();
      optionalProperty.ifPresent(property -> {
        final ColumnConditionPanel<?, ?> panel = getConditionPanel(property.getAttribute());
        if (panel != null) {
          panel.requestInputFocus();
        }
      });
    }
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  @Override
  public void addFocusGainedListener(final EventDataListener<Attribute<?>> listener) {
    conditionPanel.getColumnComponents().values().forEach(panel -> ((ColumnConditionPanel<Attribute<?>, ?>) panel).addFocusGainedListener(listener));
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  @Override
  public Controls getControls() {
    final Controls.Builder controls = Controls.builder()
            .name(FrameworkMessages.get(FrameworkMessages.SEARCH))
            .icon(frameworkIcons().filter());
    if (hasAdvancedView()) {
      controls.control(ToggleControl.builder()
              .state(getAdvancedState())
              .name(FrameworkMessages.get(FrameworkMessages.ADVANCED)));
    }
    controls.control(Control.builder(getTableConditionModel()::clearConditionModels)
            .name(FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controls.build();
  }

  /**
   * @param attribute the attribute
   * @param <T> the value type
   * @return the condition panel associated with the given property, null if none is specified
   */
  public <T> ColumnConditionPanel<Attribute<T>, T> getConditionPanel(final Attribute<T> attribute) {
    for (final TableColumn column : getTableColumns()) {
      if (column.getIdentifier().equals(attribute)) {
        return (ColumnConditionPanel<Attribute<T>, T>) conditionPanel.getColumnComponents().get(column);
      }
    }

    return null;
  }

  @Override
  protected void setAdvanced(final boolean advanced) {
    conditionPanel.getColumnComponents().forEach((column, panel) -> panel.setAdvanced(advanced));
  }

  private static Map<TableColumn, ColumnConditionPanel<?, ?>> createConditionPanels(
          final SwingFilteredTableColumnModel<Attribute<?>> columnModel, final ConditionPanelFactory conditionPanelFactory) {
    final Map<TableColumn, ColumnConditionPanel<?, ?>> conditionPanels = new HashMap<>();
    columnModel.getAllColumns().forEach(column -> {
      final ColumnConditionPanel<?, ?> conditionPanel = conditionPanelFactory.createConditionPanel(column);
      if (conditionPanel != null) {
        conditionPanels.put(column, conditionPanel);
      }
    });
    return conditionPanels;
  }
}

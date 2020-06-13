/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.Events;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.value.Values;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.table.AbstractTableColumnSyncPanel;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.value.TextValues;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;

/**
 * A UI component based on the EntityTableConditionModel
 * @see EntityTableConditionModel
 */
public final class EntityTableConditionPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTableConditionPanel.class.getName());

  private final Event<Boolean> advancedChangedEvent = Events.event();

  private final EntityTableConditionModel conditionModel;
  private final List<TableColumn> columns;

  private final JPanel advancedConditionPanel;
  private final JPanel simpleConditionPanel;

  private final ToggleControl conditionRequiredControl;

  /**
   * Instantiates a new EntityTableConditionPanel with a default condition panel setup, based on
   * an {@link AbstractTableColumnSyncPanel} containing {@link PropertyConditionPanel}s
   * @param tableModel the table model
   */
  public EntityTableConditionPanel(final SwingEntityTableModel tableModel) {
    this(tableModel, new ConditionColumnSyncPanel(tableModel), initializeSimpleConditionPanel(tableModel.getConditionModel()));
  }

  /**
   * Instantiates a new EntityTableConditionPanel, either {@code advancedConditionPanel}, {@code simpleConditionPanel} or both need to be provided
   * @param tableModel the table model
   * @param advancedConditionPanel the panel to show in case of advanced condition
   * @param simpleConditionPanel the panel to show in case of simple condition
   */
  public EntityTableConditionPanel(final SwingEntityTableModel tableModel, final JPanel advancedConditionPanel,
                                   final JPanel simpleConditionPanel) {
    if (advancedConditionPanel == null && simpleConditionPanel == null) {
      throw new IllegalArgumentException("An advanced and/or a simple condition panel is required");
    }
    this.conditionModel = tableModel.getConditionModel();
    this.columns = tableModel.getColumnModel().getAllColumns();
    this.advancedConditionPanel = advancedConditionPanel;
    this.simpleConditionPanel = simpleConditionPanel;
    this.conditionRequiredControl = Controls.toggleControl(tableModel.getQueryConditionRequiredState(), MESSAGES.getString("require_query_condition"));
    this.conditionRequiredControl.setDescription(MESSAGES.getString("require_query_condition_description"));
    setLayout(new BorderLayout());
    layoutPanel(true);
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            Controls.control(tableModel::refresh, conditionModel.getConditionChangedObserver()));
  }

  /**
   * @return the condition model this condition panel is based on
   */
  public EntityTableConditionModel getConditionModel() {
    return conditionModel;
  }

  /**
   * @param advanced true if advanced search should be enabled in the full condition panel,
   * does not apply when simple search is enabled
   */
  public void setAdvanced(final boolean advanced) {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          ((ColumnConditionPanel<Entity, ?>) panel).setAdvanced(advanced);
        }
      });
    }
    else {
      layoutPanel(advanced);
    }
    advancedChangedEvent.onEvent(advanced);
  }

  /**
   * @return true if advanced search is enabled in the full condition panel,
   * does not apply when simple search is enabled
   */
  public boolean isAdvanced() {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel conditionPanel : ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().values()) {
        if (conditionPanel instanceof ColumnConditionPanel) {
          return ((ColumnConditionPanel<Entity, ?>) conditionPanel).isAdvanced();
        }
      }
    }

    return getComponentCount() > 0 && getComponent(0) == advancedConditionPanel;
  }

  /**
   * @return true if this panel has an advanced view which can be toggled on/off
   */
  public boolean canToggleAdvanced() {
    return advancedConditionPanel instanceof AbstractTableColumnSyncPanel || (advancedConditionPanel != null && simpleConditionPanel != null);
  }

  /**
   * Allows the user to select one of the available condition panels for keyboard input focus,
   * if only one condition panel is available that one is selected automatically.
   */
  public void selectConditionPanel() {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      final List<Property<?>> conditionProperties = new ArrayList<>();
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          conditionProperties.add((Property<?>) column.getIdentifier());
        }
      });
      if (!conditionProperties.isEmpty()) {
        Properties.sort(conditionProperties);
        final Property<?> property = conditionProperties.size() == 1 ? conditionProperties.get(0) :
                Dialogs.selectValue(this, conditionProperties, Messages.get(Messages.SELECT_INPUT_FIELD));
        if (property != null) {
          final ColumnConditionPanel<Entity, Property<?>> conditionPanel = getConditionPanel(property.getAttribute());
          if (conditionPanel != null) {
            conditionPanel.requestInputFocus();
          }
        }
      }
    }
  }

  /**
   * @param listener a listener notified when a condition panel receives focus, note this does not apply
   * for custom search panels
   */
  public void addFocusGainedListener(final EventDataListener<Property<?>> listener) {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          ((ColumnConditionPanel<?, Property<?>>) panel).addFocusGainedListener(listener);
        }
      });
    }
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param searchText the search text
   */
  public void setSearchText(final String searchText) {
    getConditionModel().setSimpleConditionString(searchText);
  }

  /**
   * @return the controls provided by this condition panel, for toggling the advanced mode and clearing the condition
   */
  public ControlList getControls() {
    final ControlList controls = Controls.controlList(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controls.setIcon(frameworkIcons().filter());
    if (canToggleAdvanced()) {
      controls.add(Controls.toggleControl(this, "advanced",
              FrameworkMessages.get(FrameworkMessages.ADVANCED), advancedChangedEvent));
    }
    controls.add(Controls.control(conditionModel::clearPropertyConditionModels, FrameworkMessages.get(FrameworkMessages.CLEAR)));
    controls.addSeparator();
    controls.add(conditionRequiredControl);

    return controls;
  }

  /**
   * @param  attribute the attribute
   * @return the condition panel associated with the given property, null if none is specified
   */
  public ColumnConditionPanel<Entity, Property<?>> getConditionPanel(final Attribute<?> attribute) {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      for (final TableColumn column : columns) {
        final Property<?> property = (Property<?>) column.getIdentifier();
        if (property.getAttribute().equals(attribute)) {
          return (ColumnConditionPanel<Entity, Property<?>>) ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().get(column);
        }
      }
    }

    return null;
  }

  /**
   * @param listener a listener notified each time the advanced search state changes
   */
  public void addAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedChangedEvent.removeDataListener(listener);
  }

  /**
   * @param advanced if true then the simple condition panel is shown, if false the advanced condition panel is shown,
   * note that if either of these is not available calling this method has no effect
   */
  private void layoutPanel(final boolean advanced) {
    if (advanced && advancedConditionPanel == null) {
      return;
    }
    if (!advanced && simpleConditionPanel == null) {
      return;
    }
    removeAll();
    if (advanced) {
      add(advancedConditionPanel, BorderLayout.CENTER);
    }
    else {
      add(simpleConditionPanel, BorderLayout.CENTER);
    }
  }

  private static JPanel initializeSimpleConditionPanel(final EntityTableConditionModel conditionModel) {
    final JTextField simpleSearchTextField = new JTextField();
    final Control simpleSearchControl = Controls.control(conditionModel::performSimpleSearch, FrameworkMessages.get(FrameworkMessages.SEARCH));
    final JButton simpleSearchButton = new JButton(simpleSearchControl);
    simpleSearchTextField.addActionListener(simpleSearchControl);
    final JPanel panel = new JPanel(Layouts.borderLayout());
    Values.propertyValue(conditionModel, "simpleConditionString", String.class,
            conditionModel.getSimpleConditionStringObserver()).link(TextValues.textValue(simpleSearchTextField));
    panel.setBorder(BorderFactory.createTitledBorder(MESSAGES.getString("condition")));
    panel.add(simpleSearchTextField, BorderLayout.CENTER);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private static final class ConditionColumnSyncPanel extends AbstractTableColumnSyncPanel {

    private final EntityTableConditionModel conditionModel;

    private ConditionColumnSyncPanel(final SwingEntityTableModel tableModel) {
      super(tableModel.getColumnModel());
      this.conditionModel = tableModel.getConditionModel();
      setVerticalFillerWidth(Components.getPreferredScrollBarWidth());
      resetPanel();
    }

    @Override
    protected JPanel initializeColumnPanel(final TableColumn column) {
      final Property<?> property = (Property<?>) column.getIdentifier();
      if (conditionModel.containsPropertyConditionModel(property.getAttribute())) {
        return initializeConditionPanel(conditionModel.getPropertyConditionModel(property.getAttribute()));
      }

      return new JPanel();
    }

    /**
     * Initializes a ColumnConditionPanel for the given model
     * @param propertyConditionModel the {@link ColumnConditionModel} for which to create a condition panel
     * @return a ColumnConditionPanel based on the given model
     */
    private static ColumnConditionPanel<Entity, ? extends Property<?>> initializeConditionPanel(
            final ColumnConditionModel<Entity, ? extends Property<?>> propertyConditionModel) {
      if (propertyConditionModel instanceof ForeignKeyConditionModel) {
        return new ForeignKeyConditionPanel((ForeignKeyConditionModel) propertyConditionModel);
      }

      return new PropertyConditionPanel((ColumnConditionModel<Entity, ColumnProperty<?>>) propertyConditionModel);
    }
  }
}

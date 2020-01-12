/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.value.Values;
import org.jminor.framework.domain.property.Properties;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.ForeignKeyConditionModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.control.ToggleControl;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;
import org.jminor.swing.common.ui.value.ValueLinks;
import org.jminor.swing.framework.model.SwingEntityTableModel;

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
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A UI component based on the EntityTableConditionModel
 * @see EntityTableConditionModel
 */
public final class EntityTableConditionPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTableConditionPanel.class.getName(), Locale.getDefault());

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
    UiUtil.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            Controls.control(tableModel::refresh, (String) null, conditionModel.getConditionStateObserver()));
  }

  /**
   * @return the condition model this condition panel is based on
   */
  public EntityTableConditionModel getConditionModel() {
    return conditionModel;
  }

  /**
   * @param value true if advanced search should be enabled in the full condition panel,
   * does not apply when simple search is enabled
   */
  public void setAdvanced(final boolean value) {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          ((ColumnConditionPanel) panel).setAdvanced(value);
        }
      });
    }
    else {
      layoutPanel(value);
    }
    advancedChangedEvent.onEvent(value);
  }

  /**
   * @return true if advanced search is enabled in the full condition panel,
   * does not apply when simple search is enabled
   */
  public boolean isAdvanced() {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel conditionPanel : ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().values()) {
        if (conditionPanel instanceof ColumnConditionPanel) {
          return ((ColumnConditionPanel) conditionPanel).isAdvanced();
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
      final List<Property> conditionProperties = new ArrayList<>();
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          conditionProperties.add((Property) column.getIdentifier());
        }
      });
      if (!conditionProperties.isEmpty()) {
        Properties.sort(conditionProperties);
        final Property property = conditionProperties.size() == 1 ? conditionProperties.get(0) :
                UiUtil.selectValue(this, conditionProperties, Messages.get(Messages.SELECT_INPUT_FIELD));
        if (property != null) {
          final ColumnConditionPanel conditionPanel = getConditionPanel(property.getPropertyId());
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
  public void addFocusGainedListener(final EventDataListener<Property> listener) {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          ((ColumnConditionPanel) panel).addFocusGainedListener(listener);
        }
      });
    }
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
  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    if (canToggleAdvanced()) {
      controlSet.add(Controls.toggleControl(this, "advanced",
              FrameworkMessages.get(FrameworkMessages.ADVANCED), advancedChangedEvent));
    }
    controlSet.add(Controls.control(conditionModel::clearPropertyConditionModels, FrameworkMessages.get(FrameworkMessages.CLEAR)));
    controlSet.addSeparator();
    controlSet.add(conditionRequiredControl);

    return controlSet;
  }

  /**
   * @param propertyId the property ID
   * @return the condition panel associated with the given property, null if none is specified
   */
  public ColumnConditionPanel getConditionPanel(final String propertyId) {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      for (final TableColumn column : columns) {
        final Property property = (Property) column.getIdentifier();
        if (property.is(propertyId)) {
          return (ColumnConditionPanel) ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().get(column);
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
  public void removeAdvancedListener(final EventDataListener listener) {
    advancedChangedEvent.removeDataListener(listener);
  }

  private static JPanel initializeSimpleConditionPanel(final EntityTableConditionModel conditionModel) {
    final JTextField simpleSearchTextField = new JTextField();
    final Control simpleSearchControl = Controls.control(conditionModel::performSimpleSearch, FrameworkMessages.get(FrameworkMessages.SEARCH));
    final JButton simpleSearchButton = new JButton(simpleSearchControl);
    simpleSearchTextField.addActionListener(simpleSearchControl);
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    ValueLinks.textValueLink(simpleSearchTextField, Values.propertyValue(conditionModel, "simpleConditionString",
            String.class, conditionModel.getSimpleConditionStringObserver()));
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
      setVerticalFillerWidth(UiUtil.getPreferredScrollBarWidth());
      resetPanel();
    }

    @Override
    protected JPanel initializeColumnPanel(final TableColumn column) {
      final Property property = (Property) column.getIdentifier();
      if (conditionModel.containsPropertyConditionModel(property.getPropertyId())) {
        return initializeConditionPanel(conditionModel.getPropertyConditionModel(property.getPropertyId()));
      }

      return new JPanel();
    }

    /**
     * Initializes a ColumnConditionPanel for the given model
     * @param propertyConditionModel the {@link ColumnConditionModel} for which to create a condition panel
     * @return a ColumnConditionPanel based on the given model
     */
    private static ColumnConditionPanel initializeConditionPanel(final ColumnConditionModel propertyConditionModel) {
      if (propertyConditionModel instanceof ForeignKeyConditionModel) {
        return new ForeignKeyConditionPanel((ForeignKeyConditionModel) propertyConditionModel, true, false);
      }

      return new PropertyConditionPanel(propertyConditionModel, true, false);
    }
  }
}

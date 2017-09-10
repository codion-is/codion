/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.Events;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.framework.model.ForeignKeyConditionModel;
import org.jminor.framework.model.PropertyConditionModel;
import org.jminor.swing.common.model.table.FilteredTableModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * A UI component based on the EntityTableConditionModel
 * @see EntityTableConditionModel
 */
public final class EntityTableConditionPanel extends JPanel {

  private final Event<Boolean> advancedChangedEvent = Events.event();

  private final EntityTableConditionModel conditionModel;
  private final List<TableColumn> columns;

  private final JPanel advancedConditionPanel;
  private final JPanel simpleConditionPanel;

  /**
   * Instantiates a new EntityTableConditionPanel with a default condition panel setup, based on
   * an {@link AbstractTableColumnSyncPanel} containing {@link PropertyConditionPanel}s
   * @param tableModel the table model
   */
  public EntityTableConditionPanel(final EntityTableModel tableModel) {
    this(tableModel, initializeAdvancedConditionPanel(tableModel, UiUtil.getPreferredScrollBarWidth()),
            initializeSimpleConditionPanel(tableModel.getConditionModel()));
  }

  /**
   * Instantiates a new EntityTableConditionPanel, either {@code advancedConditionPanel}, {@code simpleConditionPanel} or both need to be provided
   * @param tableModel the table model
   * @param advancedConditionPanel the panel to show in case of advanced condition
   * @param simpleConditionPanel the panel to show in case of simple condition
   */
  public EntityTableConditionPanel(final EntityTableModel tableModel, final JPanel advancedConditionPanel,
                                   final JPanel simpleConditionPanel) {
    if (advancedConditionPanel == null && simpleConditionPanel == null) {
      throw new IllegalArgumentException("An advanced and/or a simple condition panel is required");
    }
    this.conditionModel = tableModel.getConditionModel();
    this.columns = ((FilteredTableModel) tableModel).getColumnModel().getAllColumns();
    this.advancedConditionPanel = advancedConditionPanel;
    this.simpleConditionPanel = simpleConditionPanel;
    setLayout(new BorderLayout());
    layoutPanel(true);
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
          ((ColumnConditionPanel) panel).setAdvancedConditionEnabled(value);
        }
      });
    }
    else {
      layoutPanel(value);
    }
    advancedChangedEvent.fire(value);
  }

  /**
   * @return true if advanced search is enabled in the full condition panel,
   * does not apply when simple search is enabled
   */
  public boolean isAdvanced() {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel conditionPanel : ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().values()) {
        if (conditionPanel instanceof ColumnConditionPanel) {
          return ((ColumnConditionPanel) conditionPanel).isAdvancedConditionEnabled();
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
   * Allows the user to select one of the available condition panels for keyboard input focus
   */
  public void selectConditionPanel() {
    if (advancedConditionPanel instanceof AbstractTableColumnSyncPanel) {
      final List<Property> conditionProperties = new ArrayList<>();
      ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
        if (panel instanceof ColumnConditionPanel) {
          conditionProperties.add((Property) column.getIdentifier());
        }
      });
      Entities.sort(conditionProperties);
      final Property property = UiUtil.selectValue(this, conditionProperties, Messages.get(Messages.SELECT_INPUT_FIELD));
      if (property != null) {
        final ColumnConditionPanel conditionPanel = getConditionPanel(property.getPropertyId());
        if (conditionPanel != null) {
          conditionPanel.requestInputFocus();
        }
      }
    }
  }

  /**
   * @param listener a listener notified when a condition panel receives focus
   */
  public void addFocusGainedListener(final EventInfoListener<Property> listener) {
    ((AbstractTableColumnSyncPanel) advancedConditionPanel).getColumnPanels().forEach((column, panel) -> {
      if (panel instanceof ColumnConditionPanel) {
        ((ColumnConditionPanel) panel).addFocusGainedListener(listener);
      }
    });
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
   * @param txt the search text
   */
  public void setSearchText(final String txt) {
    getConditionModel().setSimpleConditionString(txt);
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
  public void addAdvancedListener(final EventInfoListener<Boolean> listener) {
    advancedChangedEvent.addInfoListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedListener(final EventInfoListener listener) {
    advancedChangedEvent.removeInfoListener(listener);
  }

  private static JPanel initializeSimpleConditionPanel(final EntityTableConditionModel conditionModel) {
    final JTextField simpleSearchTextField = new JTextField();
    final Control simpleSearchControl = Controls.control(conditionModel::performSimpleSearch, FrameworkMessages.get(FrameworkMessages.SEARCH));
    final JButton simpleSearchButton = new JButton(simpleSearchControl);
    simpleSearchTextField.addActionListener(simpleSearchControl);
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    ValueLinks.textValueLink(simpleSearchTextField, conditionModel, "simpleConditionString", conditionModel.getSimpleConditionStringObserver());
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    panel.add(simpleSearchTextField, BorderLayout.CENTER);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private static AbstractTableColumnSyncPanel initializeAdvancedConditionPanel(final EntityTableModel tableModel, final int verticalFillerWidth) {
    final AbstractTableColumnSyncPanel panel = new AbstractTableColumnSyncPanel(((FilteredTableModel) tableModel).getColumnModel()) {
      @Override
      protected JPanel initializeColumnPanel(final TableColumn column) {
        final Property property = (Property) column.getIdentifier();
        if (tableModel.getConditionModel().containsPropertyConditionModel(property.getPropertyId())) {
          final PropertyConditionModel propertyConditionModel = tableModel.getConditionModel().getPropertyConditionModel(property.getPropertyId());
          return initializeConditionPanel(propertyConditionModel);
        }
        else {
          return new JPanel();
        }
      }
    };
    panel.setVerticalFillerWidth(verticalFillerWidth);
    panel.resetPanel();

    return panel;
  }

  /**
   * Initializes a ColumnConditionPanel for the given model
   * @param propertyConditionModel the PropertyConditionModel for which to create a condition panel
   * @return a ColumnConditionPanel based on the given model
   */
  @SuppressWarnings({"unchecked"})
  private static ColumnConditionPanel initializeConditionPanel(final PropertyConditionModel propertyConditionModel) {
    if (propertyConditionModel instanceof ForeignKeyConditionModel) {
      return new ForeignKeyConditionPanel((ForeignKeyConditionModel) propertyConditionModel, true, false);
    }

    return new PropertyConditionPanel(propertyConditionModel, true, false);
  }
}

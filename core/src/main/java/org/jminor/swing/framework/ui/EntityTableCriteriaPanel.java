/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.Events;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityTableCriteriaModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.framework.model.ForeignKeyCriteriaModel;
import org.jminor.framework.model.PropertyCriteriaModel;
import org.jminor.swing.common.model.table.FilteredTableModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.swing.common.ui.table.ColumnCriteriaPanel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * A UI component based on the EntityTableCriteriaModel
 * @see EntityTableCriteriaModel
 */
public final class EntityTableCriteriaPanel extends JPanel {

  private final Event<Boolean> advancedChangedEvent = Events.event();

  private final EntityTableCriteriaModel criteriaModel;
  private final List<TableColumn> columns;

  private final JPanel advancedCriteriaPanel;
  private final JPanel simpleCriteriaPanel;

  /**
   * Instantiates a new EntityTableCriteriaPanel with a default criteria panel setup, based on
   * an {@link AbstractTableColumnSyncPanel} containing {@link PropertyCriteriaPanel}s
   * @param tableModel the table model
   */
  public EntityTableCriteriaPanel(final EntityTableModel tableModel) {
    this(tableModel, initializeAdvancedSearchPanel(tableModel, UiUtil.getPreferredScrollBarWidth()),
            initializeSimpleSearchPanel(tableModel.getCriteriaModel()));
  }

  /**
   * Instantiates a new EntityTableCriteriaPanel, either {@code advancedCriteriaPanel}, {@code simpleCriteriaPanel} or both need to be provided
   * @param tableModel the table model
   * @param advancedCriteriaPanel the panel to show in case of advanced criteria
   * @param simpleCriteriaPanel the panel to show in case of simple criteria
   */
  public EntityTableCriteriaPanel(final EntityTableModel tableModel, final JPanel advancedCriteriaPanel,
                                  final JPanel simpleCriteriaPanel) {
    if (advancedCriteriaPanel == null && simpleCriteriaPanel == null) {
      throw new IllegalArgumentException("An advanced and/or a simple criteria panel is required");
    }
    this.criteriaModel = tableModel.getCriteriaModel();
    this.columns = ((FilteredTableModel) tableModel).getColumnModel().getAllColumns();
    this.advancedCriteriaPanel = advancedCriteriaPanel;
    this.simpleCriteriaPanel = simpleCriteriaPanel;
    setLayout(new BorderLayout());
    layoutPanel(true);
  }

  /**
   * @return the criteria model this criteria panel is based on
   */
  public EntityTableCriteriaModel getCriteriaModel() {
    return criteriaModel;
  }

  /**
   * @param value true if advanced search should be enabled in the full criteria panel,
   * does not apply when simple search is enabled
   */
  public void setAdvanced(final boolean value) {
    if (advancedCriteriaPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel searchPanel : ((AbstractTableColumnSyncPanel) advancedCriteriaPanel).getColumnPanels().values()) {
        if (searchPanel instanceof ColumnCriteriaPanel) {
          ((ColumnCriteriaPanel) searchPanel).setAdvancedCriteriaEnabled(value);
        }
      }
    }
    else {
      layoutPanel(value);
    }
    advancedChangedEvent.fire(value);
  }

  /**
   * @return true if advanced search is enabled in the full criteria panel,
   * does not apply when simple search is enabled
   */
  public boolean isAdvanced() {
    if (advancedCriteriaPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel searchPanel : ((AbstractTableColumnSyncPanel) advancedCriteriaPanel).getColumnPanels().values()) {
        if (searchPanel instanceof ColumnCriteriaPanel) {
          return ((ColumnCriteriaPanel) searchPanel).isAdvancedCriteriaEnabled();
        }
      }
    }

    return getComponentCount() > 0 && getComponent(0) == advancedCriteriaPanel;
  }

  public boolean canToggleAdvanced() {
    return advancedCriteriaPanel instanceof AbstractTableColumnSyncPanel || (advancedCriteriaPanel != null && simpleCriteriaPanel != null);
  }

  /**
   * @param advanced if true then the simple criteria panel is shown, if false the advanced criteria panel is shown,
   * note that if either of these is not available calling this method has no effect
   */
  private void layoutPanel(final boolean advanced) {
    if (advanced && advancedCriteriaPanel == null) {
      return;
    }
    if (!advanced && simpleCriteriaPanel == null) {
      return;
    }
    removeAll();
    if (advanced) {
      add(advancedCriteriaPanel, BorderLayout.CENTER);
    }
    else {
      add(simpleCriteriaPanel, BorderLayout.CENTER);
    }
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param txt the search text
   */
  public void setSearchText(final String txt) {
    getCriteriaModel().setSimpleCriteriaString(txt);
  }

  /**
   * @return the controls provided by this criteria panel, for toggling the advanced mode and clearing the criteria
   */
  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    if (canToggleAdvanced()) {
      controlSet.add(Controls.toggleControl(this, "advanced",
              FrameworkMessages.get(FrameworkMessages.ADVANCED), advancedChangedEvent));
    }
    controlSet.add(Controls.methodControl(criteriaModel, "clearPropertyCriteriaModels", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controlSet;
  }

  /**
   * @param propertyID the property ID
   * @return the criteria panel associated with the given property
   */
  public ColumnCriteriaPanel getSearchPanel(final String propertyID) {
    if (advancedCriteriaPanel instanceof AbstractTableColumnSyncPanel) {
      for (final TableColumn column : columns) {
        final Property property = (Property) column.getIdentifier();
        if (property.is(propertyID)) {
          return (ColumnCriteriaPanel) ((AbstractTableColumnSyncPanel) advancedCriteriaPanel).getColumnPanels().get(column);
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

  private static JPanel initializeSimpleSearchPanel(final EntityTableCriteriaModel criteriaModel) {
    final JTextField simpleSearchTextField = new JTextField();
    final Action simpleSearchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        criteriaModel.performSimpleSearch();
      }
    };
    final JButton simpleSearchButton = new JButton(simpleSearchAction);
    simpleSearchTextField.addActionListener(simpleSearchAction);
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    ValueLinks.textValueLink(simpleSearchTextField, criteriaModel, "simpleCriteriaString", criteriaModel.getSimpleCriteriaStringObserver());
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    panel.add(simpleSearchTextField, BorderLayout.CENTER);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private static AbstractTableColumnSyncPanel initializeAdvancedSearchPanel(final EntityTableModel tableModel, final int verticalFillerWidth) {
    final AbstractTableColumnSyncPanel panel = new AbstractTableColumnSyncPanel(((FilteredTableModel) tableModel).getColumnModel()) {
      @Override
      protected JPanel initializeColumnPanel(final TableColumn column) {
        final Property property = (Property) column.getIdentifier();
        if (tableModel.getCriteriaModel().containsPropertyCriteriaModel(property.getPropertyID())) {
          final PropertyCriteriaModel propertyCriteriaModel = tableModel.getCriteriaModel().getPropertyCriteriaModel(property.getPropertyID());
          return initializeSearchPanel(propertyCriteriaModel);
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
   * Initializes a ColumnCriteriaPanel for the given model
   * @param propertyCriteriaModel the PropertyCriteriaModel for which to create a criteria panel
   * @return a ColumnCriteriaPanel based on the given model
   */
  @SuppressWarnings({"unchecked"})
  private static ColumnCriteriaPanel initializeSearchPanel(final PropertyCriteriaModel propertyCriteriaModel) {
    if (propertyCriteriaModel instanceof ForeignKeyCriteriaModel) {
      return new ForeignKeyCriteriaPanel((ForeignKeyCriteriaModel) propertyCriteriaModel, true, false);
    }

    return new PropertyCriteriaPanel(propertyCriteriaModel, true, false);
  }
}

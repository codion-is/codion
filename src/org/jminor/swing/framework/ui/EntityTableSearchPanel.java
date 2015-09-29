/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.Events;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.swing.common.ui.table.ColumnSearchPanel;
import org.jminor.swing.framework.model.EntityTableModel;
import org.jminor.swing.framework.model.EntityTableSearchModel;
import org.jminor.swing.framework.model.ForeignKeySearchModel;
import org.jminor.swing.framework.model.PropertySearchModel;

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
 * A UI component based on the EntityTableSearchModel
 * @see EntityTableSearchModel
 */
public final class EntityTableSearchPanel extends JPanel {

  private final Event<Boolean> advancedChangedEvent = Events.event();

  private final EntityTableSearchModel searchModel;
  private final List<TableColumn> columns;

  private final JPanel advancedSearchPanel;
  private final JPanel simpleSearchPanel;

  /**
   * Instantiates a new EntityTableSearchPanel with a default search panel setup, based on
   * an {@link AbstractTableColumnSyncPanel} containing {@link PropertySearchPanel}s
   * @param tableModel the table model
   */
  public EntityTableSearchPanel(final EntityTableModel tableModel) {
    this(tableModel, initializeAdvancedSearchPanel(tableModel, UiUtil.getPreferredScrollBarWidth()),
            initializeSimpleSearchPanel(tableModel.getSearchModel()));
  }

  /**
   * Instantiates a new EntityTableSearchPanel, either {@code advancedSearchPanel}, {@code simpleSearchPanel} or both need to be provided
   * @param tableModel the table model
   * @param advancedSearchPanel the panel to show in case of advanced search
   * @param simpleSearchPanel the panel to show in case of simple search
   */
  public EntityTableSearchPanel(final EntityTableModel tableModel, final JPanel advancedSearchPanel,
                                final JPanel simpleSearchPanel) {
    if (advancedSearchPanel == null && simpleSearchPanel == null) {
      throw new IllegalArgumentException("An advanced and/or a simple search panel is required");
    }
    this.searchModel = tableModel.getSearchModel();
    this.columns = tableModel.getColumnModel().getAllColumns();
    this.advancedSearchPanel = advancedSearchPanel;
    this.simpleSearchPanel = simpleSearchPanel;
    setLayout(new BorderLayout());
    layoutPanel(true);
  }

  /**
   * @return the search model this search panel is based on
   */
  public EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  /**
   * @param value true if advanced search should be enabled in the full search panel,
   * does not apply when simple search is enabled
   */
  public void setAdvanced(final boolean value) {
    if (advancedSearchPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel searchPanel : ((AbstractTableColumnSyncPanel) advancedSearchPanel).getColumnPanels().values()) {
        if (searchPanel instanceof ColumnSearchPanel) {
          ((ColumnSearchPanel) searchPanel).setAdvancedSearchOn(value);
        }
      }
    }
    else {
      layoutPanel(value);
    }
    advancedChangedEvent.fire(value);
  }

  /**
   * @return true if advanced search is enabled in the full search panel,
   * does not apply when simple search is enabled
   */
  public boolean isAdvanced() {
    if (advancedSearchPanel instanceof AbstractTableColumnSyncPanel) {
      for (final JPanel searchPanel : ((AbstractTableColumnSyncPanel) advancedSearchPanel).getColumnPanels().values()) {
        if (searchPanel instanceof ColumnSearchPanel) {
          return ((ColumnSearchPanel) searchPanel).isAdvancedSearchOn();
        }
      }
    }

    return getComponentCount() > 0 && getComponent(0) == advancedSearchPanel;
  }

  public boolean canToggleAdvanced() {
    return advancedSearchPanel instanceof AbstractTableColumnSyncPanel || (advancedSearchPanel != null && simpleSearchPanel != null);
  }

  /**
   * @param advanced if true then the simple search panel is shown, if false the advanced search panel is shown,
   * note that if either of these is not available calling this method has no effect
   */
  private void layoutPanel(final boolean advanced) {
    if (advanced && advancedSearchPanel == null) {
      return;
    }
    if (!advanced && simpleSearchPanel == null) {
      return;
    }
    removeAll();
    if (advanced) {
      add(advancedSearchPanel, BorderLayout.CENTER);
    }
    else {
      add(simpleSearchPanel, BorderLayout.CENTER);
    }
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param txt the search text
   */
  public void setSearchText(final String txt) {
    getSearchModel().setSimpleSearchString(txt);
  }

  /**
   * @return the controls provided by this search panel, for toggling the
   * advanced mode and clearing the search
   */
  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    if (canToggleAdvanced()) {
      controlSet.add(Controls.toggleControl(this, "advanced",
              FrameworkMessages.get(FrameworkMessages.ADVANCED), advancedChangedEvent));
    }
    controlSet.add(Controls.methodControl(searchModel, "clearPropertySearchModels", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controlSet;
  }

  /**
   * @param propertyID the property ID
   * @return the search panel associated with the given property
   */
  public ColumnSearchPanel getSearchPanel(final String propertyID) {
    if (advancedSearchPanel instanceof AbstractTableColumnSyncPanel) {
      for (final TableColumn column : columns) {
        final Property property = (Property) column.getIdentifier();
        if (property.is(propertyID)) {
          return (ColumnSearchPanel) ((AbstractTableColumnSyncPanel) advancedSearchPanel).getColumnPanels().get(column);
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

  private static JPanel initializeSimpleSearchPanel(final EntityTableSearchModel searchModel) {
    final JTextField simpleSearchTextField = new JTextField();
    final Action simpleSearchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        searchModel.performSimpleSearch();
      }
    };
    final JButton simpleSearchButton = new JButton(simpleSearchAction);
    simpleSearchTextField.addActionListener(simpleSearchAction);
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    ValueLinks.textValueLink(simpleSearchTextField, searchModel, "simpleSearchString", searchModel.getSimpleSearchStringObserver());
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    panel.add(simpleSearchTextField, BorderLayout.CENTER);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private static AbstractTableColumnSyncPanel initializeAdvancedSearchPanel(final EntityTableModel tableModel, final int verticalFillerWidth) {
    final AbstractTableColumnSyncPanel panel = new AbstractTableColumnSyncPanel(tableModel.getColumnModel()) {
      @Override
      protected JPanel initializeColumnPanel(final TableColumn column) {
        final Property property = (Property) column.getIdentifier();
        if (tableModel.getSearchModel().containsPropertySearchModel(property.getPropertyID())) {
          final PropertySearchModel propertySearchModel = tableModel.getSearchModel().getPropertySearchModel(property.getPropertyID());
          return initializeSearchPanel(propertySearchModel);
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
   * Initializes a ColumnSearchPanel for the given model
   * @param propertySearchModel the PropertySearchModel for which to create a search panel
   * @return a PropertySearchPanel based on the given model
   */
  @SuppressWarnings({"unchecked"})
  private static ColumnSearchPanel initializeSearchPanel(final PropertySearchModel propertySearchModel) {
    if (propertySearchModel instanceof ForeignKeySearchModel) {
      return new ForeignKeySearchPanel((ForeignKeySearchModel) propertySearchModel, true, false);
    }

    return new PropertySearchPanel(propertySearchModel, true, false);
  }
}

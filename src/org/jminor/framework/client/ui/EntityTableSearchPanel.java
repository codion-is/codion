/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.common.ui.table.ColumnSearchPanel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.ForeignKeySearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

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

  private final Event evtAdvancedChanged = Events.event();
  private final Event evtSimpleSearchChanged = Events.event();

  private final EntityTableSearchModel searchModel;
  private final List<TableColumn> columns;

  private final AbstractTableColumnSyncPanel fullSearchPanel;
  private final JPanel simpleSearchPanel;
  private final JTextField simpleSearchTextField = new JTextField();
  private final Action simpleSearchAction;
  private boolean simpleSearch = false;

  /**
   * Instantiates a new EntityTableSearchPanel
   */
  public EntityTableSearchPanel(final EntityTableModel tableModel) {
    this(tableModel, UiUtil.getPreferredScrollBarWidth());
  }

  /**
   * Instantiates a new EntityTableSearchPanel
   * @param verticalFillerWidth the vertical filler width, f.ex. the width of a scroll bar
   */
  public EntityTableSearchPanel(final EntityTableModel tableModel, final int verticalFillerWidth) {
    this.searchModel = tableModel.getSearchModel();
    this.columns = tableModel.getColumnModel().getAllColumns();
    this.fullSearchPanel = initializeFullSearchPanel(tableModel, verticalFillerWidth);
    this.simpleSearchAction = initializeSimpleSearchAction();
    this.simpleSearchPanel = initializeSimpleSearchPanel();
    setLayout(new BorderLayout());
    setSimpleSearch(false);
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
    for (final JPanel searchPanel : fullSearchPanel.getColumnPanels().values()) {
      if (searchPanel instanceof ColumnSearchPanel) {
        ((ColumnSearchPanel) searchPanel).setAdvancedSearchOn(value);
      }
    }

    evtAdvancedChanged.fire();
  }

  /**
   * @return true if advanced search is enabled in the full search panel,
   * does not apply when simple search is enabled
   */
  public boolean isAdvanced() {
    for (final JPanel searchPanel : fullSearchPanel.getColumnPanels().values()) {
      if (searchPanel instanceof ColumnSearchPanel) {
        return ((ColumnSearchPanel) searchPanel).isAdvancedSearchOn();
      }
    }

    return false;
  }

  /**
   * @return true if the simple search panel is visible, false if the full panel is visible
   */
  public boolean isSimpleSearch() {
    return simpleSearch;
  }

  /**
   * @param simpleSearch if true then the simple search panel is shown, if false the full
   * search panel is shown
   */
  public void setSimpleSearch(final boolean simpleSearch) {
    this.simpleSearch = simpleSearch;
    removeAll();
    if (simpleSearch) {
      add(simpleSearchPanel, BorderLayout.CENTER);
    }
    else {
      add(fullSearchPanel, BorderLayout.CENTER);
    }
    evtSimpleSearchChanged.fire();
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param txt the search text
   * @see #isSimpleSearch()
   */
  public void setSearchText(final String txt) {
    simpleSearchTextField.setText(txt);
  }

  /**
   * Performs the search
   */
  public void performSearch() {
    simpleSearchAction.actionPerformed(null);
  }

  /**
   * @return the controls provided by this search panel, for toggling the
   * advanced mode and clearing the search
   */
  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    controlSet.add(Controls.toggleControl(this, "advanced",
            FrameworkMessages.get(FrameworkMessages.ADVANCED), evtAdvancedChanged));
    controlSet.add(Controls.methodControl(searchModel, "clearPropertySearchModels", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controlSet;
  }

  /**
   * @param propertyID the property ID
   * @return the search panel associated with the given property
   */
  public ColumnSearchPanel getSearchPanel(final String propertyID) {
    for (final TableColumn column : columns) {
      final Property property = (Property) column.getIdentifier();
      if (property.is(propertyID)) {
        return (ColumnSearchPanel) fullSearchPanel.getColumnPanels().get(column);
      }
    }

    return null;
  }

  /**
   * @param listener a listener notified each time the simple search state changes
   */
  public void addSimpleSearchListener(final EventListener listener) {
    evtSimpleSearchChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSimpleSearchListener(final EventListener listener) {
    evtAdvancedChanged.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time the advanced search state changes
   */
  public void addAdvancedListener(final EventListener listener) {
    evtAdvancedChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedListener(final EventListener listener) {
    evtAdvancedChanged.removeListener(listener);
  }

  private JPanel initializeSimpleSearchPanel() {
    final JButton simpleSearchButton = new JButton(simpleSearchAction);
    simpleSearchTextField.addActionListener(simpleSearchAction);
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    ValueLinks.textValueLink(simpleSearchTextField, searchModel, "simpleSearchString", searchModel.getSimpleSearchStringObserver());
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    panel.add(simpleSearchTextField, BorderLayout.CENTER);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private Action initializeSimpleSearchAction() {
    return new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        searchModel.performSimpleSearch();
      }
    };
  }

  private AbstractTableColumnSyncPanel initializeFullSearchPanel(final EntityTableModel tableModel, final int verticalFillerWidth) {
    final AbstractTableColumnSyncPanel panel = new AbstractTableColumnSyncPanel(tableModel.getColumnModel(), tableModel.getColumnModel().getAllColumns()) {
      /** {@inheritDoc} */
      @Override
      protected JPanel initializeColumnPanel(final TableColumn column) {
        final Property property = (Property) column.getIdentifier();
        if (searchModel.containsPropertySearchModel(property.getPropertyID())) {
          final PropertySearchModel propertySearchModel = searchModel.getPropertySearchModel(property.getPropertyID());
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
  private ColumnSearchPanel initializeSearchPanel(final PropertySearchModel propertySearchModel) {
    if (propertySearchModel instanceof ForeignKeySearchModel) {
      return new ForeignKeySearchPanel((ForeignKeySearchModel) propertySearchModel, false);
    }

    return new PropertySearchPanel(propertySearchModel, true, false);
  }
}

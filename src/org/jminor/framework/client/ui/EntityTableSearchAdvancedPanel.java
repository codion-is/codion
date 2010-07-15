/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Event;
import org.jminor.common.ui.AbstractTableColumnSyncPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A UI component based on the EntityTableSearchModel
 * @see EntityTableSearchModel
 */
public class EntityTableSearchAdvancedPanel extends AbstractTableColumnSyncPanel implements EntityTableSearchPanel {

  private final Event evtAdvancedChanged = new Event();

  private final EntityTableSearchModel searchModel;

  public EntityTableSearchAdvancedPanel(final EntityTableSearchModel searchModel, final TableColumnModel columnModel) {
    this(searchModel, columnModel, UiUtil.getPreferredScrollBarWidth());
  }

  public EntityTableSearchAdvancedPanel(final EntityTableSearchModel searchModel, final TableColumnModel columnModel,
                                final int verticalFillerWidth) {
    super(columnModel);
    this.searchModel = searchModel;
    setVerticalFillerWidth(verticalFillerWidth);
    resetPanel();
  }

  public EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  /**
   * @param value true if wildcards should automatically be added to strings
   */
  public void setAutomaticWildcard(final boolean value) {
    for (final JPanel searchPanel : getColumnPanels().values()) {
      if (searchPanel instanceof PropertySearchPanel) {
        ((PropertySearchPanel) searchPanel).getModel().setAutomaticWildcard(value);
      }
    }
  }

  /**
   * @param value true if advanced search should be enabled
   */
  public void setAdvanced(final boolean value) {
    for (final JPanel searchPanel : getColumnPanels().values()) {
      if (searchPanel instanceof PropertySearchPanel) {
        ((PropertySearchPanel) searchPanel).setAdvancedSearchOn(value);
      }
    }

    evtAdvancedChanged.fire();
  }

  /**
   * @return true if advanced search is enabled
   */
  public boolean isAdvanced() {
    for (final JPanel searchPanel : getColumnPanels().values()) {
      if (searchPanel instanceof PropertySearchPanel) {
        return ((PropertySearchPanel) searchPanel).isAdvancedSearchOn();
      }
    }

    return false;
  }

  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    controlSet.add(ControlFactory.toggleControl(this, "advanced",
            FrameworkMessages.get(FrameworkMessages.ADVANCED), evtAdvancedChanged));
    controlSet.add(ControlFactory.methodControl(searchModel, "clearPropertySearchModels", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controlSet;
  }

  public PropertySearchPanel getSearchPanel(final String propertyID) {
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      final Property property = (Property) column.getIdentifier();
      if (property.is(propertyID)) {
        return (PropertySearchPanel) getColumnPanels().get(column);
      }
    }

    return null;
  }

  public Event eventAdvancedChanged() {
    return evtAdvancedChanged;
  }

  @Override
  protected Map<TableColumn, JPanel> initializeColumnPanels() {
    final Map<TableColumn, JPanel> panels = new HashMap<TableColumn, JPanel>();
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      final Property property = (Property) column.getIdentifier();
      if (searchModel.containsPropertySearchModel(property.getPropertyID())) {
        final PropertySearchModel propertySearchModel = searchModel.getPropertySearchModel(property.getPropertyID());
        panels.put(column, initializePropertySearchPanel(propertySearchModel));
      }
      else {
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));
        panels.put(column, panel);
      }
    }

    return panels;
  }

  /**
   * Initializes a PropertySearchPanel for the given model
   * @param propertySearchModel the PropertySearchModel for which to create a search panel
   * @return a PropertySearchPanel based on the given model
   */
  protected PropertySearchPanel initializePropertySearchPanel(final PropertySearchModel propertySearchModel) {
    return new PropertySearchPanel(propertySearchModel, true, false);
  }
}

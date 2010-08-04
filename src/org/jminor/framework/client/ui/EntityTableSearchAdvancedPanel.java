/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.ui.AbstractTableColumnSyncPanel;
import org.jminor.common.ui.ColumnSearchPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.ForeignKeySearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Enumeration;

/**
 * A UI component based on the EntityTableSearchModel
 * @see EntityTableSearchModel
 */
public final class EntityTableSearchAdvancedPanel extends AbstractTableColumnSyncPanel implements EntityTableSearchPanel {

  private final Event evtAdvancedChanged = Events.event();

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
      if (searchPanel instanceof ColumnSearchPanel) {
        ((ColumnSearchPanel) searchPanel).getModel().setAutomaticWildcard(value);
      }
    }
  }

  /**
   * @param value true if advanced search should be enabled
   */
  public void setAdvanced(final boolean value) {
    for (final JPanel searchPanel : getColumnPanels().values()) {
      if (searchPanel instanceof ColumnSearchPanel) {
        ((ColumnSearchPanel) searchPanel).setAdvancedSearchOn(value);
      }
    }

    evtAdvancedChanged.fire();
  }

  /**
   * @return true if advanced search is enabled
   */
  public boolean isAdvanced() {
    for (final JPanel searchPanel : getColumnPanels().values()) {
      if (searchPanel instanceof ColumnSearchPanel) {
        return ((ColumnSearchPanel) searchPanel).isAdvancedSearchOn();
      }
    }

    return false;
  }

  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    controlSet.add(Controls.toggleControl(this, "advanced",
            FrameworkMessages.get(FrameworkMessages.ADVANCED), evtAdvancedChanged));
    controlSet.add(Controls.methodControl(searchModel, "clearPropertySearchModels", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controlSet;
  }

  public ColumnSearchPanel getSearchPanel(final String propertyID) {
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      final Property property = (Property) column.getIdentifier();
      if (property.is(propertyID)) {
        return (ColumnSearchPanel) getColumnPanels().get(column);
      }
    }

    return null;
  }

  public void addAdvancedListener(final ActionListener listener) {
    evtAdvancedChanged.addListener(listener);
  }

  public void removeAdvancedListener(final ActionListener listener) {
    evtAdvancedChanged.removeListener(listener);
  }

  @Override
  protected JPanel initializeColumnPanel(final TableColumn column) {
    final Property property = (Property) column.getIdentifier();
    if (searchModel.containsPropertySearchModel(property.getPropertyID())) {
      final PropertySearchModel propertySearchModel = searchModel.getPropertySearchModel(property.getPropertyID());
      return initializeSearchPanel(propertySearchModel);
    }
    else {
      final JPanel panel = new JPanel();
      panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));
      return panel;
    }
  }

  /**
   * Initializes a AbstractSearchPanel for the given model
   * @param propertySearchModel the PropertySearchModel for which to create a search panel
   * @return a PropertySearchPanel based on the given model
   */
  private ColumnSearchPanel initializeSearchPanel(final PropertySearchModel propertySearchModel) {
    if (propertySearchModel instanceof ForeignKeySearchModel) {
      return new ForeignKeySearchPanel((ForeignKeySearchModel) propertySearchModel, false);
    }

    return new PropertySearchPanel(propertySearchModel, true, false);
  }
}

/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Event;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class EntityTableSearchPanel extends JPanel {

  public final Event evtAdvancedChanged = new Event();

  private final EntityTableSearchModel searchModel;

  private final List<JPanel> searchPanels;

  public EntityTableSearchPanel(final EntityTableSearchModel searchModel) {
    if (searchModel == null)
      throw new IllegalArgumentException("EntityTableSearchPanel requires a non-null EntityTableSearchModel instance");
    this.searchModel = searchModel;
    this.searchPanels = initializeSearchPanels();
    initializeUI();
  }

  /**
   * @param value true if wildcards should automatically be added to strings
   */
  public void setAutomaticWildcard(final boolean value) {
    for (final JPanel searchPanel : searchPanels) {
      if (searchPanel instanceof PropertySearchPanel)
        ((PropertySearchPanel)searchPanel).getModel().setAutomaticWildcard(value);
    }
  }

  public void clear() {
    for (final JPanel searchPanel : searchPanels) {
      if (searchPanel instanceof PropertySearchPanel)
        ((PropertySearchPanel)searchPanel).getModel().clear();
    }
  }

  /**
   * @param value true if advanced search should be enabled
   */
  public void setAdvanced(final boolean value) {
    for (final JPanel searchPanel : searchPanels)
      if (searchPanel instanceof PropertySearchPanel)
        ((PropertySearchPanel)searchPanel).setAdvancedSearchOn(value);

    evtAdvancedChanged.fire();
  }

  /**
   * @return true if advanced search is enabled
   */
  public boolean isAdvanced() {
    for (final JPanel searchPanel : searchPanels)
      if (searchPanel instanceof PropertySearchPanel)
        return ((PropertySearchPanel)searchPanel).isAdvancedSearchOn();

    return false;
  }

  public void bindToColumnSizes(final JTable table) {
    UiUtil.bindColumnAndPanelSizes(table.getColumnModel(), searchPanels);
  }

  /** {@inheritDoc} */
  @Override
  public Dimension getPreferredSize() {
    for (final JPanel searchPanel : searchPanels)
      if (searchPanel instanceof PropertySearchPanel)
        return new Dimension(super.getPreferredSize().width, searchPanel.getPreferredSize().height);

    return new Dimension(super.getPreferredSize().width, searchPanels.get(0).getPreferredSize().height);
  }

  public ControlSet getControls() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    controlSet.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    controlSet.add(ControlFactory.toggleControl(this, "advanced",
            FrameworkMessages.get(FrameworkMessages.ADVANCED), evtAdvancedChanged));
    controlSet.add(ControlFactory.methodControl(this, "clear", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return controlSet;
  }

  public PropertySearchPanel getSearchPanel(final String propertyID) {
    for (final JPanel panel : searchPanels) {
      if (panel instanceof PropertySearchPanel && ((PropertySearchPanel)panel).getModel().getProperty().is(propertyID))
        return (PropertySearchPanel) panel;
    }

    return null;
  }

  protected void initializeUI() {
    setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    for (final JPanel searchPanel : searchPanels)
      add(searchPanel);
  }

  private List<JPanel> initializeSearchPanels() {
    final List<JPanel> panels = new ArrayList<JPanel>();
    final Enumeration<TableColumn> columnEnumeration = searchModel.getTableColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final Property property = (Property) columnEnumeration.nextElement().getIdentifier();
      final PropertySearchModel propertySearchModel = searchModel.getPropertySearchModel(property.getPropertyID());
      if (propertySearchModel != null)
        panels.add(new PropertySearchPanel(propertySearchModel, true, false));
      else {
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));
        panels.add(panel);
      }
    }

    return panels;
  }
}

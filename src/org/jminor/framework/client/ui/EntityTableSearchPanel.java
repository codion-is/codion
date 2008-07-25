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
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Property;

import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class EntityTableSearchPanel extends JPanel {

  public final Event evtAdvancedChanged = new Event("EntityTableSearchPanel.evtAdvancedChanged");

  private final EntityTableSearchModel searchModel;
  private final List<Property> tableColumnProperties;

  private final List<JPanel> searchPanels;

  public EntityTableSearchPanel(final EntityTableSearchModel searchModel, final List<Property> tableColumnProperties) {
    this.searchModel = searchModel;
    this.tableColumnProperties = tableColumnProperties;
    this.searchPanels = initializeSearchPanels();
    initializeUI();
  }

  /**
   * @param value Value to set for property 'automaticWildcard'.
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
   * @param value Value to set for property 'advanced'.
   */
  public void setAdvanced(final boolean value) {
    for (final JPanel searchPanel : searchPanels)
      if (searchPanel instanceof PropertySearchPanel)
        ((PropertySearchPanel)searchPanel).setAdvancedSearchOn(value);

    evtAdvancedChanged.fire();
  }

  /**
   * @return Value for property 'advanced'.
   */
  public boolean isAdvanced() {
    for (final JPanel searchPanel : searchPanels)
      if (searchPanel instanceof PropertySearchPanel)
        return ((PropertySearchPanel)searchPanel).isAdvancedSearchOn();

    return false;
  }

  public void bindSizeToColumns(final JTable table) {
    UiUtil.bindColumnSizesAndPanelSizes(table, searchPanels);
  }

  /** {@inheritDoc} */
  public Dimension getPreferredSize() {
    for (final JPanel searchPanel : searchPanels)
      if (searchPanel instanceof PropertySearchPanel)
        return new Dimension(super.getPreferredSize().width, searchPanel.getPreferredSize().height);

    return new Dimension(super.getPreferredSize().width, searchPanels.get(0).getPreferredSize().height);
  }

  public ControlSet getControls() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
    ret.setIcon(Images.loadImage("Filter16.gif"));
    ret.add(ControlFactory.toggleControl(this, "advanced",
            FrameworkMessages.get(FrameworkMessages.ADVANCED), evtAdvancedChanged));
    ret.add(ControlFactory.methodControl(this, "clear", FrameworkMessages.get(FrameworkMessages.CLEAR)));

    return ret;
  }

  protected void initializeUI() {
    setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    for (final JPanel searchPanel : searchPanels)
      add(searchPanel);
  }

  private List<JPanel> initializeSearchPanels() {
    final ArrayList<JPanel> ret = new ArrayList<JPanel>(tableColumnProperties.size());
    for (final Property property : tableColumnProperties) {
      final PropertySearchModel propertySearchModel = searchModel.getPropertySearchModel(property.propertyID);
      if (propertySearchModel != null)
        ret.add(new PropertySearchPanel(propertySearchModel, true, false, searchModel.getDbProvider()));
      else {
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));
        ret.add(panel);
      }
    }

    return ret;
  }
}

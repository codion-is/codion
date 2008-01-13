/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Event;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Property;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class EntityTableSearchPanel extends JPanel {

  public final Event evtAdvancedChanged = new Event("EntityTableSearchPanel.evtAdvancedChanged");

  private final List<JPanel> searchPanels;

  public EntityTableSearchPanel(final EntityTablePanel entityTable) {
    searchPanels = initializeSearchPanels(entityTable.getTableModel());
    initializeUI();
    UiUtil.bindColumnSizesAndPanelSizes(entityTable.getJTable(),
            searchPanels.toArray(new JPanel[searchPanels.size()]));
  }

  /**
   * @return Value for property 'searchPanels'.
   */
  public List<JPanel> getSearchPanels() {
    return searchPanels;
  }

  /**
   * @param val Value to set for property 'automaticWildcardOn'.
   */
  public void setAutomaticWildcardOn(boolean val) {
    for (final JPanel searchPanel : searchPanels) {
      if (searchPanel instanceof PropertySearchPanel)
        ((PropertySearchPanel)searchPanel).getModel().setAutomaticWildcardOn(val);
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
    for (final JPanel searchPanel : searchPanels) {
      if (searchPanel instanceof PropertySearchPanel)
        ((PropertySearchPanel)searchPanel).setAdvancedSearchOn(value);
      else
        setPreferredSize(searchPanel);
    }
    evtAdvancedChanged.fire();
  }

  /**
   * @param searchPanel Value to set for property 'preferredSize'.
   */
  private void setPreferredSize(final JPanel searchPanel) {
    searchPanel.setPreferredSize(new Dimension(searchPanel.getPreferredSize().width,
            searchPanels.get(0).getPreferredSize().height));
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

  /** {@inheritDoc} */
  public Dimension getPreferredSize() {
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

  private List<JPanel> initializeSearchPanels(final EntityTableModel tableModel) {
    final List<Property> properties = tableModel.getTableColumnProperties();
    final ArrayList<JPanel> ret = new ArrayList<JPanel>(properties.size());
    for (final Property property : properties) {
      final PropertySearchModel searchModel = tableModel.getPropertySearchModel(property.propertyID);
      if (searchModel != null)
        ret.add(new PropertySearchPanel(searchModel, true, false, tableModel.getDbConnectionProvider()));
      else {
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));
        ret.add(panel);
      }
    }

    return ret;
  }
}

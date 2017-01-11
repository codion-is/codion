/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(4,2,5,5));
    JTextField txt = createTextField(ADDRESS_CITY);
    setInitialFocusComponent(txt);
    txt.setColumns(12);
    addPropertyPanel(ADDRESS_CITY);
    txt = createTextField(ADDRESS_STATE);
    txt.setColumns(12);
    addPropertyPanel(ADDRESS_STATE);
    add(new JLabel());
    txt = createTextField(ADDRESS_ZIP);
    txt.setColumns(12);
    addPropertyPanel(ADDRESS_ZIP);
    txt = createTextField(ADDRESS_STREET_1);
    txt.setColumns(12);
    addPropertyPanel(ADDRESS_STREET_1);
    txt = createTextField(ADDRESS_STREET_2);
    txt.setColumns(12);
    addPropertyPanel(ADDRESS_STREET_2);
    createTextField(ADDRESS_LATITUDE);
    addPropertyPanel(ADDRESS_LATITUDE);
    createTextField(ADDRESS_LONGITUDE);
    addPropertyPanel(ADDRESS_LONGITUDE);
  }
}
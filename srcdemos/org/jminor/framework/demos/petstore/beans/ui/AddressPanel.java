/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class AddressPanel extends EntityEditPanel {

  public AddressPanel(final EntityEditModel model) {
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
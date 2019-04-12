/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ADDRESS_CITY);

    createTextField(ADDRESS_CITY).setColumns(12);
    createTextField(ADDRESS_STATE).setColumns(12);
    createTextField(ADDRESS_ZIP).setColumns(12);
    createTextField(ADDRESS_STREET_1).setColumns(12);
    createTextField(ADDRESS_STREET_2).setColumns(12);
    createTextField(ADDRESS_LATITUDE);
    createTextField(ADDRESS_LONGITUDE);

    setLayout(new FlexibleGridLayout(4, 2, 5, 5));
    addPropertyPanel(ADDRESS_CITY);
    addPropertyPanel(ADDRESS_STATE);
    add(new JLabel());
    addPropertyPanel(ADDRESS_ZIP);
    addPropertyPanel(ADDRESS_STREET_1);
    addPropertyPanel(ADDRESS_STREET_2);
    addPropertyPanel(ADDRESS_LATITUDE);
    addPropertyPanel(ADDRESS_LONGITUDE);
  }
}
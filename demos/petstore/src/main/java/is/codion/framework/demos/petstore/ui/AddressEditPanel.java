/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;

import static is.codion.framework.demos.petstore.domain.Petstore.Address;

public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Address.CITY);

    createTextField(Address.CITY).setColumns(12);
    createTextField(Address.STATE).setColumns(12);
    createTextField(Address.ZIP).setColumns(12);
    createTextField(Address.STREET_1).setColumns(12);
    createTextField(Address.STREET_2).setColumns(12);
    createTextField(Address.LATITUDE);
    createTextField(Address.LONGITUDE);

    setLayout(Layouts.flexibleGridLayout(4, 2));
    addPropertyPanel(Address.CITY);
    addPropertyPanel(Address.STATE);
    add(new JLabel());
    addPropertyPanel(Address.ZIP);
    addPropertyPanel(Address.STREET_1);
    addPropertyPanel(Address.STREET_2);
    addPropertyPanel(Address.LATITUDE);
    addPropertyPanel(Address.LONGITUDE);
  }
}
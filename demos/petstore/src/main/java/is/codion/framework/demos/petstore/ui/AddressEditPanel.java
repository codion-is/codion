/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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

    textField(Address.CITY).columns(12).build();
    textField(Address.STATE).columns(12).build();
    textField(Address.ZIP).columns(12).build();
    textField(Address.STREET_1).columns(12).build();
    textField(Address.STREET_2).columns(12).build();
    textField(Address.LATITUDE).build();
    textField(Address.LONGITUDE).build();

    setLayout(Layouts.flexibleGridLayout(4, 2));
    addInputPanel(Address.CITY);
    addInputPanel(Address.STATE);
    add(new JLabel());
    addInputPanel(Address.ZIP);
    addInputPanel(Address.STREET_1);
    addInputPanel(Address.STREET_2);
    addInputPanel(Address.LATITUDE);
    addInputPanel(Address.LONGITUDE);
  }
}
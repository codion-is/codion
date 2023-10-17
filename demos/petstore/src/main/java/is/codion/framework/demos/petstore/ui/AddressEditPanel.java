/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;

import static is.codion.framework.demos.petstore.domain.Petstore.Address;

public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Address.CITY);

    createTextField(Address.CITY);
    createTextField(Address.STATE);
    createTextField(Address.ZIP);
    createTextField(Address.STREET_1);
    createTextField(Address.STREET_2);
    createTextField(Address.LATITUDE);
    createTextField(Address.LONGITUDE);

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
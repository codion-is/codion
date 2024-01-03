/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

// tag::addressEditPanel[]
public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Address.STREET);

    createTextField(Address.STREET).columns(25);
    createTextField(Address.CITY).columns(25);
    createCheckBox(Address.VALID);

    setLayout(new GridLayout(3, 1, 5, 5));
    addInputPanel(Address.STREET);
    addInputPanel(Address.CITY);
    addInputPanel(Address.VALID);
  }
}
// end::addressEditPanel[]
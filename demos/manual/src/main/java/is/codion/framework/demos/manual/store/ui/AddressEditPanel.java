/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;

import java.awt.GridLayout;

// tag::addressEditPanel[]
public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Address.STREET);

    createTextField(Address.STREET).setColumns(25);
    createTextField(Address.CITY).setColumns(25);
    createCheckBox(Address.VALID, IncludeCaption.NO);

    setLayout(new GridLayout(3, 1, 5, 5));
    addInputPanel(Address.STREET);
    addInputPanel(Address.CITY);
    addInputPanel(Address.VALID);
  }
}
// end::addressEditPanel[]
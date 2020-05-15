/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store;
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
    setInitialFocusProperty(Store.ADDRESS_STREET);

    createTextField(Store.ADDRESS_STREET).setColumns(25);
    createTextField(Store.ADDRESS_CITY).setColumns(25);
    createCheckBox(Store.ADDRESS_VALID, null, IncludeCaption.NO);

    setLayout(new GridLayout(3, 1, 5, 5));
    addPropertyPanel(Store.ADDRESS_STREET);
    addPropertyPanel(Store.ADDRESS_CITY);
    addPropertyPanel(Store.ADDRESS_VALID);
  }
}
// end::addressEditPanel[]
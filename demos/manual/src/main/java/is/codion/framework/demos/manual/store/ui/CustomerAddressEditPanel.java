/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

// tag::customerAddressEditPanel[]
public class CustomerAddressEditPanel extends EntityEditPanel {

  public CustomerAddressEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(CustomerAddress.ADDRESS_FK);

    createForeignKeyComboBoxPanel(CustomerAddress.ADDRESS_FK, () ->
            new AddressEditPanel(new SwingEntityEditModel(Address.TYPE, editModel().connectionProvider())))
            .preferredWidth(280)
            .addButton(true);

    setLayout(borderLayout());

    addInputPanel(CustomerAddress.ADDRESS_FK);
  }
}
// end::customerAddressEditPanel[]
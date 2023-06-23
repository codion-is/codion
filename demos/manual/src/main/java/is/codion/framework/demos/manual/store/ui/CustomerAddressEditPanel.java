/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.buttonPanel;

// tag::customerAddressEditPanel[]
public class CustomerAddressEditPanel extends EntityEditPanel {

  public CustomerAddressEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(CustomerAddress.ADDRESS_FK);

    EntityComboBox addressComboBox =
            createForeignKeyComboBox(CustomerAddress.ADDRESS_FK)
                    .preferredWidth(200)
                    .build();
    Control newAddressControl = EntityPanel.builder(Address.TYPE)
            .editPanelClass(AddressEditPanel.class)
            .createInsertControl(addressComboBox);
    JPanel addressPanel = borderLayoutPanel()
            .centerComponent(addressComboBox)
            .eastComponent(buttonPanel(newAddressControl).build())
            .build();

    setLayout(new BorderLayout(5, 5));

    addInputPanel(CustomerAddress.ADDRESS_FK, addressPanel);
  }
}
// end::customerAddressEditPanel[]
/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.swing.common.ui.component.panel.Panels;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.BorderLayout;

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
    Action newAddressAction = EntityPanel.builder(Address.TYPE)
            .editPanelClass(AddressEditPanel.class)
            .createEditPanelAction(addressComboBox);
    JPanel addressPanel = Panels.createEastButtonPanel(addressComboBox, newAddressAction);

    setLayout(new BorderLayout(5, 5));

    addInputPanel(CustomerAddress.ADDRESS_FK, addressPanel);
  }
}
// end::customerAddressEditPanel[]
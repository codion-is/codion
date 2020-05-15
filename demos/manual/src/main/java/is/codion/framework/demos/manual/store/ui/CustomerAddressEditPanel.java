/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.ui;

import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.swing.common.ui.Components;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityComboBox;
import dev.codion.swing.framework.ui.EntityEditPanel;
import dev.codion.swing.framework.ui.EntityPanelBuilder;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.BorderLayout;

// tag::customerAddressEditPanel[]
public class CustomerAddressEditPanel extends EntityEditPanel {

  public CustomerAddressEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(Store.CUSTOMER_ADDRESS_ADDRESS_FK);

    EntityComboBox addressComboBox =
            createForeignKeyComboBox(Store.CUSTOMER_ADDRESS_ADDRESS_FK);
    Components.setPreferredWidth(addressComboBox, 200);
    Action newAddressAction = new EntityPanelBuilder(Store.T_ADDRESS).setEditPanelClass(AddressEditPanel.class)
            .createEditPanelAction(addressComboBox);
    JPanel addressPanel = Components.createEastButtonPanel(addressComboBox, newAddressAction);

    setLayout(new BorderLayout(5, 5));

    add(createPropertyPanel(Store.CUSTOMER_ADDRESS_ADDRESS_FK, addressPanel));
  }
}
// end::customerAddressEditPanel[]
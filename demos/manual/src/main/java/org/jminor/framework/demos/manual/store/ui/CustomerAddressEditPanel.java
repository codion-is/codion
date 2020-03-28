/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.ui;

import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;

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
    Action newAddressAction = EntityEditPanel.createEditPanelAction(addressComboBox,
            new EntityPanelBuilder(Store.T_ADDRESS)
                    .setEditPanelClass(AddressEditPanel.class));
    JPanel addressPanel = Components.createEastButtonPanel(addressComboBox, newAddressAction, false);

    setLayout(new BorderLayout(5, 5));

    add(createPropertyPanel(Store.CUSTOMER_ADDRESS_ADDRESS_FK, addressPanel));
  }
}
// end::customerAddressEditPanel[]